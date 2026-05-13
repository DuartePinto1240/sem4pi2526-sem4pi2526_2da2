package ui;

import controller.AddAircraftToFleetController;
import domain.Country;
import domain.aircraft.Aircraft;
import domain.aircraftmodel.AircraftModel;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class AddAircraftToFleetUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final AddAircraftToFleetController ctrl =
                new AddAircraftToFleetController();

        System.out.println("\n--- Add Aircraft to Fleet ---");

        // 1. select aircraft model
        final List<AircraftModel> models = new ArrayList<>();
        ctrl.availableAircraftModels().forEach(models::add);

        if (models.isEmpty()) {
            System.out.println("\n>> ERROR: No aircraft models registered.");
            System.out.println("         Please register an aircraft model first (US055).");
            return false;
        }

        System.out.println("\nAvailable aircraft models:");
        for (int i = 0; i < models.size(); i++) {
            final AircraftModel m = models.get(i);
            System.out.printf("  %d. %s by %s (%s) — MTOW: %.0f kg%n",
                    i + 1,
                    m.modelName(),
                    m.manufacturer(),
                    m.type(),
                    m.characteristics().mtow());
        }

        final int modelChoice = Console.readInteger("Select model (number):");
        if (modelChoice < 1 || modelChoice > models.size()) {
            System.out.println(">> Invalid option. Operation cancelled.");
            return false;
        }
        final AircraftModel selectedModel = models.get(modelChoice - 1);

        // 2. registration and country
        final String registration = Console.readLine("Aircraft registration (e.g. CS-TUA):");
        // DEPOIS
        final String country = selectCountry(ctrl);
        if (country == null) return false;

        // 3. cabin configuration
        System.out.println("\n-- Cabin Configuration --");
        System.out.printf("Model MTOW: %.0f kg — use this as a reference for capacity.%n",
                selectedModel.characteristics().mtow());

        final int economySeats    = Console.readInteger("Economy seats:");
        final int businessSeats   = Console.readInteger("Business seats:");
        final int firstClassSeats = Console.readInteger("First class seats:");
        final int crewSize        = Console.readInteger("Crew size:");

        // 4. company IATA (from session — hardcoded for now)
        // TODO: replace with authenticated user's company IATA when session is available
        final String companyIata = Console.readLine("Company IATA code:");

        try {
            final Aircraft aircraft = ctrl.addAircraft(
                    registration,
                    country,
                    companyIata,
                    selectedModel.identity(),
                    economySeats,
                    businessSeats,
                    firstClassSeats,
                    crewSize);

            System.out.printf("%n>> Aircraft '%s' added to fleet successfully.%n",
                    aircraft.registration());
            System.out.printf("   Model  : %s by %s%n",
                    selectedModel.modelName(), selectedModel.manufacturer());
            System.out.printf("   Status : %s%n", aircraft.operationalStatus());
            System.out.printf("   Cabin  : %s%n", aircraft.cabinConfiguration());

        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }
    private String selectCountry(final AddAircraftToFleetController ctrl) {
        final List<Country> countries = new ArrayList<>();
        ctrl.availableCountries().forEach(countries::add);

        if (countries.isEmpty()) {
            System.out.println("\n>> ERROR: No countries available.");
            return null;
        }

        System.out.println("\nCountry of registration:");
        for (int i = 0; i < countries.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, countries.get(i).name());
        }

        final int choice = Console.readInteger("Select country:");
        if (choice < 1 || choice > countries.size()) {
            System.out.println(">> Invalid option.");
            return null;
        }
        return countries.get(choice - 1).name();
    }

    @Override
    public String headline() {
        return "US070 - Add Aircraft to Fleet";
    }
}