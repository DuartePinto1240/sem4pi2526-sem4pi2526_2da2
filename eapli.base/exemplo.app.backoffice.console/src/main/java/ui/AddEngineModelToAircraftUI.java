package ui;

import controller.AddEngineModelToAircraftController;
import domain.aircraftmodel.AircraftModel;
import domain.enginemodel.EngineModel;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class AddEngineModelToAircraftUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        final AddEngineModelToAircraftController ctrl =
                new AddEngineModelToAircraftController();

        System.out.println("\n--- Add Engine Model to Aircraft Model ---");

        // ── select aircraft model ─────────────────────────────────────────────
        final List<AircraftModel> aircraftModels = new ArrayList<>();
        ctrl.availableAircraftModels().forEach(aircraftModels::add);

        if (aircraftModels.isEmpty()) {
            System.out.println("\n>> ERROR: No aircraft models registered in the system.");
            System.out.println("         Please register at least one aircraft model first (US055).");
            return false;
        }

        System.out.println("\nAircraft models:");
        for (int i = 0; i < aircraftModels.size(); i++) {
            final AircraftModel am = aircraftModels.get(i);
            System.out.printf("  %d. %s by %s (%s) — %d engine(s) certified%n",
                    i + 1, am.modelName(), am.manufacturer(), am.type(),
                    am.certifiedEngines().size());
        }

        final int aircraftChoice = Console.readInteger("Select aircraft model:");
        if (aircraftChoice < 1 || aircraftChoice > aircraftModels.size()) {
            System.out.println(">> Invalid option. Operation cancelled.");
            return false;
        }
        final AircraftModel selectedAircraft = aircraftModels.get(aircraftChoice - 1);

        // ── show currently certified engines ──────────────────────────────────
        System.out.println("\nCurrently certified engines:");
        selectedAircraft.certifiedEngines().forEach(e ->
                System.out.printf("  - %s by %s (%s)%n",
                        e.name(), e.manufacturer(), e.motorizationType()));

        // ── select engine model — filter already certified ────────────────────
        final List<EngineModel> allEngines = new ArrayList<>();
        ctrl.availableEngineModels().forEach(allEngines::add);

        final List<EngineModel> eligible = new ArrayList<>();
        for (final EngineModel e : allEngines) {
            if (!selectedAircraft.certifiedEngines().contains(e)) {
                eligible.add(e);
            }
        }

        if (eligible.isEmpty()) {
            System.out.println("\n>> All available engine models are already certified for this aircraft.");
            return false;
        }

        System.out.println("\nAvailable engine models to add:");
        for (int i = 0; i < eligible.size(); i++) {
            final EngineModel e = eligible.get(i);
            System.out.printf("  %d. %s by %s (%s)%n",
                    i + 1, e.name(), e.manufacturer(), e.motorizationType());
        }

        final int engineChoice = Console.readInteger("Select engine model to add:");
        if (engineChoice < 1 || engineChoice > eligible.size()) {
            System.out.println(">> Invalid option. Operation cancelled.");
            return false;
        }
        final EngineModel selectedEngine = eligible.get(engineChoice - 1);

        // ── execute ───────────────────────────────────────────────────────────
        try {
            final AircraftModel updated = ctrl.addEngineModel(selectedAircraft, selectedEngine);
            System.out.printf("%n>> Engine model '%s' added successfully to '%s'.%n",
                    selectedEngine.name(), updated.modelName());
            System.out.printf("   Total certified engines: %d%n",
                    updated.certifiedEngines().size());
        } catch (final IllegalArgumentException e) {
            System.out.println("\n>> ERROR: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "US057 - Add Engine Model to Aircraft Model";
    }
}