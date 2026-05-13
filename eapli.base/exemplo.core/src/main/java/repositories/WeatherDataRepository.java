package repositories;

import domain.AirControlAreaCode;
import domain.ObservationPeriod;
import domain.WeatherData;
import domain.WeatherSubArea;
import eapli.framework.domain.repositories.DomainRepository;

/**
 * Repository interface for {@link WeatherData} aggregate roots (US041).
 *
 * <p>Identity type is {@code Long} — the plain auto-generated primary key.</p>
 */
public interface WeatherDataRepository
        extends DomainRepository<Long, WeatherData> {

    /**
     * Returns all weather data records for a given air control area.
     *
     * @param areaCode the air control area code to filter by
     * @return iterable of matching records
     */
    Iterable<WeatherData> findByArea(AirControlAreaCode areaCode);

    /**
     * Returns {@code true} if a conflicting record already exists.
     *
     * <p>A conflict exists when area codes match AND sub-areas overlap
     * geographically AND observation periods overlap temporally.</p>
     *
     * @param areaCode the area to check
     * @param subArea  the sub-area to test for geographic overlap
     * @param period   the period to test for temporal overlap
     * @return {@code true} if at least one conflicting record exists
     */
    boolean existsConflicting(AirControlAreaCode areaCode,
                              WeatherSubArea subArea,
                              ObservationPeriod period);
}
