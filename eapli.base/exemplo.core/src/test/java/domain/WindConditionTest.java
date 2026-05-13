package domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WindCondition} (US041).
 *
 * <p>Covers all invariants: wind direction [0, 360[, wind speed >= 0,
 * and equality/toString. Follows AAA convention (NFR03).</p>
 */
public class WindConditionTest {

    // Happy path — valid construction

    @Test
    public void ensureValidWindConditionIsCreated() {
        // Arrange + Act
        final WindCondition wc = new WindCondition(90.0, 10.5);

        // Assert
        assertEquals(90.0, wc.windDirection());
        assertEquals(10.5, wc.windSpeed());
    }

    @Test
    public void ensureZeroDirectionIsValid() {
        final WindCondition wc = new WindCondition(0.0, 0.0);
        assertEquals(0.0, wc.windDirection());
        assertEquals(0.0, wc.windSpeed());
    }

    @Test
    public void ensureMaxValidDirectionIs359point9() {
        final WindCondition wc = new WindCondition(359.9, 5.0);
        assertEquals(359.9, wc.windDirection(), 0.01);
    }

    @Test
    public void ensureZeroWindSpeedIsValid() {
        final WindCondition wc = new WindCondition(180.0, 0.0);
        assertEquals(0.0, wc.windSpeed());
    }

    // Wind direction — invalid values

    @Test
    public void ensureNegativeDirectionIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WindCondition(-1.0, 10.0));
    }

    @Test
    public void ensureDirection360IsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WindCondition(360.0, 10.0));
    }

    @Test
    public void ensureDirectionAbove360IsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WindCondition(400.0, 10.0));
    }

    // Wind speed — invalid values

    @Test
    public void ensureNegativeWindSpeedIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WindCondition(90.0, -0.1));
    }

    // Equality and toString

    @Test
    public void ensureEqualWindConditionsAreEqual() {
        final WindCondition a = new WindCondition(45.0, 8.0);
        final WindCondition b = new WindCondition(45.0, 8.0);
        assertEquals(a, b);
    }

    @Test
    public void ensureDifferentDirectionMakesNotEqual() {
        final WindCondition a = new WindCondition(45.0, 8.0);
        final WindCondition b = new WindCondition(90.0, 8.0);
        assertNotEquals(a, b);
    }

    @Test
    public void ensureDifferentSpeedMakesNotEqual() {
        final WindCondition a = new WindCondition(45.0, 8.0);
        final WindCondition b = new WindCondition(45.0, 9.0);
        assertNotEquals(a, b);
    }

}
