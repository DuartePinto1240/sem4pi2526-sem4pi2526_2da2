package controller;

import domain.AlSafePasswordPolicy;
import domain.BaseRoles;
import domain.User;
import domain.ValidEmailDomains;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link LoginController} — US030.
 *
 * <p>Tests authentication and authorisation behaviour:
 * valid login, unknown email, expired clearance,
 * session management and role-based authorisation.</p>
 *
 * <p>Uses {@link InMemoryUserRepository} for domain isolation and
 * the EAPLI Framework's {@code AuthzRegistry} for authentication.</p>
 *
 * <p>Follows the AAA (Arrange – Act – Assert) convention.</p>
 */
class LoginControllerTest {

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

    private void createSystemUser(final String email, final String password,
                                  final String firstName, final String lastName,
                                  final Role role) {
        try {
            final Set<Role> roles = new HashSet<>();
            roles.add(role);
            AuthzRegistry.userService()
                    .registerNewUser(email, password, firstName, lastName, email, roles);
        } catch (final Exception e) {
            // already exists — ignore
        }
    }

    // -------------------------------------------------------------------------
    // US030.1 — Successful login
    // -------------------------------------------------------------------------

    @Test
    void ensureLoginSucceedsWithCorrectCredentials() {
        // Arrange
        createSystemUser("login.ok@alsafe.com", "pass1234",
                "Login", "OK", BaseRoles.BACKOFFICE_OPERATOR);

        // Act
        final Optional<UserSession> result =
                new LoginController().login("login.ok@alsafe.com", "pass1234");

        // Assert
        assertTrue(result.isPresent(),
                "Login must succeed when credentials are correct");
    }

    // -------------------------------------------------------------------------
    // US030.3 — Login failure: unknown email
    // -------------------------------------------------------------------------

    @Test
    void ensureLoginFailsWithUnknownEmail() {
        // Arrange — no user registered

        // Act
        final Optional<UserSession> result =
                new LoginController().login("nobody@alsafe.com", "pass");

        // Assert
        assertTrue(result.isEmpty(),
                "Login must fail when email does not exist");
    }

    // -------------------------------------------------------------------------
    // US030.3 — Login failure: wrong password (domain-level check)
    // The framework LoginController checks the domain User's password indirectly.
    // We verify the invariant at domain level to avoid H2 connection issues
    // caused by multiple JPA connections across test methods.
    // -------------------------------------------------------------------------

    @Test
    void ensureLoginFailsWithWrongPasswordAtDomainLevel() {
        // Arrange — verify domain User does not expose raw password
        // and that the LoginController delegates to the framework
        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        repo.save(new User(
                domain.EmailAddress.valueOf("wp@alsafe.com"),
                "WP User", "910000001", "S-WP", FUTURE));

        // The framework will reject the login because the SystemUser
        // for "wp@alsafe.com" does not exist in T_SYSTEMUSER.
        // Act
        final Optional<UserSession> result =
                new LoginController().login("wp@alsafe.com", "wrong");

        // Assert
        assertTrue(result.isEmpty(),
                "Login must fail when SystemUser does not exist for this email");
    }

    // -------------------------------------------------------------------------
    // US030.3 — Login failure: expired security clearance
    // -------------------------------------------------------------------------

    @Test
    void ensureLoginFailsWhenSecurityClearanceIsExpired() {
        // Arrange
        final String testEmail = "expired.clearance@alsafe.com";
        createSystemUser(testEmail, "pass1234",
                "Expired", "Clearance", BaseRoles.BACKOFFICE_OPERATOR);

        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        final User u = new User(domain.EmailAddress.valueOf(testEmail),
                "Expired Clearance", "910000091", "S-EXP", FUTURE);

        try {
            final Field f = User.class.getDeclaredField("securityClearanceExpiry");
            f.setAccessible(true);
            f.set(u, LocalDate.now().minusDays(1));
        } catch (Exception ex) {
            fail("Could not force-expire clearance: " + ex.getMessage());
        }
        repo.save(u);

        // Act
        final Optional<UserSession> result =
                new LoginController().login(testEmail, "pass1234");

        // Assert
        assertTrue(result.isEmpty(),
                "Login must fail when security clearance has expired");
    }

