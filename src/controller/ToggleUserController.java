package controller;

import domain.EmailAddress;
import domain.User;
import persistence.PersistenceContext;
import repositories.UserRepository;

import java.util.Optional;

/**
 * Controller for US032 — Enable/Disable backoffice users.
 *
 * <p>Allows an administrator to activate or deactivate user accounts.
 * The administrator account itself cannot be deactivated.</p>
 */
public class ToggleUserController {

    /** Repository used to retrieve and persist users. */
    private final UserRepository repo;

    /**
     * Creates a new controller using the shared persistence context.
     */
    public ToggleUserController() {
        this.repo = PersistenceContext.getInstance().userRepository();
    }

    /**
     * Returns all registered users (active and inactive).
     *
     * @return iterable of all users
     */
    public Iterable<User> allUsers() {
        return repo.findAll();
    }

    /**
     * Deactivates the user identified by the given email.
     *
     * @param email the email of the user to deactivate
     * @return {@code true} if deactivated successfully
     * @throws IllegalArgumentException if the user does not exist,
     *                                  is already inactive, or is the admin
     */
    public boolean deactivateUser(final String email) {
        final User user = findOrThrow(email);
        if (user.isAdmin()) {
            throw new IllegalArgumentException("The administrator account cannot be deactivated.");
        }
        if (!user.isActive()) {
            throw new IllegalArgumentException("User '" + email + "' is already inactive.");
        }
        user.deactivate();
        repo.save(user);
        return true;
    }

    /**
     * Activates the user identified by the given email.
     *
     * @param email the email of the user to activate
     * @return {@code true} if activated successfully
     * @throws IllegalArgumentException if the user does not exist or is already active
     */
    public boolean activateUser(final String email) {
        final User user = findOrThrow(email);
        if (user.isActive()) {
            throw new IllegalArgumentException("User '" + email + "' is already active.");
        }
        user.activate();
        repo.save(user);
        return true;
    }

    /**
     * Looks up a user by email or throws if not found.
     *
     * @param email the email to search for
     * @return the found {@link User}
     * @throws IllegalArgumentException if no user with that email exists
     */
    private User findOrThrow(final String email) {
        final EmailAddress emailVO = EmailAddress.valueOf(email);
        final Optional<User> userOpt = repo.ofIdentity(emailVO);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + email);
        }
        return userOpt.get();
    }
}
