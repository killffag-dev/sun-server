package naryn.sun.ui.menu;

/**
 * Вкладки главного окна ClickGUI. Раньше был приватным вложенным enum
 * внутри NewScreen — вынесен наружу и сделан public, чтобы панели рендера
 * (naryn.sun.ui.menu.panels.*) тоже могли на него ссылаться.
 */
public enum MenuTab {
    VISUALS, UTILITY, OPTIMIZATION, FAVORITES
}
