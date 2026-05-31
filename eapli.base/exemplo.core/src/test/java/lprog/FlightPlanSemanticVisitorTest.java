package lprog;

import lprog.ErrorCollector.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlightPlanSemanticVisitorTest {

    private FlightPlanImporter importer;

    @BeforeEach
    void setUp() {
        importer = new FlightPlanImporter();
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private String validFlight(String id, String dep, String depTime,
                               String arr, String arrTime) {
        return "flight " + id + " {\n" +
                "    type: regular\n" +
                "    leg {\n" +
                "        departure: " + dep + " 2026-05-17 " + depTime + "\n" +
                "        arrival:   " + arr + " " + arrTime + "\n" +
                "        fuel:      5000 kg\n" +
                "        segment {\n" +
                "            from: (41.2481, -8.6814)\n" +
                "            to:   (38.7813, -9.1359)\n" +
                "            altitude_slots: [8000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "    }\n" +
                "}";
    }

    // ── Rule 1: Flight ID unique ───────────────────────────────────────────────

    @Test
    void ensureDuplicateFlightIdIsRejected() {
        final String input =
                validFlight("TP001", "OPO", "14:00", "LIS", "15:00") + "\n" +
                        validFlight("TP001", "LIS", "16:00", "OPO", "17:00");

        final FlightPlanImporter.FlightPlanImportResult result =
                importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getType() == ErrorType.SEMANTIC
                        && e.getMessage().contains("Duplicate")));
    }

    @Test
    void ensureUniqueFlightIdsAreAccepted() {
        final String input =
                validFlight("TP001", "OPO", "14:00", "LIS", "15:00") + "\n" +
                        validFlight("TP002", "LIS", "16:00", "OPO", "17:00");

        final FlightPlanImporter.FlightPlanImportResult result =
                importer.importFromString(input);

        assertTrue(result.isSuccess());
    }

    // ── Rule 2: Leg arrival airport = next departure ──────────────────────────

    @Test
    void ensureLegArrivalMustMatchNextDeparture() {
        final String input =
                "flight TP003 {\n" +
                        "    type: regular\n" +
                        "    leg {\n" +
                        "        departure: OPO 2026-05-17 08:00\n" +
                        "        arrival:   LIS 09:00\n" +
                        "        fuel:      5000 kg\n" +
                        "        segment {\n" +
                        "            from: (41.2481, -8.6814)\n" +
                        "            to:   (38.7813, -9.1359)\n" +
                        "            altitude_slots: [8000]\n" +
                        "            width: 50 m\n" +
                        "            wind:  270 deg 10 m/s\n" +
                        "        }\n" +
                        "    }\n" +
                        "    leg {\n" +
                        "        departure: FAO 2026-05-17 10:00\n" +
                        "        arrival:   OPO 11:00\n" +
                        "        fuel:      3000 kg\n" +
                        "        segment {\n" +
                        "            from: (37.0144, -7.9659)\n" +
                        "            to:   (41.2481, -8.6814)\n" +
                        "            altitude_slots: [8000]\n" +
                        "            width: 50 m\n" +
                        "            wind:  90 deg 15 m/s\n" +
                        "        }\n" +
                        "    }\n" +
                        "}";

        final FlightPlanImporter.FlightPlanImportResult result =
                importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getType() == ErrorType.SEMANTIC
                        && e.getMessage().contains("does not match")));
    }

    @Test
    void ensureCorrectLegSequenceIsAccepted() {
        final String input =
                "flight TP004 {\n" +
                        "    type: regular\n" +
                        "    leg {\n" +
                        "        departure: OPO 2026-05-17 08:00\n" +
                        "        arrival:   LIS 09:00\n" +
                        "        fuel:      5000 kg\n" +
                        "        segment {\n" +
                        "            from: (41.2481, -8.6814)\n" +
                        "            to:   (38.7813, -9.1359)\n" +
                        "            altitude_slots: [8000]\n" +
                        "            width: 50 m\n" +
                        "            wind:  270 deg 10 m/s\n" +
                        "        }\n" +
                        "    }\n" +
                        "    leg {\n" +
                        "        departure: LIS 2026-05-17 10:00\n" +
                        "        arrival:   FAO 11:00\n" +
                        "        fuel:      3000 kg\n" +
                        "        segment {\n" +
                        "            from: (38.7813, -9.1359)\n" +
                        "            to:   (37.0144, -7.9659)\n" +
                        "            altitude_slots: [8000]\n" +
                        "            width: 50 m\n" +
                        "            wind:  90 deg 15 m/s\n" +
                        "        }\n" +
                        "    }\n" +
                        "}";

        final FlightPlanImporter.FlightPlanImportResult result =
                importer.importFromString(input);

        assertTrue(result.isSuccess());
    }

    // ── Rule 3: Leg arrival time < next departure time ────────────────────────

    @Test
    void ensureDepartureTimeMustBeAfterPreviousArrivalTime() {
        final String input =
                "flight TP005 {\n" +
                        "    type: regular\n" +
                        "    leg {\n" +
                        "        departure: OPO 2026-05-17 08:00\n" +
                        "        arrival:   LIS 09:00\n" +
                        "        fuel:      5000 kg\n" +
                        "        segment {\n" +
                        "            from: (41.2481, -8.6814)\n" +
                        "            to:   (38.7813, -9.1359)\n" +
                        "            altitude_slots: [8000]\n" +
                        "            width: 50 m\n" +
                        "            wind:  270 deg 10 m/s\n" +
                        "        }\n" +
                        "    }\n" +
                        "    leg {\n" +
                        "        departure: LIS 2026-05-17 08:30\n" +
                        "        arrival:   FAO 10:00\n" +
                        "        fuel:      3000 kg\n" +
                        "        segment {\n" +
                        "            from: (38.7813, -9.1359)\n" +
                        "            to:   (37.0144, -7.9659)\n" +
                        "            altitude_slots: [8000]\n" +
                        "            width: 50 m\n" +
                        "            wind:  90 deg 15 m/s\n" +
                        "        }\n" +
                        "    }\n" +
                        "}";

        final FlightPlanImporter.FlightPlanImportResult result =
                importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getType() == ErrorType.SEMANTIC
                        && e.getMessage().contains("after arrival time")));
    }

    // ── Rule 4: Fuel > 0 ─────────────────────────────────────────────────────

    @Test
    void ensureNegativeFuelIsRejected() {
        final String input = validFlight("TP006", "OPO", "14:00", "LIS", "15:00")
                .replace("fuel:      5000 kg", "fuel:      -500 kg");

        final FlightPlanImporter.FlightPlanImportResult result =
                importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getType() == ErrorType.SEMANTIC
                        && e.getMessage().contains("positive")));
    }

    @Test
    void ensureZeroFuelIsRejected() {
        final String input = validFlight("TP007", "OPO", "14:00", "LIS", "15:00")
                .replace("fuel:      5000 kg", "fuel:      0 kg");

        final FlightPlanImporter.FlightPlanImportResult result =
                importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getType() == ErrorType.SEMANTIC));
    }

    // ── Rule 5: Segment from ≠ to ─────────────────────────────────────────────

    @Test
    void ensureSegmentWithSameFromAndToIsRejected() {
        final String input = validFlight("TP008", "OPO", "14:00", "LIS", "15:00")
                .replace("to:   (38.7813, -9.1359)", "to:   (41.2481, -8.6814)");

        final FlightPlanImporter.FlightPlanImportResult result =
                importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getType() == ErrorType.SEMANTIC
                        && e.getMessage().contains("different")));
    }

    // ── Rule 6: Airport code IATA/ICAO ───────────────────────────────────────

    @Test
    void ensureInvalidAirportCodeIsRejected() {
        final String input = validFlight("TP009", "OPO", "14:00", "LIS", "15:00")
                .replace("departure: OPO", "departure: INVALID123");

        final FlightPlanImporter.FlightPlanImportResult result =
                importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getType() == ErrorType.SEMANTIC
                        || e.getType() == ErrorType.SYNTACTIC));
    }

    @Test
    void ensureIataCodeThreeLettersIsAccepted() {
        final FlightPlanImporter.FlightPlanImportResult result =
                importer.importFromString(
                        validFlight("TP010", "OPO", "14:00", "LIS", "15:00"));

        assertTrue(result.isSuccess());
    }

    @Test
    void ensureIcaoCodeFourLettersIsAccepted() {
        final FlightPlanImporter.FlightPlanImportResult result =
                importer.importFromString(
                        validFlight("TP011", "LPPR", "14:00", "LPPT", "15:00"));

        assertTrue(result.isSuccess());
    }

    // ── Multiple errors collected ─────────────────────────────────────────────

    @Test
    void ensureAllSemanticErrorsAreCollected() {
        final String input =
                "flight TP012 {\n" +
                        "    type: regular\n" +
                        "    leg {\n" +
                        "        departure: OPO 2026-05-17 14:00\n" +
                        "        arrival:   LIS 15:00\n" +
                        "        fuel:      -500 kg\n" +
                        "        segment {\n" +
                        "            from: (41.2481, -8.6814)\n" +
                        "            to:   (41.2481, -8.6814)\n" +
                        "            altitude_slots: [8000]\n" +
                        "            width: 50 m\n" +
                        "            wind:  270 deg 10 m/s\n" +
                        "        }\n" +
                        "    }\n" +
                        "}";

        final FlightPlanImporter.FlightPlanImportResult result =
                importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().size() >= 2,
                "All semantic errors must be collected in one pass");
    }

    // ── Error location info ───────────────────────────────────────────────────

    @Test
    void ensureSemanticErrorIncludesLineInfo() {
        final String input = validFlight("TP013", "OPO", "14:00", "LIS", "15:00")
                .replace("fuel:      5000 kg", "fuel:      -1 kg");

        final FlightPlanImporter.FlightPlanImportResult result =
                importer.importFromString(input);

        assertFalse(result.isSuccess());
        final ErrorCollector.ValidationError err = result.getErrors().stream()
                .filter(e -> e.getType() == ErrorType.SEMANTIC)
                .findFirst()
                .orElseThrow();

        assertTrue(err.getLine() > 0);
        assertTrue(err.getColumn() >= 0);
        assertNotNull(err.getMessage());
        assertFalse(err.getMessage().isBlank());
    }
}