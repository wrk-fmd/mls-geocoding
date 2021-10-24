package at.wrk.fmd.mls.geocoding.poi;

import at.wrk.fmd.mls.geocoding.api.dto.LatLng;
import at.wrk.fmd.mls.geocoding.api.dto.PointDto;
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
import java.util.Collections;
import java.util.List;
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
    private final NavigableMap<String, PointDto> values;

    public PoiGeocoder(ObjectMapper mapper, PoiProperties properties) {
        this.mapper = mapper;
        values = properties.getSources().entrySet().stream()
                .flatMap(e -> loadFeatureCollection(e.getKey(), e.getValue()))
                .collect(Collectors.toMap(r -> getKey(r.getPoi()), Function.identity(), (a, b) -> a, TreeMap::new));
        LOG.debug("Loaded {} POI entries", values.size());
    }

    private Stream<PointDto> loadFeatureCollection(Resource source, String prefix) {
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

    private PointDto getPoi(Feature feature, String prefix) {
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

        // TODO Parse address contained in POI text
        return PointDto.builder()
                .poi(prefix + text)
                .coordinates(new LatLng(coordinates.getLatitude(), coordinates.getLongitude()))
                .build();
    }

    private String getKey(String text) {
        return text.trim().toLowerCase().replace("\n", ", ");
    }

    @Override
    public List<PointDto> geocode(PointDto request) {
        if (request.getPoi() != null) {
            // Already have a POI entry, try to extend to full entry
            PointDto match = values.get(getKey(request.getPoi()));
            if (match == null) {
                return null;
            }

            PointDto result = match.withDetails(StringUtils.trimToNull(request.getDetails()));
            return Collections.singletonList(result);
        }

        if (request.getDetails() == null) {
            return null;
        }

        String queryString = StringUtils.trimToNull(getKey(request.getDetails()));
        if (queryString == null) {
            return null;
        }

        // TODO We probably want more sophisticated matching here, e.g. partial inner matches, separate line matching

        // Get everything that starts with the given query string
        // e.g., if query string is "foo", this will return everything in the range ["foo", "fop"[
//        char lastChar = queryString.charAt(queryString.length() - 1);
//        String queryEnd = queryString.substring(0, queryString.length() - 1) + (char) (lastChar + 1);
//        return new ArrayList<>(values.subMap(queryString, queryEnd).values());

        return values.entrySet().stream()
                .filter(e -> e.getKey().contains(queryString))
                .map(Entry::getValue)
                .collect(Collectors.toList());
    }

    // TODO Also allow reverse geocoding
}
