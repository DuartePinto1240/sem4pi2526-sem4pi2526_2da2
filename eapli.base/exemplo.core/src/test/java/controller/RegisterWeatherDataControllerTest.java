package controller;

import domain.AirControlArea;
import domain.AirControlAreaCode;
import domain.AlSafePasswordPolicy;
import domain.BaseRoles;
import domain.GeographicBoundary;
import domain.WeatherData;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RegisterWeatherDataController} (US041).
 */
class RegisterWeatherDataControllerTest {

    // Test data

    private static final String PERSISTENCE_UNIT = "eapli.base.AlSafePU";

    private static final String WP_EMAIL    = "wp.test@alsafe.com";
    private static final String WP_PASSWORD = "Weather1234";

    private static final String AREA_CODE    = "LPPC";
    private static final double AREA_MIN_LAT = 36.0, AREA_MAX_LAT = 42.0;
    private static final double AREA_MIN_LON = -10.0, AREA_MAX_LON = -6.0;

    private static final double SUB_MIN_LAT  = 37.0, SUB_MAX_LAT  = 40.0;
    private static final double SUB_MIN_LON  = -9.0,  SUB_MAX_LON  = -7.0;

    private static final double MIN_ALT = 0.0, MAX_ALT = 5000.0;

    private static final LocalDateTime START = LocalDateTime.of(2026, 5, 1,  8, 0);
    private static final LocalDateTime END   = LocalDateTime.of(2026, 5, 1, 12, 0);

    // SUT
    private RegisterWeatherDataController controller;
    private AirControlAreaRepository      areaRepo;
    private WeatherDataRepository         weatherRepo;

    @BeforeEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void setUp() throws Exception {

        // Configure AuthzRegistry (idempotent — safe to call multiple times)
        try {
            AuthzRegistry.configure(
                    new JpaAutoTxUserRepository(PERSISTENCE_UNIT, new HashMap<>()),
                    new AlSafePasswordPolicy(),
                    new PlainTextEncoder());
        } catch (final Exception ignored) {
            // already configured in a previous test — continue
        }

        // Reset PersistenceContext singleton so repos are fresh each test
        final Field instanceField =
                PersistenceContext.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        // Create fresh in-memory repos
        areaRepo    = new InMemoryAirControlAreaRepository();
        weatherRepo = new InMemoryWeatherDataRepository();

        // Inject in-memory repos into the fresh PersistenceContext
        final PersistenceContext ctx = PersistenceContext.getInstance();
        injectField(ctx, "airControlAreaRepository", areaRepo);
        injectField(ctx, "weatherDataRepository",    weatherRepo);

        // Clear any existing session
        AuthzRegistry.authorizationService().clearSession();

        // Create a WeatherPerson system user (ignored if already exists)
        try {
            final Set<Role> roles = new HashSet<>();
            roles.add(BaseRoles.WEATHER_PERSON);
            AuthzRegistry.userService().registerNewUser(
                    WP_EMAIL, WP_PASSWORD, "Alice", "Weather", WP_EMAIL, roles);
        } catch (final Exception ignored) {
            // user already registered from a previous test — continue
        }

        // Log in as WeatherPerson so authz checks pass
        new LoginController().login(WP_EMAIL, WP_PASSWORD);

        // Seed the area repo with one test area
        areaRepo.save(new AirControlArea(AREA_CODE, "Lisbon ACC",
                new GeographicBoundary(AREA_MIN_LAT, AREA_MAX_LAT,
                        AREA_MIN_LON, AREA_MAX_LON)));

        // Instantiate controller (uses PersistenceContext and AuthzRegistry)
        controller = new RegisterWeatherDataController();
    }

    // Happy path

    @Test
    void ensureValidWeatherDataIsRegistered() {
        // Act
        final WeatherData result = doRegister(controller);

        // Assert
        assertNotNull(result);
        assertEquals(new AirControlAreaCode(AREA_CODE), result.areaCode());
    }

