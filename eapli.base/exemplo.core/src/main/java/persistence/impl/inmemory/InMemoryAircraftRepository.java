package persistence.impl.inmemory;

import domain.aircraft.Aircraft;
import repositories.AircraftRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InMemoryAircraftRepository implements AircraftRepository {

    private final List<Aircraft> store = new ArrayList<>();

    @Override
    public boolean existsByRegistration(final String registration) {
        return store.stream()
                .anyMatch(a -> a.registration()
                        .equalsIgnoreCase(registration.trim()));
    }

    @Override
    public Optional<Aircraft> ofIdentity(final String registration) {
        return store.stream()
                .filter(a -> a.registration()
                        .equalsIgnoreCase(registration.trim()))
                .findFirst();
    }

    @Override
    public Iterable<Aircraft> findAll() {
        return new ArrayList<>(store);
    }

    @Override
    public Iterable<Aircraft> findByCompanyIata(final String companyIata) {
        return store.stream()
                .filter(a -> a.companyIata()
                        .equalsIgnoreCase(companyIata.trim()))
                .collect(Collectors.toList());
    }

    @Override
    public <S extends Aircraft> S save(final S aircraft) {
        // update if already exists, insert if not
        store.removeIf(a -> a.registration()
                .equalsIgnoreCase(aircraft.registration()));
        store.add(aircraft);
        return aircraft;
    }

    @Override
    public boolean containsOfIdentity(final String registration) {
        return existsByRegistration(registration);
    }

    @Override
    public long count() {
        return store.size();
    }
}