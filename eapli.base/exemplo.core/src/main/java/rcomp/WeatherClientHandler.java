package rcomp;

import application.ImportWeatherResult;
import controller.ConsultWeatherDataController;
import controller.ImportBulkWeatherDataController;
import controller.RegisterWeatherDataController;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public class WeatherClientHandler implements Runnable {

    private static final DateTimeFormatter DT_FMT   =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter DATE_FMT  =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Socket socket;
    private boolean authenticated      = false;
    private String  authenticatedEmail = null;
    private final RemoteAccessLogger logger =
            new RemoteAccessLogger("localhost");

    // ── controllers (injected or created on demand) ───────────────────────
    private final RegisterWeatherDataController   registerCtrl;
    private final ImportBulkWeatherDataController importCtrl;
    private final ConsultWeatherDataController    consultCtrl;

    /** Production constructor — creates controllers internally. */
    public WeatherClientHandler(final Socket socket) {
        this.socket       = socket;
        this.registerCtrl = new RegisterWeatherDataController();
        this.importCtrl   = new ImportBulkWeatherDataController();
        this.consultCtrl  = new ConsultWeatherDataController();
    }

    /** Package-private — for unit tests (inject controllers + pre-auth). */
    WeatherClientHandler(final Socket socket,
                         final RegisterWeatherDataController registerCtrl,
                         final ImportBulkWeatherDataController importCtrl,
                         final ConsultWeatherDataController consultCtrl,
                         final boolean preAuthenticated,
                         final String preAuthEmail) {
        this.socket             = socket;
        this.registerCtrl       = registerCtrl;
        this.importCtrl         = importCtrl;
        this.consultCtrl        = consultCtrl;
        this.authenticated      = preAuthenticated;
        this.authenticatedEmail = preAuthEmail;
    }

    @Override
    public void run() {
        try (
                final BufferedReader in  = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                final PrintWriter    out = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            System.out.printf("[WEATHER-SERVER] Client connected: %s%n",
                    socket.getInetAddress());

            String line;
            while ((line = in.readLine()) != null) {
                final String response = handle(line.trim(), in, out);
                if (response != null) out.println(response);
                if (Protocol.BYE.equals(response)) break;
            }

        } catch (final IOException e) {
            System.err.println("[WEATHER-SERVER] Client error: " + e.getMessage());
        } finally {
            if (authenticated && authenticatedEmail != null) {
                logger.log(EventType.DISCONNECT, "US44", authenticatedEmail,
                        socket.getInetAddress().getHostAddress(),
                        socket.getPort());
            }
            try { socket.close(); } catch (final IOException ignored) {}
            System.out.println("[WEATHER-SERVER] Client disconnected.");
        }
    }

    private String handle(final String line,
                          final BufferedReader in,
                          final PrintWriter out) throws IOException {
        if (line.isEmpty()) return Protocol.ERROR + " Empty command";

        final String[] parts = line.split(" ", 2);
        final String   cmd   = parts[0].toUpperCase();
        final String   args  = parts.length > 1 ? parts[1] : "";

        switch (cmd) {
            case Protocol.LOGIN:  return handleLogin(args);
            case Protocol.LOGOUT: return handleLogout();
            default:
                if (!authenticated)
                    return Protocol.ERROR + " Not authenticated";
                switch (cmd) {
                    case Protocol.LIST_AREAS:       return handleListAreas();
                    case Protocol.REGISTER_WEATHER: return handleRegister(args);
                    case Protocol.IMPORT_WEATHER:   return handleImport(in, out);
                    case Protocol.CONSULT_WEATHER:  return handleConsult(args);
                    default:
                        return Protocol.ERROR + " Unknown command: " + cmd;
                }
        }
    }

    private String handleLogin(final String args) {
        final String[] parts = args.split(" ", 2);
        if (parts.length < 2)
            return Protocol.ERROR + " Usage: LOGIN <email> <password>";

        final String email    = parts[0];
        final String password = parts[1];

        System.out.println("[WEATHER-SERVER] Login attempt: " + email);

        try {
            final Optional<UserSession> session =
                    AuthzRegistry.authenticationService()
                            .authenticate(email, password);

            if (session.isEmpty()) {
                logger.log(EventType.LOGIN_FAIL, "US44", email,
                        socket.getInetAddress().getHostAddress(),
                        socket.getPort());
                return Protocol.ERROR + " Invalid credentials";
            }

            final Optional<domain.User> domainUser =
                    PersistenceContext.getInstance()
                            .userRepository()
                            .ofIdentity(EmailAddress.valueOf(email));

            if (domainUser.isPresent()) {
                final LocalDate expiry =
                        domainUser.get().getSecurityClearanceExpiry();
                if (expiry != null && !expiry.isAfter(LocalDate.now())) {
                    AuthzRegistry.authorizationService().clearSession();
                    logger.log(EventType.LOGIN_FAIL, "US44", email,
                            socket.getInetAddress().getHostAddress(),
                            socket.getPort());
                    return Protocol.ERROR + " Security clearance expired";
                }
            }

            if (!session.get().authenticatedUser()
                    .hasAny(BaseRoles.WEATHER_PERSON)) {
                AuthzRegistry.authorizationService().clearSession();
                logger.log(EventType.LOGIN_FAIL, "US44", email,
                        socket.getInetAddress().getHostAddress(),
                        socket.getPort());
                return Protocol.ERROR + " Access denied: not a Weather Person";
            }

            authenticated      = true;
            authenticatedEmail = email;
            logger.log(EventType.LOGIN_SUCCESS, "US44", email,
                    socket.getInetAddress().getHostAddress(),
                    socket.getPort());
            System.out.println("[WEATHER-SERVER] Login OK: " + email);
            return Protocol.OK + " Welcome " + email;

        } catch (final Exception e) {
            System.err.println("[WEATHER-SERVER] Login error: " + e.getMessage());
            return Protocol.ERROR + " " + e.getMessage();
        }
    }

    private String handleLogout() {
        if (authenticatedEmail != null) {
            logger.log(EventType.LOGOUT, "US44", authenticatedEmail,
                    socket.getInetAddress().getHostAddress(),
                    socket.getPort());
        }
        authenticated      = false;
        authenticatedEmail = null;
        try {
            AuthzRegistry.authorizationService().clearSession();
        } catch (final Exception ignored) {
            // AuthzRegistry may not be configured in test environment
        }
        return Protocol.BYE;
    }

    private String handleListAreas() {
        try {
            final StringBuilder sb = new StringBuilder();
            for (final AirControlArea area : registerCtrl.allAreas()) {
                sb.append(area.identity().code())
                        .append("|")
                        .append(area.name())
                        .append("\n");
            }
            sb.append(Protocol.END);
            return sb.toString();
        } catch (final Exception e) {
            return Protocol.ERROR + " " + e.getMessage();
        }
    }

    private String handleRegister(final String args) {
        final String[] p = args.split(" ");
        if (p.length < 11)
            return Protocol.ERROR
                    + " Usage: REGISTER_WEATHER areaCode subMinLat subMaxLat"
                    + " subMinLon subMaxLon minAlt maxAlt startISO endISO"
                    + " windDir windSpeed";
        try {
            final String        areaCode  = p[0];
            final double        subMinLat = Double.parseDouble(p[1]);
            final double        subMaxLat = Double.parseDouble(p[2]);
            final double        subMinLon = Double.parseDouble(p[3]);
            final double        subMaxLon = Double.parseDouble(p[4]);
            final double        minAlt    = Double.parseDouble(p[5]);
            final double        maxAlt    = Double.parseDouble(p[6]);
            final LocalDateTime start     = LocalDateTime.parse(p[7], DT_FMT);
            final LocalDateTime end       = LocalDateTime.parse(p[8], DT_FMT);
            final double        windDir   = Double.parseDouble(p[9]);
            final double        windSpeed = Double.parseDouble(p[10]);

            registerCtrl.registerWeatherData(areaCode,
                    subMinLat, subMaxLat, subMinLon, subMaxLon,
                    minAlt, maxAlt, start, end, windDir, windSpeed);

            return Protocol.OK + " Weather data registered";

        } catch (final DateTimeParseException e) {
            return Protocol.ERROR
                    + " Invalid date/time format (expected yyyy-MM-ddTHH:mm): "
                    + e.getMessage();
        } catch (final NumberFormatException e) {
            return Protocol.ERROR + " Invalid number: " + e.getMessage();
        } catch (final IllegalArgumentException e) {
            return Protocol.ERROR + " " + e.getMessage();
        } catch (final Exception e) {
            return Protocol.ERROR + " Unexpected error: " + e.getMessage();
        }
    }

    private String handleImport(final BufferedReader in,
                                final PrintWriter out) throws IOException {
        out.println(Protocol.OK + " Ready - send CSV lines then "
                + Protocol.END_FILE);

        final File tempFile =
                File.createTempFile("weather_import_", ".csv");
        tempFile.deleteOnExit();

        try {
            try (final PrintWriter fw =
                         new PrintWriter(new FileWriter(tempFile))) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (Protocol.END_FILE.equals(line.trim())) break;
                    fw.println(line);
                }
            }

            final ImportWeatherResult result =
                    importCtrl.importWeatherData(tempFile.getAbsolutePath());

            out.println(Protocol.OK
                    + " imported=" + result.importedCount()
                    + " rejected=" + result.rejectedCount());

            for (final String error : result.errors()) {
                out.println(Protocol.ERROR + " " + error);
            }
            out.println(Protocol.END);

        } catch (final IllegalArgumentException e) {
            out.println(Protocol.ERROR + " " + e.getMessage());
            out.println(Protocol.END);
        } finally {
            tempFile.delete();
        }

        return null;
    }

    private String handleConsult(final String args) {
        final String[] p = args.split(" ", 2);
        if (p.length < 2)
            return Protocol.ERROR
                    + " Usage: CONSULT_WEATHER <areaCode> <yyyy-MM-dd>";
        try {
            final String    areaCode = p[0];
            final LocalDate date     =
                    LocalDate.parse(p[1].trim(), DATE_FMT);

            final List<WeatherDataDTO> results =
                    consultCtrl.weatherDataByAreaAndDate(areaCode, date);

            if (results.isEmpty())
                return Protocol.OK
                        + " No weather data found for area '"
                        + areaCode + "' on " + date + "\n" + Protocol.END;

            final StringBuilder sb = new StringBuilder();
            for (final WeatherDataDTO dto : results) {
                sb.append(dto.toString()).append("\n");
            }
            sb.append(Protocol.END);
            return sb.toString();

        } catch (final DateTimeParseException e) {
            return Protocol.ERROR
                    + " Invalid date format (expected yyyy-MM-dd): "
                    + e.getMessage();
        } catch (final IllegalArgumentException e) {
            return Protocol.ERROR + " " + e.getMessage();
        } catch (final Exception e) {
            return Protocol.ERROR + " Unexpected error: " + e.getMessage();
        }
    }
}