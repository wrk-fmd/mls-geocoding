package at.wrk.fmd.mls.geocoding.util;

import at.wrk.fmd.mls.geocoding.api.dto.Address;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.Objects;

public class AddressMatcher {

    private static final double MINIMUM_LEVENSHTEIN_TO_LENGTH_RATION_TO_MATCH = 0.2;

    /**
     * Non-symmetrical check if found address is a match
     *
     * @param foundAddress The found address by the search request (search hit)
     * @param searchInputAddress The address searched for (search input)
     * @return True iff post code of b matches (if given), street is a close match (as given by {@link
     * #isStreetMatchingByLevenshtein}) and number
     * of a matches the one of b exactly/is contained within range given in b
     */
    public static boolean isFoundAddressMatching(final Address foundAddress, final Address searchInputAddress) {
        return foundAddress != null && searchInputAddress != null
                && isPostCodeMatching(foundAddress, searchInputAddress)
                && isStreetMatchingByLevenshtein(foundAddress, searchInputAddress);
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
}
