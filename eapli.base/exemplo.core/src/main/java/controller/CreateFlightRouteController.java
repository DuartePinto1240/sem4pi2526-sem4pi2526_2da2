// ─── controller/CreateFlightRouteController.java ───────────────────────────

package controller;

import domain.Airport;
import domain.flightroute.*;
import persistence.PersistenceContext;
import repositories.AirportRepository;
import repositories.FlightRouteRepository;

import java.time.LocalDateTime;
import java.util.List;

public class CreateFlightRouteController {

    private final FlightRouteRepository routeRepo;
    private final AirportRepository     airportRepo;

    public CreateFlightRouteController() {
        this.routeRepo   = PersistenceContext.getInstance().flightRouteRepository();
        this.airportRepo = PersistenceContext.getInstance().airportRepository();
    }

    // package-private for testing
    CreateFlightRouteController(final FlightRouteRepository routeRepo,
                                final AirportRepository airportRepo) {
        this.routeRepo   = routeRepo;
        this.airportRepo = airportRepo;
    }

    public Iterable<Airport> availableAirports() {
        return airportRepo.findAll();
    }

    public FlightRoute createCharterRoute(final String designator,
                                          final String companyIata,
                                          final String originIata,
                                          final String destinationIata,
                                          final LocalDateTime departure,
                                          final LocalDateTime arrival) {
        checkDesignatorUniqueness(designator);
        final ScheduledDates dates = new ScheduledDates(departure, arrival);
        final FlightRoute route = new FlightRoute(
                designator, companyIata, originIata, destinationIata, dates);
        return routeRepo.save(route);
    }

    public FlightRoute createRegularRoute(final String designator,
                                          final String companyIata,
                                          final String originIata,
                                          final String destinationIata,
                                          final List<ScheduleEntry> entries) {
        checkDesignatorUniqueness(designator);
        final RecurringSchedule schedule = new RecurringSchedule(entries);
        final FlightRoute route = new FlightRoute(
                designator, companyIata, originIata, destinationIata, schedule);
        return routeRepo.save(route);
    }

    private void checkDesignatorUniqueness(final String designator) {
        if (routeRepo.existsByDesignator(designator)) {
            throw new IllegalArgumentException(
                    "A route with designator '" + designator + "' already exists.");
        }
    }
}