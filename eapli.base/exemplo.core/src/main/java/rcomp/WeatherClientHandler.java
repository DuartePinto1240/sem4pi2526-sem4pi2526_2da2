package rcomp;

import controller.ConsultWeatherDataController;
import controller.ImportBulkWeatherDataController;
import controller.RegisterWeatherDataController;
import application.ImportWeatherResult;
import domain.AirControlArea;
import domain.BaseRoles;
import dto.WeatherDataDTO;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserSession;
import persistence.PersistenceContext;
import domain.EmailAddress;
import rcomp.logging.EventType;
import rcomp.logging.RemoteAccessLogger;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Handles one Weather Person TCP connection.
 *
 * <p>Protocol (text, one command per line):</p>
 * <pre>
 *   LOGIN &lt;email&gt; &lt;password&gt;
 *   LIST_AREAS
 *   REGISTER_WEATHER &lt;areaCode&gt; &lt;subMinLat&gt; &lt;subMaxLat&gt; &lt;subMinLon&gt; &lt;subMaxLon&gt;
 *                    &lt;minAlt&gt; &lt;maxAlt&gt; &lt;startISO&gt; &lt;endISO&gt; &lt;windDir&gt; &lt;windSpeed&gt;
 *   IMPORT_WEATHER (server replies OK, then client sends CSV lines + END_FILE)
 *   CONSULT_WEATHER &lt;areaCode&gt; &lt;yyyy-MM-dd&gt;
 *   LOGOUT
 * </pre>
 *
 * <p>Date/time tokens must use ISO format without spaces:
 * {@code yyyy-MM-ddTHH:mm} (e.g. {@code 2026-06-05T10:00}).</p>
 */
public class WeatherClientHandler implements Runnable {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Socket socket;
    private boolean authenticated = false;
    private String  authenticatedEmail = null;
    private final RemoteAccessLogger logger = new RemoteAccessLogger("localhost");

