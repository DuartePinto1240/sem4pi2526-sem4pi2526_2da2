package controller;

import domain.AirControlArea;
import domain.AirTransportCompany;
import domain.BaseRoles;
import domain.Collaborator;
import dto.CollaboratorDTO;
import dto.CustomerDTO;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import persistence.PersistenceContext;
import repositories.CustomerRepository;

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

    public ListCollaboratorsController() {
        this.authz        = AuthzRegistry.authorizationService();
        this.customerRepo = PersistenceContext.getInstance().customerRepository();
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
     * Matches the sequence diagram:
     *   CTRL -> CREPO : findById(customerId, customerType)
     *   CTRL -> CUSTOMER : listActiveCollaborators()
     *
     * Filtering stays in the aggregate — the controller never filters.
     */
    public List<CollaboratorDTO> listCollaborators(final String customerId,
                                                   final CustomerDTO.CustomerType customerType) {
        authz.ensureAuthenticatedUserHasAnyOf(BaseRoles.BACKOFFICE_OPERATOR);

        final Object aggregate = customerRepo.findById(customerId, customerType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found: " + customerId));

        List<Collaborator> collaborators;

        if (aggregate instanceof AirTransportCompany) {
            return ((AirTransportCompany) aggregate).listActiveCollaborators();
        } else {
            return ((AirControlArea) aggregate).listActiveCollaborators();
        }
    }
}