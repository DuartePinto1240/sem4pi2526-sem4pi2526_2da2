package domain;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Registry of valid AISafe internal email domains (US031).
 *
 * <p>Only backoffice users (Admin, Backoffice Operator, Flight Control Operator)
 * are required to have an email belonging to one of these domains.
 * External users such as ATCC and pilots (US061) are exempt from this check.</p>
 *
 * <p>The list is populated by the bootstrap process on application startup.</p>
 */
public final class ValidEmailDomains {

    private static final Set<String> DOMAINS = new LinkedHashSet<>();

    private ValidEmailDomains() {
    }

    /**
     * Registers a valid email domain.
     * Called by the bootstrap process.
     *
     * @param domain domain name, e.g. {@code "alsafe.com"}
     */
    public static void register(final String domain) {
        if (domain != null && !domain.isBlank()) {
            DOMAINS.add(domain.trim().toLowerCase());
        }
    }

    /**
     * Checks whether the domain part of the given email address is valid.
     *
     * @param email the full email address to check
     * @return {@code true} if the domain is in the registered list
     */
    public static boolean isValid(final String email) {
        if (email == null || !email.contains("@")) return false;
        final String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        return DOMAINS.contains(domain);
    }

    /**
     * Returns an unmodifiable view of all registered domains.
     *
     * @return set of valid domains
     */
    public static Set<String> getAll() {
        return Collections.unmodifiableSet(DOMAINS);
    }

    /**
     * Returns {@code true} if no domains have been registered yet.
     *
     * @return {@code true} if empty
     */
    public static boolean isEmpty() {
        return DOMAINS.isEmpty();
    }
}
