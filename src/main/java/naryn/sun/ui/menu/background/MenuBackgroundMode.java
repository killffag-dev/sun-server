package naryn.sun.ui.menu.background;

import naryn.sun.systems.localization.Localizator;

public enum MenuBackgroundMode {
    OFF("menu.gui_settings.background.mode.off"),
    CYBER_GRID("menu.gui_settings.background.mode.cyber_grid"),
    WEB("menu.gui_settings.background.mode.web"),
    HEXAGONS("menu.gui_settings.background.mode.hexagons"),
    WARP("menu.gui_settings.background.mode.warp");

    private final String nameKey;

    MenuBackgroundMode(String nameKey) {
        this.nameKey = nameKey;
    }

    public String getNameKey() {
        return nameKey;
    }

    public String getDisplayName() {
        return Localizator.translate(nameKey);
    }
}
