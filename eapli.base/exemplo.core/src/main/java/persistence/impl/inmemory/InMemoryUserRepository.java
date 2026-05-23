package persistence.impl.inmemory;

import domain.EmailAddress;
import domain.User;
import repositories.UserRepository;

import java.util.*;

/**
 * In-memory implementation of {@link UserRepository}.
 *
 * <p>Used during development and testing as a lightweight alternative
 * to the JPA repository. Data is lost when the application stops.</p>
 *
 * <p>Uniqueness of phone number and staff number is enforced here
 * in addition to the domain-level validations.</p>
 */
public class InMemoryUserRepository implements UserRepository {

    /** Internal storage keyed by email (the aggregate identity). */
    private final Map<EmailAddress, User> store = new LinkedHashMap<>();

    /**
     * {@inheritDoc}
     *
     * <p>Also enforces uniqueness of phone number and staff number
     * across all stored users.</p>
     *
     * @throws IllegalArgumentException if phone number or staff number already exists
     */
    @Override
    public <S extends User> S save(final S entity) {
        // Only check uniqueness for new users (not updates to existing ones)
        boolean isNew = !store.containsKey(entity.identity());
        if (isNew) {
            for (User existing : store.values()) {
                if (existing.getPhoneNumber().equals(entity.getPhoneNumber())) {
                    throw new IllegalArgumentException(
                            "Phone number " + entity.getPhoneNumber() + " is already in use.");
                }
                if (existing.getStaffNumber().equals(entity.getStaffNumber())) {
                    throw new IllegalArgumentException(
                            "Staff number " + entity.getStaffNumber() + " is already in use.");
                }
            }
        }
        store.put(entity.identity(), entity);
        return entity;
    }

    /**
     * Finds a user by their email address.
     *
     * @param email the email to search for
     * @return an {@link Optional} containing the user if found
     */
    @Override
    public Optional<User> findByEmail(final EmailAddress email) {
        return Optional.ofNullable(store.get(email));
    }

    /**
     * Returns all currently active users.
     *
     * @return iterable of active users
     */
    @Override
    public Iterable<User> findAllActive() {
        final List<User> active = new ArrayList<>();
        for (User u : store.values()) {
            if (u.isActive()) active.add(u);
        }
        return active;
    }

    /**
     * Returns all users regardless of status.
     *
     * @return unmodifiable iterable of all users
     */
    @Override
    public Iterable<User> findAll() {
        return Collections.unmodifiableCollection(store.values());
    }

    /**
     * Returns all users with the given role label (e.g. {@code "BACKOFFICE_OPERATOR"}).
     *
     * @param role the role string to filter by
     * @return iterable of matching {@link User} records
     */
    @Override
    public Iterable<User> findByRole(final String role) {
        final List<User> result = new ArrayList<>();
        for (User u : store.values()) {
            if (role.equals(u.getRole())) result.add(u);
        }
        return result;
    }

    /**
     * Returns {@code true} if a user with the given phone number already exists.
     *
     * @param phone the phone number to check
     * @return {@code true} if already taken
     */
    @Override
    public boolean existsByPhone(final String phone) {
        return store.values().stream()
                .anyMatch(u -> u.getPhoneNumber().equals(phone));
    }

    /**
     * Returns {@code true} if a user with the given staff number already exists.
     *
     * @param staffNumber the staff number to check
     * @return {@code true} if already taken
     */
    @Override
    public boolean existsByStaffNumber(final String staffNumber) {
        return store.values().stream()
                .anyMatch(u -> u.getStaffNumber().equals(staffNumber));
    }

    /** {@inheritDoc} */
    @Override
    public Optional<User> ofIdentity(final EmailAddress id) {
        return findByEmail(id);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(final User entity) {
        store.remove(entity.identity());
    }

    /** {@inheritDoc} */
    @Override
    public void deleteOfIdentity(final EmailAddress id) {
        store.remove(id);
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsOfIdentity(final EmailAddress id) {
        return store.containsKey(id);
    }

    /** {@inheritDoc} */
    @Override
    public long count() {
        return store.size();
    }

    /** {@inheritDoc} */
    @Override
    public boolean contains(final User entity) {
        return store.containsKey(entity.identity());
    }
}
