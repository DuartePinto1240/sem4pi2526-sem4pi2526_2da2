package lprog;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightLegTest {

    // ── helper ────────────────────────────────────────────────────────────────

    private FlightLeg validLeg() {
        return new FlightLeg("OPO", "2026-05-17", "14:00",
                "LIS", "15:00", 5000.0, "kg");
    }

    // ── departureAirport ──────────────────────────────────────────────────────

    @Test
    void ensureDepartureAirportCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightLeg(null, "2026-05-17", "14:00",
                        "LIS", "15:00", 5000.0, "kg"));
    }

    @Test
    void ensureDepartureAirportCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightLeg("", "2026-05-17", "14:00",
                        "LIS", "15:00", 5000.0, "kg"));
    }

    // ── departureDate ─────────────────────────────────────────────────────────

    @Test
    void ensureDepartureDateCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightLeg("OPO", null, "14:00",
                        "LIS", "15:00", 5000.0, "kg"));
    }

    @Test
    void ensureDepartureDateCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightLeg("OPO", "", "14:00",
                        "LIS", "15:00", 5000.0, "kg"));
    }

    // ── departureTime ─────────────────────────────────────────────────────────

    @Test
    void ensureDepartureTimeCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightLeg("OPO", "2026-05-17", null,
                        "LIS", "15:00", 5000.0, "kg"));
    }

    @Test
    void ensureDepartureTimeCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightLeg("OPO", "2026-05-17", "",
                        "LIS", "15:00", 5000.0, "kg"));
    }

    // ── arrivalAirport ────────────────────────────────────────────────────────

    @Test
    void ensureArrivalAirportCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightLeg("OPO", "2026-05-17", "14:00",
                        null, "15:00", 5000.0, "kg"));
    }

    @Test
    void ensureArrivalAirportCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightLeg("OPO", "2026-05-17", "14:00",
                        "", "15:00", 5000.0, "kg"));
    }

    // ── arrivalTime ───────────────────────────────────────────────────────────

    @Test
    void ensureArrivalTimeCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightLeg("OPO", "2026-05-17", "14:00",
                        "LIS", null, 5000.0, "kg"));
    }

    @Test
    void ensureArrivalTimeCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightLeg("OPO", "2026-05-17", "14:00",
                        "LIS", "", 5000.0, "kg"));
    }

    // ── fuelQuantity ──────────────────────────────────────────────────────────

    @Test
    void ensureFuelQuantityCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightLeg("OPO", "2026-05-17", "14:00",
                        "LIS", "15:00", -1.0, "kg"));
    }

    @Test
    void ensureFuelQuantityCannotBeZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightLeg("OPO", "2026-05-17", "14:00",
                        "LIS", "15:00", 0.0, "kg"));
    }

    // ── fuelUnit ──────────────────────────────────────────────────────────────

    @Test
    void ensureFuelUnitCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightLeg("OPO", "2026-05-17", "14:00",
                        "LIS", "15:00", 5000.0, null));
    }

    @Test
    void ensureFuelUnitCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightLeg("OPO", "2026-05-17", "14:00",
                        "LIS", "15:00", 5000.0, ""));
    }

    // ── valid creation ────────────────────────────────────────────────────────

    @Test
    void ensureValidFlightLegIsCreatedSuccessfully() {
        // Arrange + Act
        final FlightLeg leg = validLeg();
        // Assert
        assertEquals("OPO",        leg.departureAirport());
        assertEquals("2026-05-17", leg.departureDate());
        assertEquals("14:00",      leg.departureTime());
        assertEquals("LIS",        leg.arrivalAirport());
        assertEquals("15:00",      leg.arrivalTime());
        assertEquals(5000.0,       leg.fuelQuantity());
        assertEquals("kg",         leg.fuelUnit());
    }

    // ── toString ─────────────────────────────────────────────────────────────

    @Test
    void ensureToStringContainsKeyFields() {
        final String result = validLeg().toString();
        assertTrue(result.contains("OPO"));
        assertTrue(result.contains("LIS"));
    }
}