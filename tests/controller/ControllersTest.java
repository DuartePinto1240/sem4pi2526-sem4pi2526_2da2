package controller;

import domain.AlSafePasswordPolicy;
import domain.User;
import domain.ValidEmailDomains;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.UserRepository;
import persistence.PersistenceContext;
import persistence.impl.inmemory.InMemoryUserRepository;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for US030, US031, US032 and US033 controllers.
 *
 * <p>These are integration tests because {@link RegisterUserController} and
 * {@link LoginController} interact with the EAPLI Framework's
 * {@code AuthzRegistry} (which manages SystemUser persistence via JPA).</p>
 *
 * <p>The EAPLI Framework is configured once per test class using H2.
 * Our domain {@link PersistenceContext} is reset before each test via
 * reflection to ensure domain-level isolation.</p>
 *
 * <p>Follows the AAA (Arrange – Act – Assert) convention.</p>
 */
class ControllersTest {

    private static final LocalDate FUTURE = LocalDate.now().plusYears(2);
    private static final String PERSISTENCE_UNIT = "eapli.base.AlSafePU";

    /**
     * Configure the EAPLI Framework's AuthzRegistry once for all tests.
     * Uses the same H2 file-based persistence unit as the main application.
     */
    @BeforeAll
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void setUpFramework() {
        AuthzRegistry.configure(
                new JpaAutoTxUserRepository(PERSISTENCE_UNIT, new HashMap<>()),
                new AlSafePasswordPolicy(),
                new PlainTextEncoder()
        );
    }

    /**
     * Before each test:
     * 1. Reset PersistenceContext singleton and inject a fresh InMemoryUserRepository.
     * 2. Reset ValidEmailDomains registry.
     * 3. Clear the framework session.
     */
    @BeforeEach
    void setUp() throws Exception {
        // Reset PersistenceContext singleton
        final Field instanceField = PersistenceContext.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        // Inject fresh InMemoryUserRepository into the new singleton
        final PersistenceContext ctx = PersistenceContext.getInstance();
        final Field repoField = PersistenceContext.class.getDeclaredField("userRepository");
        repoField.setAccessible(true);
        repoField.set(ctx, new InMemoryUserRepository());

        // Reset ValidEmailDomains
        final Field domainsField = ValidEmailDomains.class.getDeclaredField("DOMAINS");
        domainsField.setAccessible(true);
        ((java.util.Set<?>) domainsField.get(null)).clear();
        ValidEmailDomains.register("alsafe.com");

        // Clear any active session
        AuthzRegistry.authorizationService().clearSession();
    }

    // =========================================================================
    // US031 — Domain User registration (domain layer only)
    // =========================================================================

    @Test
    void ensureDomainUserIsCreatedWithCorrectData() {
        // Arrange / Act
        final User u = new User(
                domain.EmailAddress.valueOf("new@alsafe.com"),
                "New User", "910000001", "S-100", FUTURE);

        // Assert (US031.1, US031.6)
        assertNotNull(u);
        assertEquals("New User", u.getName());
        assertTrue(u.isActive());
        assertEquals("BACKOFFICE_OPERATOR", u.getRole());
        assertNotNull(u.getNextSkillsAssessmentDate());
    }

    @Test
    void ensureAdminDomainUserHasAdminRole() {
        // Arrange
        final User admin = new User(
                domain.EmailAddress.valueOf("admin@alsafe.com"),
                "Admin", "910000000", "S-ADMIN", LocalDate.now().plusYears(10));

        // Act (US031.7)
        admin.markAsAdmin();

        // Assert
        assertEquals("ADMIN", admin.getRole());
    }

    @Test
    void ensureSkillsAssessmentIsAutoSetToFiveYears() {
        // Arrange / Act
        final User u = new User(
                domain.EmailAddress.valueOf("assess@alsafe.com"),
                "User", "910000001", "S-101", FUTURE);

        // Assert
        assertEquals(LocalDate.now().plusYears(5), u.getNextSkillsAssessmentDate());
    }

    // =========================================================================
    // US031 — RegisterUserController (domain repository level)
    // =========================================================================

    @Test
    void ensureDomainUserRepositoryRejectsDuplicatePhone() {
        // Arrange
        final repositories.UserRepository repo =
                PersistenceContext.getInstance().userRepository();
        repo.save(new User(
                domain.EmailAddress.valueOf("a@alsafe.com"),
                "User A", "910000001", "S-101", FUTURE));

        // Act / Assert
        assertThrows(Exception.class, () ->
                repo.save(new User(
                        domain.EmailAddress.valueOf("b@alsafe.com"),
                        "User B", "910000001", "S-102", FUTURE)));
    }

