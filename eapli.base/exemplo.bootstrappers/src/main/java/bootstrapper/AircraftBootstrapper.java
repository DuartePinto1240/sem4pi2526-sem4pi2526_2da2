package bootstrapper;

import controller.AddAircraftToFleetController;
import domain.aircraftmodel.AircraftModel;
import eapli.framework.actions.Action;
import persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.List;

public class AircraftBootstrapper implements Action {

    @Override
    public boolean execute() {
        final AddAircraftToFleetController ctrl =
                new AddAircraftToFleetController();

        final List<AircraftModel> models = new ArrayList<>();
        ctrl.availableAircraftModels().forEach(models::add);

        if (models.isEmpty()) {
            System.out.println(
                    ">> AircraftBootstrapper: no aircraft models available, skipping.");
            return true;
        }

        final AircraftModel first = models.get(0);

        register(ctrl, "CS-TUA", "Portugal", "TP", first.identity(),
                150, 20, 8, 6);
        register(ctrl, "CS-TUB", "Portugal", "TP", first.identity(),
                150, 20, 8, 6);
        register(ctrl, "EI-ABC", "Ireland",  "FR", first.identity(),
                180, 0,  0, 4);

        return true;
    }

    private void register(final AddAircraftToFleetController ctrl,
                          final String registration,
                          final String country,
                          final String companyIata,
                          final long modelId,
                          final int economy,
                          final int business,
                          final int firstClass,
                          final int crew) {
        try {
            ctrl.addAircraft(registration, country, companyIata,
                    modelId, economy, business, firstClass, crew);
            System.out.printf("Bootstrapped aircraft: %s%n", registration);
        } catch (final Exception e) {
            System.out.printf("Skipped '%s': %s%n", registration, e.getMessage());
        }
    }
}