package mainUI.console;

import controller.LoginController;
import domain.BaseRoles;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import ui.*;

import java.util.Optional;

public class MainMenu extends AbstractUI {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final LoginController loginController = new LoginController();

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

    private void printMenu() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════╗");

        if (authz.session().isEmpty()) {
            System.out.println("  ║       AlSafe Backoffice — Main Menu      ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  1.  Login                               ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  0.  Exit                                ║");

        } else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.ADMIN)) {
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
            final String name = currentUserName();
            System.out.printf( "  ║  Logged in : %-28s║%n", name);
            System.out.printf( "  ║  Role      : %-28s║%n", "BACKOFFICE OPERATOR");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  [ Company Management ]                  ║");
            System.out.println("  ║  1.  Register Air Transport Company      ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  [ Backoffice Configuration ]            ║");
            System.out.println("  ║  2.  Register Air Control Area           ║");
            System.out.println("  ║  3.  Create Airport                      ║");
            System.out.println("  ║  4.  Create Engine Model                 ║");
            System.out.println("  ║  5.  Create Aircraft Model               ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  9.  Logout                              ║");
            System.out.println("  ║  0.  Exit                                ║");

        } else if (authz.isAuthenticatedUserAuthorizedTo(
                BaseRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR)) {
            final String name = currentUserName();
            System.out.printf( "  ║  Logged in : %-28s║%n", name);
            System.out.printf( "  ║  Role      : %-28s║%n", "ATCC");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  [ Fleet Management ]                    ║");
            System.out.println("  ║  1.  Add Aircraft to Fleet (US070)       ║");
            System.out.println("  ║  2.  List Company Fleet    (US072)       ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  9.  Logout                              ║");
            System.out.println("  ║  0.  Exit                                ║");

        } else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.WEATHER_PERSON)) {
            final String name = currentUserName();
            System.out.printf( "  ║  Logged in : %-28s║%n", name);
            System.out.printf( "  ║  Role      : %-28s║%n", "WEATHER PERSON");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  [ Weather Service ]                     ║");
            System.out.println("  ║  1.  Register Weather Data               ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  9.  Logout                              ║");
            System.out.println("  ║  0.  Exit                                ║");

        } else {
            final String name = currentUserName();
            System.out.printf( "  ║  Logged in : %-28s║%n", name);
            System.out.printf( "  ║  Role      : %-28s║%n", "USER");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  9.  Logout                              ║");
            System.out.println("  ║  0.  Exit                                ║");
        }

        System.out.println("  ╚══════════════════════════════════════════╝");
    }

    private void handleOption(final int option) {
        if (authz.session().isEmpty()) {
            switch (option) {
                case 1: doLogin(); break;
                case 0: doExit();  break;
                default: System.out.println("  >> Invalid option. Please try again.");
            }

        } else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.ADMIN)) {
            switch (option) {
                case 1: new RegisterUserUI().show();   break;
                case 2: new ToggleUserUI().show();     break;
                case 3: new ListUsersUI().show();      break;
                case 9: doLogout();                    break;
                case 0: doExit();                      break;
                default: System.out.println("  >> Invalid option. Please try again.");
            }

        } else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.BACKOFFICE_OPERATOR)) {
            switch (option) {
                case 1: new RegisterAirTransportCompanyUI().show(); break;
                case 2: new RegisterAirControlAreaUI().show();      break;
                case 3: new CreateAirportUI().show();               break;
                case 4: new CreateEngineModelUI().show();           break;
                case 5: new CreateAircraftModelUI().show();         break;
                case 9: doLogout();                                 break;
                case 0: doExit();                                   break;
                default: System.out.println("  >> Invalid option. Please try again.");
            }

        } else if (authz.isAuthenticatedUserAuthorizedTo(
                BaseRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR)) {
            switch (option) {
                case 1: new AddAircraftToFleetUI().show();  break;
                case 2: new ListCompanyFleetUI().show();    break;
                case 9: doLogout();                         break;
                case 0: doExit();                           break;
                default: System.out.println("  >> Invalid option. Please try again.");
            }

        } else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.WEATHER_PERSON)) {
           switch (option) {
               case 1: new RegisterWeatherDataUI().show(); break;
               case 9: doLogout();                         break;
               case 0: doExit();                           break;
               default: System.out.println("  >> Invalid option. Please try again.");
           }

        } else {
            switch (option) {
                case 9: doLogout(); break;
                case 0: doExit();   break;
                default: System.out.println(
                        "  >> Invalid option or insufficient permissions.");
            }
        }
    }

    private void doLogin() {
        System.out.println("\n  --- Login ---");
        final String email = Console.readLine("  Email:");
        final String pass  = Console.readLine("  Password:");

        final Optional<UserSession> result = loginController.login(email, pass);

        if (result.isPresent()) {
            System.out.println("\n  >> Login successful! Welcome, "
                    + currentUserName() + ".");
            if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.ADMIN)) {
                System.out.println("     Role: ADMINISTRATOR");
            } else if (authz.isAuthenticatedUserAuthorizedTo(
                    BaseRoles.BACKOFFICE_OPERATOR)) {
                System.out.println("     Role: BACKOFFICE OPERATOR");
            } else if (authz.isAuthenticatedUserAuthorizedTo(
                    BaseRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR)) {
                System.out.println("     Role: ATCC");
            }
        } else {
            System.out.println(
                    "\n  >> Login failed: incorrect email/password or account inactive.");
        }
    }

    private void doLogout() {
        final String name = currentUserName();
        authz.clearSession();
        System.out.println("\n  >> Logged out. Goodbye, " + name + ".");
    }

    private void doExit() {
        System.out.println("\n  Shutting down AlSafe. Goodbye!");
        System.exit(0);
    }

    private String currentUserName() {
        return authz.session()
                .map(UserSession::authenticatedUser)
                .map(u -> u.name().firstName())
                .orElse("User");
    }

    @Override
    public String headline() {
        return "AlSafe Backoffice";
    }
}