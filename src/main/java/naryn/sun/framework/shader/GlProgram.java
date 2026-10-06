package naryn.sun.framework.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import naryn.sun.mixin.accessors.ShaderProgramAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderLoader.LoadException;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus.Internal;

public class GlProgram {
   private static final List<Runnable> REGISTERED_PROGRAMS = new ArrayList<>();
   protected ShaderProgram backingProgram;
   protected ShaderProgramKey programKey;

   public GlProgram(Identifier id, VertexFormat vertexFormat) {
      this.programKey = new ShaderProgramKey(id.withPrefixedPath("core/"), vertexFormat, Defines.EMPTY);
      REGISTERED_PROGRAMS.add(() -> {
         try {
            this.backingProgram = MinecraftClient.getInstance().getShaderLoader().getProgramToLoad(this.programKey);
            this.setup();
         } catch (LoadException var2) {
            throw new RuntimeException("Failed to initialize shader program", var2);
         }
      });
   }

   private net.minecraft.client.render.RenderPhase.ShaderProgram cachedRenderPhase;
   private final java.util.Map<String, GlUniform> uniformCache = new java.util.HashMap<>();

   public net.minecraft.client.render.RenderPhase.ShaderProgram renderPhaseProgram() {
      if (this.cachedRenderPhase == null) {
         this.cachedRenderPhase = new net.minecraft.client.render.RenderPhase.ShaderProgram(this.programKey);
      }
      return this.cachedRenderPhase;
   }

   public ShaderProgram use() {
      return RenderSystem.setShader(this.programKey);
   }

   protected void setup() {
   }

   public GlUniform findUniform(String name) {
      GlUniform cached = this.uniformCache.get(name);
      if (cached != null) {
         return cached;
      }
      if (this.backingProgram == null) {
         return null;
      }
      GlUniform uniform = ((ShaderProgramAccessor)this.backingProgram).getUniformsByName().get(name);
      if (uniform != null) {
         this.uniformCache.put(name, uniform);
      }
      return uniform;
   }

   @Internal
   public static void loadAndSetupPrograms() {
      REGISTERED_PROGRAMS.forEach(Runnable::run);
   }
}
