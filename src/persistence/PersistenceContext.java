package persistence;

import persistence.impl.inmemory.InMemoryUserRepository;
import repositories.UserRepository;

/**
 * Contexto de persistência centralizado (Singleton).
 * Garante que todos os controllers usam o mesmo repositório.
 * Em produção, bastaria substituir InMemoryUserRepository por JpaUserRepository.
 */
public class PersistenceContext {

    private static PersistenceContext instance;

    private final UserRepository userRepository;

    private PersistenceContext() {
        this.userRepository = new InMemoryUserRepository();
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
}