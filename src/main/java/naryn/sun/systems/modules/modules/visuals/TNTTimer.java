package naryn.sun.systems.modules.modules.visuals;

import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.PreHudRenderEvent;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.modules.modules.visuals.nameutility.NameTagStyle;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.render.Utils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "TNT Timer", category = ModuleCategory.VISUALS, desc = "modules.descriptions.tnt_timer")
public class TNTTimer extends BaseModule {
   private static final ColorRGBA GLASS_BG = ColorRGBA.fromInt(NameTagStyle.GLASS_BG);
   private static final ColorRGBA GLASS_BORDER_TOP = ColorRGBA.fromInt(NameTagStyle.GLASS_BORDER_TOP);
   private static final ColorRGBA GLASS_BORDER = ColorRGBA.fromInt(NameTagStyle.GLASS_BORDER);

   private final EventListener<PreHudRenderEvent> onHudRenderEvent = event -> {
      if (mc.world == null || mc.player == null) return;
      MatrixStack matrices = event.getContext().getMatrices();
      Font font = Fonts.MEDIUM.getFont(11.0F);

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof TntEntity tnt) {
            this.renderTntTag(event, matrices, font, tnt);
         }
      }
   };

   private void renderTntTag(PreHudRenderEvent event, MatrixStack matrices, Font font, TntEntity entity) {
      Vec3d renderPos = entity.getLerpedPos(event.getTickDelta()).add(0.0, 0.5, 0.0);
      Vec2f screenPos = Utils.worldToScreen(renderPos);
      if (screenPos == null) return;

      int fuse = entity.getFuse();
      float seconds = fuse / 20.0F;
      String text = Localizator.translate("modules.tnt_timer.format", seconds);

      float distance = (float) mc.player.getPos().distanceTo(renderPos);
      float scale = MathHelper.clamp(1.0F - distance / 20.0F, 0.5F, 1.0F);

      matrices.push();
      matrices.translate(screenPos.x - 6.0F, screenPos.y, 0.0F);
      matrices.scale(scale, scale, 1.0F);

      int width = (int) font.width(text);
      int x = -width / 2;

      float boxX = x - 3.0F;
      float boxY = 1.0F;
      float boxWidth = width + 26.0F;
      float boxHeight = font.height() + 8.0F;

      // Отрисовка фона (стиль «Стекло» как в NameTag / NameUtility)
      event.getContext().drawRect(boxX, boxY, boxWidth, boxHeight, GLASS_BG);
      // Верхняя линия акцента
      event.getContext().drawRect(boxX, boxY, boxWidth, 0.5F, GLASS_BORDER_TOP);
      // Нижняя рамка
      event.getContext().drawRect(boxX, boxY + boxHeight - 0.5F, boxWidth, 0.5F, GLASS_BORDER);
      // Боковые рамки
      event.getContext().drawRect(boxX, boxY, 0.5F, boxHeight, GLASS_BORDER);
      event.getContext().drawRect(boxX + boxWidth - 0.5F, boxY, 0.5F, boxHeight, GLASS_BORDER);

      // Отрисовка иконки TNT и текста
      event.getContext().drawItem(Items.TNT, (float) x, 3.0F, 0.75F);
      event.getContext().drawText(font, text, x + 16, 5.0F, ColorRGBA.WHITE);

      matrices.pop();
   }
}
