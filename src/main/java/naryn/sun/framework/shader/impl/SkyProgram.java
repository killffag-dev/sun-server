package naryn.sun.framework.shader.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.Sun;
import naryn.sun.framework.shader.GlProgram;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.interfaces.IWindow;
import naryn.sun.utility.render.SkyDomeGeometry;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class SkyProgram extends GlProgram implements IMinecraft, IWindow {
   private VertexBuffer skyDomeBuffer;

   private GlUniform timeUniform;
   private GlUniform speedUniform;
   private GlUniform scaleUniform;
   private GlUniform intensityUniform;
   private GlUniform modeUniform;
   private GlUniform octavesUniform;

   public SkyProgram() {
      super(Sun.id("sky/data"), VertexFormats.POSITION);
   }

   @Override
   protected void setup() {
      this.timeUniform = this.findUniform("Time");
      this.speedUniform = this.findUniform("Speed");
      this.scaleUniform = this.findUniform("Scale");
      this.intensityUniform = this.findUniform("Intensity");
      this.modeUniform = this.findUniform("Mode");
      this.octavesUniform = this.findUniform("Octaves");
      super.setup();
   }

   public void apply(
      Matrix4f modelView,
      Matrix4f projection,
      float time,
      float speed,
      float scale,
      float intensity,
      int mode,
      int octaves,
      boolean secondLayer
   ) {
      if (this.skyDomeBuffer == null || this.skyDomeBuffer.isClosed()) {
         this.skyDomeBuffer = SkyDomeGeometry.createBuffer();
      }

      this.use();
      if (this.timeUniform != null) this.timeUniform.set(time);
      if (this.speedUniform != null) this.speedUniform.set(speed);
      if (this.scaleUniform != null) this.scaleUniform.set(scale);
      if (this.intensityUniform != null) this.intensityUniform.set(intensity);
      if (this.modeUniform != null) this.modeUniform.set(mode);
      if (this.octavesUniform != null) this.octavesUniform.set(octaves);

      RenderSystem.enableDepthTest();
      RenderSystem.depthFunc(GL11.GL_LEQUAL);
      RenderSystem.depthMask(false);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();

      this.skyDomeBuffer.bind();
      this.skyDomeBuffer.draw(modelView, projection, RenderSystem.getShader());

      if (secondLayer) {
         if (this.timeUniform != null) this.timeUniform.set(time * 1.3F);
         if (this.scaleUniform != null) this.scaleUniform.set(scale * 1.6F);
         if (this.intensityUniform != null) this.intensityUniform.set(intensity * 0.45F);

         RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
         this.skyDomeBuffer.draw(modelView, projection, RenderSystem.getShader());
         RenderSystem.defaultBlendFunc();
      }

      VertexBuffer.unbind();

      RenderSystem.enableCull();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
   }
}