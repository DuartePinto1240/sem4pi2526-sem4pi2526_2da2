package ui;

import controller.ListUsersController;
import domain.User;
import eapli.framework.presentation.console.AbstractUI;

/**
 * UI para US033 - Listar utilizadores do backoffice com o seu estado.
 */
public class ListUsersUI extends AbstractUI {

    private final ListUsersController theController = new ListUsersController();

    @Override
    protected boolean doShow() {
        System.out.println("\n--- Lista de Utilizadores do Backoffice ---");
        System.out.printf("%-20s | %-25s | %-12s | %-10s | %s%n",
                "Nome", "Email", "Staff Nº", "Telefone", "Estado");
        System.out.println("-".repeat(85));

        boolean hasUsers = false;
        int total = 0, ativos = 0;

        for (User u : theController.allUsers()) {
            System.out.println(u);
            total++;
            if (u.isActive()) ativos++;
            hasUsers = true;
        }

        if (!hasUsers) {
            System.out.println("  (nenhum utilizador registado)");
        } else {
            System.out.println("-".repeat(85));
            System.out.printf("Total: %d utilizador(es) | Ativos: %d | Inativos: %d%n",
                    total, ativos, total - ativos);
        }

        return false;
    }

    @Override
    public String headline() {
        return "Listar Utilizadores";
    }
}