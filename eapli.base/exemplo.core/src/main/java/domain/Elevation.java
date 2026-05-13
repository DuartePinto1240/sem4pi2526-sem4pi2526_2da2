package domain;

import eapli.framework.domain.model.ValueObject;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Value Object wrapping the elevation of an airport in metres above sea level.
 */
@Embeddable
public class Elevation implements ValueObject {

    @Column(name = "elevation_m", nullable = false)
    private double metres;

    /**
     * Creates a new Elevation.
     *
     * @param metres elevation in metres above sea level — must be > 0
     * @throws IllegalArgumentException if metres is zero or negative
     */
    public Elevation(final double metres) {
        if (metres <= 0) {
            throw new IllegalArgumentException(
                    "Elevation must be above sea level (> 0 m), got: " + metres);
        }
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
