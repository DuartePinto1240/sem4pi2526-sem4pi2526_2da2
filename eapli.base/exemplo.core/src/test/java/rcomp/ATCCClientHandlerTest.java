package rcomp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for US078 — ATCCClientHandler.
 *
 * Uses a real socket pair so the handler's full read/write loop is exercised.
 * No database or AuthzRegistry needed — handler is pre-authenticated via
 * the test constructor.
 */
class ATCCClientHandlerTest {

    private ServerSocket serverSocket;
    private Socket       clientSock;
    private Socket       serverSide;

    private BufferedReader clientIn;
    private PrintWriter    clientOut;

    @BeforeEach
    void setUp() throws Exception {
        serverSocket = new ServerSocket(0);
        clientSock   = new Socket("localhost", serverSocket.getLocalPort());
        serverSide   = serverSocket.accept();
        clientIn     = new BufferedReader(
                new InputStreamReader(clientSock.getInputStream()));
        clientOut    = new PrintWriter(
                new OutputStreamWriter(clientSock.getOutputStream()), true);
    }

    @AfterEach
    void tearDown() throws Exception {
        try { clientOut.close();    } catch (final Exception ignored) {}
        try { clientIn.close();     } catch (final Exception ignored) {}
        try { clientSock.close();   } catch (final Exception ignored) {}
        try { serverSide.close();   } catch (final Exception ignored) {}
        try { serverSocket.close(); } catch (final Exception ignored) {}
        Thread.sleep(100);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void startAuthenticated() {
        final ATCCClientHandler handler =
                new ATCCClientHandler(serverSide, true, "atcc@alsafe.com");
        final Thread t = new Thread(handler);
        t.setDaemon(true);
        t.start();
    }

    private void startUnauthenticated() {
        final ATCCClientHandler handler =
                new ATCCClientHandler(serverSide, false, null);
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

    private List<String> readUntilEnd() throws Exception {
        final List<String> lines = new ArrayList<>();
        String line;
        while ((line = readLine()) != null) {
            lines.add(line);
            if (Protocol.END.equals(line)) break;
        }
        return lines;
    }

    // ── authentication ────────────────────────────────────────────────────────

    /**
     * Test 1 — Command before LOGIN is rejected.
     * Refers to: US078.4
     */
    @Test
    void ensureUnauthenticatedCommandIsRejected() throws Exception {
        startUnauthenticated();

        sendLine("LIST_AIRPORTS");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR),
                "Expected ERROR, got: " + response);
        assertTrue(response.contains("Not authenticated"));
    }

    /**
     * Test 2 — LOGOUT sends BYE.
     * Refers to: US078.4
     */


