package domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link User} aggregate root.
 *
 * <p>Covers:</p>
 * <ul>
 *   <li>US031 — Domain invariants for user registration</li>
 *   <li>US032 — Enable/disable state transitions</li>
 *   <li>Security clearance and skills assessment rules (section 3.1.1)</li>
 * </ul>
 *
 * <p>Note: authentication fields (password, admin flag) are now managed by
 * the EAPLI Framework's {@code SystemUser} and are not part of this entity.</p>
 *
 * <p>Follows the AAA (Arrange – Act – Assert) convention.</p>
 */
class UserTest {

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private static final LocalDate FUTURE = LocalDate.now().plusYears(2);

    private EmailAddress email(final String address) {
        return EmailAddress.valueOf(address);
    }

    /** Returns a valid user with predictable data. */
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
    // US031.1 — Creation with valid data
    // -------------------------------------------------------------------------

    @Test
    void ensureNewUserIsCreatedAsActive() {
        // Arrange / Act
        final User u = validUser();
        // Assert
        assertTrue(u.isActive(), "A new user must be active by default (US031.6)");
    }

    @Test
    void ensureNewUserHasBackofficeOperatorRole() {
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
    void ensureSkillsAssessmentIsAutoSetToFiveYearsFromNow() {
        // Arrange / Act
        final User u = validUser();
        // Assert
        assertEquals(LocalDate.now().plusYears(5), u.getNextSkillsAssessmentDate());
    }

    // -------------------------------------------------------------------------
    // US031 — Validation failures
    // -------------------------------------------------------------------------

    @Test
    void ensureCreationFailsWhenEmailIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(null, "Name", "910000001", "S-001", FUTURE));
    }

    @Test
    void ensureCreationFailsWhenNameIsBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "  ", "910000001", "S-001", FUTURE));
    }

    @Test
    void ensureCreationFailsWhenStaffNumberIsBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Name", "910000001", " ", FUTURE));
    }

    @Test
    void ensureCreationFailsWhenPhoneHasFewerThanNineDigits() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Name", "91000000", "S-001", FUTURE));
    }

    @Test
    void ensureCreationFailsWhenPhoneHasMoreThanNineDigits() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Name", "9100000001", "S-001", FUTURE));
    }

    @Test
    void ensureCreationFailsWhenPhoneContainsLetters() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Name", "91000000A", "S-001", FUTURE));
    }

    @Test
    void ensureCreationFailsWhenClearanceDateIsInThePast() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Name", "910000001", "S-001",
                        LocalDate.now().minusDays(1)));
    }

    @Test
    void ensureCreationFailsWhenClearanceDateIsToday() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(email("x@alsafe.com"), "Name", "910000001", "S-001",
                        LocalDate.now()));
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
    // markAsAdmin — display role
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

    // -------------------------------------------------------------------------
    // Security clearance and skills assessment (section 3.1.1)
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
        final User u = validUser();
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
        final User u = validUser();
        assertThrows(IllegalArgumentException.class, () ->
                u.updateSkillsAssessment(null));
    }

    // -------------------------------------------------------------------------
    // sameAs / identity
    // -------------------------------------------------------------------------

    @Test
    void ensureSameEmailMeansSameUser() {
        // Arrange
        final User u1 = new User(email("x@alsafe.com"), "A", "910000001", "S-001", FUTURE);
        final User u2 = new User(email("x@alsafe.com"), "B", "910000002", "S-002", FUTURE);
        // Act / Assert
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
    void ensureSameAsReturnsFalseForNonUser() {
        assertFalse(validUser().sameAs("not a user"));
    }
}
