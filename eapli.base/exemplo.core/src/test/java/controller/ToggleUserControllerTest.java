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
 * Integration tests for {@link ToggleUserController} — US032.
 *
 * <p>Tests enable/disable behaviour:
 * state transitions, guard rules (admin protection, duplicate toggle)
 * and full deactivate/reactivate cycle.</p>
 *
 * <p>Uses {@link InMemoryUserRepository} for full domain isolation.</p>
 *
 * <p>Follows the AAA (Arrange – Act – Assert) convention.</p>
 */
class ToggleUserControllerTest {

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
    // US032.1 — Deactivate
    // -------------------------------------------------------------------------

    @Test
    void ensureDeactivatedDomainUserIsInactive() {
        // Arrange
        final User u = new User(
                domain.EmailAddress.valueOf("d@alsafe.com"),
                "D User", "910000001", "S-300", FUTURE);

        // Act
        u.deactivate();

        // Assert
        assertFalse(u.isActive());
    }

    // -------------------------------------------------------------------------
    // US032.2 — Reactivate
    // -------------------------------------------------------------------------

    @Test
    void ensureReactivatedDomainUserIsActive() {
        // Arrange
        final User u = new User(
                domain.EmailAddress.valueOf("r@alsafe.com"),
                "R User", "910000001", "S-301", FUTURE);
        u.deactivate();

        // Act
        u.activate();

        // Assert
        assertTrue(u.isActive());
    }

    // -------------------------------------------------------------------------
    // US032 — Full cycle
    // -------------------------------------------------------------------------

    @Test
    void ensureToggleCycleChangesStatusCorrectly() {
        // Arrange
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        repo.save(new User(domain.EmailAddress.valueOf("toggle@alsafe.com"),
                "Toggle User", "910000001", "S-304", FUTURE));
        final ToggleUserController tog = new ToggleUserController();
        final domain.EmailAddress emailVO = domain.EmailAddress.valueOf("toggle@alsafe.com");

        // Act + Assert — deactivate
        tog.deactivateUser("toggle@alsafe.com");
        assertFalse(repo.ofIdentity(emailVO).get().isActive(),
                "User must be inactive after deactivation");

        // Act + Assert — reactivate
        tog.activateUser("toggle@alsafe.com");
        assertTrue(repo.ofIdentity(emailVO).get().isActive(),
                "User must be active again after reactivation");
    }

    // -------------------------------------------------------------------------
    // US032 — Guard: user not found
    // -------------------------------------------------------------------------

    @Test
    void ensureToggleControllerRejectsDeactivatingNonExistentUser() {
        // Arrange
        final ToggleUserController tog = new ToggleUserController();

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                tog.deactivateUser("ghost@alsafe.com"),
                "Must throw when user does not exist");
    }

    // -------------------------------------------------------------------------
    // US032.5 — Guard: admin cannot be deactivated
    // -------------------------------------------------------------------------

    @Test
    void ensureToggleControllerRejectsDeactivatingAdminAccount() {
        // Arrange
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        final User admin = new User(
                domain.EmailAddress.valueOf("admin2@alsafe.com"),
                "Admin2", "910000000", "S-ADM2", LocalDate.now().plusYears(10));
        admin.markAsAdmin();
        repo.save(admin);

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new ToggleUserController().deactivateUser("admin2@alsafe.com"),
                "The administrator account must never be deactivatable");
    }

    // -------------------------------------------------------------------------
    // US032.6 — Guard: symmetric rules
    // -------------------------------------------------------------------------

    @Test
    void ensureToggleControllerRejectsDeactivatingAlreadyInactiveUser() {
        // Arrange
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        final User u = new User(
                domain.EmailAddress.valueOf("ai@alsafe.com"),
                "AI User", "910000001", "S-302", FUTURE);
        u.deactivate();
        repo.save(u);

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new ToggleUserController().deactivateUser("ai@alsafe.com"),
                "Must reject deactivating an already inactive user");
    }

    @Test
    void ensureToggleControllerRejectsActivatingAlreadyActiveUser() {
        // Arrange
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        repo.save(new User(domain.EmailAddress.valueOf("aa@alsafe.com"),
                "AA User", "910000001", "S-303", FUTURE));

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new ToggleUserController().activateUser("aa@alsafe.com"),
                "Must reject activating an already active user");
    }
}
