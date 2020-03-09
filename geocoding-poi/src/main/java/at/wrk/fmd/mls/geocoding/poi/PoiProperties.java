package at.wrk.fmd.mls.geocoding.poi;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties("application.poi")
public class PoiProperties {

    /**
     * A map of JSON resources and the (optional) prefix that should be used
     */
    private Map<Resource, String> sources;

    public Map<Resource, String> getSources() {
        return sources;
    }

    public void setSources(Map<Resource, String> sources) {
        this.sources = sources;
    }
}
