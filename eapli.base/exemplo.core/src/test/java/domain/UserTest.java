package domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link User} aggregate root.
 *
 * <p>These are <strong>pure unit tests</strong> — no framework, no database,
 * no repositories. Only the domain aggregate and its value objects are used.</p>
 *
 * <p>Covers:</p>
 * <ul>
 *   <li>US031 — User registration domain invariants</li>
 *   <li>US032 — Enable/disable state transitions</li>
 *   <li>Section 3.1.1 — Security clearance and skills assessment rules</li>
 * </ul>
 *
 * <p>Follows the AAA (Arrange – Act – Assert) convention.</p>
 */
class UserTest {

    // -------------------------------------------------------------------------
    // Shared fixtures
    // -------------------------------------------------------------------------

    private static final LocalDate FUTURE = LocalDate.now().plusYears(2);

    private EmailAddress email(final String address) {
        return EmailAddress.valueOf(address);
    }

    /** Returns a valid, fully constructed user with predictable data. */
    private User validUser() {
        return new User(
                email("joao@alsafe.com"),
                "Joao Silva",
                "910000001",
                "STAFF-001",
                FUTURE
        );
    }

    // -------------------------------------------------------------------------
    // US031 — Creation with valid data (happy path)
    // -------------------------------------------------------------------------

    @Test
    void ensureNewUserIsCreatedAsActive() {
        // Arrange / Act
        final User u = validUser();
        // Assert (US031.6 — new user must be active by default)
        assertTrue(u.isActive());
    }

    @Test
    void ensureNewUserHasBackofficeOperatorRoleByDefault() {
        // Arrange / Act
        final User u = validUser();
        // Assert
        assertEquals("BACKOFFICE_OPERATOR", u.getRole());
    }

    @Test
    void ensureUserIdentityIsEmail() {
        // Arrange / Act
        final User u = validUser();
        // Assert
        assertEquals("joao@alsafe.com", u.identity().toString());
    }

    @Test
    void ensureGettersReturnCorrectValues() {
        // Arrange / Act
        final User u = validUser();
        // Assert
        assertEquals("Joao Silva", u.getName());
        assertEquals("910000001",  u.getPhoneNumber());
        assertEquals("STAFF-001",  u.getStaffNumber());
        assertEquals(FUTURE,       u.getSecurityClearanceExpiry());
    }

    @Test
    void ensureSkillsAssessmentIsAutoSetToFiveYearsFromCreation() {
        // Arrange / Act (section 3.1.1 — "periodic, per regulations 5 years")
        final User u = validUser();
        // Assert
        assertEquals(LocalDate.now().plusYears(5), u.getNextSkillsAssessmentDate());
    }

    // -------------------------------------------------------------------------
    // US031 — Validation failures (sad path)
    // -------------------------------------------------------------------------

