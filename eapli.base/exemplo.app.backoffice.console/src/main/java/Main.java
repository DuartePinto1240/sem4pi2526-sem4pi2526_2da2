import bootstrapper.Bootstrap;
import domain.AlSafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import mainUI.console.MainMenu;

import java.util.HashMap;

/**
 * Application entry point for the AlSafe backoffice system.
 *
 * <p>Startup sequence:</p>
 * <ol>
 *   <li>Configure {@code AuthzRegistry} with the JPA user repository,
 *       password policy and encoder — required for all authentication
 *       and authorisation operations (US030).</li>
 *   <li>Run the bootstrap process (creates admin account and default data).</li>
 *   <li>Launch the main console menu.</li>
 * </ol>
 */
public class Main {

    private static String loadPersistenceUnit() {
        final java.util.Properties props = new java.util.Properties();
        try (final java.io.InputStream is = Main.class
                .getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) props.load(is);
        } catch (final java.io.IOException e) {
            // fall back to default
        }
        return props.getProperty("persistence.persistenceUnit", "eapli.base.AlSafePU");
    }

    /**
     * Starts the AlSafe application.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(final String[] args) {
        printBanner();

        // 1. Configure AuthzRegistry — must be done before any bootstrap or login
        final String persistenceUnit = loadPersistenceUnit();
        AuthzRegistry.configure(
                new JpaAutoTxUserRepository(persistenceUnit, new HashMap<>()),
                new AlSafePasswordPolicy(),
                new PlainTextEncoder()
        );

        // 2. Bootstrap (domains, admin account, company data)
        new Bootstrap().execute();

        // 3. Launch menu
        try {
            new MainMenu().show();
        } catch (final Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Prints the startup ASCII art banner. */
    private static void printBanner() {
        System.out.println();
        System.out.println("  ___  ___ ___       __");
        System.out.println(" / _ \\|_ _/ __| __ _/ _|___");
        System.out.println("| (_) || |\\__ \\/ _` |  _/ -_)");
        System.out.println(" \\__,_|___|___/\\__,_|_| \\___|");
        System.out.println();
        System.out.println("  Flight Control Management System");
        System.out.println("  Backoffice v1.0  |  Sprint 2");
        System.out.println("  --------------------------------");
        System.out.println("  Initialising...");
        System.out.println();
    }
}
