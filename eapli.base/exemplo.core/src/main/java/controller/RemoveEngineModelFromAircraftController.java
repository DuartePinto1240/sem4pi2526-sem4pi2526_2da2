package controller;

import domain.aircraftmodel.AircraftModel;
import domain.enginemodel.EngineModel;
import persistence.PersistenceContext;
import repositories.AircraftModelRepository;
import repositories.AircraftRepository;

public class RemoveEngineModelFromAircraftController {

    private final AircraftModelRepository aircraftModelRepo;
    private final AircraftRepository      aircraftRepo;

    public RemoveEngineModelFromAircraftController() {
        this.aircraftModelRepo = PersistenceContext.getInstance().aircraftModelRepository();
        this.aircraftRepo      = PersistenceContext.getInstance().aircraftRepository();
    }

    // Package-private constructor for unit testing
    RemoveEngineModelFromAircraftController(
            final AircraftModelRepository aircraftModelRepo,
            final AircraftRepository aircraftRepo) {
        this.aircraftModelRepo = aircraftModelRepo;
        this.aircraftRepo      = aircraftRepo;
    }

    public Iterable<AircraftModel> availableAircraftModels() {
        return aircraftModelRepo.findAll();
    }

    public AircraftModel removeEngineModel(final AircraftModel aircraftModel,
                                           final EngineModel engineModel) {
        // US058: not possible if there are actual aircrafts using that model (US058.1)
        if (hasActiveAircraftUsingModel(aircraftModel)) {
            throw new IllegalArgumentException(
                    "Cannot remove engine model: there are aircraft registered"
                            + " using the model '" + aircraftModel.modelName() + "'.");
        }

        // US058: domain enforces — engine must be certified and at least one remains
        aircraftModel.removeCertifiedEngine(engineModel);

        return aircraftModelRepo.save(aircraftModel);
    }

    private boolean hasActiveAircraftUsingModel(final AircraftModel aircraftModel) {
        for (final domain.aircraft.Aircraft a : aircraftRepo.findAll()) {
            if (aircraftModel.identity() != null
                    && aircraftModel.identity().equals(a.aircraftModelId())) {
                return true;
            }
        }
        return false;
    }
}