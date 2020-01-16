package at.wrk.coceso.geocoding.poi;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties("application.poi")
public class PoiProperties {

    private Map<Resource, String> sources;

    public Map<Resource, String> getSources() {
        return sources;
    }

    public void setSources(Map<Resource, String> sources) {
        this.sources = sources;
    }
}
