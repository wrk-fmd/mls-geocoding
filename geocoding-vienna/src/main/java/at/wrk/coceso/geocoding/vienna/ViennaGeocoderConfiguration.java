package at.wrk.coceso.geocoding.vienna;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ViennaGeocoderConfiguration {

    /**
     * Create a RestTemplate for the Vienna OGD AddressService
     */
    @Bean
    public RestTemplate ogdTemplate(RestTemplateBuilder builder) {
        return builder.rootUri("https://data.wien.gv.at/daten/OGDAddressService.svc").build();
    }
}
