package repositories;

import domain.AirControlAreaCode;
import domain.ObservationPeriod;
import domain.WeatherData;
import domain.WeatherDataId;
import domain.WeatherSubArea;
import eapli.framework.domain.repositories.DomainRepository;

/**
 * Repository interface for {@link WeatherData} aggregate roots (US041).
 *
 * <p>Extends the EAPLI Framework's {@link DomainRepository} which already
 * provides {@code save}, {@code ofIdentity}, {@code containsOfIdentity},
 * {@code delete}, {@code deleteOfIdentity}, and {@code count}.</p>
 */
public interface WeatherDataRepository
        extends DomainRepository<WeatherDataId, WeatherData> {

    /**
     * Returns all weather data records for a given air control area.
     *
     * @param areaCode the air control area code to filter by
     * @return iterable of matching {@link WeatherData} records
     */
    Iterable<WeatherData> findByArea(AirControlAreaCode areaCode);

    /**
     * Checks whether a conflicting weather record already exists for the given
     * area, sub-area, and time period.
     *
     * <p>A conflict exists when:</p>
     * <ul>
     *   <li>the area code matches, AND</li>
     *   <li>the sub-areas overlap (share any geographic region), AND</li>
     *   <li>the periods overlap (share any time interval).</li>
     * </ul>
     *
     * <p>This reflects the professor's statement that having two weather records
     * for the same sub-area in the same period would be contradictory.
     * For the MVP, overlapping sub-areas in overlapping periods are simply
     * rejected to avoid ambiguity during simulation.</p>
     *
     * @param areaCode the area to check
     * @param subArea  the sub-area to check for geographic overlap
     * @param period   the time period to check for temporal overlap
     * @return {@code true} if at least one conflicting record exists
     */
    boolean existsConflicting(AirControlAreaCode areaCode,
                               WeatherSubArea subArea,
                               ObservationPeriod period);
}
