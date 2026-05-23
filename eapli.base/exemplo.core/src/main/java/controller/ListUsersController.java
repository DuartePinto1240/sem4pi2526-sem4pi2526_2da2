package controller;

import domain.BaseRoles;
import domain.User;
import dto.UserDTO;
import persistence.PersistenceContext;
import repositories.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Controller for US033 — List BackOffice Operators.
 *
 * <p>Returns only users with the {@code BACKOFFICE_OPERATOR} role, sorted
 * alphabetically by name, including both active and inactive accounts.</p>
 *
 * <p>Returns {@link UserDTO} objects instead of domain {@link User} entities
 * to decouple the UI layer from the domain model (DTO pattern).</p>
 */
public class ListUsersController {

    /** Repository for domain-specific user data. */
    private final UserRepository repo = PersistenceContext.getInstance().userRepository();

    /**
     * Returns all BackOffice Operators as DTOs, sorted alphabetically by name
     * (case-insensitive). Both active and inactive operators are included (US033.3).
     *
     * @return sorted iterable of {@link UserDTO} records for BACKOFFICE_OPERATOR role
     */
    public Iterable<UserDTO> allUsers() {
        final List<UserDTO> dtos = new ArrayList<>();

        for (User u : repo.findByRole(BaseRoles.BACKOFFICE_OPERATOR.toString())) {
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
