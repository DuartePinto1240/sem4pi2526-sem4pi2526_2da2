package domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WeatherData} aggregate root (US041).
 */
class WeatherDataTest {

    // Shared fixtures
    private static final AirControlAreaCode CODE = new AirControlAreaCode("LPPC");

    private static final WeatherSubArea SUB = new WeatherSubArea(
            new GeographicBoundary(37.0, 40.0, -9.0, -7.0),
            new AltitudeRange(0.0, 5000.0));

    private static final ObservationPeriod PER = new ObservationPeriod(
            LocalDateTime.of(2026, 5, 1,  8, 0),
            LocalDateTime.of(2026, 5, 1, 12, 0));

    private static final WindCondition WIND = new WindCondition(90.0, 10.0);

    // Happy path
    @Test
    void ensureValidWeatherDataIsCreated() {
        final WeatherData wd = new WeatherData(CODE, SUB, PER, WIND);
        assertEquals(CODE, wd.areaCode());
        assertEquals(SUB,  wd.subArea());
        assertEquals(PER,  wd.period());
        assertEquals(WIND, wd.wind());
    }

    @Test
    void ensureIdentityIsNullBeforePersist() {
        // JPA assigns the id on first persist — before that it is null
        assertNull(new WeatherData(CODE, SUB, PER, WIND).identity());
    }

    //Null rejection

    @Test
    void ensureNullAreaCodeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData(null, SUB, PER, WIND));
    }

    @Test
    void ensureNullSubAreaIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData(CODE, null, PER, WIND));
    }

    @Test
    void ensureNullPeriodIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData(CODE, SUB, null, WIND));
    }

    @Test
    void ensureNullWindIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData(CODE, SUB, PER, null));
    }

    // sameAs

    @Test
    void ensureSameDataIsSameAs() {
        assertTrue(new WeatherData(CODE, SUB, PER, WIND)
                .sameAs(new WeatherData(CODE, SUB, PER, WIND)));
    }

    @Test
    void ensureDifferentAreaCodeIsNotSameAs() {
        assertFalse(new WeatherData(CODE, SUB, PER, WIND)
                .sameAs(new WeatherData(new AirControlAreaCode("LECM"), SUB, PER, WIND)));
    }

    @Test
    void ensureDifferentSubAreaIsNotSameAs() {
        final WeatherSubArea otherSub = new WeatherSubArea(
                new GeographicBoundary(37.0, 40.0, -9.0, -7.0),
                new AltitudeRange(5000.0, 10000.0)); // different altitude
        assertFalse(new WeatherData(CODE, SUB, PER, WIND)
                .sameAs(new WeatherData(CODE, otherSub, PER, WIND)));
    }

    @Test
    void ensureNullIsNotSameAs() {
        assertFalse(new WeatherData(CODE, SUB, PER, WIND).sameAs(null));
    }

    @Test
    void ensureNonWeatherDataIsNotSameAs() {
        assertFalse(new WeatherData(CODE, SUB, PER, WIND).sameAs("not a WeatherData"));
    }

    @Test
    void ensureToStringContainsAreaCode() {
        assertTrue(new WeatherData(CODE, SUB, PER, WIND).toString().contains("LPPC"));
    }
}
