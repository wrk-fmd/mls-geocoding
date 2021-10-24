package at.wrk.fmd.mls.geocoding.service;

import at.wrk.fmd.mls.geocoding.api.dto.LatLng;
import at.wrk.fmd.mls.geocoding.api.dto.PointDto;

import java.util.List;

/**
 * Interface for an object capable of geocoding and reverse geocoding
 */
public interface Geocoder {

    /**
     * Geocode the given data to coordinates
     *
     * @param search The search data, must not be null
     * @return The coordinates, or null if none were found
     */
    default List<PointDto> geocode(PointDto search) {
        return null;
    }

    /**
     * Reverse geocode the given coordinates
     *
     * @param coordinates The coordinates to reverse geocode, must not be null
     * @return The result, or null if none was found
     */
    default PointDto reverse(LatLng coordinates) {
        return null;
    }
}
