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
 * Application-layer controller for US062 – List Customer's Collaborators.
 *
 * Read-only: no save() is called anywhere.
 * Uses a single {@link CustomerRepository}, matching the sequence diagram.
 */
public class ListCollaboratorsController {

    private final AuthorizationService authz;
    private final CustomerRepository   customerRepo;
    private final UserRepository       userRepo;

    public ListCollaboratorsController() {
        this.authz        = AuthzRegistry.authorizationService();
        this.customerRepo = PersistenceContext.getInstance().customerRepository();
        this.userRepo     = PersistenceContext.getInstance().userRepository();
    }

    /** Package-private test constructor — bypasses PersistenceContext. */
    ListCollaboratorsController(final CustomerRepository customerRepo) {
        this.authz        = AuthzRegistry.authorizationService();
        this.customerRepo = customerRepo;
        this.userRepo     = null;
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

    /**
     * Returns active collaborators of the selected customer (US062).
     * Enriches each DTO with the phone number from the User aggregate.
     */
    public List<CollaboratorDTO> listCollaborators(final String customerId,
                                                   final CustomerDTO.CustomerType customerType) {
        authz.ensureAuthenticatedUserHasAnyOf(BaseRoles.BACKOFFICE_OPERATOR);

        final Object aggregate = customerRepo.findById(customerId, customerType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found: " + customerId));

        final List<CollaboratorDTO> dtos;
        if (aggregate instanceof AirTransportCompany) {
            dtos = ((AirTransportCompany) aggregate).listActiveCollaborators();
        } else {
            dtos = ((AirControlArea) aggregate).listActiveCollaborators();
        }

        // Enrich with phone number from UserRepository if available
        if (userRepo == null) return dtos;
        return dtos.stream().map(dto -> {
            final String phone = userRepo.findByEmail(EmailAddress.valueOf(dto.email))
                    .map(u -> u.getPhoneNumber())
                    .orElse("N/A");
            return new CollaboratorDTO(dto.email, dto.name, dto.position, dto.active, phone);
        }).collect(Collectors.toList());
    }
}