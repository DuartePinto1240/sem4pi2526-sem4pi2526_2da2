package controller;

import domain.AirControlArea;
import domain.AirTransportCompany;
import domain.BaseRoles;
import domain.EmailAddress;
import dto.CollaboratorDTO;
import dto.CustomerDTO;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import persistence.PersistenceContext;
import repositories.CustomerRepository;
import repositories.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Application-layer controller for US064 – Disable a Customer's Collaborator.
 *
 * Soft delete: sets active=false on both the Collaborator entity (step 3)
 * and the associated User aggregate (step 4). Both operations are idempotent.
 *
 * Matches the sequence diagram exactly:
 *   CTRL -> CREPO : findById(customerId, customerType)
 *   CTRL -> CUSTOMER : disableCollaborator(email) → COLLAB.disable()
 *   CTRL -> CREPO : save(customerAggregate)
 *   CTRL -> UREPO : findByEmail(email) → USER.disable()
 *   CTRL -> UREPO : save(user)
 */
public class DisableCollaboratorController {

    private final AuthorizationService authz;
    private final UserRepository       userRepo;
    private final CustomerRepository   customerRepo;

    public DisableCollaboratorController() {
        this.authz        = AuthzRegistry.authorizationService();
        this.userRepo     = PersistenceContext.getInstance().userRepository();
        this.customerRepo = PersistenceContext.getInstance().customerRepository();
    }

    /** Package-private test constructor — bypasses PersistenceContext. */
    DisableCollaboratorController(final CustomerRepository customerRepo,
                                  final UserRepository userRepo) {
        this.authz        = AuthzRegistry.authorizationService();
        this.userRepo     = userRepo;
        this.customerRepo = customerRepo;
    }

    /** Returns all customers for the selection list. */
    public List<CustomerDTO> getAvailableCustomers() {
        authz.ensureAuthenticatedUserHasAnyOf(BaseRoles.BACKOFFICE_OPERATOR);

        final List<CustomerDTO> result = new ArrayList<>();
        customerRepo.findAllAirTransportCompanies().forEach(c -> result.add(
                new CustomerDTO(c.identity().iata() + "/" + c.identity().icao(),
                        c.name(), CustomerDTO.CustomerType.AIR_TRANSPORT_COMPANY)));
        customerRepo.findAllAirControlAreas().forEach(a -> result.add(
                new CustomerDTO(a.identity().code(),
                        a.name(), CustomerDTO.CustomerType.AIR_CONTROL_AREA)));
        return result;
    }

    /** Returns active collaborators of the selected customer. */
    public List<CollaboratorDTO> listCollaborators(final String customerId,
                                                   final CustomerDTO.CustomerType customerType) {
        authz.ensureAuthenticatedUserHasAnyOf(BaseRoles.BACKOFFICE_OPERATOR);

        final Object aggregate = customerRepo.findById(customerId, customerType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found: " + customerId));

        List<CollaboratorDTO> collaborators;

        if (aggregate instanceof AirTransportCompany) {
            collaborators = ((AirTransportCompany) aggregate).listActiveCollaborators();
        } else {
            collaborators = ((AirControlArea) aggregate).listActiveCollaborators();
        }

        return collaborators;
    }

    /**
     * Disables a collaborator (US064).
     *
     * Step 3: disables the Collaborator entity inside the customer aggregate.
     * Step 4: disables the associated User aggregate.
     * Both operations are idempotent (no exception if already disabled).
     *
     * @param customerId   identifies the customer aggregate
     * @param customerType AIR_TRANSPORT_COMPANY or AIR_CONTROL_AREA
     * @param email        email of the collaborator to disable
     */
    public void disableCollaborator(final String customerId,
                                    final CustomerDTO.CustomerType customerType,
                                    final String email) {

        authz.ensureAuthenticatedUserHasAnyOf(BaseRoles.BACKOFFICE_OPERATOR);

        // Step 3: disable inside the aggregate
        final Object aggregate = customerRepo.findById(customerId, customerType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found: " + customerId));

        if (aggregate instanceof AirTransportCompany) {
            ((AirTransportCompany) aggregate).disableCollaborator(email);
            customerRepo.save((AirTransportCompany) aggregate);
        } else {
            ((AirControlArea) aggregate).disableCollaborator(email);
            customerRepo.save((AirControlArea) aggregate);
        }

        // Step 4: propagate to the User aggregate (separate aggregate root)
        // ifPresent() makes this idempotent if the user was already disabled
        userRepo.findByEmail(EmailAddress.valueOf(email)).ifPresent(user -> {
            user.deactivate();
            userRepo.save(user);
        });
    }
}