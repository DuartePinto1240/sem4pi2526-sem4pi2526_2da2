package dto;

import domain.AirControlArea;

/**
 * Read-only DTO carrying air control area data to the UI layer.
 * The UI never touches domain objects directly.
 *
 * Created by the domain AirControlArea object via the factory method below,
 * following the same pattern as CollaboratorDTO and PilotDTO.
 */
public class AirControlAreaDTO {

    public final String areaCode;
    public final String name;

    public AirControlAreaDTO(final String areaCode, final String name) {
        this.areaCode = areaCode;
        this.name     = name;
    }

    /** Convenience factory from a domain AirControlArea. */
    public static AirControlAreaDTO from(final AirControlArea a) {
        return new AirControlAreaDTO(
                a.identity().code(),
                a.name());
    }

    /** The UI only needs to display the name. */
    @Override
    public String toString() {
        return name;
    }
}