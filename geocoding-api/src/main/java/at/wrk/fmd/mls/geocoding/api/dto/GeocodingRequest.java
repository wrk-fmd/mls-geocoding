package at.wrk.fmd.mls.geocoding.api.dto;

import static java.util.Objects.requireNonNull;

public class GeocodingRequest {

    private String text;

    // Required for Jackson
    @SuppressWarnings("unused")
    private GeocodingRequest() {
    }

    public GeocodingRequest(String text) {
        this.text = requireNonNull(text, "The text for the geocoding request must not be null");
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }
}
