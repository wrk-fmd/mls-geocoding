package at.wrk.fmd.mls.geocoding;

import at.wrk.fmd.mls.geocoding.api.dto.LatLng;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
class PoiDto {

    private String text;
    private LatLng coordinates;
}
