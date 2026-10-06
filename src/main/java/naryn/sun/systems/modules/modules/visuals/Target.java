package naryn.sun.systems.modules.modules.visuals;

import naryn.sun.Sun;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.window.MouseEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.TargetListSetting;
import naryn.sun.systems.target.TargetManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(name = "Target", category = ModuleCategory.VISUALS, desc = "modules.descriptions.target")
public class Target extends BaseModule {

    private final BooleanSetting targetListToggle = new BooleanSetting(this, "modules.settings.target.list_toggle").enabled(false);
    private final TargetListSetting targetList = new TargetListSetting(this, "modules.settings.target.list", () -> !this.targetListToggle.isEnabled());

    private final EventListener<MouseEvent> onMouseEvent = event -> {
        if (mc.currentScreen == null && event.getAction() == GLFW.GLFW_PRESS && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            boolean shiftPressed = Screen.hasShiftDown()
                || InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT);

            if (!shiftPressed) {
                PlayerEntity targetEntity = null;
                if (mc.targetedEntity instanceof PlayerEntity player) {
                    targetEntity = player;
                } else if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof PlayerEntity player) {
                    targetEntity = player;
                }

                if (targetEntity != null) {
                    String name = targetEntity.getName().getString();
                    TargetManager targetManager = Sun.getInstance().getTargetManager();
                    if (targetManager.isTarget(name)) {
                        targetManager.removeTarget(name);
                    } else {
                        targetManager.addTarget(name);
                    }
                }
            }
        }
    };

    public boolean isOutline() {
        return true;
    }

    public static boolean shouldShowOutline(PlayerEntity player) {
        if (!player.isInvisible()) {
            return true;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!player.getEquippedStack(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