    @Test
    void ensureRegisteredDataIsPersisted() {
        // Act
        doRegister(controller);

        // Assert
        assertEquals(1L, weatherRepo.count());
    }

    @Test
    void ensureRegisteredDataReceivesAnId() {
        // Act
        final WeatherData result = doRegister(controller);

        // Assert
        assertNotNull(result.identity());
    }

    @Test
    void ensureTwoNonOverlappingSubAreasCanBeRegisteredForSamePeriod() {
        // Arrange — northern half
        controller.registerWeatherData(AREA_CODE,
                39.0, 42.0, -9.0, -7.0, MIN_ALT, MAX_ALT, START, END, 45.0, 5.0);

        // Act — southern half, no geographic overlap
        final WeatherData second = controller.registerWeatherData(AREA_CODE,
                36.0, 39.0, -9.0, -7.0, MIN_ALT, MAX_ALT, START, END, 90.0, 8.0);

        // Assert
        assertNotNull(second);
        assertEquals(2L, weatherRepo.count());
    }

    @Test
    void ensureAdjacentPeriodsForSameSubAreaAreAllowed() {
        // Arrange
        doRegister(controller);

        // Act — period starts exactly where the first one ends
        final WeatherData second = controller.registerWeatherData(AREA_CODE,
                SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                MIN_ALT, MAX_ALT,
                END, END.plusHours(4), 180.0, 5.0);

        // Assert
        assertNotNull(second);
        assertEquals(2L, weatherRepo.count());
    }

    @Test
    void ensureSubAreaCoveringFullParentBoundaryIsAccepted() {
        // Act
        final WeatherData result = controller.registerWeatherData(AREA_CODE,
                AREA_MIN_LAT, AREA_MAX_LAT, AREA_MIN_LON, AREA_MAX_LON,
                MIN_ALT, MAX_ALT, START, END, 0.0, 0.0);

        // Assert
        assertNotNull(result);
    }

    @Test
    void ensureAllAreasReturnsRegisteredArea() {
        // Act
        final Iterable<AirControlArea> areas = controller.allAreas();

        // Assert
        assertTrue(areas.iterator().hasNext());
    }

    @Test
    void ensureExistingWeatherDataForAreaReturnsRegisteredRecord() {
        // Arrange
        doRegister(controller);

        // Act
        final Iterable<WeatherData> existing =
                controller.existingWeatherDataForArea(AREA_CODE);

        // Assert
        assertTrue(existing.iterator().hasNext());
    }

    @Test
    void ensureExistingWeatherDataForAreaReturnsEmptyWhenNoneRegistered() {
        // Act
        final Iterable<WeatherData> existing =
                controller.existingWeatherDataForArea(AREA_CODE);

        // Assert
        assertFalse(existing.iterator().hasNext());
    }

    // Referenced area must exist

