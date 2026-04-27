package persistence.impl.inmemory;

import domain.enginemodel.EngineModel;
import domain.enginemodel.MotorizationType;
import repositories.EngineModelRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InMemoryEngineModelRepository implements EngineModelRepository {

    private final List<EngineModel> store = new ArrayList<>();

    @Override
    public <S extends EngineModel> S save(final S engineModel) {
        store.add(engineModel);
        return engineModel;
    }

    @Override
    public boolean existsByNameAndManufacturer(final String name,
                                               final String manufacturer) {
        return store.stream()
                .anyMatch(e -> e.name().equalsIgnoreCase(name)
                        && e.manufacturer().equalsIgnoreCase(manufacturer));
    }

    @Override
    public Optional<EngineModel> findByNameAndManufacturer(final String name,
                                                           final String manufacturer) {
        return store.stream()
                .filter(e -> e.name().equalsIgnoreCase(name)
                        && e.manufacturer().equalsIgnoreCase(manufacturer))
                .findFirst();
    }

    @Override
    public Iterable<EngineModel> findAll() {
        return new ArrayList<>(store);
    }

    @Override
    public Iterable<EngineModel> findByMotorizationType(final MotorizationType type) {
        return store.stream()
                .filter(e -> e.motorizationType() == type)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<EngineModel> ofIdentity(final Long id) {
        return store.stream()
                .filter(e -> e.identity() != null && e.identity().equals(id))
                .findFirst();
    }

    @Override
    public boolean containsOfIdentity(final Long id) {
        return store.stream()
                .anyMatch(e -> e.identity() != null && e.identity().equals(id));
    }

    @Override
    public long count() {
        return store.size();
    }
}