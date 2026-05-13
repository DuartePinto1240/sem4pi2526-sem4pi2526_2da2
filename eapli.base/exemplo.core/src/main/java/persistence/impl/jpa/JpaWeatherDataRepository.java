package persistence.impl.jpa;

import domain.AirControlAreaCode;
import domain.ObservationPeriod;
import domain.WeatherData;
import domain.WeatherSubArea;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import repositories.WeatherDataRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * JPA implementation of {@link WeatherDataRepository} (US041).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JpaWeatherDataRepository
        extends JpaAutoTxRepository<WeatherData, Long, Long>
        implements WeatherDataRepository {

    public JpaWeatherDataRepository(final String persistenceUnitName,
                                    final Map extendedProperties) {
        super(persistenceUnitName, extendedProperties, "id");
    }

    @Override
    public Iterable<WeatherData> findByArea(final AirControlAreaCode areaCode) {
        final Map<String, Object> params = new HashMap<>();
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
