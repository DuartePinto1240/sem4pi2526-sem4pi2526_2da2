package domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Elevation} value object (US052).
 */
class ElevationTest {

    // Valid construction

    @Test
    void ensurePositiveElevationIsStored() {
        assertEquals(113.0, new Elevation(113.0).metres());
    }

    @Test
    void ensureLargePositiveElevationIsAccepted() {
        assertEquals(4334.0, new Elevation(4334.0).metres());
    }

    // Invalid construction

    @Test
    void ensureZeroElevationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Elevation(0.0));
    }

    @Test
    void ensureNegativeElevationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Elevation(-3.35));
    }

    @Test
    void ensureLargeNegativeElevationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Elevation(-400.0));
    }

    // Equality and hashCode

    @Test
    void ensureTwoElevationsWithSameValueAreEqual() {
        assertEquals(new Elevation(113.0), new Elevation(113.0));
    }

    @Test
    void ensureTwoElevationsWithSameValueHaveSameHashCode() {
        assertEquals(new Elevation(113.0).hashCode(),
                new Elevation(113.0).hashCode());
    }

    @Test
    void ensureTwoDifferentElevationsAreNotEqual() {
        assertNotEquals(new Elevation(113.0),
                new Elevation(69.0));
    }

    @Test
    void ensureElevationIsNotEqualToNull() {
        assertNotEquals(new Elevation(100.0), null);
    }

    @Test
    void ensureElevationIsNotEqualToOtherType() {
        assertNotEquals(new Elevation(100.0), 100.0);
    }

    @Test
    void ensureElevationIsEqualToItself() {
        final Elevation e = new Elevation(100.0);
        assertEquals(e, e);
    }

    // toString

    @Test
    void ensureToStringContainsValue() {
        assertTrue(new Elevation(113.0).toString().contains("113.0"));
    }

    @Test
    void ensureToStringContainsUnit() {
        assertTrue(new Elevation(113.0).toString().contains("m"));
    }
}