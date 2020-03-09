package at.wrk.fmd.mls.geocoding.util;

import at.wrk.fmd.mls.geocoding.api.dto.Address;
import at.wrk.fmd.mls.geocoding.api.dto.Address.Number;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.Objects;

public class AddressMatcher {

    private static final double MINIMUM_LEVENSHTEIN_TO_LENGTH_RATION_TO_MATCH = 0.2;

    /**
     * Non-symmetrical check if found address is a match
     *
     * @param foundAddress The found address by the search request (search hit)
     * @param searchInputAddress The address searched for (search input)
     * @param exactNumberMatchRequired Search for an exact match of numbers
     * @return True iff post code of b matches (if given), street is a close match (as given by {@link
     * #isStreetMatchingByLevenshtein}) and number
     * of a matches the one of b exactly/is contained within range given in b
     */
    public static boolean isFoundAddressMatching(final Address foundAddress, final Address searchInputAddress,
            final boolean exactNumberMatchRequired) {
        if (foundAddress == null || searchInputAddress == null) {
            return false;
        }

        if (!isPostCodeMatching(foundAddress, searchInputAddress)) {
            return false;
        }
        if (exactNumberMatchRequired && !numbersMatch(foundAddress.getNumber(), searchInputAddress.getNumber())) {
            return false;
        }
        if (!exactNumberMatchRequired && !numberBlockContains(foundAddress.getNumber(), searchInputAddress.getNumber())) {
            return false;
        }

        return isStreetMatchingByLevenshtein(foundAddress, searchInputAddress);
    }

    private static boolean isPostCodeMatching(final Address foundAddress, final Address searchInputAddress) {
        return searchInputAddress.getPostCode() == null || Objects.equals(foundAddress.getPostCode(), searchInputAddress.getPostCode());
    }

    /**
     * Check if both streetnames match
     *
     * @param leftAddress Left address to compare.
     * @param rightAddress Right address to compare.
     * @return True iff both streets are null or the Levenshtein distance divided by the length is &lt;= 0.2
     */
    public static boolean isStreetMatchingByLevenshtein(final Address leftAddress, final Address rightAddress) {
        if (leftAddress.getStreet() == null && rightAddress.getStreet() == null) {
            // Both null is a match
            return true;
        }
        if (leftAddress.getStreet() == null || rightAddress.getStreet() == null) {
            // Only one null is not a match
            return false;
        }

        final int levenshtein = LevenshteinDistance.getDefaultInstance().apply(leftAddress.getStreet(), rightAddress.getStreet());
        final int levenshteinToLengthRatio = levenshtein / (rightAddress.getStreet().length() + 1);
        return levenshteinToLengthRatio <= MINIMUM_LEVENSHTEIN_TO_LENGTH_RATION_TO_MATCH;
    }

    /**
     * Check if found address number is an exact match.
     *
     * @param foundNumber The found number
     * @param searchInputNumber The number searched for
     * @return true iff the number matches.
     */
    private static boolean numbersMatch(final Number foundNumber, final Number searchInputNumber) {
        if (foundNumber == null && searchInputNumber == null) {
            return true;
        }
        if (foundNumber == null || searchInputNumber == null) {
            return false;
        }
        return (Objects.equals(foundNumber.getFrom(), searchInputNumber.getFrom())
                && Objects.equals(foundNumber.getTo(), searchInputNumber.getTo())
                && Objects.equals(foundNumber.getLetter(), searchInputNumber.getLetter())
                && Objects.equals(foundNumber.getBlock(), searchInputNumber.getBlock()));
    }

    /**
     * Non-symmetrical check if found number contains the number searched for
     *
     * @param foundNumber The found number
     * @param searchInputNumber The number searched for
     * @return True iff the found number block contains the number searched for
     */
    private static boolean numberBlockContains(final Number foundNumber, final Number searchInputNumber) {
        if (foundNumber == null && searchInputNumber == null) {
            // Both null: match
            return true;
        }
        if (foundNumber == null || searchInputNumber == null) {
            // Exactly one null: no match
            return false;
        }

        if (foundNumber.getFrom() == null && searchInputNumber.getFrom() == null) {
            // No number on both sides
            return true;
        }
        if (foundNumber.getFrom() == null || searchInputNumber.getFrom() == null) {
            // No number on one side
            return false;
        }

        if (foundNumber.getTo() == null &&
                (searchInputNumber.getTo() != null || !Objects.equals(foundNumber.getFrom(), searchInputNumber.getFrom()))) {
            // Result is not a concatenated number, start numbers are not equal
            return false;
        }

        if (foundNumber.getTo() != null) {
            // Result is concatenated
            if (searchInputNumber.getFrom() < foundNumber.getFrom() || searchInputNumber.getFrom() > foundNumber.getTo()
                    || searchInputNumber.getFrom() % 2 != foundNumber.getFrom() % 2) {
                // Start number not in result interval
                return false;
            }
            if (searchInputNumber.getTo() != null && searchInputNumber.getTo() > foundNumber.getTo()) {
                // End number not in interval
                return false;
            }
        }

        if (foundNumber.getLetter() != null && !Objects.equals(foundNumber.getLetter(), searchInputNumber.getLetter())) {
            // Found a not matching letter
            return false;
        }

        // Not looking for a block or blocks are equal
        return searchInputNumber.getBlock() == null || Objects.equals(foundNumber.getBlock(), searchInputNumber.getBlock());
    }
}
