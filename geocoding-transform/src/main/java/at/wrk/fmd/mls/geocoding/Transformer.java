package at.wrk.fmd.mls.geocoding;

import at.wrk.fmd.mls.geocoding.api.dto.GeocodingResult;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.Point;

import java.io.File;
import java.io.IOException;

public class Transformer {

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        for (String path : args) {
            GeocodingResult[] results = mapper.readValue(new File(path), GeocodingResult[].class);

            FeatureCollection collection = new GeoToolCollection("Generated from " + args[0]);
            for (GeocodingResult result : results) {
                Feature feature = new Feature();
                feature.setProperty("text", result.getText());

                if (result.getCoordinates() == null) {
                    System.out.println("[WARN] Entry without coordinates: " + result.getText().replace("\n", ", "));
                } else {
                    feature.setGeometry(new Point(result.getCoordinates().getLng(), result.getCoordinates().getLat()));
                }
                collection.add(feature);
            }

            mapper.writeValue(new File(path.replace(".json", "_transformed.json")), collection);
        }
    }

    @JsonTypeName("FeatureCollection")
    private static class GeoToolCollection extends FeatureCollection {

        private final String name;

        private GeoToolCollection(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
