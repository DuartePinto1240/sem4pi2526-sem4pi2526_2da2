package persistence.impl.jpa;

import domain.flightroute.RouteDesignator;
import lprog.FlightPlan;
import lprog.FlightPlanStatus;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import repositories.FlightPlanRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"rawtypes", "unchecked"})
public class JpaFlightPlanRepository
        extends JpaAutoTxRepository<FlightPlan, Long, Long>
        implements FlightPlanRepository {

    public JpaFlightPlanRepository(final String persistenceUnitName,
                                   final Map extendedProperties) {
        super(persistenceUnitName, extendedProperties, "id");
    }

    @Override
    public boolean existsByFlightId(final String flightId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("flightId", flightId.trim());
        return matchOne("e.flightId = :flightId", params).isPresent();
    }

    @Override
    public Optional<FlightPlan> findByFlightId(final String flightId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("flightId", flightId.trim());
        return matchOne("e.flightId = :flightId", params);
    }

    @Override
    public Iterable<FlightPlan> findAll() {
        return match("1=1", new HashMap<>());
    }

    @Override
    public Optional<FlightPlan> ofIdentity(final Long id) {
        return super.ofIdentity(id);
    }

    @Override
    public boolean containsOfIdentity(final Long id) {
        return ofIdentity(id).isPresent();
    }

    @Override
    public long count() {
        return super.count();
    }

    @Override
    public <S extends FlightPlan> S save(final S flightPlan) {
        return super.save(flightPlan);
    }

    @Override
    public boolean existsAssignedFlightPlansForPilot(final String pilotEmail) {
        return match("e.pilotId IS NOT NULL", new HashMap<>())
                .iterator().hasNext();
    }

    /**
     * US074 — Returns true if any flight plan exists for the given route
     * with a leg departure date after the given deactivation date.
     *
     * Uses JOIN instead of MEMBER OF subquery to ensure correct JPQL execution.
     * departureDate is stored as String (yyyy-MM-dd) — lexicographic comparison
     * works correctly for ISO-8601 date strings.
     */
    @Override
    public boolean existsPlannedFlightsAfter(final RouteDesignator routeDesignator,
                                             final LocalDate deactivationDate) {
        final String jpql =
                "SELECT COUNT(fp) FROM FlightPlan fp " +
                        "JOIN fp.legs l " +
                        "WHERE UPPER(fp.routeDesignator) = :routeDesignator " +
                        "AND l.departureDate > :deactivationDate";

        final Long count = (Long) createQuery(jpql, Long.class)
                .setParameter("routeDesignator",
                        routeDesignator.value().toUpperCase().trim())
                .setParameter("deactivationDate",
                        deactivationDate.toString())
                .getSingleResult();

        return count != null && count > 0;
    }

    @Override
    public Iterable<FlightPlan> findByPilot(final String pilotEmail) {
        return match("1=1", new HashMap<>());
    }

    @Override
    public Iterable<FlightPlan> findByPilotAndStatus(final String pilotEmail,
                                                     final FlightPlanStatus status) {
        final Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        return match("e.status = :status", params);
    }
}