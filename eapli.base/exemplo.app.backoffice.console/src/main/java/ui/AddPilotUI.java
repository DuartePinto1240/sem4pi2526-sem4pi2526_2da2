package ui;

import controller.AddPilotController;
import domain.AirTransportCompany;
import domain.aircraftmodel.AircraftModel;
import domain.pilot.Pilot;
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

        final AirTransportCompany company = selectCompany(ctrl);
        if (company == null) return false;

        final String name        = Console.readLine("Full name:");
        final String email       = Console.readLine("Email:");
        final String phoneNumber = Console.readLine("Phone number:");

        final List<AircraftModel> certifiedModels = selectCertifiedModels(ctrl);
        if (certifiedModels == null) return false;

        final Either<String, Pilot> result =
                ctrl.addPilot(name, email, phoneNumber, company, certifiedModels);

        if (result.isLeft()) {
            System.out.println("\n>> ERROR: " + result.getLeft());
        } else {
            System.out.printf("%n>> Pilot '%s' (%s) registered successfully.%n",
                    result.getRight().name(), result.getRight().email());
        }

        return false;
    }

    private AirTransportCompany selectCompany(final AddPilotController ctrl) {
        final List<AirTransportCompany> companies = new ArrayList<>();
        ctrl.availableCompanies().forEach(companies::add);

        if (companies.isEmpty()) {
            System.out.println("\n>> ERROR: No air transport companies registered in the system.");
            return null;
        }

        System.out.println("\nAvailable companies:");
        for (int i = 0; i < companies.size(); i++) {
            final AirTransportCompany c = companies.get(i);
            System.out.printf("  %d. %s (%s)%n", i + 1, c.name(), c.companyCode());
        }

        final int choice = Console.readInteger("Select company:");
        if (choice < 1 || choice > companies.size()) {
            System.out.println(">> Invalid option. Operation cancelled.");
            return null;
        }

        return companies.get(choice - 1);
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