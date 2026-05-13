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
 * EAPLI layered architecture (UI → Controller → Domain/Repository).</p>
 *
 * <p>Business rules enforced:</p>
 * <ol>
 *   <li>Only authenticated users with {@code WEATHER_PERSON} role may proceed.</li>
 *   <li>The referenced air control area must exist.</li>
 *   <li>The sub-area bounding box must lie entirely inside the area's boundary.</li>
 *   <li>No conflicting weather record may already exist (same area, overlapping
 *       sub-area, overlapping period).</li>
 * </ol>
 *
 * <p>Value-object invariants (wind direction/speed bounds, period end &gt; start,
 * coordinate ranges) are enforced by the domain classes themselves.</p>
 */
public class RegisterWeatherDataController {

    private final AuthorizationService   authz;
    private final AirControlAreaRepository areaRepo;
    private final WeatherDataRepository  weatherRepo;

    /** Production constructor — wires collaborators from framework registries. */
    public RegisterWeatherDataController() {
        this.authz       = AuthzRegistry.authorizationService();
        this.areaRepo    = PersistenceContext.getInstance().airControlAreaRepository();
        this.weatherRepo = PersistenceContext.getInstance().weatherDataRepository();
    }

    /** Package-visible constructor for unit tests — allows collaborator injection. */
    RegisterWeatherDataController(final AuthorizationService authz,
                                  final AirControlAreaRepository areaRepo,
                                  final WeatherDataRepository weatherRepo) {
        this.authz       = authz;
        this.areaRepo    = areaRepo;
        this.weatherRepo = weatherRepo;
    }

    /**
     * Returns all registered air control areas.
     *
     * <p>Called by the UI to display the list from which the user picks one.</p>
     */
    public Iterable<AirControlArea> allAreas() {
        authz.ensureAuthenticatedUserHasAnyOf(BaseRoles.WEATHER_PERSON);
        return areaRepo.findAll();
    }

    /**
     * Returns all existing weather data records for a given air control area.
     *
     * <p>Called by the UI after the user selects an area, so they can see
     * which sub-areas are already covered and choose non-conflicting coordinates.</p>
     *
     * @param areaCode the area code chosen by the user
     * @return iterable of existing {@link WeatherData} records for that area
     */
    public Iterable<WeatherData> existingWeatherDataForArea(final String areaCode) {
        authz.ensureAuthenticatedUserHasAnyOf(BaseRoles.WEATHER_PERSON);
        return weatherRepo.findByArea(new AirControlAreaCode(areaCode));
    }

    /**
     * Registers a new weather data record.
     *
     * @param areaCode      code of the air control area (must exist)
     * @param subMinLat     sub-area min latitude
     * @param subMaxLat     sub-area max latitude (&gt; subMinLat)
     * @param subMinLon     sub-area min longitude
     * @param subMaxLon     sub-area max longitude (&gt; subMinLon)
     * @param startDateTime start of the observation period
     * @param endDateTime   end of the observation period (strictly after start)
     * @param windDirection wind direction in degrees [0, 360[
     * @param windSpeed     wind speed in m/s (&gt;= 0)
     * @return the persisted {@link WeatherData}
     * @throws IllegalArgumentException  if any domain rule is violated
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
        // 1 — authorisation (always first)
        authz.ensureAuthenticatedUserHasAnyOf(BaseRoles.WEATHER_PERSON);

        // 2 — area must exist
        final AirControlAreaCode code = new AirControlAreaCode(areaCode);
        final AirControlArea area = areaRepo.ofIdentity(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Air control area '" + areaCode + "' does not exist."));

        // 3 — build value objects (constructors validate their own invariants)
        final GeographicBoundary subBoundary =
                new GeographicBoundary(subMinLat, subMaxLat, subMinLon, subMaxLon);
        final WeatherSubArea subArea = new WeatherSubArea(subBoundary);

        // 4 — sub-area must be inside the parent area
        if (!subArea.isInsideBoundary(area.boundary())) {
            throw new IllegalArgumentException(
                    "The sub-area must lie entirely inside the air control area '"
                            + areaCode + "' boundary: " + area.boundary());
        }

        final ObservationPeriod period =
                new ObservationPeriod(startDateTime, endDateTime);
        final WindCondition wind = new WindCondition(windDirection, windSpeed);

        // 5 — no conflicting record
        if (weatherRepo.existsConflicting(code, subArea, period)) {
            throw new IllegalArgumentException(
                    "A weather record already exists for area '" + areaCode
                            + "' that overlaps with the given sub-area and time period.");
        }

        // 6 — create and persist
        return weatherRepo.save(new WeatherData(code, subArea, period, wind));
    }
}
