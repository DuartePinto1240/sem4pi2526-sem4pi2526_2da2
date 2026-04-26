package controller;

import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserSession;

import java.util.Optional;

/**
 * Controller for US030 — Authentication.
 *
 * <p>Delegates authentication to the EAPLI Framework's
 * {@code AuthzRegistry.authenticationService()}. On success the framework
 * creates an active session accessible via
 * {@code AuthzRegistry.authorizationService().session()}.</p>
 *
 * <p>The email address is used as the SystemUser username.</p>
 */
public class LoginController {

    /**
     * Attempts to authenticate a user with the given credentials.
     *
     * @param email    the user's email (used as SystemUser username)
     * @param password the password to verify
     * @return an {@link Optional} containing the active {@link UserSession},
     *         or empty if credentials are invalid or the account is inactive
     */
    public Optional<UserSession> login(final String email, final String password) {
        return AuthzRegistry.authenticationService().authenticate(email, password);
    }
}
