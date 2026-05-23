package controller;

import domain.BaseRoles;
import domain.EmailAddress;
import domain.User;
import domain.ValidEmailDomains;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.Role;
import persistence.PersistenceContext;
import repositories.UserRepository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class RegisterUserController {

    private final UserManagementService userSvc = AuthzRegistry.userService();
    private final UserRepository repo = PersistenceContext.getInstance().userRepository();

    /**
     * Registers a new BackOffice Operator (US031).
     *
     * <p>The Admin can only create BackOffice Operators.
     * Other roles (Collaborator, Customer, etc.) are created by the BackOffice
     * Operator itself when registering the corresponding entities.</p>
     *
     * <p><strong>Order of operations:</strong>
     * <ol>
     *   <li>Validate domain data — build and validate the {@link User} object first.
     *       If validation fails an {@link IllegalArgumentException} is thrown <em>before</em>
     *       any SystemUser is created, so no orphan record is left behind.</li>
     *   <li>Only after a valid User exists, register the SystemUser in the framework
     *       (credentials + role).</li>
     *   <li>Persist the domain User.</li>
     * </ol>
     * </p>
     */
    public User registerBackOfficeOperator(final String email, final String name,
                                           final String phoneNumber, final String password,
                                           final String staffNumber,
                                           final LocalDate securityClearanceExpiry) {

        if (!ValidEmailDomains.isEmpty() && !ValidEmailDomains.isValid(email)) {
            throw new IllegalArgumentException(
                    "Email domain is not valid. Allowed domains: " + ValidEmailDomains.getAll());
        }

        final EmailAddress emailVO = EmailAddress.valueOf(email);
        if (repo.ofIdentity(emailVO).isPresent()) {
            throw new IllegalArgumentException(
                    "A user with email '" + email + "' already exists.");
        }

        // Step 1 — create and VALIDATE the domain User first.
        // If any field is invalid, IllegalArgumentException is thrown here and NO
        // SystemUser is created, so we can never end up with an orphan SystemUser.
        final User domainUser = new User(emailVO, name, phoneNumber,
                staffNumber, securityClearanceExpiry);
        // Role is already set to BACKOFFICE_OPERATOR by the User constructor.

        // Step 2 — domain User is valid; now register credentials in the framework.
        final String[] parts   = name.trim().split("\\s+", 2);
        final String firstName = parts[0];
        final String lastName  = parts.length > 1 ? parts[1] : parts[0];

        final Set<Role> roles = new HashSet<>();
        roles.add(BaseRoles.BACKOFFICE_OPERATOR);
        userSvc.registerNewUser(email, password, firstName, lastName, email, roles);

        // Step 3 — persist the domain User.
        repo.save(domainUser);
        return domainUser;
    }

    /**
     * Registers the default admin account. Used by bootstrap only. Idempotent.
     *
     * <p>Same ordering rule: validate and build the domain User before creating
     * the SystemUser in the framework.</p>
     */
    public User registerAdmin(final String email, final String name, final String phoneNumber,
                              final String password, final String staffNumber,
                              final LocalDate securityClearanceExpiry) {

        final EmailAddress emailVO = EmailAddress.valueOf(email);
        if (repo.ofIdentity(emailVO).isPresent()) {
            return repo.ofIdentity(emailVO).get();
        }

        // Step 1 — validate domain User first (throws IllegalArgumentException on failure).
        final User admin = new User(emailVO, name, phoneNumber, staffNumber, securityClearanceExpiry);
        admin.markAsAdmin();

        // Step 2 — domain User is valid; register credentials in the framework.
        final String[] parts   = name.trim().split("\\s+", 2);
        final String firstName = parts[0];
        final String lastName  = parts.length > 1 ? parts[1] : parts[0];

        final Set<Role> roles = new HashSet<>();
        roles.add(BaseRoles.ADMIN);
        userSvc.registerNewUser(email, password, firstName, lastName, email, roles);

        // Step 3 — persist.
        repo.save(admin);
        return admin;
    }
}