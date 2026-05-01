package domain.aircraft;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class CabinConfiguration implements ValueObject {

    private int economySeats;
    private int businessSeats;
    private int firstClassSeats;
    private int crewSize;

    public CabinConfiguration(final int economySeats,
                              final int businessSeats,
                              final int firstClassSeats,
                              final int crewSize) {
        Preconditions.ensure(economySeats >= 0,
                "Economy seats cannot be negative");
        Preconditions.ensure(businessSeats >= 0,
                "Business seats cannot be negative");
        Preconditions.ensure(firstClassSeats >= 0,
                "First class seats cannot be negative");
        Preconditions.ensure(crewSize > 0,
                "Crew size must be positive");
        Preconditions.ensure(economySeats + businessSeats + firstClassSeats > 0,
                "Total number of seats must be greater than zero");

        this.economySeats    = economySeats;
        this.businessSeats   = businessSeats;
        this.firstClassSeats = firstClassSeats;
        this.crewSize        = crewSize;
    }

    public int economySeats()    { return economySeats;    }
    public int businessSeats()   { return businessSeats;   }
    public int firstClassSeats() { return firstClassSeats; }
    public int crewSize()        { return crewSize;        }

    public int totalSeats() {
        return economySeats + businessSeats + firstClassSeats;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof CabinConfiguration)) return false;
        final CabinConfiguration that = (CabinConfiguration) o;
        return economySeats    == that.economySeats
                && businessSeats   == that.businessSeats
                && firstClassSeats == that.firstClassSeats
                && crewSize        == that.crewSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(economySeats, businessSeats, firstClassSeats, crewSize);
    }

    @Override
    public String toString() {
        return String.format(
                "Economy: %d | Business: %d | First: %d | Crew: %d | Total: %d",
                economySeats, businessSeats, firstClassSeats, crewSize, totalSeats());
    }

    protected CabinConfiguration() {
        // for ORM
    }
}