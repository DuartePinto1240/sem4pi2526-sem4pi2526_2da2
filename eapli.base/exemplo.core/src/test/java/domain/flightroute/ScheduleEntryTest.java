package domain.flightroute;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.time.DayOfWeek;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class ScheduleEntryTest {

    private static final DayOfWeek DAY = DayOfWeek.MONDAY;
    private static final LocalTime DEP = LocalTime.of(10, 0);
    private static final LocalTime ARR = LocalTime.of(11, 0);

    @Test
    void ensureValidEntryIsCreated() {
        final ScheduleEntry e = new ScheduleEntry(DAY, DEP, ARR);
        assertEquals(DAY, e.dayOfWeek());
        assertEquals(DEP, e.departureTime());
        assertEquals(ARR, e.scheduledArrivalTime());
    }

    @Test
    void ensureNullDayIsRejected() {
        assertThrows(Exception.class, () -> new ScheduleEntry(null, DEP, ARR));
    }

    @Test
    void ensureNullDepartureIsRejected() {
        assertThrows(Exception.class, () -> new ScheduleEntry(DAY, null, ARR));
    }

    @Test
    void ensureNullArrivalIsRejected() {
        assertThrows(Exception.class, () -> new ScheduleEntry(DAY, DEP, null));
    }

    @Test
    void ensureArrivalBeforeDepartureIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScheduleEntry(DAY, LocalTime.of(12, 0), LocalTime.of(11, 0)));
    }

    @Test
    void ensureArrivalEqualToDepartureIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScheduleEntry(DAY, DEP, DEP));
    }

    @Test
    void ensureTwoEntriesWithSameValuesAreEqual() {
        assertEquals(new ScheduleEntry(DAY, DEP, ARR), new ScheduleEntry(DAY, DEP, ARR));
    }

    @Test
    void ensureTwoEntriesWithDifferentDayAreNotEqual() {
        assertNotEquals(
                new ScheduleEntry(DayOfWeek.MONDAY, DEP, ARR),
                new ScheduleEntry(DayOfWeek.TUESDAY, DEP, ARR));
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final ScheduleEntry e = new ScheduleEntry(DAY, DEP, ARR);
        assertEquals(e, e);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        assertNotEquals(null, new ScheduleEntry(DAY, DEP, ARR));
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        assertNotEquals(new ScheduleEntry(DAY, DEP, ARR), "string");
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        assertEquals(
                new ScheduleEntry(DAY, DEP, ARR).hashCode(),
                new ScheduleEntry(DAY, DEP, ARR).hashCode());
    }

    @Test
    void ensureToStringIsNotBlank() {
        assertFalse(new ScheduleEntry(DAY, DEP, ARR).toString().isBlank());
    }

    @Test
    void ensureProtectedNoArgConstructorExistsForJpa() throws Exception {
        final Constructor<ScheduleEntry> ctor =
                ScheduleEntry.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final ScheduleEntry instance = ctor.newInstance();
        assertNotNull(instance);
    }
}