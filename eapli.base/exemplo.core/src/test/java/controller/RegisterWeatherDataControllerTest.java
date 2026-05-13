package controller;

import domain.AirControlArea;
import domain.AirControlAreaCode;
import domain.GeographicBoundary;
import domain.WeatherData;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.infrastructure.authz.domain.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistence.impl.inmemory.InMemoryAirControlAreaRepository;
import persistence.impl.inmemory.InMemoryWeatherDataRepository;
import repositories.AirControlAreaRepository;
import repositories.WeatherDataRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RegisterWeatherDataController} (US041).
 *
 * <p>Uses in-memory repositories and hand-written {@link AuthorizationService}
 * stubs — zero external test libraries required beyond JUnit 5.</p>
 *
 * <p>Follows AAA convention (NFR03).</p>
 */
class RegisterWeatherDataControllerTest {

    // Test data
    private static final String AREA_CODE    = "LPPC";
    private static final double AREA_MIN_LAT = 36.0, AREA_MAX_LAT = 42.0;
    private static final double AREA_MIN_LON = -10.0, AREA_MAX_LON = -6.0;

    private static final double SUB_MIN_LAT  = 37.0, SUB_MAX_LAT  = 40.0;
    private static final double SUB_MIN_LON  = -9.0,  SUB_MAX_LON  = -7.0;

    private static final LocalDateTime START = LocalDateTime.of(2026, 5, 1,  8, 0);
    private static final LocalDateTime END   = LocalDateTime.of(2026, 5, 1, 12, 0);

    // SUT
    private AirControlAreaRepository areaRepo;
    private WeatherDataRepository    weatherRepo;
    private RegisterWeatherDataController controller;

