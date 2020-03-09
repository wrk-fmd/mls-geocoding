package at.wrk.fmd.mls.geocoding.gmaps;

import at.wrk.fmd.mls.geocoding.api.dto.Address;
import at.wrk.fmd.mls.geocoding.api.dto.Address.Number;
import at.wrk.fmd.mls.geocoding.api.dto.AddressResult;
import at.wrk.fmd.mls.geocoding.api.dto.GeocodingRequest;
import at.wrk.fmd.mls.geocoding.api.dto.LatLng;
import at.wrk.fmd.mls.geocoding.service.Geocoder;
import at.wrk.fmd.mls.geocoding.util.AddressParser;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.GeocodingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public abstract class AbstractGmapsGeocoder implements Geocoder {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final GeoApiContext context;

    protected AbstractGmapsGeocoder(GeoApiContext context) {
        this.context = context;
    }

    @Override
    public AddressResult geocode(GeocodingRequest request) {
        Address address = AddressParser.parseFromString(request.getText());
        String query = buildQueryString(address);
        if (query == null) {
            LOG.trace("Cannot query null-query. Address input was: '{}'.", address);
            return null;
        }

        LOG.debug("Querying Google Maps for '{}'", query);
        try {
            GeocodingResult[] results = GeocodingApi.geocode(context, query).await();
            if (results.length > 0) {
                LatLng coordinates = new LatLng(results[0].geometry.location.lat, results[0].geometry.location.lng);
                return new AddressResult(address, request.getText(), coordinates);
            }
        } catch (Exception ex) {
            LOG.info("Error getting coordinates for address query: " + query, ex);
        }

        return null;
    }

    @Override
    public AddressResult reverse(LatLng coordinates) {
        LOG.debug("Reverse geocoding with Google Maps for ({})", coordinates);
        try {
            var gmapsLatLng = new com.google.maps.model.LatLng(coordinates.getLat(), coordinates.getLng());
            GeocodingResult[] results = GeocodingApi.reverseGeocode(context, gmapsLatLng).await();
            if (results.length > 0) {
                Address address = buildAddress(results[0].addressComponents);
                LOG.debug("Found address '{}' with Google Maps", address);
                return new AddressResult(address, coordinates);
            }
        } catch (Exception ex) {
            LOG.info("Error getting address for '{}'", coordinates, ex);
        }

        return null;
    }

    private Address buildAddress(AddressComponent[] components) {
        String street = null, city = null;
        Integer postCode = null;
        Number number = null;

        for (AddressComponent component : components) {
            for (AddressComponentType type : component.types) {
                switch (type) {
                    case ROUTE:
                        if (street == null) {
                            street = component.longName.trim();
                        }
                        break;
                    case LOCALITY:
                        if (city == null) {
                            city = component.longName.trim();
                        }
                        break;
                    case POSTAL_CODE:
                        if (postCode == null) {
                            try {
                                postCode = Integer.parseInt(component.longName.trim());
                            } catch (NumberFormatException e) {
                                // Ignore
                            }
                        }
                        break;
                    case STREET_NUMBER:
                        if (number == null || number.getFrom() == null) {
                            number = AddressParser.parseNumber(component.longName.trim());
                        }
                        break;
                }
            }
        }

        return Address.builder()
                .street(street)
                .number(number)
                .postCode(postCode)
                .city(city)
                .build();
    }

    protected abstract String buildQueryString(Address address);
}
