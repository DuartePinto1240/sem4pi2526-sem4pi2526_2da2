// ─── domain/simulationreport/FlightExecutionStatus.java ───────────────────

package domain.simulationreport;

import eapli.framework.domain.model.ValueObject;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;

@Embeddable
public class FlightExecutionStatus implements ValueObject {

    public enum Status { OK, VIOLATED, ABORTED }

    private int flightIdentifier;

    @Enumerated(EnumType.STRING)
    private Status status;

    public FlightExecutionStatus(final int flightIdentifier, final Status status) {
        this.flightIdentifier = flightIdentifier;
        this.status           = status;
    }

    public int    flightIdentifier() { return flightIdentifier; }
    public Status status()           { return status;           }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof FlightExecutionStatus)) return false;
        final FlightExecutionStatus that = (FlightExecutionStatus) o;
        return flightIdentifier == that.flightIdentifier
                && status == that.status;
    }

    @Override
    public int hashCode() { return Objects.hash(flightIdentifier, status); }

    @Override
    public String toString() {
        return "Flight " + flightIdentifier + " : " + status.name();
    }

    protected FlightExecutionStatus() {}
}