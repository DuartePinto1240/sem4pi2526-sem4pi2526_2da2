package controller;

import domain.AirControlArea;
import domain.AirControlAreaCode;
import domain.Airport;
import domain.GeographicBoundary;
import domain.IATACode;
import domain.aircraft.Aircraft;
import domain.flightroute.FlightRoute;
import domain.pilot.Pilot;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import lprog.FlightLeg;
import lprog.FlightPlan;
import lprog.FlightPlanDslGenerator;
import lprog.FlightSegment;
import persistence.PersistenceContext;
import repositories.AirControlAreaRepository;
import repositories.AircraftRepository;
import repositories.AirportRepository;
import repositories.FlightPlanRepository;
import repositories.FlightRouteRepository;
import repositories.PilotRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Application controller for US080 — Create a Flight Plan manually.
 *
 * Enforces:
 *   - Route must exist, be active, and belong to the pilot's company.
 *   - Aircraft must belong to the route's company.
 *   - Pilot must belong to the route's company.
 *   - Departure date/time must be in the future.
 *   - Fuel quantity must be positive (delegated to FlightLeg).
 *   - At least one leg must be provided (delegated to FlightPlan).
 *   - All segment coordinates must lie within the AirControlArea of the route's airports.
 */
public class CreateFlightPlanController {

    private final FlightRouteRepository    flightRouteRepo;
    private final AircraftRepository       aircraftRepo;
    private final PilotRepository          pilotRepo;
    private final FlightPlanRepository     flightPlanRepo;
    private final AirportRepository        airportRepo;
    private final AirControlAreaRepository areaRepo;

    public CreateFlightPlanController() {
        this.flightRouteRepo = PersistenceContext.getInstance().flightRouteRepository();
        this.aircraftRepo    = PersistenceContext.getInstance().aircraftRepository();
        this.pilotRepo       = PersistenceContext.getInstance().pilotRepository();
        this.flightPlanRepo  = PersistenceContext.getInstance().flightPlanRepository();
        this.airportRepo     = PersistenceContext.getInstance().airportRepository();
        this.areaRepo        = PersistenceContext.getInstance().airControlAreaRepository();
    }

    // Package-private for unit testing
    CreateFlightPlanController(final FlightRouteRepository    flightRouteRepo,
                               final AircraftRepository       aircraftRepo,
                               final PilotRepository          pilotRepo,
                               final FlightPlanRepository     flightPlanRepo,
                               final AirportRepository        airportRepo,
                               final AirControlAreaRepository areaRepo) {
        this.flightRouteRepo = flightRouteRepo;
        this.aircraftRepo    = aircraftRepo;
        this.pilotRepo       = pilotRepo;
        this.flightPlanRepo  = flightPlanRepo;
        this.airportRepo     = airportRepo;
        this.areaRepo        = areaRepo;
    }

    // -------------------------------------------------------------------------
    // Query helpers
    // -------------------------------------------------------------------------

    /**
     * Returns only active routes that belong to the pilot's company.
     * Called by the UI so the route list is already filtered — the pilot
     * cannot accidentally select a route from another company.
     */
    public Iterable<FlightRoute> activeRoutesForPilot(final Pilot pilot) {
        final String pilotIata = pilot.company().companyCode().iata();
        final Iterable<FlightRoute> all = flightRouteRepo.findAll();
        final List<FlightRoute> result = new ArrayList<>();
        for (final FlightRoute r : all) {
            if (r.isActive() && r.companyIata().equalsIgnoreCase(pilotIata)) {
                result.add(r);
            }
        }
        return result;
    }

    public Iterable<Aircraft> aircraftForRoute(final FlightRoute route) {
        return aircraftRepo.findByCompanyIata(route.companyIata());
    }

    /**
     * Returns the Pilot domain entity for the currently authenticated user,
     * matched by email. Returns empty if no matching Pilot is found.
     */
    public Optional<Pilot> currentPilot() {
        return AuthzRegistry.authorizationService().session()
                .flatMap(session -> {
                    final String email = session.authenticatedUser().username().toString();
                    return pilotRepo.findByEmail(email);
                });
    }

    /**
     * Resolves the AirControlArea boundary for a given airport IATA code.
     * Returns empty if the airport or its area is not found.
     */
    public Optional<GeographicBoundary> boundaryForAirport(final String iataCode) {
        return airportRepo.findByIataCode(new IATACode(iataCode))
                .flatMap(airport -> areaRepo.ofIdentity(airport.airControlAreaCode()))
                .map(AirControlArea::boundary);
    }

