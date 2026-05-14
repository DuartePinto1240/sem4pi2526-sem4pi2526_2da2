package domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WeatherSubArea} (US041).
 *
 */
class WeatherSubAreaTest {

    // Shared fixtures
    private static final GeographicBoundary PARENT =
            new GeographicBoundary(36.0, 42.0, -10.0, -6.0);

    private static final GeographicBoundary INSIDE =
            new GeographicBoundary(37.0, 40.0, -9.0, -7.0);

    private static final AltitudeRange ALT_LOW  = new AltitudeRange(0.0,    5000.0);
    private static final AltitudeRange ALT_HIGH = new AltitudeRange(5000.0, 10000.0);
    private static final AltitudeRange ALT_MID  = new AltitudeRange(3000.0, 8000.0);

    // Happy path

    @Test
    void ensureValidSubAreaIsCreated() {
        final WeatherSubArea sa = new WeatherSubArea(INSIDE, ALT_LOW);
        assertEquals(INSIDE,  sa.subBoundary());
        assertEquals(ALT_LOW, sa.altitudeRange());
    }

    // Null rejection
    @Test
    void ensureNullBoundaryIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherSubArea(null, ALT_LOW));
    }

    @Test
    void ensureNullAltitudeRangeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherSubArea(INSIDE, null));
    }

    // isInsideBoundary

    @Test
    void ensureSubAreaInsideParentReturnsTrue() {
        assertTrue(new WeatherSubArea(INSIDE, ALT_LOW).isInsideBoundary(PARENT));
    }

    @Test
    void ensureSubAreaMatchingParentExactlyReturnsTrue() {
        assertTrue(new WeatherSubArea(PARENT, ALT_LOW).isInsideBoundary(PARENT));
    }

    @Test
    void ensureSubAreaExceedingParentLatReturnsFalse() {
        final GeographicBoundary outside = new GeographicBoundary(35.0, 40.0, -9.0, -7.0);
        assertFalse(new WeatherSubArea(outside, ALT_LOW).isInsideBoundary(PARENT));
    }

    @Test
    void ensureSubAreaExceedingParentLonReturnsFalse() {
        final GeographicBoundary outside = new GeographicBoundary(37.0, 40.0, -11.0, -7.0);
        assertFalse(new WeatherSubArea(outside, ALT_LOW).isInsideBoundary(PARENT));
    }

    @Test
    void ensureIsInsideBoundaryWithNullReturnsFalse() {
        assertFalse(new WeatherSubArea(INSIDE, ALT_LOW).isInsideBoundary(null));
    }

    // overlapsWith

    @Test
    void ensureGeographicAndAltitudeOverlapConflicts() {
        // Same geographic overlap AND same altitude → conflict
        final GeographicBoundary b1 = new GeographicBoundary(37.0, 40.0, -9.0, -7.0);
        final GeographicBoundary b2 = new GeographicBoundary(39.0, 41.0, -8.0, -6.5);
        assertTrue(new WeatherSubArea(b1, ALT_LOW).overlapsWith(
                   new WeatherSubArea(b2, ALT_LOW)));
    }

    @Test
    void ensureGeographicOverlapWithAdjacentAltitudesDoesNotConflict() {
        final WeatherSubArea sa1 = new WeatherSubArea(INSIDE, ALT_LOW);
        final WeatherSubArea sa2 = new WeatherSubArea(INSIDE, ALT_HIGH);
        assertFalse(sa1.overlapsWith(sa2));
    }

    @Test
    void ensureGeographicOverlapWithOverlappingAltitudesConflicts() {
        final WeatherSubArea sa1 = new WeatherSubArea(INSIDE, ALT_LOW);
        final WeatherSubArea sa2 = new WeatherSubArea(INSIDE, ALT_MID);
        assertTrue(sa1.overlapsWith(sa2));
    }

    @Test
    void ensureDisjointGeographyWithOverlappingAltitudesDoesNotConflict() {
        final GeographicBoundary b1 = new GeographicBoundary(37.0, 38.5, -9.0, -8.0);
        final GeographicBoundary b2 = new GeographicBoundary(39.0, 41.0, -7.5, -6.5);
        assertFalse(new WeatherSubArea(b1, ALT_MID).overlapsWith(
                    new WeatherSubArea(b2, ALT_MID)));
    }

    @Test
    void ensureNonOverlappingGeographyWithSameAltitudeDoesNotConflict() {
        final GeographicBoundary b1 = new GeographicBoundary(37.0, 38.5, -9.0, -8.0);
        final GeographicBoundary b2 = new GeographicBoundary(39.0, 41.0, -7.5, -6.5);
        assertFalse(new WeatherSubArea(b1, ALT_LOW).overlapsWith(
                    new WeatherSubArea(b2, ALT_LOW)));
    }

    @Test
    void ensureOverlapWithNullReturnsFalse() {
        assertFalse(new WeatherSubArea(INSIDE, ALT_LOW).overlapsWith(null));
    }

    // Equality

    @Test
    void ensureEqualSubAreasAreEqual() {
        assertEquals(new WeatherSubArea(INSIDE, ALT_LOW),
                     new WeatherSubArea(INSIDE, ALT_LOW));
    }

    @Test
    void ensureDifferentBoundaryMakesNotEqual() {
        assertNotEquals(new WeatherSubArea(INSIDE, ALT_LOW),
                        new WeatherSubArea(PARENT, ALT_LOW));
    }

    @Test
    void ensureDifferentAltitudeMakesNotEqual() {
        assertNotEquals(new WeatherSubArea(INSIDE, ALT_LOW),
                        new WeatherSubArea(INSIDE, ALT_HIGH));
    }

    @Test
    void ensureToStringIsNotEmpty() {
        final String s = new WeatherSubArea(INSIDE, ALT_LOW).toString();
        assertFalse(s.isBlank());
    }
}
