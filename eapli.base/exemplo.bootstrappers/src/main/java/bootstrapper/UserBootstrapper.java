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

    private static final String BO_EMAIL     = "backoffice@alsafe.com";
    private static final String BO_PASSWORD  = "Backoffice1";
    private static final String BO_FIRSTNAME = "Backoffice";
    private static final String BO_LASTNAME  = "Operator";
    private static final String BO_PHONE     = "910000002";
    private static final String BO_STAFF     = "STAFF-BO-001";

    private static final String PILOT_EMAIL     = "pilot@alsafe.com";
    private static final String PILOT_PASSWORD  = "Pilot1234";
    private static final String PILOT_FIRSTNAME = "AlSafe";
    private static final String PILOT_LASTNAME  = "Pilot";
    private static final String PILOT_PHONE     = "910000003";
    private static final String PILOT_STAFF     = "STAFF-PILOT-001";

    @Override
    public boolean execute() {
        loadValidDomains();
        createAdminSystemUser();
        createAdminDomainUser();
        createAtccSystemUser();
        createAtccDomainUser();
        createBackofficeOperatorSystemUser();
        createBackofficeOperatorDomainUser();
        createPilotSystemUser();
        createPilotDomainUser();
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

    private void createBackofficeOperatorSystemUser() {
        try {
            final UserManagementService svc = AuthzRegistry.userService();
            final Set<Role> roles = new HashSet<>();
            roles.add(BaseRoles.BACKOFFICE_OPERATOR);
            svc.registerNewUser(
                    BO_EMAIL,
                    BO_PASSWORD,
                    BO_FIRSTNAME,
                    BO_LASTNAME,
                    BO_EMAIL,
                    roles);
        } catch (final Exception e) {
            // already exists
        }
    }

    private void createBackofficeOperatorDomainUser() {
        try {
            final UserRepository repo =
                    PersistenceContext.getInstance().userRepository();
            final EmailAddress emailVO = EmailAddress.valueOf(BO_EMAIL);
            if (repo.ofIdentity(emailVO).isPresent()) return;

            final User bo = new User(
                    emailVO,
                    BO_FIRSTNAME + " " + BO_LASTNAME,
                    BO_PHONE,
                    BO_STAFF,
                    LocalDate.now().plusYears(10));
            bo.setRole(BaseRoles.BACKOFFICE_OPERATOR.toString());
            repo.save(bo);
        } catch (final Exception e) {
            System.err.println("[Bootstrap] ERROR creating Backoffice Operator: "
                    + e.getMessage());
        }
    }

    private void createPilotSystemUser() {
        try {
            final UserManagementService svc = AuthzRegistry.userService();
            final Set<Role> roles = new HashSet<>();
            roles.add(BaseRoles.PILOT);
            svc.registerNewUser(
                    PILOT_EMAIL,
                    PILOT_PASSWORD,
                    PILOT_FIRSTNAME,
                    PILOT_LASTNAME,
                    PILOT_EMAIL,
                    roles);
        } catch (final Exception e) {
            // already exists
        }
    }

    private void createPilotDomainUser() {
        try {
            final UserRepository repo =
                    PersistenceContext.getInstance().userRepository();
            final EmailAddress emailVO = EmailAddress.valueOf(PILOT_EMAIL);
            if (repo.ofIdentity(emailVO).isPresent()) return;

            final User pilot = new User(
                    emailVO,
                    PILOT_FIRSTNAME + " " + PILOT_LASTNAME,
                    PILOT_PHONE,
                    PILOT_STAFF,
                    LocalDate.now().plusYears(10));
            repo.save(pilot);
        } catch (final Exception e) {
            System.err.println("[Bootstrap] ERROR creating Pilot: "
                    + e.getMessage());
        }
    }
}