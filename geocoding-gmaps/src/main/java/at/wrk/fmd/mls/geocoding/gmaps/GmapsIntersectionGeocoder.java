package at.wrk.fmd.mls.geocoding.gmaps;

import at.wrk.fmd.mls.geocoding.api.dto.Address;
import at.wrk.fmd.mls.geocoding.api.dto.AddressResult;
import at.wrk.fmd.mls.geocoding.api.dto.LatLng;
import com.google.maps.GeoApiContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

@Component
@ConditionalOnProperty(prefix = "application.geocoder", name = "type", havingValue = "intersection")
public class GmapsIntersectionGeocoder extends AbstractGmapsGeocoder {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    public GmapsIntersectionGeocoder(GeoApiContext context) {
        super(context);
    }

    @Override
    public String buildQueryString(final Address address) {
        if (address.getStreet() == null || address.getIntersection() == null) {
            LOG.trace("Address does not have a street or intersection set: '{}'. Cannot build query string.", address);
            return null;
        }

        String query = String.format("%s and %s", address.getStreet(), address.getIntersection());
        if (address.getCity() != null) {
            query += ", " + address.getCity();
        }

        return query;
    }

    @Override
    public AddressResult reverse(LatLng coordinates) {
        LOG.trace("Reverse geocoding is not supported by GmapsIntersectionGeocoder.");
        return null;
    }
}
