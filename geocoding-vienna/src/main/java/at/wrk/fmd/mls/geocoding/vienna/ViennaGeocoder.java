package at.wrk.fmd.mls.geocoding.vienna;

import at.wrk.fmd.mls.geocoding.api.dto.Address;
import at.wrk.fmd.mls.geocoding.api.dto.Address.AddressBuilder;
import at.wrk.fmd.mls.geocoding.api.dto.LatLng;
import at.wrk.fmd.mls.geocoding.api.dto.PointDto;
import at.wrk.fmd.mls.geocoding.service.Geocoder;
import at.wrk.fmd.mls.geocoding.util.AddressMatcher;
import at.wrk.fmd.mls.geocoding.util.AddressParser;
import at.wrk.fmd.mls.geocoding.util.LatLngBounds;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ViennaGeocoder implements Geocoder {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String GEOCODE_URL = "/GetAddressInfo?CRS=EPSG:4326&Address={query}";
    private static final String REVERSE_URL = "/ReverseGeocode?CRS=EPSG:4326&type=A3:8012&location={lng},{lat}";
    private static final LatLngBounds BOUNDS = new LatLngBounds(
            new LatLng(48.1183, 16.1827),
            new LatLng(48.3231, 16.5787)
    );

    private final RestTemplate restTemplate;

    @Autowired
    public ViennaGeocoder(final RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
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
            LOG.trace("Point does not represent an address. Point: {}", parsed);
            return null;
        }

        if (address.getStreet() == null) {
            LOG.trace("Vienna Geocoder is not applicable if no street is set. Address: {}", address);
            return null;
        }

        if (address.getIntersection() != null) {
            LOG.trace("Vienna Geocoder is not applicable if intersection is queried. Address: {}", address);
            return null;
        }

        if (address.getPostCode() != null && (address.getPostCode() < 1000 || address.getPostCode() > 1300)) {
            LOG.trace("Vienna Geocoder is not applicable if the postal code is outside of Vienna. Address: {}", address);
            return null;
        }

        if (address.getCity() != null && !address.getCity().toLowerCase().startsWith("wien")) {
            LOG.trace("Vienna Geocoder is not applicable if the city is not Vienna (=\"Wien\"). Address: {}", address);
            return null;
        }

        String query = address.getStreet();
        if (address.getNumber() != null) {
            query += " " + address.getNumber();
        }
        if (address.getBlock() != null) {
            query += " /" + address.getBlock();
        }

        FeatureCollection result;
        try {
            LOG.trace("Vienna Geocoder requests coordinates of address from Vienna OGDAddressService.");
            result = restTemplate.getForObject(GEOCODE_URL, FeatureCollection.class, query);
        } catch (RestClientException e) {
            LOG.info("Failed to get geocode data from OGDAddressService for address: {}. Error: {}", address, e.getMessage());
            LOG.debug("Underlying exception:", e);
            return null;
        }

        if (result == null || result.getFeatures().isEmpty()) {
            LOG.trace("Got no usable information in the Vienna Geocoder response.");
            return null;
        }

        return result.getFeatures().stream()
                .sorted(Comparator.nullsLast(Comparator.comparing(f -> f.getProperty("Ranking"))))
                .map(feature -> buildPoint(parsed, feature))
                .filter(point -> matchesRequest(parsed, point))
                .collect(Collectors.toList());
    }

    @Override
    public PointDto reverse(LatLng coordinates) {
        if (!BOUNDS.contains(coordinates)) {
            return null;
        }

        FeatureCollection result;
        try {
            LOG.trace("Perform reverse lookup for address on data.wien.gv.at for coordinates: {}", coordinates);
            result = restTemplate.getForObject(REVERSE_URL, FeatureCollection.class, coordinates.getLng(), coordinates.getLat());
        } catch (RestClientException ex) {
            LOG.info("Error getting address for '{}'", coordinates, ex);
            return null;
        }

        if (result == null || result.getFeatures().isEmpty()) {
            return null;
        }

        List<Feature> features = result.getFeatures();
        features.sort(Comparator.nullsLast(Comparator.comparing(f -> f.getProperty("Distance"))));
        Address address = buildAddress(features.get(0), Address.builder());
        double dist = features.get(0).getProperty("Distance");
        LOG.debug("Found address '{}' {} meters away with data.wien.gv.at", address, dist);
        return PointDto.builder()
                .address(address)
                .coordinates(coordinates)
                .build();
    }

    private Address buildAddress(Feature feature, AddressBuilder builder) {
        builder
                .street(feature.getProperty("StreetName"))
                .postCode(AddressParser.parseInt(feature.getProperty("PostalCode")))
                .city(feature.getProperty("Municipality"));

        String numberString = feature.getProperty("StreetNumber");
        if (numberString != null) {
            String[] numberParts = numberString.split("/");
            if (!numberParts[0].isEmpty()) {
                builder.number(numberParts[0]);
            }
            if (numberParts.length > 1) {
                builder.block(numberParts[1]);
            }
        }

        return builder.build();
    }

    private PointDto buildPoint(PointDto request, Feature feature) {
        if (feature == null) {
            return null;
        }

        GeoJsonObject geometry = feature.getGeometry();
        if (!(geometry instanceof Point)) {
            return null;
        }

        LngLatAlt coordinates = ((Point) geometry).getCoordinates();
        LatLng latlng = new LatLng(coordinates.getLatitude(), coordinates.getLongitude());
        Address address = buildAddress(feature, request.getAddress().toBuilder());
        return request.withAddress(address).withCoordinates(latlng);
    }

    private boolean matchesRequest(PointDto request, PointDto result) {
        return result != null && AddressMatcher.isFoundAddressMatching(result.getAddress(), request.getAddress());
    }
}