    @Test
    void ensureNonExistentAreaIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData("XXXX",
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        MIN_ALT, MAX_ALT, START, END, 90.0, 10.5));
    }

    // Sub-area must be inside parent boundary

    @Test
    void ensureSubAreaBelowParentMinLatIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        20.0, 30.0, -9.0, -7.0,
                        MIN_ALT, MAX_ALT, START, END, 90.0, 10.5));
    }

    @Test
    void ensureSubAreaAboveParentMaxLatIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        40.0, 44.0, -9.0, -7.0,
                        MIN_ALT, MAX_ALT, START, END, 90.0, 10.5));
    }

    @Test
    void ensureSubAreaBeyondParentMinLonIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        37.0, 40.0, -11.0, -7.0,
                        MIN_ALT, MAX_ALT, START, END, 90.0, 10.5));
    }

    @Test
    void ensureSubAreaBeyondParentMaxLonIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        37.0, 40.0, -9.0, -5.0,
                        MIN_ALT, MAX_ALT, START, END, 90.0, 10.5));
    }

    // No geographic + altitude + temporal conflict

    @Test
    void ensureConflictingRecordIsRejected() {
        // Arrange — first registration succeeds
        doRegister(controller);

        // Act + Assert — same sub-area, same altitude, overlapping period
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        MIN_ALT, MAX_ALT,
                        START.plusHours(1), END.plusHours(2), 180.0, 5.0));
    }

    @Test
    void ensureNonOverlappingPeriodForSameSubAreaIsAllowed() {
        // Arrange
        doRegister(controller);

        // Act — period well after the first one ends
        final WeatherData second = controller.registerWeatherData(AREA_CODE,
                SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                MIN_ALT, MAX_ALT,
                END.plusHours(2), END.plusHours(6), 180.0, 5.0);

        // Assert
        assertNotNull(second);
    }

    @Test
    void ensureSameGeographyAndPeriodButAdjacentAltitudeLayerIsAllowed() {
        // Arrange — layer [0, 5000]
        doRegister(controller);

        // Act — layer [5000, 10000]: adjacent, not overlapping
        final WeatherData second = controller.registerWeatherData(AREA_CODE,
                SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                5000.0, 10000.0,
                START, END, 200.0, 15.0);

        // Assert
        assertNotNull(second);
        assertEquals(2L, weatherRepo.count());
    }

    @Test
    void ensureSameGeographyAndPeriodWithOverlappingAltitudeIsRejected() {
        // Arrange — layer [0, 5000]
        doRegister(controller);

        // Act + Assert — layer [3000, 8000] overlaps with [0, 5000]
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        3000.0, 8000.0,
                        START, END, 200.0, 15.0));
    }

    // WindCondition invariants

    @Test
    void ensureNegativeWindSpeedIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        MIN_ALT, MAX_ALT, START, END, 90.0, -0.1));
    }

    @Test
    void ensureWindDirection360IsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        MIN_ALT, MAX_ALT, START, END, 360.0, 10.0));
    }

    @Test
    void ensureNegativeWindDirectionIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        MIN_ALT, MAX_ALT, START, END, -1.0, 10.0));
    }

    // ObservationPeriod invariants

    @Test
    void ensureEndBeforeStartIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        MIN_ALT, MAX_ALT, END, START, 90.0, 10.0));
    }

    @Test
    void ensureEndEqualToStartIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        MIN_ALT, MAX_ALT, START, START, 90.0, 10.0));
    }

    // AltitudeRange invariants

    @Test
    void ensureNegativeMinAltitudeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        -100.0, MAX_ALT, START, END, 90.0, 10.0));
    }

    @Test
    void ensureMaxAltitudeAbove14000IsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        0.0, 15000.0, START, END, 90.0, 10.0));
    }

    @Test
    void ensureMaxAltitudeEqualToMinAltitudeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        5000.0, 5000.0, START, END, 90.0, 10.0));
    }

    // Authorisation

    @Test
    void ensureUnauthenticatedUserCannotRegisterWeatherData() {
        // Arrange — clear the session set up in @BeforeEach
        AuthzRegistry.authorizationService().clearSession();

        // Act + Assert — framework throws UnauthenticatedException
        assertThrows(UnauthenticatedException.class, () -> doRegister(controller));
    }

    @Test
    void ensureUnauthenticatedUserCannotListAreas() {
        // Arrange
        AuthzRegistry.authorizationService().clearSession();

        // Act + Assert
        assertThrows(UnauthenticatedException.class, () -> controller.allAreas());
    }

    // Helper

    private WeatherData doRegister(final RegisterWeatherDataController ctrl) {
        return ctrl.registerWeatherData(AREA_CODE,
                SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                MIN_ALT, MAX_ALT,
                START, END, 90.0, 10.5);
    }

    private static void injectField(final Object target,
                                     final String fieldName,
                                     final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
