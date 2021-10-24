package at.wrk.fmd.mls.geocoding.gmaps;

import static java.util.Objects.requireNonNull;

import com.google.maps.GeoApiContext;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@ConfigurationProperties("application.gmaps")
public class GmapsConfiguration {

    /**
     * The Google Maps API key to use
     */
    private String apiKey;

    /**
     * The type of requests this Geocoder handles (regular addresses or intersections)
     */
    private Type type;

    @Bean
    public GeoApiContext geoApiContext() {
        return new GeoApiContext.Builder()
                .apiKey(requireNonNull(apiKey, "Google Maps API key must not be null"))
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .writeTimeout(2, TimeUnit.SECONDS)
                .retryTimeout(3, TimeUnit.SECONDS)
                .build();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public enum Type {
        regular, intersection;
    }
}
