package bootstrapper;

import domain.BaseRoles;
import domain.EmailAddress;
import domain.User;
import domain.ValidEmailDomains;
import eapli.framework.actions.Action;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.Role;
import persistence.PersistenceContext;
import repositories.UserRepository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Bootstrap action for the default administrator account.
 *
 * <p>Creates the admin SystemUser (EAPLI Framework — for authentication)
 * and the admin domain User (our repository — for domain data) independently,
 * so that both always exist after startup regardless of DB state.</p>
 */
public class UserBootstrapper implements Action {

    private static final String ADMIN_EMAIL     = "admin@alsafe.com";
    private static final String ADMIN_PASSWORD  = "Admin1234";
    private static final String ADMIN_FIRSTNAME = "AlSafe";
    private static final String ADMIN_LASTNAME  = "Administrator";
    private static final String ADMIN_PHONE     = "910000000";
    private static final String ADMIN_STAFF     = "STAFF-ADMIN-001";

    @Override
    public boolean execute() {
        loadValidDomains();
        createAdminSystemUser();
        createAdminDomainUser();
        return true;
    }

    private void loadValidDomains() {
        ValidEmailDomains.register("alsafe.com");
        ValidEmailDomains.register("alsafe.pt");
        ValidEmailDomains.register("isep.ipp.pt");
    }

    /**
     * Creates the admin SystemUser via the EAPLI Framework.
     * Silently skips if the SystemUser already exists (any duplicate-key
     * exception, regardless of how the framework wraps it).
     */
    private void createAdminSystemUser() {
        try {
            final UserManagementService svc = AuthzRegistry.userService();
            final Set<Role> roles = new HashSet<>();
            roles.add(BaseRoles.ADMIN);

            svc.registerNewUser(
                    ADMIN_EMAIL,       // username (= email)
                    ADMIN_PASSWORD,
                    ADMIN_FIRSTNAME,   // firstName — must not be empty
                    ADMIN_LASTNAME,    // lastName  — must not be empty
                    ADMIN_EMAIL,       // email
                    roles
            );

        } catch (final Exception e) {
            // Any exception here means the admin SystemUser already exists
            // (the framework may wrap ConstraintViolationException in
            //  RollbackException, so we catch broadly and skip silently)
        }
    }

    /**
     * Creates the admin domain User in our repository.
     * Skips silently if already present.
     */
    private void createAdminDomainUser() {
        try {
            final UserRepository repo = PersistenceContext.getInstance().userRepository();
            final EmailAddress emailVO = EmailAddress.valueOf(ADMIN_EMAIL);

            if (repo.ofIdentity(emailVO).isPresent()) {
                return;
            }

            final User admin = new User(
                    emailVO,
                    ADMIN_FIRSTNAME + " " + ADMIN_LASTNAME,
                    ADMIN_PHONE,
                    ADMIN_STAFF,
                    LocalDate.now().plusYears(10));
            admin.markAsAdmin();
            repo.save(admin);

        } catch (final Exception e) {
            System.err.println("[Bootstrap] ERROR creating admin domain User: "
                    + e.getClass().getName() + ": " + e.getMessage());
        }
    }
}