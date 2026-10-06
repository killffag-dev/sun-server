package naryn.sun.mixin.minecraft.client.gui.screen;

import naryn.sun.Sun;
import naryn.sun.mixin.accessors.ChatScreenAccessor;
import naryn.sun.systems.modules.modules.utility.LayoutFix;
import naryn.sun.utility.interfaces.IMinecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin implements IMinecraft {

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void sun$onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (keyCode != GLFW.GLFW_KEY_ENTER && keyCode != GLFW.GLFW_KEY_KP_ENTER) {
            return;
        }

        LayoutFix module = Sun.getInstance().getModuleManager().getModule(LayoutFix.class);
        if (module == null || !module.isEnabled() || mc.player == null) {
            return;
        }

        TextFieldWidget chatField = ((ChatScreenAccessor) (Object) this).sun$getChatField();
        String fixed = LayoutFix.convert(chatField.getText());
        if (fixed == null) {
            return;
        }

        mc.player.networkHandler.sendChatCommand(fixed.substring(1));
        mc.setScreen(null);

        cir.setReturnValue(true);
        cir.cancel();
    }
}