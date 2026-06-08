package dto;

import lprog.FlightPlan;
import lprog.FlightPlanStatus;

import java.time.LocalDateTime;

/**
 * Read-only DTO carrying flight plan data to the UI layer.
 * The UI never touches domain objects directly.
 *
 * Created by the domain FlightPlan object via the factory method below,
 * following the same pattern as CollaboratorDTO and PilotDTO.
 */
public class FlightPlanDTO {

    public final Long            id;
    public final String          flightId;
    public final String          flightType;
    public final FlightPlanStatus status;
    public final LocalDateTime   importedAt;
    public final String          routeDesignator;
    public final String          aircraftRegistration;
    public final Long            pilotId;
    public final Long            weatherDataId;

    public FlightPlanDTO(final Long id,
                         final String flightId,
                         final String flightType,
                         final FlightPlanStatus status,
                         final LocalDateTime importedAt,
                         final String routeDesignator,
                         final String aircraftRegistration,
                         final Long pilotId,
                         final Long weatherDataId) {
        this.id                   = id;
        this.flightId             = flightId;
        this.flightType           = flightType;
        this.status               = status;
        this.importedAt           = importedAt;
        this.routeDesignator      = routeDesignator;
        this.aircraftRegistration = aircraftRegistration;
        this.pilotId              = pilotId;
        this.weatherDataId        = weatherDataId;
    }

    /** Convenience factory from a domain FlightPlan. */
    public static FlightPlanDTO from(final FlightPlan p) {
        return new FlightPlanDTO(
                p.identity(),
                p.flightId(),
                p.flightType(),
                p.status(),
                p.importedAt(),
                p.routeDesignator(),
                p.aircraftRegistration(),
                p.pilotId(),
                p.weatherDataId());
    }

    @Override
    public String toString() {
        return String.format("%-10s | %-10s | %-10s | %s",
                flightId, flightType, status,
                routeDesignator != null ? routeDesignator : "N/A");
    }
}