package domain;

import dto.CollaboratorDTO;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Aggregate Root representing an Air Control Area in the AISafe system (US050).
 *
 * <p>An air control area is a defined region of airspace managed by a flight
 * control entity. It serves as the organisational unit to which airports,
 * weather data, and flight simulations are scoped.</p>
 *
 * <p>Domain invariants enforced by this class:</p>
 * <ul>
 *   <li>Area code must not be blank and is unique system-wide.</li>
 *   <li>Name must not be blank.</li>
 *   <li>Geographic boundary must define a valid bounding box.</li>
 * </ul>
 */
@Entity
@Table(name = "AIR_CONTROL_AREA")
public class AirControlArea implements AggregateRoot<AirControlAreaCode> {

    /** Aggregate identity — unique code for this area, e.g. "LPPC". */
    @EmbeddedId
    private AirControlAreaCode areaCode;

    /** Human-readable name of the air control area. */
    @Column(nullable = false)
    private String name;

    /** Geographic bounding box defining the spatial extent of this area. */
    @Embedded
    private GeographicBoundary boundary;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "AREA_CODE", referencedColumnName = "code")
    private List<Collaborator> collaborators = new ArrayList<>();

    /**
     * Creates a new Air Control Area.
     *
     * @param areaCode unique code for this area, must not be blank
     * @param name     human-readable name, must not be blank
     * @param boundary geographic bounding box, must be valid
     * @throws IllegalArgumentException if any argument fails validation
     */
    public AirControlArea(final String areaCode,
                          final String name,
                          final GeographicBoundary boundary) {
        Preconditions.nonEmpty(areaCode, "Area code must not be empty");
        Preconditions.nonEmpty(name, "Area name must not be empty");
        Preconditions.nonNull(boundary, "Geographic boundary must not be null");

        this.areaCode = new AirControlAreaCode(areaCode);
        this.name = name.trim();
        this.boundary = boundary;
    }

    /** {@inheritDoc} */
    @Override
    public AirControlAreaCode identity() {
        return areaCode;
    }

    /** {@inheritDoc} */
    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof AirControlArea)) return false;
        final AirControlArea that = (AirControlArea) other;
        return this.areaCode.equals(that.areaCode);
    }

    /** @return the human-readable name of this area */
    public String name() {
        return name;
    }

    /** @return the geographic bounding box of this area */
    public GeographicBoundary boundary() {
        return boundary;
    }

    @Override
    public String toString() {
        return String.format("%s — %s | %s", areaCode, name, boundary);
    }

    /** No-arg constructor required by JPA. */
    protected AirControlArea() {
    }


    /**
     * Adds a new Flight Control Operator collaborator to this area.
     * Same invariants as AirTransportCompany.addCollaborator().
     *
     * @param email    unique within this area
     * @param name     immutable after creation
     * @param position immutable after creation
     * @throws IllegalArgumentException if email already exists in this area
     */
    public void addCollaborator(final String email,
                                final String name,
                                final String position) {
        if (findCollaboratorByEmail(email).isPresent()) {
            throw new IllegalArgumentException(
                    "A collaborator with email '" + email + "' already exists in area " + this.name);
        }
        final Collaborator c = new Collaborator(email, name, position);
        collaborators.add(c);
    }

    /** Returns only active collaborators. Filtering stays in the aggregate. */
    public List<CollaboratorDTO> listActiveCollaborators() {
        List<CollaboratorDTO> activeCollaborators = new ArrayList<>();

        for (Collaborator collaborator : collaborators) {
            if (collaborator.isActive()) {
                activeCollaborators.add(
                        new CollaboratorDTO(
                                collaborator.name(),
                                collaborator.email(),
                                collaborator.position(),
                                collaborator.isActive()
                        )
                );
            }
        }

        return activeCollaborators;
    }

    /**
     * Updates the email of a collaborator within this area.
     *
     * @param oldEmail current email
     * @param newEmail new email
     * @throws IllegalArgumentException if no collaborator with oldEmail exists
     */
    public void updateCollaboratorEmail(final String oldEmail, final String newEmail) {
        Optional<Collaborator> collaborator = findCollaboratorByEmail(oldEmail);

        if (collaborator.isEmpty()) {
            throw new IllegalArgumentException(
                    "No collaborator with email '" + oldEmail + "' in company " + name);
        }

        Collaborator c = collaborator.get();
        c.updateEmail(newEmail);
    }

    /**
     * Disables a collaborator within this area (soft delete, idempotent).
     *
     * @param email email of the collaborator to disable
     * @throws IllegalArgumentException if no collaborator with that email exists
     */
    public void disableCollaborator(final String email) {
        Optional<Collaborator> collaborator = findCollaboratorByEmail(email);

        if (collaborator.isPresent()) {
            collaborator.get().disable();
        } else {
            throw new IllegalArgumentException(
                    "No collaborator with email '" + email + "' in area " + name);
        }
    }



    // Helper function
    public Optional<Collaborator> findCollaboratorByEmail(final String email) {
        if (email == null) return Optional.empty();
        final String normalised = email.trim().toLowerCase();
        return collaborators.stream()
                .filter(c -> c.email().equals(normalised))
                .findFirst();
    }

}