package ui;

import controller.CreateAircraftModelController;
import domain.aircraftmodel.AircraftModel;
import domain.aircraftmodel.AircraftType;
import domain.enginemodel.EngineModel;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class CreateAircraftModelUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final CreateAircraftModelController ctrl =
                new CreateAircraftModelController();

        System.out.println("\n--- Create Aircraft Model ---");

        final String modelName    = Console.readLine("Model name:");
        final String manufacturer = Console.readLine("Manufacturer:");

        final AircraftType type = selectAircraftType();
        if (type == null) return false;

        final List<EngineModel> engines = selectEngines(ctrl);
        if (engines == null) return false;

        System.out.println("\n-- Flight Characteristics --");
        final double emptyWeight     = Console.readDouble("Empty weight (kg):");
        final double mzfw            = Console.readDouble("Maximum Zero Fuel Weight - MZFW (kg):");
        final double mtow            = Console.readDouble("Maximum Take-Off Weight - MTOW (kg):");
        final double maxFuelCapacity = Console.readDouble("Maximum fuel capacity (litres):");
        final double serviceCeiling  = Console.readDouble("Service ceiling (m):");
        final double cruiseSpeed     = Console.readDouble("Cruise speed (m/s):");
        final double maximumRange    = Console.readDouble("Maximum range (km):");
        final double wingArea        = Console.readDouble("Wing area (m²):");
        final double dragCoefficient = Console.readDouble("Drag coefficient (Cd):");
        final double liftCoefficient = Console.readDouble("Lift coefficient (Cl):");

        try {
            final AircraftModel model = ctrl.createAircraftModel(
                    modelName, manufacturer, type,
                    emptyWeight, mzfw, mtow, maxFuelCapacity,
                    serviceCeiling, cruiseSpeed, maximumRange,
                    wingArea, dragCoefficient, liftCoefficient,
                    engines);
            System.out.printf("%n>> Aircraft model '%s' by '%s' registered successfully.%n",
                    model.modelName(), model.manufacturer());
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }

    private AircraftType selectAircraftType() {
        System.out.println("\nAircraft type:");
        final AircraftType[] types = AircraftType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, types[i]);
        }

        final int choice = Console.readInteger("Choice:");
        if (choice < 1 || choice > types.length) {
            System.out.println(">> Invalid option. Operation cancelled.");
            return null;
        }
        return types[choice - 1];
    }

    private List<EngineModel> selectEngines(final CreateAircraftModelController ctrl) {
        final List<EngineModel> available = new ArrayList<>();
        ctrl.availableEngineModels().forEach(available::add);

        if (available.isEmpty()) {
            System.out.println("\n>> ERROR: No engine models registered in the system.");
            System.out.println("         Please register at least one engine model first (US056).");
            return null;
        }

        System.out.println("\nAvailable engine models:");
        for (int i = 0; i < available.size(); i++) {
            final EngineModel e = available.get(i);
            System.out.printf("  %d. %s by %s (%s)%n",
                    i + 1, e.name(), e.manufacturer(), e.motorizationType());
        }

        final List<EngineModel> selected = new ArrayList<>();
        System.out.println("\nSelect engine models to certify (enter 0 to finish):");
        System.out.println("Note: all engines must share the same motorization type.");

        while (true) {
            final int choice = Console.readInteger("Add engine (number) or 0 to finish:");
            if (choice == 0) break;
            if (choice < 1 || choice > available.size()) {
                System.out.println("  >> Invalid option, try again.");
                continue;
            }
            final EngineModel chosen = available.get(choice - 1);
            if (selected.contains(chosen)) {
                System.out.println("  >> Already selected, choose a different one.");
                continue;
            }
            selected.add(chosen);
            System.out.printf("  >> Added: %s%n", chosen.name());
        }

        if (selected.isEmpty()) {
            System.out.println(">> At least one engine model must be selected. Operation cancelled.");
            return null;
        }

        return selected;
    }

    @Override
    public String headline() {
        return "US055 - Create Aircraft Model";
    }
}