package at.wrk.coceso.geocoding.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A class for handling WGS84 coordinates
 */
@Getter
@NoArgsConstructor // For Jackson
@AllArgsConstructor
public class LatLng {

    private double lat, lng;

    @Override
    public String toString() {
        return String.format("%f,%f", lat, lng);
    }
}
