package naryn.sun.ui.menu.background;

import com.google.gson.JsonObject;

public class MenuBackgroundConfig {
    private static final MenuBackgroundConfig INSTANCE = new MenuBackgroundConfig();

    private MenuBackgroundMode mode = MenuBackgroundMode.CYBER_GRID;
    private float speed = 1.0F;
    private float density = 1.0F;
    private boolean shockwaves = true;
    private boolean mouseInteraction = true;
    private boolean syncAccent = true;

    private MenuBackgroundConfig() {
    }

    public static MenuBackgroundConfig getInstance() {
        return INSTANCE;
    }

    public MenuBackgroundMode getMode() {
        return mode;
    }

    public void setMode(MenuBackgroundMode mode) {
        this.mode = mode == null ? MenuBackgroundMode.OFF : mode;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = Math.max(0.2F, Math.min(2.5F, speed));
    }

    public float getDensity() {
        return density;
    }

    public void setDensity(float density) {
        this.density = Math.max(0.2F, Math.min(2.0F, density));
    }

    public boolean isShockwaves() {
        return shockwaves;
    }

    public void setShockwaves(boolean shockwaves) {
        this.shockwaves = shockwaves;
    }

    public boolean isMouseInteraction() {
        return mouseInteraction;
    }

    public void setMouseInteraction(boolean mouseInteraction) {
        this.mouseInteraction = mouseInteraction;
    }

    public boolean isSyncAccent() {
        return syncAccent;
    }

    public void setSyncAccent(boolean syncAccent) {
        this.syncAccent = syncAccent;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("mode", mode.name());
        json.addProperty("speed", speed);
        json.addProperty("density", density);
        json.addProperty("shockwaves", shockwaves);
        json.addProperty("mouseInteraction", mouseInteraction);
        json.addProperty("syncAccent", syncAccent);
        return json;
    }

    public void fromJson(JsonObject json) {
        if (json == null) return;
        if (json.has("mode")) {
            try {
                this.mode = MenuBackgroundMode.valueOf(json.get("mode").getAsString());
            } catch (Exception ignored) {
                this.mode = MenuBackgroundMode.CYBER_GRID;
            }
        }
        if (json.has("speed")) {
            this.speed = Math.max(0.2F, Math.min(2.5F, json.get("speed").getAsFloat()));
        }
        if (json.has("density")) {
            this.density = Math.max(0.2F, Math.min(2.0F, json.get("density").getAsFloat()));
        }
        if (json.has("shockwaves")) {
            this.shockwaves = json.get("shockwaves").getAsBoolean();
        }
        if (json.has("mouseInteraction")) {
            this.mouseInteraction = json.get("mouseInteraction").getAsBoolean();
        }
        if (json.has("syncAccent")) {
            this.syncAccent = json.get("syncAccent").getAsBoolean();
        }
    }
}
