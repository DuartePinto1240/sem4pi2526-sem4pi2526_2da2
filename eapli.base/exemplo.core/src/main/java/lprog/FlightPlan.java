package lprog;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "FLIGHT_PLAN")
public class FlightPlan implements AggregateRoot<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_id", nullable = false, unique = true)
    private String flightId;

    @Column(name = "flight_type", nullable = false)
    private String flightType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FlightPlanStatus status;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER,
            orphanRemoval = true)
    @JoinColumn(name = "flight_plan_id")
    private List<FlightLeg> legs = new ArrayList<>();

    /** Set by US080 manual creation; null for file-imported plans (US081). */
    @Column(name = "route_designator")
    private String routeDesignator;

    /** Registration of the assigned aircraft; null for file-imported plans. */
    @Column(name = "aircraft_registration")
    private String aircraftRegistration;

    /** Database id of the assigned pilot; null for file-imported plans. */
    @Column(name = "pilot_id")
    private Long pilotId;

    /**
     * Raw DSL content of this flight plan.
     * Set by US081 (validated DSL from file) or US080 (unvalidated DSL string/file).
     * Used by US085 to run the C simulator.
     */
    @Lob
    @Column(name = "dsl_content")
    private String dslContent;

    /** US081 constructor: file import — no route/aircraft/pilot references. */
    public FlightPlan(final String flightId,
                      final String flightType,
                      final List<FlightLeg> legs) {
        Preconditions.nonEmpty(flightId,   "Flight ID cannot be empty");
        Preconditions.nonEmpty(flightType, "Flight type cannot be empty");
        Preconditions.nonNull(legs,        "Legs cannot be null");
        Preconditions.ensure(!legs.isEmpty(), "At least one leg is required");

        this.flightId   = flightId;
        this.flightType = flightType.toLowerCase();
        this.legs       = new ArrayList<>(legs);
        this.status     = FlightPlanStatus.DRAFT;
        this.importedAt = LocalDateTime.now();
    }

    /** US080 constructor: manual creation — includes route, aircraft and pilot. */
    public FlightPlan(final String flightId,
                      final String flightType,
                      final List<FlightLeg> legs,
                      final String routeDesignator,
                      final String aircraftRegistration,
                      final Long pilotId) {
        this(flightId, flightType, legs);
        Preconditions.nonEmpty(routeDesignator,      "Route designator cannot be empty");
        Preconditions.nonEmpty(aircraftRegistration, "Aircraft registration cannot be empty");
        // pilotId may be null before persistence in tests; business rule enforced by controller

        this.routeDesignator      = routeDesignator;
        this.aircraftRegistration = aircraftRegistration;
        this.pilotId              = pilotId;
    }

    @Override
    public Long identity() { return id; }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof FlightPlan)) return false;
        final FlightPlan that = (FlightPlan) other;
        return this.flightId.equalsIgnoreCase(that.flightId);
    }

    public String           flightId()             { return flightId;             }
    public String           flightType()           { return flightType;           }
    public FlightPlanStatus status()               { return status;               }
    public LocalDateTime    importedAt()           { return importedAt;           }
    public List<FlightLeg>  legs()                 { return Collections.unmodifiableList(legs); }
    public String           routeDesignator()      { return routeDesignator;      }
    public String           aircraftRegistration() { return aircraftRegistration; }
    public Long             pilotId()              { return pilotId;              }
    public String           dslContent()           { return dslContent;           }

    /** Stores the DSL content. Called by US080 (no validation) and US081 (post-validation). */
    public void setDslContent(final String dslContent) {
        this.dslContent = dslContent;
    }

    @Override
    public String toString() {
        return String.format("FlightPlan[id=%s, type=%s, legs=%d, status=%s]",
                flightId, flightType, legs.size(), status);
    }

    protected FlightPlan() {
        // for ORM
    }
}