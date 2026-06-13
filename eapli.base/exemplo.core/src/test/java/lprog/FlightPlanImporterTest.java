package lprog;

import lprog.ErrorCollector.ErrorType;
import lprog.FlightPlanImporter.FlightPlanImportResult;
import lprog.FlightPlanBuilderListener.FlightData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlightPlanImporterTest {

    private final FlightPlanImporter importer = new FlightPlanImporter();

    // ── Shared DSL helpers ────────────────────────────────────────────────────

    /** Three-segment leg (climb/cruise/descent) from OPO to LIS. */
    private static String opoLisSeg() {
        return
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
            "        }\n";
    }

    private static final String SINGLE_SEGMENT_DSL =
            "flight TP100 {\n" +
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
            "            phase: descent\n" +
            "            from: (39.5000, -9.1000)\n" +
            "            to:   (38.7813, -9.1359)\n" +
            "            altitude_range: [0, 10000]\n" +
            "            width: 50 m\n" +
            "            wind:  270 deg 10 m/s\n" +
            "        }\n" +
            "    }\n" +
            "}";

    // ── Valid input ───────────────────────────────────────────────────────────

    @Test
    void validDslStringIsAccepted() {
        final String input =
                "flight TP001 {\n" +
                "    type: regular\n" +
                "    leg {\n" +
                "        departure: OPO 2026-05-17 14:00\n" +
                "        arrival:   LIS 15:00\n" +
                "        fuel:      5000 kg\n" +
                opoLisSeg() +
                "    }\n" +
                "}";

        final FlightPlanImportResult result = importer.importFromString(input);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getFlights().size());
        assertEquals("TP001", result.getFlights().get(0).id);
        assertEquals("regular", result.getFlights().get(0).type);
    }

    @Test
    void validDslProducesCorrectLegData() {
        final String input =
                "flight TP001 {\n" +
                "    type: charter\n" +
                "    leg {\n" +
                "        departure: OPO 2026-05-17 14:00\n" +
                "        arrival:   LIS 15:00\n" +
                "        fuel:      5000 kg\n" +
                opoLisSeg() +
                "    }\n" +
                "}";

        final FlightPlanImportResult result = importer.importFromString(input);

        assertTrue(result.isSuccess());
        final FlightData flight = result.getFlights().get(0);
        assertEquals(1, flight.legs.size());
        assertEquals("OPO", flight.legs.get(0).departureAirport);
        assertEquals("LIS", flight.legs.get(0).arrivalAirport);
        assertEquals(5000.0, flight.legs.get(0).fuelQuantity);
    }

    // ── Lexical errors ────────────────────────────────────────────────────────

    @Test
    void invalidTokenProducesLexicalError() {
        final String input =
                "flight TP002 {\n" +
                "    type: regular #invalid\n" +
                "    leg {\n" +
                "        departure: OPO 2026-05-17 14:00\n" +
                "        arrival:   LIS 15:00\n" +
                "        fuel:      5000 kg\n" +
                opoLisSeg() +
                "    }\n" +
                "}";

        final FlightPlanImportResult result = importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getType() == ErrorType.LEXICAL));
    }

    // ── Semantic errors ───────────────────────────────────────────────────────

    @Test
    void negativeFuelProducesSemanticError() {
        final String input =
                "flight TP003 {\n" +
                "    type: regular\n" +
                "    leg {\n" +
                "        departure: OPO 2026-05-17 14:00\n" +
                "        arrival:   LIS 15:00\n" +
                "        fuel:      -500 kg\n" +
                opoLisSeg() +
                "    }\n" +
                "}";

        final FlightPlanImportResult result = importer.importFromString(input);

        assertFalse(result.isSuccess());
    }

    @Test
    void duplicateFlightIdProducesSemanticError() {
        final String leg =
                "    leg {\n" +
                "        departure: OPO 2026-05-17 14:00\n" +
                "        arrival:   LIS 15:00\n" +
                "        fuel:      5000 kg\n" +
                opoLisSeg() +
                "    }\n";
        final String input =
                "flight TP001 {\n    type: regular\n" + leg + "}\n" +
                "flight TP001 {\n    type: charter\n" + leg + "}";

        final FlightPlanImportResult result = importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getType() == ErrorType.SEMANTIC
                        && e.getMessage().contains("Duplicate")));
    }

    @Test
    void legSequenceMismatchProducesSemanticError() {
        final String input =
                "flight TP004 {\n" +
                "    type: regular\n" +
                "    leg {\n" +
                "        departure: OPO 2026-05-17 08:00\n" +
                "        arrival:   LIS 09:00\n" +
                "        fuel:      5000 kg\n" +
                opoLisSeg() +
                "    }\n" +
                "    leg {\n" +
                "        departure: FAO 2026-05-17 10:00\n" +
                "        arrival:   OPO 11:00\n" +
                "        fuel:      3000 kg\n" +
                "        segment {\n" +
                "            phase: climb\n" +
                "            from: (37.0144, -7.9659)\n" +
                "            to:   (37.5000, -8.3000)\n" +
                "            altitude_range: [0, 8000]\n" +
                "            width: 50 m\n" +
                "            wind:  090 deg 10 m/s\n" +
                "        }\n" +
                "        segment {\n" +
                "            phase: cruise\n" +
                "            from: (37.5000, -8.3000)\n" +
                "            to:   (40.0000, -8.7000)\n" +
                "            altitude_range: [6000, 8000]\n" +
                "            width: 50 m\n" +
                "            wind:  090 deg 12 m/s\n" +
                "        }\n" +
                "        segment {\n" +
                "            phase: descent\n" +
                "            from: (40.0000, -8.7000)\n" +
                "            to:   (41.2481, -8.6814)\n" +
                "            altitude_range: [0, 8000]\n" +
                "            width: 50 m\n" +
                "            wind:  090 deg 8 m/s\n" +
                "        }\n" +
                "    }\n" +
                "}";

        final FlightPlanImportResult result = importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getType() == ErrorType.SEMANTIC
                        && e.getMessage().contains("does not match")));
    }

    @Test
    void allSemanticErrorsAreCollectedInSingleExecution() {
        // negative fuel + same from/to coordinates → multiple errors
        final String input =
                "flight TP005 {\n" +
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

        final FlightPlanImportResult result = importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().size() >= 2);
    }

    @Test
    void errorMessagesIncludeLineAndColumnInfo() {
        final String input =
                "flight TP006 {\n" +
                "    type: regular\n" +
                "    leg {\n" +
                "        departure: OPO 2026-05-17 14:00\n" +
                "        arrival:   LIS 15:00\n" +
                "        fuel:      -100 kg\n" +
                opoLisSeg() +
                "    }\n" +
                "}";

        final FlightPlanImportResult result = importer.importFromString(input);

        assertFalse(result.isSuccess());
        final ErrorCollector.ValidationError error = result.getErrors().get(0);
        assertTrue(error.getLine() > 0);
        assertTrue(error.getColumn() >= 0);
    }

    // ── Syntactic errors ──────────────────────────────────────────────────────

    @Test
    void missingClosingBraceProducesSyntacticError() {
        final String input =
                "flight TP007 {\n" +
                "    type: regular\n" +
                "    leg {\n" +
                "        departure: OPO 2026-05-17 14:00\n" +
                "        arrival:   LIS 15:00\n" +
                "        fuel:      5000 kg\n" +
                opoLisSeg() +
                "    }\n";
                // missing closing }

        final FlightPlanImportResult result = importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getType() == ErrorType.SYNTACTIC));
    }

    // ── rawSource preservation (US081) ────────────────────────────────────────

    @Test
    void ensureSuccessResultContainsRawSource() {
        final String input =
                "flight TP008 {\n" +
                "    type: regular\n" +
                "    leg {\n" +
                "        departure: OPO 2026-05-17 14:00\n" +
                "        arrival:   LIS 15:00\n" +
                "        fuel:      5000 kg\n" +
                opoLisSeg() +
                "    }\n" +
                "}";

        final FlightPlanImportResult result = importer.importFromString(input);

        assertTrue(result.isSuccess());
        assertNotNull(result.getRawSource());
        assertFalse(result.getRawSource().isEmpty());
    }

    @Test
    void ensureFailureResultHasNullRawSource() {
        final String input = "flight TP009 { type: regular }"; // missing leg

        final FlightPlanImportResult result = importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertNull(result.getRawSource());
    }

    // ── Segment data building ─────────────────────────────────────────────────

    @Test
    void ensureSegmentsAreBuiltForLeg() {
        final FlightPlanImportResult result = importer.importFromString(SINGLE_SEGMENT_DSL);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getFlights().get(0).legs.get(0).segments.size());
    }

    @Test
    void ensureSegmentFromCoordinatesAreCorrect() {
        final FlightPlanImportResult result = importer.importFromString(SINGLE_SEGMENT_DSL);
        final FlightPlanBuilderListener.SegmentData seg =
                result.getFlights().get(0).legs.get(0).segments.get(0); // climb segment
        assertEquals(41.2481,  seg.fromLat, 0.0001);
        assertEquals(-8.6814, seg.fromLon,  0.0001);
    }

    @Test
    void ensureSegmentToCoordinatesAreCorrect() {
        // climb segment ends at mid-point
        final FlightPlanImportResult result = importer.importFromString(SINGLE_SEGMENT_DSL);
        final FlightPlanBuilderListener.SegmentData descent =
                result.getFlights().get(0).legs.get(0).segments.get(2); // descent segment
        assertEquals(38.7813,  descent.toLat, 0.0001);
        assertEquals(-9.1359, descent.toLon,  0.0001);
    }

    @Test
    void ensureSegmentAltitudeRangeIsCorrect() {
        final FlightPlanImportResult result = importer.importFromString(SINGLE_SEGMENT_DSL);
        final FlightPlanBuilderListener.SegmentData cruise =
                result.getFlights().get(0).legs.get(0).segments.get(1); // cruise segment
        assertEquals(8000.0,  cruise.altitudeMin, 0.001);
        assertEquals(10000.0, cruise.altitudeMax, 0.001);
    }

    @Test
    void ensureSegmentPhaseIsCorrect() {
        final FlightPlanImportResult result = importer.importFromString(SINGLE_SEGMENT_DSL);
        final var segs = result.getFlights().get(0).legs.get(0).segments;
        assertEquals("climb",   segs.get(0).phase);
        assertEquals("cruise",  segs.get(1).phase);
        assertEquals("descent", segs.get(2).phase);
    }

    @Test
    void ensureSegmentWidthIsCorrect() {
        final FlightPlanImportResult result = importer.importFromString(SINGLE_SEGMENT_DSL);
        final FlightPlanBuilderListener.SegmentData seg =
                result.getFlights().get(0).legs.get(0).segments.get(1);
        assertEquals(50.0, seg.width, 0.001);
    }

    @Test
    void ensureSegmentWindAngleAndSpeedAreCorrect() {
        final FlightPlanImportResult result = importer.importFromString(SINGLE_SEGMENT_DSL);
        final FlightPlanBuilderListener.SegmentData seg =
                result.getFlights().get(0).legs.get(0).segments.get(1);
        assertEquals(270.0, seg.windAngle, 0.001);
        assertEquals(10.0,  seg.windSpeed, 0.001);
    }

    @Test
    void ensureMultiLegFlightBuildsSegmentsForEachLeg() {
        final String input =
                "flight TP103 {\n" +
                "    type: regular\n" +
                "    leg {\n" +
                "        departure: OPO 2026-05-17 07:00\n" +
                "        arrival:   LIS 08:00\n" +
                "        fuel:      4500 kg\n" +
                opoLisSeg() +
                "    }\n" +
                "    leg {\n" +
                "        departure: LIS 2026-05-17 09:00\n" +
                "        arrival:   FAO 09:45\n" +
                "        fuel:      3000 kg\n" +
                "        segment {\n" +
                "            phase: climb\n" +
                "            from: (38.7813, -9.1359)\n" +
                "            to:   (38.2000, -8.7000)\n" +
                "            altitude_range: [0, 8000]\n" +
                "            width: 40 m\n" +
                "            wind:  200 deg 8 m/s\n" +
                "        }\n" +
                "        segment {\n" +
                "            phase: cruise\n" +
                "            from: (38.2000, -8.7000)\n" +
                "            to:   (37.5000, -8.3000)\n" +
                "            altitude_range: [6000, 8000]\n" +
                "            width: 40 m\n" +
                "            wind:  200 deg 8 m/s\n" +
                "        }\n" +
                "        segment {\n" +
                "            phase: descent\n" +
                "            from: (37.5000, -8.3000)\n" +
                "            to:   (37.0144, -7.9659)\n" +
                "            altitude_range: [0, 8000]\n" +
                "            width: 40 m\n" +
                "            wind:  200 deg 6 m/s\n" +
                "        }\n" +
                "    }\n" +
                "}";
        final FlightPlanImportResult result = importer.importFromString(input);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getFlights().get(0).legs.size());
        assertEquals(3, result.getFlights().get(0).legs.get(0).segments.size());
        assertEquals(3, result.getFlights().get(0).legs.get(1).segments.size());
        final FlightPlanBuilderListener.SegmentData cruise1 =
                result.getFlights().get(0).legs.get(1).segments.get(1);
        assertEquals(38.2000, cruise1.fromLat, 0.0001);
        assertEquals(200.0,   cruise1.windAngle, 0.001);
        assertEquals(8.0,     cruise1.windSpeed,  0.001);
    }
}
