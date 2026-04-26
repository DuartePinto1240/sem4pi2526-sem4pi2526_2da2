package domain;

import eapli.framework.infrastructure.authz.domain.model.PasswordPolicy;

/**
 * AlSafe password policy (US030).
 *
 * <p>Requires passwords to have at least 4 characters.
 * In production, a stricter policy should be enforced.</p>
 */
public class AlSafePasswordPolicy implements PasswordPolicy {

    private static final int MINIMUM_LENGTH = 4;

    /**
     * {@inheritDoc}
     *
     * @param rawPassword the plain-text password provided by the user
     * @return {@code true} if the password has at least {@value #MINIMUM_LENGTH} characters
     */
    @Override
    public boolean isSatisfiedBy(final String rawPassword) {
        return rawPassword != null && rawPassword.length() >= MINIMUM_LENGTH;
    }
}
