package at.wrk.fmd.mls.geocoding.util;

import static org.apache.commons.lang3.StringUtils.trimToNull;

import at.wrk.fmd.mls.geocoding.api.dto.Address;
import at.wrk.fmd.mls.geocoding.api.dto.Address.AddressBuilder;
import at.wrk.fmd.mls.geocoding.api.dto.PointDto;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AddressParser {

    private static final Pattern STREET_PATTERN = Pattern.compile("^"
            // Group 1: Street name (non-greedy)
            + "(\\w[\\w\\s\\-.]*?)"
            // Group 2: Optional number sequence
            + "( "
            // Group 3: Optional house number (may include range or letter suffix in group 4)
            + "([1-9]\\d*(-[1-9]\\d*|[a-zA-Z])?)?"
            // Group 5+6: Block starts with "/", then can contain everything up to the next "/"
            + "(/([^/]*))?"
            // Group 7+8: Number details start with "/", then contain everything until the end of the line
            + "(/(.*))?"
            // End number sequence
            + ")?"
            // Group 9+10: Optional intersection starts with " # ", then street pattern as before
            + "( # (\\w[\\w\\s\\-.]*))?"
            // Match full line
            + "$", Pattern.UNICODE_CHARACTER_CLASS);

    private static final Pattern CITY_PATTERN = Pattern.compile("^"
            // Group 1: Post code
            + "([1-9]\\d{3,4})?"
            // Whitespace between post code and city, if both are given
            + " ?"
            // Group 2: City
            + "(\\w[\\w\\s\\-.]*)?"
            // Match full line
            + "$", Pattern.UNICODE_CHARACTER_CLASS);

//	private static final Pattern NUMBER_PARTS = Pattern.compile("([1-9]\\d*)(-([1-9]\\d*)|[a-zA-Z])?");

    public static PointDto parseAddress(final PointDto request) {
        if (request.getAddress() != null) {
            // Address already parsed, do nothing
            return request;
        }

        String details = trimToNull(request.getDetails());
        if (details == null) {
            // Nothing to parse
            return request.withDetails(null);
        }

        String[] lines = details.split("\n");
        if (lines.length < 1) {
            // This should never happen, but keep it in case something above changes
            return request;
        }

        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].trim();
        }

        // Simpler parsing then before: First line is street, optional second line is city, further lines are details
        // There is no title line in non-POI entries
        Matcher street = STREET_PATTERN.matcher(lines[0]);
        if (!street.find(0)) {
            // First line does not match street, use (trimmed) original details
            return request.withDetails(details);
        }

        // Set parsed street data
        AddressBuilder builder = Address.builder()
                .street(trimToNull(street.group(1)))
                .number(trimToNull(street.group(3)))
                .block(trimToNull(street.group(6)))
                .details(trimToNull(street.group(8)))
                .intersection(trimToNull(street.group(10)));

        if (lines.length <= 1) {
            // No more lines: We are done and have no details block
            return request.withAddress(builder.build()).withDetails(null);
        }

        // Now try to parse a city from the second line
        int detailsStart = 1;
        Matcher city = CITY_PATTERN.matcher(lines[1]);
        if (city.find(0)) {
            builder.postCode(parseInt(city.group(1)));
            builder.city(trimToNull(city.group(2)));
            detailsStart = 2;
        }

        String remainingDetails = trimToNull(Arrays.stream(lines).skip(detailsStart).collect(Collectors.joining("\n")));
        return request.withAddress(builder.build()).withDetails(remainingDetails);
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
