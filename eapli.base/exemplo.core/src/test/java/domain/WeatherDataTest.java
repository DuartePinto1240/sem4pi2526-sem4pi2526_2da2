package domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WeatherData} aggregate root (US041).
 *
 * <p>Tests construction invariants and sameAs semantics. Follows AAA (NFR03).</p>
 */
class WeatherDataTest {

    private static final AirControlAreaCode CODE = new AirControlAreaCode("LPPC");
    private static final GeographicBoundary BOUNDARY =
            new GeographicBoundary(37.0, 40.0, -9.0, -7.0);
    private static final WeatherSubArea     SUB  = new WeatherSubArea(BOUNDARY);
    private static final ObservationPeriod  PER  = new ObservationPeriod(
            LocalDateTime.of(2026, 5, 1, 8, 0),
            LocalDateTime.of(2026, 5, 1, 12, 0));
    private static final WindCondition      WIND = new WindCondition(90.0, 10.0);

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
        // Before JPA assigns an ID the identity is null — that is expected
        final WeatherData wd = new WeatherData(CODE, SUB, PER, WIND);
        assertNull(wd.identity());
    }

    // Null rejection

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
        final WeatherData a = new WeatherData(CODE, SUB, PER, WIND);
        final WeatherData b = new WeatherData(CODE, SUB, PER, WIND);
        assertTrue(a.sameAs(b));
    }

    @Test
    void ensureDifferentAreaCodeIsNotSameAs() {
        final WeatherData a = new WeatherData(CODE, SUB, PER, WIND);
        final WeatherData b = new WeatherData(new AirControlAreaCode("LECM"), SUB, PER, WIND);
        assertFalse(a.sameAs(b));
    }

    @Test
    void ensureNullIsNotSameAs() {
        assertFalse(new WeatherData(CODE, SUB, PER, WIND).sameAs(null));
    }

    @Test
    void ensureToStringContainsAreaCode() {
        assertTrue(new WeatherData(CODE, SUB, PER, WIND).toString().contains("LPPC"));
    }
}
