package mainUI.console;

import controller.LoginController;
import domain.BaseRoles;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import ui.ListUsersUI;
import ui.RegisterAirTransportCompanyUI;
import ui.RegisterUserUI;
import ui.RegisterAirControlAreaUI;
import ui.ToggleUserUI;
import ui.CreateEngineModelUI;

import java.util.Optional;

/**
 * Main console menu for the AlSafe backoffice system (US030).
 *
 * <p>Uses the EAPLI Framework's {@link AuthorizationService} for session
 * management and role-based authorisation:</p>
 * <ul>
 *   <li>{@code authz.session()} — checks if a user is currently logged in.</li>
 *   <li>{@code authz.isAuthenticatedUserAuthorizedTo(role)} — checks roles.</li>
 *   <li>{@code authz.clearSession()} — ends the session (logout).</li>
 * </ul>
 *
 * <p>Navigation depends on the current session state:</p>
 * <ul>
 *   <li><b>No session</b> — Login and Exit only.</li>
 *   <li><b>Admin session</b> — Full user management and company options.</li>
 *   <li><b>Regular user session</b> — Logout only (future US will expand this).</li>
 * </ul>
 */
public class MainMenu extends AbstractUI {

    /** Framework authorisation service — holds the current session. */
    private final AuthorizationService authz = AuthzRegistry.authorizationService();

    /** Controller for the login use case. */
    private final LoginController loginController = new LoginController();

    /**
     * Runs the menu loop until the user chooses to exit.
     *
     * @return {@code false} always
     */
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

    /** Renders the appropriate menu based on the current session state. */
    private void printMenu() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════╗");

        if (authz.session().isEmpty()) {
            // No active session
            System.out.println("  ║       AlSafe Backoffice — Main Menu      ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  1.  Login                               ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  0.  Exit                                ║");

        } else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.ADMIN)) {
            // Admin session
            final String name = currentUserName();
            System.out.printf( "  ║  Logged in : %-28s║%n", name);
            System.out.printf( "  ║  Role      : %-28s║%n", "ADMINISTRATOR");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  [ User Management ]                     ║");
            System.out.println("  ║  1.  Register User                       ║");
            System.out.println("  ║  2.  Enable/Disable User                 ║");
            System.out.println("  ║  3.  List Users                          ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  9.  Logout                              ║");
            System.out.println("  ║  0.  Exit                                ║");

        } else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.BACKOFFICE_OPERATOR)) {
            // ---- BACKOFFICE_OPERATOR: system configuration ----
            final String name = currentUserName();
            System.out.printf( "  ║  Logged in : %-28s║%n", name);
            System.out.printf( "  ║  Role      : %-28s║%n", "BACKOFFICE OPERATOR");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  [ Company Management ]                  ║");
            System.out.println("  ║  1.  Register Air Transport Company      ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  [ Backoffice Configuration ]            ║");
            System.out.println("  ║  2.  Register Air Control Area           ║");  // US050
            System.out.println("  ║  3.  Create Engine Model                 ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  9.  Logout                              ║");
            System.out.println("  ║  0.  Exit                                ║");

        } else {
            // Regular user session
            final String name = currentUserName();
            System.out.printf( "  ║  Logged in : %-28s║%n", name);
            System.out.printf( "  ║  Role      : %-28s║%n", "USER");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  9.  Logout                              ║");
            System.out.println("  ║  0.  Exit                                ║");
        }

        System.out.println("  ╚══════════════════════════════════════════╝");
    }

    // -------------------------------------------------------------------------
    // Option dispatching
    // -------------------------------------------------------------------------

    /**
     * Dispatches the chosen option to the appropriate action or UI.
     *
     * @param option the integer option entered by the user
     */
    private void handleOption(final int option) {
        if (authz.session().isEmpty()) {
            switch (option) {
                case 1: doLogin(); break;
                case 0: doExit();  break;
                default: System.out.println("  >> Invalid option. Please try again.");
            }
        } else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.ADMIN)) {
            switch (option) {
                case 1: new RegisterUserUI().show();                break;
                case 2: new ToggleUserUI().show();                  break;
                case 3: new ListUsersUI().show();                   break;
                case 9: doLogout();                                 break;
                case 0: doExit();                                   break;
                default: System.out.println("  >> Invalid option. Please try again.");
            }

        } else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.BACKOFFICE_OPERATOR)) {
            switch (option) {
                case 1: new RegisterAirTransportCompanyUI().show(); break;
                case 2: new RegisterAirControlAreaUI().show();      break;
                case 3: new CreateEngineModelUI().show();           break;
                case 9: doLogout();                                  break;
                case 0: doExit();                                    break;
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

    /** Handles the login flow — reads credentials and authenticates via the framework. */
    private void doLogin() {
        System.out.println("\n  --- Login ---");
        final String email = Console.readLine("  Email:");
        final String pass  = Console.readLine("  Password:");

        final Optional<UserSession> result = loginController.login(email, pass);

        if (result.isPresent()) {
            System.out.println("\n  >> Login successful! Welcome, " + currentUserName() + ".");
            if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.ADMIN)) {
                System.out.println("     Role: ADMINISTRATOR");
            }else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.BACKOFFICE_OPERATOR)) {
                System.out.println("     Role: BACKOFFICE OPERATOR");
            }
        } else {
            System.out.println("\n  >> Login failed: incorrect email/password or account inactive.");
        }
    }

    /** Ends the current session via the framework and returns to the public menu. */
    private void doLogout() {
        final String name = currentUserName();
        authz.clearSession();
        System.out.println("\n  >> Logged out. Goodbye, " + name + ".");
    }

    /** Terminates the application. */
    private void doExit() {
        System.out.println("\n  Shutting down AlSafe. Goodbye!");
        System.exit(0);
    }

    /**
     * Returns the name of the currently authenticated user,
     * or "User" if no session is active.
     *
     * @return the user's first name from the framework session
     */
    private String currentUserName() {
        return authz.session()
                .map(UserSession::authenticatedUser)
                .map(u -> u.name().firstName())
                .orElse("User");
    }

    /**
     * {@inheritDoc}
     *
     * @return the headline shown above this menu
     */
    @Override
    public String headline() {
        return "AlSafe Backoffice";
    }
}
