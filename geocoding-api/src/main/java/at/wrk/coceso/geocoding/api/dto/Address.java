package at.wrk.coceso.geocoding.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.StringJoiner;

@Getter
@Builder
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE) // For Jackson
@AllArgsConstructor(access = AccessLevel.PRIVATE) // For Builder
public class Address {

    private String title;
    private String street;
    private String intersection;
    private Number number;
    private Integer postCode;
    private String city;
    private String additional;

    public String asText() {
        StringJoiner lines = new StringJoiner("\n");

        if (title != null) {
            lines.add(title);
        }

        if (street != null) {
            String line = street;
            if (number != null && number.from != null) {
                line += " " + number.asText();
            }
            lines.add(line);
        }

        if (postCode != null && city != null) {
            lines.add(postCode + " " + city);
        } else if (postCode != null) {
            lines.add(postCode.toString());
        } else if (city != null) {
            lines.add(city);
        }

        if (additional != null) {
            lines.add(additional);
        }

        return lines.toString();
    }

    @Getter
    @Builder
    @ToString
    @NoArgsConstructor(access = AccessLevel.PRIVATE) // For Jackson
    @AllArgsConstructor(access = AccessLevel.PRIVATE) // For Builder
    public static class Number {

        private Integer from;
        private Integer to;
        private String letter;
        private String block;
        private String details;

        public String asText() {
            if (from == null) {
                return "";
            }

            String text = from.toString();
            if (to != null) {
                text += "-" + to;
            } else if (letter != null) {
                text += letter;
            }

            if (block != null && details != null) {
                text += "/" + block + "/" + details;
            } else if (block != null) {
                text += "/" + block;
            } else if (details != null) {
                text += "//" + details;
            }

            return text;
        }
    }
}
