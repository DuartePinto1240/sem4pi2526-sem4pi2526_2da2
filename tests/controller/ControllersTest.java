package controller;

import domain.User;
import domain.ValidEmailDomains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistence.PersistenceContext;
import persistence.impl.inmemory.InMemoryUserRepository;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for US030, US031, US032 and US033 controllers.
 *
 * <p>Each test runs with a fresh {@link InMemoryUserRepository} injected into
 * the {@link PersistenceContext}, ensuring full isolation without requiring
 * a live database connection. {@link ValidEmailDomains} is also reset before
 * each test.</p>
 *
 * <p>Follows the AAA (Arrange – Act – Assert) convention.</p>
 */
class ControllersTest {

    private static final LocalDate FUTURE = LocalDate.now().plusYears(2);

    /**
     * Before each test:
     * 1. Reset the PersistenceContext singleton.
     * 2. Inject a fresh InMemoryUserRepository (avoids JPA / file DB dependency).
     * 3. Reset ValidEmailDomains and register the test domain.
     */
    @BeforeEach
    void setUp() throws Exception {
        // --- Reset PersistenceContext singleton ---
        final Field instanceField = PersistenceContext.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        // Trigger creation so we can replace the repository
        final PersistenceContext ctx = PersistenceContext.getInstance();

        // --- Inject InMemoryUserRepository ---
        final Field repoField = PersistenceContext.class.getDeclaredField("userRepository");
        repoField.setAccessible(true);
        repoField.set(ctx, new InMemoryUserRepository());

        // --- Reset ValidEmailDomains ---
        final Field domainsField = ValidEmailDomains.class.getDeclaredField("DOMAINS");
        domainsField.setAccessible(true);
        ((java.util.Set<?>) domainsField.get(null)).clear();
        ValidEmailDomains.register("alsafe.com");
    }

    // =========================================================================
    // US031 — Register Users
    // =========================================================================

    @Test
    void ensureUserIsRegisteredWithValidData() {
        // Arrange
        final RegisterUserController ctrl = new RegisterUserController();

        // Act
        final User u = ctrl.registerUser(
                "new@alsafe.com", "New User", "910000001", "pass1234", "S-100", FUTURE);

        // Assert (US031.1, US031.6)
        assertNotNull(u);
        assertEquals("New User", u.getName());
        assertEquals("new@alsafe.com", u.identity().toString());
        assertTrue(u.isActive());
        assertEquals("USER", u.getRole());
    }

    @Test
    void ensureUserHasSkillsAssessmentAutoSetToFiveYears() {
        // Arrange
        final RegisterUserController ctrl = new RegisterUserController();

        // Act
        final User u = ctrl.registerUser(
                "assess@alsafe.com", "User", "910000001", "pass1234", "S-101", FUTURE);

        // Assert
        assertEquals(LocalDate.now().plusYears(5), u.getNextSkillsAssessmentDate());
    }

