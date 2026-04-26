package domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link User} aggregate root.
 *
 * <p>Covers:</p>
 * <ul>
 *   <li>US031 — User registration business rules</li>
 *   <li>US032 — Enable/disable state transitions</li>
 *   <li>Domain invariants: password, phone, clearance date, skills assessment</li>
 * </ul>
 *
 * <p>Follows the AAA (Arrange – Act – Assert) convention.</p>
 */
class UserTest {

    // -------------------------------------------------------------------------
    // Shared test fixtures
    // -------------------------------------------------------------------------

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusYears(2);

    private EmailAddress email(final String address) {
        return EmailAddress.valueOf(address);
    }

    /** Returns a valid user with predictable data. */
    private User validUser() {
        return new User(
                email("joao@alsafe.com"),
                "Joao Silva",
                "910000001",
                "pass1234",
                "STAFF-001",
                FUTURE_DATE
        );
    }

    // -------------------------------------------------------------------------
    // US031.1 — Creation with valid data
    // -------------------------------------------------------------------------

    @Test
    void ensureNewUserIsCreatedAsActiveAndNotAdmin() {
        // Arrange / Act
        final User u = validUser();

        // Assert
        assertTrue(u.isActive(), "A new user must be active by default (US031.6)");
        assertFalse(u.isAdmin(), "A new user must not be admin by default");
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
        assertEquals("Joao Silva",  u.getName());
        assertEquals("910000001",   u.getPhoneNumber());
        assertEquals("STAFF-001",   u.getStaffNumber());
        assertEquals(FUTURE_DATE,   u.getSecurityClearanceExpiry());
    }

    @Test
    void ensureNewUserHasUserRole() {
        // Arrange / Act
        final User u = validUser();

        // Assert
        assertEquals("USER", u.getRole());
    }

    @Test
    void ensureSkillsAssessmentIsAutoSetToFiveYearsFromNow() {
        // Arrange / Act
        final User u = validUser();

        // Assert
        assertEquals(LocalDate.now().plusYears(5), u.getNextSkillsAssessmentDate());
    }

    // -------------------------------------------------------------------------
    // US031.1 / US031.3 / US031.4 — Validation failures
    // -------------------------------------------------------------------------

    @Test
    void ensureCreationFailsWhenEmailIsNull() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new User(null, "Name", "910000001", "pass1234", "S-001", FUTURE_DATE));
    }

    @Test
    void ensureCreationFailsWhenNameIsBlank() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "  ", "910000001", "pass1234", "S-001", FUTURE_DATE));
    }

    @Test
    void ensureCreationFailsWhenPasswordIsTooShort() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Name", "910000001", "ab", "S-001", FUTURE_DATE));
    }

    @Test
    void ensureCreationFailsWhenStaffNumberIsBlank() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Name", "910000001", "pass1234", "  ", FUTURE_DATE));
    }

    @Test
    void ensureCreationFailsWhenPhoneHasFewerThanNineDigits() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Name", "91000000", "pass1234", "S-001", FUTURE_DATE));
    }

    @Test
    void ensureCreationFailsWhenPhoneHasMoreThanNineDigits() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Name", "9100000001", "pass1234", "S-001", FUTURE_DATE));
    }

    @Test
    void ensureCreationFailsWhenPhoneContainsLetters() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Name", "91000000A", "pass1234", "S-001", FUTURE_DATE));
    }

    @Test
    void ensureCreationFailsWhenClearanceDateIsInThePast() {
        // Arrange
        final LocalDate pastDate = LocalDate.now().minusDays(1);

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Name", "910000001", "pass1234", "S-001", pastDate));
    }

    @Test
    void ensureCreationFailsWhenClearanceDateIsToday() {
        // Arrange
        final LocalDate today = LocalDate.now();

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Name", "910000001", "pass1234", "S-001", today));
    }

    // -------------------------------------------------------------------------
    // Password check
    // -------------------------------------------------------------------------

    @Test
    void ensureCorrectPasswordMatches() {
        // Arrange
        final User u = validUser();

        // Act / Assert
        assertTrue(u.checkPassword("pass1234"));
    }

    @Test
    void ensureWrongPasswordDoesNotMatch() {
        // Arrange
        final User u = validUser();

        // Act / Assert
        assertFalse(u.checkPassword("wrong"));
    }

    // -------------------------------------------------------------------------
    // US031.6 — Active state by default
    // -------------------------------------------------------------------------

    @Test
    void ensureNewUserIsActiveByDefault() {
        // Arrange / Act
        final User u = validUser();

        // Assert
        assertTrue(u.isActive());
    }

    // -------------------------------------------------------------------------
    // US032.1 / US032.2 — Disable and enable
    // -------------------------------------------------------------------------

    @Test
    void ensureDeactivatedUserIsInactive() {
        // Arrange
        final User u = validUser();

        // Act
        u.deactivate();

        // Assert
        assertFalse(u.isActive());
    }

    @Test
    void ensureReactivatedUserIsActive() {
        // Arrange
        final User u = validUser();
        u.deactivate();

        // Act
        u.activate();

        // Assert
        assertTrue(u.isActive());
    }

    // -------------------------------------------------------------------------
    // Admin role
    // -------------------------------------------------------------------------

    @Test
    void ensureMakeAdminSetsAdminFlagAndRole() {
        // Arrange
        final User u = validUser();
        assertFalse(u.isAdmin());

        // Act
        u.makeAdmin();

        // Assert
        assertTrue(u.isAdmin());
        assertEquals("ADMIN", u.getRole());
    }

    // -------------------------------------------------------------------------
    // Security clearance and skills assessment updates
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
    // sameAs / identity
    // -------------------------------------------------------------------------

    @Test
    void ensureSameEmailMeansSameUser() {
        // Arrange
        final User u1 = new User(email("x@alsafe.com"), "A", "910000001", "pass1234", "S-001", FUTURE_DATE);
        final User u2 = new User(email("x@alsafe.com"), "B", "910000002", "pass5678", "S-002", FUTURE_DATE);

        // Act / Assert
        assertTrue(u1.sameAs(u2));
    }

    @Test
    void ensureDifferentEmailMeansDifferentUser() {
        // Arrange
        final User u1 = validUser();
        final User u2 = new User(email("other@alsafe.com"), "Other", "910000002", "pass1234", "S-002", FUTURE_DATE);

        // Act / Assert
        assertFalse(u1.sameAs(u2));
    }

    @Test
    void ensureSameAsReturnsFalseForNonUser() {
        // Arrange
        final User u = validUser();

        // Act / Assert
        assertFalse(u.sameAs("not a user"));
    }
}
