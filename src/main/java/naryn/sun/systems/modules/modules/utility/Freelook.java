package naryn.sun.systems.modules.modules.utility;

import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.player.ClientPlayerTickEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import net.minecraft.client.option.Perspective;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(name = "Freelook", category = ModuleCategory.UTILITY, key = GLFW.GLFW_KEY_LEFT_ALT, enabledByDefault = false, holdToActivate = true)
public class Freelook extends BaseModule {

    private Perspective previousPerspective = Perspective.FIRST_PERSON;
    private float freelookYaw;
    private float freelookPitch;

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        if (!isEnabled() && mc.options.getPerspective() != previousPerspective) {
            previousPerspective = mc.options.getPerspective();
        }
        if (isEnabled() && mc.currentScreen != null) {
            this.disable();
        }
    };

    @Override
    public void onEnable() {
        if (mc.player != null) {
            this.previousPerspective = mc.options.getPerspective();
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            this.freelookYaw = mc.player.getYaw();
            this.freelookPitch = mc.player.getPitch();
        }
    }

    @Override
    public void onDisable() {
        mc.options.setPerspective(this.previousPerspective);
        this.freelookYaw = 0;
        this.freelookPitch = 0;
    }

    public void onMouseMove(double deltaX, double deltaY) {
        float sensitivity = mc.options.getMouseSensitivity().getValue().floatValue() * 0.6F + 0.2F;
        float sensitivityMultiplier = sensitivity * sensitivity * sensitivity * 8.0F;
        
        this.freelookYaw += deltaX * sensitivityMultiplier * 0.15D;
        this.freelookPitch += deltaY * sensitivityMultiplier * 0.15D;
        
        this.freelookPitch = Math.max(-90.0F, Math.min(90.0F, this.freelookPitch));
    }

    public float getFreelookYaw() {
        return freelookYaw;
    }

    public float getFreelookPitch() {
        return freelookPitch;
    }
}