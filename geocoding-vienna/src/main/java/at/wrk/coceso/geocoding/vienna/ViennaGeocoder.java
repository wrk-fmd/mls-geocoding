package at.wrk.coceso.geocoding.vienna;

import at.wrk.coceso.geocoding.api.dto.Address;
import at.wrk.coceso.geocoding.api.dto.AddressResult;
import at.wrk.coceso.geocoding.api.dto.GeocodingRequest;
import at.wrk.coceso.geocoding.api.dto.LatLng;
import at.wrk.coceso.geocoding.service.Geocoder;
import at.wrk.coceso.geocoding.util.AddressMatcher;
import at.wrk.coceso.geocoding.util.AddressParser;
import at.wrk.coceso.geocoding.util.LatLngBounds;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
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
    public AddressResult geocode(GeocodingRequest request) {
        Address address = AddressParser.parseFromString(request.getText());
        if (address == null) {
            return null;
        }

        if (address.getStreet() == null) {
            LOG.trace("Vienna Geocoder is not applicable if no street is set. Address: {}", address);
            return null;
        }

        if (address.getPostCode() != null && (address.getPostCode() < 1000 || address.getPostCode() > 1300)) {
            LOG.trace("Vienna Geocoder is not applicable if the postal code is outside of Vienna. Address: {}",
                    address);
            return null;
        }

        if (address.getCity() != null && !address.getCity().toLowerCase().startsWith("wien")) {
            LOG.trace("Vienna Geocoder is not applicable if the city is not Vienna (=\"Wien\"). Address: {}", address);
            return null;
        }

        String query = buildQueryString(address);
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

        Feature feature = getMatch(address, result.getFeatures());
        if (feature == null || !(feature.getGeometry() instanceof Point)) {
            return null;
        }

        LngLatAlt coordinates = ((Point) feature.getGeometry()).getCoordinates();
        return new AddressResult(address, request.getText(), new LatLng(coordinates.getLatitude(), coordinates.getLongitude()));
    }

    @Override
    public AddressResult reverse(LatLng coordinates) {
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
        Address address = buildAddress(features.get(0));
        double dist = features.get(0).getProperty("Distance");
        LOG.debug("Found address '{}' {} meters away with data.wien.gv.at", address, dist);
        return new AddressResult(address, coordinates);
    }

    private String buildQueryString(Address address) {
        String query = address.getStreet();
        Address.Number number = address.getNumber();
        if (number != null) {
            if (number.getFrom() != null) {
                query += " " + number.getFrom();
                if (number.getTo() != null) {
                    query += "-" + number.getTo();
                } else if (number.getLetter() != null) {
                    query += number.getLetter();
                }
                if (number.getBlock() != null) {
                    query += "/" + number.getBlock();
                }
            }
        }

        return query;
    }

    private Feature getMatch(Address address, List<Feature> features) {
        if (features.size() > 1) {
            features.sort(Comparator.nullsLast(Comparator.comparing(f -> f.getProperty("Ranking"))));

            for (Feature feature : features) {
                // First run: Look for exact match
                if (AddressMatcher.isFoundAddressMatching(buildAddress(feature), address, true)) {
                    LOG.debug("Found an exactly matching address in the result set: {}.", feature);
                    return feature;
                }
            }

            for (Feature feature : features) {
                // Second run: Look for bigger addresses containing the requested
                if (AddressMatcher.isFoundAddressMatching(buildAddress(feature), address, false)) {
                    LOG.debug("Found a matching address in the result set, with a different number: {}.", feature);
                    return feature;
                }
            }
        }

        // Only one entry or no match found, use lowest ranking
        LOG.trace("Check if single entry in returned information is matching by levenshtein distance.");
        Feature feature = features.get(0);
        return AddressMatcher.isStreetMatchingByLevenshtein(buildAddress(feature), address) ? feature : null;
    }

    private Address buildAddress(Feature feature) {
        return Address.builder()
                .street(feature.getProperty("StreetName"))
                .number(AddressParser.parseNumber(feature.getProperty("StreetNumber")))
                .postCode(AddressParser.parseInt(feature.getProperty("PostalCode")))
                .city(feature.getProperty("Municipality"))
                .build();
    }
}
