package persistence.impl.inmemory;

import domain.AirControlAreaCode;
import domain.ObservationPeriod;
import domain.WeatherData;
import domain.WeatherDataId;
import domain.WeatherSubArea;
import repositories.WeatherDataRepository;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory implementation of {@link WeatherDataRepository} (US041).
 *
 * <p>Used during development and testing. Data does not persist across
 * application restarts. For persistent storage use the JPA implementation.</p>
 */
public class InMemoryWeatherDataRepository implements WeatherDataRepository {

    private final List<WeatherData> store = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong(1);

    @Override
    public WeatherData save(final WeatherData entity) {
        if (entity.identity() == null) {
            injectId(entity, sequence.getAndIncrement());
        }
        store.removeIf(w -> w.identity() != null
                && w.identity().equals(entity.identity()));
        store.add(entity);
        return entity;
    }

    @Override
    public Optional<WeatherData> ofIdentity(final WeatherDataId id) {
        return store.stream()
                .filter(w -> id.equals(w.identity()))
                .findFirst();
    }

    @Override
    public boolean containsOfIdentity(final WeatherDataId id) {
        return store.stream().anyMatch(w -> id.equals(w.identity()));
    }

    @Override
    public void delete(final WeatherData entity) {
        store.remove(entity);
    }

    @Override
    public void deleteOfIdentity(final WeatherDataId id) {
        store.removeIf(w -> id.equals(w.identity()));
    }

    @Override
    public long count() {
        return store.size();
    }

    /**
     * Returns all weather data records stored in memory.
     *
     * <p>Required by {@code DomainRepository}. Returns an unmodifiable
     * view to protect the internal store from external mutation.</p>
     */
    @Override
    public Iterable<WeatherData> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(store));
    }

    @Override
    public Iterable<WeatherData> findByArea(final AirControlAreaCode areaCode) {
        return store.stream()
                .filter(w -> w.areaCode().equals(areaCode))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsConflicting(final AirControlAreaCode areaCode,
                                     final WeatherSubArea subArea,
                                     final ObservationPeriod period) {
        return store.stream()
                .filter(w -> w.areaCode().equals(areaCode))
                .filter(w -> w.subArea().overlapsWith(subArea))
                .anyMatch(w -> w.period().overlapsWith(period));
    }

    /**
     * Injects a generated ID into a {@link WeatherData} instance via reflection,
     * simulating what the JPA {@code @GeneratedValue} would do on first persist.
     */
    private void injectId(final WeatherData entity, final long idValue) {
        try {
            final Field idField = WeatherData.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, new WeatherDataId(idValue));
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to inject WeatherData ID", e);
        }
    }
}