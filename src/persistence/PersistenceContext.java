package persistence;

import persistence.impl.inmemory.InMemoryUserRepository;
import persistence.impl.inmemory.InMemoryAirTransportCompanyRepository;
import repositories.UserRepository;
import repositories.AirTransportCompanyRepository;

public class PersistenceContext {

    private static PersistenceContext instance;

    private final UserRepository userRepository;
    private final AirTransportCompanyRepository airTransportCompanyRepository;

    private PersistenceContext() {
        this.userRepository = new InMemoryUserRepository();
        this.airTransportCompanyRepository = new InMemoryAirTransportCompanyRepository();
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
}