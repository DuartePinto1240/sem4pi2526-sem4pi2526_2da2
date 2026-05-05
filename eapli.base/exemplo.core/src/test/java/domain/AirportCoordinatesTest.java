package domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AirportCoordinates} value object (US052).
 */
public class AirportCoordinatesTest {

    @Test
    public void ensureValidCoordinatesAreCreated() {
        final AirportCoordinates coords = new AirportCoordinates(38.7756, -9.1354);

        assertEquals(38.7756, coords.latitude());
        assertEquals(-9.1354, coords.longitude());
    }

    @Test
    public void ensureBoundaryLatitudesAreAccepted() {
        assertDoesNotThrow(() -> new AirportCoordinates(-90.0, 0.0));
        assertDoesNotThrow(() -> new AirportCoordinates(90.0, 0.0));
    }

    @Test
    public void ensureBoundaryLongitudesAreAccepted() {
        assertDoesNotThrow(() -> new AirportCoordinates(0.0, -180.0));
        assertDoesNotThrow(() -> new AirportCoordinates(0.0, 180.0));
    }

    @Test
    public void ensureLatitudeBelowMinusNinetyIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirportCoordinates(-90.001, 0.0));
    }

    @Test
    public void ensureLatitudeAboveNinetyIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirportCoordinates(90.001, 0.0));
    }

    @Test
    public void ensureLongitudeBelowMinusOneEightyIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirportCoordinates(0.0, -180.001));
    }

    @Test
    public void ensureLongitudeAboveOneEightyIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirportCoordinates(0.0, 180.001));
    }

    @Test
    public void ensureCoordinatesInsideBoundaryReturnTrue() {
        final AirportCoordinates coords = new AirportCoordinates(38.7756, -9.1354);
        final GeographicBoundary boundary = new GeographicBoundary(36.0, 42.2, -10.0, -6.0);

        assertTrue(coords.isWithinBoundary(boundary));
    }

    @Test
    public void ensureCoordinatesOnAllBoundaryEdgesReturnTrue() {
        final GeographicBoundary boundary = new GeographicBoundary(36.0, 42.2, -10.0, -6.0);

        assertTrue(new AirportCoordinates(36.0, -8.0).isWithinBoundary(boundary));
        assertTrue(new AirportCoordinates(42.2, -8.0).isWithinBoundary(boundary));
        assertTrue(new AirportCoordinates(38.0, -10.0).isWithinBoundary(boundary));
        assertTrue(new AirportCoordinates(38.0, -6.0).isWithinBoundary(boundary));
    }

    @Test
    public void ensureCoordinatesOutsideBoundaryReturnFalse() {
        final GeographicBoundary boundary = new GeographicBoundary(36.0, 42.2, -10.0, -6.0);

        assertFalse(new AirportCoordinates(35.9, -8.0).isWithinBoundary(boundary));
        assertFalse(new AirportCoordinates(42.3, -8.0).isWithinBoundary(boundary));
        assertFalse(new AirportCoordinates(38.0, -10.1).isWithinBoundary(boundary));
        assertFalse(new AirportCoordinates(38.0, -5.9).isWithinBoundary(boundary));
    }

    @Test
    public void ensureNullBoundaryReturnsFalse() {
        final AirportCoordinates coords = new AirportCoordinates(38.7756, -9.1354);

        assertFalse(coords.isWithinBoundary(null));
    }

    @Test
    public void ensureTwoIdenticalCoordinatesAreEqualAndHaveSameHashCode() {
        final AirportCoordinates first = new AirportCoordinates(38.7756, -9.1354);
        final AirportCoordinates second = new AirportCoordinates(38.7756, -9.1354);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void ensureCoordinateIsEqualToItself() {
        final AirportCoordinates coords = new AirportCoordinates(38.7756, -9.1354);

        assertEquals(coords, coords);
    }

    @Test
    public void ensureCoordinatesWithDifferentLatitudeAreNotEqual() {
        assertNotEquals(new AirportCoordinates(38.7756, -9.1354),
                new AirportCoordinates(41.2481, -9.1354));
    }

    @Test
    public void ensureCoordinatesWithDifferentLongitudeAreNotEqual() {
        assertNotEquals(new AirportCoordinates(38.7756, -9.1354),
                new AirportCoordinates(38.7756, -8.6814));
    }

    @Test
    public void ensureCoordinatesAreNotEqualToNullOrOtherType() {
        final AirportCoordinates coords = new AirportCoordinates(38.7756, -9.1354);

        assertNotEquals(coords, null);
        assertNotEquals(coords, "38.7756,-9.1354");
    }

}
