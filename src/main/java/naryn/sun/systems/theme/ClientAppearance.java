package naryn.sun.systems.theme;

import naryn.sun.Sun;
import naryn.sun.systems.localization.Language;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;

/** Settings shared by the ClickGUI and HUD; they are not a toggleable module. */
public final class ClientAppearance {
    public enum Mode { FACET_DARK, FACET_FROST }

    private static Mode mode = Mode.FACET_FROST;
    private static Mode hudMode = Mode.FACET_FROST;
    private static ColorRGBA accent = ColorRGBA.WHITE;
    private static Language language = Language.RU_RU;

    private ClientAppearance() { }

    public static Mode getMode() { return mode; }

    public static void setMode(Mode newMode) {
        mode = newMode == null ? Mode.FACET_FROST : newMode;
        Sun.getInstance().getThemeManager().setCurrentTheme(Theme.DARK);
    }

    public static Mode getHudMode() { return hudMode; }

    public static void setHudMode(Mode newHudMode) {
        hudMode = newHudMode == null ? Mode.FACET_FROST : newHudMode;
    }

    public static boolean isHudFrost() { return hudMode == Mode.FACET_FROST; }

    public static ColorRGBA getAccent() {
        if (PaletteConfig.getInstance().isEnabled() && PaletteConfig.getInstance().isSyncGui()) {
            return PaletteConfig.getInstance().getActiveColor();
        }
        return accent;
    }

    public static ColorRGBA getRawAccent() {
        return accent;
    }

    public static void setAccent(ColorRGBA newAccent) {
        if (newAccent == null) return;
        accent = newAccent.withAlpha(255.0F);
        if (!PaletteConfig.getInstance().isEnabled() || !PaletteConfig.getInstance().isSyncGui()) {
            Colors.ACCENT = accent;
            PaletteConfig.getInstance().setBaseAccent(accent);
        }
    }

    private static float menuBackgroundDarkness = 0.31F;
    private static float menuBackgroundBlur = 0.0F;

    public static float getMenuBackgroundDarkness() { return menuBackgroundDarkness; }

    public static void setMenuBackgroundDarkness(float darkness) {
        menuBackgroundDarkness = Math.max(0.0F, Math.min(1.0F, darkness));
    }

    public static float getMenuBackgroundBlur() { return menuBackgroundBlur; }

    public static void setMenuBackgroundBlur(float blur) {
        menuBackgroundBlur = Math.max(0.0F, Math.min(1.0F, blur));
    }

    public static boolean isFrost() { return mode == Mode.FACET_FROST; }

    public static Language getLanguage() { return language; }

    public static void setLanguage(Language newLanguage) {
        language = newLanguage == null ? Language.RU_RU : newLanguage;
        Localizator.setLanguage(language);
    }
}
