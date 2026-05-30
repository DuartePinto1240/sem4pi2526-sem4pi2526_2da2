// ─── domain/flightroute/ScheduledDates.java ────────────────────────────────

package domain.flightroute;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
import java.util.Objects;

@Embeddable
public class ScheduledDates implements ValueObject {

    private LocalDateTime departureDateTime;
    private LocalDateTime scheduledArrivalDateTime;

    public ScheduledDates(final LocalDateTime departureDateTime,
                          final LocalDateTime scheduledArrivalDateTime) {
        Preconditions.nonNull(departureDateTime, "Departure datetime cannot be null");
        Preconditions.nonNull(scheduledArrivalDateTime, "Arrival datetime cannot be null");
        Preconditions.ensure(scheduledArrivalDateTime.isAfter(departureDateTime),
                "Arrival must be after departure");
        this.departureDateTime        = departureDateTime;
        this.scheduledArrivalDateTime = scheduledArrivalDateTime;
    }

    public LocalDateTime departureDateTime()        { return departureDateTime;        }
    public LocalDateTime scheduledArrivalDateTime() { return scheduledArrivalDateTime; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduledDates)) return false;
        final ScheduledDates that = (ScheduledDates) o;
        return Objects.equals(departureDateTime, that.departureDateTime)
                && Objects.equals(scheduledArrivalDateTime, that.scheduledArrivalDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(departureDateTime, scheduledArrivalDateTime);
    }

    @Override
    public String toString() {
        return String.format("%s → %s", departureDateTime, scheduledArrivalDateTime);
    }

    protected ScheduledDates() {
        // for ORM
    }
}