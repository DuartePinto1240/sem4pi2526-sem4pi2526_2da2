import mainUI.console.MainMenu;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;

public class Main {

    public static void main(final String[] args) {
            System.out.println("A inicializar a aplicação AlSafe...");
            MainMenu menu = new MainMenu();
            menu.show();

            System.out.println("Aplicação terminada com sucesso.");
    }
}