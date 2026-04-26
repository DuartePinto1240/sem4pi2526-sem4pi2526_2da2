import bootstrapper.Bootstrap;
import mainUI.console.MainMenu;

/**
 * Application entry point for the AlSafe backoffice system.
 *
 * <p>Runs the bootstrap process to ensure base data exists (admin account),
 * then launches the main console menu in a loop until the user exits.</p>
 */
public class Main {

    /**
     * Starts the AlSafe application.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(final String[] args) {
        printBanner();
        new Bootstrap().execute();

        try {
            new MainMenu().show();
        } catch (Exception e) {
            System.out.println("Fatal error: " + e.getMessage());
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