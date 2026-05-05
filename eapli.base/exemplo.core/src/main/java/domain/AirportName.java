package domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Value Object wrapping the full name of an airport.
 *
 * <p>Enforces non-null, non-blank constraint and trims surrounding whitespace.
 * Encapsulating the name in its own VO keeps the {@link Airport} constructor
 * clean and makes future constraints (max length, forbidden characters) easy
 * to add without touching the aggregate.</p>
 */
@Embeddable
public class AirportName implements ValueObject {

    @Column(name = "airport_name", nullable = false)
    private String name;

    /**
     * Creates a new AirportName.
     *
     * @param name the airport's full name, must not be blank
     * @throws IllegalArgumentException if name is null or blank
     */
    public AirportName(final String name) {
        Preconditions.nonEmpty(name, "Airport name must not be blank");
        this.name = name.trim();
    }

    /** @return the airport name string */
    public String value() {
        return name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AirportName)) return false;
        return Objects.equals(name, ((AirportName) o).name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }

    /** No-arg constructor required by JPA. */
    protected AirportName() {
    }
}
