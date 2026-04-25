package controller;

import domain.EmailAddress;
import domain.User;
import persistence.PersistenceContext;
import repositories.UserRepository;

/**
 * Controller para US031 - Registar utilizadores do backoffice.
 * Só o Admin pode registar utilizadores (verificação feita na UI/Menu).
 */
public class RegisterUserController {

    private final UserRepository repo;

    public RegisterUserController() {
        this.repo = PersistenceContext.getInstance().userRepository();
    }

    /**
     * Regista um novo utilizador no sistema.
     *
     * @param email       Email único do utilizador (serve de ID)
     * @param name        Nome completo
     * @param phoneNumber Número de telefone
     * @param password    Password de acesso
     * @param staffNumber Número de staff único
     * @return O utilizador criado
     * @throws IllegalArgumentException se o email já existir
     */
    public User registerUser(String email, String name, String phoneNumber,
                             String password, String staffNumber) {
        EmailAddress emailVO = EmailAddress.valueOf(email);

        if (repo.ofIdentity(emailVO).isPresent()) {
            throw new IllegalArgumentException("Já existe um utilizador com o email: " + email);
        }

        User newUser = new User(emailVO, name, phoneNumber, password, staffNumber);
        repo.save(newUser);
        return newUser;
    }

    /**
     * Versão interna usada pelo bootstrapper para criar o admin.
     */
    public User registerAdmin(String email, String name, String phoneNumber,
                              String password, String staffNumber) {
        EmailAddress emailVO = EmailAddress.valueOf(email);

        // Se já existe (ex: segunda execução do bootstrap), não duplicar
        if (repo.ofIdentity(emailVO).isPresent()) {
            return repo.ofIdentity(emailVO).get();
        }

        User admin = new User(emailVO, name, phoneNumber, password, staffNumber);
        admin.makeAdmin();
        repo.save(admin);
        return admin;
    }
}