    /**
     * Test 3 — Empty command returns ERROR.
     * Refers to: US078.4
     */
    @Test
    void ensureEmptyCommandReturnsError() throws Exception {
        startAuthenticated();

        sendLine("");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR));
    }

    /**
     * Test 4 — Unknown command returns ERROR with message.
     * Refers to: US078.4
     */
    @Test
    void ensureUnknownCommandReturnsError() throws Exception {
        startAuthenticated();

        sendLine("UNKNOWN_CMD");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR));
        assertTrue(response.contains("Unknown command"));
    }

    // ── LIST_AIRPORTS ─────────────────────────────────────────────────────────

    /**
     * Test 5 — LIST_AIRPORTS response is terminated by END.
     * Refers to: US078.3
     */
    @Test
    void ensureListAirportsReturnsEndLine() throws Exception {
        startAuthenticated();

        sendLine("LIST_AIRPORTS");
        final List<String> lines = readUntilEnd();

        assertFalse(lines.isEmpty());
        assertEquals(Protocol.END, lines.get(lines.size() - 1));
    }

    // ── LIST_MODELS ───────────────────────────────────────────────────────────

    /**
     * Test 6 — LIST_MODELS response is terminated by END.
     * Refers to: US078.3
     */
    @Test
    void ensureListModelsReturnsEndLine() throws Exception {
        startAuthenticated();

        sendLine("LIST_MODELS");
        final List<String> lines = readUntilEnd();

        assertFalse(lines.isEmpty());
        assertEquals(Protocol.END, lines.get(lines.size() - 1));
    }

    // ── ADD_AIRCRAFT ──────────────────────────────────────────────────────────

    /**
     * Test 7 — ADD_AIRCRAFT with too few arguments returns ERROR with usage.
     * Refers to: US078.3 (US070)
     */
    @Test
    void ensureAddAircraftWithMissingArgsReturnsError() throws Exception {
        startAuthenticated();

        sendLine("ADD_AIRCRAFT CS-TCP Portugal");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR));
        assertTrue(response.contains("Usage"));
    }

    // ── LIST_FLEET ────────────────────────────────────────────────────────────

    /**
     * Test 8 — LIST_FLEET response is terminated by END.
     * Refers to: US078.3 (US072)
     */
    @Test
    void ensureListFleetReturnsEndLine() throws Exception {
        startAuthenticated();

        sendLine("LIST_FLEET TP");
        final List<String> lines = readUntilEnd();

        assertEquals(Protocol.END, lines.get(lines.size() - 1));
    }

    /**
     * Test 9 — LIST_FLEET_MODEL with missing args returns ERROR.
     * Refers to: US078.3 (US072)
     */
    @Test
    void ensureListFleetModelWithMissingArgsReturnsError() throws Exception {
        startAuthenticated();

        sendLine("LIST_FLEET_MODEL TP");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR));
        assertTrue(response.contains("Usage"));
    }

    /**
     * Test 10 — LIST_FLEET_MAKER with missing args returns ERROR.
     * Refers to: US078.3 (US072)
     */
    @Test
    void ensureListFleetMakerWithMissingArgsReturnsError() throws Exception {
        startAuthenticated();

        sendLine("LIST_FLEET_MAKER TP");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR));
        assertTrue(response.contains("Usage"));
    }

    /**
     * Test 11 — LIST_FLEET_CAP with missing args returns ERROR.
     * Refers to: US078.3 (US072)
     */
    @Test
    void ensureListFleetCapWithMissingArgsReturnsError() throws Exception {
        startAuthenticated();

        sendLine("LIST_FLEET_CAP TP");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR));
        assertTrue(response.contains("Usage"));
    }

    /**
     * Test 12 — LIST_FLEET_AGE response is terminated by END.
     * Refers to: US078.3 (US072)
     */
    @Test
    void ensureListFleetAgeReturnsEndLine() throws Exception {
        startAuthenticated();

        sendLine("LIST_FLEET_AGE TP");
        final List<String> lines = readUntilEnd();

        assertEquals(Protocol.END, lines.get(lines.size() - 1));
    }

    // ── DECOMMISSION ──────────────────────────────────────────────────────────

    /**
     * Test 13 — DECOMMISSION with missing companyIata returns ERROR.
     * Refers to: US078.3 (US071)
     */
    @Test
    void ensureDecommissionWithMissingArgsReturnsError() throws Exception {
        startAuthenticated();

        sendLine("DECOMMISSION CS-TCP");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR));
        assertTrue(response.contains("Usage"));
    }

    // ── CREATE_ROUTE ──────────────────────────────────────────────────────────

    /**
     * Test 14 — CREATE_ROUTE with too few arguments returns ERROR.
     * Refers to: US078.3 (US073)
     */
    @Test
    void ensureCreateRouteWithMissingArgsReturnsError() throws Exception {
        startAuthenticated();

        sendLine("CREATE_ROUTE TP001 TP OPO");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR));
        assertTrue(response.contains("Usage"));
    }

    /**
     * Test 15 — CREATE_ROUTE REGULAR with invalid day name returns ERROR.
     * Refers to: US078.3 (US073)
     */
    @Test
    void ensureCreateRouteRegularWithInvalidDayReturnsError() throws Exception {
        startAuthenticated();

        sendLine("CREATE_ROUTE TP001 TP OPO LIS REGULAR BADDAY 14:00 15:00");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR));
    }

    /**
     * Test 16 — CREATE_ROUTE CHARTER with invalid date format returns ERROR.
     * Refers to: US078.3 (US073)
     */
    @Test
    void ensureCreateRouteCharterWithInvalidDateReturnsError() throws Exception {
        startAuthenticated();

        sendLine("CREATE_ROUTE TP001 TP OPO LIS CHARTER BADDATE 10:00 BADDATE 11:00");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR));
    }
}