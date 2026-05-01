package controller;

import domain.EmailAddress;
import domain.User;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserSession;
import persistence.PersistenceContext;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Controller for US030 — Authentication.
 *
 * <p>Delegates credential verification to the EAPLI Framework's
 * {@code AuthzRegistry.authenticationService()}. After a successful
 * framework authentication, additionally checks the domain user's
 * <strong>security clearance expiry date</strong>:</p>
 * <ul>
 *   <li>If the clearance has expired, the session is cleared and
 *       {@code Optional.empty()} is returned — the user cannot log in.</li>
 *   <li>Skills assessment expiry has <em>no effect</em> on login
 *       (per client clarification).</li>
 * </ul>
 *
 * <p>The email address is used as the SystemUser username.</p>
 */
public class LoginController {

    /**
     * Attempts to authenticate a user with the given credentials.
     *
     * <p>Returns empty if:</p>
     * <ul>
     *   <li>Credentials are invalid (framework check).</li>
     *   <li>The account is inactive (framework check).</li>
     *   <li>The security clearance has expired (domain check — US030/US031).</li>
     * </ul>
     *
     * @param email    the user's email (used as SystemUser username)
     * @param password the password to verify
     * @return an {@link Optional} containing the active {@link UserSession},
     *         or empty if authentication fails for any reason
     */
    public Optional<UserSession> login(final String email, final String password) {
        // Step 1 — framework authentication (password + active status)
        final Optional<UserSession> session =
                AuthzRegistry.authenticationService().authenticate(email, password);

        if (session.isEmpty()) {
            return Optional.empty();
        }

        // Step 2 — security clearance check (domain rule, per client clarification)
        final Optional<User> domainUser = PersistenceContext.getInstance()
                .userRepository()
                .ofIdentity(EmailAddress.valueOf(email));

        if (domainUser.isPresent()) {
            final LocalDate expiry = domainUser.get().getSecurityClearanceExpiry();
            if (expiry != null && !expiry.isAfter(LocalDate.now())) {
                // Clearance expired — clear the session and deny access
                AuthzRegistry.authorizationService().clearSession();
                return Optional.empty();
            }
        }

        return session;
    }
}
