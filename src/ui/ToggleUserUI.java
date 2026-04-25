package ui;

import controller.ListUsersController;
import controller.ToggleUserController;
import domain.User;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * UI para US032 - Ativar/Desativar utilizadores do backoffice.
 * Apenas acessível pelo Admin.
 */
public class ToggleUserUI extends AbstractUI {

    private final ToggleUserController toggleController = new ToggleUserController();
    private final ListUsersController  listController   = new ListUsersController();

    @Override
    protected boolean doShow() {
        System.out.println("\n--- Ativar / Desativar Utilizador ---");

        // Mostrar lista de utilizadores para ajudar na escolha
        printUserList();

        String email = Console.readLine("\nEmail do utilizador a alterar (ou ENTER para cancelar):");
        if (email == null || email.isBlank()) {
            System.out.println("Operação cancelada.");
            return false;
        }

        System.out.println("\n1. Ativar utilizador");
        System.out.println("2. Desativar utilizador");
        int option = Console.readInteger("Escolha");

        try {
            switch (option) {
                case 1:
                    toggleController.activateUser(email);
                    System.out.println(">> Utilizador ativado com sucesso.");
                    break;
                case 2:
                    toggleController.deactivateUser(email);
                    System.out.println(">> Utilizador desativado com sucesso.");
                    break;
                default:
                    System.out.println("Opção inválida.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println(">> ERRO: " + e.getMessage());
        }

        return false;
    }

    private void printUserList() {
        System.out.println("\nUtilizadores registados:");
        System.out.printf("%-20s | %-25s | %-12s | %-10s | %s%n",
                "Nome", "Email", "Staff Nº", "Telefone", "Estado");
        System.out.println("-".repeat(85));

        boolean hasUsers = false;
        for (User u : listController.allUsers()) {
            System.out.println(u);
            hasUsers = true;
        }
        if (!hasUsers) {
            System.out.println("  (nenhum utilizador registado)");
        }
    }

    @Override
    public String headline() {
        return "Ativar/Desativar Utilizador";
    }
}