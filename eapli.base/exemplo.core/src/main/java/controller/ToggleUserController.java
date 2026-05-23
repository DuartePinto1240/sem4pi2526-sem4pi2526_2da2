package controller;

import domain.BaseRoles;
import domain.EmailAddress;
import domain.User;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import persistence.PersistenceContext;
import repositories.UserRepository;

import java.util.Optional;

/**
 * Controller for US032 — Enable/Disable BackOffice Operators.
 *
 * <p>Only users with the {@code BACKOFFICE_OPERATOR} role can be toggled.
 * Both the EAPLI Framework's {@link SystemUser} (authentication layer)
 * and the domain {@link User} (domain layer) are updated to keep them in sync.
 * The framework enforces that a deactivated user cannot log in.</p>
 */
public class ToggleUserController {

    /** Framework service for managing SystemUser active status. */
    private final UserManagementService userSvc = AuthzRegistry.userService();

    /** Repository for domain-specific user data. */
    private final UserRepository repo = PersistenceContext.getInstance().userRepository();

    /**
     * Returns all BackOffice Operators (active and inactive) for display.
     *
     * @return iterable of {@link User} records with BACKOFFICE_OPERATOR role
     */
    public Iterable<User> allUsers() {
        return repo.findByRole(BaseRoles.BACKOFFICE_OPERATOR.toString());
    }

    /**
     * Deactivates the BackOffice Operator identified by the given email (US032.1).
     *
     * <p>Deactivates the {@link SystemUser} via the framework and mirrors
     * the change to the domain {@link User}.</p>
     *
     * @param email the email of the operator to deactivate
     * @return {@code true} if deactivated successfully
     * @throws IllegalArgumentException if the user is not found, is not a
     *                                  BackOffice Operator, or is already inactive
     */
    public boolean deactivateUser(final String email) {
        final User domainUser = findBackOfficeOperatorOrThrow(email);

        if (!domainUser.isActive()) {
            throw new IllegalArgumentException("Operator '" + email + "' is already inactive.");
        }

        // Deactivate SystemUser via framework
        findSystemUser(email).ifPresent(userSvc::deactivateUser);

        // Mirror status in domain User
        domainUser.deactivate();
        repo.save(domainUser);
        return true;
    }

    /**
     * Activates the BackOffice Operator identified by the given email (US032.2).
     *
     * <p>Activates the {@link SystemUser} via the framework and mirrors
     * the change to the domain {@link User}.</p>
     *
     * @param email the email of the operator to activate
     * @return {@code true} if activated successfully
     * @throws IllegalArgumentException if the user is not found, is not a
     *                                  BackOffice Operator, or is already active
     */
    public boolean activateUser(final String email) {
        final User domainUser = findBackOfficeOperatorOrThrow(email);

        if (domainUser.isActive()) {
            throw new IllegalArgumentException("Operator '" + email + "' is already active.");
        }

        // Activate SystemUser via framework
        findSystemUser(email).ifPresent(su -> userSvc.activateUser(su));

        // Mirror status in domain User
        domainUser.activate();
        repo.save(domainUser);
        return true;
    }

    /**
     * Finds the domain User by email, enforcing that it must be a
     * BackOffice Operator, or throws if not found / wrong role.
     */
    private User findBackOfficeOperatorOrThrow(final String email) {
        final Optional<User> userOpt = repo.ofIdentity(EmailAddress.valueOf(email));
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + email);
        }
        final User user = userOpt.get();
        if (!BaseRoles.BACKOFFICE_OPERATOR.toString().equals(user.getRole())) {
            throw new IllegalArgumentException(
                    "User '" + email + "' is not a BackOffice Operator.");
        }
        return user;
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
