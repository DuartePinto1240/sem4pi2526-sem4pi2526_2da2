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

    /**
     * Produces a syntactically and semantically valid single-leg flight plan
     * with three segments (climb → cruise → descent).
     */
    private String validFlight(String id, String dep, String depTime,
                               String arr, String arrTime) {
        return "flight " + id + " {\n" +
                "    type: regular\n" +
                "    leg {\n" +
                "        departure: " + dep + " 2026-05-17 " + depTime + "\n" +
                "        arrival:   " + arr + " " + arrTime + "\n" +
                "        fuel:      5000 kg\n" +
                "        segment {\n" +
                "            phase: climb\n" +
                "            from: (41.2481, -8.6814)\n" +
                "            to:   (40.5000, -8.9000)\n" +
                "            altitude_range: [0, 10000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "        segment {\n" +
                "            phase: cruise\n" +
                "            from: (40.5000, -8.9000)\n" +
                "            to:   (39.5000, -9.1000)\n" +
                "            altitude_range: [8000, 10000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "        segment {\n" +
                "            phase: descent\n" +
                "            from: (39.5000, -9.1000)\n" +
                "            to:   (38.7813, -9.1359)\n" +
                "            altitude_range: [0, 10000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "    }\n" +
                "}";
    }

    /** Three-segment leg block for a given route. */
    private String threeSegLeg(String depAirport, String depDate, String depTime,
                               String arrAirport, String arrTime,
                               double fuel,
                               double lat1, double lon1,
                               double latMid, double lonMid,
                               double latMid2, double lonMid2,
                               double lat2, double lon2) {
        return "    leg {\n" +
                "        departure: " + depAirport + " " + depDate + " " + depTime + "\n" +
                "        arrival:   " + arrAirport + " " + arrTime + "\n" +
                "        fuel:      " + (int) fuel + " kg\n" +
                "        segment {\n" +
                "            phase: climb\n" +
                "            from: (" + lat1 + ", " + lon1 + ")\n" +
                "            to:   (" + latMid + ", " + lonMid + ")\n" +
                "            altitude_range: [0, 8000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "        segment {\n" +
                "            phase: cruise\n" +
                "            from: (" + latMid + ", " + lonMid + ")\n" +
                "            to:   (" + latMid2 + ", " + lonMid2 + ")\n" +
                "            altitude_range: [6000, 8000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "        segment {\n" +
                "            phase: descent\n" +
                "            from: (" + latMid2 + ", " + lonMid2 + ")\n" +
                "            to:   (" + lat2 + ", " + lon2 + ")\n" +
                "            altitude_range: [0, 8000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "    }\n";
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
        // Second leg departs from FAO, but first leg arrives at LIS — mismatch
        final String input =
                "flight TP003 {\n" +
                "    type: regular\n" +
                threeSegLeg("OPO", "2026-05-17", "08:00", "LIS", "09:00", 5000,
                        41.2481, -8.6814, 40.5, -8.9, 39.5, -9.1, 38.7813, -9.1359) +
                threeSegLeg("FAO", "2026-05-17", "10:00", "OPO", "11:00", 3000,
                        37.0144, -7.9659, 38.0, -8.3, 39.5, -8.6, 41.2481, -8.6814) +
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
        // Second leg departs from LIS — matches first leg arrival
        final String input =
                "flight TP004 {\n" +
                "    type: regular\n" +
                threeSegLeg("OPO", "2026-05-17", "08:00", "LIS", "09:00", 5000,
                        41.2481, -8.6814, 40.5, -8.9, 39.5, -9.1, 38.7813, -9.1359) +
                threeSegLeg("LIS", "2026-05-17", "10:00", "FAO", "11:00", 3000,
                        38.7813, -9.1359, 38.2, -8.8, 37.5, -8.3, 37.0144, -7.9659) +
                "}";

        final FlightPlanImporter.FlightPlanImportResult result =
                importer.importFromString(input);

        assertTrue(result.isSuccess());
    }

    // ── Rule 3: Leg arrival time < next departure time ────────────────────────

    @Test
    void ensureDepartureTimeMustBeAfterPreviousArrivalTime() {
        // First leg arrives 09:00, second leg departs 08:30 — overlap
        final String input =
                "flight TP005 {\n" +
                "    type: regular\n" +
                threeSegLeg("OPO", "2026-05-17", "08:00", "LIS", "09:00", 5000,
                        41.2481, -8.6814, 40.5, -8.9, 39.5, -9.1, 38.7813, -9.1359) +
                threeSegLeg("LIS", "2026-05-17", "08:30", "FAO", "10:00", 3000,
                        38.7813, -9.1359, 38.2, -8.8, 37.5, -8.3, 37.0144, -7.9659) +
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
        // Climb segment has identical from/to
        final String input =
                "flight TP008 {\n" +
                "    type: regular\n" +
                "    leg {\n" +
                "        departure: OPO 2026-05-17 14:00\n" +
                "        arrival:   LIS 15:00\n" +
                "        fuel:      5000 kg\n" +
                "        segment {\n" +
                "            phase: climb\n" +
                "            from: (41.2481, -8.6814)\n" +
                "            to:   (41.2481, -8.6814)\n" +  // same as from!
                "            altitude_range: [0, 10000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "        segment {\n" +
                "            phase: cruise\n" +
                "            from: (40.5000, -8.9000)\n" +
                "            to:   (39.5000, -9.1000)\n" +
                "            altitude_range: [8000, 10000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "        segment {\n" +
                "            phase: descent\n" +
                "            from: (39.5000, -9.1000)\n" +
                "            to:   (38.7813, -9.1359)\n" +
                "            altitude_range: [0, 10000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "    }\n" +
                "}";

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

    // ── Rule 7: Phase sequence (climb → cruise → descent) ────────────────────

    @Test
    void ensureLegWithFewerThanThreeSegmentsIsRejected() {
        // Only climb + descent — no cruise middle segment
        final String input =
                "flight TP014 {\n" +
                "    type: regular\n" +
                "    leg {\n" +
                "        departure: OPO 2026-05-17 14:00\n" +
                "        arrival:   LIS 15:00\n" +
                "        fuel:      5000 kg\n" +
                "        segment {\n" +
                "            phase: climb\n" +
                "            from: (41.2481, -8.6814)\n" +
                "            to:   (40.5000, -8.9000)\n" +
                "            altitude_range: [0, 10000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "        segment {\n" +
                "            phase: descent\n" +
                "            from: (40.5000, -8.9000)\n" +
                "            to:   (38.7813, -9.1359)\n" +
                "            altitude_range: [0, 10000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "    }\n" +
                "}";

        final FlightPlanImporter.FlightPlanImportResult result =
                importer.importFromString(input);

        assertFalse(result.isSuccess());
    }

    @Test
    void ensureFirstSegmentMustBeClimb() {
        // First segment is cruise, not climb
        final String input =
                "flight TP015 {\n" +
                "    type: regular\n" +
                "    leg {\n" +
                "        departure: OPO 2026-05-17 14:00\n" +
                "        arrival:   LIS 15:00\n" +
                "        fuel:      5000 kg\n" +
                "        segment {\n" +
                "            phase: cruise\n" +
                "            from: (41.2481, -8.6814)\n" +
                "            to:   (40.5000, -8.9000)\n" +
                "            altitude_range: [0, 10000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "        segment {\n" +
                "            phase: cruise\n" +
                "            from: (40.5000, -8.9000)\n" +
                "            to:   (39.5000, -9.1000)\n" +
                "            altitude_range: [8000, 10000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "        segment {\n" +
                "            phase: descent\n" +
                "            from: (39.5000, -9.1000)\n" +
                "            to:   (38.7813, -9.1359)\n" +
                "            altitude_range: [0, 10000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "    }\n" +
                "}";

        final FlightPlanImporter.FlightPlanImportResult result =
                importer.importFromString(input);

        assertFalse(result.isSuccess());
    }

    @Test
    void ensureLastSegmentMustBeDescent() {
        // Last segment is cruise, not descent
        final String input =
                "flight TP016 {\n" +
                "    type: regular\n" +
                "    leg {\n" +
                "        departure: OPO 2026-05-17 14:00\n" +
                "        arrival:   LIS 15:00\n" +
                "        fuel:      5000 kg\n" +
                "        segment {\n" +
                "            phase: climb\n" +
                "            from: (41.2481, -8.6814)\n" +
                "            to:   (40.5000, -8.9000)\n" +
                "            altitude_range: [0, 10000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "        segment {\n" +
                "            phase: cruise\n" +
                "            from: (40.5000, -8.9000)\n" +
                "            to:   (39.5000, -9.1000)\n" +
                "            altitude_range: [8000, 10000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "        segment {\n" +
                "            phase: cruise\n" +
                "            from: (39.5000, -9.1000)\n" +
                "            to:   (38.7813, -9.1359)\n" +
                "            altitude_range: [6000, 10000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "    }\n" +
                "}";

        final FlightPlanImporter.FlightPlanImportResult result =
                importer.importFromString(input);

        assertFalse(result.isSuccess());
    }

    // ── Multiple errors collected ─────────────────────────────────────────────

    @Test
    void ensureAllSemanticErrorsAreCollected() {
        // negative fuel + climb segment has same from/to → at least 2 semantic errors
        final String input =
                "flight TP012 {\n" +
                "    type: regular\n" +
                "    leg {\n" +
                "        departure: OPO 2026-05-17 14:00\n" +
                "        arrival:   LIS 15:00\n" +
                "        fuel:      -500 kg\n" +
                "        segment {\n" +
                "            phase: climb\n" +
                "            from: (41.2481, -8.6814)\n" +
                "            to:   (41.2481, -8.6814)\n" +
                "            altitude_range: [0, 10000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "        segment {\n" +
                "            phase: cruise\n" +
                "            from: (40.5000, -8.9000)\n" +
                "            to:   (39.5000, -9.1000)\n" +
                "            altitude_range: [8000, 10000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "        segment {\n" +
                "            phase: descent\n" +
                "            from: (39.5000, -9.1000)\n" +
                "            to:   (38.7813, -9.1359)\n" +
                "            altitude_range: [0, 10000]\n" +
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
