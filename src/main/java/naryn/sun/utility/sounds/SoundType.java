package naryn.sun.utility.sounds;

public enum SoundType {
    SCROLL("scroll", "menu.gui_settings.sounds.scroll", 0.7F),
    TYPING("typing", "menu.gui_settings.sounds.typing", 0.8F),
    MODULE_TOGGLE("toggle", "menu.gui_settings.sounds.module_toggle", 0.8F),
    BUTTON_CLICK("click", "menu.gui_settings.sounds.button_click", 0.8F),
    MENU_OPEN("open", "menu.gui_settings.sounds.menu_open", 0.85F);

    private final String soundName;
    private final String nameKey;
    private final float defaultVolume;

    SoundType(String soundName, String nameKey, float defaultVolume) {
        this.soundName = soundName;
        this.nameKey = nameKey;
        this.defaultVolume = defaultVolume;
    }

    public String getSoundName() {
        return soundName;
    }

    public String getNameKey() {
        return nameKey;
    }

    public float getDefaultVolume() {
        return defaultVolume;
    }
}
