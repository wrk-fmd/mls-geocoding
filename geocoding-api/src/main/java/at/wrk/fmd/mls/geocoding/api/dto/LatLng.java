package at.wrk.fmd.mls.geocoding.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A class for handling WGS84 coordinates
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE) // For Jackson
@AllArgsConstructor
public class LatLng {

    private double lat, lng;

    @Override
    public String toString() {
        return String.format("%f,%f", lat, lng);
    }
}
