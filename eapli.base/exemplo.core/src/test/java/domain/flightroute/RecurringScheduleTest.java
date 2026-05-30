// ─── domain/flightroute/RecurringScheduleTest.java ─────────────────────────

package domain.flightroute;

import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RecurringScheduleTest {

    private static final ScheduleEntry ENTRY = new ScheduleEntry(
            DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0));

    @Test
    void ensureValidScheduleIsCreated() {
        final RecurringSchedule rs = new RecurringSchedule(List.of(ENTRY));
        assertEquals(1, rs.entries().size());
    }

    @Test
    void ensureEmptyListIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new RecurringSchedule(Collections.emptyList()));
    }

    @Test
    void ensureNullListIsRejected() {
        assertThrows(Exception.class, () -> new RecurringSchedule(null));
    }

    @Test
    void ensureEntriesListIsUnmodifiable() {
        final RecurringSchedule rs = new RecurringSchedule(List.of(ENTRY));
        assertThrows(UnsupportedOperationException.class,
                () -> rs.entries().add(ENTRY));
    }

    @Test
    void ensureTwoSchedulesWithSameEntriesAreEqual() {
        assertEquals(
                new RecurringSchedule(List.of(ENTRY)),
                new RecurringSchedule(List.of(ENTRY)));
    }

    @Test
    void ensureTwoSchedulesWithDifferentEntriesAreNotEqual() {
        final ScheduleEntry other = new ScheduleEntry(
                DayOfWeek.FRIDAY, LocalTime.of(14, 0), LocalTime.of(15, 0));
        assertNotEquals(
                new RecurringSchedule(List.of(ENTRY)),
                new RecurringSchedule(List.of(other)));
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final RecurringSchedule rs = new RecurringSchedule(List.of(ENTRY));
        assertEquals(rs, rs);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        assertNotEquals(null, new RecurringSchedule(List.of(ENTRY)));
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        assertNotEquals(new RecurringSchedule(List.of(ENTRY)), "string");
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        assertEquals(
                new RecurringSchedule(List.of(ENTRY)).hashCode(),
                new RecurringSchedule(List.of(ENTRY)).hashCode());
    }

    @Test
    void ensureToStringIsNotBlank() {
        assertFalse(new RecurringSchedule(List.of(ENTRY)).toString().isBlank());
    }

    @Test
    void ensureMultipleEntriesAreStored() {
        final ScheduleEntry second = new ScheduleEntry(
                DayOfWeek.FRIDAY, LocalTime.of(14, 0), LocalTime.of(15, 0));
        final RecurringSchedule rs = new RecurringSchedule(List.of(ENTRY, second));
        assertEquals(2, rs.entries().size());
    }
}