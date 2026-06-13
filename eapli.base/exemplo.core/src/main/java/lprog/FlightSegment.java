package lprog;

import eapli.framework.validations.Preconditions;

import jakarta.persistence.*;

/**
 * A segment is a sub-section of a FlightLeg defined by geographic
 * coordinates, altitude range, corridor width and wind conditions.
 *
 * Each segment has an explicit flight phase (climb, cruise, descent)
 * and an altitude range [min, max] within which the aircraft operates
 * during that phase.
 *
 * Corresponds to the DSL block:
 *   segment {
 *       phase: climb | cruise | descent
 *       from: (lat, lon)
 *       to:   (lat, lon)
 *       altitude_range: [minFt, maxFt]
 *       width: <m> m
 *       wind:  <angle> deg <speed> m/s
 *   }
 */
@Entity
@Table(name = "FLIGHT_SEGMENT")
public class FlightSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Flight phase: "climb", "cruise", or "descent". */
    @Column(name = "phase", nullable = false)
    private String phase;

    @Column(name = "from_lat", nullable = false)
    private double fromLat;

    @Column(name = "from_lon", nullable = false)
    private double fromLon;

    @Column(name = "to_lat", nullable = false)
    private double toLat;

    @Column(name = "to_lon", nullable = false)
    private double toLon;

    /** Minimum altitude (ft) for this segment. */
    @Column(name = "altitude_min_ft", nullable = false)
    private double altitudeMin;

    /** Maximum altitude (ft) for this segment. */
    @Column(name = "altitude_max_ft", nullable = false)
    private double altitudeMax;

    @Column(name = "width_m", nullable = false)
    private double widthMeters;

    @Column(name = "wind_angle_deg", nullable = false)
    private double windAngleDeg;

    @Column(name = "wind_speed_ms", nullable = false)
    private double windSpeedMs;

    public FlightSegment(final double fromLat,    final double fromLon,
                         final double toLat,      final double toLon,
                         final double altitudeMin, final double altitudeMax,
                         final String phase,
                         final double widthMeters,
                         final double windAngleDeg,
                         final double windSpeedMs) {
        Preconditions.nonEmpty(phase, "Phase cannot be empty");
        Preconditions.ensure(altitudeMin >= 0,          "Altitude min must be non-negative");
        Preconditions.ensure(altitudeMax > altitudeMin, "Altitude max must be greater than min");
        Preconditions.ensure(widthMeters > 0,           "Width must be positive");

        this.phase        = phase.toLowerCase();
        this.fromLat      = fromLat;
        this.fromLon      = fromLon;
        this.toLat        = toLat;
        this.toLon        = toLon;
        this.altitudeMin  = altitudeMin;
        this.altitudeMax  = altitudeMax;
        this.widthMeters  = widthMeters;
        this.windAngleDeg = windAngleDeg;
        this.windSpeedMs  = windSpeedMs;
    }

    public String phase()        { return phase;        }
    public double fromLat()      { return fromLat;      }
    public double fromLon()      { return fromLon;      }
    public double toLat()        { return toLat;        }
    public double toLon()        { return toLon;        }
    public double altitudeMin()  { return altitudeMin;  }
    public double altitudeMax()  { return altitudeMax;  }
    public double widthMeters()  { return widthMeters;  }
    public double windAngleDeg() { return windAngleDeg; }
    public double windSpeedMs()  { return windSpeedMs;  }

    @Override
    public String toString() {
        return String.format(
                "Segment[%s (%.4f,%.4f)->(%.4f,%.4f) alt=[%.0f,%.0f]ft width=%.0fm wind=%.0fdeg %.1fm/s]",
                phase, fromLat, fromLon, toLat, toLon,
                altitudeMin, altitudeMax, widthMeters, windAngleDeg, windSpeedMs);
    }

    protected FlightSegment() {
        // for ORM
    }
}
