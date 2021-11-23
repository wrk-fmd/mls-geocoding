package at.wrk.fmd.mls.geocoding.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import at.wrk.fmd.mls.geocoding.api.dto.Address;
import at.wrk.fmd.mls.geocoding.api.dto.PointDto;
import org.junit.jupiter.api.Test;

public class AddressParserTest {

    @Test
    public void parseAddressWithNumber_fullNumberIsReturned() {
        Address address = parseFromString("Neubaugasse 1-3/2/3/4 Meier");
        assertEquals("Neubaugasse", address.getStreet());
        assertEquals("1-3", address.getNumber());
        assertEquals("2", address.getBlock());
        assertEquals("3/4 Meier", address.getDetails());
    }

    @Test
    public void parseAddressWithShortNumber_numberIsReturned() {
        Address address = parseFromString("Neubaugasse 1///EHF");
        assertEquals("Neubaugasse", address.getStreet());
        assertEquals("1", address.getNumber());
        assertNull(address.getBlock());
        assertEquals("/EHF", address.getDetails());
    }

    @Test
    public void parseAddressWithIntersection_intersectionIsReturned() {
        Address address = parseFromString("Neubaugasse # Stanislausgasse");
        assertEquals("Neubaugasse", address.getStreet());
        assertEquals("Stanislausgasse", address.getIntersection());
    }

    @Test
    public void parseAddressWithIntersectionAndCity_intersectionIsReturned() {
        Address address = parseFromString("Neubaugasse # Stanislausgasse\n1234 Wien");
        assertEquals("Neubaugasse", address.getStreet());
        assertEquals("Stanislausgasse", address.getIntersection());
        assertEquals("Wien", address.getCity());
        assertEquals(1234, address.getPostCode());
    }

    private Address parseFromString(String text) {
        PointDto request = PointDto.builder().details(text).build();
        Address address = AddressParser.parseAddress(request).getAddress();
        assertNotNull(address, "Expected parsed address to be not null");
        return address;
    }
}