package controller;

import domain.AirTransportCompany;
import domain.BaseRoles;
import domain.EmailAddress;
import domain.User;
import domain.aircraftmodel.AircraftModel;
import domain.pilot.Pilot;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.Username;
import persistence.PersistenceContext;
import repositories.AircraftModelRepository;
import repositories.AirTransportCompanyRepository;
import repositories.PilotRepository;
import repositories.UserRepository;
import util.Either;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
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
     */
    public Either<String, Pilot> addPilot(final String name,
                                          final String email,
                                          final String phoneNumber,
                                          final String staffNumber,
                                          final String password,
                                          final AirTransportCompany company,
                                          final List<AircraftModel> certifiedModels) {

        // Pilot aggregate already exists — nothing to do
        if (pilotRepository.findByEmail(email).isPresent()) {
            return Either.left("A pilot with email '" + email + "' already exists.");
        }

        if (certifiedModels == null || certifiedModels.isEmpty()) {
            return Either.left("At least one aircraft model must be selected.");
        }

        // Check for partial state: system user exists but Pilot entity doesn't (bootstrap recovery).
        // Any other combination where users already exist is treated as a conflict.
        final boolean systemUserExists =
                userSvc.userOfIdentity(Username.valueOf(email)).isPresent();
        final boolean domainUserExists =
                userRepository.findByEmail(EmailAddress.valueOf(email)).isPresent();

        if (domainUserExists && !systemUserExists) {
            // Inconsistent state: domain User registered but no SystemUser — block creation.
            return Either.left("Email '" + email + "' is already registered in the system.");
        }

        if (!systemUserExists) {
            // Normal path: neither system user nor domain user exist yet.

            // Step 2 — build and validate domain User
            final User domainUser = new User(
                    EmailAddress.valueOf(email),
                    name,
                    phoneNumber,
                    staffNumber,
                    LocalDate.now().plusYears(1));
            domainUser.setRole(BaseRoles.PILOT.toString());

            // Step 3 — register SystemUser in framework
            final String[] parts   = name.trim().split("\\s+", 2);
            final String firstName = parts[0];
            final String lastName  = parts.length > 1 ? parts[1] : parts[0];
            final Set<eapli.framework.infrastructure.authz.domain.model.Role> roles = new HashSet<>();
            roles.add(BaseRoles.PILOT);
            try {
                userSvc.registerNewUser(email, password, firstName, lastName, email, roles);
            } catch (final Exception e) {
                return Either.left("Failed to register system user: " + e.getMessage());
            }

            // Step 4 — persist domain User (guard against duplicate just in case)
            if (!domainUserExists) {
                try {
                    userRepository.save(domainUser);
                } catch (final Exception e) {
                    return Either.left("Failed to persist domain user: " + e.getMessage());
                }
            }
        }
        // If systemUserExists == true: system user already exists (bootstrap recovery) —
        // skip user/system-user creation and proceed directly to Pilot entity creation.

        // Step 5 — create and persist Pilot aggregate
        try {
            final Pilot pilot = new Pilot(name, email, phoneNumber, company, certifiedModels);
            pilotRepository.save(pilot);
            return Either.right(pilot);
        } catch (final Exception e) {
            return Either.left("Failed to persist pilot: " + e.getMessage());
        }
    }
}