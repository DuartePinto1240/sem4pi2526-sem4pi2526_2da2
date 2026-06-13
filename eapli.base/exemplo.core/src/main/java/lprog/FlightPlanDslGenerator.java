package lprog;

import java.util.List;

/**
 * Generates a Core Flight DSL string from a FlightPlan domain object
 * or directly from raw leg data.
 *
 * Used by US080 to automatically produce the DSL after manual input.
 */
public class FlightPlanDslGenerator {

    /**
     * Generates DSL directly from raw legs.
     */
    public String generate(final String flightId,
                           final String flightType,
                           final List<FlightLeg> legs) {
        final StringBuilder sb = new StringBuilder();
        sb.append("flight ").append(flightId).append(" {\n");
        sb.append("    type: ").append(flightType).append("\n");
        for (final FlightLeg leg : legs) {
            appendLeg(sb, leg);
        }
        sb.append("}\n");
        return sb.toString();
    }

    /** Generates DSL from a persisted FlightPlan (e.g. for display/re-export). */
    public String generate(final FlightPlan plan) {
        return generate(plan.flightId(), plan.flightType(), plan.legs());
    }

    // ── shared leg/segment formatting ────────────────────────────────────────

    private void appendLeg(final StringBuilder sb, final FlightLeg leg) {
        sb.append("    leg {\n");
        sb.append("        departure: ")
          .append(leg.departureAirport()).append(" ")
          .append(leg.departureDate()).append(" ")
          .append(leg.departureTime()).append("\n");
        sb.append("        arrival:   ")
          .append(leg.arrivalAirport()).append(" ")
          .append(leg.arrivalTime()).append("\n");
        sb.append("        fuel:      ")
          .append(formatFuel(leg.fuelQuantity())).append(" ")
          .append(leg.fuelUnit()).append("\n");

        for (final FlightSegment seg : leg.segments()) {
            sb.append("        segment {\n");
            sb.append("            phase: ")
              .append(seg.phase()).append("\n");
            sb.append("            from: (")
              .append(formatCoord(seg.fromLat())).append(", ")
              .append(formatCoord(seg.fromLon())).append(")\n");
            sb.append("            to:   (")
              .append(formatCoord(seg.toLat())).append(", ")
              .append(formatCoord(seg.toLon())).append(")\n");
            sb.append("            altitude_range: [")
              .append(formatAlt(seg.altitudeMin())).append(", ")
              .append(formatAlt(seg.altitudeMax())).append("]\n");
            sb.append("            width: ")
              .append((int) seg.widthMeters()).append(" m\n");
            sb.append("            wind:  ")
              .append((int) seg.windAngleDeg()).append(" deg ")
              .append(formatSpeed(seg.windSpeedMs())).append(" m/s\n");
            sb.append("        }\n");
        }

        sb.append("    }\n");
    }

    private String formatCoord(final double v) {
        return String.format("%.4f", v);
    }

    private String formatFuel(final double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    private String formatSpeed(final double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    private String formatAlt(final double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
    }
}
