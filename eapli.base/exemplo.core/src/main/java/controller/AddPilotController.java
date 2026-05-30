package controller;

import domain.AirTransportCompany;
import domain.aircraftmodel.AircraftModel;
import domain.pilot.Pilot;
import persistence.PersistenceContext;
import repositories.AircraftModelRepository;
import repositories.AirTransportCompanyRepository;
import repositories.PilotRepository;
import util.Either;

import java.util.List;

public class AddPilotController {

    private final PilotRepository               pilotRepository;
    private final AirTransportCompanyRepository companyRepository;
    private final AircraftModelRepository       aircraftModelRepository;

    public AddPilotController() {
        this.pilotRepository         = PersistenceContext.getInstance().pilotRepository();
        this.companyRepository       = PersistenceContext.getInstance().airTransportCompanyRepository();
        this.aircraftModelRepository = PersistenceContext.getInstance().aircraftModelRepository();
    }

    // Package-private constructor for unit testing
    AddPilotController(final PilotRepository pilotRepository,
                       final AirTransportCompanyRepository companyRepository,
                       final AircraftModelRepository aircraftModelRepository) {
        this.pilotRepository         = pilotRepository;
        this.companyRepository       = companyRepository;
        this.aircraftModelRepository = aircraftModelRepository;
    }

    /**
     * Returns all registered air transport companies for the ATCC to select from.
     */
    public Iterable<AirTransportCompany> availableCompanies() {
        return companyRepository.findAll();
    }

    /**
     * Returns all available aircraft models for the ATCC to select certifications.
     */
    public Iterable<AircraftModel> availableAircraftModels() {
        return aircraftModelRepository.findAll();
    }

    /**
     * Adds a new pilot to the given company.
     *
     * Following professor feedback, business rule violations are returned
     * as a left value instead of throwing exceptions.
     *
     * @param name            pilot's full name
     * @param email           pilot's email — must be unique in the system
     * @param phoneNumber     pilot's phone number
     * @param company         the company to add the pilot to
     * @param certifiedModels list of aircraft models the pilot is certified for
     * @return the saved Pilot on success, or an error message on failure
     */
    public Either<String, Pilot> addPilot(final String name,
                                          final String email,
                                          final String phoneNumber,
                                          final AirTransportCompany company,
                                          final List<AircraftModel> certifiedModels) {

        if (pilotRepository.findByEmail(email).isPresent()) {
            return Either.left("A pilot with email '" + email + "' already exists.");
        }

        if (certifiedModels == null || certifiedModels.isEmpty()) {
            return Either.left("At least one aircraft model must be selected.");
        }

        final Pilot pilot = new Pilot(name, email, phoneNumber, company, certifiedModels);
        pilotRepository.save(pilot);
        return Either.right(pilot);
    }
}