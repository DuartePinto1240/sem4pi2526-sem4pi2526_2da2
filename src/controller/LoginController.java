package controller;

import domain.User;
import domain.EmailAddress;
import persistence.PersistenceContext;
import repositories.UserRepository;
import java.util.Optional;

/**
 * Controller para Login - US030.
 * Autentica o utilizador verificando email, password e se está ativo.
 */
public class LoginController {

    private final UserRepository repo;

    public LoginController() {
        this.repo = PersistenceContext.getInstance().userRepository();
    }

    /**
     * Tenta autenticar um utilizador.
     *
     * @param email    Email do utilizador
     * @param password Password introduzida
     * @return Optional com o User se credenciais corretas e conta ativa, vazio caso contrário
     */
    public Optional<User> login(String email, String password) {
        try {
            EmailAddress emailVO = EmailAddress.valueOf(email);
            Optional<User> userOpt = repo.ofIdentity(emailVO);

            if (userOpt.isPresent()) {
                User u = userOpt.get();
                if (u.isActive() && u.checkPassword(password)) {
                    return Optional.of(u);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro no login: " + e.getMessage());
        }
        return Optional.empty();
    }
}