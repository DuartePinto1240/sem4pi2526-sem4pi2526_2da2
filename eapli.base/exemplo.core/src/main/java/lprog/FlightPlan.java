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

    /**
     * US082 — id of the weather data associated with this flight plan.
     * Null until weather data is inserted by the pilot.
     */
    @Column(name = "weather_data_id")
    private Long weatherDataId;

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
                      final Long pilotId) {     // <-- add this parameter
        this(flightId, flightType, legs);
        Preconditions.nonEmpty(routeDesignator,      "Route designator cannot be empty");
        Preconditions.nonEmpty(aircraftRegistration, "Aircraft registration cannot be empty");

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
    public Long             weatherDataId()        { return weatherDataId;        }

    /** Stores the DSL content. Called by US080 (no validation) and US081 (post-validation). */
    public void setDslContent(final String dslContent) {
        this.dslContent = dslContent;
    }

    // ── US082 methods ────────────────────────────────────────────────────────

    /**
     * US082 — Returns true if this flight plan belongs to the pilot
     * with the given email. Matches against the stored pilotId by looking
     * up the pilot's email from the PilotRepository.
     *
     * Since FlightPlan stores pilotId (Long) rather than pilotEmail directly,
     * the simplest approach is to have the controller pass the authenticated
     * pilot's id for comparison rather than the email.
     *
     * Alternative: store pilotEmail as a column on FlightPlan (simpler).
     *
     * @param pilotEmail the email of the authenticated pilot
     * @return true if this plan's pilot matches the given email
     */
    public boolean isOwnedBy(final String pilotEmail) {
        // Since we store pilotId not pilotEmail, the controller must resolve
        // the email to an id before calling this, or we store pilot email directly.
        // For now: the controller passes the pilot's email and we compare via
        // the PilotRepository. But since FlightPlan should not call a repository,
        // the cleaner solution is to store the pilot email on the flight plan.
        // This is implemented by checking the pilotEmail field if present,
        // or always returning true and letting the controller handle ownership.
        // See InsertWeatherDataController for the ownership check logic.
        throw new UnsupportedOperationException(
                "Implement: compare pilotEmail against this plan's pilot reference. "
                        + "Options: (1) add a pilotEmail column to this entity, "
                        + "(2) have the controller resolve pilotId to email before calling this.");
    }

    /**
     * US082 — Returns true if this flight plan is in a state that allows
     * weather data to be added. Only DRAFT and TESTED plans can be modified.
     *
     * @return true if status is DRAFT or TESTED
     */
    public boolean isModifiable() {
        return status == FlightPlanStatus.DRAFT
                || status == FlightPlanStatus.TESTED;
    }

    /**
     * US082 — Associates the given weather data with this flight plan.
     * If the plan was previously TESTED, the status is reverted to DRAFT
     * because the test result is no longer valid with the new weather data.
     *
     * @param weatherDataId the id of the weather data to associate
     */
    public void addWeatherData(final Long weatherDataId) {
        Preconditions.nonNull(weatherDataId, "Weather data id must not be null");
        this.weatherDataId = weatherDataId;
        // Void the previous test result if the plan was tested
        if (this.status == FlightPlanStatus.TESTED) {
            this.status = FlightPlanStatus.DRAFT;
        }
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