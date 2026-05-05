package persistence.impl.inmemory;

import domain.Country;
import repositories.CountryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In-memory implementation of {@link CountryRepository}.
 *
 * <p>Sufficient for bootstrap data that does not need to persist across
 * restarts (countries are reloaded from {@code CountryBootstrapper} every time).
 * If persistence is needed in the future, a JPA implementation can be added.</p>
 */
public class InMemoryCountryRepository implements CountryRepository {

    private final List<Country> store = new ArrayList<>();

    @Override
    public Country save(final Country country) {
        // Replace if already exists (idempotent bootstrap)
        store.removeIf(c -> c.name().equalsIgnoreCase(country.name()));
        store.add(country);
        return country;
    }

    @Override
    public Iterable<Country> findAll() {
        return new ArrayList<>(store);
    }

    @Override
    public Optional<Country> findByName(final String name) {
        if (name == null) return Optional.empty();
        return store.stream()
                .filter(c -> c.name().equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    @Override
    public boolean existsByName(final String name) {
        if (name == null) return false;
        return store.stream()
                .anyMatch(c -> c.name().equalsIgnoreCase(name.trim()));
    }

    @Override
    public long count() {
        return store.size();
    }
}