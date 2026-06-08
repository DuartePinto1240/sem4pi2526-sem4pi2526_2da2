package controller;

import domain.AirTransportCompany;
import domain.flightroute.FlightRoute;
import domain.flightroute.RouteDesignator;
import dto.FlightRouteDTO;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import persistence.PersistenceContext;
import repositories.AirTransportCompanyRepository;
import repositories.FlightPlanRepository;
import repositories.FlightRouteRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DeleteFlightRouteController {

    private final FlightRouteRepository         routeRepo;
    private final FlightPlanRepository          flightPlanRepo;
    private final AirTransportCompanyRepository atcRepo;

    public DeleteFlightRouteController() {
        this.routeRepo      = PersistenceContext.getInstance().flightRouteRepository();
        this.flightPlanRepo = PersistenceContext.getInstance().flightPlanRepository();
        this.atcRepo        = PersistenceContext.getInstance().airTransportCompanyRepository();
    }

    // Package-private constructor for unit testing
    DeleteFlightRouteController(final FlightRouteRepository routeRepo,
                                final FlightPlanRepository flightPlanRepo,
                                final AirTransportCompanyRepository atcRepo) {
        this.routeRepo      = routeRepo;
        this.flightPlanRepo = flightPlanRepo;
        this.atcRepo        = atcRepo;
    }

    /**
     * Returns all active routes for the company of the given collaborator email.
     */
    public List<FlightRouteDTO> getCompanyRoutes(final String collaboratorEmail) {
        final AirTransportCompany company = findByCollaborator(collaboratorEmail);
        List<FlightRouteDTO> activeRoutes = new ArrayList<>();
        for (FlightRoute fr : listActiveRoutes(company)) {
            activeRoutes.add(FlightRouteDTO.from(fr));
        }
        return activeRoutes;
    }

    private AirTransportCompany findByCollaborator(final String collaboratorEmail) {
        return atcRepo.findByCollaborator(collaboratorEmail)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No company found for collaborator " + collaboratorEmail));
    }

    private List<FlightRoute> listActiveRoutes(final AirTransportCompany company) {
        final List<FlightRoute> activeRoutes = new ArrayList<>();
        for (final FlightRoute r : routeRepo.findByCompanyIata(
                company.companyCode().iata())) {
            if (r.isActive()) {
                activeRoutes.add(r);
            }
        }
        return activeRoutes;
    }

    /**
     * Deactivates a flight route from the given date onwards.
     */
    public void deleteRoute(final String routeName, final LocalDate deactivationDate) {
        final FlightRoute route = findRouteByDesignator(routeName);

        if (existsPlannedFlightsAfter(route.identity(), deactivationDate)) {
            throw new IllegalArgumentException(
                    "Cannot deactivate route '" + routeName
                            + "': planned flights exist after " + deactivationDate + ".");
        }

        deactivateRoute(route, deactivationDate);
    }

    private FlightRoute findRouteByDesignator(final String designator) {
        return routeRepo.ofIdentity(new RouteDesignator(designator))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Route not found: " + designator));
    }

    private boolean existsPlannedFlightsAfter(final RouteDesignator routeDesignator,
                                              final LocalDate date) {
        return flightPlanRepo.existsPlannedFlightsAfter(routeDesignator, date);
    }

    private void deactivateRoute(final FlightRoute route, final LocalDate deactivationDate) {
        route.deactivate(deactivationDate);
        routeRepo.save(route);
    }
}