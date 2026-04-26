package bootstrapper;

import controller.RegisterUserController;
import domain.ValidEmailDomains;
import eapli.framework.actions.Action;

import java.time.LocalDate;

/**
 * Bootstrap action that:
 * <ol>
 *   <li>Registers the valid AISafe internal email domains (section 3.1.1).</li>
 *   <li>Ensures the default administrator account exists.</li>
 * </ol>
 *
 * <p>Executed once at application startup. If the admin account already exists
 * (e.g. from a previous run in the H2 database), step 2 is a no-op.</p>
 *
 * <p>Default admin credentials:</p>
 * <ul>
 *   <li>Email:    {@value #ADMIN_EMAIL}</li>
 *   <li>Password: {@value #ADMIN_PASSWORD}</li>
 *   <li>Staff No: {@value #ADMIN_STAFF}</li>
 * </ul>
 */
public class UserBootstrapper implements Action {

    private static final String ADMIN_EMAIL    = "admin@alsafe.com";
    private static final String ADMIN_PASSWORD = "Admin1234";
    private static final String ADMIN_NAME     = "AlSafe Administrator";
    private static final String ADMIN_PHONE    = "910000000";
    private static final String ADMIN_STAFF    = "STAFF-ADMIN-001";

    /**
     * Loads valid email domains and creates the admin account if needed.
     *
     * @return {@code true} if completed without errors
     */
    @Override
    public boolean execute() {
        loadValidDomains();
        createAdminIfAbsent();
        return true;
    }

    /**
     * Registers the valid AISafe internal email domains.
     * These are the only domains accepted for backoffice user registration.
     */
    private void loadValidDomains() {
        ValidEmailDomains.register("alsafe.com");
        ValidEmailDomains.register("alsafe.pt");
        ValidEmailDomains.register("isep.ipp.pt");
    }

    /**
     * Creates the default admin account if it does not already exist.
     * The admin's security clearance is set to 10 years from today.
     */
    private void createAdminIfAbsent() {
        try {
            final LocalDate clearanceExpiry = LocalDate.now().plusYears(10);
            new RegisterUserController().registerAdmin(
                    ADMIN_EMAIL, ADMIN_NAME, ADMIN_PHONE,
                    ADMIN_PASSWORD, ADMIN_STAFF, clearanceExpiry);
        } catch (Exception e) {
            // Admin already exists — silently skip
        }
    }
}
