package lprog;

import lprog.ErrorCollector.ErrorType;
import lprog.FlightPlanImporter.FlightPlanImportResult;
import lprog.FlightPlanBuilderListener.FlightData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlightPlanImporterTest {

    private final FlightPlanImporter importer = new FlightPlanImporter();

    // ─────────────────────────────────────────
    // Valid input
    // ─────────────────────────────────────────

    @Test
    void validDslStringIsAccepted() {
        String input =
                "flight TP001 {\n" +
                        "    type: regular\n" +
                        "    leg {\n" +
                        "        departure: OPO 2026-05-17 14:00\n" +
                        "        arrival:   LIS 15:00\n" +
                        "        fuel:      5000 kg\n" +
                        "        segment {\n" +
                        "            from: (41.2481, -8.6814)\n" +
                        "            to:   (38.7813, -9.1359)\n" +
                        "            altitude_slots: [8000, 10000]\n" +
                        "            width: 50 m\n" +
                        "            wind:  270 deg 10 m/s\n" +
                        "        }\n" +
                        "    }\n" +
                        "}";

        FlightPlanImportResult result = importer.importFromString(input);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getFlights().size());
        assertEquals("TP001", result.getFlights().get(0).id);
        assertEquals("regular", result.getFlights().get(0).type);
    }

    @Test
    void validDslProducesCorrectLegData() {
        String input =
                "flight TP001 {\n" +
                        "    type: charter\n" +
                        "    leg {\n" +
                        "        departure: OPO 2026-05-17 14:00\n" +
                        "        arrival:   LIS 15:00\n" +
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

        FlightPlanImportResult result = importer.importFromString(input);

        assertTrue(result.isSuccess());
        FlightData flight = result.getFlights().get(0);
        assertEquals(1, flight.legs.size());
        assertEquals("OPO", flight.legs.get(0).departureAirport);
        assertEquals("LIS", flight.legs.get(0).arrivalAirport);
        assertEquals(5000.0, flight.legs.get(0).fuelQuantity);
    }

    // ─────────────────────────────────────────
    // Lexical errors
    // ─────────────────────────────────────────

    @Test
    void invalidTokenProducesLexicalError() {
        String input =
                "flight TP002 {\n" +
                        "    type: regular #invalid\n" +
                        "    leg {\n" +
                        "        departure: OPO 2026-05-17 14:00\n" +
                        "        arrival:   LIS 15:00\n" +
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

        FlightPlanImportResult result = importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getType() == ErrorType.LEXICAL));
    }

    // ─────────────────────────────────────────
    // Semantic errors
    // ─────────────────────────────────────────

    @Test
    void negativeFuelProducesSemanticError() {
        String input =
                "flight TP003 {\n" +
                        "    type: regular\n" +
                        "    leg {\n" +
                        "        departure: OPO 2026-05-17 14:00\n" +
                        "        arrival:   LIS 15:00\n" +
                        "        fuel:      -500 kg\n" +
                        "        segment {\n" +
                        "            from: (41.2481, -8.6814)\n" +
                        "            to:   (38.7813, -9.1359)\n" +
                        "            altitude_slots: [8000]\n" +
                        "            width: 50 m\n" +
                        "            wind:  270 deg 10 m/s\n" +
                        "        }\n" +
                        "    }\n" +
                        "}";

        FlightPlanImportResult result = importer.importFromString(input);

        // debug — ver que erros foram gerados
        result.getErrors().forEach(System.out::println);

        assertFalse(result.isSuccess());
    }

    @Test
    void duplicateFlightIdProducesSemanticError() {
        String input =
                "flight TP001 {\n" +
                        "    type: regular\n" +
                        "    leg {\n" +
                        "        departure: OPO 2026-05-17 14:00\n" +
                        "        arrival:   LIS 15:00\n" +
                        "        fuel:      5000 kg\n" +
                        "        segment {\n" +
                        "            from: (41.2481, -8.6814)\n" +
                        "            to:   (38.7813, -9.1359)\n" +
                        "            altitude_slots: [8000]\n" +
                        "            width: 50 m\n" +
                        "            wind:  270 deg 10 m/s\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n" +
                        "flight TP001 {\n" +
                        "    type: charter\n" +
                        "    leg {\n" +
                        "        departure: LIS 2026-05-17 16:00\n" +
                        "        arrival:   OPO 17:00\n" +
                        "        fuel:      4000 kg\n" +
                        "        segment {\n" +
                        "            from: (38.7813, -9.1359)\n" +
                        "            to:   (41.2481, -8.6814)\n" +
                        "            altitude_slots: [8000]\n" +
                        "            width: 50 m\n" +
                        "            wind:  90 deg 10 m/s\n" +
                        "        }\n" +
                        "    }\n" +
                        "}";

        FlightPlanImportResult result = importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getType() == ErrorType.SEMANTIC
                        && e.getMessage().contains("Duplicate")));
    }

    @Test
    void legSequenceMismatchProducesSemanticError() {
        String input =
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

        FlightPlanImportResult result = importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getType() == ErrorType.SEMANTIC
                        && e.getMessage().contains("does not match")));
    }

    @Test
    void allSemanticErrorsAreCollectedInSingleExecution() {
        String input =
                "flight TP005 {\n" +
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

        FlightPlanImportResult result = importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().size() >= 2);
    }

    @Test
    void errorMessagesIncludeLineAndColumnInfo() {
        String input =
                "flight TP006 {\n" +
                        "    type: regular\n" +
                        "    leg {\n" +
                        "        departure: OPO 2026-05-17 14:00\n" +
                        "        arrival:   LIS 15:00\n" +
                        "        fuel:      -100 kg\n" +
                        "        segment {\n" +
                        "            from: (41.2481, -8.6814)\n" +
                        "            to:   (38.7813, -9.1359)\n" +
                        "            altitude_slots: [8000]\n" +
                        "            width: 50 m\n" +
                        "            wind:  270 deg 10 m/s\n" +
                        "        }\n" +
                        "    }\n" +
                        "}";

        FlightPlanImportResult result = importer.importFromString(input);

        assertFalse(result.isSuccess());
        ErrorCollector.ValidationError error = result.getErrors().get(0);
        assertTrue(error.getLine() > 0);
        assertTrue(error.getColumn() >= 0);
    }

    // ─────────────────────────────────────────
    // ─────────────────────────────────────────
    // Syntactic errors
    // ─────────────────────────────────────────

    @Test
    void missingClosingBraceProducesSyntacticError() {
        String input =
                "flight TP007 {\n" +
                        "    type: regular\n" +
                        "    leg {\n" +
                        "        departure: OPO 2026-05-17 14:00\n" +
                        "        arrival:   LIS 15:00\n" +
                        "        fuel:      5000 kg\n" +
                        "        segment {\n" +
                        "            from: (41.2481, -8.6814)\n" +
                        "            to:   (38.7813, -9.1359)\n" +
                        "            altitude_slots: [8000]\n" +
                        "            width: 50 m\n" +
                        "            wind:  270 deg 10 m/s\n" +
                        "        }\n" +
                        "    }\n";
                        // missing closing }

        FlightPlanImportResult result = importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getType() == ErrorType.SYNTACTIC));
    }

    // ─────────────────────────────────────────
    // rawSource preservation (US081)
    // ─────────────────────────────────────────

    @Test
    void ensureSuccessResultContainsRawSource() {
        String input =
                "flight TP008 {\n" +
                        "    type: regular\n" +
                        "    leg {\n" +
                        "        departure: OPO 2026-05-17 14:00\n" +
                        "        arrival:   LIS 15:00\n" +
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

        FlightPlanImportResult result = importer.importFromString(input);

        assertTrue(result.isSuccess());
        assertNotNull(result.getRawSource());
        assertFalse(result.getRawSource().isEmpty());
    }

    @Test
    void ensureFailureResultHasNullRawSource() {
        String input = "flight TP009 { type: regular }"; // missing leg

        FlightPlanImportResult result = importer.importFromString(input);

        assertFalse(result.isSuccess());
        assertNull(result.getRawSource());
    }

    // ─────────────────────────────────────────
    // Segment data building (US121)
    // ─────────────────────────────────────────

    private static final String SINGLE_SEGMENT_DSL =
            "flight TP100 {\n" +
            "    type: regular\n" +
            "    leg {\n" +
            "        departure: OPO 2026-05-17 14:00\n" +
            "        arrival:   LIS 15:00\n" +
            "        fuel:      5000 kg\n" +
            "        segment {\n" +
            "            from: (41.2481, -8.6814)\n" +
            "            to:   (38.7813, -9.1359)\n" +
            "            altitude_slots: [9000]\n" +
            "            width: 50 m\n" +
            "            wind:  270 deg 10 m/s\n" +
            "        }\n" +
            "    }\n" +
            "}";

    @Test
    void ensureSegmentIsBuiltForLeg() {
        final FlightPlanImportResult result = importer.importFromString(SINGLE_SEGMENT_DSL);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getFlights().get(0).legs.get(0).segments.size());
    }

    @Test
    void ensureSegmentFromCoordinatesAreCorrect() {
        final FlightPlanImportResult result = importer.importFromString(SINGLE_SEGMENT_DSL);
        final FlightPlanBuilderListener.SegmentData seg =
                result.getFlights().get(0).legs.get(0).segments.get(0);
        assertEquals(41.2481,  seg.fromLat, 0.0001);
        assertEquals(-8.6814, seg.fromLon,  0.0001);
    }

    @Test
    void ensureSegmentToCoordinatesAreCorrect() {
        final FlightPlanImportResult result = importer.importFromString(SINGLE_SEGMENT_DSL);
        final FlightPlanBuilderListener.SegmentData seg =
                result.getFlights().get(0).legs.get(0).segments.get(0);
        assertEquals(38.7813,  seg.toLat, 0.0001);
        assertEquals(-9.1359, seg.toLon,  0.0001);
    }

    @Test
    void ensureSegmentSingleAltitudeSlotIsCorrect() {
        final FlightPlanImportResult result = importer.importFromString(SINGLE_SEGMENT_DSL);
        final FlightPlanBuilderListener.SegmentData seg =
                result.getFlights().get(0).legs.get(0).segments.get(0);
        assertEquals(1, seg.altitudeSlots.size());
        assertEquals(9000.0, seg.altitudeSlots.get(0), 0.001);
    }

    @Test
    void ensureSegmentWidthIsCorrect() {
        final FlightPlanImportResult result = importer.importFromString(SINGLE_SEGMENT_DSL);
        final FlightPlanBuilderListener.SegmentData seg =
                result.getFlights().get(0).legs.get(0).segments.get(0);
        assertEquals(50.0, seg.width, 0.001);
    }

    @Test
    void ensureSegmentWindAngleAndSpeedAreCorrect() {
        final FlightPlanImportResult result = importer.importFromString(SINGLE_SEGMENT_DSL);
        final FlightPlanBuilderListener.SegmentData seg =
                result.getFlights().get(0).legs.get(0).segments.get(0);
        assertEquals(270.0, seg.windAngle, 0.001);
        assertEquals(10.0,  seg.windSpeed, 0.001);
    }

    @Test
    void ensureMultipleAltitudeSlotsAreAllParsed() {
        final String input =
                "flight TP101 {\n" +
                "    type: regular\n" +
                "    leg {\n" +
                "        departure: OPO 2026-05-17 14:00\n" +
                "        arrival:   LIS 15:00\n" +
                "        fuel:      5000 kg\n" +
                "        segment {\n" +
                "            from: (41.2481, -8.6814)\n" +
                "            to:   (38.7813, -9.1359)\n" +
                "            altitude_slots: [8000, 9000, 10000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "    }\n" +
                "}";
        final FlightPlanImportResult result = importer.importFromString(input);
        assertTrue(result.isSuccess());
        final FlightPlanBuilderListener.SegmentData seg =
                result.getFlights().get(0).legs.get(0).segments.get(0);
        assertEquals(3, seg.altitudeSlots.size());
        assertEquals(8000.0,  seg.altitudeSlots.get(0), 0.001);
        assertEquals(9000.0,  seg.altitudeSlots.get(1), 0.001);
        assertEquals(10000.0, seg.altitudeSlots.get(2), 0.001);
    }

    @Test
    void ensureMultipleSegmentsPerLegAreAllBuilt() {
        final String input =
                "flight TP102 {\n" +
                "    type: regular\n" +
                "    leg {\n" +
                "        departure: OPO 2026-05-17 14:00\n" +
                "        arrival:   LIS 15:00\n" +
                "        fuel:      5000 kg\n" +
                "        segment {\n" +
                "            from: (41.2481, -8.6814)\n" +
                "            to:   (40.0000, -9.0000)\n" +
                "            altitude_slots: [8000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 10 m/s\n" +
                "        }\n" +
                "        segment {\n" +
                "            from: (40.0000, -9.0000)\n" +
                "            to:   (38.7813, -9.1359)\n" +
                "            altitude_slots: [9000]\n" +
                "            width: 60 m\n" +
                "            wind:  260 deg 12 m/s\n" +
                "        }\n" +
                "    }\n" +
                "}";
        final FlightPlanImportResult result = importer.importFromString(input);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getFlights().get(0).legs.get(0).segments.size());
        assertEquals(60.0, result.getFlights().get(0).legs.get(0).segments.get(1).width, 0.001);
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
                "        segment {\n" +
                "            from: (41.2481, -8.6814)\n" +
                "            to:   (38.7813, -9.1359)\n" +
                "            altitude_slots: [8000]\n" +
                "            width: 50 m\n" +
                "            wind:  270 deg 12 m/s\n" +
                "        }\n" +
                "    }\n" +
                "    leg {\n" +
                "        departure: LIS 2026-05-17 09:00\n" +
                "        arrival:   FAO 09:45\n" +
                "        fuel:      3000 kg\n" +
                "        segment {\n" +
                "            from: (38.7813, -9.1359)\n" +
                "            to:   (37.0144, -7.9659)\n" +
                "            altitude_slots: [6000]\n" +
                "            width: 40 m\n" +
                "            wind:  200 deg 8 m/s\n" +
                "        }\n" +
                "    }\n" +
                "}";
        final FlightPlanImportResult result = importer.importFromString(input);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getFlights().get(0).legs.size());
        assertEquals(1, result.getFlights().get(0).legs.get(0).segments.size());
        assertEquals(1, result.getFlights().get(0).legs.get(1).segments.size());
        final FlightPlanBuilderListener.SegmentData seg1 =
                result.getFlights().get(0).legs.get(1).segments.get(0);
        assertEquals(38.7813, seg1.fromLat,              0.0001);
        assertEquals(6000.0,  seg1.altitudeSlots.get(0), 0.001);
        assertEquals(200.0,   seg1.windAngle,             0.001);
        assertEquals(8.0,     seg1.windSpeed,             0.001);
    }
}
