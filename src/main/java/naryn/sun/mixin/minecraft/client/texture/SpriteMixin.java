package naryn.sun.mixin.minecraft.client.texture;

import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.optimization.Optimizer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Sprite.class)
public abstract class SpriteMixin {
   @Shadow
   @Final
   private SpriteContents contents;

   @Inject(method = "createAnimation", at = @At("RETURN"), cancellable = true)
   private void sun$wrapAnimation(CallbackInfoReturnable<Sprite.TickableAnimation> cir) {
      Sprite.TickableAnimation original = cir.getReturnValue();
      if (original != null) {
         String path = this.contents.getId().getPath();
         if (path.contains("water") || path.contains("lava")) {
            cir.setReturnValue(new Sprite.TickableAnimation() {
               @Override
               public void tick() {
                  Optimizer optimizer = Sun.getInstance().getModuleManager().getModule(Optimizer.class);
                  if (optimizer != null && optimizer.isEnabled() && optimizer.getStaticFluids().isEnabled()) {
                     return;
                  }
                  original.tick();
               }

               @Override
               public void close() {
                  original.close();
               }
            });
         }
      }
   }
}
