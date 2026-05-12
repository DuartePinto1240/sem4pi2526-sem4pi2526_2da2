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
     * Registers a new user with a specific role (US031).
     * Creates both the SystemUser (framework) and the domain User.
     * The domain User's role display label is set to match the selected role.
     */
    public User registerUserWithRole(final String email, final String name,
                                     final String phoneNumber, final String password,
                                     final String staffNumber,
                                     final LocalDate securityClearanceExpiry,
                                     final Role role) {

        if (!ValidEmailDomains.isEmpty() && !ValidEmailDomains.isValid(email)) {
            throw new IllegalArgumentException(
                    "Email domain is not valid. Allowed domains: " + ValidEmailDomains.getAll());
        }

        final EmailAddress emailVO = EmailAddress.valueOf(email);
        if (repo.ofIdentity(emailVO).isPresent()) {
            throw new IllegalArgumentException(
                    "A user with email '" + email + "' already exists.");
        }

        // Split name for framework (requires non-empty firstName and lastName)
        final String[] parts    = name.trim().split("\\s+", 2);
        final String firstName  = parts[0];
        final String lastName   = parts.length > 1 ? parts[1] : parts[0];

        // Step 1 — create SystemUser in the framework (auth + role)
        final Set<Role> roles = new HashSet<>();
        roles.add(role);
        userSvc.registerNewUser(email, password, firstName, lastName, email, roles);

        // Step 2 — create domain User and mirror the selected role
        final User domainUser = new User(emailVO, name, phoneNumber,
                staffNumber, securityClearanceExpiry);
        domainUser.setRole(role.toString()); // ← mirrors the framework role
        repo.save(domainUser);
        return domainUser;
    }

    /** Returns the roles available for selection during registration. */
    public Role[] availableRoles() {
        return new Role[] {
                BaseRoles.BACKOFFICE_OPERATOR,
                BaseRoles.AIR_TRANSPORT_COMPANY_COLLABORATOR,
                BaseRoles.WEATHER_PERSON,
                BaseRoles.PILOT,
                BaseRoles.FLIGHT_CONTROL_OPERATOR
        };
    }

    /** Registers the default admin account. Used by bootstrap only. Idempotent. */
    public User registerAdmin(final String email, final String name, final String phoneNumber,
                              final String password, final String staffNumber,
                              final LocalDate securityClearanceExpiry) {

        final EmailAddress emailVO = EmailAddress.valueOf(email);
        if (repo.ofIdentity(emailVO).isPresent()) {
            return repo.ofIdentity(emailVO).get();
        }

        final String[] parts   = name.trim().split("\\s+", 2);
        final String firstName = parts[0];
        final String lastName  = parts.length > 1 ? parts[1] : parts[0];

        final Set<Role> roles = new HashSet<>();
        roles.add(BaseRoles.ADMIN);
        userSvc.registerNewUser(email, password, firstName, lastName, email, roles);

        final User admin = new User(emailVO, name, phoneNumber, staffNumber, securityClearanceExpiry);
        admin.markAsAdmin();
        repo.save(admin);
        return admin;
    }
}