    @Test
    void ensureCreationFailsWhenEmailIsNull() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new User(null, "Name", "910000001", "S-001", FUTURE));
    }

    @Test
    void ensureCreationFailsWhenNameIsBlank() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "  ", "910000001", "S-001", FUTURE));
    }

    @Test
    void ensureCreationFailsWhenStaffNumberIsBlank() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Name", "910000001", " ", FUTURE));
    }

    @Test
    void ensureCreationFailsWhenPhoneHasFewerThanNineDigits() {
        // Arrange / Act / Assert (US031.4)
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Name", "91000000", "S-001", FUTURE));
    }

    @Test
    void ensureCreationFailsWhenPhoneHasMoreThanNineDigits() {
        // Arrange / Act / Assert (US031.4)
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Name", "9100000001", "S-001", FUTURE));
    }

    @Test
    void ensureCreationFailsWhenPhoneContainsLetters() {
        // Arrange / Act / Assert (US031.4)
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Name", "91000000A", "S-001", FUTURE));
    }

    @Test
    void ensureCreationFailsWhenClearanceDateIsInThePast() {
        // Arrange
        final LocalDate past = LocalDate.now().minusDays(1);
        // Act / Assert (section 3.1.1 — clearance must be future)
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Name", "910000001", "S-001", past));
    }

    @Test
    void ensureCreationFailsWhenClearanceDateIsToday() {
        // Arrange
        final LocalDate today = LocalDate.now();
        // Act / Assert (today is not "in the future")
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Name", "910000001", "S-001", today));
    }

    // -------------------------------------------------------------------------
    // US032 — Enable/disable state transitions
    // -------------------------------------------------------------------------

    @Test
    void ensureDeactivatedUserIsInactive() {
        // Arrange
        final User u = validUser();
        // Act (US032.1)
        u.deactivate();
        // Assert
        assertFalse(u.isActive());
    }

    @Test
    void ensureReactivatedUserIsActive() {
        // Arrange
        final User u = validUser();
        u.deactivate();
        // Act (US032.2)
        u.activate();
        // Assert
        assertTrue(u.isActive());
    }

    @Test
    void ensureNewUserIsActiveBeforeAnyToggle() {
        // Arrange / Act
        final User u = validUser();
        // Assert — baseline for US032 tests
        assertTrue(u.isActive());
    }

    // -------------------------------------------------------------------------
    // US031 — Role assignment
    // -------------------------------------------------------------------------

    @Test
    void ensureMarkAsAdminSetsRoleToAdmin() {
        // Arrange
        final User u = validUser();
        assertEquals("BACKOFFICE_OPERATOR", u.getRole());
        // Act
        u.markAsAdmin();
        // Assert
        assertEquals("ADMIN", u.getRole());
    }

    @Test
    void ensureSetRoleChangesDisplayRole() {
        // Arrange
        final User u = validUser();
        // Act
        u.setRole("PILOT");
        // Assert
        assertEquals("PILOT", u.getRole());
    }

    @Test
    void ensureSetRoleFailsWhenBlank() {
        // Arrange
        final User u = validUser();
        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> u.setRole("  "));
    }

    // -------------------------------------------------------------------------
    // Section 3.1.1 — Security clearance and skills assessment updates
    // -------------------------------------------------------------------------

    @Test
    void ensureSecurityClearanceCanBeUpdatedWithFutureDate() {
        // Arrange
        final User u = validUser();
        final LocalDate newDate = LocalDate.now().plusYears(4);
        // Act
        u.updateSecurityClearance(newDate);
        // Assert
        assertEquals(newDate, u.getSecurityClearanceExpiry());
    }

    @Test
    void ensureSecurityClearanceUpdateFailsWithPastDate() {
        // Arrange
        final User u = validUser();
        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                u.updateSecurityClearance(LocalDate.now().minusDays(1)));
    }

    @Test
    void ensureSecurityClearanceUpdateFailsWithNull() {
        // Arrange
        final User u = validUser();
        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                u.updateSecurityClearance(null));
    }

    @Test
    void ensureSkillsAssessmentCanBeUpdated() {
        // Arrange
        final User u = validUser();
        final LocalDate newDate = LocalDate.now().plusYears(7);
        // Act
        u.updateSkillsAssessment(newDate);
        // Assert
        assertEquals(newDate, u.getNextSkillsAssessmentDate());
    }

    @Test
    void ensureSkillsAssessmentUpdateFailsWithNull() {
        // Arrange
        final User u = validUser();
        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                u.updateSkillsAssessment(null));
    }

    // -------------------------------------------------------------------------
    // sameAs / identity — aggregate root invariants
    // -------------------------------------------------------------------------

    @Test
    void ensureSameEmailMeansSameUser() {
        // Arrange
        final User u1 = new User(email("x@alsafe.com"), "A", "910000001", "S-001", FUTURE);
        final User u2 = new User(email("x@alsafe.com"), "B", "910000002", "S-002", FUTURE);
        // Act / Assert (identity is email — DDD aggregate root rule)
        assertTrue(u1.sameAs(u2));
    }

    @Test
    void ensureDifferentEmailMeansDifferentUser() {
        // Arrange
        final User u1 = validUser();
        final User u2 = new User(email("other@alsafe.com"), "Other", "910000002", "S-002", FUTURE);
        // Act / Assert
        assertFalse(u1.sameAs(u2));
    }

    @Test
    void ensureSameAsReturnsFalseForNonUserObject() {
        // Arrange
        final User u = validUser();
        // Act / Assert
        assertFalse(u.sameAs("not a user"));
    }

    @Test
    void ensureSameAsReturnsFalseForNull() {
        // Arrange
        final User u = validUser();
        // Act / Assert
        assertFalse(u.sameAs(null));
    }
}