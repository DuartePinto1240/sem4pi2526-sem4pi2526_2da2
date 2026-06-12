package ui;

import controller.AddPilotController;
import domain.AirTransportCompany;
import domain.aircraftmodel.AircraftModel;
import domain.pilot.Pilot;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import util.Either;

import java.util.ArrayList;
import java.util.List;

public class AddPilotUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final AddPilotController ctrl = new AddPilotController();

        System.out.println("\n--- Add Pilot ---");

        final String collaboratorEmail = currentUserEmail();
        if (collaboratorEmail == null) {
            System.out.println(">> No authenticated user.");
            return false;
        }

        final AirTransportCompany company = ctrl.companyOfCollaborator(collaboratorEmail)
                .orElse(null);
        if (company == null) {
            System.out.println(">> ERROR: Your account isn't linked to any registered company.");
            return false;
        }

        final String name        = Console.readLine("Full name:");
        final String email       = Console.readLine("Email:");
        final String phoneNumber = Console.readLine("Phone number:");
        final String staffNumber = Console.readLine("Staff number:");
        final String password    = Console.readLine("Password:");

        final List<AircraftModel> certifiedModels = selectCertifiedModels(ctrl);
        if (certifiedModels == null) return false;

        final Either<String, Pilot> result =
                ctrl.addPilot(name, email, phoneNumber, staffNumber, password,
                        company, certifiedModels);

        if (result.isLeft()) {
            System.out.println("\n>> ERROR: " + result.getLeft());
        } else {
            System.out.printf("%n>> Pilot '%s' (%s) registered successfully.%n",
                    result.getRight().name(), result.getRight().email());
        }

        return false;
    }

    private String currentUserEmail() {
        return AuthzRegistry.authorizationService().session()
                .map(UserSession::authenticatedUser)
                .map(u -> u.username().toString())
                .orElse(null);
    }

    private List<AircraftModel> selectCertifiedModels(final AddPilotController ctrl) {
        final List<AircraftModel> available = new ArrayList<>();
        ctrl.availableAircraftModels().forEach(available::add);

        if (available.isEmpty()) {
            System.out.println("\n>> ERROR: No aircraft models registered in the system.");
            System.out.println("         Please register at least one aircraft model first (US055).");
            return null;
        }

        System.out.println("\nAvailable aircraft models:");
        for (int i = 0; i < available.size(); i++) {
            final AircraftModel m = available.get(i);
            System.out.printf("  %d. %s by %s (%s)%n",
                    i + 1, m.modelName(), m.manufacturer(), m.type());
        }

        final List<AircraftModel> selected = new ArrayList<>();
        System.out.println("\nSelect aircraft models to certify (enter 0 to finish):");

        while (true) {
            final int choice = Console.readInteger("Add model (number) or 0 to finish:");
            if (choice == 0) break;
            if (choice < 1 || choice > available.size()) {
                System.out.println("  >> Invalid option, try again.");
                continue;
            }
            final AircraftModel chosen = available.get(choice - 1);
            if (selected.contains(chosen)) {
                System.out.println("  >> Already selected, choose a different one.");
                continue;
            }
            selected.add(chosen);
            System.out.printf("  >> Added: %s by %s%n", chosen.modelName(), chosen.manufacturer());
        }

        if (selected.isEmpty()) {
            System.out.println(">> At least one aircraft model must be selected. Operation cancelled.");
            return null;
        }

        return selected;
    }

    @Override
    public String headline() {
        return "US075 - Add Pilot";
    }
}