package persistence.impl.inmemory;

import lprog.FlightPlan;
import repositories.FlightPlanRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryFlightPlanRepository implements FlightPlanRepository {

    private final List<FlightPlan> store = new ArrayList<>();

    @Override
    public <S extends FlightPlan> S save(final S flightPlan) {
        store.add(flightPlan);
        return flightPlan;
    }

    @Override
    public boolean existsByFlightId(final String flightId) {
        return store.stream()
                .anyMatch(f -> f.flightId().equalsIgnoreCase(flightId));
    }

    @Override
    public Optional<FlightPlan> findByFlightId(final String flightId) {
        return store.stream()
                .filter(f -> f.flightId().equalsIgnoreCase(flightId))
                .findFirst();
    }

    @Override
    public Iterable<FlightPlan> findAll() {
        return new ArrayList<>(store);
    }

    @Override
    public Optional<FlightPlan> ofIdentity(final Long id) {
        return store.stream()
                .filter(f -> f.identity() != null && f.identity().equals(id))
                .findFirst();
    }

    @Override
    public boolean containsOfIdentity(final Long id) {
        return store.stream()
                .anyMatch(f -> f.identity() != null && f.identity().equals(id));
    }

    @Override
    public long count() {
        return store.size();
    }
}