package domain.flightroute;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ScheduledDatesTest {

    private static final LocalDateTime DEP = LocalDateTime.of(2026, 6, 1, 10, 0);
    private static final LocalDateTime ARR = LocalDateTime.of(2026, 6, 1, 11, 0);

    @Test
    void ensureValidDatesAreCreated() {
        final ScheduledDates sd = new ScheduledDates(DEP, ARR);
        assertEquals(DEP, sd.departureDateTime());
        assertEquals(ARR, sd.scheduledArrivalDateTime());
    }

    @Test
    void ensureNullDepartureIsRejected() {
        assertThrows(Exception.class, () -> new ScheduledDates(null, ARR));
    }

    @Test
    void ensureNullArrivalIsRejected() {
        assertThrows(Exception.class, () -> new ScheduledDates(DEP, null));
    }

    @Test
    void ensureArrivalBeforeDepartureIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScheduledDates(ARR, DEP));
    }

    @Test
    void ensureArrivalEqualToDepartureIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScheduledDates(DEP, DEP));
    }

    @Test
    void ensureTwoDatesWithSameValuesAreEqual() {
        assertEquals(new ScheduledDates(DEP, ARR), new ScheduledDates(DEP, ARR));
    }

    @Test
    void ensureTwoDatesWithDifferentValuesAreNotEqual() {
        assertNotEquals(
                new ScheduledDates(DEP, ARR),
                new ScheduledDates(DEP, ARR.plusHours(1)));
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final ScheduledDates sd = new ScheduledDates(DEP, ARR);
        assertEquals(sd, sd);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        assertNotEquals(null, new ScheduledDates(DEP, ARR));
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        assertNotEquals(new ScheduledDates(DEP, ARR), "string");
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        assertEquals(
                new ScheduledDates(DEP, ARR).hashCode(),
                new ScheduledDates(DEP, ARR).hashCode());
    }

    @Test
    void ensureToStringIsNotBlank() {
        assertFalse(new ScheduledDates(DEP, ARR).toString().isBlank());
    }

    @Test
    void ensureProtectedNoArgConstructorExistsForJpa() throws Exception {
        final Constructor<ScheduledDates> ctor =
                ScheduledDates.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final ScheduledDates instance = ctor.newInstance();
        assertNotNull(instance);
    }
}