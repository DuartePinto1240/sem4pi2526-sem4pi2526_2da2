package domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Value Object representing the unique code that identifies an Air Control Area.
 *
 * <p>The code must be non-blank. No specific format is enforced beyond that,
 * allowing ICAO region codes (e.g. "LPPC") or other conventions.</p>
 *
 * <p>Used as the {@link AirControlArea} aggregate identity ({@code @EmbeddedId})
 * and as a reference key by other aggregates (e.g. Airport, WeatherData).</p>
 */
@Embeddable
public class AirControlAreaCode implements ValueObject, Comparable<AirControlAreaCode> {

    private String code;

    /**
     * Creates a new AirControlAreaCode.
     *
     * @param code the area code, must not be blank
     * @throws IllegalArgumentException if code is null or blank
     */
    public AirControlAreaCode(final String code) {
        Preconditions.nonEmpty(code, "Air control area code must not be empty");
        this.code = code.toUpperCase().trim();
    }

    /** @return the area code string */
    public String code() {
        return code;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AirControlAreaCode)) return false;
        final AirControlAreaCode that = (AirControlAreaCode) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public int compareTo(final AirControlAreaCode other) {
        return this.code.compareTo(other.code);
    }

    @Override
    public String toString() {
        return code;
    }

    /** No-arg constructor required by JPA. */
    protected AirControlAreaCode() {
    }
}
