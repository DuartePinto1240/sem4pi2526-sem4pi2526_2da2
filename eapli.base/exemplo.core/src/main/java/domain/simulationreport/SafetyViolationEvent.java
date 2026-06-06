// ─── domain/simulationreport/SafetyViolationEvent.java ────────────────────

package domain.simulationreport;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class SafetyViolationEvent implements ValueObject {

    private double timestamp;
    private int    flightI;
    private int    flightJ;

    private double latI;
    private double lonI;
    private double altI;
    private double vxI;
    private double vyI;
    private double vzI;

    private double latJ;
    private double lonJ;
    private double altJ;
    private double vxJ;
    private double vyJ;
    private double vzJ;

    public SafetyViolationEvent(final double timestamp,
                                final int    flightI,
                                final int    flightJ,
                                final double latI, final double lonI, final double altI,
                                final double vxI,  final double vyI,  final double vzI,
                                final double latJ, final double lonJ, final double altJ,
                                final double vxJ,  final double vyJ,  final double vzJ) {
        Preconditions.ensure(timestamp >= 0, "Timestamp must be non-negative");
        this.timestamp = timestamp;
        this.flightI   = flightI;
        this.flightJ   = flightJ;
        this.latI = latI; this.lonI = lonI; this.altI = altI;
        this.vxI  = vxI;  this.vyI  = vyI;  this.vzI  = vzI;
        this.latJ = latJ; this.lonJ = lonJ; this.altJ = altJ;
        this.vxJ  = vxJ;  this.vyJ  = vyJ;  this.vzJ  = vzJ;
    }

    public double timestamp() { return timestamp; }
    public int    flightI()   { return flightI;   }
    public int    flightJ()   { return flightJ;   }
    public double latI()      { return latI;      }
    public double lonI()      { return lonI;      }
    public double altI()      { return altI;      }
    public double vxI()       { return vxI;       }
    public double vyI()       { return vyI;       }
    public double vzI()       { return vzI;       }
    public double latJ()      { return latJ;      }
    public double lonJ()      { return lonJ;      }
    public double altJ()      { return altJ;      }
    public double vxJ()       { return vxJ;       }
    public double vyJ()       { return vyJ;       }
    public double vzJ()       { return vzJ;       }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetyViolationEvent)) return false;
        final SafetyViolationEvent that = (SafetyViolationEvent) o;
        return Double.compare(timestamp, that.timestamp) == 0
                && flightI == that.flightI && flightJ == that.flightJ;
    }

    @Override
    public int hashCode() { return Objects.hash(timestamp, flightI, flightJ); }

    @Override
    public String toString() {
        return String.format("t=%.2fs Flight%d vs Flight%d", timestamp, flightI, flightJ);
    }

    protected SafetyViolationEvent() {}
}