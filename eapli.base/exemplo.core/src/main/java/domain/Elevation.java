package domain;

import eapli.framework.domain.model.ValueObject;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Value Object wrapping the elevation of an airport in metres above sea level.
 *
 * <p>Negative values are intentionally allowed: some airports exist below
 * sea level (e.g. Amsterdam Schiphol at −3.35 m). No lower bound is imposed.</p>
 */
@Embeddable
public class Elevation implements ValueObject {

    @Column(name = "elevation_m", nullable = false)
    private double metres;

    /**
     * Creates a new Elevation.
     *
     * @param metres the elevation in metres above sea level (may be negative)
     */
    public Elevation(final double metres) {
        this.metres = metres;
    }

    /** @return the elevation in metres */
    public double metres() {
        return metres;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Elevation)) return false;
        return Double.compare(((Elevation) o).metres, metres) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(metres);
    }

    @Override
    public String toString() {
        return metres + " m";
    }

    /** No-arg constructor required by JPA. */
    protected Elevation() {
    }
}
