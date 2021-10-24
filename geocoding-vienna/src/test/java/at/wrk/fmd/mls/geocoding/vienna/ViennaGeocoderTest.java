package at.wrk.fmd.mls.geocoding.vienna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.wrk.fmd.mls.geocoding.api.dto.LatLng;
import at.wrk.fmd.mls.geocoding.api.dto.PointDto;
import at.wrk.fmd.mls.geocoding.config.MessageBrokerConfiguration;
import at.wrk.fmd.mls.geocoding.service.GeocodingRequestListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

// TODO Just inject what is really needed
@ExtendWith(SpringExtension.class)
@SpringBootTest
@MockBean({GeocodingRequestListener.class, MessageBrokerConfiguration.class})
public class ViennaGeocoderTest {

    @Autowired
    private ViennaGeocoder geocoder;

    @Test
    public void addressWithCity() {
        PointDto request = createRequest("Neubaugasse", "7/3", 1070, "Wien");
        List<PointDto> result = geocoder.geocode(request);
        assertCoordinates(new LatLng(48.199, 16.349), result.get(0).getCoordinates());
    }

    @Test
    public void addressWithoutPostalCode() {
        PointDto request = createRequest("Neubaugasse", "7/3", null, null);
        List<PointDto> result = geocoder.geocode(request);
        assertCoordinates(new LatLng(48.199, 16.349), result.get(0).getCoordinates());
    }

    @Test
    public void addressWithPostalCodeOutsideOfVienna_noResult() {
        PointDto request = createRequest("Mozartstraße", "1", 4020, null);
        List<PointDto> result = geocoder.geocode(request);
        assertNull(result);
    }

    @Test
    public void addressWithCityOutsideOfVienna_noResult() {
        PointDto request = createRequest("Mozartstraße", "1", null, "Linz");
        List<PointDto> result = geocoder.geocode(request);
        assertNull(result);
    }

    @Test
    public void invalidAddressInVienna_noResult() {
        PointDto request = createRequest("Invalid", "1", 1130, null);
        List<PointDto> result = geocoder.geocode(request);
        assertTrue(result == null || result.isEmpty());
    }

    /**
     * Test of reverse method, of class ViennaGeocoder.
     */
    @Test
    public void testReverse() {
        PointDto result;

        result = geocoder.reverse(new LatLng(48.202813, 16.342256));
        assertNotNull(result, "Expected a result at Kandlgasse, 1070");
        assertEquals("Kandlgasse", result.getAddress().getStreet());
        assertNotNull(result.getAddress().getPostCode(), "Expected a post code at Kandlgasse, 1070");
        assertEquals(1070, (int) result.getAddress().getPostCode());
        String numberString = result.getAddress().getNumber();
        assertNotNull(numberString, "Expected a number at Kandlgasse, 1070");
        int number = Integer.parseInt(numberString);
        assertTrue(number >= 19 && number <= 24, "Expected number to be in range 19-24");

        result = geocoder.reverse(new LatLng(48.32, 16.19));
        assertNotNull(result, "Expected a result near the border of Vienna");
        assertNotNull(result.getAddress().getPostCode(), "Expected a post code for request outside of Vienna");
        assertEquals(1140, (int) result.getAddress().getPostCode());

        result = geocoder.reverse(new LatLng(48.18, 16.10));
        assertNull(result, "Expected no result for request at Pressbaum");
    }

    private PointDto createRequest(final String street, final String number, final Integer postCode, final String city) {
        String request = String.format("%s %s\n", street, number);
        if (postCode != null) {
            request += postCode + " ";
        }
        if (city != null) {
            request += city;
        }
        return PointDto.builder().details(request.trim()).build();
    }

    private void assertCoordinates(final LatLng expected, final LatLng result) {
        assertEquals(expected.getLat(), result.getLat(), 0.0005);
        assertEquals(expected.getLng(), result.getLng(), 0.0005);
    }
}
