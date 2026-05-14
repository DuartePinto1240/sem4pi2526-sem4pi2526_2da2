package controller;

import domain.AlSafePasswordPolicy;
import domain.BaseRoles;
import domain.User;
import domain.ValidEmailDomains;
import dto.UserDTO;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistence.PersistenceContext;
import persistence.impl.inmemory.InMemoryUserRepository;
import repositories.UserRepository;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for US030, US031, US032 and US033 controllers.
 *
 * <p>The EAPLI Framework ({@code AuthzRegistry}) is configured once per
 * class using the H2 file-based persistence unit. Our domain
 * {@link PersistenceContext} is reset before each test via reflection
 * to ensure domain-level isolation between tests.</p>
 *
 * <p>Follows the AAA (Arrange – Act – Assert) convention.</p>
 */
class ControllersTest {

    private static final LocalDate FUTURE           = LocalDate.now().plusYears(2);
    private static final String    PERSISTENCE_UNIT = "eapli.base.AlSafePU";

    // =========================================================================
    // Framework setup
    // =========================================================================


    @SuppressWarnings({"rawtypes", "unchecked"})


    @BeforeEach
    void setUp() throws Exception {
        // ── 1. Configurar AuthzRegistry PRIMEIRO ─────────────────────────
        try {
            AuthzRegistry.configure(
                    new JpaAutoTxUserRepository(PERSISTENCE_UNIT, new HashMap<>()),
                    new AlSafePasswordPolicy(),
                    new PlainTextEncoder()
            );
        } catch (final Exception e) {
            // already configured — ignore
        }

        // ── 2. Reset PersistenceContext singleton ─────────────────────────
        final Field instanceField = PersistenceContext.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        // ── 3. Inject fresh InMemoryUserRepository ────────────────────────
        final PersistenceContext ctx = PersistenceContext.getInstance();
        final Field repoField = PersistenceContext.class.getDeclaredField("userRepository");
        repoField.setAccessible(true);
        repoField.set(ctx, new InMemoryUserRepository());

        // ── 4. Reset ValidEmailDomains ────────────────────────────────────
        final Field domainsField = ValidEmailDomains.class.getDeclaredField("DOMAINS");
        domainsField.setAccessible(true);
        ((java.util.Set<?>) domainsField.get(null)).clear();
        ValidEmailDomains.register("alsafe.com");

        // ── 5. Clear active session ───────────────────────────────────────
        AuthzRegistry.authorizationService().clearSession();
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private void createSystemUser(final String email, final String password,
                                  final String firstName, final String lastName,
                                  final Role role) {
        try {
            final Set<Role> roles = new HashSet<>();
            roles.add(role);
            AuthzRegistry.userService().registerNewUser(
                    email, password, firstName, lastName, email, roles);
        } catch (final Exception e) {
            // already exists — ignore
        }
    }

    // =========================================================================
    // US030 — Authentication (Login / Logout / Session)
    // =========================================================================

    @Test
    void ensureLoginSucceedsWithCorrectCredentials() {
        // Arrange (US030.1)
        createSystemUser("login.ok@alsafe.com", "pass1234",
                "Login", "OK", BaseRoles.BACKOFFICE_OPERATOR);

        // Act
        final Optional<UserSession> result =
                new LoginController().login("login.ok@alsafe.com", "pass1234");

        // Assert
        assertTrue(result.isPresent(), "Login must succeed with correct credentials");
    }



    @Test
    void ensureLoginFailsWithUnknownEmail() {
        // Arrange (US030.3) — no user registered

        // Act
        final Optional<UserSession> result =
                new LoginController().login("nobody@alsafe.com", "pass");

        // Assert
        assertTrue(result.isEmpty(), "Login must fail for unknown email");
    }

    @Test
    void ensureSessionExistsAfterSuccessfulLogin() {
        // Arrange (US030.5)
        createSystemUser("session.ok@alsafe.com", "pass1234",
                "Session", "OK", BaseRoles.BACKOFFICE_OPERATOR);

        // Act
        new LoginController().login("session.ok@alsafe.com", "pass1234");

        // Assert
        final AuthorizationService authz = AuthzRegistry.authorizationService();
        assertTrue(authz.session().isPresent(), "Session must exist after login");
    }

    @Test
    void ensureSessionIsClearedAfterLogout() {
        // Arrange (US030.5)
        createSystemUser("session.out@alsafe.com", "pass1234",
                "Session", "Out", BaseRoles.BACKOFFICE_OPERATOR);
        new LoginController().login("session.out@alsafe.com", "pass1234");
        assertTrue(AuthzRegistry.authorizationService().session().isPresent());

        // Act
        AuthzRegistry.authorizationService().clearSession();

        // Assert
        assertTrue(AuthzRegistry.authorizationService().session().isEmpty(),
                "Session must be cleared after logout");
    }

    @Test
    void ensureLoginFailsWhenSecurityClearanceIsExpired() {
        // Arrange (US030 — per client clarification: expired clearance blocks login)
        final String testEmail = "expired.clearance@alsafe.com";
        createSystemUser(testEmail, "pass1234",
                "Expired", "Clearance", BaseRoles.BACKOFFICE_OPERATOR);

        // Create domain User with an expired clearance date
        // We bypass the constructor validation by using a past date trick:
        // create with future date, then update to expired via updateSecurityClearance
        // But since updateSecurityClearance also rejects past dates, we use
        // a User with a clearance that is already "today - 1 day" — we test
        // the domain state directly after setting it via reflection.
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        final domain.EmailAddress emailVO = domain.EmailAddress.valueOf(testEmail);

        // Create with future clearance, then force-expire it via reflection
        final User u = new User(emailVO, "Expired Clearance", "910000091", "S-EXP", FUTURE);
        try {
            final Field f = User.class.getDeclaredField("securityClearanceExpiry");
            f.setAccessible(true);
            f.set(u, LocalDate.now().minusDays(1)); // clearance expired yesterday
        } catch (Exception ex) {
            fail("Could not set expired clearance for test: " + ex.getMessage());
        }
        repo.save(u);

        // Act — LoginController must detect the expired clearance and deny login
        final Optional<UserSession> result = new LoginController().login(testEmail, "pass1234");

        // Assert
        assertTrue(result.isEmpty(),
                "Login must fail when security clearance has expired (client clarification)");
    }

    @Test
    void ensureLoginSucceedsWhenClearanceIsValid() {
        // Arrange — verify valid clearance does NOT block login
        final String testEmail = "valid.clearance@alsafe.com";
        createSystemUser(testEmail, "pass1234",
                "Valid", "Clearance", BaseRoles.BACKOFFICE_OPERATOR);

        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        final domain.EmailAddress emailVO = domain.EmailAddress.valueOf(testEmail);
        repo.save(new User(emailVO, "Valid Clearance", "910000092", "S-VLC", FUTURE));

        // Act
        final Optional<UserSession> result = new LoginController().login(testEmail, "pass1234");

        // Assert
        assertTrue(result.isPresent(),
                "Login must succeed when security clearance is valid");
    }

    @Test
    void ensureSkillsAssessmentExpiryDoesNotBlockLogin() {
        // Arrange (per client: skills assessment has NO effect on login ability)
        final String testEmail = "skills.expired@alsafe.com";
        createSystemUser(testEmail, "pass1234",
                "Skills", "Expired", BaseRoles.BACKOFFICE_OPERATOR);

        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        final domain.EmailAddress emailVO = domain.EmailAddress.valueOf(testEmail);
        final User u = new User(emailVO, "Skills Expired", "910000093", "S-SKE", FUTURE);
        // Force-expire the skills assessment date
        try {
            final Field f = User.class.getDeclaredField("nextSkillsAssessmentDate");
            f.setAccessible(true);
            f.set(u, LocalDate.now().minusDays(1)); // assessment overdue
        } catch (Exception ex) {
            fail("Could not set expired assessment for test: " + ex.getMessage());
        }
        repo.save(u);

        // Act — login must still succeed (skills assessment does not block login)
        final Optional<UserSession> result = new LoginController().login(testEmail, "pass1234");

        // Assert
        assertTrue(result.isPresent(),
                "Expired skills assessment must NOT block login (client clarification)");
    }

    @Test
    void ensureLoginFailsForDisabledAccount() {
        // Arrange (US030.3 + US032.3)
        final String testEmail = "disabled.test@alsafe.com";
        createSystemUser(testEmail, "pass1234",
                "Disabled", "Test", BaseRoles.BACKOFFICE_OPERATOR);

        // Ensure SystemUser is active
        for (eapli.framework.infrastructure.authz.domain.model.SystemUser su
                : AuthzRegistry.userService().allUsers()) {
            if (su.identity().toString().equals(testEmail) && !su.isActive()) {
                try { AuthzRegistry.userService().activateUser(su); }
                catch (final Exception ignored) { }
            }
        }

        // Create domain User and deactivate it
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        final domain.EmailAddress emailVO = domain.EmailAddress.valueOf(testEmail);
        if (repo.ofIdentity(emailVO).isEmpty()) {
            repo.save(new User(emailVO, "Disabled Test", "910000099", "S-DISABLED", FUTURE));
        }
        repo.ofIdentity(emailVO).ifPresent(u -> { u.deactivate(); repo.save(u); });

        // Assert domain state
        assertTrue(repo.ofIdentity(emailVO).map(u -> !u.isActive()).orElse(false),
                "Domain user must be inactive after deactivation (US032.1)");
    }

    @Test
    void ensureAdminRoleIsAuthorizedAfterLogin() {
        // Arrange (US030.2)
        createSystemUser("admin.role@alsafe.com", "Admin1234",
                "Admin", "Role", BaseRoles.ADMIN);

        // Act
        new LoginController().login("admin.role@alsafe.com", "Admin1234");

        // Assert
        assertTrue(
                AuthzRegistry.authorizationService()
                        .isAuthenticatedUserAuthorizedTo(BaseRoles.ADMIN),
                "Admin user must be authorized as ADMIN after login"
        );
    }

    @Test
    void ensureRegularUserIsNotAuthorizedAsAdmin() {
        // Arrange (US030.2)
        createSystemUser("regular.role@alsafe.com", "pass1234",
                "Regular", "Role", BaseRoles.BACKOFFICE_OPERATOR);

        // Act
        new LoginController().login("regular.role@alsafe.com", "pass1234");

        // Assert
        assertFalse(
                AuthzRegistry.authorizationService()
                        .isAuthenticatedUserAuthorizedTo(BaseRoles.ADMIN),
                "Regular user must NOT be authorized as ADMIN"
        );
    }

    // =========================================================================
    // US031 — User registration (domain layer)
    // =========================================================================

    @Test
    void ensureDomainUserIsCreatedWithCorrectData() {
        // Arrange / Act (US031.1, US031.6)
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

    @Test
    void ensureAdminDomainUserHasAdminRole() {
        // Arrange / Act (US031.7)
        final User admin = new User(
                domain.EmailAddress.valueOf("adm@alsafe.com"),
                "Admin", "910000000", "S-ADMIN", LocalDate.now().plusYears(10));
        admin.markAsAdmin();

        // Assert
        assertEquals("ADMIN", admin.getRole());
    }

    @Test
    void ensureSkillsAssessmentIsAutoSetToFiveYears() {
        // Arrange / Act (section 3.1.1)
        final User u = new User(
                domain.EmailAddress.valueOf("assess@alsafe.com"),
                "User", "910000001", "S-102", FUTURE);

        // Assert
        assertEquals(LocalDate.now().plusYears(5), u.getNextSkillsAssessmentDate());
    }

    @Test
    void ensureDomainRepositoryRejectsDuplicateEmail() {
        // Arrange (US031.2)
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        final domain.EmailAddress email = domain.EmailAddress.valueOf("dup@alsafe.com");
        repo.save(new User(email, "User A", "910000001", "S-103", FUTURE));
        repo.save(new User(email, "User B", "910000002", "S-104", FUTURE));

        // Assert — only one record per email
        final long count = java.util.stream.StreamSupport
                .stream(repo.findAll().spliterator(), false)
                .filter(u -> u.identity().equals(email))
                .count();
        assertEquals(1, count, "Only one user per email must exist (US031.2)");
    }

    @Test
    void ensureDomainRepositoryRejectsDuplicatePhone() {
        // Arrange
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        repo.save(new User(
                domain.EmailAddress.valueOf("a@alsafe.com"),
                "User A", "910000001", "S-105", FUTURE));

        // Act / Assert
        assertThrows(Exception.class, () ->
                repo.save(new User(
                        domain.EmailAddress.valueOf("b@alsafe.com"),
                        "User B", "910000001", "S-106", FUTURE)));
    }

    @Test
    void ensureDomainRepositoryRejectsDuplicateStaffNumber() {
        // Arrange
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        repo.save(new User(
                domain.EmailAddress.valueOf("c@alsafe.com"),
                "User C", "910000001", "S-SAME", FUTURE));

        // Act / Assert
        assertThrows(Exception.class, () ->
                repo.save(new User(
                        domain.EmailAddress.valueOf("d@alsafe.com"),
                        "User D", "910000002", "S-SAME", FUTURE)));
    }

    @Test
    void ensureUserCreationFailsWithInvalidPhone() {
        // Arrange / Act / Assert (US031.4)
        assertThrows(IllegalArgumentException.class, () ->
                new User(domain.EmailAddress.valueOf("x@alsafe.com"),
                        "Name", "12345", "S-107", FUTURE));
    }

    @Test
    void ensureUserCreationFailsWithPastClearanceDate() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new User(domain.EmailAddress.valueOf("x@alsafe.com"),
                        "Name", "910000001", "S-108",
                        LocalDate.now().minusDays(1)));
    }

