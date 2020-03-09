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
     * The priority order of the geocoder (lower is called earlier, first geocoder must have priority {@code null})
     */
    private Integer priority;

    /**
     * Whether the request should be re-queued for the next geocoder if resolving failed
     */
    private boolean requeue;
}
