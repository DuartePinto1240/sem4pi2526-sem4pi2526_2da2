package controller;

import domain.flightroute.FlightRoute;
import domain.flightroute.ScheduleEntry;
import persistence.impl.inmemory.InMemoryAirportRepository;
import persistence.impl.inmemory.InMemoryFlightRouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.AirTransportCompanyRepository;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CreateFlightRouteControllerTest {

    private CreateFlightRouteController ctrl;

    private static final LocalDateTime DEP =
            LocalDateTime.of(2026, 6, 1, 10, 0);
    private static final LocalDateTime ARR =
            LocalDateTime.of(2026, 6, 1, 11, 0);
    private static final List<ScheduleEntry> ENTRIES = List.of(
            new ScheduleEntry(DayOfWeek.MONDAY,
                    LocalTime.of(10, 0), LocalTime.of(11, 0)));

    @BeforeEach
    void setUp() {
        ctrl = new CreateFlightRouteController(
                new InMemoryFlightRouteRepository(),
                new InMemoryAirportRepository());
    }

    // ── availableAirports ─────────────────────────────────────────────────

    @Test
    void ensureAvailableAirportsReturnsEmptyWhenNoneExist() {
        int count = 0;
        for (final Object ignored : ctrl.availableAirports()) count++;
        assertEquals(0, count);
    }

    // ── validateDesignatorPrefix ──────────────────────────────────────────

    @Test
    void ensureValidPrefixPassesValidation() {
        assertDoesNotThrow(() ->
                ctrl.validateDesignatorPrefix("TP123", "TP"));
    }

    @Test
    void ensureWrongPrefixIsRejected() {
        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () ->
                        ctrl.validateDesignatorPrefix("FR123", "TP"));
        assertTrue(ex.getMessage().contains("FR"));
        assertTrue(ex.getMessage().contains("TP"));
    }

    @Test
    void ensureNullDesignatorIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.validateDesignatorPrefix(null, "TP"));
    }

    @Test
    void ensureTooShortDesignatorIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.validateDesignatorPrefix("T", "TP"));
    }

    @Test
    void ensurePrefixValidationIsCaseInsensitive() {
        assertDoesNotThrow(() ->
                ctrl.validateDesignatorPrefix("tp123", "TP"));
    }

    // ── createCharterRoute ────────────────────────────────────────────────

    @Test
    void ensureValidCharterRouteIsCreated() {
        final FlightRoute r = ctrl.createCharterRoute(
                "TP001", "TP", "OPO", "LIS", DEP, ARR);
        assertNotNull(r);
        assertEquals("TP001", r.identity().value());
        assertTrue(r.flightType().isCharter());
        assertTrue(r.isActive());
    }

    @Test
    void ensureCharterRouteStoresCorrectData() {
        final FlightRoute r = ctrl.createCharterRoute(
                "TP002", "TP", "OPO", "LIS", DEP, ARR);
        assertEquals("OPO", r.originIata());
        assertEquals("LIS", r.destinationIata());
        assertEquals("TP",  r.companyIata());
        assertNotNull(r.scheduledDates());
        assertNull(r.recurringSchedule());
    }

    @Test
    void ensureDuplicateCharterDesignatorIsRejected() {
        final CreateFlightRouteController fresh =
                new CreateFlightRouteController(
                        new InMemoryFlightRouteRepository(),
                        new InMemoryAirportRepository());
        fresh.createCharterRoute("TP901", "TP", "OPO", "LIS", DEP, ARR);
        assertThrows(IllegalArgumentException.class, () ->
                fresh.createCharterRoute("TP901", "TP", "LIS", "FAO",
                        DEP, ARR));
    }

    @Test
    void ensureDuplicateCharterExceptionMessageContainsDesignator() {
        final CreateFlightRouteController fresh =
                new CreateFlightRouteController(
                        new InMemoryFlightRouteRepository(),
                        new InMemoryAirportRepository());
        fresh.createCharterRoute("TP902", "TP", "OPO", "LIS", DEP, ARR);
        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () ->
                        fresh.createCharterRoute("TP902", "TP", "LIS", "FAO",
                                DEP, ARR));
        assertTrue(ex.getMessage().contains("TP902"));
    }

    // ── createRegularRoute ────────────────────────────────────────────────

    @Test
    void ensureValidRegularRouteIsCreated() {
        final FlightRoute r = ctrl.createRegularRoute(
                "FR001", "FR", "OPO", "LIS", ENTRIES);
        assertNotNull(r);
        assertEquals("FR001", r.identity().value());
        assertTrue(r.flightType().isRegular());
        assertTrue(r.isActive());
    }

    @Test
    void ensureRegularRouteStoresSchedule() {
        final FlightRoute r = ctrl.createRegularRoute(
                "FR002", "FR", "OPO", "LIS", ENTRIES);
        assertNotNull(r.recurringSchedule());
        assertNull(r.scheduledDates());
        assertEquals(1, r.recurringSchedule().entries().size());
    }

    @Test
    void ensureDuplicateRegularDesignatorIsRejected() {
        final CreateFlightRouteController fresh =
                new CreateFlightRouteController(
                        new InMemoryFlightRouteRepository(),
                        new InMemoryAirportRepository());
        fresh.createRegularRoute("FR901", "FR", "OPO", "LIS", ENTRIES);
        assertThrows(IllegalArgumentException.class, () ->
                fresh.createRegularRoute("FR901", "FR", "LIS", "FAO",
                        ENTRIES));
    }

    @Test
    void ensureDuplicateRegularExceptionMessageContainsDesignator() {
        final CreateFlightRouteController fresh =
                new CreateFlightRouteController(
                        new InMemoryFlightRouteRepository(),
                        new InMemoryAirportRepository());
        fresh.createRegularRoute("FR902", "FR", "OPO", "LIS", ENTRIES);
        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () ->
                        fresh.createRegularRoute("FR902", "FR", "LIS", "FAO",
                                ENTRIES));
        assertTrue(ex.getMessage().contains("FR902"));
    }

    // ── mixed ─────────────────────────────────────────────────────────────

    @Test
    void ensureMultipleRoutesWithDifferentDesignatorsSucceed() {
        ctrl.createCharterRoute("TP003", "TP", "OPO", "LIS", DEP, ARR);
        final FlightRoute r2 = ctrl.createRegularRoute(
                "FR003", "FR", "OPO", "LIS", ENTRIES);
        assertNotNull(r2);
    }

    @Test
    void ensureCharterAndRegularWithSameDesignatorIsRejected() {
        final CreateFlightRouteController fresh =
                new CreateFlightRouteController(
                        new InMemoryFlightRouteRepository(),
                        new InMemoryAirportRepository());
        fresh.createCharterRoute("TP903", "TP", "OPO", "LIS", DEP, ARR);
        assertThrows(IllegalArgumentException.class, () ->
                fresh.createRegularRoute("TP903", "TP", "OPO", "LIS",
                        ENTRIES));
    }

    // ── domain validation via controller ─────────────────────────────────

    @Test
    void ensureSameOriginAndDestinationIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.createCharterRoute("TP004", "TP", "OPO", "OPO",
                        DEP, ARR));
    }

    @Test
    void ensureInvalidDesignatorFormatIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.createCharterRoute("T001", "TP", "OPO", "LIS",
                        DEP, ARR));
    }

    @Test
    void ensureEmptyCompanyIataIsRejected() {
        assertThrows(Exception.class, () ->
                ctrl.createCharterRoute("TP005", "", "OPO", "LIS",
                        DEP, ARR));
    }

    @Test
    void ensureArrivalBeforeDepartureIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.createCharterRoute("TP006", "TP", "OPO", "LIS",
                        ARR, DEP));
    }

    @Test
    void ensureEmptyScheduleEntriesIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.createRegularRoute("FR004", "FR", "OPO", "LIS",
                        List.of()));
    }

    @Test
    void ensureEmptyOriginIsRejected() {
        assertThrows(Exception.class, () ->
                ctrl.createCharterRoute("TP007", "TP", "", "LIS",
                        DEP, ARR));
    }

    @Test
    void ensureEmptyDestinationIsRejected() {
        assertThrows(Exception.class, () ->
                ctrl.createCharterRoute("TP008", "TP", "OPO", "",
                        DEP, ARR));
    }

    // ── currentUserCompanyIata ────────────────────────────────────────────

    @Test
    void ensureCurrentUserCompanyIataThrowsWhenNoSession() {
        final CreateFlightRouteController ctrlWithMock =
                new CreateFlightRouteController(
                        new InMemoryFlightRouteRepository(),
                        new InMemoryAirportRepository(),
                        mock(AirTransportCompanyRepository.class));
        assertThrows(Exception.class,
                ctrlWithMock::currentUserCompanyIata);
    }

    @Test
    void ensureCurrentUserCompanyIataThrowsWhenNoCompanyFound() {
        final AirTransportCompanyRepository mockRepo =
                mock(AirTransportCompanyRepository.class);
        when(mockRepo.findByCollaborator(anyString()))
                .thenReturn(Optional.empty());
        final CreateFlightRouteController ctrlWithMock =
                new CreateFlightRouteController(
                        new InMemoryFlightRouteRepository(),
                        new InMemoryAirportRepository(),
                        mockRepo);
        assertThrows(Exception.class,
                ctrlWithMock::currentUserCompanyIata);
    }
}