package persistence;

import persistence.impl.inmemory.InMemoryAirTransportCompanyRepository;
import persistence.impl.jpa.JpaUserRepository;
import repositories.AirTransportCompanyRepository;
import repositories.UserRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Singleton that provides access to all repository instances.
 *
 * <p>Reads {@code application.properties} from the classpath to determine
 * the JPA persistence unit name. The {@link UserRepository} uses JPA with
 * an H2 file-based database so data persists across restarts.</p>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PersistenceContext {

    private static final String PROPERTIES_FILE      = "application.properties";
    private static final String PERSISTENCE_UNIT_KEY = "persistence.persistenceUnit";
    private static final String SCHEMA_KEY            =
            "jakarta.persistence.schema-generation.database.action";

    private static PersistenceContext instance;

    private final UserRepository userRepository;
    private final AirTransportCompanyRepository airTransportCompanyRepository;

    private PersistenceContext() {
        final Properties props = loadProperties();
        final String persistenceUnit =
                props.getProperty(PERSISTENCE_UNIT_KEY, "eapli.base.AlSafePU");

        final Map extendedProps = new HashMap();
        final String schemaAction = props.getProperty(SCHEMA_KEY);
        if (schemaAction != null) {
            extendedProps.put(SCHEMA_KEY, schemaAction);
        }

        this.userRepository = new JpaUserRepository(persistenceUnit, extendedProps);
        this.airTransportCompanyRepository = new InMemoryAirTransportCompanyRepository();
    }

    /**
     * Returns the singleton instance, creating it if necessary.
     *
     * @return the single {@link PersistenceContext} instance
     */
    public static synchronized PersistenceContext getInstance() {
        if (instance == null) {
            instance = new PersistenceContext();
        }
        return instance;
    }

    /**
     * Returns the shared {@link UserRepository} backed by H2.
     *
     * @return user repository
     */
    public UserRepository userRepository() {
        return userRepository;
    }

    /**
     * Returns the shared {@link AirTransportCompanyRepository}.
     *
     * @return air transport company repository
     */
    public AirTransportCompanyRepository airTransportCompanyRepository() {
        return airTransportCompanyRepository;
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
