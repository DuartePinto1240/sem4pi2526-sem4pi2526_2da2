// ─── domain/flightroute/FlightRoute.java ───────────────────────────────────

package domain.flightroute;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "FLIGHT_ROUTE")
public class FlightRoute implements AggregateRoot<RouteDesignator> {

    @EmbeddedId
    private RouteDesignator designator;

    @Embedded
    private FlightType flightType;

    @Column(name = "origin_iata", nullable = false)
    private String originIata;

    @Column(name = "destination_iata", nullable = false)
    private String destinationIata;

    @Column(name = "company_iata", nullable = false)
    private String companyIata;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "departureDateTime",
                    column = @Column(name = "charter_departure")),
            @AttributeOverride(name = "scheduledArrivalDateTime",
                    column = @Column(name = "charter_arrival"))
    })
    private ScheduledDates scheduledDates;

    @Embedded
    private RecurringSchedule recurringSchedule;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "deactivation_date")
    private LocalDate deactivationDate;

    // ── CHARTER constructor ───────────────────────────────────────────────

    public FlightRoute(final String designator,
                       final String companyIata,
                       final String originIata,
                       final String destinationIata,
                       final ScheduledDates scheduledDates) {
        Preconditions.nonNull(scheduledDates, "Charter route must have scheduled dates");
        validate(designator, companyIata, originIata, destinationIata);
        this.designator      = new RouteDesignator(designator);
        this.companyIata     = companyIata.toUpperCase().trim();
        this.originIata      = originIata.toUpperCase().trim();
        this.destinationIata = destinationIata.toUpperCase().trim();
        this.flightType      = FlightType.charter();
        this.scheduledDates  = scheduledDates;
        this.active          = true;
    }

    // ── REGULAR constructor ───────────────────────────────────────────────

    public FlightRoute(final String designator,
                       final String companyIata,
                       final String originIata,
                       final String destinationIata,
                       final RecurringSchedule recurringSchedule) {
        Preconditions.nonNull(recurringSchedule, "Regular route must have a recurring schedule");
        validate(designator, companyIata, originIata, destinationIata);
        this.designator        = new RouteDesignator(designator);
        this.companyIata       = companyIata.toUpperCase().trim();
        this.originIata        = originIata.toUpperCase().trim();
        this.destinationIata   = destinationIata.toUpperCase().trim();
        this.flightType        = FlightType.regular();
        this.recurringSchedule = recurringSchedule;
        this.active            = true;
    }

    private void validate(final String designator,
                          final String companyIata,
                          final String originIata,
                          final String destinationIata) {
        Preconditions.nonEmpty(designator,    "Designator cannot be empty");
        Preconditions.nonEmpty(companyIata,   "Company IATA cannot be empty");
        Preconditions.nonEmpty(originIata,    "Origin airport cannot be empty");
        Preconditions.nonEmpty(destinationIata, "Destination airport cannot be empty");
        Preconditions.ensure(!originIata.equalsIgnoreCase(destinationIata),
                "Origin and destination airports must be different");
    }

    public void deactivate(final LocalDate fromDate) {
        Preconditions.ensure(active, "Route is already inactive");
        Preconditions.nonNull(fromDate, "Deactivation date cannot be null");
        this.active           = false;
        this.deactivationDate = fromDate;
    }

    @Override
    public RouteDesignator identity() { return designator; }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof FlightRoute)) return false;
        return this.designator.equals(((FlightRoute) other).designator);
    }

    public FlightType      flightType()        { return flightType;        }
    public String          originIata()        { return originIata;        }
    public String          destinationIata()   { return destinationIata;   }
    public String          companyIata()       { return companyIata;       }
    public ScheduledDates  scheduledDates()    { return scheduledDates;    }
    public RecurringSchedule recurringSchedule() { return recurringSchedule; }
    public boolean         isActive()          { return active;            }
    public LocalDate       deactivationDate()  { return deactivationDate;  }

    @Override
    public String toString() {
        return String.format("%s | %s → %s | %s | %s",
                designator, originIata, destinationIata,
                flightType, active ? "ACTIVE" : "INACTIVE");
    }

    protected FlightRoute() {
        // for ORM
    }
}