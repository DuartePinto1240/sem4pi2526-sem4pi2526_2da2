package domain;

import domain.GeographicBoundary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WeatherSubArea} (US041).
 *
 * <p>Covers: valid construction, null rejection, containment check,
 * overlap detection, and equality. Follows AAA convention (NFR03).</p>
 */
public class WeatherSubAreaTest {

    // Parent area: Lat[36, 42] Lon[-10, -6]
    private static final GeographicBoundary PARENT =
            new GeographicBoundary(36.0, 42.0, -10.0, -6.0);

    // Sub-area fully inside the parent
    private static final GeographicBoundary INSIDE =
            new GeographicBoundary(37.0, 40.0, -9.0, -7.0);

    // =========================================================================
    // Happy path
    // =========================================================================

    @Test
    public void ensureValidSubAreaIsCreated() {
        final WeatherSubArea sa = new WeatherSubArea(INSIDE);
        assertEquals(INSIDE, sa.subBoundary());
    }

    // =========================================================================
    // Null rejection
    // =========================================================================

    @Test
    public void ensureNullBoundaryIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherSubArea(null));
    }

    // =========================================================================
    // isInsideBoundary
    // =========================================================================

    @Test
    public void ensureSubAreaInsideParentReturnsTrue() {
        final WeatherSubArea sa = new WeatherSubArea(INSIDE);
        assertTrue(sa.isInsideBoundary(PARENT));
    }

    @Test
    public void ensureSubAreaMatchingParentExactlyReturnsTrue() {
        // Sub-area == parent boundary — allowed (covers the whole area)
        final WeatherSubArea sa = new WeatherSubArea(PARENT);
        assertTrue(sa.isInsideBoundary(PARENT));
    }

    @Test
    public void ensureSubAreaExceedingParentLatReturnsFalse() {
        final GeographicBoundary outside = new GeographicBoundary(35.0, 40.0, -9.0, -7.0);
        final WeatherSubArea sa = new WeatherSubArea(outside);
        assertFalse(sa.isInsideBoundary(PARENT));
    }

    @Test
    public void ensureSubAreaExceedingParentLonReturnsFalse() {
        final GeographicBoundary outside = new GeographicBoundary(37.0, 40.0, -11.0, -7.0);
        final WeatherSubArea sa = new WeatherSubArea(outside);
        assertFalse(sa.isInsideBoundary(PARENT));
    }

    @Test
    public void ensureIsInsideBoundaryWithNullReturnsFalse() {
        final WeatherSubArea sa = new WeatherSubArea(INSIDE);
        assertFalse(sa.isInsideBoundary(null));
    }

    // =========================================================================
    // overlapsWith
    // =========================================================================

    @Test
    public void ensureOverlappingSubAreasAreDetected() {
        final GeographicBoundary b1 = new GeographicBoundary(37.0, 40.0, -9.0, -7.0);
        final GeographicBoundary b2 = new GeographicBoundary(39.0, 41.0, -8.0, -6.5);
        final WeatherSubArea sa1 = new WeatherSubArea(b1);
        final WeatherSubArea sa2 = new WeatherSubArea(b2);
        assertTrue(sa1.overlapsWith(sa2));
    }

    @Test
    public void ensureNonOverlappingSubAreasAreNotDetected() {
        final GeographicBoundary b1 = new GeographicBoundary(37.0, 38.5, -9.0, -8.0);
        final GeographicBoundary b2 = new GeographicBoundary(39.0, 41.0, -7.5, -6.5);
        final WeatherSubArea sa1 = new WeatherSubArea(b1);
        final WeatherSubArea sa2 = new WeatherSubArea(b2);
        assertFalse(sa1.overlapsWith(sa2));
    }

    @Test
    public void ensureOverlapWithNullReturnsFalse() {
        final WeatherSubArea sa = new WeatherSubArea(INSIDE);
        assertFalse(sa.overlapsWith(null));
    }

    // =========================================================================
    // Equality
    // =========================================================================

    @Test
    public void ensureEqualSubAreasAreEqual() {
        final WeatherSubArea sa1 = new WeatherSubArea(INSIDE);
        final WeatherSubArea sa2 = new WeatherSubArea(INSIDE);
        assertEquals(sa1, sa2);
    }

    @Test
    public void ensureDifferentBoundaryMakesNotEqual() {
        final WeatherSubArea sa1 = new WeatherSubArea(INSIDE);
        final WeatherSubArea sa2 = new WeatherSubArea(PARENT);
        assertNotEquals(sa1, sa2);
    }
}
