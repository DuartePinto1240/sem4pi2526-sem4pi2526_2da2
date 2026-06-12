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
     * Returns the authenticated user's email from the session.
     * Called by the UI before passing to getCompanyRoutes().
     */
    public String authenticatedUserEmail() {
        return AuthzRegistry.authorizationService()
                .session()
                .map(s -> s.authenticatedUser().username().toString())
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }

    /**
     * Returns all active CHARTER routes for the company of the given collaborator.
     * REGULAR routes are excluded — they cannot be deactivated.
     */
    public List<FlightRouteDTO> getCompanyRoutes(final String collaboratorEmail) {
        final AirTransportCompany company = findByCollaborator(collaboratorEmail);
        return listActiveCharterRoutes(company);
    }

    private AirTransportCompany findByCollaborator(final String collaboratorEmail) {
        return atcRepo.findByCollaborator(collaboratorEmail)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No company found for collaborator " + collaboratorEmail));
    }

    /**
     * Returns only active CHARTER routes — REGULAR routes cannot be deactivated
     * so they are excluded from the listing entirely.
     */
    private List<FlightRouteDTO> listActiveCharterRoutes(
            final AirTransportCompany company) {
        final List<FlightRouteDTO> result = new ArrayList<>();
        for (final FlightRoute r : routeRepo.findByCompanyIata(
                company.companyCode().iata())) {
            if (r.isActive() && r.flightType().isCharter()) {
                result.add(FlightRouteDTO.from(r));
            }
        }
        return result;
    }

    /**
     * Deactivates a CHARTER flight route from the given date onwards.
     *
     * @param routeName        the route designator
     * @param deactivationDate date from which the route becomes inactive
     * @throws IllegalArgumentException if the route is REGULAR, not found,
     *                                  or has planned flights after the date
     */
    public void deleteRoute(final String routeName,
                            final LocalDate deactivationDate) {
        final FlightRoute route = findRouteByDesignator(routeName);

        // REGULAR routes cannot be deactivated
        if (route.flightType().isRegular()) {
            throw new IllegalArgumentException(
                    "Cannot deactivate regular route '" + routeName
                            + "'. Only charter routes can be deactivated.");
        }

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

    private void deactivateRoute(final FlightRoute route,
                                 final LocalDate deactivationDate) {
        route.deactivate(deactivationDate);
        routeRepo.save(route);
    }
}