package controller;

import domain.AirControlArea;
import domain.AirControlAreaCode;
import domain.BaseRoles;
import domain.GeographicBoundary;
import domain.ObservationPeriod;
import domain.WeatherData;
import domain.WeatherSubArea;
import domain.WindCondition;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import persistence.PersistenceContext;
import repositories.AirControlAreaRepository;
import repositories.WeatherDataRepository;

import java.time.LocalDateTime;

/**
 * Application-layer controller for US041 — Register Weather Data.
 *
 * <p>Orchestrates the registration of a weather data record following the
 * EAPLI layered architecture pattern (UI → Controller → Domain/Repository).</p>
 *
 * <p>Business rules enforced here:</p>
 * <ol>
 *   <li>Only authenticated users with the {@code WEATHER_PERSON} role may proceed.</li>
 *   <li>The referenced air control area must exist.</li>
 *   <li>The sub-area bounding box must lie entirely inside the area's boundary.</li>
 *   <li>No conflicting weather record may already exist (same area, overlapping
 *       sub-area, overlapping period).</li>
 * </ol>
 *
 * <p>All value-object invariants (coordinate ranges, wind direction/speed bounds,
 * period end > start) are enforced by the domain classes themselves.</p>
 */
public class RegisterWeatherDataController {

    private final AuthorizationService authz;
    private final AirControlAreaRepository areaRepo;
    private final WeatherDataRepository weatherRepo;

    /**
     * Production constructor — obtains all collaborators from the framework
     * registry and the {@link PersistenceContext}.
     */
    public RegisterWeatherDataController() {
        this.authz       = AuthzRegistry.authorizationService();
        this.areaRepo    = PersistenceContext.getInstance().airControlAreaRepository();
        this.weatherRepo = PersistenceContext.getInstance().weatherDataRepository();
    }

    /**
     * Package-visible constructor for testing — allows injection of collaborators.
     *
     * @param authz       the authorization service
     * @param areaRepo    the air control area repository
     * @param weatherRepo the weather data repository
     */
    RegisterWeatherDataController(final AuthorizationService authz,
                                   final AirControlAreaRepository areaRepo,
                                   final WeatherDataRepository weatherRepo) {
        this.authz       = authz;
        this.areaRepo    = areaRepo;
        this.weatherRepo = weatherRepo;
    }

    /**
     * Returns all registered air control areas for display in the UI.
     *
     * <p>The UI shows the existing areas (with their boundaries) so the user
     * can choose one and enter sub-area coordinates that fit inside it.</p>
     *
     * @return iterable of all {@link AirControlArea} records
     */
    public Iterable<AirControlArea> allAreas() {
        authz.ensureAuthenticatedUserHasAnyOf(BaseRoles.WEATHER_PERSON);
        return areaRepo.findAll();
    }

    /**
     * Registers a new weather data record.
     *
     * @param areaCode      code of the air control area (must exist)
     * @param subMinLat     sub-area min latitude
     * @param subMaxLat     sub-area max latitude (must be &gt; subMinLat)
     * @param subMinLon     sub-area min longitude
     * @param subMaxLon     sub-area max longitude (must be &gt; subMinLon)
     * @param startDateTime start of the observation period
     * @param endDateTime   end of the observation period (must be after start)
     * @param windDirection wind direction in degrees [0, 360[
     * @param windSpeed     wind speed in m/s (&gt;= 0)
     * @return the persisted {@link WeatherData}
     * @throws IllegalArgumentException if any rule is violated
     */
    public WeatherData registerWeatherData(final String areaCode,
                                            final double subMinLat,
                                            final double subMaxLat,
                                            final double subMinLon,
                                            final double subMaxLon,
                                            final LocalDateTime startDateTime,
                                            final LocalDateTime endDateTime,
                                            final double windDirection,
                                            final double windSpeed) {
        // Step 1 — authorisation (must be first)
        authz.ensureAuthenticatedUserHasAnyOf(BaseRoles.WEATHER_PERSON);

        // Step 2 — area must exist
        final AirControlAreaCode code = new AirControlAreaCode(areaCode);
        final AirControlArea area = areaRepo.ofIdentity(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Air control area '" + areaCode + "' does not exist."));

        // Step 3 — build value objects (their constructors validate the values)
        final GeographicBoundary subBoundary =
                new GeographicBoundary(subMinLat, subMaxLat, subMinLon, subMaxLon);
        final WeatherSubArea subArea = new WeatherSubArea(subBoundary);

        // Step 4 — sub-area must be inside the parent area
        if (!subArea.isInsideBoundary(area.boundary())) {
            throw new IllegalArgumentException(
                    "The sub-area boundary must lie entirely inside the air control area '"
                            + areaCode + "' boundary: " + area.boundary());
        }

        final ObservationPeriod period = new ObservationPeriod(startDateTime, endDateTime);
        final WindCondition wind = new WindCondition(windDirection, windSpeed);

        // Step 5 — no conflicting record
        if (weatherRepo.existsConflicting(code, subArea, period)) {
            throw new IllegalArgumentException(
                    "A weather record already exists for area '" + areaCode
                            + "' that overlaps with the given sub-area and time period.");
        }

        // Step 6 — create and persist
        final WeatherData weatherData = new WeatherData(code, subArea, period, wind);
        return weatherRepo.save(weatherData);
    }
}
