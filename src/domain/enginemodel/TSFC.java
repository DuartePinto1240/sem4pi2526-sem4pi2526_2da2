package domain.enginemodel;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Thrust Specific Fuel Consumption.
 * Stored in SI units: N/N/s.
 * Describes fuel efficiency per unit of thrust output.
 */
@Embeddable
public class TSFC implements ValueObject {

    // "value" is a reserved word in H2 — mapped to a safe column name
    @Column(name = "tsfc_value")
    private double value;

    public TSFC(final double value) {
        Preconditions.ensure(value > 0,
                "TSFC must be strictly positive");
        this.value = value;
    }

    public double value() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof TSFC)) return false;
        final TSFC that = (TSFC) o;
        return Double.compare(that.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.format("TSFC[%.6f N/N/s]", value);
    }

    protected TSFC() {
        // for ORM
    }
}