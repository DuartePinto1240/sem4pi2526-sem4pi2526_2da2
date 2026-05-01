package domain.aircraft;

import eapli.framework.domain.model.ValueObject;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;

@Embeddable
public class OperationalStatus implements ValueObject {

    public enum Status {
        ACTIVE,
        DECOMMISSIONED
    }

    @Enumerated(EnumType.STRING)
    private Status status;

    private OperationalStatus(final Status status) {
        this.status = status;
    }

    public static OperationalStatus active() {
        return new OperationalStatus(Status.ACTIVE);
    }

    public static OperationalStatus decommissioned() {
        return new OperationalStatus(Status.DECOMMISSIONED);
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public boolean isDecommissioned() {
        return status == Status.DECOMMISSIONED;
    }

    public Status status() {
        return status;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof OperationalStatus)) return false;
        final OperationalStatus that = (OperationalStatus) o;
        return status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(status);
    }

    @Override
    public String toString() {
        return status.name();
    }

    protected OperationalStatus() {
        // for ORM
    }
}