package persistence.impl.jpa;

import domain.AirControlAreaCode;
import domain.ObservationPeriod;
import domain.WeatherData;
import domain.WeatherSubArea;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import repositories.WeatherDataRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public JpaWeatherDataRepository(final String persistenceUnitName, final Map extendedProperties) {
        super(persistenceUnitName, extendedProperties, "id");
    }

    @Override
    public Iterable<WeatherData> findByArea(final AirControlAreaCode areaCode) {
        final Map<String, Object> params = new HashMap<>();
        params.put("code", areaCode.code());
        return match("e.areaCode.code = :code", params);
    }

    @Override
    public Iterable<WeatherData> findByAreaAndDate(final AirControlAreaCode areaCode, final LocalDate date) {

        // Convert the date to the start and end of the day for the JPQL range query
        final LocalDateTime dayStart = date.atStartOfDay();
        final LocalDateTime dayEnd   = date.plusDays(1).atStartOfDay();

        final Map<String, Object> params = new HashMap<>();
        params.put("code",     areaCode.code());
        params.put("dayStart", dayStart);
        params.put("dayEnd",   dayEnd);

        // A period covers the date if it starts before end-of-day AND ends after start-of-day
        return match("e.areaCode.code = :code AND e.period.startDateTime < :dayEnd "
                        + "AND e.period.endDateTime   > :dayStart", params);
    }

    @Override
    public boolean existsConflicting(final AirControlAreaCode areaCode, final WeatherSubArea subArea,
                                     final ObservationPeriod period) {
        return StreamSupport.stream(findByArea(areaCode).spliterator(), false)
                .filter(w -> w.subArea().overlapsWith(subArea)).anyMatch(w -> w.period().overlapsWith(period));
    }
}
