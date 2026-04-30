package repositories;

import domain.EmailAddress;
import domain.User;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Optional;

public interface UserRepository extends DomainRepository<EmailAddress, User> {

    /**
     * Exemplo de um método customizado: encontrar utilizadores ativos.
     */
    Iterable<User> findAllActive();

    /**
     * Encontrar por email (a identidade já faz isto, mas ajuda na legibilidade).
     */
    Optional<User> findByEmail(EmailAddress email);
}