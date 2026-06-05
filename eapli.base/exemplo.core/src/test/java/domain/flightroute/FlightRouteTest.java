// ─── domain/flightroute/FlightRouteTest.java ───────────────────────────────

package domain.flightroute;

import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FlightRouteTest {

    private static final ScheduledDates DATES = new ScheduledDates(
            LocalDateTime.of(2026, 6, 1, 10, 0),
            LocalDateTime.of(2026, 6, 1, 11, 0));

    private static final RecurringSchedule SCHEDULE = new RecurringSchedule(List.of(
            new ScheduleEntry(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0))));

    // ── Charter ──────────────────────────────────────────────────────────

    @Test
    void ensureValidCharterRouteIsCreated() {
        final FlightRoute r = new FlightRoute("TP001", "TP", "OPO", "LIS", DATES);
        assertEquals("TP001", r.identity().value());
        assertTrue(r.flightType().isCharter());
        assertTrue(r.isActive());
        assertEquals("OPO", r.originIata());
        assertEquals("LIS", r.destinationIata());
        assertEquals("TP", r.companyIata());
        assertEquals(DATES, r.scheduledDates());
        assertNull(r.recurringSchedule());
    }

    @Test
    void ensureCharterRouteWithNullDatesIsRejected() {
        assertThrows(Exception.class,
                () -> new FlightRoute("TP001", "TP", "OPO", "LIS", (ScheduledDates) null));
    }

    // ── Regular ──────────────────────────────────────────────────────────

    @Test
    void ensureValidRegularRouteIsCreated() {
        final FlightRoute r = new FlightRoute("FR001", "FR", "OPO", "LIS", SCHEDULE);
        assertEquals("FR001", r.identity().value());
        assertTrue(r.flightType().isRegular());
        assertTrue(r.isActive());
        assertEquals(SCHEDULE, r.recurringSchedule());
        assertNull(r.scheduledDates());
    }

    @Test
    void ensureRegularRouteWithNullScheduleIsRejected() {
        assertThrows(Exception.class,
                () -> new FlightRoute("FR001", "FR", "OPO", "LIS", (RecurringSchedule) null));
    }

    // ── Validate ─────────────────────────────────────────────────────────

    @Test
    void ensureEmptyDesignatorIsRejected() {
        assertThrows(Exception.class,
                () -> new FlightRoute("", "TP", "OPO", "LIS", DATES));
    }

    @Test
    void ensureEmptyCompanyIataIsRejected() {
        assertThrows(Exception.class,
                () -> new FlightRoute("TP001", "", "OPO", "LIS", DATES));
    }

    @Test
    void ensureEmptyOriginIsRejected() {
        assertThrows(Exception.class,
                () -> new FlightRoute("TP001", "TP", "", "LIS", DATES));
    }

    @Test
    void ensureEmptyDestinationIsRejected() {
        assertThrows(Exception.class,
                () -> new FlightRoute("TP001", "TP", "OPO", "", DATES));
    }

    @Test
    void ensureSameOriginAndDestinationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightRoute("TP001", "TP", "OPO", "OPO", DATES));
    }

    @Test
    void ensureIataAndDesignatorAreStoredUppercase() {
        final FlightRoute r = new FlightRoute("tp001", "tp", "opo", "lis", DATES);
        assertEquals("TP001", r.identity().value());
        assertEquals("TP", r.companyIata());
        assertEquals("OPO", r.originIata());
        assertEquals("LIS", r.destinationIata());
    }

    // ── Deactivate ───────────────────────────────────────────────────────

    @Test
    void ensureDeactivationMakesRouteInactive() {
        final FlightRoute r = new FlightRoute("TP001", "TP", "OPO", "LIS", DATES);
        r.deactivate(LocalDate.now());
        assertFalse(r.isActive());
        assertEquals(LocalDate.now(), r.deactivationDate());
    }

    @Test
    void ensureDeactivatingAlreadyInactiveRouteThrows() {
        final FlightRoute r = new FlightRoute("TP001", "TP", "OPO", "LIS", DATES);
        r.deactivate(LocalDate.now());
        assertThrows(IllegalArgumentException.class,
                () -> r.deactivate(LocalDate.now()));
    }

    @Test
    void ensureDeactivationWithNullDateThrows() {
        final FlightRoute r = new FlightRoute("TP001", "TP", "OPO", "LIS", DATES);
        assertThrows(Exception.class, () -> r.deactivate(null));
    }

    // ── Identity / sameAs ────────────────────────────────────────────────

    @Test
    void ensureSameAsReturnsTrueForSameDesignator() {
        final FlightRoute r1 = new FlightRoute("TP001", "TP", "OPO", "LIS", DATES);
        final FlightRoute r2 = new FlightRoute("TP001", "FR", "LIS", "OPO", SCHEDULE);
        assertTrue(r1.sameAs(r2));
    }

    @Test
    void ensureSameAsReturnsFalseForDifferentDesignator() {
        final FlightRoute r1 = new FlightRoute("TP001", "TP", "OPO", "LIS", DATES);
        final FlightRoute r2 = new FlightRoute("FR002", "FR", "OPO", "LIS", DATES);
        assertFalse(r1.sameAs(r2));
    }

    @Test
    void ensureSameAsReturnsFalseForNonRoute() {
        final FlightRoute r = new FlightRoute("TP001", "TP", "OPO", "LIS", DATES);
        assertFalse(r.sameAs("not a route"));
    }

    @Test
    void ensureToStringContainsDesignator() {
        final FlightRoute r = new FlightRoute("TP001", "TP", "OPO", "LIS", DATES);
        assertTrue(r.toString().contains("TP001"));
    }

    @Test
    void ensureDeactivationDateIsNullForNewRoute() {
        final FlightRoute r = new FlightRoute("TP001", "TP", "OPO", "LIS", DATES);
        assertNull(r.deactivationDate());
    }
    @Test
    void ensureToStringContainsInactiveWhenDeactivated() {
        final FlightRoute r = new FlightRoute("TP001", "TP", "OPO", "LIS", DATES);
        r.deactivate(LocalDate.now());
        assertTrue(r.toString().contains("INACTIVE"));
    }
    @Test
    void ensureAllGettersReturnCorrectValues() {
        final FlightRoute r = new FlightRoute("TP001", "TP", "OPO", "LIS", DATES);
        assertNotNull(r.flightType());
        assertEquals("OPO", r.originIata());
        assertEquals("LIS", r.destinationIata());
        assertEquals("TP", r.companyIata());
        assertEquals(DATES, r.scheduledDates());
        assertNull(r.recurringSchedule());
        assertTrue(r.isActive());
        assertNull(r.deactivationDate());
    }

    @Test
    void ensureRegularRouteGettersReturnCorrectValues() {
        final FlightRoute r = new FlightRoute("FR001", "FR", "OPO", "LIS", SCHEDULE);
        assertNotNull(r.recurringSchedule());
        assertNull(r.scheduledDates());
        assertTrue(r.flightType().isRegular());
    }
    @Test
    void ensureProtectedNoArgConstructorExistsForJpa() throws Exception {
        final java.lang.reflect.Constructor<FlightRoute> ctor =
                FlightRoute.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final FlightRoute instance = ctor.newInstance();
        assertNotNull(instance);
    }
}