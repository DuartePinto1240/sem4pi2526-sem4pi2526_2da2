package domain.aircraft;

import domain.aircraftmodel.AircraftModel;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.validations.Preconditions;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "AIRCRAFT")
public class Aircraft implements AggregateRoot<String> {

    @Id
    @Column(name = "registration", unique = true, nullable = false)
    private String registration;

    @Column(name = "country_of_registration", nullable = false)
    private String countryOfRegistration;

    @Column(name = "company_iata", nullable = false)
    private String companyIata;

    @Column(name = "aircraft_model_id", nullable = false)
    private Long aircraftModelId;

    @Embedded
    private CabinConfiguration cabinConfiguration;

    @Embedded
    private OperationalStatus operationalStatus;

    @Column(name = "registration_date", nullable = false)
    private LocalDate registrationDate;

    public Aircraft(final String registration,
                    final String countryOfRegistration,
                    final String companyIata,
                    final AircraftModel model,
                    final CabinConfiguration cabinConfiguration) {
        Preconditions.nonEmpty(registration,
                "Registration cannot be empty");
        Preconditions.nonEmpty(countryOfRegistration,
                "Country of registration cannot be empty");
        Preconditions.nonEmpty(companyIata,
                "Company IATA cannot be empty");
        Preconditions.nonNull(model,
                "Aircraft model cannot be null");
        Preconditions.nonNull(cabinConfiguration,
                "Cabin configuration cannot be null");
        Preconditions.ensure(
                cabinConfiguration.totalSeats() <= model.characteristics().mtow(),
                "Total seats exceed model capacity");

        this.registration          = registration.toUpperCase().trim();
        this.countryOfRegistration = countryOfRegistration.trim();
        this.companyIata           = companyIata.toUpperCase().trim();
        this.aircraftModelId       = model.identity();
        this.cabinConfiguration    = cabinConfiguration;
        this.operationalStatus     = OperationalStatus.active();
        this.registrationDate      = LocalDate.now();
    }

    public void decommission() {
        Preconditions.ensure(operationalStatus.isActive(),
                "Aircraft is already decommissioned");
        this.operationalStatus = OperationalStatus.decommissioned();
    }

    @Override
    public String identity() {
        return registration;
    }

    @Override
    public boolean sameAs(final Object other) {
        if (!(other instanceof Aircraft)) return false;
        final Aircraft that = (Aircraft) other;
        return this.registration.equalsIgnoreCase(that.registration);
    }

    public String registration()                   { return registration;          }
    public String countryOfRegistration()          { return countryOfRegistration; }
    public String companyIata()                    { return companyIata;           }
    public Long aircraftModelId()                  { return aircraftModelId;       }
    public CabinConfiguration cabinConfiguration() { return cabinConfiguration;   }
    public OperationalStatus operationalStatus()   { return operationalStatus;     }
    public LocalDate registrationDate()            { return registrationDate;      }

    public int ageInYears() {
        return LocalDate.now().getYear() - registrationDate.getYear();
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s | %s | Registered: %s",
                registration, countryOfRegistration,
                operationalStatus, cabinConfiguration,
                registrationDate);
    }

    protected Aircraft() {
        // for ORM
    }
}