package naryn.sun.ui.menu.components;
import naryn.sun.systems.localization.Localizator;
/**
 * Типы карточек в разделе GUI Settings.
 */
public enum GuiSettingType {
    APPEARANCE("gui.setting.appearance.name", "gui.setting.appearance.desc"),
    BACKGROUND("gui.setting.background.name", "gui.setting.background.desc"),
    PALETTE("gui.setting.palette.name", "gui.setting.palette.desc"),
    LANGUAGE("gui.setting.language.name", "gui.setting.language.desc"),
    SOUNDS("gui.setting.sounds.name", "gui.setting.sounds.desc"),
    ANIMATIONS("gui.setting.animations.name", "gui.setting.animations.desc"),
    CONFIGS("gui.setting.configs.name", "gui.setting.configs.desc"),
    JVM_PRESETS("gui.setting.jvm_presets.name", "gui.setting.jvm_presets.desc"),
    CREDITS("gui.setting.credits.name", "gui.setting.credits.desc");

    private final String displayKey;
    private final String descriptionKey;

    GuiSettingType(String displayKey, String descriptionKey) {
        this.displayKey = displayKey;
        this.descriptionKey = descriptionKey;
    }

    public String getDisplayName() {
        return Localizator.translate(displayKey);
    }

    public String getDescription() {
        return Localizator.translate(descriptionKey);
    }
}