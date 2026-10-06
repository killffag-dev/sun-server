package naryn.sun.framework.base;

import naryn.sun.Sun;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.MsdfRenderer;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.gradient.Gradient;
import naryn.sun.mixin.accessors.DrawContextAccessor;
import naryn.sun.systems.modules.modules.visuals.Interface;
import naryn.sun.systems.theme.ClientAppearance;
import naryn.sun.systems.theme.Theme;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.render.DrawUtility;
import naryn.sun.utility.render.obj.CustomSprite;
import naryn.sun.utility.render.obj.Rect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CustomDrawContext extends DrawContext implements IMinecraft {
   private final DrawContext originalContext;

   protected CustomDrawContext(DrawContext originalContext) {
      super(MinecraftClient.getInstance(), ((DrawContextAccessor)originalContext).getVertexConsumers());
      this.originalContext = originalContext;
   }

   public static CustomDrawContext of(DrawContext originalContext) {
      return new CustomDrawContext(originalContext);
   }

   public void drawClientRect(float x, float y, float width, float height, float alpha, float dragAnim, float squircle) {
      BorderRadius radius = BorderRadius.all(6.0F);
      if (ClientAppearance.isHudFrost()) {
         this.drawPerimeterShadow(x, y + 1.0F, width, height, 10.0F, radius, ColorRGBA.BLACK.withAlpha((int) (110 * alpha)));
         this.drawBlurredRect(x, y, width, height, 28.0F, radius, ColorRGBA.WHITE.withAlpha((int) (255 * alpha)));
         this.drawRoundedRect(x, y, width, height, radius, new ColorRGBA(10, 12, 16, (int) (30 * alpha)));
         this.drawRoundedBorder(x, y, width, height, 0.5F, radius, new ColorRGBA(255, 255, 255, (int) (15 * alpha)));
      } else {
         this.drawPerimeterShadow(x, y + 1.0F, width, height, 8.0F, radius, ColorRGBA.BLACK.withAlpha((int) (120 * alpha)));
         this.drawRoundedRect(x, y, width, height, radius, new ColorRGBA(23, 24, 32, (int) (230 * alpha)));
         this.drawRoundedBorder(x, y, width, height, 0.5F, radius, new ColorRGBA(255, 255, 255, (int) (10 * alpha)));
      }
   }

   public void pushMatrix() {
      this.getMatrices().push();
   }

   public void popMatrix() {
      this.getMatrices().pop();
   }

   // Принудительно флашит буфер предметов (Immediate vertexConsumers),
   // чтобы гарантировать порядок отрисовки: предмет должен быть закоммичен
   // на экран ДО того, как поверх него рисуется что-то ещё (текст, оверлеи).
   // Без этого ItemRenderState.render() уходит в буфер и флашится позже,
   // перекрывая уже отрисованные "immediate" элементы (текст, рект и т.д.),
   // из-за чего они визуально оказываются "под" предметом.
   public void flushItems() {
      ((DrawContextAccessor) this.originalContext).getVertexConsumers().draw();
   }

   public void drawRect(float x, float y, float width, float height, ColorRGBA color) {
      DrawUtility.drawRect(this.getMatrices(), x, y, width, height, color);
   }

   public void drawLine(Vec2f from, Vec2f to, ColorRGBA color) {
      DrawUtility.drawLine(this.getMatrices(), from, to, color);
   }

   public void drawBezier(Vec2f p0, Vec2f p1, Vec2f p2, Vec2f p3, ColorRGBA color, int resolution) {
      DrawUtility.drawBezier(this.getMatrices(), p0, p1, p2, p3, color, resolution);
   }

   public void drawSquircle(float x, float y, float width, float height, float squirt, BorderRadius borderRadius, ColorRGBA color) {
      DrawUtility.drawSquircle(this.getMatrices(), x, y, width, height, squirt, borderRadius, color);
   }

   public void drawRoundedRect(float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
      DrawUtility.drawRoundedRect(this.getMatrices(), x, y, width, height, borderRadius, color);
   }

   public void drawRoundedRect(float x, float y, float width, float height, BorderRadius borderRadius, Gradient gradient) {
      DrawUtility.drawRoundedRect(this.getMatrices(), x, y, width, height, borderRadius, gradient);
   }

   public void drawLiquidGlass(float x, float y, float width, float height, float squirt, float power, BorderRadius borderRadius, ColorRGBA color) {
      borderRadius = new BorderRadius(
         borderRadius.topLeftRadius() * squirt / 2.0F,
         borderRadius.topRightRadius() * squirt / 2.0F,
         borderRadius.bottomLeftRadius() * squirt / 2.0F,
         borderRadius.bottomRightRadius() * squirt / 2.0F
      );
      DrawUtility.drawLiquidGlass(
         this.getMatrices(),
         x - 5.0F * Interface.minimalizm(),
         y - 5.0F * Interface.minimalizm(),
         width + 10.0F * Interface.minimalizm(),
         height + 10.0F * Interface.minimalizm(),
         borderRadius,
         color,
         color.getAlpha() / 255.0F * Interface.glass(),
         (height == 240.0F ? 100 : 50) * Interface.glass(),
         color.withAlpha(255.0F),
         1.0F,
         true,
         0.0F,
         power * Interface.glass(),
         squirt,
         false
      );
   }

   public void drawLiquidGlass(float x, float y, float width, float height, float squirt, BorderRadius borderRadius, ColorRGBA color, boolean clean) {
      borderRadius = new BorderRadius(
         borderRadius.topLeftRadius() * squirt / 2.0F,
         borderRadius.topRightRadius() * squirt / 2.0F,
         borderRadius.bottomLeftRadius() * squirt / 2.0F,
         borderRadius.bottomRightRadius() * squirt / 2.0F
      );
      DrawUtility.drawLiquidGlass(
         this.getMatrices(),
         x,
         y,
         width,
         height,
         borderRadius,
         color,
         color.getAlpha() / 255.0F,
         height == 240.0F ? 100.0F : 50.0F,
         color.withAlpha(255.0F),
         1.0F,
         true,
         0.0F,
         0.08F,
         squirt,
         clean
      );
   }

   public void drawLoadingRect(float x, float y, float width, float height, float progress, BorderRadius borderRadius, ColorRGBA color) {
      DrawUtility.drawLoadingRect(this.getMatrices(), x, y, width, height, progress, borderRadius, color);
   }

   public void drawRoundedBorder(float x, float y, float width, float height, float borderThickness, BorderRadius borderRadius, ColorRGBA borderColor) {
      DrawUtility.drawRoundedBorder(this.getMatrices(), x, y, width, height, borderThickness, borderRadius, borderColor);
   }

   public void drawGradientRoundedBorder(float x, float y, float width, float height, float borderThickness, BorderRadius borderRadius,
                                       ColorRGBA topLeftColor, ColorRGBA bottomLeftColor, ColorRGBA bottomRightColor, ColorRGBA topRightColor) {
      DrawUtility.drawRoundedBorder(this.getMatrices(), x, y, width, height, borderThickness, borderRadius,
           topLeftColor, bottomLeftColor, bottomRightColor, topRightColor);
   }	

   public void drawStar(float centerX, float centerY, float outerRadius, float innerRadius, ColorRGBA color) {
      DrawUtility.drawStar(this.getMatrices(), centerX, centerY, outerRadius, innerRadius, color);
   }

   public void drawStarOutline(float centerX, float centerY, float outerRadius, float innerRadius, ColorRGBA color) {
      DrawUtility.drawStarOutline(this.getMatrices(), centerX, centerY, outerRadius, innerRadius, color);
   }

   public void drawTexture(Identifier identifier, Rect rect) {
      this.drawTexture(identifier, rect, ColorRGBA.WHITE);
   }

   public void drawTexture(Identifier identifier, Rect rect, ColorRGBA color) {
      DrawUtility.drawTexture(this.getMatrices(), identifier, rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), color);
   }

   public void drawTexture(Identifier identifier, float x, float y, float width, float height) {
      DrawUtility.drawTexture(this.getMatrices(), identifier, x, y, width, height, ColorRGBA.WHITE);
   }

   public void drawTexture(Identifier identifier, float x, float y, float width, float height, float u1, float u2, float v1, float v2, ColorRGBA color) {
      DrawUtility.drawTexture(this.getMatrices(), identifier, x, y, width, height, u1, u2, v1, v2, color);
   }

   public void drawTexture(Identifier identifier, float x, float y, float width, float height, ColorRGBA textureColor) {
      DrawUtility.drawTexture(this.getMatrices(), identifier, x, y, width, height, textureColor);
   }

   public void drawSprite(CustomSprite sprite, float x, float y, float width, float height, ColorRGBA textureColor) {
      DrawUtility.drawSprite(this.getMatrices(), sprite, x, y, width, height, textureColor);
   }

   public void drawRoundedTexture(Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius) {
      DrawUtility.drawRoundedTexture(this.getMatrices(), identifier, x, y, width, height, borderRadius);
   }

   public void drawRoundedTexture(Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
      DrawUtility.drawRoundedTexture(this.getMatrices(), identifier, x, y, width, height, borderRadius, color);
   }

   public void drawShadow(float x, float y, float width, float height, float softness, BorderRadius borderRadius, ColorRGBA color) {
      DrawUtility.drawShadow(this.getMatrices(), x, y, width, height, softness, borderRadius, color);
   }

   public void drawPerimeterShadow(float x, float y, float width, float height, float softness, BorderRadius borderRadius, ColorRGBA color) {
      DrawUtility.drawPerimeterShadow(this.getMatrices(), x, y, width, height, softness, borderRadius, color);
   }

   public void drawBlurredRect(float x, float y, float width, float height, float blurRadius, BorderRadius borderRadius, ColorRGBA color) {
      DrawUtility.drawBlur(this.getMatrices(), x, y, width, height, blurRadius, borderRadius, color);
   }

   public void drawBlurredRect(float x, float y, float width, float height, float blurRadius, float squirt, BorderRadius borderRadius, ColorRGBA color) {
      DrawUtility.drawBlur(this.getMatrices(), x, y, width, height, blurRadius, squirt, borderRadius, color);
   }

   // Лёгкое зерно (grain) поверх стеклянной панели — см. FacetFrostSkin.renderPanelShell.
   // alpha обычно 0.03-0.05: убирает эффект "ровной пластиковой заливки" у glassmorphism.
   public void drawNoiseOverlay(float x, float y, float width, float height, BorderRadius borderRadius, float alpha) {
      DrawUtility.drawNoiseOverlay(this.getMatrices(), x, y, width, height, borderRadius, ColorRGBA.WHITE.withAlpha(255.0F * alpha));
   }

   public void drawText(Font font, String text, float x, float y, ColorRGBA color) {
      MsdfRenderer.renderText(font.getFont(), text, font.getSize(), color.getRGB(), this.getMatrices().peek().getPositionMatrix(), x, y, 0.0F);
   }

   public void drawText(Font font, Text text, float x, float y) {
      MsdfRenderer.renderText(font.getFont(), text, font.getSize(), this.getMatrices().peek().getPositionMatrix(), x, y, 0.0F);
   }

   public void drawFadeoutText(Font font, String text, float x, float y, ColorRGBA color, float fadeoutStart, float fadeoutEnd) {
      MsdfRenderer.renderText(
         font.getFont(), text, font.getSize(), color.getRGB(), this.getMatrices().peek().getPositionMatrix(), x, y, 0.0F, true, fadeoutStart, fadeoutEnd
      );
   }

   public void drawFadeoutText(Font font, String text, float x, float y, ColorRGBA color, float fadeoutStart, float fadeoutEnd, float maxWidth) {
      MsdfRenderer.renderText(
         font.getFont(),
         text,
         font.getSize(),
         color.getRGB(),
         this.getMatrices().peek().getPositionMatrix(),
         x,
         y,
         0.0F,
         true,
         fadeoutStart,
         fadeoutEnd,
         maxWidth
      );
   }

   public void drawCenteredText(Font font, String text, float x, float y, ColorRGBA color) {
      this.drawText(font, text, x - font.getFont().getWidth(text, font.getSize()) / 2.0F, y, color);
   }

   public void drawRightText(Font font, String text, float x, float y, ColorRGBA color) {
      this.drawText(font, text, x - font.getFont().getWidth(text, font.getSize()), y, color);
   }

   public void drawItem(Item item, float x, float y, float size) {
      this.drawItem(item.getDefaultStack(), x, y, size);
   }

   public void drawItem(ItemStack item, float x, float y, float size) {
      this.getMatrices().push();
      this.getMatrices().translate(x, y, 0.0F);
      this.getMatrices().scale(size, size, size);
      DiffuseLighting.disableGuiDepthLighting();
      this.drawItem(item, 0, 0);
      DiffuseLighting.disableGuiDepthLighting();
      this.getMatrices().pop();
   }

   public void drawHead(AbstractClientPlayerEntity player, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color) {
      DrawUtility.drawPlayerHeadWithHat(this.getMatrices(), player, x, y, size, borderRadius, color);
   }

   public void drawHead(LivingEntity entity, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color) {
      DrawUtility.drawEntityHeadWithHat(this.getMatrices(), entity, x, y, size, borderRadius, color);
   }

   public void drawBatchItem(ItemStack item, int x, int y) {
      this.drawBatchItem(mc.player, mc.world, item, x, y, 0);
   }

   private void drawBatchItem(@Nullable LivingEntity entity, @Nullable World world, ItemStack stack, int x, int y, int seed) {
      this.drawBatchItem(entity, world, stack, x, y, seed, 0);
   }

   private void drawBatchItem(@Nullable LivingEntity entity, @Nullable World world, ItemStack stack, int x, int y, int seed, int z) {
      MatrixStack matrices = this.getMatrices();
      ItemRenderState itemRenderState = ((DrawContextAccessor)this.originalContext).getItemRenderState();
      Immediate vertexConsumers = ((DrawContextAccessor)this.originalContext).getVertexConsumers();
      if (!stack.isEmpty()) {
         itemRenderState.clear();
         mc.getItemModelManager().update(itemRenderState, stack, ModelTransformationMode.GUI, false, world, entity, seed);
         matrices.push();
         matrices.translate(x + 8, y + 8, 150 + (itemRenderState.hasDepth() ? z : 0));

         try {
            matrices.scale(16.0F, -16.0F, 16.0F);
            boolean bl = !itemRenderState.isSideLit();
            if (bl) {
               DiffuseLighting.disableGuiDepthLighting();
            }

            itemRenderState.render(matrices, vertexConsumers, 15728880, OverlayTexture.DEFAULT_UV);
            if (bl) {
               DiffuseLighting.enableGuiDepthLighting();
            }
         } catch (Throwable var15) {
            CrashReport crashReport = CrashReport.create(var15, "Rendering item");
            CrashReportSection crashReportSection = crashReport.addElement("Item being rendered");
            crashReportSection.add("Item Type", () -> String.valueOf(stack.getItem()));
            crashReportSection.add("Item Components", () -> String.valueOf(stack.getComponents()));
            crashReportSection.add("Item Foil", () -> String.valueOf(stack.hasGlint()));
            throw new CrashException(crashReport);
         }

         matrices.pop();
      }
   }
}