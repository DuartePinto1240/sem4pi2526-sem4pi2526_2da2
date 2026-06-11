package mainUI.console;

import controller.LoginController;
import domain.BaseRoles;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import persistence.PersistenceContext;
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
            printLoggedInHeader("ADMINISTRATOR");
            System.out.println("  ║  [ User Management ]                     ║");
            System.out.println("  ║  1.  Register User                       ║");
            System.out.println("  ║  2.  Enable/Disable User                 ║");
            System.out.println("  ║  3.  List Users                          ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            printLogoutExit();

        } else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.BACKOFFICE_OPERATOR)) {
            printLoggedInHeader("BACKOFFICE OPERATOR");
            System.out.println("  ║  [ Company Management ]                  ║");
            System.out.println("  ║  1.  Register Air Transport Company      ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  [ Backoffice Configuration ]            ║");
            System.out.println("  ║  2.  Register Air Control Area           ║");
            System.out.println("  ║  3.  Create Airport                      ║");
            System.out.println("  ║  4.  Create Engine Model                 ║");
            System.out.println("  ║  5.  Create Aircraft Model               ║");
            System.out.println("  ║  6.  Add Engine Model to Aircraft Model  ║");
            System.out.println("  ║  7.  Remove Engine from Aircraft Model   ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  [ Collaborator Management ]             ║");
            System.out.println("  ║  8.  Add Customer's Collaborator         ║");
            System.out.println("  ║  9.  List Customer's Collaborators       ║");
            System.out.println("  ║  10.  Edit Customer's Collaborator       ║");
            System.out.println("  ║  11. Disable Customer's Collaborator     ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            printLogoutExit();

        } else if (authz.isAuthenticatedUserAuthorizedTo(
                BaseRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR)) {
            printLoggedInHeader("ATCC");
            System.out.println("  ║  [ Fleet Management ]                    ║");
            System.out.println("  ║  1.  Add Aircraft to Fleet               ║");
            System.out.println("  ║  2.  List Company Fleet                  ║");
            System.out.println("  ║  3.  Decommission Aircraft               ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  [ Route Management ]                    ║");
            System.out.println("  ║  4.  Create Flight Route                 ║");
            System.out.println("  ║  5.  Delete Flight Route                 ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  [ Pilot Management ]                    ║");
            System.out.println("  ║  6.  Add Pilot                           ║");
            System.out.println("  ║  7.  List Pilot Roster                   ║");
            System.out.println("  ║  8.  Remove Pilot                        ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            printLogoutExit();

        } else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.WEATHER_PERSON)) {
            printLoggedInHeader("WEATHER PERSON");
            System.out.println("  ║  [ Weather Service ]                     ║");
            System.out.println("  ║  1.  Register Weather Data               ║");
            System.out.println("  ║  2.  Import Bulk Weather Data            ║");
            System.out.println("  ║  3.  Consult Weather Data                ║");
            System.out.println("  ║  4.  Export Weather Data for Simulation  ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            printLogoutExit();

        } else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.PILOT)) {
            printLoggedInHeader("PILOT");
            System.out.println("  ║  [ Flight Management ]                   ║");
            System.out.println("  ║  1.  Create Flight Plan                  ║");
            System.out.println("  ║  2.  Import Flight Plan from File        ║");
            System.out.println("  ║  3.  Insert Weather Data                 ║");
            System.out.println("  ║  4.  Test/Validate Flight Plan           ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            printLogoutExit();

        } else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.FLIGHT_CONTROL_OPERATOR)) {
            printLoggedInHeader("FLIGHT CONTROL OPERATOR");
            System.out.println("  ║  [ Simulation ]                          ║");
            System.out.println("  ║  1.  Generate Simulation Report          ║");
            System.out.println("  ║  2.  Monthly Statistics Report           ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            printLogoutExit();

        } else {
            printLoggedInHeader("USER");
            printLogoutExit();
        }

        System.out.println("  ╚══════════════════════════════════════════╝");
    }

    private void printLoggedInHeader(final String roleLabel) {
        System.out.printf("  ║  Logged in : %-28s║%n", currentUserName());
        System.out.printf("  ║  Role      : %-28s║%n", roleLabel);
        final String company = currentUserCompany();
        if (!company.isEmpty()) {
            System.out.printf("  ║  Company   : %-28s║%n", company);
        }
        System.out.println("  ╠══════════════════════════════════════════╣");
    }

    private String currentUserCompany() {
        final String email = authz.session()
                .map(UserSession::authenticatedUser)
                .map(u -> u.username().toString())
                .orElse(null);
        if (email == null) return "";

        try {
            if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.PILOT)) {
                return PersistenceContext.getInstance().pilotRepository()
                        .findByEmail(email)
                        .map(p -> p.company().name())
                        .orElse("");
            }
            if (authz.isAuthenticatedUserAuthorizedTo(
                    BaseRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR)) {
                return PersistenceContext.getInstance().airTransportCompanyRepository()
                        .findByCollaborator(email)
                        .map(c -> c.name())
                        .orElse("");
            }
        } catch (final Exception ignored) {
            // not critical — just don't show the company line
        }
        return "";
    }

    private void printLogoutExit() {
        System.out.println("  ║  99.  Logout                             ║");
        System.out.println("  ║  0.  Exit                                ║");
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
                case 1:  new RegisterUserUI().show();  break;
                case 2:  new ToggleUserUI().show();    break;
                case 3:  new ListUsersUI().show();     break;
                case 99: doLogout();                   break;
                case 0:  doExit();                     break;
                default: System.out.println("  >> Invalid option. Please try again.");
            }

        } else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.BACKOFFICE_OPERATOR)) {
            switch (option) {
                case 1:  new RegisterAirTransportCompanyUI().show();   break;
                case 2:  new RegisterAirControlAreaUI().show();        break;
                case 3:  new CreateAirportUI().show();                 break;
                case 4:  new CreateEngineModelUI().show();             break;
                case 5:  new CreateAircraftModelUI().show();           break;
                case 6:  new AddEngineModelToAircraftUI().show();      break;
                case 7:  new RemoveEngineModelFromAircraftUI().show(); break;
                case 8:  new AddCollaboratorUI().show();               break;
                case 9:  new ListCollaboratorsUI().show();             break;
                case 10: new EditCollaboratorUI().show();              break;
                case 11: new DisableCollaboratorUI().show();           break;
                case 99: doLogout();                                   break;
                case 0:  doExit();                                     break;
                default: System.out.println("  >> Invalid option. Please try again.");
            }

        } else if (authz.isAuthenticatedUserAuthorizedTo(
                BaseRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR)) {
            switch (option) {
                case 1:  new AddAircraftToFleetUI().show();    break;
                case 2:  new ListCompanyFleetUI().show();      break;
                case 3:  new DecommissionAircraftUI().show();  break;
                case 4:  new CreateFlightRouteUI().show();     break;
                case 5:  new DeleteFlightRouteUI().show();     break;
                case 6:  new AddPilotUI().show();              break;
                case 7:  new ListPilotRosterUI().show();       break;
                case 8:  new RemovePilotUI().show();           break;
                case 99: doLogout();                           break;
                case 0:  doExit();                             break;
                default: System.out.println("  >> Invalid option. Please try again.");
            }

        } else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.WEATHER_PERSON)) {
            switch (option) {
                case 1:  new RegisterWeatherDataUI().show();          break;
                case 2:  new ImportBulkWeatherDataUI().show();        break;
                case 3:  new ConsultWeatherDataUI().show();           break;
                case 4:  new ExportWeatherDataUI().show();            break;
                case 99: doLogout();                                  break;
                case 0:  doExit();                                    break;
                default: System.out.println("  >> Invalid option. Please try again.");
            }

        } else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.PILOT)) {
            switch (option) {
                case 1:  new CreateFlightPlanUI().show();    break;
                case 2:  new ImportFlightPlanUI().show();    break;
                case 3:  new InsertWeatherDataUI().show();   break;
                case 4:  new TestFlightPlanUI().show();      break;
                case 99: doLogout();                         break;
                case 0:  doExit();                           break;
                default: System.out.println("  >> Invalid option or insufficient permissions.");
            }

        } else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.FLIGHT_CONTROL_OPERATOR)) {
            switch (option) {
                case 1:  new GenerateSimulationReportUI().show(); break;
                case 2:  new MonthlyReportUI().show();            break;
                case 99: doLogout();                              break;
                case 0:  doExit();                                break;
                default: System.out.println("  >> Invalid option or insufficient permissions.");
            }

        } else {
            switch (option) {
                case 99: doLogout(); break;
                case 0:  doExit();   break;
                default: System.out.println("  >> Invalid option or insufficient permissions.");
            }
        }
    }

    private void doLogin() {
        System.out.println("\n  --- Login ---");
        final String email = Console.readLine("  Email:");
        final String pass  = Console.readLine("  Password:");

        if (email == null || email.isBlank() || pass == null || pass.isBlank()) {
            System.out.println("\n  >> Email and password cannot be empty.");
            return;
        }

        final Optional<UserSession> result = loginController.login(email, pass);

        if (result.isPresent()) {
            System.out.println("\n  >> Login successful! Welcome, " + currentUserName() + ".");
            if      (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.ADMIN))
                System.out.println("     Role: ADMINISTRATOR");
            else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.BACKOFFICE_OPERATOR))
                System.out.println("     Role: BACKOFFICE OPERATOR");
            else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR))
                System.out.println("     Role: ATCC");
            else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.WEATHER_PERSON))
                System.out.println("     Role: WEATHER PERSON");
            else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.PILOT))
                System.out.println("     Role: PILOT");
            else if (authz.isAuthenticatedUserAuthorizedTo(BaseRoles.FLIGHT_CONTROL_OPERATOR))
                System.out.println("     Role: FLIGHT CONTROL OPERATOR");
        } else {
            System.out.println("\n  >> Login failed: incorrect email/password or account inactive.");
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