package domain;

import eapli.framework.domain.model.ValueObject;

import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Value Object encapsulating the wind conditions for a weather observation.
 *
 * <p>Groups wind direction and wind speed together so they are always validated
 * and transferred as a pair. This models the "Segment" wind attributes defined
 * in section 3.2 of the project specification.</p>
 *
 * <p>Invariants enforced:</p>
 * <ul>
 *   <li>Wind direction: [0, 360[ degrees — 360 is excluded (same as 0).</li>
 *   <li>Wind speed: non-negative value in m/s.</li>
 * </ul>
 */
@Embeddable
public class WindCondition implements ValueObject {

    /** Wind direction angle relative to North, in degrees [0, 360[. */
    private double windDirection;

    /** Wind speed in m/s, must be >= 0. */
    private double windSpeed;

    /**
     * Creates a new WindCondition.
     *
     * @param windDirection angle relative to North in degrees; must be in [0, 360[
     * @param windSpeed     speed in m/s; must be >= 0
     * @throws IllegalArgumentException if either value is out of range
     */
    public WindCondition(final double windDirection, final double windSpeed) {
        if (windDirection < 0 || windDirection >= 360) {
            throw new IllegalArgumentException(
                    "Wind direction must be in [0, 360[ degrees (got " + windDirection + ")");
        }
        if (windSpeed < 0) {
            throw new IllegalArgumentException(
                    "Wind speed must be non-negative (got " + windSpeed + ")");
        }
        this.windDirection = windDirection;
        this.windSpeed = windSpeed;
    }

    /** @return wind direction in degrees [0, 360[ */
    public double windDirection() {
        return windDirection;
    }

    /** @return wind speed in m/s */
    public double windSpeed() {
        return windSpeed;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof WindCondition)) return false;
        final WindCondition that = (WindCondition) o;
        return Double.compare(that.windDirection, windDirection) == 0
                && Double.compare(that.windSpeed, windSpeed) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(windDirection, windSpeed);
    }

    @Override
    public String toString() {
        return String.format("Direction=%.1f° Speed=%.2f m/s", windDirection, windSpeed);
    }

    /** No-arg constructor required by JPA. */
    protected WindCondition() {
    }
}
