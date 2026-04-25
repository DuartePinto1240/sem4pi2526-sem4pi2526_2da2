import bootstrapper.Bootstrap;
import mainUI.console.MainMenu;

public class Main {
    public static void main(final String[] args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║    AlSafe - Sistema de Gestão        ║");
        System.out.println("║    A inicializar...                  ║");
        System.out.println("╚══════════════════════════════════════╝");

        // Bootstrap: cria a conta admin se ainda não existir
        new Bootstrap().execute();

        // Iniciar o menu principal — o loop continua até o utilizador escolher Sair
        try {
            MainMenu menu = new MainMenu();
            menu.show();
        } catch (Exception e) {
            System.out.println("Erro fatal ao iniciar: " + e.getMessage());
            e.printStackTrace();
        }
    }
}