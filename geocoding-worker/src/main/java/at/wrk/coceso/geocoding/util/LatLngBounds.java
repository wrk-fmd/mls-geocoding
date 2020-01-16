package at.wrk.coceso.geocoding.util;

import at.wrk.coceso.geocoding.api.dto.LatLng;

/**
 * A class for handling WGS84 coordinate bounds
 */
public class LatLngBounds {

    private final LatLng sw, ne;

    /**
     * Create an instance with the given coordinates
     *
     * @param sw South-west boundary
     * @param ne North-east boundary
     */
    public LatLngBounds(LatLng sw, LatLng ne) {
        this.sw = sw;
        this.ne = ne;
    }

    /**
     * Check if the given coordinates are within the bounds
     *
     * @param coordinates The coordinates to check
     * @return True iff within the boundaries, not considering wrapping at 180°
     */
    public boolean contains(LatLng coordinates) {
        return coordinates != null && coordinates.getLat() >= sw.getLat() && coordinates.getLat() <= ne.getLat()
            && coordinates.getLng() >= sw.getLng() && coordinates.getLng() <= ne.getLng();
    }

    @Override
    public String toString() {
        return String.format("(%s),(%s)", sw, ne);
    }

}
