package ui;

import controller.RemoveEngineModelFromAircraftController;
import domain.aircraftmodel.AircraftModel;
import domain.enginemodel.EngineModel;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class RemoveEngineModelFromAircraftUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final RemoveEngineModelFromAircraftController ctrl =
                new RemoveEngineModelFromAircraftController();

        System.out.println("\n--- Remove Engine Model from Aircraft Model ---");

        // ── select aircraft model ─────────────────────────────────────────────
        final List<AircraftModel> aircraftModels = new ArrayList<>();
        ctrl.availableAircraftModels().forEach(aircraftModels::add);

        if (aircraftModels.isEmpty()) {
            System.out.println("\n>> No aircraft models registered in the system.");
            return false;
        }

        System.out.println("\nAircraft models:");
        for (int i = 0; i < aircraftModels.size(); i++) {
            final AircraftModel am = aircraftModels.get(i);
            System.out.printf("  %d. %s by %s — %d engine(s) certified%n",
                    i + 1, am.modelName(), am.manufacturer(),
                    am.certifiedEngines().size());
        }

        final int aircraftChoice = Console.readInteger("Select aircraft model:");
        if (aircraftChoice < 1 || aircraftChoice > aircraftModels.size()) {
            System.out.println(">> Invalid option. Operation cancelled.");
            return false;
        }
        final AircraftModel selectedAircraft = aircraftModels.get(aircraftChoice - 1);

        // ── show certified engines ────────────────────────────────────────────
        final List<EngineModel> engines =
                new ArrayList<>(selectedAircraft.certifiedEngines());

        if (engines.size() <= 1) {
            System.out.println(
                    "\n>> This aircraft model has only one certified engine."
                            + " At least one must be retained — removal is not possible.");
            return false;
        }

        System.out.println("\nCertified engines:");
        for (int i = 0; i < engines.size(); i++) {
            final EngineModel e = engines.get(i);
            System.out.printf("  %d. %s by %s (%s)%n",
                    i + 1, e.name(), e.manufacturer(), e.motorizationType());
        }

        final int engineChoice = Console.readInteger("Select engine model to remove:");
        if (engineChoice < 1 || engineChoice > engines.size()) {
            System.out.println(">> Invalid option. Operation cancelled.");
            return false;
        }
        final EngineModel selectedEngine = engines.get(engineChoice - 1);

        // ── execute ───────────────────────────────────────────────────────────
        try {
            final AircraftModel updated = ctrl.removeEngineModel(
                    selectedAircraft, selectedEngine);
            System.out.printf(
                    "%n>> Engine model '%s' removed successfully from '%s'.%n",
                    selectedEngine.name(), updated.modelName());
            System.out.printf("   Remaining certified engines: %d%n",
                    updated.certifiedEngines().size());
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "US058 - Remove Engine Model from Aircraft Model";
    }
}