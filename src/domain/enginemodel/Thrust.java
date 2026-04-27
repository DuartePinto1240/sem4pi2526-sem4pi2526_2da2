package domain.enginemodel;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class Thrust implements ValueObject {

    // Sea level static thrust (kN)
    private double seaLevelStopped;
    // Sea level thrust at cruise/maximum speed (kN)
    private double seaLevelCruise;

    public Thrust(final double seaLevelStopped, final double seaLevelCruise) {
        Preconditions.ensure(seaLevelStopped > 0,
                "Sea level stopped thrust must be positive");
        Preconditions.ensure(seaLevelCruise > 0,
                "Sea level cruise thrust must be positive");
        Preconditions.ensure(seaLevelCruise > seaLevelStopped,
                "Sea level cruise thrust must be greater than sea level stopped thrust");
        this.seaLevelStopped = seaLevelStopped;
        this.seaLevelCruise = seaLevelCruise;
    }

    public double seaLevelStopped() {
        return seaLevelStopped;
    }

    public double seaLevelCruise() {
        return seaLevelCruise;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Thrust)) return false;
        final Thrust that = (Thrust) o;
        return Double.compare(that.seaLevelStopped, seaLevelStopped) == 0
                && Double.compare(that.seaLevelCruise, seaLevelCruise) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(seaLevelStopped, seaLevelCruise);
    }

    @Override
    public String toString() {
        return String.format("Thrust[stopped=%.2f kN, cruise=%.2f kN]",
                seaLevelStopped, seaLevelCruise);
    }

    protected Thrust() {
        // for ORM
    }
}