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
}