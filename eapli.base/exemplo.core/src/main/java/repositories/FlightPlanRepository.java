package repositories;

import domain.flightroute.RouteDesignator;
import lprog.FlightPlan;

import java.time.LocalDate;
import java.util.Optional;

public interface FlightPlanRepository {

    boolean existsByFlightId(String flightId);

    Optional<FlightPlan> findByFlightId(String flightId);

    Iterable<FlightPlan> findAll();

    <S extends FlightPlan> S save(S flightPlan);

    Optional<FlightPlan> ofIdentity(Long id);

    boolean containsOfIdentity(Long id);

    long count();

    /**
     * US077 — Returns true if the pilot has any flight plans currently assigned.
     * Used to block pilot deactivation.
     */
    boolean existsAssignedFlightPlansForPilot(String pilotEmail);

    /**
     * US074 — Returns true if planned flights exist on the given route
     * after the specified deactivation date. Used to block route deactivation.
     */
    boolean existsPlannedFlightsAfter(RouteDesignator routeDesignator, LocalDate deactivationDate);

    /**
     * US082 — Returns all flight plans belonging to the pilot with the given email.
     */
    Iterable<FlightPlan> findByPilot(String pilotEmail);
}