    /**
     * Validates that all segment coordinates in a single leg lie within the
     * AirControlArea boundary of the leg's departure airport.
     * Throws {@link IllegalArgumentException} with a descriptive message on the
     * first out-of-bounds coordinate found.
     * Does nothing if the airport / its area is not registered.
     */
    public void validateLegCoordinates(final FlightLeg leg) {
        final Optional<GeographicBoundary> boundaryOpt = boundaryForAirport(leg.departureAirport());
        if (boundaryOpt.isEmpty()) return;
        final GeographicBoundary boundary = boundaryOpt.get();
        int segIdx = 1;
        for (final lprog.FlightSegment seg : leg.segments()) {
            if (!isWithin(seg.fromLat(), seg.fromLon(), boundary)) {
                throw new IllegalArgumentException(String.format(
                        "Segment %d: 'from' (%.4f, %.4f) fora do limite da área de controlo %s.",
                        segIdx, seg.fromLat(), seg.fromLon(), boundary));
            }
            if (!isWithin(seg.toLat(), seg.toLon(), boundary)) {
                throw new IllegalArgumentException(String.format(
                        "Segment %d: 'to' (%.4f, %.4f) fora do limite da área de controlo %s.",
                        segIdx, seg.toLat(), seg.toLon(), boundary));
            }
            segIdx++;
        }
    }

    // -------------------------------------------------------------------------
    // Creation
    // -------------------------------------------------------------------------

    /**
     * Validates all business rules, persists the flight plan, and auto-generates its DSL.
     *
     * @return the persisted FlightPlan (status DRAFT, dslContent set)
     * @throws IllegalArgumentException if any business rule is violated
     */
    public FlightPlan createFlightPlan(final String          flightId,
                                       final FlightRoute      route,
                                       final Aircraft         aircraft,
                                       final Pilot            pilot,
                                       final LocalDateTime    departureDateTime,
                                       final List<FlightLeg>  legs) {

        // US080.0 — flight ID must be unique (one plan per flight, no different pilots/aircraft)
        if (flightPlanRepo.existsByFlightId(flightId)) {
            throw new IllegalArgumentException(
                    "A flight plan for flight '" + flightId + "' already exists.");
        }

        // US080.1 — route must be active
        if (!route.isActive()) {
            throw new IllegalArgumentException(
                    "Route " + route.identity() + " is not active.");
        }

        // US080.2 — aircraft must belong to the route's company
        if (!aircraft.companyIata().equalsIgnoreCase(route.companyIata())) {
            throw new IllegalArgumentException(
                    "Aircraft " + aircraft.registration()
                            + " does not belong to company " + route.companyIata() + ".");
        }

        // US080.3 — pilot must belong to the route's company
        if (!pilot.company().companyCode().iata().equalsIgnoreCase(route.companyIata())) {
            throw new IllegalArgumentException(
                    "Pilot " + pilot.name()
                            + " does not belong to company " + route.companyIata() + ".");
        }

        // US080.4 — departure must be in the future
        if (!departureDateTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Departure date/time must be in the future.");
        }

        // US080.8 — validate segment coordinates against air control area boundaries
        validateSegmentCoordinates(legs);

        // Generate DSL from the raw legs list BEFORE creating the FlightPlan entity
        // to avoid any JPA collection lifecycle interference.
        final String dsl = new FlightPlanDslGenerator().generate(
                flightId, route.flightType().toString(), legs);

        // US080.6/7 — delegate remaining validation to the aggregate
        final FlightPlan plan = new FlightPlan(
                flightId,
                route.flightType().toString(),
                legs,
                route.identity().toString(),
                aircraft.registration(),
                pilot.identity());

        plan.setDslContent(dsl);
        plan.setPilotEmail(pilot.email());
        return flightPlanRepo.save(plan);
    }

    /**
     * Validates that each segment's from/to coordinates lie within the
     * AirControlArea boundary of the leg's departure airport.
     *
     * If the airport or its area is not registered in the system, the check
     * is skipped for that leg (the airport might be bootstrapped without area data).
     */
    private void validateSegmentCoordinates(final List<FlightLeg> legs) {
        for (final FlightLeg leg : legs) {
            final Optional<GeographicBoundary> boundaryOpt =
                    boundaryForAirport(leg.departureAirport());

            if (boundaryOpt.isEmpty()) {
                // area not found — skip validation for this leg
                continue;
            }

            final GeographicBoundary boundary = boundaryOpt.get();
            int segIdx = 1;
            for (final FlightSegment seg : leg.segments()) {
                if (!isWithin(seg.fromLat(), seg.fromLon(), boundary)) {
                    throw new IllegalArgumentException(String.format(
                            "Leg %s, segment %d: 'from' coordinate (%.4f, %.4f) is outside "
                            + "the air control area boundary %s.",
                            leg.departureAirport(), segIdx,
                            seg.fromLat(), seg.fromLon(), boundary));
                }
                if (!isWithin(seg.toLat(), seg.toLon(), boundary)) {
                    throw new IllegalArgumentException(String.format(
                            "Leg %s, segment %d: 'to' coordinate (%.4f, %.4f) is outside "
                            + "the air control area boundary %s.",
                            leg.departureAirport(), segIdx,
                            seg.toLat(), seg.toLon(), boundary));
                }
                segIdx++;
            }
        }
    }

    private boolean isWithin(final double lat, final double lon,
                              final GeographicBoundary boundary) {
        return lat >= boundary.minLat() && lat <= boundary.maxLat()
            && lon >= boundary.minLon() && lon <= boundary.maxLon();
    }
}
