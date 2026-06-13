package persistence;

import persistence.impl.inmemory.*;
import repositories.FlightPlanRepository;
import persistence.impl.jpa.*;
import repositories.*;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
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
    private final FlightRouteRepository         flightRouteRepository;
    private final PilotRepository               pilotRepository;
    private final SimulationReportRepository simulationReportRepository;


    // ---------------------------------------------------------------------------
    // Schema migration: removes the old altitude_slots join table (if it still
    // exists) and adds the new altitude_min_ft / altitude_max_ft / phase columns
    // to FLIGHT_SEGMENT.  Runs only when the postgres persistence unit is active.
    // Safe to run multiple times (uses IF NOT EXISTS / IF EXISTS guards).
    // ---------------------------------------------------------------------------
    private void runPostgresMigrationIfNeeded(final String persistenceUnit) {
        if (!persistenceUnit.contains("postgres")) return;

        final String url  = "jdbc:postgresql://vsgate-s1.dei.isep.ipp.pt:10466/alsafe";
        final String user = "postgres";
        final String pass = "AXfIVQJIUOIc";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement  st   = conn.createStatement()) {

            // Track whether migration has already been applied
            st.execute("CREATE TABLE IF NOT EXISTS schema_migration "
                    + "(migration_id VARCHAR(255) PRIMARY KEY, applied_at TIMESTAMP DEFAULT NOW())");

            final boolean alreadyRun;
            try (java.sql.ResultSet rs = st.executeQuery(
                    "SELECT 1 FROM schema_migration WHERE migration_id = 'sprint3_altitude_range_v2'")) {
                alreadyRun = rs.next();
            }

            if (!alreadyRun) {
                // 1. Remove old join table
                st.execute("DROP TABLE IF EXISTS flight_segment_altitude_slots CASCADE");

                // 2. Add new columns
                st.execute("ALTER TABLE flight_segment ADD COLUMN IF NOT EXISTS "
                        + "altitude_min_ft DOUBLE PRECISION NOT NULL DEFAULT 0");
                st.execute("ALTER TABLE flight_segment ADD COLUMN IF NOT EXISTS "
                        + "altitude_max_ft DOUBLE PRECISION NOT NULL DEFAULT 10000");
                st.execute("ALTER TABLE flight_segment ADD COLUMN IF NOT EXISTS "
                        + "phase VARCHAR(255) NOT NULL DEFAULT 'cruise'");

                // 3. Wipe all flight plans (old DSL format incompatible with new parser).
                //    Delete in FK order: segments → legs → plans.
                st.execute("DELETE FROM flight_segment");
                st.execute("DELETE FROM flight_leg");
                st.execute("DELETE FROM flight_plan");

                // Mark migration as done
                st.execute("INSERT INTO schema_migration (migration_id) "
                        + "VALUES ('sprint3_altitude_range_v2')");
            }

            // ── Migration 2: add pilot_email column for pilot/company isolation ──
            final boolean pilotEmailMigrationRun;
            try (java.sql.ResultSet rs2 = st.executeQuery(
                    "SELECT 1 FROM schema_migration WHERE migration_id = 'sprint3_pilot_email_v1'")) {
                pilotEmailMigrationRun = rs2.next();
            }
            if (!pilotEmailMigrationRun) {
                st.execute("ALTER TABLE flight_plan ADD COLUMN IF NOT EXISTS "
                        + "pilot_email VARCHAR(255)");
                st.execute("INSERT INTO schema_migration (migration_id) "
                        + "VALUES ('sprint3_pilot_email_v1')");
            }

            // ── Migration 3: wipe plans with NULL pilot_email (imported before fix) ──
            final boolean wipedOrphanPlans;
            try (java.sql.ResultSet rs3 = st.executeQuery(
                    "SELECT 1 FROM schema_migration WHERE migration_id = 'sprint3_pilot_email_wipe_v1'")) {
                wipedOrphanPlans = rs3.next();
            }
            if (!wipedOrphanPlans) {
                st.execute("DELETE FROM flight_segment WHERE flight_leg_id IN "
                        + "(SELECT id FROM flight_leg WHERE flight_plan_id IN "
                        + " (SELECT id FROM flight_plan WHERE pilot_email IS NULL))");
                st.execute("DELETE FROM flight_leg WHERE flight_plan_id IN "
                        + "(SELECT id FROM flight_plan WHERE pilot_email IS NULL)");
                st.execute("DELETE FROM flight_plan WHERE pilot_email IS NULL");
                st.execute("INSERT INTO schema_migration (migration_id) "
                        + "VALUES ('sprint3_pilot_email_wipe_v1')");
            }

        } catch (final SQLException e) {
            // Table does not exist yet on first-ever run — Hibernate will create it.
        }
    }

    private PersistenceContext() {
        final Properties props = loadProperties();
        final String persistenceUnit =
                props.getProperty(PERSISTENCE_UNIT_KEY, "eapli.base.AlSafePU");

        runPostgresMigrationIfNeeded(persistenceUnit);

        final Map extendedProps = new HashMap();
        final String schemaAction = props.getProperty(SCHEMA_KEY);
        if (schemaAction != null) {
            extendedProps.put(SCHEMA_KEY, schemaAction);
        }


        this.userRepository                 = new JpaUserRepository(persistenceUnit, extendedProps);
        this.airTransportCompanyRepository  = new JpaAirTransportCompanyRepository(persistenceUnit, extendedProps);
        this.engineModelRepository          = new JpaEngineModelRepository(persistenceUnit, extendedProps);
        this.airControlAreaRepository       = new JpaAirControlAreaRepository(persistenceUnit, extendedProps);
        this.weatherDataRepository          = new JpaWeatherDataRepository(persistenceUnit, extendedProps);
        this.airportRepository              = new JpaAirportRepository(persistenceUnit, extendedProps);
        this.countryRepository              = new InMemoryCountryRepository();
        this.aircraftModelRepository        = new JpaAircraftModelRepository(persistenceUnit, extendedProps);
        this.aircraftRepository             = new JpaAircraftRepository(persistenceUnit, extendedProps);
        this.flightPlanRepository           = new JpaFlightPlanRepository(persistenceUnit, extendedProps);
        this.customerRepository             = new JpaCustomerRepository(persistenceUnit, extendedProps);
        this.flightRouteRepository          = new JpaFlightRouteRepository(persistenceUnit, extendedProps);;
        this.pilotRepository                = new JpaPilotRepository(persistenceUnit, extendedProps);
        this.simulationReportRepository     = new JpaSimulationReportRepository(persistenceUnit, extendedProps);

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
    public CustomerRepository customerRepository()                       { return customerRepository; }
    public FlightRouteRepository flightRouteRepository()                 { return flightRouteRepository; }
    public PilotRepository pilotRepository()                             { return pilotRepository; }
    public SimulationReportRepository simulationReportRepository() {
        return simulationReportRepository;
    }

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