package persistence.impl.inmemory;

import domain.EmailAddress;
import domain.User;
import repositories.UserRepository;

import java.util.*;

/**
 * Repositório in-memory para desenvolvimento e testes.
 * US031, US032, US033
 */
public class InMemoryUserRepository implements UserRepository {

    private final Map<EmailAddress, User> store = new LinkedHashMap<>();

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return Optional.ofNullable(store.get(email));
    }

    @Override
    public Iterable<User> findAllActive() {
        List<User> active = new ArrayList<>();
        for (User u : store.values()) {
            if (u.isActive()) active.add(u);
        }
        return active;
    }

    @Override
    public Iterable<User> findAll() {
        return Collections.unmodifiableCollection(store.values());
    }

    @Override
    public <S extends User> S save(S entity) {
        store.put(entity.identity(), entity);
        return entity;
    }

    @Override
    public Optional<User> ofIdentity(EmailAddress id) {
        return findByEmail(id);
    }

    @Override
    public void delete(User entity) {
        store.remove(entity.identity());
    }

    @Override
    public void deleteOfIdentity(EmailAddress id) {
        store.remove(id);
    }

    @Override
    public boolean containsOfIdentity(EmailAddress id) {
        return store.containsKey(id);
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public boolean contains(User entity) {
        return store.containsKey(entity.identity());
    }
}
