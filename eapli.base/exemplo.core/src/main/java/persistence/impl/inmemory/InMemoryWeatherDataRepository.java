package persistence.impl.inmemory;

import domain.AirControlAreaCode;
import domain.ObservationPeriod;
import domain.WeatherData;
import domain.WeatherSubArea;
import repositories.WeatherDataRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory implementation of {@link WeatherDataRepository} (US041).
 */
public class InMemoryWeatherDataRepository implements WeatherDataRepository {

    private final List<WeatherData> store    = new ArrayList<>();
    private final AtomicLong        sequence = new AtomicLong(1);

    @Override
    public WeatherData save(final WeatherData entity) {
        if (entity.identity() == null) {
            injectId(entity, sequence.getAndIncrement());
        }
        store.removeIf(w -> entity.identity().equals(w.identity()));
        store.add(entity);
        return entity;
    }

    @Override
    public Optional<WeatherData> ofIdentity(final Long id) {
        return store.stream().filter(w -> id.equals(w.identity())).findFirst();
    }

    @Override
    public boolean containsOfIdentity(final Long id) {
        return store.stream().anyMatch(w -> id.equals(w.identity()));
    }

    @Override
    public Iterable<WeatherData> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(store));
    }

    @Override
    public void delete(final WeatherData entity) {
        store.remove(entity);
    }

    @Override
    public void deleteOfIdentity(final Long id) {
        store.removeIf(w -> id.equals(w.identity()));
    }

    @Override
    public long count() {
        return store.size();
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

    /** Injects the generated Long id into the entity via reflection. */
    private static void injectId(final WeatherData entity, final long value) {
        try {
            final java.lang.reflect.Field f =
                    WeatherData.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, value);
        } catch (final Exception e) {
            throw new IllegalStateException("Cannot inject WeatherData id", e);
        }
    }
}