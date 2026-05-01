package controller;

import domain.aircraft.Aircraft;
import domain.aircraft.CabinConfiguration;
import domain.aircraftmodel.AircraftModel;
import persistence.PersistenceContext;
import repositories.AircraftModelRepository;
import repositories.AircraftRepository;

import java.util.Optional;

public class AddAircraftToFleetController {

    private final AircraftRepository      aircraftRepo;
    private final AircraftModelRepository aircraftModelRepo;

    public AddAircraftToFleetController() {
        this.aircraftRepo      = PersistenceContext.getInstance().aircraftRepository();
        this.aircraftModelRepo = PersistenceContext.getInstance().aircraftModelRepository();
    }

    // package-private for testing
    AddAircraftToFleetController(final AircraftRepository aircraftRepo,
                                 final AircraftModelRepository aircraftModelRepo) {
        this.aircraftRepo      = aircraftRepo;
        this.aircraftModelRepo = aircraftModelRepo;
    }

    public Iterable<AircraftModel> availableAircraftModels() {
        return aircraftModelRepo.findAll();
    }

    public Aircraft addAircraft(final String registration,
                                final String countryOfRegistration,
                                final String companyIata,
                                final long modelId,
                                final int economySeats,
                                final int businessSeats,
                                final int firstClassSeats,
                                final int crewSize) {

        // 1. check registration uniqueness
        if (aircraftRepo.existsByRegistration(registration)) {
            throw new IllegalArgumentException(
                    "An aircraft with registration '" + registration
                            + "' already exists.");
        }

        // 2. find the aircraft model
        final Optional<AircraftModel> modelOpt =
                aircraftModelRepo.ofIdentity(modelId);
        if (modelOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "Aircraft model with ID " + modelId + " not found.");
        }
        final AircraftModel model = modelOpt.get();

        // 3. build cabin configuration
        final CabinConfiguration cabin = new CabinConfiguration(
                economySeats, businessSeats, firstClassSeats, crewSize);

        // 4. create and save aircraft
        final Aircraft aircraft = new Aircraft(
                registration, countryOfRegistration,
                companyIata, model, cabin);

        return aircraftRepo.save(aircraft);
    }
}