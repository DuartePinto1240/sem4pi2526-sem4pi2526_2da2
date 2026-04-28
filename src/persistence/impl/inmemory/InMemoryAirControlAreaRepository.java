package persistence.impl.inmemory;

import domain.AirControlArea;
import domain.AirControlAreaCode;
import domain.GeographicBoundary;
import repositories.AirControlAreaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In-memory implementation of {@link AirControlAreaRepository} (US050).
 *
 * <p>Used during development and testing. Data does not persist across
 * application restarts. For persistent storage use
 * {@link persistence.impl.jpa.JpaAirControlAreaRepository} instead.</p>
 */
public class InMemoryAirControlAreaRepository
        implements AirControlAreaRepository {

    private final List<AirControlArea> areas = new ArrayList<>();

    @Override
    public Iterable<AirControlArea> findAll() {
        return new ArrayList<>(areas);
    }

    @Override
    public boolean existsByName(final String name) {
        return areas.stream()
                .anyMatch(a -> a.name().equalsIgnoreCase(name.trim()));
    }

    @Override
    public boolean existsOverlappingBoundary(final GeographicBoundary boundary) {
        return areas.stream()
                .anyMatch(a -> a.boundary().overlapsWith(boundary));
    }

    @Override
    public AirControlArea save(final AirControlArea area) {
        areas.removeIf(a -> a.identity().equals(area.identity()));
        areas.add(area);
        return area;
    }

    @Override
    public Optional<AirControlArea> ofIdentity(final AirControlAreaCode code) {
        return areas.stream()
                .filter(a -> a.identity().equals(code))
                .findFirst();
    }

    @Override
    public boolean containsOfIdentity(final AirControlAreaCode code) {
        return areas.stream()
                .anyMatch(a -> a.identity().equals(code));
    }

    @Override
    public void delete(final AirControlArea area) {
        areas.remove(area);
    }

    @Override
    public void deleteOfIdentity(final AirControlAreaCode code) {
        areas.removeIf(a -> a.identity().equals(code));
    }

    @Override
    public long count() {
        return areas.size();
    }
}
