package naryn.sun.mixin.minecraft.client.gui.overlay;

import java.util.HashMap;
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
   private static final String FILTERED_TEXT = "둅ꈣꈃ둄ꈣꈅ";
   private final Map<UUID, String> lastProcessedNames = new HashMap<>();

   @Inject(method = "render", at = @At("HEAD"))
   private void onRenderHead(DrawContext context, CallbackInfo ci) {
      int ctTimer = 0;
      Map<UUID, ClientBossBar> bossBars = ((BossBarHudAccessor) (Object) this).getBossBars();

      for (ClientBossBar bossBar : bossBars.values()) {
         if (bossBar.getName() != null) {
            String name = bossBar.getName().getString().toLowerCase();
            if (name.contains("бой") || name.contains("pvp")) {
               Matcher matcher = PVP_TIME_PATTERN.matcher(bossBar.getName().getString());
               if (matcher.find()) {
                  ctTimer = Integer.parseInt(matcher.group(1));
               }
               break;
            }
         }
      }

      ServerUtility.setHasCT(ctTimer > 0);
      ServerUtility.setCtTime(ctTimer);
   }

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void render(CallbackInfo ci) {
      AntiOverlay antiOverlay = Sun.getInstance().getModuleManager().getModule(AntiOverlay.class);
      if (antiOverlay != null && antiOverlay.isEnabled() && antiOverlay.getBossBar().isSelected()) {
         ci.cancel();
      }
   }
}
