// ─── domain/flightroute/RouteDesignator.java ───────────────────────────────

package domain.flightroute;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class RouteDesignator implements ValueObject, Comparable<RouteDesignator> {

    private String designator;

    public RouteDesignator(final String designator) {
        Preconditions.nonEmpty(designator, "Route designator cannot be empty");
        final String upper = designator.toUpperCase().trim();
        if (!upper.matches("[A-Z]{2}[0-9]{1,4}")) {
            throw new IllegalArgumentException(
                    "Route designator must be 2 letters followed by 1 to 4 digits (e.g. TP123)");
        }
        this.designator = upper;
    }

    public String value() {
        return designator;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof RouteDesignator)) return false;
        return Objects.equals(designator, ((RouteDesignator) o).designator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(designator);
    }

    @Override
    public int compareTo(final RouteDesignator other) {
        return this.designator.compareTo(other.designator);
    }

    @Override
    public String toString() {
        return designator;
    }

    protected RouteDesignator() {
        // for ORM
    }
}