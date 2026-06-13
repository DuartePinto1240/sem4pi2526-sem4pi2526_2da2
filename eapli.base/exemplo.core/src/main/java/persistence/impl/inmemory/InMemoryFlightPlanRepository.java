package persistence.impl.inmemory;

import domain.flightroute.RouteDesignator;
import lprog.FlightLeg;
import lprog.FlightPlan;
import lprog.FlightPlanStatus;
import repositories.FlightPlanRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InMemoryFlightPlanRepository implements FlightPlanRepository {

    private final List<FlightPlan> store = new ArrayList<>();

    @Override
    public <S extends FlightPlan> S save(final S flightPlan) {
        store.removeIf(f -> f.identity() != null
                && f.identity().equals(flightPlan.identity()));
        store.add(flightPlan);
        return flightPlan;
    }

    @Override
    public boolean existsByFlightId(final String flightId) {
        return store.stream()
                .anyMatch(f -> f.flightId().equalsIgnoreCase(flightId));
    }

    @Override
    public Optional<FlightPlan> findByFlightId(final String flightId) {
        return store.stream()
                .filter(f -> f.flightId().equalsIgnoreCase(flightId))
                .findFirst();
    }

    @Override
    public Iterable<FlightPlan> findAll() {
        return new ArrayList<>(store);
    }

    @Override
    public Optional<FlightPlan> ofIdentity(final Long id) {
        return store.stream()
                .filter(f -> f.identity() != null && f.identity().equals(id))
                .findFirst();
    }

    @Override
    public boolean containsOfIdentity(final Long id) {
        return store.stream()
                .anyMatch(f -> f.identity() != null && f.identity().equals(id));
    }

    @Override
    public long count() {
        return store.size();
    }

    /**
     * US077 — Returns true if any flight plan belongs to the given pilot.
     */
    @Override
    public boolean existsAssignedFlightPlansForPilot(final String pilotEmail) {
        if (pilotEmail == null) return false;
        return store.stream()
                .anyMatch(f -> pilotEmail.equalsIgnoreCase(f.pilotEmail()));
    }

    /**
     * US074 — Returns true if any flight plan exists for the given route
     * with a departure date after the deactivation date.
     * FlightLeg stores departureDate as a String (e.g. "2026-05-20") and
     * departureTime as a separate String — parsed here using LocalDate.
     */
    @Override
    public boolean existsPlannedFlightsAfter(final RouteDesignator routeDesignator,
                                             final LocalDate deactivationDate) {
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return store.stream()
                .filter(f -> routeDesignator.value().equalsIgnoreCase(f.routeDesignator()))
                .anyMatch(f -> {
                    if (f.legs() == null || f.legs().isEmpty()) return false;
                    final FlightLeg firstLeg = f.legs().get(0);
                    try {
                        final LocalDate departure =
                                LocalDate.parse(firstLeg.departureDate(), fmt);
                        return departure.isAfter(deactivationDate);
                    } catch (Exception e) {
                        return false;
                    }
                });
    }

    /**
     * US082 — Returns all flight plans belonging to the given pilot.
     */
    @Override
    public Iterable<FlightPlan> findByPilot(final String pilotEmail) {
        if (pilotEmail == null) return new ArrayList<>();
        return store.stream()
                .filter(f -> pilotEmail.equalsIgnoreCase(f.pilotEmail()))
                .collect(Collectors.toList());
    }

    /**
     * US085 — Returns flight plans for the given pilot that are in the given status.
     */
    @Override
    public Iterable<FlightPlan> findByPilotAndStatus(final String pilotEmail,
                                                      final FlightPlanStatus status) {
        if (pilotEmail == null) return new ArrayList<>();
        return store.stream()
                .filter(f -> pilotEmail.equalsIgnoreCase(f.pilotEmail())
                          && f.status() == status)
                .collect(Collectors.toList());
    }
}