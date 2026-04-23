package controller;

import domain.User;
import domain.EmailAddress;
import repositories.UserRepository;
import persistence.impl.jpa.JpaUserRepository;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.framework.infrastructure.authz.domain.model.Role;
import java.util.Set;
import java.util.Calendar;

public class RegisterUserController {

    private UserRepository repository = null;

    private UserRepository repository() {
        if (this.repository == null) {

            this.repository = new JpaUserRepository(PersistenceContext.repositories().transactionalContext());
        }
        return this.repository;
    }

    public User registerUser(String email, String name, String phone,
                             Set<Role> roles, Calendar expiry) {

        EmailAddress emailVO = EmailAddress.valueOf(email);
        User newUser = new User(emailVO, name, phone, roles, expiry);


        System.out.println("LOG: Utilizador criado em memória (Persistência ignorada para teste)");
        return newUser;
    }
}