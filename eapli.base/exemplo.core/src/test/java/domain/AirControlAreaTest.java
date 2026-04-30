package domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AirControlArea} aggregate root (US050).
 *
 * <p>Follows the AAA (Arrange-Act-Assert) convention as required by NFR03.</p>
 */
public class AirControlAreaTest {

    // Reusable valid boundary
    private static final GeographicBoundary VALID_BOUNDARY =
            new GeographicBoundary(36.0, 42.0, -10.0, -6.0);

    // =========================================================================
    // Valid construction
    // =========================================================================

    @Test
    public void ensureValidAreaIsCreatedWithCorrectCode() {
        // Arrange & Act
        final AirControlArea area = new AirControlArea("LPPC", "Lisbon ACC", VALID_BOUNDARY);

        // Assert
        assertEquals("LPPC", area.identity().code());
    }

    @Test
    public void ensureValidAreaIsCreatedWithCorrectName() {
        // Arrange & Act
        final AirControlArea area = new AirControlArea("LPPC", "Lisbon ACC", VALID_BOUNDARY);

        // Assert
        assertEquals("Lisbon ACC", area.name());
    }

    @Test
    public void ensureValidAreaIsCreatedWithCorrectBoundary() {
        // Arrange & Act
        final AirControlArea area = new AirControlArea("LPPC", "Lisbon ACC", VALID_BOUNDARY);

        // Assert
        assertEquals(VALID_BOUNDARY, area.boundary());
    }

    @Test
    public void ensureAreaCodeIsNormalisedToUpperCase() {
        // Arrange & Act
        final AirControlArea area = new AirControlArea("lppc", "Lisbon ACC", VALID_BOUNDARY);

        // Assert
        assertEquals("LPPC", area.identity().code());
    }

    @Test
    public void ensureAreaWithDifferentCodeAndNameIsCreated() {
        // Arrange & Act
        final GeographicBoundary spanishBoundary =
                new GeographicBoundary(36.0, 44.0, -9.5, 4.5);
        final AirControlArea area = new AirControlArea("LECM", "Madrid ACC", spanishBoundary);

        // Assert
        assertEquals("LECM", area.identity().code());
        assertEquals("Madrid ACC", area.name());
    }

    // =========================================================================
    // Invalid construction — area code
    // =========================================================================

    @Test
    public void ensureNullAreaCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AirControlArea(null, "Lisbon ACC", VALID_BOUNDARY);
        });
    }

    @Test
    public void ensureEmptyAreaCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AirControlArea("", "Lisbon ACC", VALID_BOUNDARY);
        });
    }

    @Test
    public void ensureBlankAreaCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AirControlArea("   ", "Lisbon ACC", VALID_BOUNDARY);
        });
    }

    // =========================================================================
    // Invalid construction — name
    // =========================================================================

    @Test
    public void ensureNullNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AirControlArea("LPPC", null, VALID_BOUNDARY);
        });
    }

    @Test
    public void ensureEmptyNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AirControlArea("LPPC", "", VALID_BOUNDARY);
        });
    }

    @Test
    public void ensureBlankNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AirControlArea("LPPC", "   ", VALID_BOUNDARY);
        });
    }

    // =========================================================================
    // Invalid construction — boundary
    // =========================================================================

    @Test
    public void ensureNullBoundaryIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AirControlArea("LPPC", "Lisbon ACC", null);
        });
    }

    // =========================================================================
    // Identity and sameAs
    // =========================================================================

    @Test
    public void ensureIdentityReturnsAreaCode() {
        // Arrange
        final AirControlArea area = new AirControlArea("LPPC", "Lisbon ACC", VALID_BOUNDARY);

        // Assert
        assertNotNull(area.identity());
        assertEquals("LPPC", area.identity().code());
    }

    @Test
    public void ensureTwoAreasWithSameCodeAreSame() {
        // Arrange — same code, different name (code is the identity)
        final AirControlArea a1 = new AirControlArea("LPPC", "Lisbon ACC",  VALID_BOUNDARY);
        final AirControlArea a2 = new AirControlArea("LPPC", "Lisboa ACC",  VALID_BOUNDARY);

        // Assert
        assertTrue(a1.sameAs(a2));
    }

    @Test
    public void ensureTwoAreasWithDifferentCodesAreNotSame() {
        // Arrange
        final AirControlArea a1 = new AirControlArea("LPPC", "Lisbon ACC",
                new GeographicBoundary(36.0, 42.0, -10.0, -6.0));
        final AirControlArea a2 = new AirControlArea("LECM", "Madrid ACC",
                new GeographicBoundary(36.0, 44.0, -9.5, 4.5));

        // Assert
        assertFalse(a1.sameAs(a2));
    }

    @Test
    public void ensureSameAsReturnsFalseForNonAirControlArea() {
        // Arrange
        final AirControlArea area = new AirControlArea("LPPC", "Lisbon ACC", VALID_BOUNDARY);

        // Assert
        assertFalse(area.sameAs("not an area"));
    }

    @Test
    public void ensureSameAsReturnsFalseForNull() {
        // Arrange
        final AirControlArea area = new AirControlArea("LPPC", "Lisbon ACC", VALID_BOUNDARY);

        // Assert
        assertFalse(area.sameAs(null));
    }

    // =========================================================================
    // toString
    // =========================================================================

    @Test
    public void ensureToStringContainsCodeAndName() {
        // Arrange
        final AirControlArea area = new AirControlArea("LPPC", "Lisbon ACC", VALID_BOUNDARY);

        // Act
        final String result = area.toString();

        // Assert
        assertTrue(result.contains("LPPC"));
        assertTrue(result.contains("Lisbon ACC"));
    }
}