    public WeatherClientHandler(final Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            System.out.printf("[WEATHER-SERVER] Client connected: %s%n", socket.getInetAddress());

            String line;
            while ((line = in.readLine()) != null) {
                final String response = handle(line.trim(), in, out);
                // For IMPORT_WEATHER the handler writes directly to 'out' during the file-receive loop, so handle()
                // returns null to avoid a double-write.
                if (response != null) {
                    out.println(response);
                }
                if (Protocol.BYE.equals(response)) break;
            }

        } catch (final IOException e) {
            System.err.println("[WEATHER-SERVER] Client error: " + e.getMessage());
        } finally {
            if (authenticated && authenticatedEmail != null) {
                logger.log(EventType.DISCONNECT, "US44", authenticatedEmail,
                        socket.getInetAddress().getHostAddress(), socket.getPort());
            }
            try { socket.close(); } catch (final IOException ignored) {}
            System.out.println("[WEATHER-SERVER] Client disconnected.");
        }
    }

    // Dispatcher
    private String handle(final String line, final BufferedReader in, final PrintWriter out) throws IOException {
        if (line.isEmpty()) return Protocol.ERROR + " Empty command";

        final String[] parts = line.split(" ", 2);
        final String cmd = parts[0].toUpperCase();
        final String args = parts.length > 1 ? parts[1] : "";

        switch (cmd) {
            case Protocol.LOGIN: return handleLogin(args);
            case Protocol.LOGOUT: return handleLogout();
            default:
                if (!authenticated)
                    return Protocol.ERROR + " Not authenticated";
                switch (cmd) {
                    case Protocol.LIST_AREAS: return handleListAreas();
                    case Protocol.REGISTER_WEATHER: return handleRegister(args);
                    case Protocol.IMPORT_WEATHER: return handleImport(in, out);
                    case Protocol.CONSULT_WEATHER: return handleConsult(args);
                    default:
                        return Protocol.ERROR + " Unknown command: " + cmd;
                }
        }
    }

    // LOGIN
    private String handleLogin(final String args) {
        final String[] parts = args.split(" ", 2);
        if (parts.length < 2)
            return Protocol.ERROR + " Usage: LOGIN <email> <password>";

        final String email = parts[0];
        final String password = parts[1];

        System.out.println("[WEATHER-SERVER] Login attempt: " + email);

        try {
            final Optional<UserSession> session = AuthzRegistry.authenticationService().authenticate(email, password);

            if (session.isEmpty()) {
                logger.log(EventType.LOGIN_FAIL, "US44", email,
                        socket.getInetAddress().getHostAddress(), socket.getPort());
                return Protocol.ERROR + " Invalid credentials";
            }

            // Check security clearance
            final Optional<domain.User> domainUser = PersistenceContext.getInstance().userRepository().ofIdentity(EmailAddress.valueOf(email));

            if (domainUser.isPresent()) {
                final LocalDate expiry = domainUser.get().getSecurityClearanceExpiry();
                if (expiry != null && !expiry.isAfter(LocalDate.now())) {
                    AuthzRegistry.authorizationService().clearSession();
                    logger.log(EventType.LOGIN_FAIL, "US44", email,
                            socket.getInetAddress().getHostAddress(), socket.getPort());
                    return Protocol.ERROR + " Security clearance expired";
                }
            }

            // Verify the user actually has WEATHER_PERSON role
            final boolean hasRole = session.get().authenticatedUser().hasAny(BaseRoles.WEATHER_PERSON);
            if (!hasRole) {
                AuthzRegistry.authorizationService().clearSession();
                logger.log(EventType.LOGIN_FAIL, "US44", email,
                        socket.getInetAddress().getHostAddress(), socket.getPort());
                return Protocol.ERROR + " Access denied: not a Weather Person";
            }

            authenticated = true;
            authenticatedEmail = email;
            logger.log(EventType.LOGIN_SUCCESS, "US44", email,
                    socket.getInetAddress().getHostAddress(), socket.getPort());
            System.out.println("[WEATHER-SERVER] Login OK: " + email);
            return Protocol.OK + " Welcome " + email;

        } catch (final Exception e) {
            System.err.println("[WEATHER-SERVER] Login error: " + e.getMessage());
            return Protocol.ERROR + " " + e.getMessage();
        }
    }

    // LOGOUT
    private String handleLogout() {
        if (authenticatedEmail != null) {
            logger.log(EventType.LOGOUT, "US44", authenticatedEmail,
                    socket.getInetAddress().getHostAddress(), socket.getPort());
        }
        authenticated      = false;
        authenticatedEmail = null;
        AuthzRegistry.authorizationService().clearSession();
        return Protocol.BYE;
    }

    // LIST_AREAS
    private String handleListAreas() {
        try {
            final StringBuilder sb = new StringBuilder();
            for (final AirControlArea area : new RegisterWeatherDataController().allAreas()) {
                sb.append(area.identity().code()).append("|").append(area.name()).append("\n");
            }
            sb.append(Protocol.END);
            return sb.toString();
        } catch (final Exception e) {
            return Protocol.ERROR + " " + e.getMessage();
        }
    }

    // REGISTER_WEATHER
    // Args: areaCode subMinLat subMaxLat subMinLon subMaxLon minAlt maxAlt startISO endISO windDir windSpeed
    private String handleRegister(final String args) {
        final String[] p = args.split(" ");
        if (p.length < 11)
            return Protocol.ERROR + " Usage: REGISTER_WEATHER areaCode subMinLat subMaxLat subMinLon"
                    + " subMaxLon minAlt maxAlt startISO endISO windDir windSpeed";
        try {
            final String areaCode = p[0];
            final double subMinLat = Double.parseDouble(p[1]);
            final double subMaxLat = Double.parseDouble(p[2]);
            final double subMinLon = Double.parseDouble(p[3]);
            final double subMaxLon = Double.parseDouble(p[4]);
            final double minAlt = Double.parseDouble(p[5]);
            final double maxAlt = Double.parseDouble(p[6]);
            final LocalDateTime start = LocalDateTime.parse(p[7], DT_FMT);
            final LocalDateTime end = LocalDateTime.parse(p[8], DT_FMT);
            final double windDir = Double.parseDouble(p[9]);
            final double windSpeed = Double.parseDouble(p[10]);

            new RegisterWeatherDataController().registerWeatherData(areaCode, subMinLat, subMaxLat, subMinLon, subMaxLon,
                    minAlt, maxAlt, start, end, windDir, windSpeed);

            return Protocol.OK + " Weather data registered";

        } catch (final DateTimeParseException e) {
            return Protocol.ERROR + " Invalid date/time format (expected yyyy-MM-ddTHH:mm): " + e.getMessage();
        } catch (final NumberFormatException e) {
            return Protocol.ERROR + " Invalid number: " + e.getMessage();
        } catch (final IllegalArgumentException e) {
            return Protocol.ERROR + " " + e.getMessage();
        } catch (final Exception e) {
            return Protocol.ERROR + " Unexpected error: " + e.getMessage();
        }
    }

    // IMPORT_WEATHER
    // Flow:
    //   Client sends:  IMPORT_WEATHER
    //   Server sends:  OK Ready — send CSV lines then END_FILE
    //   Client sends:  <header line>
    //                  <data line 1>
    //                  ...
    //                  END_FILE
    //   Server sends:  OK imported=N rejected=M
    //                  (plus one ERROR line per rejected record)
    //                  END
    private String handleImport(final BufferedReader in, final PrintWriter out) throws IOException {
        // Signal to the client that we are ready to receive
        out.println(Protocol.OK + " Ready - send CSV lines then " + Protocol.END_FILE);

        // Accumulate lines in a temp file so the existing CsvWeatherDataReader
        // (which expects a real file path) can process them unchanged.
        final File tempFile = File.createTempFile("weather_import_", ".csv");
        tempFile.deleteOnExit();

        try {
            // Write received lines to the temp file
            try (final PrintWriter fw = new PrintWriter(new FileWriter(tempFile))) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (Protocol.END_FILE.equals(line.trim())) break;
                    fw.println(line);
                }
            }

            // Delegate to the existing controller (reads from the temp file)
            final ImportWeatherResult result = new ImportBulkWeatherDataController().importWeatherData(tempFile.getAbsolutePath());

            // Send summary
            out.println(Protocol.OK + " imported=" + result.importedCount() + " rejected=" + result.rejectedCount());

            // Send one line per error so the client can display them
            for (final String error : result.errors()) {
                out.println(Protocol.ERROR + " " + error);
            }
            out.println(Protocol.END);

        } catch (final IllegalArgumentException e) {
            out.println(Protocol.ERROR + " " + e.getMessage());
            out.println(Protocol.END);
        } finally {
            // Always delete the temp file, even if processing failed
            tempFile.delete();
        }

        // Return null: we already wrote everything directly to 'out' above
        return null;
    }

    // CONSULT_WEATHER
    // Args: areaCode date (yyyy-MM-dd)
    private String handleConsult(final String args) {
        final String[] p = args.split(" ", 2);
        if (p.length < 2)
            return Protocol.ERROR + " Usage: CONSULT_WEATHER <areaCode> <yyyy-MM-dd>";
        try {
            final String areaCode = p[0];
            final LocalDate date = LocalDate.parse(p[1].trim(), DATE_FMT);

            final List<WeatherDataDTO> results = new ConsultWeatherDataController().weatherDataByAreaAndDate(areaCode, date);

            if (results.isEmpty()) {
                return Protocol.OK + " No weather data found for area '" + areaCode + "' on " + date + "\n" + Protocol.END;
            }

            final StringBuilder sb = new StringBuilder();
            for (final WeatherDataDTO dto : results) {
                // One record per line using the DTO's toString()
                sb.append(dto.toString()).append("\n");
            }
            sb.append(Protocol.END);
            return sb.toString();

        } catch (final DateTimeParseException e) {
            return Protocol.ERROR + " Invalid date format (expected yyyy-MM-dd): " + e.getMessage();
        } catch (final IllegalArgumentException e) {
            return Protocol.ERROR + " " + e.getMessage();
        } catch (final Exception e) {
            return Protocol.ERROR + " Unexpected error: " + e.getMessage();
        }
    }
}