    @BeforeEach
    void setUp() {
        areaRepo    = new InMemoryAirControlAreaRepository();
        weatherRepo = new InMemoryWeatherDataRepository();

        areaRepo.save(new AirControlArea(AREA_CODE, "Lisbon ACC",
                new GeographicBoundary(AREA_MIN_LAT, AREA_MAX_LAT,
                        AREA_MIN_LON, AREA_MAX_LON)));

        controller = new RegisterWeatherDataController(
                new PermissiveAuthz(), areaRepo, weatherRepo);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void ensureValidWeatherDataIsRegistered() {
        final WeatherData result = doRegister(controller);
        assertNotNull(result);
        assertEquals(new AirControlAreaCode(AREA_CODE), result.areaCode());
    }

    @Test
    void ensureRegisteredDataIsPersisted() {
        doRegister(controller);
        assertEquals(1L, weatherRepo.count());
    }

    @Test
    void ensureRegisteredDataReceivesAnId() {
        final WeatherData result = doRegister(controller);
        assertNotNull(result.identity());
    }

    @Test
    void ensureTwoNonOverlappingSubAreasCanBeRegisteredForSamePeriod() {
        controller.registerWeatherData(AREA_CODE,
                39.0, 42.0, -9.0, -7.0, START, END, 45.0, 5.0);

        final WeatherData second = controller.registerWeatherData(AREA_CODE,
                36.0, 39.0, -9.0, -7.0, START, END, 90.0, 8.0);

        assertNotNull(second);
        assertEquals(2L, weatherRepo.count());
    }

    @Test
    void ensureAdjacentPeriodsForSameSubAreaAreAllowed() {
        doRegister(controller);

        final WeatherData second = controller.registerWeatherData(AREA_CODE,
                SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                END, END.plusHours(4), 180.0, 5.0);

        assertNotNull(second);
        assertEquals(2L, weatherRepo.count());
    }

    @Test
    void ensureSubAreaCoveringFullParentBoundaryIsAccepted() {
        final WeatherData result = controller.registerWeatherData(AREA_CODE,
                AREA_MIN_LAT, AREA_MAX_LAT, AREA_MIN_LON, AREA_MAX_LON,
                START, END, 0.0, 0.0);
        assertNotNull(result);
    }

    @Test
    void ensureAllAreasReturnsRegisteredArea() {
        assertTrue(controller.allAreas().iterator().hasNext());
    }

    @Test
    void ensureExistingWeatherDataForAreaReturnsRegisteredRecord() {
        doRegister(controller);
        final Iterable<WeatherData> existing =
                controller.existingWeatherDataForArea(AREA_CODE);
        assertTrue(existing.iterator().hasNext());
    }

    @Test
    void ensureExistingWeatherDataForAreaReturnsEmptyWhenNoneRegistered() {
        final Iterable<WeatherData> existing =
                controller.existingWeatherDataForArea(AREA_CODE);
        assertFalse(existing.iterator().hasNext());
    }

    // Authorisation
    @Test
    void ensureUnauthorisedUserCannotRegisterWeatherData() {
        final RegisterWeatherDataController blocked =
                new RegisterWeatherDataController(
                        new BlockingAuthz(), areaRepo, weatherRepo);
        assertThrows(RuntimeException.class, () -> doRegister(blocked));
    }

    @Test
    void ensureUnauthorisedUserCannotListAreas() {
        final RegisterWeatherDataController blocked =
                new RegisterWeatherDataController(
                        new BlockingAuthz(), areaRepo, weatherRepo);
        assertThrows(RuntimeException.class, blocked::allAreas);
    }

    // Area must exist

    @Test
    void ensureNonExistentAreaIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData("XXXX",
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        START, END, 90.0, 10.5));
    }

    // Sub-area must be inside parent boundary

    @Test
    void ensureSubAreaBelowParentMinLatIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        20.0, 30.0, -9.0, -7.0, START, END, 90.0, 10.5));
    }

    @Test
    void ensureSubAreaAboveParentMaxLatIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        40.0, 44.0, -9.0, -7.0, START, END, 90.0, 10.5));
    }

    @Test
    void ensureSubAreaBeyondParentMinLonIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        37.0, 40.0, -11.0, -7.0, START, END, 90.0, 10.5));
    }

    @Test
    void ensureSubAreaBeyondParentMaxLonIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        37.0, 40.0, -9.0, -5.0, START, END, 90.0, 10.5));
    }

    // No temporal + spatial conflict

    @Test
    void ensureConflictingRecordIsRejected() {
        doRegister(controller);
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        START.plusHours(1), END.plusHours(2), 180.0, 5.0));
    }

    @Test
    void ensureNonOverlappingPeriodForSameSubAreaIsAllowed() {
        doRegister(controller);
        final WeatherData second = controller.registerWeatherData(AREA_CODE,
                SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                END.plusHours(2), END.plusHours(6), 180.0, 5.0);
        assertNotNull(second);
    }

    // Rule 5: WindCondition invariants

    @Test
    void ensureNegativeWindSpeedIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        START, END, 90.0, -0.1));
    }

    @Test
    void ensureWindDirection360IsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        START, END, 360.0, 10.0));
    }

    @Test
    void ensureNegativeWindDirectionIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        START, END, -1.0, 10.0));
    }

    // ObservationPeriod invariants

    @Test
    void ensureEndBeforeStartIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        END, START, 90.0, 10.0));
    }

    @Test
    void ensureEndEqualToStartIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        START, START, 90.0, 10.0));
    }

    // Helper

    private WeatherData doRegister(final RegisterWeatherDataController ctrl) {
        return ctrl.registerWeatherData(AREA_CODE,
                SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                START, END, 90.0, 10.5);
    }

    // ── AuthorizationService stubs — pure Java, zero external dependencies ────

    private static final class PermissiveAuthz extends AuthorizationService {
        @Override public void ensureAuthenticatedUserHasAnyOf(final Role... roles) { }
        @Override public boolean isAuthenticatedUserAuthorizedTo(final Role... roles) { return true; }
        @Override public Optional<UserSession> session() { return Optional.empty(); }
        @Override public boolean hasSession() { return true; }
    }

    private static final class BlockingAuthz extends AuthorizationService {
        @Override public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            throw new RuntimeException("Unauthorized: missing WEATHER_PERSON role");
        }
        @Override public boolean isAuthenticatedUserAuthorizedTo(final Role... roles) { return false; }
        @Override public Optional<UserSession> session() { return Optional.empty(); }
        @Override public boolean hasSession() { return false; }
    }
}
