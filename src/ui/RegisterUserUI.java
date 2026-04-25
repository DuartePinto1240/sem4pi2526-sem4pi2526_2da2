package ui;

import controller.RegisterUserController;
import domain.User;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * UI para US031 - Registar utilizadores do backoffice.
 * Apenas acessível pelo Admin.
 */
public class RegisterUserUI extends AbstractUI {

    private final RegisterUserController theController = new RegisterUserController();

    @Override
    protected boolean doShow() {
        System.out.println("\n--- Registar Utilizador do Backoffice ---");

        final String name        = Console.readLine("Nome Completo:");
        final String email       = Console.readLine("Email:");
        final String phoneNumber = Console.readLine("Número de Telefone:");
        final String staffNumber = Console.readLine("Número de Staff:");
        final String password    = Console.readLine("Password (mínimo 4 caracteres):");

        try {
            User u = theController.registerUser(email, name, phoneNumber, password, staffNumber);
            System.out.println("\n>> Utilizador registado com sucesso!");
            System.out.println("   Nome:    " + u.getName());
            System.out.println("   Email:   " + u.identity());
            System.out.println("   Staff Nº:" + u.getStaffNumber());
            System.out.println("   Estado:  ATIVO");
        } catch (IllegalArgumentException e) {
            System.out.println("\n>> ERRO: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("\n>> ERRO inesperado: " + e.getMessage());
        }

        // Retorna false para voltar ao menu em vez de terminar
        return false;
    }

    @Override
    public String headline() {
        return "Registar Utilizador";
    }
}