    @Test
    void ensureDomainUserRepositoryRejectsDuplicateStaffNumber() {
        // Arrange
        final repositories.UserRepository repo =
                PersistenceContext.getInstance().userRepository();
        repo.save(new User(
                domain.EmailAddress.valueOf("a@alsafe.com"),
                "User A", "910000001", "S-SAME", FUTURE));

        // Act / Assert
        assertThrows(Exception.class, () ->
                repo.save(new User(
                        domain.EmailAddress.valueOf("b@alsafe.com"),
                        "User B", "910000002", "S-SAME", FUTURE)));
    }

    @Test
    void ensureUserCreationFailsWithInvalidPhone() {
        // Arrange / Act / Assert (US031.4)
        assertThrows(IllegalArgumentException.class, () ->
                new User(domain.EmailAddress.valueOf("x@alsafe.com"),
                        "Name", "12345", "S-103", FUTURE));
    }

    @Test
    void ensureUserCreationFailsWithPastClearanceDate() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new User(domain.EmailAddress.valueOf("x@alsafe.com"),
                        "Name", "910000001", "S-104",
                        LocalDate.now().minusDays(1)));
    }

    // =========================================================================
    // US030 — Login (framework integration)
    // =========================================================================

    @Test
    void ensureLoginFailsWithUnknownEmail() {
        // Arrange (no user registered)
        final LoginController login = new LoginController();

        // Act
        final Optional<UserSession> result = login.login("nobody@alsafe.com", "pass");

        // Assert (US030.3)
        assertTrue(result.isEmpty());
    }

    @Test
    void ensureLoginFailsWithWrongPassword() {
        // Arrange — create SystemUser via framework
        try {
            final java.util.Set<eapli.framework.infrastructure.authz.domain.model.Role> roles = new java.util.HashSet<>();
            roles.add(domain.BaseRoles.BACKOFFICE_OPERATOR);
            AuthzRegistry.userService().registerNewUser(
                    "wp@alsafe.com", "pass1234", "WP User", "", "wp@alsafe.com", roles);
        } catch (Exception e) {
            // user may already exist from a previous test run
        }

        // Act
        final Optional<UserSession> result = new LoginController().login("wp@alsafe.com", "wrong");

        // Assert
        assertTrue(result.isEmpty());
    }

    // =========================================================================
    // US032 — Enable/Disable (domain layer)
    // =========================================================================

    @Test
    void ensureDeactivatedDomainUserIsInactive() {
        // Arrange (US032.1)
        final User u = new User(
                domain.EmailAddress.valueOf("d@alsafe.com"),
                "D User", "910000001", "S-300", FUTURE);

        // Act
        u.deactivate();

        // Assert
        assertFalse(u.isActive());
    }

    @Test
    void ensureReactivatedDomainUserIsActive() {
        // Arrange (US032.2)
        final User u = new User(
                domain.EmailAddress.valueOf("r@alsafe.com"),
                "R User", "910000001", "S-301", FUTURE);
        u.deactivate();

        // Act
        u.activate();

        // Assert
        assertTrue(u.isActive());
    }

    @Test
    void ensureToggleControllerRejectsDeactivatingNonExistentUser() {
        // Arrange (US032)
        final ToggleUserController tog = new ToggleUserController();

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                tog.deactivateUser("ghost@alsafe.com"));
    }

    @Test
    void ensureToggleControllerRejectsDeactivatingAdminRole() {
        // Arrange
        final repositories.UserRepository repo =
                PersistenceContext.getInstance().userRepository();
        final User admin = new User(
                domain.EmailAddress.valueOf("admin2@alsafe.com"),
                "Admin2", "910000000", "S-ADM2", LocalDate.now().plusYears(10));
        admin.markAsAdmin();
        repo.save(admin);

        final ToggleUserController tog = new ToggleUserController();

        // Act / Assert (US032.5)
        assertThrows(IllegalArgumentException.class, () ->
                tog.deactivateUser("admin2@alsafe.com"));
    }

    @Test
    void ensureToggleControllerRejectsDeactivatingAlreadyInactiveUser() {
        // Arrange (US032.6)
        final repositories.UserRepository repo =
                PersistenceContext.getInstance().userRepository();
        final User u = new User(
                domain.EmailAddress.valueOf("ai@alsafe.com"),
                "AI User", "910000001", "S-302", FUTURE);
        u.deactivate();
        repo.save(u);

        final ToggleUserController tog = new ToggleUserController();

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                tog.deactivateUser("ai@alsafe.com"));
    }

    @Test
    void ensureToggleControllerRejectsActivatingAlreadyActiveUser() {
        // Arrange (US032.6 — symmetric)
        final repositories.UserRepository repo =
                PersistenceContext.getInstance().userRepository();
        repo.save(new User(
                domain.EmailAddress.valueOf("aa@alsafe.com"),
                "AA User", "910000001", "S-303", FUTURE));

        final ToggleUserController tog = new ToggleUserController();

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                tog.activateUser("aa@alsafe.com"));
    }

    // =========================================================================
    // US033 — List Users
    // =========================================================================

    @Test
    void ensureListReturnsAllUsersIncludingInactive() {
        // Arrange (US033.1, US033.3)
        final repositories.UserRepository repo =
                PersistenceContext.getInstance().userRepository();
        repo.save(new User(domain.EmailAddress.valueOf("act1@alsafe.com"),
                "Active One",   "910000001", "S-400", FUTURE));
        repo.save(new User(domain.EmailAddress.valueOf("act2@alsafe.com"),
                "Active Two",   "910000002", "S-401", FUTURE));
        final User inactive = new User(domain.EmailAddress.valueOf("inact@alsafe.com"),
                "Inactive One", "910000003", "S-402", FUTURE);
        inactive.deactivate();
        repo.save(inactive);

        // Act
        int count = 0;
        for (User ignored : new ListUsersController().allUsers()) count++;

        // Assert
        assertEquals(3, count);
    }

    @Test
    void ensureListIsSortedAlphabeticallyByName() {
        // Arrange (US033.4)
        final repositories.UserRepository repo =
                PersistenceContext.getInstance().userRepository();
        repo.save(new User(domain.EmailAddress.valueOf("z@alsafe.com"),
                "Zara",  "910000001", "S-403", FUTURE));
        repo.save(new User(domain.EmailAddress.valueOf("a@alsafe.com"),
                "Alice", "910000002", "S-404", FUTURE));
        repo.save(new User(domain.EmailAddress.valueOf("m@alsafe.com"),
                "Maria", "910000003", "S-405", FUTURE));

        // Act
        final List<String> names = new ArrayList<>();
        for (User u : new ListUsersController().allUsers()) names.add(u.getName());

        // Assert
        assertEquals("Alice", names.get(0));
        assertEquals("Maria", names.get(1));
        assertEquals("Zara",  names.get(2));
    }

    @Test
    void ensureListIsEmptyWhenNoUsersExist() {
        // Arrange (no users in repo)

        // Act
        int count = 0;
        for (User ignored : new ListUsersController().allUsers()) count++;

        // Assert (US033.1 — edge case)
        assertEquals(0, count);
    }

    @Test
    void ensureListShowsCorrectStatusForEachUser() {
        // Arrange (US033.2, US033.3)
        final repositories.UserRepository repo =
                PersistenceContext.getInstance().userRepository();
        repo.save(new User(domain.EmailAddress.valueOf("active@alsafe.com"),
                "Active User", "910000001", "S-406", FUTURE));
        final User inactiveUser = new User(domain.EmailAddress.valueOf("inactive@alsafe.com"),
                "Inactive User", "910000002", "S-407", FUTURE);
        inactiveUser.deactivate();
        repo.save(inactiveUser);

        // Act
        final List<User> users = new ArrayList<>();
        for (User u : new ListUsersController().allUsers()) users.add(u);

        // Assert
        assertTrue(users.stream().anyMatch(u -> u.getName().equals("Active User")   && u.isActive()));
        assertTrue(users.stream().anyMatch(u -> u.getName().equals("Inactive User") && !u.isActive()));
    }

    @Test
    void ensureListShowsRoleForEachUser() {
        // Arrange (US033.2)
        final repositories.UserRepository repo =
                PersistenceContext.getInstance().userRepository();
        final User admin = new User(domain.EmailAddress.valueOf("adm@alsafe.com"),
                "Admin", "910000000", "S-ADM", LocalDate.now().plusYears(10));
        admin.markAsAdmin();
        repo.save(admin);
        repo.save(new User(domain.EmailAddress.valueOf("usr@alsafe.com"),
                "Regular", "910000001", "S-USR", FUTURE));

        // Act
        final List<User> users = new ArrayList<>();
        for (User u : new ListUsersController().allUsers()) users.add(u);

        // Assert
        assertTrue(users.stream().anyMatch(u -> u.getRole().equals("ADMIN")));
        assertTrue(users.stream().anyMatch(u -> u.getRole().equals("BACKOFFICE_OPERATOR")));
    }
}
