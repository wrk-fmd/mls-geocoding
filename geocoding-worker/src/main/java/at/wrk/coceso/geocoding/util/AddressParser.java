package at.wrk.coceso.geocoding.util;

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.commons.lang3.StringUtils.upperCase;

import at.wrk.coceso.geocoding.api.dto.Address;
import at.wrk.coceso.geocoding.api.dto.Address.AddressBuilder;
import at.wrk.coceso.geocoding.api.dto.Address.Number;
import at.wrk.coceso.geocoding.api.dto.Address.Number.NumberBuilder;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class AddressParser {

    // TODO Maybe these Regexes can be cleaned up a little
    private static final Pattern STREET_PATTERN = Pattern.compile("^(\\w[\\w\\s\\-.]*?)"
        + "( ([1-9]\\d*(-([1-9]\\d*)|[a-zA-Z])?)?(/.*)?)?( # (\\w[\\w\\s\\-.]*))?$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern CITY_PATTERN = Pattern
        .compile("^(([1-9]\\d{3,4}) )?(\\w[\\w\\s\\-.]*)$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern NUMBER_PARTS = Pattern.compile("([1-9]\\d*)(-([1-9]\\d*)|[a-zA-Z])?");


    public static Address parseFromString(final String addressString) {
        if (StringUtils.isBlank(addressString)) {
            return null;
        }

        String[] lines = addressString.trim().split("\n");
        if (lines.length < 1) {
            // This should never happen, but keep it in case something above changes
            return null;
        }

        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].trim();
        }

        AddressBuilder builder = Address.builder();
        Matcher street0 = STREET_PATTERN.matcher(lines[0]);
        if (lines.length == 1) {
            if (street0.find(0)) {
                // First (and only) line represents street
                setFromRegex(builder, street0, null);
            } else {
                // Use as title (e.g. POI)
                builder.title(lines[0]);
            }
        } else {
            Matcher street1 = STREET_PATTERN.matcher(lines[1]);
            Matcher city1 = CITY_PATTERN.matcher(lines[1]);
            Matcher city2 = lines.length >= 3 ? CITY_PATTERN.matcher(lines[2]) : null;
            int additionalStart;

            if (city2 != null && street1.find(0) && city2.find(0)) {
                // Second line is street, third is city
                builder.title(lines[0]);
                setFromRegex(builder, street1, city2);
                additionalStart = 3;
            } else if (street0.find(0) && city1.find(0)) {
                // First line is street, second is city
                setFromRegex(builder, street0, city1);
                additionalStart = 2;
            } else if (street1.find(0)) {
                // Second line is street
                builder.title(lines[0]);
                setFromRegex(builder, street1, null);
                additionalStart = 2;
            } else if (street0.find(0)) {
                // First line is street
                setFromRegex(builder, street0, null);
                additionalStart = 1;
            } else {
                builder.title(lines[0]);
                additionalStart = 1;
            }

            builder.additional(String.join("\n", Arrays.copyOfRange(lines, additionalStart, lines.length)).trim());
        }

        return builder.build();
    }


    private static void setFromRegex(AddressBuilder builder, Matcher street, Matcher city) {
        if (street != null) {
            builder.street(trimToNull(street.group(1)));
            builder.number(parseNumber(trimToNull(street.group(2))));
            builder.intersection(trimToNull(street.group(8)));
        }
        if (city != null) {
            builder.postCode(parseInt(city.group(2)));
            builder.city(trimToNull(city.group(3)));
        }
    }

    public static Number parseNumber(String number) {
        NumberBuilder builder = Number.builder();
        if (number == null) {
            return builder.build();
        }

        String[] components = number.split("/", 3);
        if (components.length >= 3) {
            builder.details(trimToNull(components[2]));
        }
        if (components.length >= 2) {
            builder.block(trimToNull(components[1]));
        }
        if (components.length >= 1) {
            if (!components[0].isBlank()) {
                Matcher matcher = NUMBER_PARTS.matcher(components[0].trim());
                if (matcher.find()) {
                    Integer parsedFrom = parseInt(matcher.group(1));
                    if (parsedFrom != null) {
                        builder.from(parsedFrom);
                        Integer parsedTo = parseInt(matcher.group(3));
                        if (parsedTo == null) {
                            // No to for range given, try with a letter
                            builder.letter(trimToNull(upperCase(matcher.group(2))));
                        } else if (parsedTo > parsedFrom || parsedTo % 2 == parsedFrom % 2) {
                            // Valid interval
                            builder.to(parsedTo);
                        }
                    }
                }
            }
        }

        return builder.build();
    }

    public static Integer parseInt(String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }

        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
