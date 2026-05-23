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

    /**
     * Returns all users that have the given role label (e.g. {@code "BACKOFFICE_OPERATOR"}).
     *
     * @param role the role string to filter by
     * @return iterable of matching {@link User} records
     */
    Iterable<User> findByRole(String role);

    /**
     * Returns {@code true} if a user with the given phone number already exists.
     *
     * @param phone the phone number to check
     * @return {@code true} if already taken
     */
    boolean existsByPhone(String phone);

    /**
     * Returns {@code true} if a user with the given staff number already exists.
     *
     * @param staffNumber the staff number to check
     * @return {@code true} if already taken
     */
    boolean existsByStaffNumber(String staffNumber);
}