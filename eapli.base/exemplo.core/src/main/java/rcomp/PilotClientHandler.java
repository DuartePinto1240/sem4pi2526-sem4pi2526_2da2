package rcomp;

import controller.ImportFlightPlanController;
import controller.InsertWeatherDataController;
import controller.TestFlightPlanController;
import domain.BaseRoles;
import dto.AirControlAreaDTO;
import dto.FlightPlanDTO;
import dto.WeatherDataDTO;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserSession;
import lprog.FlightPlan;
import lprog.FlightPlanTestResult;
import rcomp.logging.EventType;
import rcomp.logging.RemoteAccessLogger;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Optional;

/**
 * Handles one Pilot TCP connection (US86).
 *
 * <p>Text protocol — one command per line:</p>
 * <pre>
 *   LOGIN &lt;email&gt; &lt;password&gt;
 *   LIST_DRAFT_PLANS
 *   LIST_ALL_PLANS
 *   IMPORT_FLIGHT_PLAN   (server replies OK, client sends DSL lines + END_FILE)
 *   LIST_WEATHER_AREAS
 *   LIST_WEATHER_DATA &lt;areaCode&gt;
 *   INSERT_WEATHER &lt;flightPlanId&gt; &lt;weatherDataId&gt;
 *   TEST_FLIGHT_PLAN &lt;flightPlanId&gt;
 *   LOGOUT
 * </pre>
 */
public class PilotClientHandler implements Runnable {

    private final Socket socket;
    private final TestFlightPlanController    testCtrl;
    private final ImportFlightPlanController  importCtrl;
    private final InsertWeatherDataController weatherCtrl;
    private final RemoteAccessLogger logger = new RemoteAccessLogger("localhost");

    private boolean authenticated      = false;
    private String  authenticatedEmail = null;

    /** Production constructor — creates controllers internally. */
    public PilotClientHandler(final Socket socket) {
        this.socket      = socket;
        this.testCtrl    = new TestFlightPlanController();
        this.importCtrl  = new ImportFlightPlanController();
        this.weatherCtrl = new InsertWeatherDataController();
    }

    /** Test constructor — injects controllers and optionally pre-authenticates. */
    PilotClientHandler(final Socket socket,
                       final TestFlightPlanController testCtrl,
                       final ImportFlightPlanController importCtrl,
                       final InsertWeatherDataController weatherCtrl,
                       final boolean preAuthenticated,
                       final String preAuthEmail) {
        this.socket           = socket;
        this.testCtrl         = testCtrl;
        this.importCtrl       = importCtrl;
        this.weatherCtrl      = weatherCtrl;
        this.authenticated    = preAuthenticated;
        this.authenticatedEmail = preAuthEmail;
    }

    @Override
    public void run() {
        try (
            final BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final PrintWriter    out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            System.out.printf("[PILOT-SERVER] Client connected: %s%n", socket.getInetAddress());

            String line;
            while ((line = in.readLine()) != null) {
                final String response = handle(line.trim(), in, out);
                if (response != null) {
                    out.println(response);
                    // Auto-append END for single-line responses that don't have it yet
                    if (!response.endsWith(Protocol.END) && !Protocol.BYE.equals(response)) {
                        out.println(Protocol.END);
                    }
                }
                if (Protocol.BYE.equals(response)) break;
            }

        } catch (final IOException e) {
            System.err.println("[PILOT-SERVER] Client error: " + e.getMessage());
        } finally {
            if (authenticated && authenticatedEmail != null) {
                logger.log(EventType.DISCONNECT, "US86", authenticatedEmail,
                        socket.getInetAddress().getHostAddress(), socket.getPort());
            }
            try { socket.close(); } catch (final IOException ignored) {}
            System.out.println("[PILOT-SERVER] Client disconnected.");
        }
    }

    // ── Dispatcher ────────────────────────────────────────────────────────────

