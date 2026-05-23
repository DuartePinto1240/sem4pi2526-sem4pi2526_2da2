package persistence.impl.jpa;

import domain.EmailAddress;
import domain.User;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import repositories.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JPA implementation of {@link UserRepository} backed by an H2 file-based database.
 *
 * <p>Data persists across application restarts. The database file is located
 * at {@code ./db/alsafe} relative to the working directory.</p>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JpaUserRepository
        extends JpaAutoTxRepository<User, EmailAddress, EmailAddress>
        implements UserRepository {

    /**
     * Creates a repository that manages its own transactions (standalone mode).
     *
     * @param persistenceUnitName the JPA persistence unit name
     * @param extendedProperties  additional JPA properties
     */
    public JpaUserRepository(final String persistenceUnitName, final Map extendedProperties) {
        super(persistenceUnitName, extendedProperties, "email");
    }

    /**
     * Creates a repository that participates in an externally managed transaction.
     *
     * @param autoTx the shared transactional context
     */
    public JpaUserRepository(final TransactionalContext autoTx) {
        super(autoTx, "email");
    }

    /**
     * Returns all users whose account is currently active.
     *
     * @return iterable of active {@link User} records
     */
    @Override
    public Iterable<User> findAllActive() {
        return match("e.active = true");
    }

    /**
     * Finds a user by their email address.
     *
     * @param email the email to search for
     * @return an {@link Optional} containing the user if found
     */
    @Override
    public Optional<User> findByEmail(final EmailAddress email) {
        final Map<String, Object> params = new HashMap<>();
        params.put("email", email);
        return matchOne("e.email = :email", params);
    }

    /**
     * Returns all users with the given role label (e.g. {@code "BACKOFFICE_OPERATOR"}).
     *
     * @param role the role string to filter by
     * @return iterable of matching {@link User} records
     */
    @Override
    public Iterable<User> findByRole(final String role) {
        final Map<String, Object> params = new HashMap<>();
        params.put("role", role);
        return match("e.role = :role", params);
    }

    /**
     * Returns {@code true} if a user with the given phone number already exists.
     *
     * @param phone the phone number to check
     * @return {@code true} if already taken
     */
    @Override
    public boolean existsByPhone(final String phone) {
        final Map<String, Object> params = new HashMap<>();
        params.put("phone", phone);
        return matchOne("e.phoneNumber = :phone", params).isPresent();
    }

    /**
     * Returns {@code true} if a user with the given staff number already exists.
     *
     * @param staffNumber the staff number to check
     * @return {@code true} if already taken
     */
    @Override
    public boolean existsByStaffNumber(final String staffNumber) {
        final Map<String, Object> params = new HashMap<>();
        params.put("staffNumber", staffNumber);
        return matchOne("e.staffNumber = :staffNumber", params).isPresent();
    }
}
