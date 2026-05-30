package domain.pilot;

import domain.AirTransportCompany;
import domain.aircraftmodel.AircraftModel;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate root representing a pilot of an air transport company.
 *
 * A pilot is a system user certified to fly one or more aircraft models.
 * Created from scratch by an Air Transport Company Collaborator (US075).
 *
 * Invariants enforced here:
 *   - name, email and phoneNumber must not be blank.
 *   - certifiedModels must contain at least one aircraft model.
 *   - active is always true on creation.
 *   - email is immutable after creation.
 */
@Entity
@Table(name = "PILOT")
public class Pilot implements AggregateRoot<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "PILOT_NAME", nullable = false)
    private String name;

    @Column(name = "PILOT_EMAIL", nullable = false, unique = true)
    private String email;

    @Column(name = "PILOT_PHONE", nullable = false)
    private String phoneNumber;

    @Column(name = "PILOT_ACTIVE", nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumns({
            @JoinColumn(name = "COMPANY_IATA", referencedColumnName = "iata"),
            @JoinColumn(name = "COMPANY_ICAO", referencedColumnName = "icao")
    })
    private AirTransportCompany company;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "PILOT_CERTIFIED_MODEL",
            joinColumns = @JoinColumn(name = "pilot_id"),
            inverseJoinColumns = @JoinColumn(name = "aircraft_model_id"))
    private List<AircraftModel> certifiedModels = new ArrayList<>();

    public Pilot(final String name,
                 final String email,
                 final String phoneNumber,
                 final AirTransportCompany company,
                 final List<AircraftModel> certifiedModels) {
        Preconditions.nonEmpty(name,        "Pilot name must not be empty");
        Preconditions.nonEmpty(email,       "Pilot email must not be empty");
        Preconditions.nonEmpty(phoneNumber, "Pilot phone number must not be empty");
        Preconditions.nonNull(company,      "Company must not be null");
        Preconditions.nonNull(certifiedModels, "Certified models must not be null");
        Preconditions.ensure(!certifiedModels.isEmpty(),
                "Pilot must be certified in at least one aircraft model");

        this.name            = name.trim();
        this.email           = email.trim().toLowerCase();
        this.phoneNumber     = phoneNumber.trim();
        this.company         = company;
        this.certifiedModels = new ArrayList<>(certifiedModels);
        this.active          = true;
    }

    /**
     * Deactivates this pilot.
     * Should only be called after verifying no flight plans are assigned (US077).
     * Idempotent: calling on an already-inactive pilot is safe.
     */
    public void deactivate() {
        this.active = false;
    }

    @Override
    public Long identity() {
        return id;
    }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof Pilot)) return false;
        final Pilot that = (Pilot) other;
        return this.email.equalsIgnoreCase(that.email);
    }

    public String name()                          { return name; }
    public String email()                         { return email; }
    public String phoneNumber()                   { return phoneNumber; }
    public boolean isActive()                     { return active; }
    public AirTransportCompany company()          { return company; }
    public List<AircraftModel> certifiedModels()  { return Collections.unmodifiableList(certifiedModels); }

    @Override
    public String toString() {
        return String.format("%-30s | %-25s | %-15s | %s",
                name, email, phoneNumber, active ? "ACTIVE" : "INACTIVE");
    }

    /** Required by JPA. */
    protected Pilot() {
    }
}