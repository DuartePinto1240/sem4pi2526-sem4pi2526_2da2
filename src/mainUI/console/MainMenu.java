package mainUI.console;

import eapli.framework.actions.menu.Menu;
import eapli.framework.actions.menu.MenuItem;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.ExitWithMessageAction;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;
import ui.RegisterUserUI;

public class MainMenu extends AbstractUI {

    @Override
    protected boolean doShow() {
        final Menu menu = buildMainMenu();

        final VerticalMenuRenderer renderer = new VerticalMenuRenderer(menu, new MenuItemRenderer() {
            @Override
            public void render(final MenuItem item) {
                int optionNumber = (int) item.option().orElse(0);
                String label = (optionNumber == 1) ? "Registar Utilizador" : "Sair";
                System.out.printf("%d. %s%n", optionNumber, label);
            }
        });

        return renderer.render();
    }

    private Menu buildMainMenu() {
        final Menu menu = new Menu("--- AlSafe Backoffice ---");

        menu.addItem(1, "Registar Utilizador (US031)", new RegisterUserUI()::show);
        menu.addItem(0, "Sair", new ExitWithMessageAction("Até à próxima!"));

        return menu;
    }

    @Override
    public String headline() {
        return "Menu Principal";
    }
}