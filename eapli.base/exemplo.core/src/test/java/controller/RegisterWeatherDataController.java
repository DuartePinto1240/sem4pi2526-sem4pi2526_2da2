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
 * <p>Uses in-memory repositories and hand-written stub implementations of
 * {@link AuthorizationService} — no external libraries (no Mockito, no
 * Spring, no framework test support) required beyond JUnit 5 itself.
 * This follows the same pattern used elsewhere in this project.</p>
 *
 * <p>Two stubs are provided as private static nested classes:</p>
 * <ul>
 *   <li>{@link PermissiveAuthz} — does nothing on role checks, simulating a
 *       user that has the required role.</li>
 *   <li>{@link BlockingAuthz} — throws {@link RuntimeException} on any role
 *       check, simulating a user that does NOT have the required role.</li>
 * </ul>
 *
 * <p>Follows the AAA (Arrange–Act–Assert) convention (NFR03).</p>
 */
class RegisterWeatherDataControllerTest {

    // ── Shared test data ──────────────────────────────────────────────────────

    /** Area code for the pre-loaded AirControlArea. */
    private static final String AREA_CODE = "LPPC";

    // Parent area boundary
    private static final double AREA_MIN_LAT = 36.0;
    private static final double AREA_MAX_LAT = 42.0;
    private static final double AREA_MIN_LON = -10.0;
    private static final double AREA_MAX_LON = -6.0;

    // Valid sub-area coordinates — fully inside LPPC
    private static final double SUB_MIN_LAT = 37.0;
    private static final double SUB_MAX_LAT = 40.0;
    private static final double SUB_MIN_LON = -9.0;
    private static final double SUB_MAX_LON = -7.0;

    // Time period
    private static final LocalDateTime START = LocalDateTime.of(2026, 5, 1,  8, 0);
    private static final LocalDateTime END   = LocalDateTime.of(2026, 5, 1, 12, 0);

    // ── SUT collaborators ─────────────────────────────────────────────────────

    private AirControlAreaRepository areaRepo;
    private WeatherDataRepository    weatherRepo;

    /** The controller under test — uses PermissiveAuthz by default. */
    private RegisterWeatherDataController controller;

    // ── Fixture ───────────────────────────────────────────────────────────────

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

