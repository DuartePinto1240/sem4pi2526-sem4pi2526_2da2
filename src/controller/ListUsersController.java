package controller;

import domain.User;
import persistence.PersistenceContext;
import repositories.UserRepository;

/**
 * Controller para US033 - Listar utilizadores do backoffice com o seu estado.
 */
public class ListUsersController {

    private final UserRepository repo;

    public ListUsersController() {
        this.repo = PersistenceContext.getInstance().userRepository();
    }

    /**
     * Devolve todos os utilizadores registados no sistema, incluindo o seu estado.
     */
    public Iterable<User> allUsers() {
        return repo.findAll();
    }
}