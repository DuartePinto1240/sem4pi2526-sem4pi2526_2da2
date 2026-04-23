package ui;

import controller.RegisterUserController;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class RegisterUserUI extends AbstractUI {

    private final RegisterUserController theController = new RegisterUserController();

    @Override
    protected boolean doShow() {
        System.out.println("\n--- Registo de Utilizador do Backoffice ---");

        final String email = Console.readLine("Email:");
        final String name = Console.readLine("Nome Completo:");
        final String phone = Console.readLine("Número de Telefone:");

        // No enunciado, o Security Clearance expira. Vamos pedir os dias de validade:
        final int days = Console.readInteger("Dias de validade do Security Clearance (ex: 365):");
        final Calendar expiry = Calendar.getInstance();
        expiry.add(Calendar.DAY_OF_YEAR, days);

        // Para simplificar agora, passamos um Set de Roles vazio ou fixo
        final Set<Role> roles = new HashSet<>();

        try {
            this.theController.registerUser(email, name, phone, roles, expiry);
            System.out.println("\n>> Utilizador registado com sucesso.");
        } catch (final Exception e) {
            System.out.println("\n>> ERRO: " + e.getMessage());
        }

        return true;
    }

    @Override
    public String headline() {
        return "US031 - Registar Utilizador";
    }
}