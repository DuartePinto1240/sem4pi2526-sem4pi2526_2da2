// ─── domain/flightroute/ScheduleEntry.java ─────────────────────────────────

package domain.flightroute;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Embeddable;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

@Embeddable
public class ScheduleEntry implements ValueObject {

    private DayOfWeek dayOfWeek;
    private LocalTime departureTime;
    private LocalTime scheduledArrivalTime;

    public ScheduleEntry(final DayOfWeek dayOfWeek,
                         final LocalTime departureTime,
                         final LocalTime scheduledArrivalTime) {
        Preconditions.nonNull(dayOfWeek, "Day of week cannot be null");
        Preconditions.nonNull(departureTime, "Departure time cannot be null");
        Preconditions.nonNull(scheduledArrivalTime, "Arrival time cannot be null");
        Preconditions.ensure(scheduledArrivalTime.isAfter(departureTime),
                "Arrival time must be after departure time");
        this.dayOfWeek           = dayOfWeek;
        this.departureTime       = departureTime;
        this.scheduledArrivalTime = scheduledArrivalTime;
    }

    public DayOfWeek dayOfWeek()            { return dayOfWeek;            }
    public LocalTime departureTime()        { return departureTime;        }
    public LocalTime scheduledArrivalTime() { return scheduledArrivalTime; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduleEntry)) return false;
        final ScheduleEntry that = (ScheduleEntry) o;
        return Objects.equals(dayOfWeek, that.dayOfWeek)
                && Objects.equals(departureTime, that.departureTime)
                && Objects.equals(scheduledArrivalTime, that.scheduledArrivalTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dayOfWeek, departureTime, scheduledArrivalTime);
    }

    @Override
    public String toString() {
        return String.format("%s %s → %s", dayOfWeek, departureTime, scheduledArrivalTime);
    }

    protected ScheduleEntry() {
        // for ORM
    }
}