    @Test
    void ensureDuplicateEmailIsRejected() {
        // Arrange
        final RegisterUserController ctrl = new RegisterUserController();
        ctrl.registerUser("dup@alsafe.com", "User A", "910000001", "pass1234", "S-101", FUTURE);

        // Act / Assert (US031.2)
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerUser("dup@alsafe.com", "User B", "910000002", "pass5678", "S-102", FUTURE));
    }

    @Test
    void ensureDuplicatePhoneIsRejected() {
        // Arrange
        final RegisterUserController ctrl = new RegisterUserController();
        ctrl.registerUser("a@alsafe.com", "User A", "910000001", "pass1234", "S-101", FUTURE);

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerUser("b@alsafe.com", "User B", "910000001", "pass1234", "S-102", FUTURE));
    }

    @Test
    void ensureDuplicateStaffNumberIsRejected() {
        // Arrange
        final RegisterUserController ctrl = new RegisterUserController();
        ctrl.registerUser("a@alsafe.com", "User A", "910000001", "pass1234", "S-SAME", FUTURE);

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerUser("b@alsafe.com", "User B", "910000002", "pass1234", "S-SAME", FUTURE));
    }

    @Test
    void ensureInvalidEmailDomainIsRejected() {
        // Arrange
        final RegisterUserController ctrl = new RegisterUserController();

        // Act / Assert (US031.3 — valid email format but invalid domain)
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerUser("user@gmail.com", "User", "910000001", "pass1234", "S-103", FUTURE));
    }

    @Test
    void ensurePastClearanceDateIsRejected() {
        // Arrange
        final RegisterUserController ctrl = new RegisterUserController();
        final LocalDate pastDate = LocalDate.now().minusDays(1);

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.registerUser("x@alsafe.com", "User", "910000001", "pass1234", "S-104", pastDate));
    }

    @Test
    void ensureBootstrapCreatesAdminUser() {
        // Arrange
        final RegisterUserController ctrl = new RegisterUserController();

        // Act (US031.7)
        final User admin = ctrl.registerAdmin(
                "admin@alsafe.com", "Admin", "910000000", "Admin1234", "S-ADMIN", FUTURE);

        // Assert
        assertNotNull(admin);
        assertTrue(admin.isAdmin());
        assertEquals("ADMIN", admin.getRole());
    }

    @Test
    void ensureBootstrapIsIdempotent() {
        // Arrange
        final RegisterUserController ctrl = new RegisterUserController();
        final User first = ctrl.registerAdmin(
                "admin@alsafe.com", "Admin", "910000000", "Admin1234", "S-ADMIN", FUTURE);

        // Act (calling bootstrap a second time must not throw or duplicate)
        final User second = ctrl.registerAdmin(
                "admin@alsafe.com", "Admin", "910000000", "Admin1234", "S-ADMIN", FUTURE);

        // Assert
        assertEquals(first.identity(), second.identity());
    }

    // =========================================================================
    // US030 — Authentication (Login)
    // =========================================================================

    @Test
    void ensureLoginSucceedsWithCorrectCredentials() {
        // Arrange
        new RegisterUserController().registerUser(
                "login@alsafe.com", "Login User", "910000001", "pass1234", "S-200", FUTURE);

        // Act
        final Optional<User> result = new LoginController().login("login@alsafe.com", "pass1234");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Login User", result.get().getName());
    }

    @Test
    void ensureLoginFailsWithWrongPassword() {
        // Arrange
        new RegisterUserController().registerUser(
                "wp@alsafe.com", "User", "910000001", "pass1234", "S-201", FUTURE);

        // Act
        final Optional<User> result = new LoginController().login("wp@alsafe.com", "wrong");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void ensureLoginFailsWithUnknownEmail() {
        // Arrange (no user registered)

        // Act
        final Optional<User> result = new LoginController().login("nobody@alsafe.com", "pass");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void ensureLoginFailsForInactiveUser() {
        // Arrange
        new RegisterUserController().registerUser(
                "inactive@alsafe.com", "Inactive", "910000001", "pass1234", "S-202", FUTURE);
        new ToggleUserController().deactivateUser("inactive@alsafe.com");

        // Act (US032.3)
        final Optional<User> result = new LoginController().login("inactive@alsafe.com", "pass1234");

        // Assert
        assertTrue(result.isEmpty());
    }

    // =========================================================================
    // US032 — Enable/Disable Users
    // =========================================================================

    @Test
    void ensureActiveUserCanBeDisabled() {
        // Arrange
        new RegisterUserController().registerUser(
                "d@alsafe.com", "D User", "910000001", "pass1234", "S-300", FUTURE);

        // Act (US032.1)
        new ToggleUserController().deactivateUser("d@alsafe.com");

        // Assert (US032.3 + US032.4 — data preserved, login refused)
        assertTrue(new LoginController().login("d@alsafe.com", "pass1234").isEmpty());
    }

    @Test
    void ensureDisabledUserCanBeReEnabled() {
        // Arrange
        new RegisterUserController().registerUser(
                "r@alsafe.com", "R User", "910000001", "pass1234", "S-301", FUTURE);
        new ToggleUserController().deactivateUser("r@alsafe.com");

        // Act (US032.2)
        new ToggleUserController().activateUser("r@alsafe.com");

        // Assert
        assertTrue(new LoginController().login("r@alsafe.com", "pass1234").isPresent());
    }

    @Test
    void ensureDeactivatingNonExistentUserThrows() {
        // Arrange (no user registered)

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new ToggleUserController().deactivateUser("ghost@alsafe.com"));
    }

    @Test
    void ensureDeactivatingAlreadyInactiveUserThrows() {
        // Arrange (US032.6)
        new RegisterUserController().registerUser(
                "ai@alsafe.com", "AI User", "910000001", "pass1234", "S-302", FUTURE);
        new ToggleUserController().deactivateUser("ai@alsafe.com");

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new ToggleUserController().deactivateUser("ai@alsafe.com"));
    }

    @Test
    void ensureActivatingAlreadyActiveUserThrows() {
        // Arrange (US032.6 — symmetric rule)
        new RegisterUserController().registerUser(
                "aa@alsafe.com", "AA User", "910000001", "pass1234", "S-303", FUTURE);

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new ToggleUserController().activateUser("aa@alsafe.com"));
    }

    @Test
    void ensureAdminAccountCannotBeDisabled() {
        // Arrange (US032.5)
        new RegisterUserController().registerAdmin(
                "admin2@alsafe.com", "Admin2", "910000000", "Admin1234", "S-ADM2", FUTURE);

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new ToggleUserController().deactivateUser("admin2@alsafe.com"));
    }

    // =========================================================================
    // US033 — List Users
    // =========================================================================

    @Test
    void ensureAllUsersAreListedIncludingInactive() {
        // Arrange (US033.1, US033.3)
        final RegisterUserController reg = new RegisterUserController();
        reg.registerUser("act1@alsafe.com",  "Active One",   "910000001", "pass1234", "S-400", FUTURE);
        reg.registerUser("act2@alsafe.com",  "Active Two",   "910000002", "pass1234", "S-401", FUTURE);
        reg.registerUser("inact@alsafe.com", "Inactive One", "910000003", "pass1234", "S-402", FUTURE);
        new ToggleUserController().deactivateUser("inact@alsafe.com");

        // Act
        int count = 0;
        for (User ignored : new ListUsersController().allUsers()) count++;

        // Assert
        assertEquals(3, count);
    }

    @Test
    void ensureListIsEmptyWhenNoUsersExist() {
        // Arrange (no users registered)

        // Act
        int count = 0;
        for (User ignored : new ListUsersController().allUsers()) count++;

        // Assert (US033.1 — edge case)
        assertEquals(0, count);
    }

    @Test
    void ensureListIsSortedAlphabeticallyByName() {
        // Arrange (US033.4)
        final RegisterUserController reg = new RegisterUserController();
        reg.registerUser("z@alsafe.com", "Zara",  "910000001", "pass1234", "S-403", FUTURE);
        reg.registerUser("a@alsafe.com", "Alice", "910000002", "pass1234", "S-404", FUTURE);
        reg.registerUser("m@alsafe.com", "Maria", "910000003", "pass1234", "S-405", FUTURE);

        // Act
        final List<String> names = new ArrayList<>();
        for (User u : new ListUsersController().allUsers()) names.add(u.getName());

        // Assert
        assertEquals("Alice", names.get(0));
        assertEquals("Maria", names.get(1));
        assertEquals("Zara",  names.get(2));
    }

    @Test
    void ensureUserStatusIsCorrectlyReportedInList() {
        // Arrange (US033.2, US033.3)
        final RegisterUserController reg = new RegisterUserController();
        reg.registerUser("active@alsafe.com",   "Active User",   "910000001", "pass1234", "S-406", FUTURE);
        reg.registerUser("inactive@alsafe.com", "Inactive User", "910000002", "pass1234", "S-407", FUTURE);
        new ToggleUserController().deactivateUser("inactive@alsafe.com");

        // Act
        final List<User> users = new ArrayList<>();
        for (User u : new ListUsersController().allUsers()) users.add(u);

        // Assert
        assertTrue(users.stream().anyMatch(u -> u.getName().equals("Active User")   && u.isActive()));
        assertTrue(users.stream().anyMatch(u -> u.getName().equals("Inactive User") && !u.isActive()));
    }

    @Test
    void ensureListShowsRoleForEachUser() {
        // Arrange (US033.2 — role must be visible)
        final RegisterUserController reg = new RegisterUserController();
        reg.registerAdmin("adm@alsafe.com",  "Admin",    "910000000", "Admin1234", "S-ADM", FUTURE);
        reg.registerUser("usr@alsafe.com",   "Regular",  "910000001", "pass1234",  "S-USR", FUTURE);

        // Act
        final List<User> users = new ArrayList<>();
        for (User u : new ListUsersController().allUsers()) users.add(u);

        // Assert
        assertTrue(users.stream().anyMatch(u -> u.getRole().equals("ADMIN")));
        assertTrue(users.stream().anyMatch(u -> u.getRole().equals("USER")));
    }
}
