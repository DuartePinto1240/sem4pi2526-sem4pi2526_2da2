package dto;

import domain.flightroute.FlightRoute;

import java.time.LocalDate;

/**
 * Read-only DTO carrying flight route data to the UI layer.
 * The UI never touches domain objects directly.
 *
 * Created by the domain FlightRoute object via the factory method below,
 * following the same pattern as CollaboratorDTO and PilotDTO.
 */
public class FlightRouteDTO {

    public final String    designator;
    public final String    companyIata;
    public final String    originIata;
    public final String    destinationIata;
    public final String    flightType;
    public final boolean   active;
    public final LocalDate deactivationDate;

    public FlightRouteDTO(final String designator,
                          final String companyIata,
                          final String originIata,
                          final String destinationIata,
                          final String flightType,
                          final boolean active,
                          final LocalDate deactivationDate) {
        this.designator       = designator;
        this.companyIata      = companyIata;
        this.originIata       = originIata;
        this.destinationIata  = destinationIata;
        this.flightType       = flightType;
        this.active           = active;
        this.deactivationDate = deactivationDate;
    }

    /** Convenience factory from a domain FlightRoute. */
    public static FlightRouteDTO from(final FlightRoute r) {
        return new FlightRouteDTO(
                r.identity().value(),
                r.companyIata(),
                r.originIata(),
                r.destinationIata(),
                r.flightType().toString(),
                r.isActive(),
                r.deactivationDate());
    }

    @Override
    public String toString() {
        return String.format("%-10s | %-5s | %s → %s | %-10s | %s",
                designator, companyIata, originIata, destinationIata,
                flightType, active ? "ACTIVE" : "INACTIVE");
    }
}