package dto;

import domain.Collaborator;

/**
 * Read-only DTO carrying collaborator data to the UI layer.
 * The UI never touches domain objects directly.
 */
public class CollaboratorDTO {

    public final String email;
    public final String name;
    public final String position;
    public final boolean active;
    public final String phoneNumber;

    public CollaboratorDTO(final String email, final String name,
                           final String position, final boolean active,
                           final String phoneNumber) {
        this.email       = email;
        this.name        = name;
        this.position    = position;
        this.active      = active;
        this.phoneNumber = phoneNumber != null ? phoneNumber : "N/A";
    }

    /** Convenience factory from a domain Collaborator (no phone). */
    public static CollaboratorDTO from(final Collaborator c) {
        return new CollaboratorDTO(c.email(), c.name(), c.position(), c.isActive(), null);
    }

    /** Convenience factory from a domain Collaborator with phone. */
    public static CollaboratorDTO from(final Collaborator c, final String phone) {
        return new CollaboratorDTO(c.email(), c.name(), c.position(), c.isActive(), phone);
    }

    @Override
    public String toString() {
        return String.format("%-30s | %-25s | %-20s | %s",
                email, name, position, active ? "ACTIVE" : "INACTIVE");
    }
}