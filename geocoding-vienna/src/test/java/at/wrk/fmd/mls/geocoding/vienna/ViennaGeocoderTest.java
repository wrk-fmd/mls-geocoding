package at.wrk.fmd.mls.geocoding.vienna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.wrk.fmd.mls.geocoding.api.dto.AddressResult;
import at.wrk.fmd.mls.geocoding.api.dto.GeocodingRequest;
import at.wrk.fmd.mls.geocoding.api.dto.GeocodingResult;
import at.wrk.fmd.mls.geocoding.api.dto.LatLng;
import at.wrk.fmd.mls.geocoding.config.MessageBrokerConfiguration;
import at.wrk.fmd.mls.geocoding.service.GeocodingRequestListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

// TODO Just inject what is really needed
@ExtendWith(SpringExtension.class)
@SpringBootTest
@MockBean({GeocodingRequestListener.class, MessageBrokerConfiguration.class})
public class ViennaGeocoderTest {

    @Autowired
    private ViennaGeocoder geocoder;

    @Test
    public void addressWithCity() {
        GeocodingRequest request = createRequest("Neubaugasse", "7/3", 1070, "Wien");
        GeocodingResult result = geocoder.geocode(request);
        assertCoordinates(new LatLng(48.199, 16.349), result.getCoordinates());
    }

    @Test
    public void addressWithoutPostalCode() {
        GeocodingRequest request = createRequest("Neubaugasse", "7/3", null, null);
        GeocodingResult result = geocoder.geocode(request);
        assertCoordinates(new LatLng(48.199, 16.349), result.getCoordinates());
    }

    @Test
    public void addressWithPostalCodeOutsideOfVienna_noResult() {
        GeocodingRequest request = createRequest("Mozartstraße", "1", 4020, null);
        GeocodingResult result = geocoder.geocode(request);
        assertNull(result);
    }

    @Test
    public void addressWithCityOutsideOfVienna_noResult() {
        GeocodingRequest request = createRequest("Mozartstraße", "1", null, "Linz");
        GeocodingResult result = geocoder.geocode(request);
        assertNull(result);
    }

    @Test
    public void invalidAddressInVienna_noResult() {
        GeocodingRequest request = createRequest("Invalid", "1", 1130, null);
        GeocodingResult result = geocoder.geocode(request);
        assertNull(result);
    }

    /**
     * Test of reverse method, of class ViennaGeocoder.
     */
    @Test
    public void testReverse() {
        AddressResult result;

        result = geocoder.reverse(new LatLng(48.202813, 16.342256));
        assertNotNull(result, "Expected a result at Kandlgasse, 1070");
        assertEquals("Kandlgasse", result.getAddress().getStreet());
        assertNotNull(result.getAddress().getPostCode(), "Expected a post code at Kandlgasse, 1070");
        assertEquals(1070, (int) result.getAddress().getPostCode());
        Integer number = result.getAddress().getNumber().getFrom();
        assertNotNull(number, "Expected a number at Kandlgasse, 1070");
        assertTrue(number >= 19 && number <= 24, "Expected number to be in range 19-24");

        result = geocoder.reverse(new LatLng(48.32, 16.19));
        assertNotNull(result, "Expected a result near the border of Vienna");
        assertNotNull(result.getAddress().getPostCode(), "Expected a post code for request outside of Vienna");
        assertEquals(1140, (int) result.getAddress().getPostCode());

        result = geocoder.reverse(new LatLng(48.18, 16.10));
        assertNull(result, "Expected no result for request at Pressbaum");
    }

    private GeocodingRequest createRequest(final String street, final String number, final Integer postCode, final String city) {
        String request = String.format("%s %s\n", street, number);
        if (postCode != null) {
            request += postCode + " ";
        }
        if (city != null) {
            request += city;
        }
        return new GeocodingRequest(request.trim());
    }

    private void assertCoordinates(final LatLng expected, final LatLng result) {
        assertEquals(expected.getLat(), result.getLat(), 0.0005);
        assertEquals(expected.getLng(), result.getLng(), 0.0005);
    }
}
