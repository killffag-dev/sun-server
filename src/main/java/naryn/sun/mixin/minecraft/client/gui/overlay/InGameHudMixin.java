package naryn.sun.mixin.minecraft.client.gui.overlay;

import naryn.sun.Sun;
import naryn.sun.framework.base.CustomDrawContext;
import naryn.sun.systems.event.impl.render.HudRenderEvent;
import naryn.sun.systems.event.impl.render.PostHudRenderEvent;
import naryn.sun.systems.event.impl.render.PreHudRenderEvent;
import naryn.sun.systems.modules.modules.optimization.NoRender;
import naryn.sun.systems.modules.modules.utility.AntiOverlay;
import naryn.sun.systems.modules.modules.visuals.PotionStatus;
import net.minecraft.entity.Entity;
import naryn.sun.utility.game.server.ServerUtility;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.render.DrawUtility;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(InGameHud.class)
public class InGameHudMixin implements IMinecraft {
   @Inject(
      method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void renderScoreboardSidebarHook(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
      if (objective.getDisplayName().getString().contains("Анархия") && (ServerUtility.isFT() || ServerUtility.isST())) {
         try {
            ServerUtility.ftAn = Integer.parseInt(objective.getDisplayName().getString().split("-")[1].trim());
         } catch (Exception var5) {
         }
      }

      AntiOverlay antiOverlay = Sun.getInstance().getModuleManager().getModule(AntiOverlay.class);
      if (antiOverlay != null && antiOverlay.isEnabled() && antiOverlay.getScoreboard().isSelected()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
   private void renderPortalOverlayHook(DrawContext context, float nauseaStrength, CallbackInfo ci) {
      AntiOverlay antiOverlay = Sun.getInstance().getModuleManager().getModule(AntiOverlay.class);
      if (antiOverlay != null && antiOverlay.isEnabled() && antiOverlay.getPortal().isSelected()) {
         ci.cancel();
         return;
      }
      NoRender noRender = Sun.getInstance().getModuleManager().getModule(NoRender.class);
      if (noRender != null && noRender.isEnabled() && noRender.getPortal().isSelected()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
   private void renderVignetteOverlayHook(DrawContext context, Entity entity, CallbackInfo ci) {
      AntiOverlay antiOverlay = Sun.getInstance().getModuleManager().getModule(AntiOverlay.class);
      if (antiOverlay != null && antiOverlay.isEnabled() && antiOverlay.getVignette().isSelected()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true)
   private void renderSpyglassOverlayHook(DrawContext context, float scale, CallbackInfo ci) {
      AntiOverlay antiOverlay = Sun.getInstance().getModuleManager().getModule(AntiOverlay.class);
      if (antiOverlay != null && antiOverlay.isEnabled() && antiOverlay.getSpyglass().isSelected()) {
         ci.cancel();
      }
   }

   @ModifyArgs(
      method = "renderMiscOverlays",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/hud/InGameHud;renderOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/util/Identifier;F)V",
         ordinal = 0
      )
   )
   private void onRenderPumpkinOverlay(Args args) {
      AntiOverlay antiOverlay = Sun.getInstance().getModuleManager().getModule(AntiOverlay.class);
      if (antiOverlay != null && antiOverlay.isEnabled() && antiOverlay.getPumpkin().isSelected()) {
         args.set(2, 0.0F);
         return;
      }
      NoRender noRender = Sun.getInstance().getModuleManager().getModule(NoRender.class);
      if (noRender != null && noRender.isEnabled() && noRender.getPumpkin().isSelected()) {
         args.set(2, 0.0F);
      }
   }

   @ModifyArgs(
      method = "renderMiscOverlays",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/hud/InGameHud;renderOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/util/Identifier;F)V",
         ordinal = 1
      )
   )
   private void onRenderPowderSnowOverlay(Args args) {
      AntiOverlay antiOverlay = Sun.getInstance().getModuleManager().getModule(AntiOverlay.class);
      if (antiOverlay != null && antiOverlay.isEnabled() && antiOverlay.getFreezing().isSelected()) {
         args.set(2, 0.0F);
      }
   }

   @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
   private void onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      naryn.sun.systems.modules.modules.visuals.Crosshair crosshair = Sun.getInstance().getModuleManager().getModule(naryn.sun.systems.modules.modules.visuals.Crosshair.class);
      if (crosshair != null && crosshair.isEnabled()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
   private void renderStatusEffectOverlayHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      PotionStatus potionStatus = Sun.getInstance().getModuleManager().getModule(PotionStatus.class);
      if (potionStatus.isEnabled()) {
         ci.cancel();
      }
   }

   @Inject(method = "render", at = @At("HEAD"))
   public void triggerPreHudRenderEvent(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      CustomDrawContext customDrawContext = CustomDrawContext.of(context);
      Sun.getInstance().getEventManager().triggerEvent(new PreHudRenderEvent(customDrawContext, tickCounter.getTickDelta(false)));
   }

   @Inject(method = "render", at = @At("RETURN"))
   public void triggerPostHudRenderEvent(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      CustomDrawContext customDrawContext = CustomDrawContext.of(context);
      Sun.getInstance().getEventManager().triggerEvent(new PostHudRenderEvent(customDrawContext, tickCounter.getTickDelta(false)));
   }

   @Inject(method = "renderMainHud", at = @At("TAIL"))
   private void triggerHudRenderEvent(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      CustomDrawContext customDrawContext = CustomDrawContext.of(context);
      DrawUtility.blurProgram.draw();
      Sun.getInstance().getEventManager().triggerEvent(new HudRenderEvent(customDrawContext, tickCounter.getTickDelta(false)));
   }
}