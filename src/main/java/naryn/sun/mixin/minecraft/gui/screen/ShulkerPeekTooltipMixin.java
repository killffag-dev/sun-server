package naryn.sun.mixin.minecraft.client.gui.screen;

import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.visuals.ShulkerPeek;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// ПРОВЕРЬ В IDE: имя метода "drawMouseoverTooltip" и его сигнатура (context, x, y) —
// это тот метод HandledScreen, что рисует имя/лор предмета под курсором.
// Если компилятор не найдёт метод — глянь в HandledScreen через Ctrl+клик,
// как называется метод, вызываемый в конце render() для рисования тултипа.
@Mixin(HandledScreen.class)
public abstract class ShulkerPeekTooltipMixin {

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void onDrawMouseoverTooltip(DrawContext context, int x, int y, CallbackInfo ci) {
        ShulkerPeek module = Sun.getInstance().getModuleManager().getModule(ShulkerPeek.class);
        if (module != null && module.isHoveringContainer()) {
            ci.cancel();
        }
    }
}
