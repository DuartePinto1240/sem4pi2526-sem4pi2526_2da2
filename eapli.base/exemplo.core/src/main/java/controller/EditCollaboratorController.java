package controller;

import domain.AirControlArea;
import domain.AirTransportCompany;
import domain.BaseRoles;
import domain.EmailAddress;
import domain.User;
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
 * Application-layer controller for – Edit a Customer's Collaborator.
 *
 * Only email and phone number are editable. Name and position are structurally
 * immutable — {@link domain.Collaborator} has no setters for them.
 *
 * Matches the sequence diagram:
 *   - Phone-only change: update User only, no aggregate change.
 *   - Email change: check uniqueness → update User → update collaborator
 *     entity inside aggregate → save both.
 */
public class EditCollaboratorController {

    private final AuthorizationService authz;
    private final UserRepository       userRepo;
    private final CustomerRepository   customerRepo;

    public EditCollaboratorController() {
        this.authz        = AuthzRegistry.authorizationService();
        this.userRepo     = PersistenceContext.getInstance().userRepository();
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

    /** Returns active collaborators for the selection list. */
    public List<CollaboratorDTO> listCollaborators(final String customerId,
                                                   final CustomerDTO.CustomerType customerType) {
        authz.ensureAuthenticatedUserHasAnyOf(BaseRoles.BACKOFFICE_OPERATOR);

        final Object aggregate = customerRepo.findById(customerId, customerType)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        if (aggregate instanceof AirTransportCompany) {
            return ((AirTransportCompany) aggregate).listActiveCollaborators();
        } else {
            return ((AirControlArea) aggregate).listActiveCollaborators();
        }
    }

    /**
     * Edits a collaborator's email and/or phone number.
     *
     * Matches the sequence diagram branches:
     *   newEmail != currentEmail → uniqueness check → update User email
     *                            → updateCollaboratorEmail() on aggregate
     *                            → save both
     *   phone only              → update User.phoneNumber → save User
     *
     * @param customerId   identifies the customer aggregate
     * @param customerType AIR_TRANSPORT_COMPANY or AIR_CONTROL_AREA
     * @param currentEmail current email of the collaborator to edit
     * @param newEmail     new email (may equal currentEmail if only phone changes)
     * @param newPhone     new phone number
     */
    public void editCollaborator(final String customerId,
                                 final CustomerDTO.CustomerType customerType,
                                 final String currentEmail,
                                 final String newEmail,
                                 final String newPhone) {

        authz.ensureAuthenticatedUserHasAnyOf(BaseRoles.BACKOFFICE_OPERATOR);

        final boolean emailChanged =
                !currentEmail.trim().equalsIgnoreCase(newEmail.trim());

        if (emailChanged) {
            // Guard: new email must not exist system-wide (US063 — same rule as US061)
            if (userRepo.findByEmail(EmailAddress.valueOf(newEmail)).isPresent()) {
                throw new IllegalArgumentException(
                        "Email '" + newEmail + "' is already registered.");
            }

            // Step 4a: update User aggregate email
            // Since EmailAddress is the @EmbeddedId, we delete + re-insert
            final User user = userRepo.ofIdentity(EmailAddress.valueOf(currentEmail))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User not found for email: " + currentEmail));
            userRepo.delete(user);
            final User updated = new User(
                    EmailAddress.valueOf(newEmail),
                    user.getName(),
                    newPhone,
                    user.getStaffNumber(),
                    user.getSecurityClearanceExpiry());
            updated.setRole(user.getRole());
            userRepo.save(updated);

            // Step 4b: propagate new email to collaborator entity inside aggregate
            final Object aggregate = customerRepo.findById(customerId, customerType)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Customer not found: " + customerId));
            if (aggregate instanceof AirTransportCompany) {
                ((AirTransportCompany) aggregate)
                        .updateCollaboratorEmail(currentEmail, newEmail);
                customerRepo.save((AirTransportCompany) aggregate);
            } else {
                ((AirControlArea) aggregate)
                        .updateCollaboratorEmail(currentEmail, newEmail);
                customerRepo.save((AirControlArea) aggregate);
            }

        } else {
            // Step 5: phone-only change — update User only, no aggregate needed
            final User user = userRepo.ofIdentity(EmailAddress.valueOf(currentEmail))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User not found for email: " + currentEmail));
            user.updatePhoneNumber(newPhone);
            userRepo.save(user);
        }
    }
}