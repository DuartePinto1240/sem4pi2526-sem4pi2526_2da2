// ─── domain/simulationreport/SimulationReport.java ────────────────────────

package domain.simulationreport;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "SIMULATION_REPORT")
public class SimulationReport implements AggregateRoot<SimulationReportId> {

    @EmbeddedId
    private SimulationReportId id;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "total_flights", nullable = false)
    private int totalFlights;

    @Embedded
    private OverallValidationResult result;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "FLIGHT_EXECUTION_STATUS",
            joinColumns = @JoinColumn(name = "report_id"))
    private List<FlightExecutionStatus> flightStatuses = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "SAFETY_VIOLATION_EVENT",
            joinColumns = @JoinColumn(name = "report_id"))
    private List<SafetyViolationEvent> violationEvents = new ArrayList<>();

    public SimulationReport(final SimulationReportId id,
                            final LocalDateTime generatedAt,
                            final int totalFlights,
                            final List<FlightExecutionStatus> flightStatuses,
                            final List<SafetyViolationEvent> violationEvents,
                            final OverallValidationResult result) {
        Preconditions.nonNull(id,           "Report ID cannot be null");
        Preconditions.nonNull(generatedAt,  "Generated at cannot be null");
        Preconditions.nonNull(result,       "Result cannot be null");
        Preconditions.ensure(totalFlights > 0, "Total flights must be positive");
        Preconditions.ensure(flightStatuses != null && !flightStatuses.isEmpty(),
                "Flight statuses cannot be empty");

        this.id             = id;
        this.generatedAt    = generatedAt;
        this.totalFlights   = totalFlights;
        this.flightStatuses = new ArrayList<>(flightStatuses);
        this.violationEvents = violationEvents != null
                ? new ArrayList<>(violationEvents)
                : new ArrayList<>();
        this.result         = result;
    }

    @Override
    public SimulationReportId identity() { return id; }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof SimulationReport)) return false;
        return this.id.equals(((SimulationReport) other).id);
    }

    public LocalDateTime                  generatedAt()     { return generatedAt;     }
    public int                            totalFlights()    { return totalFlights;    }
    public OverallValidationResult        result()          { return result;          }
    public List<FlightExecutionStatus>    flightStatuses()  {
        return Collections.unmodifiableList(flightStatuses);
    }
    public List<SafetyViolationEvent>     violationEvents() {
        return Collections.unmodifiableList(violationEvents);
    }

    @Override
    public String toString() {
        return String.format("Report %s | %d flights | %s | violations: %d",
                id, totalFlights, result, violationEvents.size());
    }

    protected SimulationReport() {}
}