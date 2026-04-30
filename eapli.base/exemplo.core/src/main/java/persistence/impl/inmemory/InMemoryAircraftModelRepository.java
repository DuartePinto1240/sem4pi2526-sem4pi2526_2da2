package persistence.impl.inmemory;

import domain.aircraftmodel.AircraftModel;
import repositories.AircraftModelRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryAircraftModelRepository implements AircraftModelRepository {

    private final List<AircraftModel> store = new ArrayList<>();

    @Override
    public <S extends AircraftModel> S save(final S aircraftModel) {
        store.add(aircraftModel);
        return aircraftModel;
    }

    @Override
    public boolean existsByNameAndManufacturer(final String name,
                                               final String manufacturer) {
        return store.stream()
                .anyMatch(a -> a.modelName().equalsIgnoreCase(name)
                        && a.manufacturer().equalsIgnoreCase(manufacturer));
    }

    @Override
    public Optional<AircraftModel> findByNameAndManufacturer(final String name,
                                                             final String manufacturer) {
        return store.stream()
                .filter(a -> a.modelName().equalsIgnoreCase(name)
                        && a.manufacturer().equalsIgnoreCase(manufacturer))
                .findFirst();
    }

    @Override
    public Iterable<AircraftModel> findAll() {
        return new ArrayList<>(store);
    }

    @Override
    public Optional<AircraftModel> ofIdentity(final Long id) {
        return store.stream()
                .filter(a -> a.identity() != null && a.identity().equals(id))
                .findFirst();
    }

    @Override
    public boolean containsOfIdentity(final Long id) {
        return store.stream()
                .anyMatch(a -> a.identity() != null && a.identity().equals(id));
    }

    @Override
    public long count() {
        return store.size();
    }
}