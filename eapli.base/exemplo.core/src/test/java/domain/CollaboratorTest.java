package domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Collaborator} entity (US061–US064).
 * Follows the AAA convention (NFR03).
 */
class CollaboratorTest {

    private Collaborator buildCollaborator() {
        return new Collaborator("joao@tap.pt", "João Silva", "Manager");
    }

    // ── Construction ──────────────────────────────────────────────

    @Test
    void ensureValidCollaboratorIsCreated() {
        // Arrange & Act
        final Collaborator c = buildCollaborator();

        // Assert
        assertEquals("joao@tap.pt", c.email());
        assertEquals("João Silva",  c.name());
        assertEquals("Manager",     c.position());
    }

    @Test
    void ensureNewCollaboratorIsAlwaysActive() {
        // Arrange & Act
        final Collaborator c = buildCollaborator();

        // Assert — US061.5
        assertTrue(c.isActive());
    }

    @Test
    void ensureEmailIsNormalisedToLowerCase() {
        // Arrange & Act
        final Collaborator c = new Collaborator("JOAO@TAP.PT", "João Silva", "Manager");

        // Assert
        assertEquals("joao@tap.pt", c.email());
    }

    @Test
    void ensureNullEmailIsRejected() {
        assertThrows(Exception.class,
                () -> new Collaborator(null, "João Silva", "Manager"));
    }

    @Test
    void ensureEmptyEmailIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Collaborator("", "João Silva", "Manager"));
    }

    @Test
    void ensureBlankEmailIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Collaborator("   ", "João Silva", "Manager"));
    }

    @Test
    void ensureNullNameIsRejected() {
        assertThrows(Exception.class,
                () -> new Collaborator("joao@tap.pt", null, "Manager"));
    }

    @Test
    void ensureEmptyNameIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Collaborator("joao@tap.pt", "", "Manager"));
    }

    @Test
    void ensureNullPositionIsRejected() {
        assertThrows(Exception.class,
                () -> new Collaborator("joao@tap.pt", "João Silva", null));
    }

    @Test
    void ensureEmptyPositionIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Collaborator("joao@tap.pt", "João Silva", ""));
    }

    // ── disable() — US064 ─────────────────────────────────────────

    @Test
    void ensureDisableSetsActiveFalse() {
        // Arrange
        final Collaborator c = buildCollaborator();

        // Act
        c.disable();

        // Assert
        assertFalse(c.isActive());
    }

    @Test
    void ensureDisableIsIdempotent() {
        // Arrange
        final Collaborator c = buildCollaborator();
        c.disable();

        // Act & Assert — US064.5: second call must not throw
        assertDoesNotThrow(c::disable);
        assertFalse(c.isActive());
    }

    // ── updateEmail() — US063 ─────────────────────────────────────

    @Test
    void ensureUpdateEmailChangesEmail() {
        // Arrange
        final Collaborator c = buildCollaborator();

        // Act
        c.updateEmail("novo@tap.pt");

        // Assert
        assertEquals("novo@tap.pt", c.email());
    }

    @Test
    void ensureUpdateEmailNormalisesToLowerCase() {
        // Arrange
        final Collaborator c = buildCollaborator();

        // Act
        c.updateEmail("NOVO@TAP.PT");

        // Assert
        assertEquals("novo@tap.pt", c.email());
    }

    @Test
    void ensureUpdateEmailWithNullThrows() {
        final Collaborator c = buildCollaborator();
        assertThrows(Exception.class, () -> c.updateEmail(null));
    }

    @Test
    void ensureUpdateEmailWithEmptyThrows() {
        final Collaborator c = buildCollaborator();
        assertThrows(IllegalArgumentException.class, () -> c.updateEmail(""));
    }

    // ── Immutability — US063 ──────────────────────────────────────

    @Test
    void ensureNameHasNoSetterMethod() {
        // Assert structurally: Collaborator must not expose setName()
        assertThrows(NoSuchMethodException.class,
                () -> Collaborator.class.getDeclaredMethod("setName", String.class));
    }

    @Test
    void ensurePositionHasNoSetterMethod() {
        assertThrows(NoSuchMethodException.class,
                () -> Collaborator.class.getDeclaredMethod("setPosition", String.class));
    }

    // ── Equality ──────────────────────────────────────────────────

    @Test
    void ensureTwoCollaboratorsWithSameEmailAreEqual() {
        // Arrange
        final Collaborator c1 = new Collaborator("joao@tap.pt", "João Silva", "Manager");
        final Collaborator c2 = new Collaborator("joao@tap.pt", "João S.",    "Analyst");

        // Assert — equality is by email only
        assertEquals(c1, c2);
    }

    @Test
    void ensureTwoCollaboratorsWithDifferentEmailsAreNotEqual() {
        final Collaborator c1 = new Collaborator("joao@tap.pt",  "João",  "Manager");
        final Collaborator c2 = new Collaborator("maria@tap.pt", "Maria", "Analyst");
        assertNotEquals(c1, c2);
    }
}