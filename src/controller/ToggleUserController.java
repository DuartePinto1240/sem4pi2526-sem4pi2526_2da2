package controller;

import domain.EmailAddress;
import domain.User;
import persistence.PersistenceContext;
import repositories.UserRepository;

import java.util.Optional;

/**
 * Controller para US032 - Ativar/Desativar utilizadores do backoffice.
 * Apenas o Admin pode executar estas operações.
 */
public class ToggleUserController {

    private final UserRepository repo;

    public ToggleUserController() {
        this.repo = PersistenceContext.getInstance().userRepository();
    }

    /**
     * Devolve todos os utilizadores (ativos e inativos), exceto o admin.
     */
    public Iterable<User> allUsers() {
        return repo.findAll();
    }

    /**
     * Desativa um utilizador pelo seu email.
     *
     * @param email Email do utilizador a desativar
     * @return true se desativado com sucesso
     * @throws IllegalArgumentException se o utilizador não existir ou for admin
     */
    public boolean deactivateUser(String email) {
        User user = findOrThrow(email);
        if (user.isAdmin()) {
            throw new IllegalArgumentException("Não é permitido desativar a conta de administrador.");
        }
        if (!user.isActive()) {
            throw new IllegalArgumentException("O utilizador já está inativo.");
        }
        user.deactivate();
        repo.save(user);
        return true;
    }

    /**
     * Ativa um utilizador pelo seu email.
     *
     * @param email Email do utilizador a ativar
     * @return true se ativado com sucesso
     * @throws IllegalArgumentException se o utilizador não existir
     */
    public boolean activateUser(String email) {
        User user = findOrThrow(email);
        if (user.isActive()) {
            throw new IllegalArgumentException("O utilizador já está ativo.");
        }
        user.activate();
        repo.save(user);
        return true;
    }

    private User findOrThrow(String email) {
        EmailAddress emailVO = EmailAddress.valueOf(email);
        Optional<User> userOpt = repo.ofIdentity(emailVO);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Utilizador não encontrado: " + email);
        }
        return userOpt.get();
    }
}