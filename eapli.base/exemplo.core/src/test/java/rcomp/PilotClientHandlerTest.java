package rcomp;

import controller.ImportFlightPlanController;
import controller.InsertWeatherDataController;
import controller.TestFlightPlanController;
import dto.AirControlAreaDTO;
import dto.FlightPlanDTO;
import dto.WeatherDataDTO;
import lprog.FlightLeg;
import lprog.FlightPlan;
import lprog.ErrorCollector;
import lprog.ErrorCollector.ValidationError;
import lprog.FlightPlanImporter.FlightPlanImportResult;
import lprog.FlightPlanStatus;
import lprog.FlightPlanTestResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for US086 — PilotClientHandler.
 *
 * <p>Uses a real socket pair so the handler's full read/write loop is exercised.
 * Controllers are mocked so no database or C binary is needed.</p>
 */
class PilotClientHandlerTest {

    private ServerSocket serverSocket;
    private Socket       clientSock;
    private Socket       serverSide;

    private TestFlightPlanController    testCtrl;
    private ImportFlightPlanController  importCtrl;
    private InsertWeatherDataController weatherCtrl;

    private BufferedReader clientIn;
    private PrintWriter    clientOut;

    @BeforeEach
    void setUp() throws Exception {
        // Random free port
        serverSocket = new ServerSocket(0);

        // Connect client
        clientSock = new Socket("localhost", serverSocket.getLocalPort());
        serverSide = serverSocket.accept();

        // Mock controllers
        testCtrl    = mock(TestFlightPlanController.class);
        importCtrl  = mock(ImportFlightPlanController.class);
        weatherCtrl = mock(InsertWeatherDataController.class);

        clientIn  = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
        clientOut = new PrintWriter(new OutputStreamWriter(clientSock.getOutputStream()), true);
    }

    @AfterEach
    void tearDown() throws Exception {
        clientSock.close();
        serverSide.close();
        serverSocket.close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Starts the handler thread with pre-authenticated state. */
    private void startAuthenticated() {
        final PilotClientHandler handler = new PilotClientHandler(
                serverSide, testCtrl, importCtrl, weatherCtrl,
                true, "pilot@alsafe.com");
        final Thread t = new Thread(handler);
        t.setDaemon(true);
        t.start();
    }

    /** Starts the handler thread NOT authenticated. */
    private void startUnauthenticated() {
        final PilotClientHandler handler = new PilotClientHandler(
                serverSide, testCtrl, importCtrl, weatherCtrl,
                false, null);
        final Thread t = new Thread(handler);
        t.setDaemon(true);
        t.start();
    }

    private String readLine() throws Exception {
        return clientIn.readLine();
    }

    private void sendLine(final String line) {
        clientOut.println(line);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    /**
     * Test 1 — Unauthenticated command is rejected with ERROR.
     * Refers to: US086.4
     */
    @Test
    void ensureUnauthenticatedCommandIsRejected() throws Exception {
        startUnauthenticated();

        sendLine("LIST_DRAFT_PLANS");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR), "Expected ERROR, got: " + response);
        assertTrue(response.contains("Not authenticated"));
    }

    /**
     * Test 2 — LOGOUT sends BYE.
     * Refers to: US086.4
     */
    @Test
    void ensureLogoutReturnsBye() throws Exception {
        startAuthenticated();

        sendLine("LOGOUT");
        final String response = readLine();

        assertEquals(Protocol.BYE, response);
    }

    /**
     * Test 3 — LIST_DRAFT_PLANS returns plans then END.
     * Refers to: US086.3
     */
    @Test
    void ensureListDraftPlansReturnsPlansAndEnd() throws Exception {
        final FlightPlan plan = new FlightPlan("TP010", "regular",
                List.of(new FlightLeg("LIS", "2026-06-01", "07:00", "MAD", "08:30", 1600.0, "kg")));

        when(testCtrl.listDraftFlightPlans("pilot@alsafe.com")).thenReturn(List.of(plan));

        startAuthenticated();

        sendLine("LIST_DRAFT_PLANS");

        final String line1 = readLine();
        final String line2 = readLine();

        assertTrue(line1.startsWith("TP010|DRAFT"), "Expected plan line, got: " + line1);
        assertEquals(Protocol.END, line2);
    }

    /**
     * Test 4 — LIST_DRAFT_PLANS with no plans returns OK + END.
     * Refers to: US086.3
     */
    @Test
    void ensureListDraftPlansEmptyReturnsOkAndEnd() throws Exception {
        when(testCtrl.listDraftFlightPlans(anyString())).thenReturn(List.of());

        startAuthenticated();

        sendLine("LIST_DRAFT_PLANS");

        final String line1 = readLine();
        final String line2 = readLine();

        assertTrue(line1.startsWith(Protocol.OK));
        assertEquals(Protocol.END, line2);
    }

