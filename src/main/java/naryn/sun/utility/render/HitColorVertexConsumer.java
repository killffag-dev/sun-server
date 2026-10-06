package naryn.sun.utility.render;

import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.system.MemoryStack;

// В отличие от ванильного OutlineVertexConsumerProvider.OutlineVertexConsumer (который просто
// "роняет" overlay/light/normal — это допустимо только для урезанного формата outline-слоя),
// эта версия форвардит ВСЕ элементы вершины делегату, иначе строгие буферы (Sodium) кидают
// "Missing elements in vertex" на обычных render layer'ах. Overlay форвардим, но принудительно
// "погашенным" — чтобы ванильная бело-красная вспышка урона не блендилась поверх нашего цвета.
public class HitColorVertexConsumer implements VertexConsumer, VertexBufferWriter {
   private static final int NO_OVERLAY_U = OverlayTexture.DEFAULT_UV & 65535;
   private static final int NO_OVERLAY_V = OverlayTexture.DEFAULT_UV >> 16 & 65535;

   private final VertexConsumer delegate;
   private final int red;
   private final int green;
   private final int blue;
   private final int alpha;

   public HitColorVertexConsumer(VertexConsumer delegate, int argbColor) {
      this.delegate = delegate;
      this.red = ColorHelper.getRed(argbColor);
      this.green = ColorHelper.getGreen(argbColor);
      this.blue = ColorHelper.getBlue(argbColor);
      this.alpha = ColorHelper.getAlpha(argbColor);
   }

   @Override
   public void push(MemoryStack stack, long ptr, int count, net.minecraft.client.render.VertexFormat format) {
      VertexBufferWriter writer = VertexBufferWriter.of(this.delegate);
      if (writer != null) {
         writer.push(stack, ptr, count, format);
      }
   }

   @Override
   public VertexConsumer vertex(float x, float y, float z) {
      this.delegate.vertex(x, y, z);
      return this;
   }

   @Override
   public VertexConsumer color(int red, int green, int blue, int alpha) {
      this.delegate.color(this.red, this.green, this.blue, this.alpha);
      return this;
   }

   @Override
   public VertexConsumer texture(float u, float v) {
      this.delegate.texture(u, v);
      return this;
   }

   @Override
   public VertexConsumer overlay(int u, int v) {
      this.delegate.overlay(NO_OVERLAY_U, NO_OVERLAY_V);
      return this;
   }

   @Override
   public VertexConsumer light(int u, int v) {
      this.delegate.light(u, v);
      return this;
   }

   @Override
   public VertexConsumer normal(float x, float y, float z) {
      this.delegate.normal(x, y, z);
      return this;
   }
}