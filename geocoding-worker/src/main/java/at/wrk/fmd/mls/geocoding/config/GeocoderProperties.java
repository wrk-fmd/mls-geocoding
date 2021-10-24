package at.wrk.fmd.mls.geocoding.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("application.geocoder")
public class GeocoderProperties {

    /**
     * The name of the geocoder
     */
    private String name;

    /**
     * The priority of the geocoder
     */
    private int priority;
}
