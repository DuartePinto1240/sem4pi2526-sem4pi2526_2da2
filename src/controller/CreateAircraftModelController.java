package controller;

import domain.aircraftmodel.AircraftModel;
import domain.aircraftmodel.AircraftType;
import domain.aircraftmodel.FlightCharacteristics;
import domain.enginemodel.EngineModel;
import persistence.PersistenceContext;
import repositories.AircraftModelRepository;
import repositories.EngineModelRepository;

import java.util.List;

public class CreateAircraftModelController {

    private final AircraftModelRepository aircraftModelRepo;
    private final EngineModelRepository   engineModelRepo;

    public CreateAircraftModelController() {
        this.aircraftModelRepo = PersistenceContext.getInstance().aircraftModelRepository();
        this.engineModelRepo   = PersistenceContext.getInstance().engineModelRepository();
    }

    // Package-private constructor for unit testing
    CreateAircraftModelController(final AircraftModelRepository aircraftModelRepo,
                                  final EngineModelRepository engineModelRepo) {
        this.aircraftModelRepo = aircraftModelRepo;
        this.engineModelRepo   = engineModelRepo;
    }

    public Iterable<EngineModel> availableEngineModels() {
        return engineModelRepo.findAll();
    }

    public AircraftModel createAircraftModel(final String modelName,
                                             final String manufacturer,
                                             final AircraftType type,
                                             final double emptyWeight,
                                             final double mzfw,
                                             final double mtow,
                                             final double maxFuelCapacity,
                                             final double serviceCeiling,
                                             final double cruiseSpeed,
                                             final double maximumRange,
                                             final double wingArea,
                                             final double dragCoefficient,
                                             final double liftCoefficient,
                                             final List<EngineModel> engines) {
        if (aircraftModelRepo.existsByNameAndManufacturer(modelName, manufacturer)) {
            throw new IllegalArgumentException(
                    "An aircraft model '" + modelName
                            + "' by '" + manufacturer + "' already exists.");
        }

        final FlightCharacteristics characteristics = new FlightCharacteristics(
                emptyWeight, mzfw, mtow, maxFuelCapacity,
                serviceCeiling, cruiseSpeed, maximumRange,
                wingArea, dragCoefficient, liftCoefficient);

        final AircraftModel model = new AircraftModel(
                modelName, manufacturer, type, characteristics, engines);

        return aircraftModelRepo.save(model);
    }
}