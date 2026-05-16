package controller;

import domain.*;
import dto.CollaboratorDTO;
import dto.CustomerDTO;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.Role;
import persistence.PersistenceContext;
import repositories.CustomerRepository;
import repositories.UserRepository;

import java.time.LocalDate;
import java.util.*;

/**
 * Application-layer controller for US061 – Add a Customer's Collaborator.
 *
 * Uses a single {@link CustomerRepository} for both Air Transport Companies
 * and Air Control Areas, matching the sequence diagram exactly:
 *
 *   CTRL -> CREPO : findAllAirTransportCompanies()
 *   CTRL -> CREPO : findAllAirControlAreas()
 *   CTRL -> CREPO : findById(customerId, customerType)
 *   CTRL -> CREPO : save(customerAggregate)
 */
public class AddCollaboratorController {

    private final AuthorizationService  authz;
    private final UserManagementService userSvc;
    private final UserRepository        userRepo;
    private final CustomerRepository    customerRepo;

    public AddCollaboratorController() {
        this.authz        = AuthzRegistry.authorizationService();
        this.userSvc      = AuthzRegistry.userService();
        this.userRepo     = PersistenceContext.getInstance().userRepository();
        this.customerRepo = PersistenceContext.getInstance().customerRepository();
    }

    /**
     * Returns all available customers (companies + areas) as DTOs.
     * Called before the operator fills in collaborator data.
     */
    public List<CustomerDTO> getAvailableCustomers() {
        authz.ensureAuthenticatedUserHasAnyOf(BaseRoles.BACKOFFICE_OPERATOR);

        final List<CustomerDTO> result = new ArrayList<>();

        customerRepo.findAllAirTransportCompanies().forEach(c -> result.add(
                new CustomerDTO(
                        c.identity().iata() + "/" + c.identity().icao(),
                        c.name(),
                        CustomerDTO.CustomerType.AIR_TRANSPORT_COMPANY)));

        customerRepo.findAllAirControlAreas().forEach(a -> result.add(
                new CustomerDTO(
                        a.identity().code(),
                        a.name(),
                        CustomerDTO.CustomerType.AIR_CONTROL_AREA)));

        return result;
    }

    /**
     * Registers a new collaborator for the given customer.
     * <p>
     * 1. Auth check.
     * 2. findByEmail() — abort if email already taken.
     * 3. Create User + save.
     * 4. findById(customerId, customerType) — load aggregate.
     * 5. aggregate.addCollaborator() — aggregate enforces its invariants.
     * 6. save(customerAggregate).
     *
     * @param customerId   "IATA/ICAO" for a company, area code for an area
     * @param customerType discriminates between company and area
     * @param email        must be unique system-wide
     * @param name         collaborator full name
     * @param phone        9-digit phone number
     * @param staffNo      unique staff number
     * @param position     job position within the customer
     * @param password     initial password for the new system user
     */
    public void addCollaborator(
            final String customerId,
            final CustomerDTO.CustomerType customerType,
            final String email,
            final String name,
            final String phone,
            final String staffNo,
            final String position,
            final String password) {

        // 1. Auth
        authz.ensureAuthenticatedUserHasAnyOf(BaseRoles.BACKOFFICE_OPERATOR);

        // 2. System-wide email uniqueness guard
        if (userRepo.findByEmail(EmailAddress.valueOf(email)).isPresent()) {
            throw new IllegalArgumentException(
                    "Email '" + email + "' is already registered. " + "Each collaborator must be a distinct system user.");
        }

        // 3. Create SystemUser + domain User
        final Role role = customerType == CustomerDTO.CustomerType.AIR_TRANSPORT_COMPANY
                ? BaseRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR
                : BaseRoles.FLIGHT_CONTROL_OPERATOR;
        createSystemAndDomainUser(email, name, phone, staffNo, password, role);

        // 4. Load the customer aggregate
        Optional<Object> customer = customerRepo.findById(customerId, customerType);

        if (customer.isEmpty()) {
            throw new IllegalArgumentException(
                    "Customer not found: " + customerId);
        }

        final Object aggregate = customer.get();

        // 5. Delegate to aggregate
        if (aggregate instanceof AirTransportCompany) {
            ((AirTransportCompany) aggregate).addCollaborator(email, name, position);
            // 6. Save updated aggregate
            customerRepo.save((AirTransportCompany) aggregate);
        } else {
            ((AirControlArea) aggregate).addCollaborator(email, name, position);
            customerRepo.save((AirControlArea) aggregate);
        }
    }

    // Private helper
    private void createSystemAndDomainUser(final String email, final String name,
                                           final String phone, final String staffNo,
                                           final String password, final Role role) {
        final String[] parts     = name.trim().split("\\s+", 2);
        final String   firstName = parts[0];
        final String   lastName  = parts.length > 1 ? parts[1] : parts[0];

        final Set<Role> roles = new HashSet<>();
        roles.add(role);
        userSvc.registerNewUser(email, password, firstName, lastName, email, roles);

        final User domainUser = new User(
                EmailAddress.valueOf(email), name, phone, staffNo,
                LocalDate.now().plusYears(1));
        domainUser.setRole(role.toString());
        userRepo.save(domainUser);
    }
}