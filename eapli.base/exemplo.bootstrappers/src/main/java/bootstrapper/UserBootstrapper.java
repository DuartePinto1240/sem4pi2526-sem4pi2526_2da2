package bootstrapper;

import domain.AirTransportCompany;
import domain.BaseRoles;
import domain.CompanyCode;
import domain.EmailAddress;
import domain.User;
import domain.ValidEmailDomains;
import eapli.framework.actions.Action;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.Role;
import persistence.PersistenceContext;
import repositories.AirTransportCompanyRepository;
import repositories.UserRepository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Bootstrap data loader that creates one account per role for demo/testing.
 *
 * <p><strong>Order of operations per account (mirrors the production rule):</strong>
 * <ol>
 *   <li>Build and validate the domain {@link User} — if the data is wrong the
 *       bootstrap entry is skipped without creating any SystemUser.</li>
 *   <li>Register the SystemUser in the EAPLI framework (credentials + role).</li>
 *   <li>Persist the domain User.</li>
 * </ol>
 * Every step is wrapped individually so a single bad entry does not abort the
 * whole bootstrap sequence.
 * </p>
 */
public class UserBootstrapper implements Action {

    private static final LocalDate FAR_FUTURE = LocalDate.now().plusYears(10);

    // -------------------------------------------------------------------------
    // Account constants
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    @Override
    public boolean execute() {
        loadValidDomains();

        bootstrapAdmin();
        bootstrapUser(ATCC_EMAIL,  ATCC_PASSWORD,  ATCC_FIRSTNAME,  ATCC_LASTNAME,
                ATCC_PHONE,  ATCC_STAFF,  BaseRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
        bootstrapUser(BO_EMAIL,    BO_PASSWORD,    BO_FIRSTNAME,    BO_LASTNAME,
                BO_PHONE,    BO_STAFF,    BaseRoles.BACKOFFICE_OPERATOR);
        bootstrapUser(PILOT_EMAIL, PILOT_PASSWORD, PILOT_FIRSTNAME, PILOT_LASTNAME,
                PILOT_PHONE, PILOT_STAFF, BaseRoles.PILOT);
        bootstrapUser(WP_EMAIL,    WP_PASSWORD,    WP_FIRSTNAME,    WP_LASTNAME,
                WP_PHONE,    WP_STAFF,    BaseRoles.WEATHER_PERSON);
        bootstrapUser(FCO_EMAIL,   FCO_PASSWORD,   FCO_FIRSTNAME,   FCO_LASTNAME,
                FCO_PHONE,   FCO_STAFF,   BaseRoles.FLIGHT_CONTROL_OPERATOR);

        bootstrapCompanyCollaborators();

        return true;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void loadValidDomains() {
        ValidEmailDomains.register("alsafe.com");
        ValidEmailDomains.register("alsafe.pt");
        ValidEmailDomains.register("isep.ipp.pt");
    }

    /**
     * Bootstraps the admin account following the correct order:
     * domain User validated first, SystemUser registered only on success.
     */
    private void bootstrapAdmin() {
        try {
            final UserRepository repo = PersistenceContext.getInstance().userRepository();
            final EmailAddress emailVO = EmailAddress.valueOf(ADMIN_EMAIL);
            if (repo.ofIdentity(emailVO).isPresent()) return;

            // Step 1 — build and validate domain User
            final User admin = new User(
                    emailVO,
                    ADMIN_FIRSTNAME + " " + ADMIN_LASTNAME,
                    ADMIN_PHONE,
                    ADMIN_STAFF,
                    FAR_FUTURE);
            admin.markAsAdmin();

            // Step 2 — only register SystemUser if domain User is valid
            final Set<Role> roles = new HashSet<>();
            roles.add(BaseRoles.ADMIN);
            AuthzRegistry.userService().registerNewUser(
                    ADMIN_EMAIL, ADMIN_PASSWORD,
                    ADMIN_FIRSTNAME, ADMIN_LASTNAME,
                    ADMIN_EMAIL, roles);

            // Step 3 — persist domain User
            repo.save(admin);

        } catch (final Exception e) {
            System.err.println("[Bootstrap] ERROR creating admin: " + e.getMessage());
        }
    }

    /**
     * Bootstraps a generic user account following the correct order:
     * domain User validated first, SystemUser registered only on success.
     *
     * @param email     account email (also used as username)
     * @param password  plain-text password (bootstrap only)
     * @param firstName first name for the framework
     * @param lastName  last name for the framework
     * @param phone     9-digit phone number
     * @param staff     unique staff number
     * @param role      framework role to assign
     */
    private void bootstrapUser(final String email, final String password,
                               final String firstName, final String lastName,
                               final String phone, final String staff,
                               final Role role) {
        try {
            final UserRepository repo = PersistenceContext.getInstance().userRepository();
            final EmailAddress emailVO = EmailAddress.valueOf(email);

            // If the user already exists but has the wrong role (stale bootstrap data),
            // correct it in place — do NOT recreate the SystemUser.
            if (repo.ofIdentity(emailVO).isPresent()) {
                final User existing = repo.ofIdentity(emailVO).get();
                if (!role.toString().equals(existing.getRole())) {
                    existing.setRole(role.toString());
                    repo.save(existing);
                }
                return;
            }

            // Step 1 — build and validate domain User
            final User user = new User(
                    emailVO,
                    firstName + " " + lastName,
                    phone,
                    staff,
                    FAR_FUTURE);
            user.setRole(role.toString());

            // Step 2 — only register SystemUser if domain User is valid
            final Set<Role> roles = new HashSet<>();
            roles.add(role);
            AuthzRegistry.userService().registerNewUser(
                    email, password, firstName, lastName, email, roles);

            // Step 3 — persist domain User
            repo.save(user);

        } catch (final Exception e) {
            System.err.println("[Bootstrap] ERROR creating user " + email
                    + ": " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // ATCC logins per company
    // -------------------------------------------------------------------------

    /**
     * Creates one ATCC SystemUser per air transport company registered by
     * AirTransportCompanyBootstrapper, and links each as a Collaborator of
     * their respective company so logins resolve to the right company.
     */
    private void bootstrapCompanyCollaborators() {
        bootstrapCompanyAtcc("tap@alsafe.com",       "Atcc1234", "TAP",       "Collaborator", "910100001", "STAFF-ATCC-TP",  "TP", "TAP");
        bootstrapCompanyAtcc("ryanair@alsafe.com",   "Atcc1234", "Ryanair",   "Collaborator", "910100002", "STAFF-ATCC-FR",  "FR", "RYR");
        bootstrapCompanyAtcc("easyjet@alsafe.com",   "Atcc1234", "EasyJet",   "Collaborator", "910100003", "STAFF-ATCC-U2",  "EZ", "EZY");
        bootstrapCompanyAtcc("lufthansa@alsafe.com", "Atcc1234", "Lufthansa", "Collaborator", "910100004", "STAFF-ATCC-LH",  "LH", "DLH");
        bootstrapCompanyAtcc("airfrance@alsafe.com", "Atcc1234", "AirFrance", "Collaborator", "910100005", "STAFF-ATCC-AF",  "AF", "AFR");
    }

    /**
     * Creates a SystemUser with AIR_TRANSPORT_COMPANY_COLLABORATOR role and
     * adds them as a Collaborator of the company identified by iata/icao.
     * Skips user creation if it already exists; skips the collaborator link
     * if the company doesn't exist yet or already has this collaborator.
     */
    private void bootstrapCompanyAtcc(final String email, final String password,
                                      final String firstName, final String lastName,
                                      final String phone, final String staff,
                                      final String iata, final String icao) {
        try {
            final UserRepository repo = PersistenceContext.getInstance().userRepository();
            final EmailAddress emailVO = EmailAddress.valueOf(email);

            if (repo.ofIdentity(emailVO).isEmpty()) {
                final User user = new User(
                        emailVO,
                        firstName + " " + lastName,
                        phone,
                        staff,
                        FAR_FUTURE);
                user.setRole(BaseRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR.toString());

                final Set<Role> roles = new HashSet<>();
                roles.add(BaseRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR);
                AuthzRegistry.userService().registerNewUser(
                        email, password, firstName, lastName, email, roles);

                repo.save(user);
            }

            linkCollaboratorToCompany(email, firstName + " " + lastName, "Operations Manager", iata, icao);

        } catch (final Exception e) {
            System.err.println("[Bootstrap] ERROR creating company ATCC " + email + ": " + e.getMessage());
        }
    }

    private void linkCollaboratorToCompany(final String email, final String name,
                                           final String position,
                                           final String iata, final String icao) {
        try {
            final AirTransportCompanyRepository companyRepo =
                    PersistenceContext.getInstance().airTransportCompanyRepository();

            final AirTransportCompany company =
                    companyRepo.ofIdentity(new CompanyCode(iata, icao)).orElse(null);

            if (company == null) return; // company not bootstrapped yet
            if (company.findCollaboratorByEmail(email).isPresent()) return;

            company.addCollaborator(email, name, position);
            companyRepo.save(company);

        } catch (final Exception e) {
            System.err.println("[Bootstrap] ERROR linking collaborator " + email
                    + " to " + iata + "/" + icao + ": " + e.getMessage());
        }
    }
}