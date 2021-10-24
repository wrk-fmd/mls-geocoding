package at.wrk.fmd.mls.geocoding.gmaps;

import at.wrk.fmd.mls.geocoding.api.dto.Address;
import com.google.maps.GeoApiContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

@Component
@ConditionalOnProperty(prefix = "application.gmaps", name = "type", havingValue = "regular", matchIfMissing = true)
public class GmapsGeocoder extends AbstractGmapsGeocoder {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    public GmapsGeocoder(GeoApiContext context) {
        super(context);
    }

    @Override
    protected String buildQueryString(Address address) {
        if (address.getIntersection() != null) {
            LOG.trace("Address '{}' has an intersection set, which this service does not handle.", address);
            return null;
        }

        if (address.getStreet() == null) {
            LOG.trace("Address '{}' does not have a street set. Cannot build query for geocode.", address);
            return null;
        }

        String query = address.getStreet();
        if (address.getNumber() != null) {
            query += " " + address.getNumber();
        }
        return appendCityQuery(query, address);
    }
}
