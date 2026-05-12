package repositories;

import lprog.FlightPlan;

import java.util.Optional;

public interface FlightPlanRepository {

    boolean existsByFlightId(String flightId);

    Optional<FlightPlan> findByFlightId(String flightId);

    Iterable<FlightPlan> findAll();

    <S extends FlightPlan> S save(S flightPlan);

    Optional<FlightPlan> ofIdentity(Long id);

    boolean containsOfIdentity(Long id);

    long count();
}