package mainUI.console;

import controller.LoginController;
import domain.User;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import ui.ListUsersUI;
import ui.RegisterUserUI;
import ui.ToggleUserUI;

import java.util.Optional;

/**
 * Menu principal do sistema AlSafe.
 *
 * Antes do login: só mostra Login e Sair.
 * Após login como Admin: mostra todas as opções de gestão (US031, US032, US033).
 * Após login como utilizador normal: mostra apenas as opções do seu perfil.
 */
public class MainMenu extends AbstractUI {

    private final LoginController loginController = new LoginController();
    private User currentUser = null;

    @Override
    protected boolean doShow() {
        int option = -1;

        while (option != 0) {
            printMenu();
            option = Console.readInteger("Escolha uma opção");
            handleOption(option);
        }

        return false;
    }

    // ---------------------------------------------------------------
    // Menu
    // ---------------------------------------------------------------

    private void printMenu() {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║        AlSafe - Menu Principal       ║");
        System.out.println("╠══════════════════════════════════════╣");

        if (currentUser == null) {
            // Sem sessão iniciada: login e registo disponíveis
            System.out.println("║  1. Login                            ║");
            System.out.println("║  2. Registar utilizador              ║");
            System.out.println("║  0. Sair                             ║");

        } else if (currentUser.isAdmin()) {
            // Admin autenticado
            System.out.printf("║ Logado: %-29s║%n", currentUser.getName());
            System.out.printf("║ Perfil: %-29s║%n", "ADMINISTRADOR");
            System.out.println("╠══════════════════════════════════════╣");
            System.out.println("║  [Gestão de Utilizadores]            ║");
            System.out.println("║  1. Ativar/Desativar user            ║");
            System.out.println("║  2. Listar utilizadores              ║");
            System.out.println("╠══════════════════════════════════════╣");
            System.out.println("║  9. Logout                           ║");
            System.out.println("║  0. Sair                             ║");

        } else {
            // Utilizador normal autenticado
            System.out.printf("║ Logado: %-29s║%n", currentUser.getName());
            System.out.printf("║ Perfil: %-29s║%n", "Utilizador");
            System.out.println("╠══════════════════════════════════════╣");
            System.out.println("║  9. Logout                           ║");
            System.out.println("║  0. Sair                             ║");
        }

        System.out.println("╚══════════════════════════════════════╝");
    }

    private void handleOption(int option) {
        if (currentUser == null) {
            switch (option) {
                case 1: doLogin();               break;
                case 2: new RegisterUserUI().show(); break;
                case 0: doExit();                break;
                default: System.out.println(">> Opção inválida. Tente novamente.");
            }
        } else if (currentUser.isAdmin()) {
            switch (option) {
                case 1: new ToggleUserUI().show();   break;
                case 2: new ListUsersUI().show();    break;
                case 9: doLogout();              break;
                case 0: doExit();                break;
                default: System.out.println(">> Opção inválida. Tente novamente.");
            }
        } else {
            switch (option) {
                case 9: doLogout(); break;
                case 0: doExit();   break;
                default: System.out.println(">> Opção inválida ou sem permissão.");
            }
        }
    }

    // ---------------------------------------------------------------
    // Ações
    // ---------------------------------------------------------------

    private void doLogin() {
        System.out.println("\n--- Login ---");
        String email = Console.readLine("Email:");
        String pass  = Console.readLine("Password:");

        Optional<User> result = loginController.login(email, pass);

        if (result.isPresent()) {
            currentUser = result.get();
            System.out.println("\n>> Login efetuado com sucesso!");
            System.out.println("   Bem-vindo(a), " + currentUser.getName() + "!");
            if (currentUser.isAdmin()) {
                System.out.println("   Perfil: ADMINISTRADOR");
            }
        } else {
            System.out.println("\n>> Erro: Email ou password incorretos, ou conta inativa.");
        }
    }

    private void doLogout() {
        System.out.println("\n>> Logout efetuado. Até logo, " + currentUser.getName() + "!");
        currentUser = null;
    }

    private void doExit() {
        System.out.println("\nA fechar o sistema AlSafe. Até logo!");
        System.exit(0);
    }

    @Override
    public String headline() {
        return "AlSafe - Menu Principal";
    }
}