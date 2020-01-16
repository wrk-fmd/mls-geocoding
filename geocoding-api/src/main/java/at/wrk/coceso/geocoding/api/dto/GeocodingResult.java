package at.wrk.coceso.geocoding.api.dto;

import static java.util.Objects.requireNonNull;

import lombok.Getter;

@Getter
public class GeocodingResult {

    private String text;
    private LatLng coordinates;

    protected GeocodingResult() {
    }

    public GeocodingResult(String text, LatLng coordinates) {
        this.text = requireNonNull(text, "The text for the geocoding result must not be null");
        this.coordinates = requireNonNull(coordinates, "The coordinates for the geocoding result must not be null");
    }

    @Override
    public String toString() {
        return String.format("%s: %s", text, coordinates);
    }
}
