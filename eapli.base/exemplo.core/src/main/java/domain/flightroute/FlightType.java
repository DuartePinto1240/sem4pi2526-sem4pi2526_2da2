// ─── domain/flightroute/FlightType.java ────────────────────────────────────

package domain.flightroute;

import eapli.framework.domain.model.ValueObject;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;

@Embeddable
public class FlightType implements ValueObject {

    public enum Type {
        REGULAR, CHARTER
    }

    @Enumerated(EnumType.STRING)
    private Type type;

    private FlightType(final Type type) {
        this.type = type;
    }

    public static FlightType regular() {
        return new FlightType(Type.REGULAR);
    }

    public static FlightType charter() {
        return new FlightType(Type.CHARTER);
    }

    public boolean isRegular() {
        return type == Type.REGULAR;
    }

    public boolean isCharter() {
        return type == Type.CHARTER;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof FlightType)) return false;
        return type == ((FlightType) o).type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public String toString() {
        return type.name();
    }

    protected FlightType() {
        // for ORM
    }
}