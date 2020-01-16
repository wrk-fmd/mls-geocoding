package at.wrk.coceso.geocoding.api.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE) // For Jackson
public class AddressResult extends GeocodingResult {

    private Address address;

    public AddressResult(Address address, LatLng coordinates) {
        this(address, address.asText(), coordinates);
    }

    public AddressResult(Address address, String text, LatLng coordinates) {
        super(text, coordinates);
        this.address = address;
    }
}
