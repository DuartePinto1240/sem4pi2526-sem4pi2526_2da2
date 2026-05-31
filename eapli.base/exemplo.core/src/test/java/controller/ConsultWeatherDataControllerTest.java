package controller;

import domain.*;
import dto.WeatherDataDTO;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthenticatedException;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistence.PersistenceContext;
import persistence.impl.inmemory.InMemoryAirControlAreaRepository;
import persistence.impl.inmemory.InMemoryWeatherDataRepository;
import repositories.AirControlAreaRepository;
import repositories.WeatherDataRepository;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ConsultWeatherDataController} (US043).
 */
class ConsultWeatherDataControllerTest {

    private static final String PERSISTENCE_UNIT = "eapli.base.AlSafePU";

    private static final String WP_EMAIL    = "wp.consult@alsafe.com";
    private static final String WP_PASSWORD = "Weather1234";

    private static final String AREA_CODE    = "LPPC";
    private static final double AREA_MIN_LAT = 36.0, AREA_MAX_LAT = 42.0;
    private static final double AREA_MIN_LON = -10.0, AREA_MAX_LON = -6.0;

    private static final LocalDateTime START = LocalDateTime.of(2026, 6, 1, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 6, 2, 23, 59);

    private ConsultWeatherDataController controller;
    private AirControlAreaRepository     areaRepo;
    private WeatherDataRepository        weatherRepo;

    @BeforeEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void setUp() throws Exception {
        // configure AuthzRegistry
        try {
            AuthzRegistry.configure(new JpaAutoTxUserRepository(PERSISTENCE_UNIT, new HashMap<>()),
                    new AlSafePasswordPolicy(), new PlainTextEncoder());
        } catch (final Exception ignored) { }

        // reset PersistenceContext singleton so repos are fresh each test
        final Field instanceField = PersistenceContext.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        // create fresh in-memory repos
        areaRepo    = new InMemoryAirControlAreaRepository();
        weatherRepo = new InMemoryWeatherDataRepository();

        // inject repos into PersistenceContext
        final PersistenceContext ctx = PersistenceContext.getInstance();
        injectField(ctx, "airControlAreaRepository", areaRepo);
        injectField(ctx, "weatherDataRepository",    weatherRepo);

        // clear any existing session
        AuthzRegistry.authorizationService().clearSession();

        // register a WEATHER_PERSON user (ignored if already exists)
        try {
            final Set<Role> roles = new HashSet<>();
            roles.add(BaseRoles.WEATHER_PERSON);
            AuthzRegistry.userService().registerNewUser(
                    WP_EMAIL, WP_PASSWORD, "Alice", "Weather", WP_EMAIL, roles);
        } catch (final Exception ignored) { }

        // log in so authz checks pass
        new LoginController().login(WP_EMAIL, WP_PASSWORD);

        // seed one area and one weather record
        areaRepo.save(new AirControlArea(AREA_CODE, "Lisbon FIR",
                new GeographicBoundary(AREA_MIN_LAT, AREA_MAX_LAT, AREA_MIN_LON, AREA_MAX_LON)));

        weatherRepo.save(new WeatherData(new AirControlAreaCode(AREA_CODE), new WeatherSubArea(
                        new GeographicBoundary(37.0, 40.0, -9.0, -7.0),
                        new AltitudeRange(0.0, 5000.0)), new ObservationPeriod(START, END),
                        new WindCondition(270.0, 15.5)));

        // instantiate SUT via production constructor
        controller = new ConsultWeatherDataController();
    }

    // availableAreas

    @Test
    void ensureAvailableAreasReturnsRegisteredArea() {
        final Iterable<AirControlArea> areas = controller.availableAreas();
        assertTrue(areas.iterator().hasNext());
    }

    @Test
    void ensureUnauthenticatedUserCannotListAreas() {
        AuthzRegistry.authorizationService().clearSession();
        assertThrows(UnauthenticatedException.class,
                () -> controller.availableAreas());
    }

    // weatherDataByAreaAndDate

    @Test
    void ensureResultsReturnedForValidAreaAndDate() {
        final List<WeatherDataDTO> results =
                controller.weatherDataByAreaAndDate(AREA_CODE, LocalDate.of(2026, 6, 1));
        assertFalse(results.isEmpty());
    }



    @Test
    void ensureUnknownAreaCodeThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.weatherDataByAreaAndDate("XXXX", LocalDate.now()));
    }

    @Test
    void ensureDTOContainsCorrectData() {
        final List<WeatherDataDTO> results =
                controller.weatherDataByAreaAndDate(AREA_CODE,
                        LocalDate.of(2026, 6, 1));
        final WeatherDataDTO dto = results.get(0);
        assertEquals(AREA_CODE, dto.areaCode);
        assertEquals(270.0,     dto.windDirection, 0.001);
        assertEquals(15.5,      dto.windSpeed,     0.001);
        assertEquals(START,     dto.startDateTime);
        assertEquals(END,       dto.endDateTime);
    }

    @Test
    void ensureUnauthenticatedUserCannotConsultWeatherData() {
        AuthzRegistry.authorizationService().clearSession();
        assertThrows(UnauthenticatedException.class,
                () -> controller.weatherDataByAreaAndDate(AREA_CODE, LocalDate.now()));
    }

    // Helper

    private static void injectField(final Object target, final String fieldName, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
