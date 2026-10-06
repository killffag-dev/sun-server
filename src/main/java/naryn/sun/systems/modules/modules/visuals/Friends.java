package naryn.sun.systems.modules.modules.visuals;

import naryn.sun.Sun;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.window.MouseEvent;
import naryn.sun.systems.friends.FriendManager;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.FriendListSetting;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(name = "Friends", category = ModuleCategory.VISUALS, desc = "modules.descriptions.friends")
public class Friends extends BaseModule {
    private final BooleanSetting outline = new BooleanSetting(this, "modules.settings.friends.outline").enabled(true);
    private final BooleanSetting friendListToggle = new BooleanSetting(this, "modules.settings.friends.list_toggle").enabled(false);
    private final FriendListSetting friendList = new FriendListSetting(this, "modules.settings.friends.list", () -> !this.friendListToggle.isEnabled());

    private final EventListener<MouseEvent> onMouseEvent = event -> {
        if (mc.currentScreen == null && event.getAction() == GLFW.GLFW_PRESS && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            boolean shiftPressed = Screen.hasShiftDown()
                || InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT);

            if (shiftPressed) {
                PlayerEntity target = null;
                if (mc.targetedEntity instanceof PlayerEntity player) {
                    target = player;
                } else if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof PlayerEntity player) {
                    target = player;
                }

                if (target != null) {
                    String name = target.getName().getString();
                    FriendManager friendManager = Sun.getInstance().getFriendManager();
                    if (friendManager.isFriend(name)) {
                        friendManager.remove(name);
                    } else {
                        friendManager.add(name);
                    }
                }
            }
        }
    };

    public boolean isOutline() {
        return this.outline.isEnabled();
    }
}
