package naryn.sun.systems.theme;

import com.google.gson.JsonObject;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;

public class PaletteConfig {
    private static final PaletteConfig INSTANCE = new PaletteConfig();

    public enum PaletteMode {
        RAINBOW("menu.gui_settings.palette.mode.rainbow"),
        WHITE("menu.gui_settings.palette.mode.white"),
        PURPLE("menu.gui_settings.palette.mode.purple"),
        SKY("menu.gui_settings.palette.mode.sky"),
        EMERALD("menu.gui_settings.palette.mode.emerald"),
        ROSE("menu.gui_settings.palette.mode.rose"),
        SUNSET("menu.gui_settings.palette.mode.sunset"),
        GOLD("menu.gui_settings.palette.mode.gold"),
        CUSTOM("menu.gui_settings.palette.mode.custom");

        private final String nameKey;

        PaletteMode(String nameKey) {
            this.nameKey = nameKey;
        }

        public String getNameKey() {
            return nameKey;
        }
    }

    private boolean enabled = false;
    private PaletteMode mode = PaletteMode.WHITE;
    private ColorRGBA customColor = ColorRGBA.WHITE;
    private ColorRGBA baseAccent = ColorRGBA.WHITE;
    private boolean syncGui = true;
    private boolean syncModules = true;

    private PaletteConfig() {
    }

    public static PaletteConfig getInstance() {
        return INSTANCE;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        apply();
    }

    public PaletteMode getMode() {
        return mode;
    }

    public void setMode(PaletteMode mode) {
        this.mode = mode == null ? PaletteMode.WHITE : mode;
        apply();
    }

    public ColorRGBA getCustomColor() {
        return customColor;
    }

    public void setCustomColor(ColorRGBA customColor) {
        if (customColor != null) {
            this.customColor = customColor;
            apply();
        }
    }

    public ColorRGBA getBaseAccent() {
        return baseAccent;
    }

    public void setBaseAccent(ColorRGBA baseAccent) {
        if (baseAccent != null) {
            this.baseAccent = baseAccent;
        }
    }

    public boolean isSyncGui() {
        return syncGui;
    }

    public void setSyncGui(boolean syncGui) {
        this.syncGui = syncGui;
        apply();
    }

    public boolean isSyncModules() {
        return syncModules;
    }

    public void setSyncModules(boolean syncModules) {
        this.syncModules = syncModules;
    }

    public ColorRGBA getActiveColor() {
        return switch (mode) {
            case RAINBOW -> getRainbowColor(0.0F, 0.85F, 1.0F);
            case WHITE   -> ColorRGBA.WHITE;
            case PURPLE  -> new ColorRGBA(151.0F, 71.0F, 255.0F, 255.0F);
            case SKY     -> new ColorRGBA(56.0F, 189.0F, 248.0F, 255.0F);
            case EMERALD -> new ColorRGBA(16.0F, 185.0F, 129.0F, 255.0F);
            case ROSE    -> new ColorRGBA(244.0F, 63.0F, 94.0F, 255.0F);
            case SUNSET  -> new ColorRGBA(249.0F, 115.0F, 22.0F, 255.0F);
            case GOLD    -> new ColorRGBA(251.0F, 191.0F, 36.0F, 255.0F);
            case CUSTOM  -> customColor != null ? customColor : ColorRGBA.WHITE;
        };
    }

    public static ColorRGBA getStaticColorForMode(PaletteMode m) {
        return switch (m) {
            case RAINBOW -> getRainbowColor(0.0F, 0.85F, 1.0F);
            case WHITE   -> ColorRGBA.WHITE;
            case PURPLE  -> new ColorRGBA(151.0F, 71.0F, 255.0F, 255.0F);
            case SKY     -> new ColorRGBA(56.0F, 189.0F, 248.0F, 255.0F);
            case EMERALD -> new ColorRGBA(16.0F, 185.0F, 129.0F, 255.0F);
            case ROSE    -> new ColorRGBA(244.0F, 63.0F, 94.0F, 255.0F);
            case SUNSET  -> new ColorRGBA(249.0F, 115.0F, 22.0F, 255.0F);
            case GOLD    -> new ColorRGBA(251.0F, 191.0F, 36.0F, 255.0F);
            case CUSTOM  -> getInstance().getCustomColor();
        };
    }

    public static ColorRGBA getRainbowColor(float offsetSeconds, float saturation, float brightness) {
        float hue = (((System.currentTimeMillis() % 4000L) / 4000.0F) + offsetSeconds) % 1.0F;
        if (hue < 0.0F) hue += 1.0F;
        return ColorRGBA.fromHSB(hue, saturation, brightness);
    }

    public void apply() {
        if (enabled && syncGui) {
            ColorRGBA c = getActiveColor();
            Colors.ACCENT = c;
            ClientAppearance.setAccent(c);
        } else {
            Colors.ACCENT = ColorRGBA.WHITE;
            ClientAppearance.setAccent(ColorRGBA.WHITE);
        }
    }

    public void updatePerFrame() {
        if (enabled && syncGui && mode == PaletteMode.RAINBOW) {
            ColorRGBA c = getActiveColor();
            Colors.ACCENT = c;
            ClientAppearance.setAccent(c);
        }
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", enabled);
        json.addProperty("mode", mode.name());
        json.addProperty("customColor", customColor != null ? customColor.toHex() : "#FFFFFFFF");
        json.addProperty("baseAccent", baseAccent != null ? baseAccent.toHex() : "#FFFFFFFF");
        json.addProperty("syncGui", syncGui);
        json.addProperty("syncModules", syncModules);
        return json;
    }

    public void fromJson(JsonObject json) {
        if (json == null) return;
        if (json.has("enabled")) enabled = json.get("enabled").getAsBoolean();
        if (json.has("mode")) {
            try {
                mode = PaletteMode.valueOf(json.get("mode").getAsString());
            } catch (Exception ignored) {
                mode = PaletteMode.WHITE;
            }
        }
        if (json.has("customColor")) {
            customColor = ColorRGBA.fromHex(json.get("customColor").getAsString());
        }
        if (json.has("baseAccent")) {
            baseAccent = ColorRGBA.fromHex(json.get("baseAccent").getAsString());
        }
        if (json.has("syncGui")) syncGui = json.get("syncGui").getAsBoolean();
        if (json.has("syncModules")) syncModules = json.get("syncModules").getAsBoolean();
        apply();
    }
}
