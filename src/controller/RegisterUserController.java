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

/**
 * Controller for US031 — Register backoffice users.
 *
 * <p>Creating a user involves two steps:</p>
 * <ol>
 *   <li>Creating a {@link eapli.framework.infrastructure.authz.domain.model.SystemUser}
 *       via the EAPLI Framework's {@link UserManagementService} — handles auth and roles.</li>
 *   <li>Creating a domain {@link User} in our repository — stores domain-specific data.</li>
 * </ol>
 *
 * <p>The email address is used as the SystemUser username.
 * The framework requires both firstName and lastName to be non-empty:
 * the user's full name is split on the first space into firstName + lastName.</p>
 */
public class RegisterUserController {

    private final UserManagementService userSvc = AuthzRegistry.userService();
    private final UserRepository repo = PersistenceContext.getInstance().userRepository();

    /**
     * Registers a new backoffice user (US031).
     *
     * @param email                   unique email — also used as SystemUser username
     * @param name                    full name (split into first + last for the framework)
     * @param phoneNumber             contact phone — exactly 9 digits, unique
     * @param password                access password — at least 4 characters
     * @param staffNumber             unique staff identifier
     * @param securityClearanceExpiry clearance expiry — must be a future date
     * @return the created domain {@link User}
     * @throws IllegalArgumentException if any validation or uniqueness rule is violated
     */
    public User registerUser(final String email, final String name, final String phoneNumber,
                             final String password, final String staffNumber,
                             final LocalDate securityClearanceExpiry) {

        // 1. Email domain validation
        if (!ValidEmailDomains.isEmpty() && !ValidEmailDomains.isValid(email)) {
            throw new IllegalArgumentException(
                    "Email domain is not valid. Allowed domains: " + ValidEmailDomains.getAll());
        }

        // 2. Duplicate email check
        final EmailAddress emailVO = EmailAddress.valueOf(email);
        if (repo.ofIdentity(emailVO).isPresent()) {
            throw new IllegalArgumentException("A user with email '" + email + "' already exists.");
        }

        // 3. Create SystemUser via framework
        //    Framework requires non-empty lastName — split on first space
        final String[] parts = name.trim().split("\\s+", 2);
        final String firstName = parts[0];
        final String lastName  = parts.length > 1 ? parts[1] : parts[0];

        final Set<Role> roles = new HashSet<>();
        roles.add(BaseRoles.BACKOFFICE_OPERATOR);
        userSvc.registerNewUser(email, password, firstName, lastName, email, roles);

        // 4. Create domain User
        final User domainUser = new User(emailVO, name, phoneNumber, staffNumber, securityClearanceExpiry);
        repo.save(domainUser);
        return domainUser;
    }

    /**
     * Registers the default administrator account.
     * Used exclusively by the bootstrap process. Idempotent.
     *
     * @param email                   admin email
     * @param name                    admin full name
     * @param phoneNumber             admin phone — exactly 9 digits
     * @param password                admin password
     * @param staffNumber             admin staff number
     * @param securityClearanceExpiry admin clearance expiry
     * @return the admin domain {@link User}
     */
    public User registerAdmin(final String email, final String name, final String phoneNumber,
                              final String password, final String staffNumber,
                              final LocalDate securityClearanceExpiry) {

        final EmailAddress emailVO = EmailAddress.valueOf(email);

        if (repo.ofIdentity(emailVO).isPresent()) {
            return repo.ofIdentity(emailVO).get();
        }

        // Framework requires non-empty lastName
        final String[] parts = name.trim().split("\\s+", 2);
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