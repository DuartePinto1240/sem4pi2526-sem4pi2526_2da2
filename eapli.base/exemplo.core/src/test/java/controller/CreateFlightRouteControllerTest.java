package controller;

import domain.flightroute.FlightRoute;
import domain.flightroute.ScheduleEntry;
import persistence.impl.inmemory.InMemoryAirportRepository;
import persistence.impl.inmemory.InMemoryFlightRouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreateFlightRouteControllerTest {

    private CreateFlightRouteController ctrl;

    private static final LocalDateTime DEP = LocalDateTime.of(2026, 6, 1, 10, 0);
    private static final LocalDateTime ARR = LocalDateTime.of(2026, 6, 1, 11, 0);
    private static final List<ScheduleEntry> ENTRIES = List.of(
            new ScheduleEntry(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0)));

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




    // ── createRegularRoute ────────────────────────────────────────────────





    @Test
    void ensureDuplicateRegularExceptionMessageContainsDesignator() {
        ctrl.createRegularRoute("FR001", "FR", "OPO", "LIS", ENTRIES);
        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () ->
                        ctrl.createRegularRoute("FR001", "FR", "LIS", "FAO", ENTRIES));
        assertTrue(ex.getMessage().contains("FR001"));
    }

    // ── mixed ─────────────────────────────────────────────────────────────



    // ── domain validation via controller ─────────────────────────────────

    @Test
    void ensureSameOriginAndDestinationIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.createCharterRoute("TP002", "TP", "OPO", "OPO", DEP, ARR));
    }

    @Test
    void ensureInvalidDesignatorFormatIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.createCharterRoute("T001", "TP", "OPO", "LIS", DEP, ARR));
    }

    @Test
    void ensureEmptyCompanyIataIsRejected() {
        assertThrows(Exception.class, () ->
                ctrl.createCharterRoute("TP003", "", "OPO", "LIS", DEP, ARR));
    }

    @Test
    void ensureArrivalBeforeDepartureIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.createCharterRoute("TP004", "TP", "OPO", "LIS", ARR, DEP));
    }

    @Test
    void ensureEmptyScheduleEntriesIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ctrl.createRegularRoute("FR002", "FR", "OPO", "LIS", List.of()));
    }

    @Test
    void ensureEmptyOriginIsRejected() {
        assertThrows(Exception.class, () ->
                ctrl.createCharterRoute("TP005", "TP", "", "LIS", DEP, ARR));
    }

    @Test
    void ensureEmptyDestinationIsRejected() {
        assertThrows(Exception.class, () ->
                ctrl.createCharterRoute("TP006", "TP", "OPO", "", DEP, ARR));
    }
}