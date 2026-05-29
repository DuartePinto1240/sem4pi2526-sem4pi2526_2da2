// ─── domain/flightroute/RecurringSchedule.java ─────────────────────────────

package domain.flightroute;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Embeddable
public class RecurringSchedule implements ValueObject {

    @ElementCollection
    private List<ScheduleEntry> entries = new ArrayList<>();

    public RecurringSchedule(final List<ScheduleEntry> entries) {
        Preconditions.nonNull(entries, "Schedule entries cannot be null");
        Preconditions.ensure(!entries.isEmpty(),
                "Recurring schedule must have at least one entry");
        this.entries = new ArrayList<>(entries);
    }

    public List<ScheduleEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof RecurringSchedule)) return false;
        return Objects.equals(entries, ((RecurringSchedule) o).entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries);
    }

    @Override
    public String toString() {
        return entries.toString();
    }

    protected RecurringSchedule() {
        // for ORM
    }
}