    /**
     * Test 5 — IMPORT_FLIGHT_PLAN success returns OK.
     * Refers to: US086.3 (US081)
     */
    @Test
    void ensureImportFlightPlanSuccessReturnsOk() throws Exception {
        final lprog.FlightPlanBuilderListener.FlightData fd =
                new lprog.FlightPlanBuilderListener.FlightData();
        fd.id   = "TP010";
        fd.legs = new java.util.ArrayList<>();

        final FlightPlanImportResult result = FlightPlanImportResult.success(List.of(fd), "dsl");
        when(importCtrl.importFromDsl(anyString(), any())).thenReturn(result);
        when(importCtrl.lastSkippedIds()).thenReturn(List.of());

        startAuthenticated();

        sendLine("IMPORT_FLIGHT_PLAN");
        // Read server's "OK Ready" acknowledgement
        final String ack = readLine();
        assertTrue(ack.startsWith(Protocol.OK), "Expected OK Ready, got: " + ack);

        // Send DSL
        sendLine("flight TP010 { type: regular }");
        sendLine(Protocol.END_FILE);

        // The import response is multi-line; read until END
        final StringBuilder sb = new StringBuilder();
        String line;
        while ((line = readLine()) != null && !line.equals(Protocol.END)) {
            sb.append(line).append("\n");
        }
        final String response = sb.toString();
        assertTrue(response.startsWith(Protocol.OK), "Expected OK imported, got: " + response);
        assertTrue(response.contains("TP010"), "Expected TP010 in response, got: " + response);
    }

    /**
     * Test 6 — IMPORT_FLIGHT_PLAN failure returns ERROR.
     * Refers to: US086.3 (US081)
     */
    @Test
    void ensureImportFlightPlanFailureReturnsError() throws Exception {
        final FlightPlanImportResult result =
                FlightPlanImportResult.failure(List.of(
                        new ValidationError(ErrorCollector.ErrorType.SYNTACTIC, 1, 0, "Unexpected token")));
        when(importCtrl.importFromDsl(anyString(), any())).thenReturn(result);

        startAuthenticated();

        sendLine("IMPORT_FLIGHT_PLAN");
        readLine(); // consume OK Ready

        sendLine("invalid dsl");
        sendLine(Protocol.END_FILE);

        final String response = readLine();
        assertTrue(response.startsWith(Protocol.ERROR), "Expected ERROR, got: " + response);
    }

    /**
     * Test 7 — TEST_FLIGHT_PLAN PASS returns OK PASS.
     * Refers to: US086.3 (US085)
     */
    @Test
    void ensureTestFlightPlanPassReturnsOk() throws Exception {
        when(testCtrl.testFlightPlan("TP010")).thenReturn(FlightPlanTestResult.pass());

        startAuthenticated();

        sendLine("TEST_FLIGHT_PLAN TP010");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.OK), "Expected OK PASS, got: " + response);
        assertTrue(response.contains("PASS"));
    }

    /**
     * Test 8 — TEST_FLIGHT_PLAN FAIL returns ERROR with details.
     * Refers to: US086.3 (US085)
     */
    @Test
    void ensureTestFlightPlanFailReturnsError() throws Exception {
        when(testCtrl.testFlightPlan("TP012"))
                .thenReturn(FlightPlanTestResult.fail(List.of("Leg 1: wind speed exceeds limit")));

        startAuthenticated();

        sendLine("TEST_FLIGHT_PLAN TP012");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR), "Expected ERROR FAIL, got: " + response);
        assertTrue(response.contains("FAIL"));
    }

    /**
     * Test 9 — LIST_WEATHER_AREAS returns areas then END.
     * Refers to: US086.3 (US082)
     */
    @Test
    void ensureListWeatherAreasReturnsAreasAndEnd() throws Exception {
        when(weatherCtrl.getAvailableAreas())
                .thenReturn(List.of(new AirControlAreaDTO("LPPC", "Lisbon ACC")));

        startAuthenticated();

        sendLine("LIST_WEATHER_AREAS");

        final String line1 = readLine();
        final String line2 = readLine();

        assertEquals("LPPC|Lisbon ACC", line1);
        assertEquals(Protocol.END, line2);
    }

    /**
     * Test 10 — INSERT_WEATHER with invalid IDs returns ERROR.
     * Refers to: US086.3 (US082)
     */
    @Test
    void ensureInsertWeatherWithInvalidIdsReturnsError() throws Exception {
        startAuthenticated();

        // weather data ID (second arg) must be numeric; flight plan ID (first) is a string
        sendLine("INSERT_WEATHER TP010 notANumber");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR));
        assertTrue(response.contains("numeric"));
    }

    /**
     * Test 11 — INSERT_WEATHER success returns OK.
     * Refers to: US086.3 (US082)
     */
    @Test
    void ensureInsertWeatherSuccessReturnsOk() throws Exception {
        doNothing().when(weatherCtrl).insertWeatherData(5L, 1L);

        startAuthenticated();

        sendLine("INSERT_WEATHER 5 1");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.OK));
    }

    /**
     * Test 12 — Unknown command returns ERROR.
     * Refers to: US086.4
     */
    @Test
    void ensureUnknownCommandReturnsError() throws Exception {
        startAuthenticated();

        sendLine("UNKNOWN_CMD");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR));
        assertTrue(response.contains("Unknown command"));
    }
}
