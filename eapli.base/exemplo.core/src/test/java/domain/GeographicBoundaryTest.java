package domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GeographicBoundary} value object (US050).
 *
 * <p>Follows the AAA (Arrange-Act-Assert) convention as required by NFR03.
 * Coverage targets: all constructors, all validation paths, equals/hashCode,
 * toString, and overlapsWith in all geometric configurations.</p>
 */
public class GeographicBoundaryTest {

    // Valid construction

    @Test
    public void ensureValidBoundaryIsCreated() {
        // Arrange & Act
        final GeographicBoundary b = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);

        assertEquals(36.0,  b.minLat(), 0.0001);
        assertEquals(42.0,  b.maxLat(), 0.0001);
        assertEquals(-10.0, b.minLon(), 0.0001);
        assertEquals(-6.0,  b.maxLon(), 0.0001);
    }

    @Test
    public void ensureBoundaryAtExactLimitsIsAccepted() {
        // Arrange & Act — boundary exactly at [-90,90] x [-180,180]
        final GeographicBoundary b = new GeographicBoundary(-90.0, 90.0, -180.0, 180.0);

        // Assert
        assertNotNull(b);
    }

    @Test
    public void ensureNegativeLatitudesAreAccepted() {
        // Arrange & Act — southern hemisphere
        final GeographicBoundary b = new GeographicBoundary(-55.0, -10.0, -70.0, -30.0);

        // Assert
        assertEquals(-55.0, b.minLat(), 0.0001);
        assertEquals(-10.0, b.maxLat(), 0.0001);
    }

    // Convenience constructor

    @Test
    public void ensureConvenienceConstructorAppliesDefaultMinAltitude() {
        // Arrange & Act — no altitude supplied
        final GeographicBoundary b = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);

        // Assert — must always be the system constant, never a magic 0
        assertEquals(GeographicBoundary.MIN_ALTITUDE_METRES, b.minAltitude(), 0.0001);
    }

    @Test
    public void ensureConvenienceConstructorAppliesDefaultMaxAltitude() {
        // Arrange & Act
        final GeographicBoundary b = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);

        // Assert — must always be the system constant (14 000 m)
        assertEquals(GeographicBoundary.MAX_ALTITUDE_METRES, b.maxAltitude(), 0.0001);
    }

    @Test
    public void ensureTwoBoundariesBuiltWithConvenienceConstructorHaveSameAltitude() {
        // Arrange — two different areas, both using the 4-param constructor
        final GeographicBoundary b1 = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);
        final GeographicBoundary b2 = new GeographicBoundary(50.0, 55.0,   2.0,  8.0);

        // Assert — both carry the same regulated vertical extent
        assertEquals(b1.minAltitude(), b2.minAltitude(), 0.0001);
        assertEquals(b1.maxAltitude(), b2.maxAltitude(), 0.0001);
    }

    // Valid construction — 6-parameter constructor (explicit altitude)

    @Test
    public void ensureExplicitAltitudeIsStoredCorrectly() {
        // Arrange & Act
        final GeographicBoundary b =
                new GeographicBoundary(36.0, 42.0, -10.0, -6.0, 0.0, 5000.0);

        // Assert
        assertEquals(0.0,    b.minAltitude(), 0.0001);
        assertEquals(5000.0, b.maxAltitude(), 0.0001);
    }

    @Test
    public void ensureAltitudeAtExactSystemLimitsIsAccepted() {
        // Arrange & Act — exactly 0 to MAX_ALTITUDE_METRES
        final GeographicBoundary b = new GeographicBoundary(
                36.0, 42.0, -10.0, -6.0,
                GeographicBoundary.MIN_ALTITUDE_METRES,
                GeographicBoundary.MAX_ALTITUDE_METRES);

        // Assert
        assertNotNull(b);
    }

    // Invalid latitude

    @Test
    public void ensureMinLatBelowMinus90IsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new GeographicBoundary(-91.0, 42.0, -10.0, -6.0));
    }

    @Test
    public void ensureMaxLatAbove90IsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new GeographicBoundary(36.0, 91.0, -10.0, -6.0));
    }

    @Test
    public void ensureMinLatGreaterThanMaxLatIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new GeographicBoundary(42.0, 36.0, -10.0, -6.0));
    }

    @Test
    public void ensureMinLatEqualToMaxLatIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new GeographicBoundary(36.0, 36.0, -10.0, -6.0));
    }

    // Invalid longitude

    @Test
    public void ensureMinLonBelowMinus180IsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new GeographicBoundary(36.0, 42.0, -181.0, -6.0));
    }

    @Test
    public void ensureMaxLonAbove180IsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new GeographicBoundary(36.0, 42.0, -10.0, 181.0));
    }

    @Test
    public void ensureMinLonGreaterThanMaxLonIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new GeographicBoundary(36.0, 42.0, -6.0, -10.0));
    }

    @Test
    public void ensureMinLonEqualToMaxLonIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new GeographicBoundary(36.0, 42.0, -10.0, -10.0));
    }

    // Invalid altitude

    @Test
    public void ensureNegativeMinAltitudeIsRejected() {
        // Arrange & Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                new GeographicBoundary(36.0, 42.0, -10.0, -6.0, -1.0, 14000.0));
    }

    @Test
    public void ensureMaxAltitudeExceedingSystemCeilingIsRejected() {
        // Arrange & Act & Assert — 14 001 m is above the regulated ceiling
        assertThrows(IllegalArgumentException.class, () ->
                new GeographicBoundary(36.0, 42.0, -10.0, -6.0, 0.0, 14_001.0));
    }

    @Test
    public void ensureMinAltitudeEqualToMaxAltitudeIsRejected() {
        // Arrange & Act & Assert — zero-height slab is not a valid area
        assertThrows(IllegalArgumentException.class, () ->
                new GeographicBoundary(36.0, 42.0, -10.0, -6.0, 5000.0, 5000.0));
    }

    @Test
    public void ensureMinAltitudeGreaterThanMaxAltitudeIsRejected() {
        // Arrange & Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                new GeographicBoundary(36.0, 42.0, -10.0, -6.0, 8000.0, 3000.0));
    }

    // overlapsWith — the 9 geometric configurations

    @Test
    public void ensureIdenticalBoundariesOverlap() {
        // Arrange
        final GeographicBoundary b1 = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);
        final GeographicBoundary b2 = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);

        // Assert
        assertTrue(b1.overlapsWith(b2));
    }

    @Test
    public void ensurePartiallyOverlappingBoundariesAreDetected() {
        // Arrange — b2 overlaps the top-right corner of b1
        final GeographicBoundary b1 = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);
        final GeographicBoundary b2 = new GeographicBoundary(40.0, 45.0,  -8.0, -3.0);

        // Assert
        assertTrue(b1.overlapsWith(b2));
        assertTrue(b2.overlapsWith(b1)); // symmetric
    }

    @Test
    public void ensureContainedBoundaryOverlaps() {
        // Arrange — b2 is entirely inside b1
        final GeographicBoundary b1 = new GeographicBoundary(30.0, 50.0, -15.0,  0.0);
        final GeographicBoundary b2 = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);

        // Assert
        assertTrue(b1.overlapsWith(b2));
        assertTrue(b2.overlapsWith(b1));
    }

    @Test
    public void ensureBoundariesTouchingOnLonEdgeDoNotOverlap() {
        // Arrange — b2 shares the right edge of b1 (maxLon b1 == minLon b2)
        final GeographicBoundary b1 = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);
        final GeographicBoundary b2 = new GeographicBoundary(36.0, 42.0,  -6.0, -2.0);

        // Assert — touching edges are NOT an overlap
        assertFalse(b1.overlapsWith(b2));
    }

    @Test
    public void ensureBoundariesTouchingOnLatEdgeDoNotOverlap() {
        // Arrange — b2 is directly above b1, sharing the lat=42 edge
        final GeographicBoundary b1 = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);
        final GeographicBoundary b2 = new GeographicBoundary(42.0, 48.0, -10.0, -6.0);

        // Assert
        assertFalse(b1.overlapsWith(b2));
    }

    @Test
    public void ensureBoundaryEntirelyAboveDoesNotOverlap() {
        // Arrange — b2 is entirely north of b1 (no lat overlap)
        final GeographicBoundary b1 = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);
        final GeographicBoundary b2 = new GeographicBoundary(43.0, 50.0, -10.0, -6.0);

        // Assert
        assertFalse(b1.overlapsWith(b2));
        assertFalse(b2.overlapsWith(b1));
    }

    @Test
    public void ensureBoundaryEntirelyBelowDoesNotOverlap() {
        // Arrange — b2 is entirely south of b1
        final GeographicBoundary b1 = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);
        final GeographicBoundary b2 = new GeographicBoundary(20.0, 35.0, -10.0, -6.0);

        // Assert
        assertFalse(b1.overlapsWith(b2));
    }

    @Test
    public void ensureBoundaryEntirelyToTheRightDoesNotOverlap() {
        // Arrange — b2 is entirely east of b1 (no lon overlap)
        final GeographicBoundary b1 = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);
        final GeographicBoundary b2 = new GeographicBoundary(36.0, 42.0,   0.0,  5.0);

        // Assert
        assertFalse(b1.overlapsWith(b2));
    }

    @Test
    public void ensureBoundaryEntirelyToTheLeftDoesNotOverlap() {
        // Arrange — b2 is entirely west of b1
        final GeographicBoundary b1 = new GeographicBoundary(36.0, 42.0, -10.0,  -6.0);
        final GeographicBoundary b2 = new GeographicBoundary(36.0, 42.0, -20.0, -11.0);

        // Assert
        assertFalse(b1.overlapsWith(b2));
    }

    @Test
    public void ensureOverlapsWithNullReturnsFalse() {
        // Arrange
        final GeographicBoundary b = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);

        // Assert — null guard
        assertFalse(b.overlapsWith(null));
    }

    // Equality and hashCode

    @Test
    public void ensureTwoBoundariesWithSameValuesAreEqual() {
        // Arrange
        final GeographicBoundary b1 = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);
        final GeographicBoundary b2 = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);

        // Assert
        assertEquals(b1, b2);
        assertEquals(b1.hashCode(), b2.hashCode());
    }

    @Test
    public void ensureBoundariesWithDifferentLatLonAreNotEqual() {
        // Arrange
        final GeographicBoundary b1 = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);
        final GeographicBoundary b2 = new GeographicBoundary(40.0, 50.0,  -5.0,  5.0);

        // Assert
        assertNotEquals(b1, b2);
    }

    @Test
    public void ensureBoundariesWithDifferentAltitudeAreNotEqual() {
        // Arrange — same lat/lon, different max altitude
        final GeographicBoundary b1 = new GeographicBoundary(36.0, 42.0, -10.0, -6.0, 0.0, 5000.0);
        final GeographicBoundary b2 = new GeographicBoundary(36.0, 42.0, -10.0, -6.0, 0.0, 8000.0);

        // Assert
        assertNotEquals(b1, b2);
    }

    @Test
    public void ensureBoundaryDoesNotEqualNull() {
        // Arrange
        final GeographicBoundary b = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);

        // Assert
        assertNotEquals(null, b);
    }

    @Test
    public void ensureBoundaryEqualsItself() {
        // Arrange
        final GeographicBoundary b = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);

        // Assert
        assertEquals(b, b);
    }

    // toString

    @Test
    public void ensureToStringContainsLatLonValues() {
        // Arrange
        final GeographicBoundary b = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);

        // Act
        final String result = b.toString();

        // Assert
        assertTrue(result.contains("36"));
        assertTrue(result.contains("42"));
        assertTrue(result.contains("-10"));
        assertTrue(result.contains("-6"));
    }

    @Test
    public void ensureToStringContainsAltitudeValues() {
        // Arrange
        final GeographicBoundary b = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);

        // Act
        final String result = b.toString();

        // Assert — default altitude values must appear in the string representation
        assertTrue(result.contains("0"));
        assertTrue(result.contains("14000"));
    }
}
