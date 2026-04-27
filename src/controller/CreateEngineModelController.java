package controller;

import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import domain.enginemodel.Thrust;
import domain.enginemodel.TSFC;
import persistence.PersistenceContext;
import repositories.EngineModelRepository;

public class CreateEngineModelController {

    private final EngineModelRepository repo;

    public CreateEngineModelController() {
        this.repo = PersistenceContext.getInstance().engineModelRepository();
    }

    // Package-private constructor for unit testing
    CreateEngineModelController(final EngineModelRepository repo) {
        this.repo = repo;
    }

    public EngineModel createEngineModel(final String name,
                                         final String manufacturer,
                                         final MotorizationType motorizationType,
                                         final double thrustStopped,
                                         final double thrustCruise,
                                         final double tsfc) {
        if (repo.existsByNameAndManufacturer(name, manufacturer)) {
            throw new IllegalArgumentException(
                    "An engine model with name '" + name
                            + "' and manufacturer '" + manufacturer
                            + "' already exists.");
        }

        final EngineModel model = new EngineModel(
                name,
                manufacturer,
                motorizationType,
                new Thrust(thrustStopped, thrustCruise),
                new TSFC(tsfc)
        );

        return repo.save(model);
    }

    public Iterable<EngineModel> listAllEngineModels() {
        return repo.findAll();
    }
}