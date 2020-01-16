package at.wrk.coceso.geocoding.gmaps;

import at.wrk.coceso.geocoding.api.dto.Address;
import at.wrk.coceso.geocoding.api.dto.Address.Number;
import com.google.maps.GeoApiContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

@Component
@ConditionalOnProperty(prefix = "application.geocoder", name = "type", havingValue = "default", matchIfMissing = true)
public class GmapsGeocoder extends AbstractGmapsGeocoder {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    public GmapsGeocoder(GeoApiContext context) {
        super(context);
    }

    @Override
    protected String buildQueryString(Address address) {
        if (address.getStreet() == null) {
            LOG.trace("Address '{}' does not have an address set. Cannot build query for geocode.", address);
            return null;
        }

        String query = address.getStreet();
        Number number = address.getNumber();
        if (number != null) {
            if (number.getFrom() != null) {
                query += " " + number.getFrom();
                if (number.getTo() != null) {
                    query += "-" + number.getTo();
                } else if (number.getLetter() != null) {
                    query += number.getLetter();
                }
            }
        }

        if (address.getCity() != null) {
            query += ", " + address.getCity();
        }

        return query;
    }
}
