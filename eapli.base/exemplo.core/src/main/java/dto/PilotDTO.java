package dto;

import domain.aircraftmodel.AircraftModel;
import domain.pilot.Pilot;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only DTO carrying pilot data to the UI layer.
 * The UI never touches domain objects directly.
 */
public class PilotDTO {

    public final String       name;
    public final String       email;
    public final String       phoneNumber;
    public final boolean      active;
    public final List<String> certifiedModels;

    public PilotDTO(final String name,
                    final String email,
                    final String phoneNumber,
                    final boolean active,
                    final List<String> certifiedModels) {
        this.name            = name;
        this.email           = email;
        this.phoneNumber     = phoneNumber;
        this.active          = active;
        this.certifiedModels = certifiedModels;
    }

    /** Convenience factory from a domain Pilot. */
    public static PilotDTO from(final Pilot pilot) {
        final List<String> models = pilot.certifiedModels().stream()
                .map(m -> m.modelName() + " by " + m.manufacturer())
                .collect(Collectors.toList());

        return new PilotDTO(
                pilot.name(),
                pilot.email(),
                pilot.phoneNumber(),
                pilot.isActive(),
                models);
    }

    @Override
    public String toString() {
        return String.format("%-30s | %-25s | %-15s | %s",
                name, email, phoneNumber, active ? "ACTIVE" : "INACTIVE");
    }
}