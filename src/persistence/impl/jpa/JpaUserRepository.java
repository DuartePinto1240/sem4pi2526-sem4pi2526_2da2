package persistence.impl.jpa;

import domain.EmailAddress;
import domain.User;
import repositories.UserRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JpaUserRepository
        extends JpaAutoTxRepository<User, EmailAddress, EmailAddress>
        implements UserRepository {

    public JpaUserRepository(TransactionalContext autoTx) {
        super(autoTx, "email");
    }

    @Override
    public Iterable<User> findAllActive() {
        return match("e.active = true");
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        final Map<String, Object> params = new HashMap<>();
        params.put("email", email);
        return matchOne("e.email = :email", params);
    }
}
