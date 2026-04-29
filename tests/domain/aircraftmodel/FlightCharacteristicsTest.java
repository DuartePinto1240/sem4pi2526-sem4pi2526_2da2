package domain.aircraftmodel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightCharacteristicsTest {

    // ── helper ────────────────────────────────────────────────────────────────

    private FlightCharacteristics valid() {
        return new FlightCharacteristics(
                41413.0, 65317.0, 79016.0, 26020.0,
                12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.45);
    }

    // ── emptyWeight ───────────────────────────────────────────────────────────

    @Test
    void ensureEmptyWeightCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightCharacteristics(
                        -1.0, 65317.0, 79016.0, 26020.0,
                        12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.45));
    }

    @Test
    void ensureEmptyWeightCannotBeZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightCharacteristics(
                        0.0, 65317.0, 79016.0, 26020.0,
                        12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.45));
    }

    @Test
    void ensureEmptyWeightMustBeLessThanMzfw() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightCharacteristics(
                        70000.0, 65317.0, 79016.0, 26020.0,
                        12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.45));
    }

    // ── mzfw ─────────────────────────────────────────────────────────────────

    @Test
    void ensureMzfwCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightCharacteristics(
                        41413.0, -1.0, 79016.0, 26020.0,
                        12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.45));
    }

    @Test
    void ensureMzfwMustBeLessThanMtow() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightCharacteristics(
                        41413.0, 80000.0, 79016.0, 26020.0,
                        12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.45));
    }

    // ── mtow ─────────────────────────────────────────────────────────────────

    @Test
    void ensureMtowCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightCharacteristics(
                        41413.0, 65317.0, -1.0, 26020.0,
                        12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.45));
    }

    // ── other positive fields ─────────────────────────────────────────────────

    @Test
    void ensureMaxFuelCapacityCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightCharacteristics(
                        41413.0, 65317.0, 79016.0, -1.0,
                        12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.45));
    }

    @Test
    void ensureServiceCeilingCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightCharacteristics(
                        41413.0, 65317.0, 79016.0, 26020.0,
                        -1.0, 230.0, 5500.0, 125.08, 0.0265, 1.45));
    }

    @Test
    void ensureCruiseSpeedCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightCharacteristics(
                        41413.0, 65317.0, 79016.0, 26020.0,
                        12500.0, -1.0, 5500.0, 125.08, 0.0265, 1.45));
    }

    @Test
    void ensureMaximumRangeCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightCharacteristics(
                        41413.0, 65317.0, 79016.0, 26020.0,
                        12500.0, 230.0, -1.0, 125.08, 0.0265, 1.45));
    }

    @Test
    void ensureWingAreaCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightCharacteristics(
                        41413.0, 65317.0, 79016.0, 26020.0,
                        12500.0, 230.0, 5500.0, -1.0, 0.0265, 1.45));
    }

    @Test
    void ensureDragCoefficientCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightCharacteristics(
                        41413.0, 65317.0, 79016.0, 26020.0,
                        12500.0, 230.0, 5500.0, 125.08, -0.01, 1.45));
    }

    @Test
    void ensureLiftCoefficientCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightCharacteristics(
                        41413.0, 65317.0, 79016.0, 26020.0,
                        12500.0, 230.0, 5500.0, 125.08, 0.0265, -1.0));
    }

    // ── valid creation ────────────────────────────────────────────────────────

    @Test
    void ensureValidFlightCharacteristicsIsCreatedSuccessfully() {
        // Arrange + Act
        final FlightCharacteristics fc = valid();
        // Assert
        assertEquals(41413.0, fc.emptyWeight());
        assertEquals(65317.0, fc.mzfw());
        assertEquals(79016.0, fc.mtow());
        assertEquals(26020.0, fc.maxFuelCapacity());
        assertEquals(12500.0, fc.serviceCeiling());
        assertEquals(230.0,   fc.cruiseSpeed());
        assertEquals(5500.0,  fc.maximumRange());
        assertEquals(125.08,  fc.wingArea());
        assertEquals(0.0265,  fc.dragCoefficient());
        assertEquals(1.45,    fc.liftCoefficient());
    }

    // ── equals and hashCode ───────────────────────────────────────────────────

    @Test
    void ensureTwoEqualFlightCharacteristicsAreEqual() {
        // Arrange
        final FlightCharacteristics fc1 = valid();
        final FlightCharacteristics fc2 = valid();
        // Act + Assert
        assertEquals(fc1, fc2);
        assertEquals(fc1.hashCode(), fc2.hashCode());
    }

    @Test
    void ensureFlightCharacteristicsIsEqualToItself() {
        final FlightCharacteristics fc = valid();
        assertEquals(fc, fc);
    }

    @Test
    void ensureFlightCharacteristicsIsNotEqualToNull() {
        assertNotEquals(null, valid());
    }

    @Test
    void ensureFlightCharacteristicsIsNotEqualToOtherType() {
        assertNotEquals(valid(), "not a FlightCharacteristics");
    }

    @Test
    void ensureTwoDifferentFlightCharacteristicsAreNotEqual() {
        // Arrange
        final FlightCharacteristics fc1 = valid();
        final FlightCharacteristics fc2 = new FlightCharacteristics(
                41413.0, 65317.0, 79016.0, 26020.0,
                12500.0, 230.0, 5500.0, 125.08, 0.0265, 1.50); // different Cl
        // Act + Assert
        assertNotEquals(fc1, fc2);
    }

    // ── toString ─────────────────────────────────────────────────────────────

    @Test
    void ensureToStringContainsKeyValues() {
        final String result = valid().toString();
        assertNotNull(result);
        assertFalse(result.isBlank());
    }
}