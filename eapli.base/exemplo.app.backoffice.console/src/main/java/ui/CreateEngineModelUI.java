package ui;

import controller.CreateEngineModelController;
import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class CreateEngineModelUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final CreateEngineModelController ctrl =
                new CreateEngineModelController();

        System.out.println("\n--- Create Engine Model ---");

        final String name         = Console.readLine("Engine model name:");
        final String manufacturer = Console.readLine("Manufacturer:");

        final MotorizationType type = selectMotorizationType();
        if (type == null) return false;

        final double thrustStopped = Console.readDouble("Sea level stopped thrust (kN):");
        final double thrustCruise  = Console.readDouble("Sea level cruise thrust (kN):");
        final double tsfc          = Console.readDouble("TSFC (N/N/s):");

        try {
            final EngineModel model = ctrl.createEngineModel(
                    name, manufacturer, type, thrustStopped, thrustCruise, tsfc);
            System.out.printf("%n>> Engine model '%s' by '%s' registered successfully.%n",
                    model.name(), model.manufacturer());
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }

    private MotorizationType selectMotorizationType() {
        System.out.println("\nMotorization type:");
        final MotorizationType[] types = MotorizationType.values();
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

    @Override
    public String headline() {
        return "US056 - Create Engine Model";
    }
}