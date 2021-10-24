package at.wrk.fmd.mls.geocoding.gmaps;

import static java.util.Objects.requireNonNull;

import at.wrk.fmd.mls.geocoding.api.dto.Address;
import at.wrk.fmd.mls.geocoding.api.dto.Address.AddressBuilder;
import at.wrk.fmd.mls.geocoding.api.dto.LatLng;
import at.wrk.fmd.mls.geocoding.api.dto.PointDto;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class AbstractGmapsGeocoder implements Geocoder {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final GeoApiContext context;

    protected AbstractGmapsGeocoder(GeoApiContext context) {
        this.context = requireNonNull(context, "GeoApiContext must not be null");
    }

    @Override
    public List<PointDto> geocode(PointDto request) {
        if (request.getCoordinates() != null || request.getPoi() != null) {
            // We already have coordinates or a POI, don't need to look anymore
            return null;
        }

        PointDto parsed = AddressParser.parseAddress(request);
        Address address = parsed.getAddress();
        if (address == null) {
            // No address parsed from given data
            return null;
        }

        String query = buildQueryString(address);
        if (query == null) {
            LOG.trace("Cannot query null-query. Address input was: '{}'.", address);
            return null;
        }

        LOG.debug("Querying Google Maps for '{}'", query);
        try {
            GeocodingResult[] results = GeocodingApi.geocode(context, query).await();
            return Arrays.stream(results)
                    .map(feature -> buildResult(parsed, feature))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            LOG.info("Error getting coordinates for address query: " + query, ex);
        }

        return null;
    }

    @Override
    public PointDto reverse(LatLng coordinates) {
        LOG.debug("Reverse geocoding with Google Maps for ({})", coordinates);
        try {
            var gmapsLatLng = new com.google.maps.model.LatLng(coordinates.getLat(), coordinates.getLng());
            GeocodingResult[] results = GeocodingApi.reverseGeocode(context, gmapsLatLng).await();
            if (results.length > 0) {
                Address address = buildAddress(results[0].addressComponents, Address.builder());
                LOG.debug("Found address '{}' with Google Maps", address);
                return PointDto.builder()
                        .address(address)
                        .coordinates(coordinates)
                        .build();
            }
        } catch (Exception ex) {
            LOG.info("Error getting address for '{}'", coordinates, ex);
        }

        return null;
    }

    private Address buildAddress(AddressComponent[] components, AddressBuilder builder) {
        for (AddressComponent component : components) {
            for (AddressComponentType type : component.types) {
                switch (type) {
                    case ROUTE:
                        builder.street(component.longName.trim());
                        break;
                    case LOCALITY:
                        builder.city(component.longName.trim());
                        break;
                    case POSTAL_CODE:
                        try {
                            builder.postCode(Integer.parseInt(component.longName.trim()));
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                        break;
                    case STREET_NUMBER:
                        builder.number(component.longName.trim());
                        break;
                }
            }
        }

        return builder.build();
    }

    private PointDto buildResult(PointDto request, GeocodingResult result) {
        if (result == null) {
            // No result at all
            return null;
        }

        if (result.geometry == null) {
            // No coordinates for result, ignore
            return null;
        }

        LatLng latlng = new LatLng(result.geometry.location.lat, result.geometry.location.lng);
        Address address = buildAddress(result.addressComponents, request.getAddress().toBuilder());
        return request.withAddress(address).withCoordinates(latlng);
    }

    protected abstract String buildQueryString(Address address);

    protected String appendCityQuery(String query, Address address) {
        if (address.getPostCode() != null && address.getCity() != null) {
            return query + ", " + address.getPostCode() + " " + address.getCity();
        }
        if (address.getPostCode() != null) {
            return query + ", " + address.getPostCode();
        }
        if (address.getCity() != null) {
            return query + ", " + address.getCity();
        }
        return query;
    }
}