    private String handle(final String line, final BufferedReader in, final PrintWriter out)
            throws IOException {
        if (line.isEmpty()) return Protocol.ERROR + " Empty command";

        final String[] parts = line.split(" ", 2);
        final String cmd  = parts[0].toUpperCase();
        final String args = parts.length > 1 ? parts[1] : "";

        if (Protocol.LOGIN.equals(cmd))  return handleLogin(args);
        if (Protocol.LOGOUT.equals(cmd)) return handleLogout();

        if (!authenticated)
            return Protocol.ERROR + " Not authenticated. Use: LOGIN <email> <password>";

        switch (cmd) {
            case Protocol.LIST_DRAFT_PLANS:   return handleListDraftPlans();
            case Protocol.LIST_ALL_PLANS:     return handleListAllPlans();
            case Protocol.IMPORT_FLIGHT_PLAN: return handleImportFlightPlan(in, out);
            case Protocol.LIST_WEATHER_AREAS: return handleListWeatherAreas();
            case Protocol.LIST_WEATHER_DATA:  return handleListWeatherData(args);
            case Protocol.INSERT_WEATHER:     return handleInsertWeather(args);
            case Protocol.TEST_FLIGHT_PLAN:   return handleTestFlightPlan(args);
            default:
                return Protocol.ERROR + " Unknown command: " + cmd;
        }
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    private String handleLogin(final String args) {
        final String[] p = args.split(" ", 2);
        if (p.length < 2)
            return Protocol.ERROR + " Usage: LOGIN <email> <password>";

        final String email    = p[0];
        final String password = p[1];

        System.out.println("[PILOT-SERVER] Login attempt: " + email);
        try {
            final Optional<UserSession> session =
                    AuthzRegistry.authenticationService().authenticate(email, password);

            if (session.isEmpty()) {
                logger.log(EventType.LOGIN_FAIL, "US86",
                        email, socket.getInetAddress().getHostAddress(), socket.getPort());
                return Protocol.ERROR + " Invalid credentials";
            }

            if (!session.get().authenticatedUser().hasAny(BaseRoles.PILOT)) {
                AuthzRegistry.authorizationService().clearSession();
                return Protocol.ERROR + " Access denied: not a Pilot";
            }

            authenticated      = true;
            authenticatedEmail = email;
            System.out.println("[PILOT-SERVER] Login OK: " + email);
            logger.log(EventType.LOGIN_SUCCESS, "US86", email,
                    socket.getInetAddress().getHostAddress(), socket.getPort());

            return Protocol.OK + " Welcome " + email;

        } catch (final Exception e) {
            return Protocol.ERROR + " " + e.getMessage();
        }
    }

    // ── LOGOUT ────────────────────────────────────────────────────────────────

    private String handleLogout() {
        if (authenticatedEmail != null) {
            logger.log(EventType.LOGOUT, "US86", authenticatedEmail,
                    socket.getInetAddress().getHostAddress(), socket.getPort());
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

    // ── LIST_DRAFT_PLANS ──────────────────────────────────────────────────────

    private String handleListDraftPlans() {
        try {
            final List<FlightPlan> plans =
                    testCtrl.listDraftFlightPlans(authenticatedEmail);
            if (plans.isEmpty())
                return Protocol.OK + " No DRAFT flight plans\n" + Protocol.END;

            final StringBuilder sb = new StringBuilder();
            for (final FlightPlan p : plans) {
                sb.append(p.flightId()).append("|")
                  .append(p.status()).append("|")
                  .append(p.legs().size()).append(" leg(s)\n");
            }
            sb.append(Protocol.END);
            return sb.toString();
        } catch (final Exception e) {
            return Protocol.ERROR + " " + e.getMessage();
        }
    }

    // ── LIST_ALL_PLANS ────────────────────────────────────────────────────────

    private String handleListAllPlans() {
        try {
            final List<FlightPlanDTO> plans =
                    weatherCtrl.getPilotFlightPlans(authenticatedEmail);
            if (plans.isEmpty())
                return Protocol.OK + " No flight plans found\n" + Protocol.END;

            final StringBuilder sb = new StringBuilder();
            for (final FlightPlanDTO p : plans) {
                sb.append(p.flightId).append("|")
                  .append(p.flightType).append("|")
                  .append(p.status).append("\n");
            }
            sb.append(Protocol.END);
            return sb.toString();
        } catch (final Exception e) {
            return Protocol.ERROR + " " + e.getMessage();
        }
    }

    // ── IMPORT_FLIGHT_PLAN ────────────────────────────────────────────────────
    // Flow:
    //   Client → IMPORT_FLIGHT_PLAN
    //   Server → OK Ready - send DSL lines then END_FILE
    //   Client → <line1> ... <lineN> END_FILE
    //   Server → OK Flight plan imported: <id>   OR   ERROR <details>

    private String handleImportFlightPlan(final BufferedReader in, final PrintWriter out)
            throws IOException {
        out.println(Protocol.OK + " Ready - send DSL lines then " + Protocol.END_FILE);

        // Accumulate DSL into a string
        final StringBuilder dsl = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            if (Protocol.END_FILE.equals(line.trim())) break;
            dsl.append(line).append("\n");
        }

        try {
            final lprog.FlightPlanImporter.FlightPlanImportResult result =
                    importCtrl.importFromDsl(dsl.toString(), authenticatedEmail);

            final StringBuilder sb = new StringBuilder();

            if (result.isSuccess()) {
                final java.util.List<String> skipped = importCtrl.lastSkippedIds();
                final java.util.List<lprog.FlightPlanBuilderListener.FlightData> imported =
                        result.getFlights().stream()
                                .filter(f -> !skipped.contains(f.id))
                                .collect(java.util.stream.Collectors.toList());

                if (imported.isEmpty()) {
                    sb.append(Protocol.ERROR).append(" Flight plan(s) already exist in the system:\n");
                    skipped.forEach(id -> sb.append("  - ").append(id).append("\n"));
                    sb.append("  Use a different flight ID or delete the existing plan first.\n");
                } else {
                    sb.append(Protocol.OK).append(" Flight plan(s) imported successfully:\n");
                    for (final lprog.FlightPlanBuilderListener.FlightData fd : imported) {
                        sb.append("  Flight ID : ").append(fd.id).append("\n");
                        sb.append("  Type      : ").append(fd.type).append("\n");
                        final java.util.List<lprog.FlightPlanBuilderListener.LegData> legs =
                            fd.legs != null ? fd.legs : java.util.Collections.emptyList();
                        sb.append("  Legs      : ").append(legs.size()).append("\n");
                        for (int i = 0; i < legs.size(); i++) {
                            final lprog.FlightPlanBuilderListener.LegData leg = legs.get(i);
                            sb.append(String.format("    Leg %d   : %s -> %s  (fuel: %.0f %s)%n",
                                    i + 1, leg.departureAirport, leg.arrivalAirport,
                                    leg.fuelQuantity, leg.fuelUnit));
                        }
                        sb.append("  Status    : DRAFT\n");
                    }
                    if (!skipped.isEmpty())
                        sb.append("  (skipped duplicates: ").append(String.join(", ", skipped)).append(")\n");
                }
            } else {
                sb.append(Protocol.ERROR).append(" Invalid flight plan. Errors found:\n");
                result.getErrors().forEach(e -> sb.append("  ").append(e).append("\n"));
            }

            sb.append(Protocol.END);
            out.println(sb.toString());

        } catch (final Exception e) {
            out.println(Protocol.ERROR + " " + e.getMessage() + "\n" + Protocol.END);
        }
        return null; // already wrote directly
    }

    // ── LIST_WEATHER_AREAS ────────────────────────────────────────────────────

    private String handleListWeatherAreas() {
        try {
            final List<AirControlAreaDTO> areas =
                    weatherCtrl.getAvailableAreas();
            if (areas.isEmpty())
                return Protocol.OK + " No areas found\n" + Protocol.END;

            final StringBuilder sb = new StringBuilder();
            for (final AirControlAreaDTO a : areas) {
                sb.append(a.areaCode).append("|").append(a.name).append("\n");
            }
            sb.append(Protocol.END);
            return sb.toString();
        } catch (final Exception e) {
            return Protocol.ERROR + " " + e.getMessage();
        }
    }

    // ── LIST_WEATHER_DATA ─────────────────────────────────────────────────────

    private String handleListWeatherData(final String args) {
        if (args.isBlank())
            return Protocol.ERROR + " Usage: LIST_WEATHER_DATA <areaCode>";
        try {
            final List<WeatherDataDTO> list =
                    weatherCtrl.getAvailableWeatherData(args.trim());
            if (list.isEmpty())
                return Protocol.OK + " No weather data for area " + args + "\n" + Protocol.END;

            final StringBuilder sb = new StringBuilder();
            for (final WeatherDataDTO w : list) {
                sb.append(w.id).append("|").append(w.toString()).append("\n");
            }
            sb.append(Protocol.END);
            return sb.toString();
        } catch (final Exception e) {
            return Protocol.ERROR + " " + e.getMessage();
        }
    }

    // ── INSERT_WEATHER ────────────────────────────────────────────────────────
    // Args: <flightPlanId> <weatherDataId>

    private String handleInsertWeather(final String args) {
        final String[] p = args.split(" ");
        if (p.length < 2)
            return Protocol.ERROR + " Usage: INSERT_WEATHER <flightPlanId> <weatherDataId>";
        try {
            final String flightPlanId  = p[0];
            final Long   weatherDataId = Long.parseLong(p[1]);
            weatherCtrl.insertWeatherDataByFlightId(flightPlanId, weatherDataId);
            return Protocol.OK + " Weather data inserted into flight plan '" + flightPlanId + "'";
        } catch (final NumberFormatException e) {
            return Protocol.ERROR + " Weather data ID must be numeric";
        } catch (final IllegalArgumentException e) {
            return Protocol.ERROR + " " + e.getMessage();
        } catch (final Exception e) {
            return Protocol.ERROR + " Unexpected error: " + e.getMessage();
        }
    }

    // ── TEST_FLIGHT_PLAN ──────────────────────────────────────────────────────
    // Args: <flightPlanId (string)>

    private String handleTestFlightPlan(final String args) {
        if (args.isBlank())
            return Protocol.ERROR + " Usage: TEST_FLIGHT_PLAN <flightPlanId>";
        try {
            final FlightPlanTestResult result =
                    testCtrl.testFlightPlan(args.trim());

            if (result.isPassed()) {
                final StringBuilder sb = new StringBuilder();
                sb.append(Protocol.OK).append(" PASS - Flight plan '").append(args.trim()).append("' is valid and feasible\n");
                sb.append(String.format("  Total distance   : %.0f km%n", result.totalDistanceKm()));
                sb.append(String.format("  Estimated flight : %s%n",      result.estimatedDuration()));
                sb.append(String.format("  Declared fuel    : %.0f kg%n", result.totalDeclaredFuelKg()));
                sb.append(String.format("  Estimated burn   : %.0f kg%n", result.estimatedFuelBurnKg()));
                sb.append(String.format("  Remaining fuel   : %.0f kg%n", result.remainingFuelKg()));
                sb.append(Protocol.END);
                return sb.toString();
            } else {
                final StringBuilder sb = new StringBuilder(Protocol.ERROR + " FAIL:\n");
                result.errors().forEach(e -> sb.append("  • ").append(e).append("\n"));
                sb.append(Protocol.END);
                return sb.toString();
            }

        } catch (final IllegalArgumentException e) {
            return Protocol.ERROR + " " + e.getMessage();
        } catch (final java.io.IOException e) {
            return Protocol.ERROR + " Could not run feasibility checker: " + e.getMessage();
        } catch (final Exception e) {
            return Protocol.ERROR + " Unexpected error: " + e.getMessage();
        }
    }
}
