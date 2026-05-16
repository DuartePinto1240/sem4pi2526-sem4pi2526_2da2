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

    // Valid construction

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

    // Invalid construction — area code

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

    // Invalid construction — name

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

    // Invalid construction — boundary

    @Test
    public void ensureNullBoundaryIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AirControlArea("LPPC", "Lisbon ACC", null);
        });
    }

    // Identity and sameAs

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
        final AirControlArea a1 = new AirControlArea("LPPC", "Lisbon ACC", VALID_BOUNDARY);
        final AirControlArea a2 = new AirControlArea("LPPC", "Lisboa ACC", VALID_BOUNDARY);

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

    // toString

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


// ── Collaborator management (US061–US064) ─────────────────────

    @Test
    public void ensureAddCollaboratorToAreaSuccessfully() {
        // Arrange
        final AirControlArea area = new AirControlArea("LPPC", "Lisbon ACC", VALID_BOUNDARY);

        // Act
        area.addCollaborator("fco@lppc.pt", "Maria Santos", "Senior FCO");

        // Assert — US061
        assertTrue(area.findCollaboratorByEmail("fco@lppc.pt").isPresent());
    }

    @Test
    public void ensureNewAreaCollaboratorIsActive() {
        // Arrange
        final AirControlArea area = new AirControlArea("LPPC", "Lisbon ACC", VALID_BOUNDARY);

        // Act
        area.addCollaborator("fco@lppc.pt", "Maria Santos", "Senior FCO");

        // Assert — US061.5
        assertTrue(area.findCollaboratorByEmail("fco@lppc.pt").get().isActive());
    }

    @Test
    public void ensureAddDuplicateEmailToAreaThrows() {
        // Arrange
        final AirControlArea area = new AirControlArea("LPPC", "Lisbon ACC", VALID_BOUNDARY);
        area.addCollaborator("fco@lppc.pt", "Maria Santos", "Senior FCO");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> area.addCollaborator("fco@lppc.pt", "Other Person", "Junior FCO"));
    }

    @Test
    public void ensureListActiveAreaCollaboratorsExcludesDisabled() {
        // Arrange
        final AirControlArea area = new AirControlArea("LPPC", "Lisbon ACC", VALID_BOUNDARY);
        area.addCollaborator("active@lppc.pt", "Active", "Senior FCO");
        area.addCollaborator("disabled@lppc.pt", "Disabled", "Junior FCO");
        area.disableCollaborator("disabled@lppc.pt");

        // Act
        final var list = area.listActiveCollaborators();

        // Assert — US062
        assertEquals(1, list.size());
        assertEquals("active@lppc.pt", list.get(0).email);
    }

    @Test
    public void ensureUpdateAreaCollaboratorEmailSuccessfully() {
        // Arrange
        final AirControlArea area = new AirControlArea("LPPC", "Lisbon ACC", VALID_BOUNDARY);
        area.addCollaborator("old@lppc.pt", "Maria", "Senior FCO");

        // Act — US063
        area.updateCollaboratorEmail("old@lppc.pt", "new@lppc.pt");

        // Assert
        assertFalse(area.findCollaboratorByEmail("old@lppc.pt").isPresent());
        assertTrue(area.findCollaboratorByEmail("new@lppc.pt").isPresent());
    }

    @Test
    public void ensureDisableAreaCollaboratorSetsActiveFalse() {
        // Arrange
        final AirControlArea area = new AirControlArea("LPPC", "Lisbon ACC", VALID_BOUNDARY);
        area.addCollaborator("fco@lppc.pt", "Maria", "Senior FCO");

        // Act — US064
        area.disableCollaborator("fco@lppc.pt");

        // Assert
        assertFalse(area.findCollaboratorByEmail("fco@lppc.pt").get().isActive());
    }

    @Test
    public void ensureDisableAreaCollaboratorIsIdempotent() {
        // Arrange
        final AirControlArea area = new AirControlArea("LPPC", "Lisbon ACC", VALID_BOUNDARY);
        area.addCollaborator("fco@lppc.pt", "Maria", "Senior FCO");
        area.disableCollaborator("fco@lppc.pt");

        // Act & Assert — US064.5
        assertDoesNotThrow(() -> area.disableCollaborator("fco@lppc.pt"));
    }

    @Test
    public void ensureAreaCollaboratorRecordRetainedAfterDisable() {
        // Arrange
        final AirControlArea area = new AirControlArea("LPPC", "Lisbon ACC", VALID_BOUNDARY);
        area.addCollaborator("fco@lppc.pt", "Maria", "Senior FCO");

        // Act — US064 soft delete
        area.disableCollaborator("fco@lppc.pt");

        // Assert
        assertTrue(area.findCollaboratorByEmail("fco@lppc.pt").isPresent());
        assertFalse(area.findCollaboratorByEmail("fco@lppc.pt").get().isActive());
    }
}