package controller;

import domain.AlSafePasswordPolicy;
import domain.User;
import domain.ValidEmailDomains;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistence.PersistenceContext;
import persistence.impl.inmemory.InMemoryUserRepository;
import repositories.UserRepository;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link RegisterUserController} — US031.
 *
 * <p>Tests domain invariants for user registration:
 * valid creation, uniqueness constraints, validation failures
 * and bootstrap admin creation.</p>
 *
 * <p>Uses {@link InMemoryUserRepository} for full domain isolation.</p>
 *
 * <p>Follows the AAA (Arrange – Act – Assert) convention.</p>
 */
class RegisterUserControllerTest {

    private static final LocalDate FUTURE           = LocalDate.now().plusYears(2);
    private static final String    PERSISTENCE_UNIT = "eapli.base.AlSafePU";

    @BeforeAll
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void setUpFramework() {
        try {
            AuthzRegistry.configure(
                    new JpaAutoTxUserRepository(PERSISTENCE_UNIT, new HashMap<>()),
                    new AlSafePasswordPolicy(),
                    new PlainTextEncoder()
            );
        } catch (final Exception e) {
            // already configured — ignore
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        final Field instanceField = PersistenceContext.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        final PersistenceContext ctx = PersistenceContext.getInstance();
        final Field repoField = PersistenceContext.class.getDeclaredField("userRepository");
        repoField.setAccessible(true);
        repoField.set(ctx, new InMemoryUserRepository());

        final Field domainsField = ValidEmailDomains.class.getDeclaredField("DOMAINS");
        domainsField.setAccessible(true);
        ((java.util.Set<?>) domainsField.get(null)).clear();
        ValidEmailDomains.register("alsafe.com");

        AuthzRegistry.authorizationService().clearSession();
    }

    // -------------------------------------------------------------------------
    // US031.1 / US031.6 — Creation with valid data
    // -------------------------------------------------------------------------

    @Test
    void ensureDomainUserIsCreatedWithCorrectData() {
        // Arrange / Act
        final User u = new User(
                domain.EmailAddress.valueOf("new@alsafe.com"),
                "New User", "910000001", "S-100", FUTURE);

        // Assert
        assertNotNull(u);
        assertEquals("New User", u.getName());
        assertTrue(u.isActive());
        assertEquals("BACKOFFICE_OPERATOR", u.getRole());
        assertNotNull(u.getNextSkillsAssessmentDate());
    }

    @Test
    void ensureNewUserIsActiveByDefault() {
        // Arrange / Act (US031.6)
        final User u = new User(
                domain.EmailAddress.valueOf("active@alsafe.com"),
                "Active User", "910000001", "S-101", FUTURE);

        // Assert
        assertTrue(u.isActive());
    }

    // -------------------------------------------------------------------------
    // US031.7 — Bootstrap admin
    // -------------------------------------------------------------------------

    @Test
    void ensureAdminDomainUserHasAdminRole() {
        // Arrange
        final User admin = new User(
                domain.EmailAddress.valueOf("adm@alsafe.com"),
                "Admin", "910000000", "S-ADMIN", LocalDate.now().plusYears(10));

        // Act
        admin.markAsAdmin();

        // Assert
        assertEquals("ADMIN", admin.getRole());
    }

    // -------------------------------------------------------------------------
    // Section 3.1.1 — Skills assessment auto-calculation
    // -------------------------------------------------------------------------

    @Test
    void ensureSkillsAssessmentIsAutoSetToFiveYears() {
        // Arrange / Act
        final User u = new User(
                domain.EmailAddress.valueOf("assess@alsafe.com"),
                "User", "910000001", "S-102", FUTURE);

        // Assert
        assertEquals(LocalDate.now().plusYears(5), u.getNextSkillsAssessmentDate());
    }

    // -------------------------------------------------------------------------
    // US031.2 — Uniqueness constraints
    // -------------------------------------------------------------------------

    @Test
    void ensureDomainRepositoryRejectsDuplicateEmail() {
        // Arrange
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        final domain.EmailAddress email = domain.EmailAddress.valueOf("dup@alsafe.com");
        repo.save(new User(email, "User A", "910000001", "S-103", FUTURE));
        repo.save(new User(email, "User B", "910000002", "S-104", FUTURE));

        // Assert — only one record per email
        final long count = java.util.stream.StreamSupport
                .stream(repo.findAll().spliterator(), false)
                .filter(u -> u.identity().equals(email))
                .count();
        assertEquals(1, count,
                "Only one domain User must exist per email address");
    }

    @Test
    void ensureDomainRepositoryRejectsDuplicatePhone() {
        // Arrange
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        repo.save(new User(domain.EmailAddress.valueOf("a@alsafe.com"),
                "User A", "910000001", "S-105", FUTURE));

        // Act / Assert
        assertThrows(Exception.class, () ->
                repo.save(new User(domain.EmailAddress.valueOf("b@alsafe.com"),
                        "User B", "910000001", "S-106", FUTURE)),
                "Duplicate phone number must be rejected");
    }

    @Test
    void ensureDomainRepositoryRejectsDuplicateStaffNumber() {
        // Arrange
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        repo.save(new User(domain.EmailAddress.valueOf("c@alsafe.com"),
                "User C", "910000001", "S-SAME", FUTURE));

        // Act / Assert
        assertThrows(Exception.class, () ->
                repo.save(new User(domain.EmailAddress.valueOf("d@alsafe.com"),
                        "User D", "910000002", "S-SAME", FUTURE)),
                "Duplicate staff number must be rejected");
    }

    // -------------------------------------------------------------------------
    // US031.4 — Phone validation
    // -------------------------------------------------------------------------

    @Test
    void ensureUserCreationFailsWithInvalidPhone() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new User(domain.EmailAddress.valueOf("x@alsafe.com"),
                        "Name", "12345", "S-107", FUTURE));
    }

    // -------------------------------------------------------------------------
    // Section 3.1.1 — Clearance date validation
    // -------------------------------------------------------------------------

    @Test
    void ensureUserCreationFailsWithPastClearanceDate() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new User(domain.EmailAddress.valueOf("x@alsafe.com"),
                        "Name", "910000001", "S-108",
                        LocalDate.now().minusDays(1)));
    }
}
