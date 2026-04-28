package domain;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link GeographicBoundary} value object (US050).
 *
 * <p>Follows the AAA (Arrange-Act-Assert) convention as required by NFR03.
 * Coverage targets: all constructors, all validation paths, equals/hashCode,
 * toString, and overlapsWith in all geometric configurations.</p>
 */
public class GeographicBoundaryTest {

    // =========================================================================
    // Valid construction
    // =========================================================================

    @Test
    public void ensureValidBoundaryIsCreated() {
        // Arrange & Act
        final GeographicBoundary b = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);

        // Assert — all four values stored correctly
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

    // =========================================================================
    // Invalid latitude
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void ensureMinLatBelowMinus90IsRejected() {
        new GeographicBoundary(-91.0, 42.0, -10.0, -6.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureMaxLatAbove90IsRejected() {
        new GeographicBoundary(36.0, 91.0, -10.0, -6.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureMinLatGreaterThanMaxLatIsRejected() {
        new GeographicBoundary(42.0, 36.0, -10.0, -6.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureMinLatEqualToMaxLatIsRejected() {
        // min == max is not a valid area
        new GeographicBoundary(36.0, 36.0, -10.0, -6.0);
    }

    // =========================================================================
    // Invalid longitude
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void ensureMinLonBelowMinus180IsRejected() {
        new GeographicBoundary(36.0, 42.0, -181.0, -6.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureMaxLonAbove180IsRejected() {
        new GeographicBoundary(36.0, 42.0, -10.0, 181.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureMinLonGreaterThanMaxLonIsRejected() {
        new GeographicBoundary(36.0, 42.0, -6.0, -10.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureMinLonEqualToMaxLonIsRejected() {
        new GeographicBoundary(36.0, 42.0, -10.0, -10.0);
    }

    // =========================================================================
    // overlapsWith — the 9 geometric configurations
    // =========================================================================

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
        final GeographicBoundary b2 = new GeographicBoundary(40.0, 45.0, -8.0,  -3.0);

        // Assert
        assertTrue(b1.overlapsWith(b2));
        assertTrue(b2.overlapsWith(b1)); // symmetric
    }

    @Test
    public void ensureContainedBoundaryOverlaps() {
        // Arrange — b2 is entirely inside b1
        final GeographicBoundary b1 = new GeographicBoundary(30.0, 50.0, -15.0, 0.0);
        final GeographicBoundary b2 = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);

        // Assert
        assertTrue(b1.overlapsWith(b2));
        assertTrue(b2.overlapsWith(b1));
    }

    @Test
    public void ensureBoundariesTouchingOnLonEdgeDoNotOverlap() {
        // Arrange — b2 shares the right edge of b1 (maxLon b1 == minLon b2)
        // Adjacent areas sharing a border is normal in air traffic control.
        final GeographicBoundary b1 = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);
        final GeographicBoundary b2 = new GeographicBoundary(36.0, 42.0, -6.0,  -2.0);

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
        final GeographicBoundary b2 = new GeographicBoundary(36.0, 42.0,  0.0,   5.0);

        // Assert
        assertFalse(b1.overlapsWith(b2));
    }

    @Test
    public void ensureBoundaryEntirelyToTheLeftDoesNotOverlap() {
        // Arrange — b2 is entirely west of b1
        final GeographicBoundary b1 = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);
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

    // =========================================================================
    // Equality and hashCode
    // =========================================================================

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
    public void ensureBoundariesWithDifferentValuesAreNotEqual() {
        // Arrange
        final GeographicBoundary b1 = new GeographicBoundary(36.0, 42.0, -10.0, -6.0);
        final GeographicBoundary b2 = new GeographicBoundary(40.0, 50.0,  -5.0,  5.0);

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

    // =========================================================================
    // toString
    // =========================================================================

    @Test
    public void ensureToStringContainsAllValues() {
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
}
