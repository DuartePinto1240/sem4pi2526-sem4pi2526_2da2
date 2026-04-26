package controller;

import domain.EmailAddress;
import domain.User;
import persistence.PersistenceContext;
import repositories.UserRepository;

import java.util.Optional;

/**
 * Controller for US030 — Authentication.
 *
 * <p>Authenticates a user by verifying their email and password.
 * Login is only successful when the account exists and is active.</p>
 */
public class LoginController {

    /** Repository used to look up users. */
    private final UserRepository repo;

    /**
     * Creates a new controller using the shared persistence context.
     */
    public LoginController() {
        this.repo = PersistenceContext.getInstance().userRepository();
    }

    /**
     * Attempts to authenticate a user with the given credentials.
     *
     * @param email    the user's email address
     * @param password the password to verify
     * @return an {@link Optional} containing the authenticated {@link User},
     *         or empty if credentials are invalid or the account is inactive
     */
    public Optional<User> login(final String email, final String password) {
        try {
            final EmailAddress emailVO = EmailAddress.valueOf(email);
            final Optional<User> userOpt = repo.ofIdentity(emailVO);
            if (userOpt.isPresent()) {
                final User u = userOpt.get();
                if (u.isActive() && u.checkPassword(password)) {
                    return Optional.of(u);
                }
            }
        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
        }
        return Optional.empty();
    }
}
