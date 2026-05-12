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

public class UserBootstrapper implements Action {

    private static final String ADMIN_EMAIL     = "admin@alsafe.com";
    private static final String ADMIN_PASSWORD  = "Admin1234";
    private static final String ADMIN_FIRSTNAME = "AlSafe";
    private static final String ADMIN_LASTNAME  = "Administrator";
    private static final String ADMIN_PHONE     = "910000000";
    private static final String ADMIN_STAFF     = "STAFF-ADMIN-001";

    private static final String ATCC_EMAIL     = "atcc@alsafe.com";
    private static final String ATCC_PASSWORD  = "Atcc1234";
    private static final String ATCC_FIRSTNAME = "ATCC";
    private static final String ATCC_LASTNAME  = "Collaborator";
    private static final String ATCC_PHONE     = "910000001";
    private static final String ATCC_STAFF     = "STAFF-ATCC-001";

    @Override
    public boolean execute() {
        loadValidDomains();
        createAdminSystemUser();
        createAdminDomainUser();
        createAtccSystemUser();
        createAtccDomainUser();
        return true;
    }

    private void loadValidDomains() {
        ValidEmailDomains.register("alsafe.com");
        ValidEmailDomains.register("alsafe.pt");
        ValidEmailDomains.register("isep.ipp.pt");
    }

    private void createAdminSystemUser() {
        try {
            final UserManagementService svc = AuthzRegistry.userService();
            final Set<Role> roles = new HashSet<>();
            roles.add(BaseRoles.ADMIN);
            svc.registerNewUser(
                    ADMIN_EMAIL,
                    ADMIN_PASSWORD,
                    ADMIN_FIRSTNAME,
                    ADMIN_LASTNAME,
                    ADMIN_EMAIL,
                    roles);
        } catch (final Exception e) {
            // already exists
        }
    }

    private void createAdminDomainUser() {
        try {
            final UserRepository repo =
                    PersistenceContext.getInstance().userRepository();
            final EmailAddress emailVO = EmailAddress.valueOf(ADMIN_EMAIL);
            if (repo.ofIdentity(emailVO).isPresent()) return;

            final User admin = new User(
                    emailVO,
                    ADMIN_FIRSTNAME + " " + ADMIN_LASTNAME,
                    ADMIN_PHONE,
                    ADMIN_STAFF,
                    LocalDate.now().plusYears(10));
            admin.markAsAdmin();
            repo.save(admin);
        } catch (final Exception e) {
            System.err.println("[Bootstrap] ERROR creating admin: "
                    + e.getMessage());
        }
    }

    private void createAtccSystemUser() {
        try {
            final UserManagementService svc = AuthzRegistry.userService();
            final Set<Role> roles = new HashSet<>();
            roles.add(BaseRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
            svc.registerNewUser(
                    ATCC_EMAIL,
                    ATCC_PASSWORD,
                    ATCC_FIRSTNAME,
                    ATCC_LASTNAME,
                    ATCC_EMAIL,
                    roles);
        } catch (final Exception e) {
            // already exists
        }
    }

    private void createAtccDomainUser() {
        try {
            final UserRepository repo =
                    PersistenceContext.getInstance().userRepository();
            final EmailAddress emailVO = EmailAddress.valueOf(ATCC_EMAIL);
            if (repo.ofIdentity(emailVO).isPresent()) return;

            final User atcc = new User(
                    emailVO,
                    ATCC_FIRSTNAME + " " + ATCC_LASTNAME,
                    ATCC_PHONE,
                    ATCC_STAFF,
                    LocalDate.now().plusYears(10));
            repo.save(atcc);
        } catch (final Exception e) {
            System.err.println("[Bootstrap] ERROR creating ATCC user: "
                    + e.getMessage());
        }
    }
}