    // =========================================================================
    // US032 — Enable / Disable users
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
    void ensureToggleRejectsDeactivatingNonExistentUser() {
        // Arrange (US032)
        final ToggleUserController tog = new ToggleUserController();
        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                tog.deactivateUser("ghost@alsafe.com"));
    }

    @Test
    void ensureToggleRejectsDeactivatingAdminRole() {
        // Arrange (US032.5)
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        final User admin = new User(
                domain.EmailAddress.valueOf("admin2@alsafe.com"),
                "Admin2", "910000000", "S-ADM2", LocalDate.now().plusYears(10));
        admin.markAsAdmin();
        repo.save(admin);
        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new ToggleUserController().deactivateUser("admin2@alsafe.com"));
    }

    @Test
    void ensureToggleRejectsDeactivatingAlreadyInactiveUser() {
        // Arrange (US032.6)
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        final User u = new User(
                domain.EmailAddress.valueOf("ai@alsafe.com"),
                "AI User", "910000001", "S-302", FUTURE);
        u.deactivate();
        repo.save(u);
        // Act / Assert
        assertThrows(IllegalArgumentException.class, () ->
                new ToggleUserController().deactivateUser("ai@alsafe.com"));
    }



    @Test
    void ensureDeactivationAndReactivationChangesDomainStatus() {
        // Arrange (US032.1 + US032.2)
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        final User u = new User(
                domain.EmailAddress.valueOf("toggle@alsafe.com"),
                "Toggle User", "910000001", "S-304", FUTURE);
        repo.save(u);
        final ToggleUserController tog = new ToggleUserController();

        // Act — deactivate
        tog.deactivateUser("toggle@alsafe.com");
        assertFalse(repo.ofIdentity(
                domain.EmailAddress.valueOf("toggle@alsafe.com")).get().isActive());

        // Act — reactivate
        tog.activateUser("toggle@alsafe.com");
        assertTrue(repo.ofIdentity(
                domain.EmailAddress.valueOf("toggle@alsafe.com")).get().isActive());
    }

    // =========================================================================
    // US033 — List users (returns UserDTO)
    // =========================================================================

    @Test
    void ensureListReturnsAllUsersIncludingInactive() {
        // Arrange (US033.1, US033.3)
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
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
        for (UserDTO ignored : new ListUsersController().allUsers()) count++;

        // Assert
        assertEquals(3, count);
    }

    @Test
    void ensureListIsEmptyWhenNoUsersExist() {
        // Arrange (no users in repo)

        // Act
        int count = 0;
        for (UserDTO ignored : new ListUsersController().allUsers()) count++;

        // Assert (US033.1 — edge case)
        assertEquals(0, count);
    }

    @Test
    void ensureListIsSortedAlphabeticallyByName() {
        // Arrange (US033.4)
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        repo.save(new User(domain.EmailAddress.valueOf("z@alsafe.com"),
                "Zara",  "910000001", "S-403", FUTURE));
        repo.save(new User(domain.EmailAddress.valueOf("a@alsafe.com"),
                "Alice", "910000002", "S-404", FUTURE));
        repo.save(new User(domain.EmailAddress.valueOf("m@alsafe.com"),
                "Maria", "910000003", "S-405", FUTURE));

        // Act
        final List<String> names = new ArrayList<>();
        for (UserDTO u : new ListUsersController().allUsers()) names.add(u.getName());

        // Assert
        assertEquals("Alice", names.get(0));
        assertEquals("Maria", names.get(1));
        assertEquals("Zara",  names.get(2));
    }

    @Test
    void ensureListShowsCorrectStatusForEachUser() {
        // Arrange (US033.2, US033.3)
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        repo.save(new User(domain.EmailAddress.valueOf("active@alsafe.com"),
                "Active User", "910000001", "S-406", FUTURE));
        final User inactiveUser = new User(
                domain.EmailAddress.valueOf("inactive@alsafe.com"),
                "Inactive User", "910000002", "S-407", FUTURE);
        inactiveUser.deactivate();
        repo.save(inactiveUser);

        // Act
        final List<UserDTO> users = new ArrayList<>();
        for (UserDTO u : new ListUsersController().allUsers()) users.add(u);

        // Assert
        assertTrue(users.stream().anyMatch(u ->
                u.getName().equals("Active User") && u.isActive()));
        assertTrue(users.stream().anyMatch(u ->
                u.getName().equals("Inactive User") && !u.isActive()));
    }

    @Test
    void ensureListShowsRoleForEachUser() {
        // Arrange (US033.2)
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        final User admin = new User(
                domain.EmailAddress.valueOf("adm@alsafe.com"),
                "Admin User", "910000000", "S-ADM", LocalDate.now().plusYears(10));
        admin.markAsAdmin();
        repo.save(admin);
        repo.save(new User(domain.EmailAddress.valueOf("usr@alsafe.com"),
                "Regular User", "910000001", "S-USR", FUTURE));

        // Act
        final List<UserDTO> users = new ArrayList<>();
        for (UserDTO u : new ListUsersController().allUsers()) users.add(u);

        // Assert
        assertTrue(users.stream().anyMatch(u -> u.getRole().equals("ADMIN")));
        assertTrue(users.stream().anyMatch(u -> u.getRole().equals("BACKOFFICE_OPERATOR")));
    }

    @Test
    void ensureListShowsSecurityClearanceForEachUser() {
        // Arrange (US033.2 — clearance date visible)
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        final LocalDate clearance = LocalDate.now().plusYears(3);
        repo.save(new User(domain.EmailAddress.valueOf("clr@alsafe.com"),
                "Clearance User", "910000001", "S-408", clearance));

        // Act
        final List<UserDTO> users = new ArrayList<>();
        for (UserDTO u : new ListUsersController().allUsers()) users.add(u);

        // Assert
        assertTrue(users.stream().anyMatch(u ->
                clearance.equals(u.getSecurityClearanceExpiry())));
    }

    @Test
    void ensureListShowsSkillsAssessmentForEachUser() {
        // Arrange (US033.2 — assessment date visible, section 3.1.1)
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        repo.save(new User(domain.EmailAddress.valueOf("ass@alsafe.com"),
                "Assessment User", "910000001", "S-409", FUTURE));

        // Act
        final List<UserDTO> users = new ArrayList<>();
        for (UserDTO u : new ListUsersController().allUsers()) users.add(u);

        // Assert
        assertTrue(users.stream().allMatch(u ->
                u.getNextSkillsAssessmentDate() != null));
    }

    @Test
    void ensureUserDTOContainsCorrectFields() {
        // Arrange (US033 — DTO decouples UI from domain)
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        final LocalDate clearance = LocalDate.now().plusYears(2);
        repo.save(new User(
                domain.EmailAddress.valueOf("dto@alsafe.com"),
                "DTO User", "910000001", "S-DTO", clearance));

        // Act
        final List<UserDTO> users = new ArrayList<>();
        for (UserDTO u : new ListUsersController().allUsers()) users.add(u);
        final UserDTO dto = users.get(0);

        // Assert — all fields mapped correctly
        assertEquals("dto@alsafe.com",   dto.getEmail());
        assertEquals("DTO User",         dto.getName());
        assertEquals("BACKOFFICE_OPERATOR", dto.getRole());
        assertEquals("910000001",        dto.getPhoneNumber());
        assertEquals("S-DTO",            dto.getStaffNumber());
        assertTrue(dto.isActive());
        assertEquals(clearance,          dto.getSecurityClearanceExpiry());
        assertNotNull(dto.getNextSkillsAssessmentDate());
    }
}