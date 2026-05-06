package domain;

import domain.AirControlAreaCode;
import domain.GeographicBoundary;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WeatherData} aggregate root (US041).
 *
 * <p>Tests construction, invariants, and identity/equality. Follows AAA (NFR03).</p>
 */
public class WeatherDataTest {

    private static final AirControlAreaCode CODE = new AirControlAreaCode("LPPC");
    private static final GeographicBoundary BOUNDARY =
            new GeographicBoundary(37.0, 40.0, -9.0, -7.0);
    private static final WeatherSubArea SUB_AREA = new WeatherSubArea(BOUNDARY);
    private static final ObservationPeriod PERIOD = new ObservationPeriod(
            LocalDateTime.of(2026, 5, 1, 8, 0),
            LocalDateTime.of(2026, 5, 1, 12, 0));
    private static final WindCondition WIND = new WindCondition(90.0, 10.0);

    // =========================================================================
    // Happy path
    // =========================================================================

    @Test
    public void ensureValidWeatherDataIsCreated() {
        final WeatherData wd = new WeatherData(CODE, SUB_AREA, PERIOD, WIND);

        assertEquals(CODE,     wd.areaCode());
        assertEquals(SUB_AREA, wd.subArea());
        assertEquals(PERIOD,   wd.period());
        assertEquals(WIND,     wd.wind());
    }

    // =========================================================================
    // Null rejection
    // =========================================================================

    @Test
    public void ensureNullAreaCodeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData(null, SUB_AREA, PERIOD, WIND));
    }

    @Test
    public void ensureNullSubAreaIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData(CODE, null, PERIOD, WIND));
    }

    @Test
    public void ensureNullPeriodIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData(CODE, SUB_AREA, null, WIND));
    }

    @Test
    public void ensureNullWindIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData(CODE, SUB_AREA, PERIOD, null));
    }

    // =========================================================================
    // sameAs
    // =========================================================================

    @Test
    public void ensureSameWeatherDataIsSameAs() {
        final WeatherData wd1 = new WeatherData(CODE, SUB_AREA, PERIOD, WIND);
        final WeatherData wd2 = new WeatherData(CODE, SUB_AREA, PERIOD, WIND);
        assertTrue(wd1.sameAs(wd2));
    }

    @Test
    public void ensureDifferentAreaCodeIsNotSameAs() {
        final WeatherData wd1 = new WeatherData(CODE, SUB_AREA, PERIOD, WIND);
        final WeatherData wd2 = new WeatherData(
                new AirControlAreaCode("LECM"), SUB_AREA, PERIOD, WIND);
        assertFalse(wd1.sameAs(wd2));
    }

    @Test
    public void ensureNullIsNotSameAs() {
        final WeatherData wd = new WeatherData(CODE, SUB_AREA, PERIOD, WIND);
        assertFalse(wd.sameAs(null));
    }

    @Test
    public void ensureToStringContainsAreaCode() {
        final WeatherData wd = new WeatherData(CODE, SUB_AREA, PERIOD, WIND);
        assertTrue(wd.toString().contains("LPPC"));
    }
}
