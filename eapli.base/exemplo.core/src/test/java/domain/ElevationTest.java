package domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Elevation} value object (US052).
 *
 * <p>The specification states "elevation in meters above sea level" as the
 * unit of reference. Negative values are accepted because some airports
 * (e.g. Amsterdam Schiphol at −3.35 m) exist below sea level.</p>
 */
class ElevationTest {

    // Valid construction — all numeric values accepted

    @Test
    void ensurePositiveElevationIsStored() {
        assertEquals(113.0, new Elevation(113.0).metres());
    }

    @Test
    void ensureZeroElevationIsAccepted() {
        assertEquals(0.0, new Elevation(0.0).metres());
    }

    @Test
    void ensureNegativeElevationIsAccepted() {
        // Schiphol airport is ~3.35 m below sea level
        assertEquals(-3.35, new Elevation(-3.35).metres());
    }

    @Test
    void ensureLargePositiveElevationIsAccepted() {
        // Qamdo Bamda, Tibet — highest commercial airport at ~4334 m
        assertEquals(4334.0, new Elevation(4334.0).metres());
    }

    @Test
    void ensureLargeNegativeElevationIsAccepted() {
        // Dead Sea area ~ -400 m
        assertEquals(-400.0, new Elevation(-400.0).metres());
    }

    // Equality and hashCode

    @Test
    void ensureTwoElevationsWithSameValueAreEqual() {
        assertEquals(new Elevation(113.0), new Elevation(113.0));
    }

    @Test
    void ensureTwoElevationsWithSameValueHaveSameHashCode() {
        assertEquals(new Elevation(113.0).hashCode(), new Elevation(113.0).hashCode());
    }

    @Test
    void ensureTwoDifferentElevationsAreNotEqual() {
        assertNotEquals(new Elevation(113.0), new Elevation(69.0));
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

