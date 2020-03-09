package at.wrk.fmd.mls.geocoding.poi;

import at.wrk.fmd.mls.geocoding.api.dto.GeocodingRequest;
import at.wrk.fmd.mls.geocoding.api.dto.GeocodingResult;
import at.wrk.fmd.mls.geocoding.api.dto.LatLng;
import at.wrk.fmd.mls.geocoding.service.Geocoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class PoiGeocoder implements Geocoder {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ObjectMapper mapper;
    private final NavigableMap<String, GeocodingResult> values;

    public PoiGeocoder(ObjectMapper mapper, PoiProperties properties) {
        this.mapper = mapper;
        values = properties.getSources().entrySet().stream()
                .flatMap(e -> loadFeatureCollection(e.getKey(), e.getValue()))
                .collect(Collectors.toMap(r -> getKey(r.getText()), Function.identity(), (a, b) -> a, TreeMap::new));
        LOG.debug("Loaded {} POI entries", values.size());
    }

    private Stream<GeocodingResult> loadFeatureCollection(Resource source, String prefix) {
        LOG.debug("Reading POI features from {}", source);

        try (InputStream inputStream = source.getInputStream()) {
            String fullPrefix = StringUtils.isBlank(prefix) ? "" : prefix.trim() + "/";
            FeatureCollection collection = mapper.readValue(inputStream, FeatureCollection.class);
            LOG.debug("Read FeatureCollection with {} features", collection.getFeatures().size());
            return collection.getFeatures().stream()
                    .map(f -> getPoi(f, fullPrefix))
                    .filter(Objects::nonNull);
        } catch (IOException ex) {
            LOG.error("Error reading POI data", ex);
            return Stream.empty();
        }
    }

    private GeocodingResult getPoi(Feature feature, String prefix) {
        GeoJsonObject geometry = feature.getGeometry();
        if (!(geometry instanceof Point)) {
            return null;
        }

        LngLatAlt coordinates = ((Point) geometry).getCoordinates();
        if (coordinates == null) {
            return null;
        }

        String text = feature.getProperty("text");
        if (text == null) {
            return null;
        }

        return new GeocodingResult(prefix + text, new LatLng(coordinates.getLatitude(), coordinates.getLongitude()));
    }

    private String getKey(String text) {
        return text.trim().toLowerCase().replace("\n", ", ");
    }

    @Override
    public GeocodingResult geocode(GeocodingRequest request) {
        if (StringUtils.isBlank(request.getText())) {
            return null;
        }

        String query = getKey(request.getText());

        GeocodingResult exact = values.get(query);
        if (exact != null) {
            return exact;
        }

        // Get the last entry before the queried one
        Entry<String, GeocodingResult> entry = values.floorEntry(query);
        return (entry != null && query.startsWith(entry.getKey())) ? entry.getValue() : null;
    }

    // TODO Also allow reverse geocoding
}
