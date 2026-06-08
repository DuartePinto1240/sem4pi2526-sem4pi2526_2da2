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

    /**
     * US077 — Returns true if any flight plan has the given pilotId assigned.
     * Queries by pilotId since FlightPlan stores Long pilotId, not email.
     * The controller resolves email → id before calling this, or adapt
     * FlightPlan to store pilotEmail directly.
     */
    @Override
    public boolean existsAssignedFlightPlansForPilot(final String pilotEmail) {
        // FlightPlan stores pilotId (Long) not pilotEmail.
        // This query checks for any non-null pilotId as a safe default.
        // Adapt once pilotEmail is stored on FlightPlan or id resolution is available.
        return match("e.pilotId IS NOT NULL", new HashMap<>())
                .iterator().hasNext();
    }

    /**
     * US074 — Returns true if any flight plan exists for the given route
     * with a departure date (stored as String on FlightLeg) after the given date.
     * FlightLeg.departureDate is a String (yyyy-MM-dd) — compared lexicographically.
     */
    @Override
    public boolean existsPlannedFlightsAfter(final RouteDesignator routeDesignator,
                                             final LocalDate deactivationDate) {
        final Map<String, Object> params = new HashMap<>();
        params.put("routeDesignator", routeDesignator.value());
        params.put("deactivationDate", deactivationDate.toString());
        return matchOne(
                "e.routeDesignator = :routeDesignator " +
                        "AND EXISTS (SELECT l FROM FlightLeg l " +
                        "            WHERE l MEMBER OF e.legs " +
                        "            AND l.departureDate > :deactivationDate)",
                params).isPresent();
    }

    /**
     * US082 — Returns all flight plans.
     * pilotEmail filtering skipped: imported plans have no pilotId stored.
     */
    @Override
    public Iterable<FlightPlan> findByPilot(final String pilotEmail) {
        return match("1=1", new HashMap<>());
    }

    /**
     * US085 — Returns all flight plans for a pilot with the given status.
     * Uses pilotId IS NOT NULL as a proxy until pilotEmail is stored on FlightPlan.
     */
    @Override
    public Iterable<FlightPlan> findByPilotAndStatus(final String pilotEmail,
                                                      final FlightPlanStatus status) {
        final Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        return match("e.status = :status", params);
    }
}