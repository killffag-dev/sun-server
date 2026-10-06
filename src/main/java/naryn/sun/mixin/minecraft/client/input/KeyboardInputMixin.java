package naryn.sun.mixin.minecraft.client.input;

import naryn.sun.Sun;
import naryn.sun.systems.event.impl.player.InputEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
   @Inject(method = "tick", at = @At("TAIL"))
   private void onTick(CallbackInfo ci) {
      Input input = (Input) (Object) this;
      InputAccessor accessor = (InputAccessor) input;
      PlayerInput keys = accessor.getInput();
      float movementForward = accessor.getMovementForward();
      float movementSideways = accessor.getMovementSideways();
      boolean jumping = accessor.getInput().jump();
      boolean sneaking = accessor.getInput().sneak();
      boolean sprint = accessor.getInput().sprint();
      InputEvent event = InputEvent.INSTANCE.set(movementForward, movementSideways, jumping, sneaking, sprint);
      Sun.getInstance().getEventManager().triggerEvent(event);
      accessor.setMovementForward(event.getForward());
      accessor.setMovementSideways(event.getStrafe());
      boolean forwardKey = event.getForward() > 0.0F;
      boolean backwardKey = event.getForward() < 0.0F;
      boolean leftKey = event.getStrafe() > 0.0F;
      boolean rightKey = event.getStrafe() < 0.0F;
      boolean jmp = event.isJump();
      boolean snk = event.isSneak();
      boolean spr = event.isSprint();

      if (forwardKey != keys.forward() || backwardKey != keys.backward()
            || leftKey != keys.left() || rightKey != keys.right()
            || jmp != keys.jump() || snk != keys.sneak() || spr != keys.sprint()) {
         accessor.setInput(new PlayerInput(forwardKey, backwardKey, leftKey, rightKey, jmp, snk, spr));
      }
   }
}
