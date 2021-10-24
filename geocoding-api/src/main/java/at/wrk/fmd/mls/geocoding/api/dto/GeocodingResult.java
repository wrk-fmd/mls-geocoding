package at.wrk.fmd.mls.geocoding.api.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public class GeocodingResult {

    private final PointDto data;
    private final String source;
    private final int priority;
}
