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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(InGameHud.class)
public class InGameHudMixin implements IMinecraft {
   @Unique
   private CustomDrawContext cachedHudContext;
   @Unique private static AntiOverlay sun$antiOverlay;
   @Unique private static NoRender sun$noRender;
   @Unique private static naryn.sun.systems.modules.modules.utility.minigames.MiniGames sun$miniGames;
   @Unique private static naryn.sun.systems.modules.modules.visuals.Crosshair sun$crosshair;
   @Unique private static PotionStatus sun$potionStatus;
   @Unique private static String sun$cachedScoreboardTitle;
   @Unique private static int sun$cachedFtAn = -1;

   @Unique
   private static AntiOverlay sun$getAntiOverlay() {
      if (sun$antiOverlay == null && Sun.getInstance() != null && Sun.getInstance().getModuleManager() != null) {
         sun$antiOverlay = Sun.getInstance().getModuleManager().getModule(AntiOverlay.class);
      }
      return sun$antiOverlay;
   }

   @Unique
   private static NoRender sun$getNoRender() {
      if (sun$noRender == null && Sun.getInstance() != null && Sun.getInstance().getModuleManager() != null) {
         sun$noRender = Sun.getInstance().getModuleManager().getModule(NoRender.class);
      }
      return sun$noRender;
   }

   @Unique
   private static naryn.sun.systems.modules.modules.utility.minigames.MiniGames sun$getMiniGames() {
      if (sun$miniGames == null && Sun.getInstance() != null && Sun.getInstance().getModuleManager() != null) {
         sun$miniGames = Sun.getInstance().getModuleManager().getModule(naryn.sun.systems.modules.modules.utility.minigames.MiniGames.class);
      }
      return sun$miniGames;
   }

   @Unique
   private static naryn.sun.systems.modules.modules.visuals.Crosshair sun$getCrosshair() {
      if (sun$crosshair == null && Sun.getInstance() != null && Sun.getInstance().getModuleManager() != null) {
         sun$crosshair = Sun.getInstance().getModuleManager().getModule(naryn.sun.systems.modules.modules.visuals.Crosshair.class);
      }
      return sun$crosshair;
   }

   @Unique
   private static PotionStatus sun$getPotionStatus() {
      if (sun$potionStatus == null && Sun.getInstance() != null && Sun.getInstance().getModuleManager() != null) {
         sun$potionStatus = Sun.getInstance().getModuleManager().getModule(PotionStatus.class);
      }
      return sun$potionStatus;
   }

   @Inject(
      method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void renderScoreboardSidebarHook(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
      if (objective != null && (ServerUtility.isFT() || ServerUtility.isST())) {
         String title = objective.getDisplayName().getString();
         if (title.equals(sun$cachedScoreboardTitle)) {
            if (sun$cachedFtAn != -1) {
               ServerUtility.ftAn = sun$cachedFtAn;
            }
         } else {
            sun$cachedScoreboardTitle = title;
            if (title.contains("Анархия")) {
               try {
                  String[] parts = title.split("-");
                  if (parts.length > 1) {
                     sun$cachedFtAn = Integer.parseInt(parts[1].trim());
                     ServerUtility.ftAn = sun$cachedFtAn;
                  }
               } catch (Exception ignored) {
               }
            } else {
               sun$cachedFtAn = -1;
            }
         }
      }

      AntiOverlay antiOverlay = sun$getAntiOverlay();
      if (antiOverlay != null && antiOverlay.isEnabled() && antiOverlay.getScoreboard().isSelected()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
   private void renderPortalOverlayHook(DrawContext context, float nauseaStrength, CallbackInfo ci) {
      AntiOverlay antiOverlay = sun$getAntiOverlay();
      if (antiOverlay != null && antiOverlay.isEnabled() && antiOverlay.getPortal().isSelected()) {
         ci.cancel();
         return;
      }
      NoRender noRender = sun$getNoRender();
      if (noRender != null && noRender.isEnabled() && noRender.getPortal().isSelected()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
   private void renderVignetteOverlayHook(DrawContext context, Entity entity, CallbackInfo ci) {
      AntiOverlay antiOverlay = sun$getAntiOverlay();
      if (antiOverlay != null && antiOverlay.isEnabled() && antiOverlay.getVignette().isSelected()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true)
   private void renderSpyglassOverlayHook(DrawContext context, float scale, CallbackInfo ci) {
      AntiOverlay antiOverlay = sun$getAntiOverlay();
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
      AntiOverlay antiOverlay = sun$getAntiOverlay();
      if (antiOverlay != null && antiOverlay.isEnabled() && antiOverlay.getPumpkin().isSelected()) {
         args.set(2, 0.0F);
         return;
      }
      NoRender noRender = sun$getNoRender();
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
      AntiOverlay antiOverlay = sun$getAntiOverlay();
      if (antiOverlay != null && antiOverlay.isEnabled() && antiOverlay.getFreezing().isSelected()) {
         args.set(2, 0.0F);
      }
   }

   @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
   private void onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      naryn.sun.systems.modules.modules.utility.minigames.MiniGames miniGames = sun$getMiniGames();
      if (miniGames != null && miniGames.isAimingAtBoard()) {
         ci.cancel();
         return;
      }
      naryn.sun.systems.modules.modules.visuals.Crosshair crosshair = sun$getCrosshair();
      if (crosshair != null && crosshair.isEnabled()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
   private void renderStatusEffectOverlayHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      PotionStatus potionStatus = sun$getPotionStatus();
      if (potionStatus != null && potionStatus.isEnabled()) {
         ci.cancel();
      }
   }

   @Inject(method = "render", at = @At("HEAD"))
   public void triggerPreHudRenderEvent(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      this.cachedHudContext = CustomDrawContext.of(context);
      Sun.getInstance().getEventManager().triggerEvent(PreHudRenderEvent.INSTANCE.set(this.cachedHudContext, tickCounter.getTickDelta(false)));
   }

   @Inject(method = "render", at = @At("RETURN"))
   public void triggerPostHudRenderEvent(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      CustomDrawContext ctx = this.cachedHudContext != null ? this.cachedHudContext : CustomDrawContext.of(context);
      Sun.getInstance().getEventManager().triggerEvent(PostHudRenderEvent.INSTANCE.set(ctx, tickCounter.getTickDelta(false)));
      this.cachedHudContext = null;
   }

   @Inject(method = "renderMainHud", at = @At("TAIL"))
   private void triggerHudRenderEvent(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      CustomDrawContext ctx = this.cachedHudContext != null ? this.cachedHudContext : CustomDrawContext.of(context);
      DrawUtility.blurProgram.draw();
      Sun.getInstance().getEventManager().triggerEvent(HudRenderEvent.INSTANCE.set(ctx, tickCounter.getTickDelta(false)));
   }
}