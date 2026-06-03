package rcomp;

import controller.*;
import domain.Airport;
import domain.aircraftmodel.AircraftModel;
import domain.flightroute.FlightRoute;
import domain.flightroute.ScheduleEntry;
import domain.aircraft.Aircraft;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserSession;
import persistence.PersistenceContext;

import java.io.*;
import java.net.Socket;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ATCCClientHandler implements Runnable {

    private final Socket socket;
    private boolean authenticated = false;
    private String  companyIata   = null;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ATCCClientHandler(final Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                final BufferedReader in  = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                final PrintWriter    out = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            System.out.printf("[SERVER] Client connected: %s%n",
                    socket.getInetAddress());

            String line;
            while ((line = in.readLine()) != null) {
                final String response = handle(line.trim());
                out.println(response);
                if (response.equals(Protocol.BYE)) break;
            }

        } catch (final IOException e) {
            System.err.println("[SERVER] Client error: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (final IOException ignored) {}
            System.out.println("[SERVER] Client disconnected.");
        }
    }

    private String handle(final String line) {
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
                    case Protocol.LIST_AIRPORTS:    return handleListAirports();
                    case Protocol.LIST_MODELS:      return handleListModels();
                    case Protocol.ADD_AIRCRAFT:     return handleAddAircraft(args);
                    case Protocol.LIST_FLEET:       return handleListFleet(args);
                    case Protocol.LIST_FLEET_MODEL: return handleListFleetByModel(args);
                    case Protocol.LIST_FLEET_MAKER: return handleListFleetByMaker(args);
                    case Protocol.LIST_FLEET_CAP:   return handleListFleetByCap(args);
                    case Protocol.LIST_FLEET_AGE:   return handleListFleetByAge(args);
                    case Protocol.DECOMMISSION:     return handleDecommission(args);
                    case Protocol.CREATE_ROUTE:     return handleCreateRoute(args);
                    default:
                        return Protocol.ERROR + " Unknown command: " + cmd;
                }
        }
    }

    // ── LOGIN email password ───────────────────────────────────────────────

    private String handleLogin(final String args) {
        final String[] parts = args.split(" ", 2);
        if (parts.length < 2)
            return Protocol.ERROR + " Usage: LOGIN <email> <password>";

        final String email    = parts[0];
        final String password = parts[1];

        System.out.println("[SERVER] Login attempt: email=" + email);

        try {
            System.out.println("[SERVER] Calling authenticate...");
            final Optional<UserSession> session =
                    AuthzRegistry.authenticationService().authenticate(email, password);

            System.out.println("[SERVER] Session present: " + session.isPresent());

            if (session.isEmpty())
                return Protocol.ERROR + " Invalid credentials";

            final Optional<domain.User> domainUser = PersistenceContext.getInstance()
                    .userRepository()
                    .ofIdentity(domain.EmailAddress.valueOf(email));

            System.out.println("[SERVER] Domain user present: " + domainUser.isPresent());

            if (domainUser.isPresent()) {
                final LocalDate expiry =
                        domainUser.get().getSecurityClearanceExpiry();
                System.out.println("[SERVER] Clearance expiry: " + expiry);
                if (expiry != null && !expiry.isAfter(LocalDate.now())) {
                    AuthzRegistry.authorizationService().clearSession();
                    return Protocol.ERROR + " Security clearance expired";
                }
            }

            authenticated = true;
            return Protocol.OK + " Welcome " + email;

        } catch (final Exception e) {
            System.err.println("[SERVER] Login exception: "
                    + e.getClass().getName() + " — " + e.getMessage());
            e.printStackTrace();
            return Protocol.ERROR + " " + e.getMessage();
        }
    }

    // ── LOGOUT ────────────────────────────────────────────────────────────

    private String handleLogout() {
        authenticated = false;
        companyIata   = null;
        AuthzRegistry.authorizationService().clearSession();
        return Protocol.BYE;
    }

    // ── LIST_AIRPORTS ─────────────────────────────────────────────────────

    private String handleListAirports() {
        final StringBuilder sb = new StringBuilder();
        for (final Airport a : PersistenceContext.getInstance()
                .airportRepository().findAll()) {
            sb.append(a.iataCode().code())
                    .append("|")
                    .append(a.identity().code())
                    .append("|")
                    .append(a.name())
                    .append("\n");
        }
        sb.append(Protocol.END);
        return sb.toString();
    }

    // ── LIST_MODELS ───────────────────────────────────────────────────────

    private String handleListModels() {
        final StringBuilder sb = new StringBuilder();
        for (final AircraftModel m : PersistenceContext.getInstance()
                .aircraftModelRepository().findAll()) {
            sb.append(m.identity())
                    .append("|")
                    .append(m.modelName())
                    .append("|")
                    .append(m.manufacturer())
                    .append("|")
                    .append(m.type())
                    .append("\n");
        }
        sb.append(Protocol.END);
        return sb.toString();
    }

    // ── ADD_AIRCRAFT reg country iata modelId eco bus first crew ──────────

    private String handleAddAircraft(final String args) {
        final String[] p = args.split(" ");
        if (p.length < 8)
            return Protocol.ERROR +
                    " Usage: ADD_AIRCRAFT reg country iata modelId eco bus first crew";
        try {
            final String reg     = p[0];
            final String country = p[1];
            final String iata    = p[2];
            final long   modelId = Long.parseLong(p[3]);
            final int    eco     = Integer.parseInt(p[4]);
            final int    bus     = Integer.parseInt(p[5]);
            final int    first   = Integer.parseInt(p[6]);
            final int    crew    = Integer.parseInt(p[7]);

            final Aircraft aircraft =
                    new AddAircraftToFleetController()
                            .addAircraft(reg, country, iata, modelId,
                                    eco, bus, first, crew);

            return Protocol.OK + " " + aircraft.registration();
        } catch (final Exception e) {
            return Protocol.ERROR + " " + e.getMessage();
        }
    }

    // ── LIST_FLEET iata ───────────────────────────────────────────────────

    private String handleListFleet(final String args) {
        return buildFleetResponse(
                new ListCompanyFleetController().listFleet(args.trim()));
    }

    private String handleListFleetByModel(final String args) {
        final String[] p = args.split(" ", 2);
        if (p.length < 2)
            return Protocol.ERROR + " Usage: LIST_FLEET_MODEL iata modelId";
        try {
            return buildFleetResponse(
                    new ListCompanyFleetController()
                            .listByModel(p[0], Long.parseLong(p[1])));
        } catch (final Exception e) {
            return Protocol.ERROR + " " + e.getMessage();
        }
    }

    private String handleListFleetByMaker(final String args) {
        final String[] p = args.split(" ", 2);
        if (p.length < 2)
            return Protocol.ERROR + " Usage: LIST_FLEET_MAKER iata maker";
        return buildFleetResponse(
                new ListCompanyFleetController().listByMaker(p[0], p[1]));
    }

    private String handleListFleetByCap(final String args) {
        final String[] p = args.split(" ", 2);
        if (p.length < 2)
            return Protocol.ERROR + " Usage: LIST_FLEET_CAP iata minSeats";
        try {
            return buildFleetResponse(
                    new ListCompanyFleetController()
                            .listByCapacity(p[0], Integer.parseInt(p[1])));
        } catch (final Exception e) {
            return Protocol.ERROR + " " + e.getMessage();
        }
    }

    private String handleListFleetByAge(final String args) {
        return buildFleetResponse(
                new ListCompanyFleetController().listByAge(args.trim()));
    }

    private String buildFleetResponse(final Iterable<Aircraft> fleet) {
        final StringBuilder sb = new StringBuilder();
        for (final Aircraft a : fleet) {
            sb.append(a.registration())
                    .append("|")
                    .append(a.countryOfRegistration())
                    .append("|")
                    .append(a.companyIata())
                    .append("|")
                    .append(a.operationalStatus())
                    .append("|")
                    .append(a.cabinConfiguration().totalSeats())
                    .append("|")
                    .append(a.registrationDate())
                    .append("\n");
        }
        sb.append(Protocol.END);
        return sb.toString();
    }

    // ── DECOMMISSION reg companyIata ──────────────────────────────────────

    private String handleDecommission(final String args) {
        final String[] p = args.split(" ");
        if (p.length < 2)
            return Protocol.ERROR + " Usage: DECOMMISSION <reg> <companyIata>";
        try {
            new DecommissionAircraftController()
                    .decommissionAircraft(p[0].trim(), p[1].trim());
            return Protocol.OK + " " + p[0].trim() + " decommissioned";
        } catch (final Exception e) {
            return Protocol.ERROR + " " + e.getMessage();
        }
    }

    // ── CREATE_ROUTE ──────────────────────────────────────────────────────
    // REGULAR: CREATE_ROUTE designator companyIata origin dest REGULAR day dep arr
    // CHARTER: CREATE_ROUTE designator companyIata origin dest CHARTER date1 time1 date2 time2

    private String handleCreateRoute(final String args) {
        final String[] p = args.split(" ");
        if (p.length < 6)
            return Protocol.ERROR +
                    " Usage: CREATE_ROUTE designator companyIata origin dest type ...";
        try {
            final String designator  = p[0];
            final String company     = p[1];
            final String origin      = p[2];
            final String destination = p[3];
            final String type        = p[4].toUpperCase();

            final CreateFlightRouteController ctrl =
                    new CreateFlightRouteController();

            final FlightRoute route;

            if ("CHARTER".equals(type)) {
                final LocalDateTime dep =
                        LocalDateTime.parse(p[5] + " " + p[6], DT_FMT);
                final LocalDateTime arr =
                        LocalDateTime.parse(p[7] + " " + p[8], DT_FMT);
                route = ctrl.createCharterRoute(
                        designator, company, origin, destination, dep, arr);
            } else {
                final DayOfWeek day = DayOfWeek.valueOf(p[5].toUpperCase());
                final LocalTime dep = LocalTime.parse(p[6]);
                final LocalTime arr = LocalTime.parse(p[7]);
                final List<ScheduleEntry> entries = new ArrayList<>();
                entries.add(new ScheduleEntry(day, dep, arr));
                route = ctrl.createRegularRoute(
                        designator, company, origin, destination, entries);
            }

            return Protocol.OK + " " + route.identity().value();
        } catch (final Exception e) {
            return Protocol.ERROR + " " + e.getMessage();
        }
    }
}