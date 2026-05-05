package domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Value Object representing an IATA airport code.
 *
 * <p>IATA codes are 3-letter identifiers assigned by the International
 * Air Transport Association.
 * Uniqueness across the system is enforced by the application service and a unique DB constraint.</p>
 *
 * <p>Validation: exactly 3 uppercase ASCII letters.</p>
 */
@Embeddable
public class IATACode implements ValueObject {

    @Column(name = "iata_code", nullable = false, unique = true)
    private String iata;

    /**
     * Creates a new IATACode.
     *
     * @param iata the 3-letter IATA code, must match [A-Z]{3}
     * @throws IllegalArgumentException if the code is null, blank, or not 3 letters
     */
    public IATACode(final String iata) {
        Preconditions.nonEmpty(iata, "IATA code must not be empty");
        final String normalised = iata.trim().toUpperCase();
        if (!normalised.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "IATA code must be exactly 3 uppercase letters, got: '" + iata + "'");
        }
        this.iata = normalised;
    }

    /** @return the IATA code string */
    public String code() {
        return iata;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof IATACode)) return false;
        return Objects.equals(iata, ((IATACode) o).iata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(iata);
    }

    @Override
    public String toString() {
        return iata;
    }

    /** No-arg constructor required by JPA. */
    protected IATACode() {
    }
}
