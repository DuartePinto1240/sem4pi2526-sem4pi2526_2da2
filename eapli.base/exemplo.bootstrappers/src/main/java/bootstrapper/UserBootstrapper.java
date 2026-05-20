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

    private static final String WP_EMAIL     = "wtps@alsafe.com";
    private static final String WP_PASSWORD  = "1234";
    private static final String WP_FIRSTNAME = "AlSafe";
    private static final String WP_LASTNAME  = "WeatherPerson";
    private static final String WP_PHONE     = "910000004";
    private static final String WP_STAFF     = "STAFF-WP-001";

    private static final String FCO_EMAIL     = "ftctrl@alsafe.com";
    private static final String FCO_PASSWORD  = "1234";
    private static final String FCO_FIRSTNAME = "AlSafe";
    private static final String FCO_LASTNAME  = "FlightControl";
    private static final String FCO_PHONE     = "910000005";
    private static final String FCO_STAFF     = "STAFF-FCO-001";

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
        createWeatherPersonSystemUser();
        createWeatherPersonDomainUser();
        createFlightControlOperatorSystemUser();
        createFlightControlOperatorDomainUser();
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
            atcc.setRole(BaseRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR.toString());
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
            pilot.setRole(BaseRoles.PILOT.toString());
            repo.save(pilot);
        } catch (final Exception e) {
            System.err.println("[Bootstrap] ERROR creating Pilot: "
                    + e.getMessage());
        }
    }

    private void createWeatherPersonSystemUser() {
        try {
            final UserManagementService svc = AuthzRegistry.userService();
            final Set<Role> roles = new HashSet<>();
            roles.add(BaseRoles.WEATHER_PERSON);
            svc.registerNewUser(
                    WP_EMAIL,
                    WP_PASSWORD,
                    WP_FIRSTNAME,
                    WP_LASTNAME,
                    WP_EMAIL,
                    roles);
        } catch (final Exception e) {
            // already exists
        }
    }

    private void createWeatherPersonDomainUser() {
        try {
            final UserRepository repo =
                    PersistenceContext.getInstance().userRepository();
            final EmailAddress emailVO = EmailAddress.valueOf(WP_EMAIL);
            if (repo.ofIdentity(emailVO).isPresent()) return;

            final User wp = new User(
                    emailVO,
                    WP_FIRSTNAME + " " + WP_LASTNAME,
                    WP_PHONE,
                    WP_STAFF,
                    LocalDate.now().plusYears(10));
            wp.setRole(BaseRoles.WEATHER_PERSON.toString());
            repo.save(wp);
        } catch (final Exception e) {
            System.err.println("[Bootstrap] ERROR creating Weather Person: "
                    + e.getMessage());
        }
    }

    private void createFlightControlOperatorSystemUser() {
        try {
            final UserManagementService svc = AuthzRegistry.userService();
            final Set<Role> roles = new HashSet<>();
            roles.add(BaseRoles.FLIGHT_CONTROL_OPERATOR);
            svc.registerNewUser(
                    FCO_EMAIL,
                    FCO_PASSWORD,
                    FCO_FIRSTNAME,
                    FCO_LASTNAME,
                    FCO_EMAIL,
                    roles);
        } catch (final Exception e) {
            // already exists
        }
    }

    private void createFlightControlOperatorDomainUser() {
        try {
            final UserRepository repo =
                    PersistenceContext.getInstance().userRepository();
            final EmailAddress emailVO = EmailAddress.valueOf(FCO_EMAIL);
            if (repo.ofIdentity(emailVO).isPresent()) return;

            final User fco = new User(
                    emailVO,
                    FCO_FIRSTNAME + " " + FCO_LASTNAME,
                    FCO_PHONE,
                    FCO_STAFF,
                    LocalDate.now().plusYears(10));
            fco.setRole(BaseRoles.FLIGHT_CONTROL_OPERATOR.toString());
            repo.save(fco);
        } catch (final Exception e) {
            System.err.println("[Bootstrap] ERROR creating Flight Control Operator: "
                    + e.getMessage());
        }
    }
}