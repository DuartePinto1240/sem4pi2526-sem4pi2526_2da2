package persistence.impl.jpa;

import domain.AirControlAreaCode;
import domain.ObservationPeriod;
import domain.WeatherData;
import domain.WeatherDataId;
import domain.WeatherSubArea;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import repositories.WeatherDataRepository;

import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * JPA implementation of {@link WeatherDataRepository} (US041).
 *
 * <p>Extends {@link JpaAutoTxRepository} — the same base class used by all
 * other JPA repositories in this project — which manages transactions per
 * operation automatically.</p>
 *
 * <p>The conflict check ({@link #existsConflicting}) loads all records for
 * the given area and applies the geographic and temporal overlap tests in
 * memory. This is a deliberate MVP simplification: for the data volumes
 * expected in a prototype, a full in-memory scan is acceptable. A production
 * system would push the overlap logic into a spatial SQL query.</p>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JpaWeatherDataRepository
        extends JpaAutoTxRepository<WeatherData, WeatherDataId, WeatherDataId>
        implements WeatherDataRepository {

    /**
     * Standalone constructor — manages its own transactions.
     *
     * @param persistenceUnitName the JPA persistence unit name
     * @param extendedProperties  additional JPA properties
     */
    public JpaWeatherDataRepository(final String persistenceUnitName,
                                     final Map extendedProperties) {
        super(persistenceUnitName, extendedProperties, "id");
    }

    @Override
    public Iterable<WeatherData> findByArea(final AirControlAreaCode areaCode) {
        final Map<String, Object> params = new java.util.HashMap<>();
        params.put("code", areaCode.code());
        return match("e.areaCode.code = :code", params);
    }

    @Override
    public boolean existsConflicting(final AirControlAreaCode areaCode,
                                      final WeatherSubArea subArea,
                                      final ObservationPeriod period) {
        return StreamSupport
                .stream(findByArea(areaCode).spliterator(), false)
                .filter(w -> w.subArea().overlapsWith(subArea))
                .anyMatch(w -> w.period().overlapsWith(period));
    }
}