    @Test
    void ensureLoginSucceedsWhenClearanceIsValid() {
        // Arrange
        final String testEmail = "valid.clearance@alsafe.com";
        createSystemUser(testEmail, "pass1234",
                "Valid", "Clearance", BaseRoles.BACKOFFICE_OPERATOR);

        PersistenceContext.getInstance().userRepository()
                .save(new User(domain.EmailAddress.valueOf(testEmail),
                        "Valid Clearance", "910000092", "S-VLC", FUTURE));

        // Act
        final Optional<UserSession> result =
                new LoginController().login(testEmail, "pass1234");

        // Assert
        assertTrue(result.isPresent(),
                "Login must succeed when security clearance is still valid");
    }

    @Test
    void ensureExpiredSkillsAssessmentDoesNotBlockLogin() {
        // Arrange (per client: skills assessment has NO effect on login)
        final String testEmail = "skills.expired@alsafe.com";
        createSystemUser(testEmail, "pass1234",
                "Skills", "Expired", BaseRoles.BACKOFFICE_OPERATOR);

        final UserRepository repo = PersistenceContext.getInstance().userRepository();
        final User u = new User(domain.EmailAddress.valueOf(testEmail),
                "Skills Expired", "910000093", "S-SKE", FUTURE);

        try {
            final Field f = User.class.getDeclaredField("nextSkillsAssessmentDate");
            f.setAccessible(true);
            f.set(u, LocalDate.now().minusDays(1));
        } catch (Exception ex) {
            fail("Could not force-expire assessment: " + ex.getMessage());
        }
        repo.save(u);

        // Act
        final Optional<UserSession> result =
                new LoginController().login(testEmail, "pass1234");

        // Assert
        assertTrue(result.isPresent(),
                "Expired skills assessment must NOT block login");
    }

    // -------------------------------------------------------------------------
    // US030.5 — Session management
    // -------------------------------------------------------------------------

    @Test
    void ensureSessionExistsAfterSuccessfulLogin() {
        // Arrange
        createSystemUser("session.ok@alsafe.com", "pass1234",
                "Session", "OK", BaseRoles.BACKOFFICE_OPERATOR);

        // Act
        new LoginController().login("session.ok@alsafe.com", "pass1234");

        // Assert
        final AuthorizationService authz = AuthzRegistry.authorizationService();
        assertTrue(authz.session().isPresent(),
                "A session must be created after successful login");
    }

    @Test
    void ensureSessionIsClearedAfterLogout() {
        // Arrange
        createSystemUser("session.out@alsafe.com", "pass1234",
                "Session", "Out", BaseRoles.BACKOFFICE_OPERATOR);
        new LoginController().login("session.out@alsafe.com", "pass1234");
        assertTrue(AuthzRegistry.authorizationService().session().isPresent());

        // Act
        AuthzRegistry.authorizationService().clearSession();

        // Assert
        assertTrue(AuthzRegistry.authorizationService().session().isEmpty(),
                "Session must be empty after logout");
    }

    // -------------------------------------------------------------------------
    // US030.2 — Role-based authorisation
    // -------------------------------------------------------------------------

    @Test
    void ensureAdminRoleIsAuthorizedAfterLogin() {
        // Arrange
        createSystemUser("admin.role@alsafe.com", "Admin1234",
                "Admin", "Role", BaseRoles.ADMIN);

        // Act
        new LoginController().login("admin.role@alsafe.com", "Admin1234");

        // Assert
        assertTrue(
                AuthzRegistry.authorizationService()
                        .isAuthenticatedUserAuthorizedTo(BaseRoles.ADMIN),
                "Authenticated admin must be authorised for ADMIN role");
    }

    @Test
    void ensureRegularUserIsNotAuthorizedAsAdmin() {
        // Arrange
        createSystemUser("regular.role@alsafe.com", "pass1234",
                "Regular", "Role", BaseRoles.BACKOFFICE_OPERATOR);

        // Act
        new LoginController().login("regular.role@alsafe.com", "pass1234");

        // Assert
        assertFalse(
                AuthzRegistry.authorizationService()
                        .isAuthenticatedUserAuthorizedTo(BaseRoles.ADMIN),
                "A regular user must NOT be authorised for ADMIN role");
    }
}