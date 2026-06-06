// ─── domain/simulationreport/SimulationReportId.java ──────────────────────

package domain.simulationreport;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class SimulationReportId implements ValueObject, Comparable<SimulationReportId> {

    private String id;

    public static SimulationReportId of(final String id) {
        return new SimulationReportId(id);
    }

    private SimulationReportId(final String id) {
        Preconditions.nonEmpty(id, "Report ID cannot be empty");
        this.id = id.trim();
    }

    public String value() { return id; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SimulationReportId)) return false;
        return Objects.equals(id, ((SimulationReportId) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public int compareTo(final SimulationReportId other) {
        return this.id.compareTo(other.id);
    }

    @Override
    public String toString() { return id; }

    protected SimulationReportId() {}
}