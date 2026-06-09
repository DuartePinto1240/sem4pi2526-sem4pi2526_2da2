package controller;

import domain.AirTransportCompany;
import domain.BaseRoles;
import domain.EmailAddress;
import domain.User;
import domain.aircraftmodel.AircraftModel;
import domain.pilot.Pilot;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import persistence.PersistenceContext;
import repositories.AircraftModelRepository;
import repositories.AirTransportCompanyRepository;
import repositories.PilotRepository;
import repositories.UserRepository;
import util.Either;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AddPilotController {

    private final PilotRepository               pilotRepository;
    private final AirTransportCompanyRepository companyRepository;
    private final AircraftModelRepository       aircraftModelRepository;
    private final UserRepository                userRepository;
    private final UserManagementService         userSvc;

    public AddPilotController() {
        this.pilotRepository         = PersistenceContext.getInstance().pilotRepository();
        this.companyRepository       = PersistenceContext.getInstance().airTransportCompanyRepository();
        this.aircraftModelRepository = PersistenceContext.getInstance().aircraftModelRepository();
        this.userRepository          = PersistenceContext.getInstance().userRepository();
        this.userSvc                 = AuthzRegistry.userService();
    }

    // Package-private constructor for unit testing
    AddPilotController(final PilotRepository pilotRepository,
                       final AirTransportCompanyRepository companyRepository,
                       final AircraftModelRepository aircraftModelRepository,
                       final UserRepository userRepository,
                       final UserManagementService userSvc) {
        this.pilotRepository         = pilotRepository;
        this.companyRepository       = companyRepository;
        this.aircraftModelRepository = aircraftModelRepository;
        this.userRepository          = userRepository;
        this.userSvc                 = userSvc;
    }

    /**
     * Returns all registered air transport companies for selection.
     */
    public Iterable<AirTransportCompany> availableCompanies() {
        return companyRepository.findAll();
    }

    /**
     * Returns all available aircraft models for certification selection.
     */
    public Iterable<AircraftModel> availableAircraftModels() {
        return aircraftModelRepository.findAll();
    }

    /**
     * Adds a new pilot to the given company and registers them as a system user.
     *
     * Order of operations (mirrors AddCollaboratorController pattern):
     *   1. Business rule checks — fail fast before any persistence.
     *   2. Build and validate domain User — throws if data is invalid.
     *   3. Register SystemUser in the EAPLI framework (credentials + PILOT role).
     *   4. Persist domain User.
     *   5. Create and persist Pilot aggregate.
     *
     * @param name            pilot's full name
     * @param email           pilot's email — must be unique in the system
     * @param phoneNumber     pilot's phone number
     * @param staffNumber     unique staff number
     * @param password        initial login password
     * @param company         the company to add the pilot to
     * @param certifiedModels list of aircraft models the pilot is certified for
     * @return the saved Pilot on success, or an error message on failure
     */
    public Either<String, Pilot> addPilot(final String name,
                                          final String email,
                                          final String phoneNumber,
                                          final String staffNumber,
                                          final String password,
                                          final AirTransportCompany company,
                                          final List<AircraftModel> certifiedModels) {

        if (pilotRepository.findByEmail(email).isPresent()) {
            return Either.left("A pilot with email '" + email + "' already exists.");
        }

        if (userRepository.findByEmail(EmailAddress.valueOf(email)).isPresent()) {
            return Either.left("Email '" + email + "' is already registered in the system.");
        }

        if (certifiedModels == null || certifiedModels.isEmpty()) {
            return Either.left("At least one aircraft model must be selected.");
        }

        // Step 2 — build and validate domain User first
        final User domainUser = new User(
                EmailAddress.valueOf(email),
                name,
                phoneNumber,
                staffNumber,
                LocalDate.now().plusYears(1));
        domainUser.setRole(BaseRoles.PILOT.toString());

        // Step 3 — register SystemUser in framework
        final String[] parts    = name.trim().split("\\s+", 2);
        final String firstName  = parts[0];
        final String lastName   = parts.length > 1 ? parts[1] : parts[0];
        final Set<eapli.framework.infrastructure.authz.domain.model.Role> roles = new HashSet<>();
        roles.add(BaseRoles.PILOT);
        userSvc.registerNewUser(email, password, firstName, lastName, email, roles);

        // Step 4 — persist domain User
        userRepository.save(domainUser);

        // Step 5 — create and persist Pilot aggregate
        final Pilot pilot = new Pilot(name, email, phoneNumber, company, certifiedModels);
        pilotRepository.save(pilot);
        return Either.right(pilot);
    }
}