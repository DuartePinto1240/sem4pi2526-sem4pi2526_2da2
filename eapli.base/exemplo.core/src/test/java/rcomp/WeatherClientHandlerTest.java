package rcomp;

import application.ImportWeatherResult;
import controller.ConsultWeatherDataController;
import controller.ImportBulkWeatherDataController;
import controller.RegisterWeatherDataController;
import dto.WeatherDataDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for US044 — WeatherClientHandler.
 *
 * Uses a real socket pair so the handler's full read/write loop is exercised.
 * Controllers are mocked so no database is needed.
 * Handler is pre-authenticated via the test constructor.
 */
class WeatherClientHandlerTest {

    private ServerSocket serverSocket;
    private Socket       clientSock;
    private Socket       serverSide;

    private BufferedReader clientIn;
    private PrintWriter    clientOut;

    private RegisterWeatherDataController  registerCtrl;
    private ImportBulkWeatherDataController importCtrl;
    private ConsultWeatherDataController   consultCtrl;

    @BeforeEach
    void setUp() throws Exception {
        serverSocket = new ServerSocket(0);
        clientSock   = new Socket("localhost", serverSocket.getLocalPort());
        serverSide   = serverSocket.accept();

        registerCtrl = mock(RegisterWeatherDataController.class);
        importCtrl   = mock(ImportBulkWeatherDataController.class);
        consultCtrl  = mock(ConsultWeatherDataController.class);

        clientIn  = new BufferedReader(
                new InputStreamReader(clientSock.getInputStream()));
        clientOut = new PrintWriter(
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
        final WeatherClientHandler handler = new WeatherClientHandler(
                serverSide, registerCtrl, importCtrl, consultCtrl,
                true, "wtps@alsafe.com");
        final Thread t = new Thread(handler);
        t.setDaemon(true);
        t.start();
    }

    private void startUnauthenticated() {
        final WeatherClientHandler handler = new WeatherClientHandler(
                serverSide, registerCtrl, importCtrl, consultCtrl,
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
     * Refers to: US044.4
     */
    @Test
    void ensureUnauthenticatedCommandIsRejected() throws Exception {
        startUnauthenticated();

        sendLine("LIST_AREAS");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR),
                "Expected ERROR, got: " + response);
        assertTrue(response.contains("Not authenticated"));
    }

    /**
     * Test 2 — LOGOUT sends BYE.
     * Refers to: US044.4
     */
    @Test
    void ensureLogoutReturnsBye() throws Exception {
        startAuthenticated();

        sendLine("LOGOUT");
        final String response = readLine();

        assertEquals(Protocol.BYE, response);
    }

    /**
     * Test 3 — Empty command returns ERROR.
     * Refers to: US044.4
     */
    @Test
    void ensureEmptyCommandReturnsError() throws Exception {
        startAuthenticated();

        sendLine("");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR));
    }

    /**
     * Test 4 — Unknown command returns ERROR.
     * Refers to: US044.4
     */
    @Test
    void ensureUnknownCommandReturnsError() throws Exception {
        startAuthenticated();

        sendLine("UNKNOWN_CMD");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR));
        assertTrue(response.contains("Unknown command"));
    }

    // ── LIST_AREAS ────────────────────────────────────────────────────────────

    /**
     * Test 5 — LIST_AREAS returns areas terminated by END.
     * Refers to: US044.3
     */
    @Test
    void ensureListAreasReturnsAreasAndEnd() throws Exception {
        final domain.AirControlArea area = mock(domain.AirControlArea.class);
        final domain.AirControlAreaCode code = mock(domain.AirControlAreaCode.class);
        when(code.code()).thenReturn("LPPC");
        when(area.identity()).thenReturn(code);
        when(area.name()).thenReturn("Lisbon ACC");
        when(registerCtrl.allAreas()).thenReturn(List.of(area));

        startAuthenticated();

        sendLine("LIST_AREAS");
        final List<String> lines = readUntilEnd();

        assertTrue(lines.stream().anyMatch(l -> l.contains("LPPC")));
        assertEquals(Protocol.END, lines.get(lines.size() - 1));
    }

    /**
     * Test 6 — LIST_AREAS with no areas returns only END.
     * Refers to: US044.3
     */
    @Test
    void ensureListAreasEmptyReturnsEnd() throws Exception {
        when(registerCtrl.allAreas()).thenReturn(List.of());

        startAuthenticated();

        sendLine("LIST_AREAS");
        final List<String> lines = readUntilEnd();

        assertEquals(1, lines.size());
        assertEquals(Protocol.END, lines.get(0));
    }

    // ── REGISTER_WEATHER ──────────────────────────────────────────────────────

    /**
     * Test 7 — REGISTER_WEATHER with too few arguments returns ERROR.
     * Refers to: US044.3
     */
    @Test
    void ensureRegisterWeatherWithMissingArgsReturnsError() throws Exception {
        startAuthenticated();

        sendLine("REGISTER_WEATHER LPPC 37.0 43.0");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR));
        assertTrue(response.contains("Usage"));
    }

    /**
     * Test 8 — REGISTER_WEATHER with invalid number returns ERROR.
     * Refers to: US044.3
     */
    @Test
    void ensureRegisterWeatherWithInvalidNumberReturnsError() throws Exception {
        startAuthenticated();

        sendLine("REGISTER_WEATHER LPPC BADNUM 43.0 -9.8 4.9 0 14000"
                + " 2026-06-01T05:00 2026-06-01T08:00 90 25");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR));
    }

    /**
     * Test 9 — REGISTER_WEATHER with invalid date format returns ERROR.
     * Refers to: US044.3
     */
    @Test
    void ensureRegisterWeatherWithInvalidDateReturnsError() throws Exception {
        startAuthenticated();

        sendLine("REGISTER_WEATHER LPPC 37.0 43.0 -9.8 4.9 0 14000"
                + " BADDATE BADDATE 90 25");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR));
    }

    

    // ── IMPORT_WEATHER ────────────────────────────────────────────────────────

    /**
     * Test 11 — IMPORT_WEATHER success returns OK with counts and END.
     * Refers to: US044.3
     */
    @Test
    void ensureImportWeatherSuccessReturnsOkAndEnd() throws Exception {
        final ImportWeatherResult result = mock(ImportWeatherResult.class);
        when(result.importedCount()).thenReturn(3);
        when(result.rejectedCount()).thenReturn(0);
        when(result.errors()).thenReturn(List.of());
        when(importCtrl.importWeatherData(anyString())).thenReturn(result);

        startAuthenticated();

        sendLine("IMPORT_WEATHER");
        final String ack = readLine();
        assertTrue(ack.startsWith(Protocol.OK), "Expected OK Ready, got: " + ack);

        sendLine("ACA,Lat.,Long.,Lat.,Long.,Alt_inf (ft),Alt_Sup (ft),Direction,Value,Day,Star,End");
        sendLine("121,43.84,-9.79,40.22,-7.95,0,1000,90,28.75,2026-06-22,05:00:00,08:15:00");
        sendLine(Protocol.END_FILE);

        final String summary = readLine();
        assertTrue(summary.startsWith(Protocol.OK));
        assertTrue(summary.contains("imported=3"));

        final String endLine = readLine();
        assertEquals(Protocol.END, endLine);
    }

    /**
     * Test 12 — IMPORT_WEATHER with rejections returns errors and END.
     * Refers to: US044.3
     */
    @Test
    void ensureImportWeatherWithRejectionsReturnsErrorLines() throws Exception {
        final ImportWeatherResult result = mock(ImportWeatherResult.class);
        when(result.importedCount()).thenReturn(1);
        when(result.rejectedCount()).thenReturn(1);
        when(result.errors()).thenReturn(List.of("Row 2: invalid area code"));
        when(importCtrl.importWeatherData(anyString())).thenReturn(result);

        startAuthenticated();

        sendLine("IMPORT_WEATHER");
        readLine(); // consume OK Ready

        sendLine("header");
        sendLine("bad row");
        sendLine(Protocol.END_FILE);

        final String summary = readLine();
        assertTrue(summary.contains("rejected=1"));

        final String errorLine = readLine();
        assertTrue(errorLine.startsWith(Protocol.ERROR));
        assertTrue(errorLine.contains("Row 2"));

        final String endLine = readLine();
        assertEquals(Protocol.END, endLine);
    }

    // ── CONSULT_WEATHER ───────────────────────────────────────────────────────

    /**
     * Test 13 — CONSULT_WEATHER with missing args returns ERROR.
     * Refers to: US044.3
     */
    @Test
    void ensureConsultWeatherWithMissingArgsReturnsError() throws Exception {
        startAuthenticated();

        sendLine("CONSULT_WEATHER LPPC");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR));
        assertTrue(response.contains("Usage"));
    }

    /**
     * Test 14 — CONSULT_WEATHER with invalid date returns ERROR.
     * Refers to: US044.3
     */
    @Test
    void ensureConsultWeatherWithInvalidDateReturnsError() throws Exception {
        startAuthenticated();

        sendLine("CONSULT_WEATHER LPPC BADDATE");
        final String response = readLine();

        assertTrue(response.startsWith(Protocol.ERROR));
    }

    /**
     * Test 15 — CONSULT_WEATHER with no results returns OK + END.
     * Refers to: US044.3
     */
    @Test
    void ensureConsultWeatherNoResultsReturnsOkAndEnd() throws Exception {
        when(consultCtrl.weatherDataByAreaAndDate(eq("LPPC"), any(LocalDate.class)))
                .thenReturn(List.of());

        startAuthenticated();

        sendLine("CONSULT_WEATHER LPPC 2026-06-01");

        final List<String> lines = readUntilEnd();

        assertTrue(lines.get(0).startsWith(Protocol.OK));
        assertEquals(Protocol.END, lines.get(lines.size() - 1));
    }

    /**
     * Test 16 — CONSULT_WEATHER with results returns data lines and END.
     * Refers to: US044.3
     */
    @Test
    void ensureConsultWeatherWithResultsReturnsDataAndEnd() throws Exception {
        final WeatherDataDTO dto = mock(WeatherDataDTO.class);
        when(dto.toString()).thenReturn("LPPC|37.0|43.0|-9.8|4.9|0|14000|90|25");
        when(consultCtrl.weatherDataByAreaAndDate(eq("LPPC"), any(LocalDate.class)))
                .thenReturn(List.of(dto));

        startAuthenticated();

        sendLine("CONSULT_WEATHER LPPC 2026-06-01");
        final List<String> lines = readUntilEnd();

        assertTrue(lines.stream().anyMatch(l -> l.contains("LPPC")));
        assertEquals(Protocol.END, lines.get(lines.size() - 1));
    }
}