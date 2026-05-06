package domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ObservationPeriod} (US041).
 *
 * <p>Covers: valid construction, end-before-start rejection, null inputs,
 * overlap detection, and equality. Follows AAA convention (NFR03).</p>
 */
public class ObservationPeriodTest {

    private static final LocalDateTime T1 = LocalDateTime.of(2026, 5, 1, 8, 0);
    private static final LocalDateTime T2 = LocalDateTime.of(2026, 5, 1, 12, 0);
    private static final LocalDateTime T3 = LocalDateTime.of(2026, 5, 1, 16, 0);
    private static final LocalDateTime T4 = LocalDateTime.of(2026, 5, 1, 20, 0);

    // =========================================================================
    // Happy path
    // =========================================================================

    @Test
    public void ensureValidPeriodIsCreated() {
        final ObservationPeriod p = new ObservationPeriod(T1, T2);
        assertEquals(T1, p.startDateTime());
        assertEquals(T2, p.endDateTime());
    }

    // =========================================================================
    // Invalid construction
    // =========================================================================

    @Test
    public void ensureEndBeforeStartIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ObservationPeriod(T2, T1));
    }

    @Test
    public void ensureEndEqualToStartIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ObservationPeriod(T1, T1));
    }

    @Test
    public void ensureNullStartIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ObservationPeriod(null, T2));
    }

    @Test
    public void ensureNullEndIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ObservationPeriod(T1, null));
    }

    // =========================================================================
    // Overlap detection
    // =========================================================================

    @Test
    public void ensureOverlappingPeriodsAreDetected() {
        // [T1, T3] overlaps with [T2, T4]
        final ObservationPeriod p1 = new ObservationPeriod(T1, T3);
        final ObservationPeriod p2 = new ObservationPeriod(T2, T4);
        assertTrue(p1.overlapsWith(p2));
        assertTrue(p2.overlapsWith(p1)); // symmetric
    }

    @Test
    public void ensureContainedPeriodOverlaps() {
        // [T1, T4] contains [T2, T3]
        final ObservationPeriod outer = new ObservationPeriod(T1, T4);
        final ObservationPeriod inner = new ObservationPeriod(T2, T3);
        assertTrue(outer.overlapsWith(inner));
    }

    @Test
    public void ensureAdjacentPeriodsDoNotOverlap() {
        // [T1, T2] and [T2, T3] share only a point — not overlapping
        final ObservationPeriod p1 = new ObservationPeriod(T1, T2);
        final ObservationPeriod p2 = new ObservationPeriod(T2, T3);
        assertFalse(p1.overlapsWith(p2));
    }

    @Test
    public void ensureDisjointPeriodsDoNotOverlap() {
        final ObservationPeriod p1 = new ObservationPeriod(T1, T2);
        final ObservationPeriod p3 = new ObservationPeriod(T3, T4);
        assertFalse(p1.overlapsWith(p3));
    }

    @Test
    public void ensureOverlapWithNullReturnsFalse() {
        final ObservationPeriod p = new ObservationPeriod(T1, T2);
        assertFalse(p.overlapsWith(null));
    }

    // =========================================================================
    // Equality
    // =========================================================================

    @Test
    public void ensureEqualPeriodsAreEqual() {
        final ObservationPeriod p1 = new ObservationPeriod(T1, T2);
        final ObservationPeriod p2 = new ObservationPeriod(T1, T2);
        assertEquals(p1, p2);
    }

    @Test
    public void ensureDifferentStartMakesNotEqual() {
        final ObservationPeriod p1 = new ObservationPeriod(T1, T3);
        final ObservationPeriod p2 = new ObservationPeriod(T2, T3);
        assertNotEquals(p1, p2);
    }
}
