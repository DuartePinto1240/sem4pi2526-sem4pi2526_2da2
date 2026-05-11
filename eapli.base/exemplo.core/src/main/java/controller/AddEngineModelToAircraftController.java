package controller;

import domain.aircraftmodel.AircraftModel;
import domain.enginemodel.EngineModel;
import persistence.PersistenceContext;
import repositories.AircraftModelRepository;
import repositories.EngineModelRepository;

public class AddEngineModelToAircraftController {

    private final AircraftModelRepository aircraftModelRepo;
    private final EngineModelRepository   engineModelRepo;

    public AddEngineModelToAircraftController() {
        this.aircraftModelRepo = PersistenceContext.getInstance().aircraftModelRepository();
        this.engineModelRepo   = PersistenceContext.getInstance().engineModelRepository();
    }

    // Package-private constructor for unit testing
    AddEngineModelToAircraftController(final AircraftModelRepository aircraftModelRepo,
                                       final EngineModelRepository engineModelRepo) {
        this.aircraftModelRepo = aircraftModelRepo;
        this.engineModelRepo   = engineModelRepo;
    }

    public Iterable<AircraftModel> availableAircraftModels() {
        return aircraftModelRepo.findAll();
    }

    public Iterable<EngineModel> availableEngineModels() {
        return engineModelRepo.findAll();
    }

    public AircraftModel addEngineModel(final AircraftModel aircraftModel,
                                        final EngineModel engineModel) {
        // Domain enforces: type compatibility and no duplicates (US057.2, US057.3)
        aircraftModel.addCertifiedEngine(engineModel);
        return aircraftModelRepo.save(aircraftModel);
    }
}