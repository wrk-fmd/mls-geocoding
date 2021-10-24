package at.wrk.fmd.mls.geocoding.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE) // For Jackson
@AllArgsConstructor(access = AccessLevel.PRIVATE) // For Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Address {

    private String street;
    private String intersection;
    private String number;
    private String block;
    private String details;
    private Integer postCode;
    private String city;
}
