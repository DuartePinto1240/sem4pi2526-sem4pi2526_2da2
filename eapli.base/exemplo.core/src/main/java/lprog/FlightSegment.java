package lprog;

import eapli.framework.validations.Preconditions;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A segment is a sub-section of a FlightLeg defined by geographic
 * coordinates, altitude slots, corridor width and wind conditions.
 *
 * Corresponds to the DSL block:
 *   segment {
 *       from: (lat, lon)
 *       to:   (lat, lon)
 *       altitude_slots: [h1, h2, ...]
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

    @Column(name = "from_lat", nullable = false)
    private double fromLat;

    @Column(name = "from_lon", nullable = false)
    private double fromLon;

    @Column(name = "to_lat", nullable = false)
    private double toLat;

    @Column(name = "to_lon", nullable = false)
    private double toLon;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "FLIGHT_SEGMENT_ALTITUDE_SLOTS",
            joinColumns = @JoinColumn(name = "segment_id"))
    @Column(name = "altitude_m")
    private List<Double> altitudeSlots = new ArrayList<>();

    @Column(name = "width_m", nullable = false)
    private double widthMeters;

    @Column(name = "wind_angle_deg", nullable = false)
    private double windAngleDeg;

    @Column(name = "wind_speed_ms", nullable = false)
    private double windSpeedMs;

    public FlightSegment(final double fromLat, final double fromLon,
                         final double toLat,  final double toLon,
                         final List<Double> altitudeSlots,
                         final double widthMeters,
                         final double windAngleDeg,
                         final double windSpeedMs) {
        Preconditions.nonNull(altitudeSlots, "Altitude slots cannot be null");
        Preconditions.ensure(!altitudeSlots.isEmpty(), "At least one altitude slot is required");
        Preconditions.ensure(widthMeters > 0, "Width must be positive");

        this.fromLat       = fromLat;
        this.fromLon       = fromLon;
        this.toLat         = toLat;
        this.toLon         = toLon;
        this.altitudeSlots = new ArrayList<>(altitudeSlots);
        this.widthMeters   = widthMeters;
        this.windAngleDeg  = windAngleDeg;
        this.windSpeedMs   = windSpeedMs;
    }

    public double       fromLat()       { return fromLat;       }
    public double       fromLon()       { return fromLon;       }
    public double       toLat()         { return toLat;         }
    public double       toLon()         { return toLon;         }
    public List<Double> altitudeSlots() { return Collections.unmodifiableList(altitudeSlots); }
    public double       widthMeters()   { return widthMeters;   }
    public double       windAngleDeg()  { return windAngleDeg;  }
    public double       windSpeedMs()   { return windSpeedMs;   }

    @Override
    public String toString() {
        return String.format("Segment[(%.4f,%.4f)->(%.4f,%.4f) alt=%s width=%.0fm wind=%.0fdeg %.1fm/s]",
                fromLat, fromLon, toLat, toLon, altitudeSlots, widthMeters, windAngleDeg, windSpeedMs);
    }

    protected FlightSegment() {
        // for ORM
    }
}
