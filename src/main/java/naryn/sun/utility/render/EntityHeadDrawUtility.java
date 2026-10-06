package naryn.sun.utility.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import lombok.Generated;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.interfaces.IMinecraft;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

public final class EntityHeadDrawUtility implements IMinecraft {

   private static final Map<Class<?>, LivingEntityRenderState> RENDER_STATE_CACHE = new HashMap<>();

   public static void drawPlayerHeadWithHat(
      MatrixStack matrices, AbstractClientPlayerEntity player, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color
   ) {
      Identifier skinTexture = player.getSkinTextures().texture();
      drawPlayerHeadWithRoundedShader(matrices, skinTexture, x, y, size, borderRadius, color);
      drawPlayerHatLayerWithRoundedShader(matrices, skinTexture, x, y, size, borderRadius, color);
   }

   @SuppressWarnings("unchecked")
   public static <T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> void drawEntityHeadWithHat(
      MatrixStack matrices, T entity, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color
   ) {
      EntityRenderer<? super T, ?> renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
      if (renderer instanceof LivingEntityRenderer<?, ?, ?> livingRendererRaw) {
         LivingEntityRenderer<T, S, M> livingRenderer = (LivingEntityRenderer<T, S, M>) livingRendererRaw;
         S state = (S) RENDER_STATE_CACHE.computeIfAbsent(livingRenderer.getClass(), k -> livingRenderer.createRenderState());
         livingRenderer.updateRenderState(entity, state, 0.0F);
         Identifier skinTexture = livingRenderer.getTexture(state);
         drawPlayerHeadWithRoundedShader(matrices, skinTexture, x, y, size, borderRadius, color);
         drawPlayerHatLayerWithRoundedShader(matrices, skinTexture, x, y, size, borderRadius, color);
      }
   }

   public static void drawPlayerHeadWithRoundedShader(
      MatrixStack matrices, Identifier skinTexture, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color
   ) {
      TextureDrawUtility.drawRoundedTextureWithUV(matrices, skinTexture, x, y, size, size, borderRadius, color, 0.125F, 0.125F, 0.25F, 0.25F, true);
   }

   public static void drawPlayerHatLayerWithRoundedShader(
      MatrixStack matrices, Identifier skinTexture, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color
   ) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      TextureDrawUtility.drawRoundedTextureWithUV(matrices, skinTexture, x, y, size, size, borderRadius, color, 0.625F, 0.125F, 0.75F, 0.25F, true);
      RenderSystem.disableBlend();
   }

   public record HeadUV(float u1, float v1, float uSize, float vSize) {
   }

   @Generated
   private EntityHeadDrawUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
