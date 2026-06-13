package ui;

import controller.ImportFlightPlanController;
import domain.aircraft.Aircraft;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import lprog.ErrorCollector.ValidationError;
import lprog.FlightPlanBuilderListener.FlightData;
import lprog.FlightPlanBuilderListener.LegData;
import lprog.FlightPlanImporter.FlightPlanImportResult;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.io.File;
import java.util.List;

public class ImportFlightPlanUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final ImportFlightPlanController ctrl = new ImportFlightPlanController();

        System.out.println("\n--- Import Flight Plan from File ---");

        // ── list available files ──────────────────────────────────────────────
        final List<File> files = ctrl.availableFlightPlanFiles();

        if (files.isEmpty()) {
            System.out.println("\n>> No flight plan files found in the 'flightplans/' folder.");
            System.out.println("   Please add a .fp file to the 'flightplans/' directory and try again.");
            return false;
        }

        System.out.println("\nAvailable flight plan files:");
        for (int i = 0; i < files.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, files.get(i).getName());
        }

        // ── select file ───────────────────────────────────────────────────────
        final int choice = Console.readInteger("Select file:");
        if (choice < 1 || choice > files.size()) {
            System.out.println(">> Invalid option. Operation cancelled.");
            return false;
        }
        final File selected = files.get(choice - 1);

        // ── select aircraft ───────────────────────────────────────────────────
        final String pilotEmail = AuthzRegistry.authorizationService()
                .session()
                .map(s -> s.authenticatedUser().username().toString())
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));

        final List<Aircraft> aircraft = ctrl.availableAircraftForPilot(pilotEmail);
        String aircraftRegistration = null;

        if (aircraft.isEmpty()) {
            System.out.println("\n>> Warning: no active aircraft found for your company.");
            System.out.println("   The flight plan will be imported without an assigned aircraft.");
            System.out.println("   You must assign one before testing (US085).");
        } else {
            System.out.println("\nSelect aircraft for this flight plan:");
            for (int i = 0; i < aircraft.size(); i++) {
                final Aircraft a = aircraft.get(i);
                System.out.printf("  %d. %s%n", i + 1, a.registration());
            }
            final int ac = Console.readInteger("Aircraft:") - 1;
            if (ac < 0 || ac >= aircraft.size()) {
                System.out.println(">> Invalid selection. Operation cancelled.");
                return false;
            }
            aircraftRegistration = aircraft.get(ac).registration();
        }

        System.out.printf("%nImporting '%s'...%n", selected.getName());

        // ── import ────────────────────────────────────────────────────────────
        final FlightPlanImportResult result =
                ctrl.importFromFile(selected, aircraftRegistration, pilotEmail);

        if (result.isSuccess()) {
            printSuccess(result, ctrl, aircraftRegistration);
        } else {
            printErrors(result);
        }

        return false;
    }

    private void printSuccess(final FlightPlanImportResult result,
                              final ImportFlightPlanController ctrl,
                              final String aircraftRegistration) {
        final java.util.List<String> skipped = ctrl.lastSkippedIds();
        final java.util.List<FlightData> newFlights = result.getFlights().stream()
                .filter(f -> !skipped.contains(f.id))
                .collect(java.util.stream.Collectors.toList());

        if (!newFlights.isEmpty()) {
            System.out.println("\n>> Flight plan imported successfully.");
            for (final FlightData flight : newFlights) {
                System.out.printf("   Flight ID : %s%n", flight.id);
                System.out.printf("   Type      : %s%n", flight.type);
                System.out.printf("   Legs      : %d%n", flight.legs.size());
                for (int i = 0; i < flight.legs.size(); i++) {
                    final LegData leg = flight.legs.get(i);
                    System.out.printf("     Leg %d   : %s → %s  (fuel: %.0f %s)%n",
                            i + 1,
                            leg.departureAirport,
                            leg.arrivalAirport,
                            leg.fuelQuantity,
                            leg.fuelUnit);
                }
                System.out.println("   Status    : DRAFT");
                if (aircraftRegistration != null) {
                    System.out.printf("   Aircraft  : %s%n", aircraftRegistration);
                }
            }
        }

        if (!skipped.isEmpty()) {
            System.out.println("\n>> The following flight plan(s) were NOT imported because");
            System.out.println("   they already exist in the system (use a different flight ID");
            System.out.println("   or test/delete the existing plan first):");
            for (final String id : skipped) {
                System.out.printf("     - %s%n", id);
            }
        }
    }

    private void printErrors(final FlightPlanImportResult result) {
        System.out.println("\n>> Flight plan file is invalid. The following errors were found:\n");
        for (final ValidationError error : result.getErrors()) {
            System.out.println("   " + error);
        }
        System.out.println("\n>> Flight plan was NOT created.");
    }

    @Override
    public String headline() {
        return "US081 - Import Flight Plan from File";
    }
}