package domain;

import eapli.framework.domain.model.AggregateRoot;
import jakarta.persistence.*;

/**
 * Entidade agregada User.
 * Representa um utilizador do sistema AlSafe (US031, US032, US033).
 */
@Entity
@Table(name = "ALSAFE_USER")
public final class User implements AggregateRoot<EmailAddress> {

    @EmbeddedId
    private EmailAddress email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String staffNumber;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean admin;

    protected User() {
        // Para o JPA
    }

    /**
     * Construtor principal — US031.
     */
    public User(EmailAddress email, String name, String phoneNumber,
                String password, String staffNumber) {
        if (email == null || name == null || password == null
                || staffNumber == null || phoneNumber == null) {
            throw new IllegalArgumentException("Todos os campos são obrigatórios.");
        }
        if (name.isBlank()) throw new IllegalArgumentException("O nome não pode estar vazio.");
        if (password.length() < 4) throw new IllegalArgumentException("A password deve ter pelo menos 4 caracteres.");
        if (staffNumber.isBlank()) throw new IllegalArgumentException("O número de staff não pode estar vazio.");

        this.email       = email;
        this.name        = name;
        this.phoneNumber = phoneNumber;
        this.password    = password;
        this.staffNumber = staffNumber;
        this.active      = true;
        this.admin       = false;
    }

    /** Promove este utilizador a administrador (usado no bootstrap). */
    public void makeAdmin() {
        this.admin = true;
    }

    /** US032 - Desativa a conta. */
    public void deactivate() { this.active = false; }

    /** US032 - Ativa a conta. */
    public void activate()   { this.active = true; }

    public boolean isActive() { return this.active; }
    public boolean isAdmin()  { return this.admin; }

    public boolean checkPassword(String password) {
        return this.password != null && this.password.equals(password);
    }

    public String getName()        { return name; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getStaffNumber() { return staffNumber; }

    @Override
    public boolean sameAs(Object other) {
        if (!(other instanceof User)) return false;
        return this.email.equals(((User) other).email);
    }

    @Override
    public EmailAddress identity() { return this.email; }

    @Override
    public String toString() {
        return String.format("%-20s | %-25s | %-12s | %-10s | %s",
                name, email.toString(), staffNumber, phoneNumber,
                active ? "ATIVO" : "INATIVO");
    }
}