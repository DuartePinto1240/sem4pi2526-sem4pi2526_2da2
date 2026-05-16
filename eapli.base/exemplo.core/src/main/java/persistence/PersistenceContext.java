package persistence;

import persistence.impl.inmemory.*;
import repositories.FlightPlanRepository;
import persistence.impl.jpa.*;
import repositories.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@SuppressWarnings({"rawtypes", "unchecked"})
public class PersistenceContext {

    private static final String PROPERTIES_FILE      = "application.properties";
    private static final String PERSISTENCE_UNIT_KEY = "persistence.persistenceUnit";
    private static final String SCHEMA_KEY           =
            "jakarta.persistence.schema-generation.database.action";

    private static PersistenceContext instance;

    private final UserRepository                userRepository;
    private final AirTransportCompanyRepository airTransportCompanyRepository;
    private final EngineModelRepository         engineModelRepository;
    private final AirControlAreaRepository      airControlAreaRepository;
    private final WeatherDataRepository         weatherDataRepository;
    private final AirportRepository             airportRepository;
    private final CountryRepository             countryRepository;
    private final AircraftModelRepository       aircraftModelRepository;
    private final AircraftRepository            aircraftRepository;
    private final FlightPlanRepository          flightPlanRepository;
    private final CustomerRepository            customerRepository;

    private PersistenceContext() {
        final Properties props = loadProperties();
        final String persistenceUnit =
                props.getProperty(PERSISTENCE_UNIT_KEY, "eapli.base.AlSafePU");

        final Map extendedProps = new HashMap();
        final String schemaAction = props.getProperty(SCHEMA_KEY);
        if (schemaAction != null) {
            extendedProps.put(SCHEMA_KEY, schemaAction);
        }


        this.userRepository               = new JpaUserRepository(persistenceUnit, extendedProps);
        this.airTransportCompanyRepository = new JpaAirTransportCompanyRepository(persistenceUnit, extendedProps);
        this.engineModelRepository        = new JpaEngineModelRepository(persistenceUnit, extendedProps);
        this.airControlAreaRepository     = new JpaAirControlAreaRepository(persistenceUnit, extendedProps);
        this.weatherDataRepository        = new JpaWeatherDataRepository(persistenceUnit, extendedProps);
        this.airportRepository            = new JpaAirportRepository(persistenceUnit, extendedProps);
        this.countryRepository            = new InMemoryCountryRepository();
        this.aircraftModelRepository      = new InMemoryAircraftModelRepository();
        this.aircraftRepository           = new InMemoryAircraftRepository();
        this.flightPlanRepository         = new InMemoryFlightPlanRepository();
        this.customerRepository           = new JpaCustomerRepository(persistenceUnit, extendedProps);
    }

    public static synchronized PersistenceContext getInstance() {
        if (instance == null) {
            instance = new PersistenceContext();
        }
        return instance;
    }

    public UserRepository userRepository()                               { return userRepository;                }
    public AirTransportCompanyRepository airTransportCompanyRepository() { return airTransportCompanyRepository; }
    public EngineModelRepository engineModelRepository()                 { return engineModelRepository;         }
    public AirControlAreaRepository airControlAreaRepository()           { return airControlAreaRepository;      }
    public WeatherDataRepository weatherDataRepository()                 { return weatherDataRepository;         }
    public AirportRepository airportRepository()                         { return airportRepository;             }
    public CountryRepository countryRepository()                         { return countryRepository;             }
    public AircraftModelRepository aircraftModelRepository()             { return aircraftModelRepository;       }
    public AircraftRepository aircraftRepository()                       { return aircraftRepository;            }
    public FlightPlanRepository flightPlanRepository()                   { return flightPlanRepository;          }
    public CustomerRepository customerRepository() { return customerRepository; }

    private static Properties loadProperties() {
        final Properties props = new Properties();
        try (final InputStream is = PersistenceContext.class
                .getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (is != null) props.load(is);
        } catch (final IOException e) {
            // fall back to defaults defined in persistence.xml
        }
        return props;
    }
}