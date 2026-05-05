package domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Value Object representing an ICAO airport code.
 *
 * <p>ICAO codes are globally unique 4-letter identifiers assigned by the
 * International Civil Aviation Organisation.
 * This VO acts as the {@link Airport} aggregate identity ({@code @EmbeddedId}).</p>
 *
 * <p>Validation: exactly 4 uppercase ASCII letters.</p>
 */
@Embeddable
public class ICAOCode implements ValueObject, Comparable<ICAOCode> {

    private String icao;

    /**
     * Creates a new ICAOCode.
     *
     * @param icao the 4-letter ICAO code, must match [A-Z]{4}
     * @throws IllegalArgumentException if the code is null, blank, or not 4 letters
     */
    public ICAOCode(final String icao) {
        Preconditions.nonEmpty(icao, "ICAO code must not be empty");
        final String normalised = icao.trim().toUpperCase();
        if (!normalised.matches("[A-Z]{4}")) {
            throw new IllegalArgumentException(
                    "ICAO code must be exactly 4 uppercase letters, got: '" + icao + "'");
        }
        this.icao = normalised;
    }

    /** @return the ICAO code string */
    public String code() {
        return icao;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ICAOCode)) return false;
        return Objects.equals(icao, ((ICAOCode) o).icao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(icao);
    }

    @Override
    public int compareTo(final ICAOCode other) {
        return this.icao.compareTo(other.icao);
    }

    @Override
    public String toString() {
        return icao;
    }

    /** No-arg constructor required by JPA. */
    protected ICAOCode() {
    }
}
