package domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Embeddable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value Object representing the temporal validity period of a weather observation.
 *
 * <p>A weather observation is valid from a start date/time to an end date/time
 * (inclusive).</p>
 *
 * <p>Invariants enforced:</p>
 * <ul>
 *   <li>Both {@code startDateTime} and {@code endDateTime} must not be null.</li>
 *   <li>{@code endDateTime} must be strictly after {@code startDateTime}.</li>
 * </ul>
 */
@Embeddable
public class ObservationPeriod implements ValueObject {

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    /**
     * Creates a new ObservationPeriod.
     *
     * @param startDateTime start of the period; must not be null
     * @param endDateTime   end of the period; must not be null and must be after start
     * @throws IllegalArgumentException if either argument is null or end is not after start
     */
    public ObservationPeriod(final LocalDateTime startDateTime, final LocalDateTime endDateTime) {
        Preconditions.nonNull(startDateTime, "Start date/time must not be null");
        Preconditions.nonNull(endDateTime,   "End date/time must not be null");
        if (!endDateTime.isAfter(startDateTime)) {
            throw new IllegalArgumentException("End date/time must be strictly after start date/time");
        }
        this.startDateTime = startDateTime;
        this.endDateTime   = endDateTime;
    }

    /** @return start of the observation period */
    public LocalDateTime startDateTime() {
        return startDateTime;
    }

    /** @return end of the observation period */
    public LocalDateTime endDateTime() {
        return endDateTime;
    }

    /**
     * Returns {@code true} if this period overlaps with {@code other}.
     *
     * <p>Two periods overlap when neither ends before the other starts.
     * Periods that merely touch at a single point (end == other.start)
     * are considered NOT overlapping for scheduling purposes.</p>
     *
     * @param other the other period to test; must not be null
     * @return {@code true} if the periods share any interval
     */
    public boolean overlapsWith(final ObservationPeriod other) {
        if (other == null) return false;
        return this.startDateTime.isBefore(other.endDateTime) && other.startDateTime.isBefore(this.endDateTime);
    }

    /**
     * Returns {@code true} if the given date falls within this observation period.
     *
     * <p>A date is considered "within" the period if it is on or after the start
     * date AND on or before the end date (inclusive on both ends).</p>
     *
     * @param date the calendar day to test; must not be null
     * @return {@code true} if this period covers the given day
     */
    public boolean containsDate(final LocalDate date) {
        if (date == null) return false;
        final LocalDate startDate = startDateTime.toLocalDate();
        final LocalDate endDate   = endDateTime.toLocalDate();
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ObservationPeriod)) return false;
        final ObservationPeriod that = (ObservationPeriod) o;
        return Objects.equals(startDateTime, that.startDateTime)
                && Objects.equals(endDateTime, that.endDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startDateTime, endDateTime);
    }

    @Override
    public String toString() {
        return String.format("%s → %s", startDateTime, endDateTime);
    }

    /** No-arg constructor required by JPA. */
    protected ObservationPeriod() {
    }
}
