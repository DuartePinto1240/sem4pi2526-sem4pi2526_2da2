package domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AltitudeRange} (US041).
 *
 * <p>Covers all invariants: valid construction, boundary values, invalid
 * inputs, overlap detection, equality, hashCode, and toString.
 * Follows AAA convention (NFR03).</p>
 */
class AltitudeRangeTest {

    // Happy path
    @Test
    void ensureValidAltitudeRangeIsCreated() {
        // Arrange + Act
        final AltitudeRange ar = new AltitudeRange(0.0, 5000.0);

        // Assert
        assertEquals(0.0,    ar.minAltitude());
        assertEquals(5000.0, ar.maxAltitude());
    }

    @Test
    void ensureMinZeroIsAccepted() {
        final AltitudeRange ar = new AltitudeRange(0.0, 1.0);
        assertEquals(0.0, ar.minAltitude());
    }

    @Test
    void ensureMaxExactly14000IsAccepted() {
        final AltitudeRange ar = new AltitudeRange(13000.0, 14000.0);
        assertEquals(14000.0, ar.maxAltitude());
    }

    @Test
    void ensureAdjacentLayersAtZeroAndMaxAreAccepted() {
        // Full vertical span — valid
        final AltitudeRange ar = new AltitudeRange(0.0, 14000.0);
        assertEquals(0.0,     ar.minAltitude());
        assertEquals(14000.0, ar.maxAltitude());
    }

    // Invalid construction
    @Test
    void ensureNegativeMinAltitudeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AltitudeRange(-1.0, 5000.0));
    }

    @Test
    void ensureMinAbove14000IsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AltitudeRange(14001.0, 15000.0));
    }

    @Test
    void ensureMaxAbove14000IsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AltitudeRange(5000.0, 14001.0));
    }

    @Test
    void ensureMaxEqualToMinIsRejected() {
        // A zero-height layer makes no sense
        assertThrows(IllegalArgumentException.class,
                () -> new AltitudeRange(5000.0, 5000.0));
    }

    @Test
    void ensureMaxBelowMinIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new AltitudeRange(8000.0, 3000.0));
    }

    // overlapsWith
    @Test
    void ensureOverlappingRangesAreDetected() {
        // [0, 5000] and [3000, 8000] — share [3000, 5000]
        final AltitudeRange a = new AltitudeRange(0.0,    5000.0);
        final AltitudeRange b = new AltitudeRange(3000.0, 8000.0);
        assertTrue(a.overlapsWith(b));
        assertTrue(b.overlapsWith(a)); // symmetric
    }

    @Test
    void ensureContainedRangeOverlaps() {
        // [0, 14000] contains [3000, 8000]
        final AltitudeRange outer = new AltitudeRange(0.0,    14000.0);
        final AltitudeRange inner = new AltitudeRange(3000.0, 8000.0);
        assertTrue(outer.overlapsWith(inner));
        assertTrue(inner.overlapsWith(outer));
    }

    @Test
    void ensureAdjacentRangesDoNotOverlap() {
        // [0, 5000] and [5000, 10000] — share only boundary point, not overlapping
        final AltitudeRange a = new AltitudeRange(0.0,    5000.0);
        final AltitudeRange b = new AltitudeRange(5000.0, 10000.0);
        assertFalse(a.overlapsWith(b));
        assertFalse(b.overlapsWith(a));
    }

    @Test
    void ensureDisjointRangesDoNotOverlap() {
        // [0, 3000] and [5000, 8000] — completely separate
        final AltitudeRange a = new AltitudeRange(0.0,    3000.0);
        final AltitudeRange b = new AltitudeRange(5000.0, 8000.0);
        assertFalse(a.overlapsWith(b));
    }

    @Test
    void ensureOverlapWithNullReturnsFalse() {
        final AltitudeRange ar = new AltitudeRange(0.0, 5000.0);
        assertFalse(ar.overlapsWith(null));
    }

    @Test
    void ensureRangeOverlapsWithItself() {
        final AltitudeRange ar = new AltitudeRange(1000.0, 6000.0);
        assertTrue(ar.overlapsWith(ar));
    }

    // Equality and hashCode
    @Test
    void ensureEqualRangesAreEqual() {
        final AltitudeRange a = new AltitudeRange(1000.0, 5000.0);
        final AltitudeRange b = new AltitudeRange(1000.0, 5000.0);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ensureDifferentMinMakesNotEqual() {
        final AltitudeRange a = new AltitudeRange(1000.0, 5000.0);
        final AltitudeRange b = new AltitudeRange(2000.0, 5000.0);
        assertNotEquals(a, b);
    }

    @Test
    void ensureDifferentMaxMakesNotEqual() {
        final AltitudeRange a = new AltitudeRange(1000.0, 5000.0);
        final AltitudeRange b = new AltitudeRange(1000.0, 6000.0);
        assertNotEquals(a, b);
    }

    @Test
    void ensureNotEqualToNull() {
        final AltitudeRange ar = new AltitudeRange(0.0, 5000.0);
        assertNotEquals(null, ar);
    }

    @Test
    void ensureNotEqualToDifferentType() {
        final AltitudeRange ar = new AltitudeRange(0.0, 5000.0);
        assertNotEquals("not an AltitudeRange", ar);
    }

    // toString
    @Test
    void ensureToStringContainsBothBounds() {
        final AltitudeRange ar = new AltitudeRange(1000.0, 5000.0);
        final String s = ar.toString();
        assertTrue(s.contains("1000"), "toString should contain min altitude");
        assertTrue(s.contains("5000"), "toString should contain max altitude");
    }
}
