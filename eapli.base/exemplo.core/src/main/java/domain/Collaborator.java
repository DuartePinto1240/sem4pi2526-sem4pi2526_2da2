package domain;

import eapli.framework.validations.Preconditions;
import jakarta.persistence.*;

import java.util.Objects;

/**
 * Entity representing a collaborator of a customer (US061–US064).
 *
 * Owned by its customer aggregate root (AirTransportCompany or AirControlArea)
 * via a @OneToMany relationship. References the system User by email only —
 * no direct Java reference crosses aggregate boundaries.
 *
 * Invariants enforced here:
 *   - email, name and position must not be blank.
 *   - active is always true on creation.
 *   - disable() is idempotent.
 *   - name and position are immutable after creation.
 */
@Entity
@Table(name = "COLLABORATOR")
public class Collaborator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "COLLAB_EMAIL", nullable = false)
    private String email;

    @Column(name = "COLLAB_NAME", nullable = false)
    private String name;

    @Column(name = "COLLAB_POSITION", nullable = false)
    private String position;

    @Column(name = "COLLAB_ACTIVE", nullable = false)
    private boolean active;

    /** Required by JPA. */
    protected Collaborator() {
    }

    /**
     * Creates a new active collaborator.
     *
     * @param email    must not be blank — also the cross-aggregate key to User
     * @param name     must not be blank — immutable after creation
     * @param position must not be blank — immutable after creation
     */
    public Collaborator(final String email, final String name, final String position) {
        Preconditions.nonEmpty(email,    "Collaborator email must not be empty");
        Preconditions.nonEmpty(name,     "Collaborator name must not be empty");
        Preconditions.nonEmpty(position, "Collaborator position must not be empty");
        this.email    = email.trim().toLowerCase();
        this.name     = name.trim();
        this.position = position.trim();
        this.active   = true; // always active on creation
    }

    /**
     * Disables this collaborator.
     * Idempotent: calling this on an already-disabled collaborator is safe.
     */
    public void disable() {
        this.active = false;
    }

    /**
     * Updates the email of this collaborator.
     * Called by the customer aggregate when the Backoffice Operator changes the
     * collaborator's email. The controller is responsible for also
     * updating the associated User aggregate.
     *
     * @param newEmail must not be blank
     */
    public void updateEmail(final String newEmail) {
        Preconditions.nonEmpty(newEmail, "New email must not be empty");
        this.email = newEmail.trim().toLowerCase();
    }


    // name and position have NO setters — immutable after creation

    public String  email()    { return email; }
    public String  name()     { return name; }
    public String  position() { return position; }
    public boolean isActive() { return active; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Collaborator)) return false;
        return Objects.equals(email, ((Collaborator) o).email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    @Override
    public String toString() {
        return String.format("%-30s | %-25s | %-20s | %s",
                email, name, position, active ? "ACTIVE" : "INACTIVE");
    }
}