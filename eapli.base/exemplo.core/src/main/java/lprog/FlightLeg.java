package lprog;

import eapli.framework.validations.Preconditions;

import jakarta.persistence.*;

@Entity
@Table(name = "FLIGHT_LEG")
public class FlightLeg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "departure_airport", nullable = false)
    private String departureAirport;

    @Column(name = "departure_date", nullable = false)
    private String departureDate;

    @Column(name = "departure_time", nullable = false)
    private String departureTime;

    @Column(name = "arrival_airport", nullable = false)
    private String arrivalAirport;

    @Column(name = "arrival_time", nullable = false)
    private String arrivalTime;

    @Column(name = "fuel_quantity", nullable = false)
    private double fuelQuantity;

    @Column(name = "fuel_unit", nullable = false)
    private String fuelUnit;

    public FlightLeg(final String departureAirport,
                     final String departureDate,
                     final String departureTime,
                     final String arrivalAirport,
                     final String arrivalTime,
                     final double fuelQuantity,
                     final String fuelUnit) {
        Preconditions.nonEmpty(departureAirport, "Departure airport cannot be empty");
        Preconditions.nonEmpty(departureDate,    "Departure date cannot be empty");
        Preconditions.nonEmpty(departureTime,    "Departure time cannot be empty");
        Preconditions.nonEmpty(arrivalAirport,   "Arrival airport cannot be empty");
        Preconditions.nonEmpty(arrivalTime,      "Arrival time cannot be empty");
        Preconditions.ensure(fuelQuantity > 0,   "Fuel quantity must be positive");
        Preconditions.nonEmpty(fuelUnit,         "Fuel unit cannot be empty");

        this.departureAirport = departureAirport;
        this.departureDate    = departureDate;
        this.departureTime    = departureTime;
        this.arrivalAirport   = arrivalAirport;
        this.arrivalTime      = arrivalTime;
        this.fuelQuantity     = fuelQuantity;
        this.fuelUnit         = fuelUnit;
    }

    public String departureAirport() { return departureAirport; }
    public String departureDate()    { return departureDate;    }
    public String departureTime()    { return departureTime;    }
    public String arrivalAirport()   { return arrivalAirport;   }
    public String arrivalTime()      { return arrivalTime;      }
    public double fuelQuantity()     { return fuelQuantity;     }
    public String fuelUnit()         { return fuelUnit;         }

    @Override
    public String toString() {
        return String.format("%s(%s %s) → %s(%s) | fuel: %.0f %s",
                departureAirport, departureDate, departureTime,
                arrivalAirport, arrivalTime, fuelQuantity, fuelUnit);
    }

    protected FlightLeg() {
        // for ORM
    }
}