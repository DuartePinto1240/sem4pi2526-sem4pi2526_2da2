package domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Value Object representing the unique identity of a {@link WeatherData} aggregate.
 *
 * <p>Uses a {@code long} surrogate key auto-generated at persistence time.
 * The identity is assigned by the repository on first save and is immutable
 * thereafter.</p>
 *
 * <p>Used as {@code @EmbeddedId} in the {@link WeatherData} JPA entity.</p>
 */
@Embeddable
public class WeatherDataId implements ValueObject, Comparable<WeatherDataId> {

    private Long id;

    /**
     * Creates a new WeatherDataId with the given numeric value.
     *
     * @param id must not be null and must be positive
     * @throws IllegalArgumentException if id is null or non-positive
     */
    public WeatherDataId(final Long id) {
        Preconditions.nonNull(id, "WeatherData id must not be null");
        if (id <= 0) {
            throw new IllegalArgumentException("WeatherData id must be positive");
        }
        this.id = id;
    }

    /** @return the underlying numeric id */
    public Long value() {
        return id;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof WeatherDataId)) return false;
        return Objects.equals(id, ((WeatherDataId) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public int compareTo(final WeatherDataId other) {
        return Long.compare(this.id, other.id);
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }

    /** No-arg constructor required by JPA. */
    protected WeatherDataId() {
    }
}