    // =========================================================================
    // Happy path
    // =========================================================================

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
        doRegister(controller);
        assertEquals(1L, weatherRepo.count());
    }

    @Test
    void ensureTwoNonOverlappingSubAreasCanBeRegisteredForSamePeriod() {
        // Northern sub-area
        controller.registerWeatherData(AREA_CODE,
                39.0, 42.0, -9.0, -7.0,
                START, END, 45.0, 5.0);

        // Southern sub-area — no geographic overlap with the northern one
        final WeatherData second = controller.registerWeatherData(AREA_CODE,
                36.0, 39.0, -9.0, -7.0,
                START, END, 90.0, 8.0);

        assertNotNull(second);
        assertEquals(2L, weatherRepo.count());
    }

    @Test
    void ensureAdjacentPeriodsForSameSubAreaAreAllowed() {
        // Period 1: [START, END]
        doRegister(controller);

        // Period 2: [END, END+4h] — adjacent, not overlapping
        final WeatherData second = controller.registerWeatherData(AREA_CODE,
                SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                END, END.plusHours(4), 180.0, 5.0);

        assertNotNull(second);
        assertEquals(2L, weatherRepo.count());
    }

    @Test
    void ensureSubAreaCoveringFullParentBoundaryIsAccepted() {
        // A sub-area equal to the full parent boundary is the simplest valid case
        final WeatherData result = controller.registerWeatherData(AREA_CODE,
                AREA_MIN_LAT, AREA_MAX_LAT, AREA_MIN_LON, AREA_MAX_LON,
                START, END, 0.0, 0.0);

        assertNotNull(result);
    }

    @Test
    void ensureAllAreasReturnsAtLeastOneArea() {
        final Iterable<AirControlArea> areas = controller.allAreas();
        assertTrue(areas.iterator().hasNext());
    }

    // =========================================================================
    // Rule 1 — Authorisation
    // =========================================================================

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

    // =========================================================================
    // Rule 2 — Referenced area must exist
    // =========================================================================

    @Test
    void ensureNonExistentAreaIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData("XXXX",
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        START, END, 90.0, 10.5));
    }

    // =========================================================================
    // Rule 3 — Sub-area must be inside the parent boundary
    // =========================================================================

    @Test
    void ensureSubAreaBelowParentMinLatIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        20.0, 30.0, -9.0, -7.0,   // minLat=20 < LPPC.minLat=36
                        START, END, 90.0, 10.5));
    }

    @Test
    void ensureSubAreaAboveParentMaxLatIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        40.0, 44.0, -9.0, -7.0,   // maxLat=44 > LPPC.maxLat=42
                        START, END, 90.0, 10.5));
    }

    @Test
    void ensureSubAreaBeyondParentMinLonIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        37.0, 40.0, -11.0, -7.0,  // minLon=-11 < LPPC.minLon=-10
                        START, END, 90.0, 10.5));
    }

    @Test
    void ensureSubAreaBeyondParentMaxLonIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        37.0, 40.0, -9.0, -5.0,   // maxLon=-5 > LPPC.maxLon=-6
                        START, END, 90.0, 10.5));
    }

    // =========================================================================
    // Rule 4 — No temporal + spatial conflict
    // =========================================================================

    @Test
    void ensureConflictingRecordIsRejected() {
        // First registration succeeds
        doRegister(controller);

        // Same sub-area, overlapping period → conflict
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        START.plusHours(1), END.plusHours(2), 180.0, 5.0));
    }

    @Test
    void ensureNonOverlappingPeriodForSameSubAreaIsAllowed() {
        doRegister(controller);

        // Period starts well after the first one ends — no conflict
        final WeatherData second = controller.registerWeatherData(AREA_CODE,
                SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                END.plusHours(2), END.plusHours(6), 180.0, 5.0);

        assertNotNull(second);
    }

    // =========================================================================
    // Rule 5 — WindCondition invariants
    // =========================================================================

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

    // =========================================================================
    // Rule 6 — ObservationPeriod invariants
    // =========================================================================

    @Test
    void ensureEndBeforeStartIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        END, START,   // swapped
                        90.0, 10.0));
    }

    @Test
    void ensureEndEqualToStartIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.registerWeatherData(AREA_CODE,
                        SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                        START, START, 90.0, 10.0));
    }

    // =========================================================================
    // Helper
    // =========================================================================

    /** Registers a weather record with default valid data on the given controller. */
    private WeatherData doRegister(final RegisterWeatherDataController ctrl) {
        return ctrl.registerWeatherData(AREA_CODE,
                SUB_MIN_LAT, SUB_MAX_LAT, SUB_MIN_LON, SUB_MAX_LON,
                START, END, 90.0, 10.5);
    }

    // =========================================================================
    // AuthorizationService stubs — pure Java, zero external dependencies
    // =========================================================================

    /**
     * Stub that accepts any role check without complaint.
     * Simulates a user that IS authorised (has the WEATHER_PERSON role).
     */
    private static final class PermissiveAuthz extends AuthorizationService {

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            // authorised — do nothing
        }

        @Override
        public boolean isAuthenticatedUserAuthorizedTo(final Role... roles) {
            return true;
        }

        @Override
        public Optional<UserSession> session() {
            return Optional.empty();
        }

        @Override
        public boolean hasSession() {
            return true;
        }
    }

    /**
     * Stub that always throws on any role check.
     * Simulates a user that does NOT have the required role.
     */
    private static final class BlockingAuthz extends AuthorizationService {

        @Override
        public void ensureAuthenticatedUserHasAnyOf(final Role... roles) {
            throw new RuntimeException("Unauthorized: missing WEATHER_PERSON role");
        }

        @Override
        public boolean isAuthenticatedUserAuthorizedTo(final Role... roles) {
            return false;
        }

        @Override
        public Optional<UserSession> session() {
            return Optional.empty();
        }

        @Override
        public boolean hasSession() {
            return false;
        }
    }
}
