package domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Value Object representing a country name.
 *
 * <p>Countries are pre-loaded via bootstrap and selected from a predefined
 * list when registering an airport. This class is a Value Object — it has
 * no identity beyond its value and is NOT an Aggregate Root.</p>
 *
 * <p>Validation: name must not be null or blank.</p>
 */
@Embeddable
public class Country implements ValueObject {

    @Column(name = "country_name", nullable = false)
    private String name;

    /**
     * Creates a new Country.
     *
     * @param name the country name, must not be blank
     * @throws IllegalArgumentException if name is null or blank
     */
    public Country(final String name) {
        Preconditions.nonEmpty(name, "Country name must not be blank");
        this.name = name.trim();
    }

    /** @return the country name */
    public String name() {
        return name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Country)) return false;
        return Objects.equals(name, ((Country) o).name);
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
    protected Country() {
    }
}