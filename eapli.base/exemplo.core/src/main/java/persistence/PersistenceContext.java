package persistence;

import persistence.impl.inmemory.InMemoryAirTransportCompanyRepository;
import persistence.impl.inmemory.InMemoryAirControlAreaRepository;
import persistence.impl.inmemory.InMemoryAircraftModelRepository;
import persistence.impl.inmemory.InMemoryAircraftRepository;
import persistence.impl.jpa.JpaAirControlAreaRepository;
import persistence.impl.jpa.JpaEngineModelRepository;
import persistence.impl.jpa.JpaUserRepository;
import repositories.AircraftModelRepository;
import repositories.AircraftRepository;
import repositories.AirTransportCompanyRepository;
import repositories.AirControlAreaRepository;
import repositories.EngineModelRepository;
import repositories.UserRepository;

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
    private final AircraftModelRepository       aircraftModelRepository;
    private final AircraftRepository            aircraftRepository;

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
        this.airTransportCompanyRepository = new InMemoryAirTransportCompanyRepository();
        this.engineModelRepository        = new JpaEngineModelRepository(persistenceUnit, extendedProps);
        this.airControlAreaRepository     = new JpaAirControlAreaRepository(persistenceUnit, extendedProps);
        this.aircraftModelRepository      = new InMemoryAircraftModelRepository();
        this.aircraftRepository           = new InMemoryAircraftRepository();
    }

    public static synchronized PersistenceContext getInstance() {
        if (instance == null) {
            instance = new PersistenceContext();
        }
        return instance;
    }

    public UserRepository userRepository() {
        return userRepository;
    }

    public AirTransportCompanyRepository airTransportCompanyRepository() {
        return airTransportCompanyRepository;
    }

    public EngineModelRepository engineModelRepository() {
        return engineModelRepository;
    }

    public AirControlAreaRepository airControlAreaRepository() {
        return airControlAreaRepository;
    }

    public AircraftModelRepository aircraftModelRepository() {
        return aircraftModelRepository;
    }

    public AircraftRepository aircraftRepository() {
        return aircraftRepository;
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