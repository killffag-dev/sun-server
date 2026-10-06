package naryn.sun.systems.animation;

import com.google.gson.JsonObject;

public class ClientAnimationConfig {
    private static final ClientAnimationConfig INSTANCE = new ClientAnimationConfig();

    private boolean animationsEnabled = true;
    private boolean buttonHoverLift = true;
    private boolean smoothTabs = true;
    private boolean smoothTooltip = true;
    private boolean starEffect = true;
    private boolean togglePhysics = true;
    private boolean cardExpansion = true;
    private float animationSpeed = 1.0F;
    private float menuScrollSpeed = 1.0F;

    private ClientAnimationConfig() {
    }

    public static ClientAnimationConfig getInstance() {
        return INSTANCE;
    }

    public boolean isAnimationsEnabled() {
        return animationsEnabled;
    }

    public void setAnimationsEnabled(boolean animationsEnabled) {
        this.animationsEnabled = animationsEnabled;
    }

    public boolean isButtonHoverLift() {
        return animationsEnabled && buttonHoverLift;
    }

    public void setButtonHoverLift(boolean buttonHoverLift) {
        this.buttonHoverLift = buttonHoverLift;
    }

    public boolean isSmoothTabs() {
        return animationsEnabled && smoothTabs;
    }

    public void setSmoothTabs(boolean smoothTabs) {
        this.smoothTabs = smoothTabs;
    }

    public boolean isSmoothTooltip() {
        return animationsEnabled && smoothTooltip;
    }

    public void setSmoothTooltip(boolean smoothTooltip) {
        this.smoothTooltip = smoothTooltip;
    }

    public boolean isStarEffect() {
        return animationsEnabled && starEffect;
    }

    public void setStarEffect(boolean starEffect) {
        this.starEffect = starEffect;
    }

    public boolean isTogglePhysics() {
        return animationsEnabled && togglePhysics;
    }

    public void setTogglePhysics(boolean togglePhysics) {
        this.togglePhysics = togglePhysics;
    }

    public boolean isCardExpansion() {
        return animationsEnabled && cardExpansion;
    }

    public void setCardExpansion(boolean cardExpansion) {
        this.cardExpansion = cardExpansion;
    }

    public float getAnimationSpeed() {
        return animationsEnabled ? Math.max(0.5F, Math.min(2.0F, animationSpeed)) : 1.0F;
    }

    public void setAnimationSpeed(float animationSpeed) {
        this.animationSpeed = Math.max(0.5F, Math.min(2.0F, animationSpeed));
    }

    public float getMenuScrollSpeed() {
        return Math.max(0.2F, Math.min(3.0F, menuScrollSpeed));
    }

    public void setMenuScrollSpeed(float menuScrollSpeed) {
        this.menuScrollSpeed = Math.max(0.2F, Math.min(3.0F, menuScrollSpeed));
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("animationsEnabled", animationsEnabled);
        json.addProperty("buttonHoverLift", buttonHoverLift);
        json.addProperty("smoothTabs", smoothTabs);
        json.addProperty("smoothTooltip", smoothTooltip);
        json.addProperty("starEffect", starEffect);
        json.addProperty("togglePhysics", togglePhysics);
        json.addProperty("cardExpansion", cardExpansion);
        json.addProperty("animationSpeed", animationSpeed);
        json.addProperty("menuScrollSpeed", menuScrollSpeed);
        return json;
    }

    public void fromJson(JsonObject json) {
        if (json == null) return;
        if (json.has("animationsEnabled")) {
            this.animationsEnabled = json.get("animationsEnabled").getAsBoolean();
        }
        if (json.has("buttonHoverLift")) {
            this.buttonHoverLift = json.get("buttonHoverLift").getAsBoolean();
        }
        if (json.has("smoothTabs")) {
            this.smoothTabs = json.get("smoothTabs").getAsBoolean();
        }
        if (json.has("smoothTooltip")) {
            this.smoothTooltip = json.get("smoothTooltip").getAsBoolean();
        }
        if (json.has("starEffect")) {
            this.starEffect = json.get("starEffect").getAsBoolean();
        }
        if (json.has("togglePhysics")) {
            this.togglePhysics = json.get("togglePhysics").getAsBoolean();
        }
        if (json.has("cardExpansion")) {
            this.cardExpansion = json.get("cardExpansion").getAsBoolean();
        }
        if (json.has("animationSpeed")) {
            this.animationSpeed = json.get("animationSpeed").getAsFloat();
        }
        if (json.has("menuScrollSpeed")) {
            this.menuScrollSpeed = json.get("menuScrollSpeed").getAsFloat();
        }
    }
}
