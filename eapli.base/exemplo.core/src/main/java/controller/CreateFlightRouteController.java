package controller;

import domain.AirTransportCompany;
import domain.Airport;
import domain.flightroute.*;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import persistence.PersistenceContext;
import repositories.AirportRepository;
import repositories.AirTransportCompanyRepository;
import repositories.FlightRouteRepository;

import java.time.LocalDateTime;
import java.util.List;

public class CreateFlightRouteController {

    private final FlightRouteRepository         routeRepo;
    private final AirportRepository             airportRepo;
    private final AirTransportCompanyRepository companyRepo;

    public CreateFlightRouteController() {
        this.routeRepo   = PersistenceContext.getInstance().flightRouteRepository();
        this.airportRepo = PersistenceContext.getInstance().airportRepository();
        this.companyRepo = PersistenceContext.getInstance()
                .airTransportCompanyRepository();
    }

    CreateFlightRouteController(final FlightRouteRepository routeRepo,
                                final AirportRepository airportRepo) {
        this.routeRepo   = routeRepo;
        this.airportRepo = airportRepo;
        this.companyRepo = PersistenceContext.getInstance()
                .airTransportCompanyRepository();
    }

    CreateFlightRouteController(final FlightRouteRepository routeRepo,
                                final AirportRepository airportRepo,
                                final AirTransportCompanyRepository companyRepo) {
        this.routeRepo   = routeRepo;
        this.airportRepo = airportRepo;
        this.companyRepo = companyRepo;
    }

    public String currentUserCompanyIata() {
        final String email = AuthzRegistry.authorizationService()
                .session()
                .map(s -> s.authenticatedUser().username().toString())
                .orElseThrow(() ->
                        new IllegalStateException("No authenticated user"));

        final AirTransportCompany company =
                companyRepo.findByCollaborator(email)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No company found for user: " + email));

        return company.companyCode().iata();
    }

    public Iterable<Airport> availableAirports() {
        return airportRepo.findAll();
    }

    public void validateDesignatorPrefix(final String designator,
                                         final String companyIata) {
        if (designator == null || designator.length() < 2)
            throw new IllegalArgumentException(
                    "Designator must have at least 2 characters.");
        final String prefix = designator.substring(0, 2).toUpperCase();
        if (!prefix.equals(companyIata.toUpperCase().trim()))
            throw new IllegalArgumentException(
                    "Designator prefix '" + prefix +
                            "' must match company IATA '" +
                            companyIata.toUpperCase() + "'.");
    }

    public FlightRoute createCharterRoute(final String designator,
                                          final String companyIata,
                                          final String originIata,
                                          final String destinationIata,
                                          final LocalDateTime departure,
                                          final LocalDateTime arrival) {
        checkDesignatorUniqueness(designator);
        validateDesignatorPrefix(designator, companyIata);
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
        validateDesignatorPrefix(designator, companyIata);
        final RecurringSchedule schedule = new RecurringSchedule(entries);
        final FlightRoute route = new FlightRoute(
                designator, companyIata, originIata, destinationIata, schedule);
        return routeRepo.save(route);
    }

    private void checkDesignatorUniqueness(final String designator) {
        if (routeRepo.existsByDesignator(designator)) {
            throw new IllegalArgumentException(
                    "A route with designator '" + designator +
                            "' already exists.");
        }
    }
}