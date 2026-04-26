package mainUI.console;

import controller.LoginController;
import domain.User;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import ui.ListUsersUI;
import ui.RegisterAirTransportCompanyUI;
import ui.RegisterUserUI;
import ui.ToggleUserUI;

import java.util.Optional;

/**
 * Main console menu for the AlSafe backoffice system.
 *
 * <p>Navigation depends on the current session state:</p>
 * <ul>
 *   <li><b>No session</b> — Login and Exit only.
 *       User registration is an admin operation and requires authentication.</li>
 *   <li><b>Admin session</b> — Full user management and company options.</li>
 *   <li><b>Regular user session</b> — Logout only (future US will expand this).</li>
 * </ul>
 */
public class MainMenu extends AbstractUI {

    private final LoginController loginController = new LoginController();
    private User currentUser = null;

    @Override
    protected boolean doShow() {
        int option = -1;
        while (option != 0) {
            printMenu();
            option = Console.readInteger("Option");
            handleOption(option);
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Menu rendering
    // -------------------------------------------------------------------------

    private void printMenu() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════╗");

        if (currentUser == null) {
            System.out.println("  ║       AlSafe Backoffice — Main Menu      ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  1.  Login                               ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  0.  Exit                                ║");

        } else if (currentUser.isAdmin()) {
            System.out.printf( "  ║  Logged in : %-28s║%n", currentUser.getName());
            System.out.printf( "  ║  Role      : %-28s║%n", "ADMINISTRATOR");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  [ User Management ]                     ║");
            System.out.println("  ║  1.  Register User                       ║");
            System.out.println("  ║  2.  Enable/Disable User                 ║");
            System.out.println("  ║  3.  List Users                          ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  [ Company Management ]                  ║");
            System.out.println("  ║  4.  Register Air Transport Company      ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  9.  Logout                              ║");
            System.out.println("  ║  0.  Exit                                ║");

        } else {
            System.out.printf( "  ║  Logged in : %-28s║%n", currentUser.getName());
            System.out.printf( "  ║  Role      : %-28s║%n", currentUser.getRole());
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  9.  Logout                              ║");
            System.out.println("  ║  0.  Exit                                ║");
        }

        System.out.println("  ╚══════════════════════════════════════════╝");
    }

    // -------------------------------------------------------------------------
    // Option dispatching
    // -------------------------------------------------------------------------

    private void handleOption(final int option) {
        if (currentUser == null) {
            switch (option) {
                case 1: doLogin(); break;
                case 0: doExit();  break;
                default: System.out.println("  >> Invalid option. Please try again.");
            }
        } else if (currentUser.isAdmin()) {
            switch (option) {
                case 1: new RegisterUserUI().show();                break;
                case 2: new ToggleUserUI().show();                  break;
                case 3: new ListUsersUI().show();                   break;
                case 4: new RegisterAirTransportCompanyUI().show(); break;
                case 9: doLogout();                                 break;
                case 0: doExit();                                   break;
                default: System.out.println("  >> Invalid option. Please try again.");
            }
        } else {
            switch (option) {
                case 9: doLogout(); break;
                case 0: doExit();   break;
                default: System.out.println("  >> Invalid option or insufficient permissions.");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    private void doLogin() {
        System.out.println("\n  --- Login ---");
        final String email = Console.readLine("  Email   :");
        final String pass  = Console.readLine("  Password:");

        final Optional<User> result = loginController.login(email, pass);
        if (result.isPresent()) {
            currentUser = result.get();
            System.out.println("\n  >> Login successful! Welcome, " + currentUser.getName() + ".");
            if (currentUser.isAdmin()) System.out.println("     Role: ADMINISTRATOR");
        } else {
            System.out.println("\n  >> Login failed: incorrect email/password or account inactive.");
        }
    }

    private void doLogout() {
        System.out.println("\n  >> Logged out. Goodbye, " + currentUser.getName() + ".");
        currentUser = null;
    }

    private void doExit() {
        System.out.println("\n  Shutting down AlSafe. Goodbye!");
        System.exit(0);
    }

    @Override
    public String headline() {
        return "AlSafe Backoffice";
    }
}
