package ui;

import controller.ListCompanyFleetController;
import domain.aircraft.Aircraft;
import domain.aircraftmodel.AircraftModel;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class ListCompanyFleetUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final ListCompanyFleetController ctrl =
                new ListCompanyFleetController();

        final String companyIata = Console.readLine("Your company IATA code:");

        System.out.println("\n-- Fleet Listing --");
        System.out.println("1. List all aircraft                    (US072)");
        System.out.println("2. List by model                        (US072a)");
        System.out.println("3. List by maker                        (US072b)");
        System.out.println("4. List by minimum passenger capacity   (US072c)");
        System.out.println("5. List by age (oldest first)           (US072d)");

        final int option = Console.readInteger("Option:");

        Iterable<Aircraft> result;

        switch (option) {

            case 1:
                result = ctrl.listFleet(companyIata);
                break;

            case 2:
                final List<AircraftModel> models = new ArrayList<>();
                ctrl.availableModels().forEach(models::add);
                if (models.isEmpty()) {
                    System.out.println(">> No models available.");
                    return false;
                }
                System.out.println("\nAvailable models:");
                for (int i = 0; i < models.size(); i++) {
                    System.out.printf("  %d. %s by %s%n",
                            i + 1,
                            models.get(i).modelName(),
                            models.get(i).manufacturer());
                }
                final int modelChoice = Console.readInteger("Select model:");
                if (modelChoice < 1 || modelChoice > models.size()) {
                    System.out.println(">> Invalid option.");
                    return false;
                }
                result = ctrl.listByModel(companyIata,
                        models.get(modelChoice - 1).identity());
                break;

            case 3:
                final List<String> makers = new ArrayList<>();
                ctrl.usedManufacturers(companyIata).forEach(makers::add);

                if (makers.isEmpty()) {
                    System.out.println(">> No manufacturers found for this company.");
                    return false;
                }

                System.out.println("\nAvailable manufacturers:");
                for (int i = 0; i < makers.size(); i++) {
                    System.out.printf("  %d. %s%n", i + 1, makers.get(i));
                }

                final int makerChoice = Console.readInteger("Select manufacturer:");
                if (makerChoice < 1 || makerChoice > makers.size()) {
                    System.out.println(">> Invalid option.");
                    return false;
                }
                result = ctrl.listByMaker(companyIata, makers.get(makerChoice - 1));
                break;

            case 4:
                final int minSeats = Console.readInteger(
                        "Minimum number of passengers:");
                result = ctrl.listByCapacity(companyIata, minSeats);
                break;

            case 5:
                result = ctrl.listByAge(companyIata);
                break;

            default:
                System.out.println(">> Invalid option.");
                return false;
        }

        printFleet(result);
        return false;
    }

    private void printFleet(final Iterable<Aircraft> fleet) {
        System.out.println();
        System.out.println("┌──────────────┬──────────────────┬──────────┬────────┬──────────────┐");
        System.out.println("│ Registration │ Country          │ Status   │ Seats  │ Registered   │");
        System.out.println("├──────────────┼──────────────────┼──────────┼────────┼──────────────┤");

        boolean empty = true;
        for (final Aircraft a : fleet) {
            empty = false;
            System.out.printf("│ %-12s │ %-16s │ %-8s │ %-6d │ %-12s │%n",
                    a.registration(),
                    a.countryOfRegistration(),
                    a.operationalStatus(),
                    a.cabinConfiguration().totalSeats(),
                    a.registrationDate());
        }

        if (empty) {
            System.out.println(
                    "│                    No aircraft found.                         │");
        }

        System.out.println("└──────────────┴──────────────────┴──────────┴────────┴──────────────┘");
    }

    @Override
    public String headline() {
        return "US072 - List Company Fleet";
    }
}