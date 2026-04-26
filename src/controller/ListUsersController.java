package controller;

import domain.User;
import persistence.PersistenceContext;
import repositories.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Controller for US033 — List backoffice users.
 *
 * <p>Returns all registered users sorted alphabetically by name,
 * including both active and inactive accounts with their current status.</p>
 */
public class ListUsersController {

    /** Repository for domain-specific user data. */
    private final UserRepository repo = PersistenceContext.getInstance().userRepository();

    /**
     * Returns all registered users sorted alphabetically by name (case-insensitive).
     * Both active and inactive users are included (US033.3).
     *
     * @return sorted iterable of all {@link User} records
     */
    public Iterable<User> allUsers() {
        final List<User> users = new ArrayList<>();
        for (User u : repo.findAll()) {
            users.add(u);
        }
        users.sort(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER));
        return users;
    }
}
