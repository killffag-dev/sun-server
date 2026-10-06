package naryn.sun.mixin.minecraft.client.gui.overlay;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import naryn.sun.Sun;
import naryn.sun.mixin.accessors.BossBarHudAccessor;
import naryn.sun.systems.modules.modules.utility.AntiOverlay;
import naryn.sun.utility.game.server.ServerUtility;
import naryn.sun.utility.interfaces.IMinecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossBarHud.class)
public class BossBarHudMixin implements IMinecraft {
   // Приватное поле BossBarHud.bossBars вынесено в BossBarHudAccessor
   // (Version Adapter, Этап 3) — единая точка правки при смене маппингов.
   @Unique
   private static final Pattern PVP_TIME_PATTERN = Pattern.compile("(\\d+)\\s*[сc][еe][кk](?=$|\\s|\\p{Punct})", 66);
   @Unique private static String sun$lastBossBarText;
   @Unique private static int sun$lastParsedCtTimer;
   @Unique private static AntiOverlay sun$antiOverlay;

   @Unique
   private static AntiOverlay sun$getAntiOverlay() {
      if (sun$antiOverlay == null && Sun.getInstance() != null && Sun.getInstance().getModuleManager() != null) {
         sun$antiOverlay = Sun.getInstance().getModuleManager().getModule(AntiOverlay.class);
      }
      return sun$antiOverlay;
   }

   @Inject(method = "render", at = @At("HEAD"))
   private void onRenderHead(DrawContext context, CallbackInfo ci) {
      int ctTimer = 0;
      Map<UUID, ClientBossBar> bossBars = ((BossBarHudAccessor) (Object) this).getBossBars();

      for (ClientBossBar bossBar : bossBars.values()) {
         if (bossBar.getName() != null) {
            String rawName = bossBar.getName().getString();
            if (rawName.equals(sun$lastBossBarText)) {
               ctTimer = sun$lastParsedCtTimer;
               break;
            }
            sun$lastBossBarText = rawName;
            String name = rawName.toLowerCase();
            if (name.contains("бой") || name.contains("pvp")) {
               Matcher matcher = PVP_TIME_PATTERN.matcher(rawName);
               if (matcher.find()) {
                  ctTimer = Integer.parseInt(matcher.group(1));
               }
               sun$lastParsedCtTimer = ctTimer;
               break;
            } else {
               sun$lastParsedCtTimer = 0;
            }
         }
      }

      ServerUtility.setHasCT(ctTimer > 0);
      ServerUtility.setCtTime(ctTimer);
   }

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void render(CallbackInfo ci) {
      AntiOverlay antiOverlay = sun$getAntiOverlay();
      if (antiOverlay != null && antiOverlay.isEnabled() && antiOverlay.getBossBar().isSelected()) {
         ci.cancel();
      }
   }
}
