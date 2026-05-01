package persistence.impl.inmemory;

import domain.aircraftmodel.AircraftModel;
import repositories.AircraftModelRepository;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAircraftModelRepository implements AircraftModelRepository {

    private final List<AircraftModel> store = new ArrayList<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public <S extends AircraftModel> S save(final S aircraftModel) {
        // se ainda não tem ID, atribui um
        if (aircraftModel.identity() == null) {
            try {
                final Field idField = AircraftModel.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(aircraftModel, idSequence.getAndIncrement());
            } catch (final NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Failed to set ID on AircraftModel", e);
            }
        }
        store.removeIf(a -> a.identity().equals(aircraftModel.identity()));
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