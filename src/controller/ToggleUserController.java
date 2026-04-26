package controller;

import domain.EmailAddress;
import domain.User;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import persistence.PersistenceContext;
import repositories.UserRepository;

import java.util.Optional;

/**
 * Controller for US032 — Enable/Disable backoffice users.
 *
 * <p>Both the EAPLI Framework's {@link SystemUser} (authentication layer)
 * and the domain {@link User} (domain layer) are updated to keep them
 * in sync. The framework enforces that a deactivated user cannot log in.</p>
 *
 * <p>The administrator account cannot be deactivated.</p>
 */
public class ToggleUserController {

    /** Framework service for managing SystemUser active status. */
    private final UserManagementService userSvc = AuthzRegistry.userService();

    /** Repository for domain-specific user data. */
    private final UserRepository repo = PersistenceContext.getInstance().userRepository();

    /**
     * Returns all registered users (active and inactive) for display.
     *
     * @return iterable of all domain {@link User} records
     */
    public Iterable<User> allUsers() {
        return repo.findAll();
    }

    /**
     * Deactivates the user identified by the given email (US032.1).
     *
     * <p>Deactivates the {@link SystemUser} via the framework and mirrors
     * the change to the domain {@link User}.</p>
     *
     * @param email the email of the user to deactivate
     * @return {@code true} if deactivated successfully
     * @throws IllegalArgumentException if the user is not found, is already
     *                                  inactive, or is the admin account
     */
    public boolean deactivateUser(final String email) {
        final User domainUser = findDomainOrThrow(email);

        if ("ADMIN".equals(domainUser.getRole())) {
            throw new IllegalArgumentException("The administrator account cannot be deactivated.");
        }
        if (!domainUser.isActive()) {
            throw new IllegalArgumentException("User '" + email + "' is already inactive.");
        }

        // Deactivate SystemUser via framework
        findSystemUser(email).ifPresent(userSvc::deactivateUser);

        // Mirror status in domain User
        domainUser.deactivate();
        repo.save(domainUser);
        return true;
    }

    /**
     * Activates the user identified by the given email (US032.2).
     *
     * <p>Activates the {@link SystemUser} via the framework and mirrors
     * the change to the domain {@link User}.</p>
     *
     * @param email the email of the user to activate
     * @return {@code true} if activated successfully
     * @throws IllegalArgumentException if the user is not found or is already active
     */
    public boolean activateUser(final String email) {
        final User domainUser = findDomainOrThrow(email);

        if (domainUser.isActive()) {
            throw new IllegalArgumentException("User '" + email + "' is already active.");
        }

        // Activate SystemUser via framework
        findSystemUser(email).ifPresent(su -> userSvc.activateUser(su));

        // Mirror status in domain User
        domainUser.activate();
        repo.save(domainUser);
        return true;
    }

    /**
     * Finds the domain User by email or throws if not found.
     */
    private User findDomainOrThrow(final String email) {
        final Optional<User> userOpt = repo.ofIdentity(EmailAddress.valueOf(email));
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + email);
        }
        return userOpt.get();
    }

    /**
     * Finds the framework's SystemUser by email (used as username).
     */
    private Optional<SystemUser> findSystemUser(final String email) {
        for (SystemUser su : userSvc.allUsers()) {
            if (su.identity().toString().equals(email)) {
                return Optional.of(su);
            }
        }
        return Optional.empty();
    }
}
