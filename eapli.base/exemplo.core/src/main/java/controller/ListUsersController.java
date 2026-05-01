package controller;

import domain.User;
import dto.UserDTO;
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
 *
 * <p>Returns {@link UserDTO} objects instead of domain {@link User} entities
 * to decouple the UI layer from the domain model (DTO pattern).</p>
 */
public class ListUsersController {

    /** Repository for domain-specific user data. */
    private final UserRepository repo = PersistenceContext.getInstance().userRepository();

    /**
     * Returns all registered users as DTOs, sorted alphabetically by name
     * (case-insensitive). Both active and inactive users are included (US033.3).
     *
     * @return sorted iterable of {@link UserDTO} records
     */
    public Iterable<UserDTO> allUsers() {
        final List<UserDTO> dtos = new ArrayList<>();

        for (User u : repo.findAll()) {
            dtos.add(new UserDTO(
                    u.identity().toString(),
                    u.getName(),
                    u.getRole(),
                    u.getPhoneNumber(),
                    u.getStaffNumber(),
                    u.isActive(),
                    u.getSecurityClearanceExpiry(),
                    u.getNextSkillsAssessmentDate()
            ));
        }

        dtos.sort(Comparator.comparing(UserDTO::getName, String.CASE_INSENSITIVE_ORDER));
        return dtos;
    }
}
