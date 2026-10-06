package naryn.sun.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import naryn.sun.Sun;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.Render3DEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.ColorSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.render.DrawUtility;
import naryn.sun.utility.render.RenderUtility;
import naryn.sun.utility.render.Utils;
import naryn.sun.utility.time.Timer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Fresh Drop", category = ModuleCategory.VISUALS, desc = "Только что выброшенный предмет несколько секунд имеет красивое свечение")
public class FreshDrop extends BaseModule {

   private static final Identifier BLOOM_ID = Sun.id("textures/bloom.png");

   // ===== настройки =====
   private final SliderSetting duration = new SliderSetting(this, "modules.settings.fresh_drop.duration").min(1.0F).max(60.0F).step(1.0F).suffix(" sec").currentValue(5.0F);
   private final SliderSetting height = new SliderSetting(this, "modules.settings.fresh_drop.height").min(0.5F).max(5.0F).step(0.1F).suffix(" block").currentValue(2.0F);
   private final SliderSetting brightness = new SliderSetting(this, "modules.settings.fresh_drop.brightness").min(0.1F).max(3.0F).step(0.1F).currentValue(1.0F);
   private final ColorSetting color = new ColorSetting(this, "modules.settings.fresh_drop.color").color(Colors.ACCENT);

   // ===== состояние =====
   // все id ItemEntity, которые мы уже видели (чтобы не подсвечивать предметы, лежавшие до включения модуля)
   private final it.unimi.dsi.fastutil.ints.IntOpenHashSet seenIds = new it.unimi.dsi.fastutil.ints.IntOpenHashSet();
   // предметы, которые сейчас светятся
   private final it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<FreshDrop.GlowEntry> glowing = new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();
   private final it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<ItemEntity> currentEntities = new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();
   private boolean initialized = false;

   private final EventListener<Render3DEvent> on3DRender = event -> {
      if (this.glowing.isEmpty()) {
         return;
      }

      MatrixStack ms = event.getMatrices();
      Camera camera = mc.gameRenderer.getCamera();

      ms.push();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.enableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      RenderSystem.setShaderTexture(0, BLOOM_ID);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      float heightBlocks = this.height.getCurrentValue();
      float brightnessMul = this.brightness.getCurrentValue();
      int layers = 10;

      for (FreshDrop.GlowEntry glow : this.glowing.values()) {
         float fade = glow.alpha.getValue();
         if (fade <= 0.0F) {
            continue;
         }

         Vec3d pos = Utils.getInterpolatedPos(glow.prev, glow.pos, event.getTickDelta());

         for (int i = 0; i < layers; i++) {
            float t = (float) i / (float) (layers - 1);
            double yOffset = heightBlocks * t;
            float layerAlpha = (1.0F - t) * fade * brightnessMul;
            if (layerAlpha <= 0.0F) {
               continue;
            }

            float size = 0.7F - t * 0.3F;
            Vec3d layerPos = pos.add(0.0, yOffset, 0.0);

            ms.push();
            RenderUtility.prepareMatrices(ms, layerPos);
            ms.multiply(camera.getRotation());
            DrawUtility.drawImage(
               ms,
               builder,
               (double) (-size / 2.0F),
               (double) (-size / 2.0F),
               0.0,
               (double) size,
               (double) size,
               this.color.getColor().withAlpha(255.0F * Math.min(1.0F, layerAlpha))
            );
            ms.pop();
         }
      }

      BuiltBuffer builtBuffer = builder.endNullable();
      if (builtBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtBuffer);
      }

      RenderSystem.depthMask(true);
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
      RenderSystem.disableDepthTest();
      ms.pop();
   };

   @Override
   public void tick() {
      if (mc.world == null) {
         this.glowing.clear();
         this.seenIds.clear();
         this.initialized = false;
         return;
      }

      long durationMillis = (long) (this.duration.getCurrentValue() * 1000.0F);

      // собираем все ItemEntity, которые сейчас есть в мире
      this.currentEntities.clear();
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof ItemEntity itemEntity) {
            this.currentEntities.put(itemEntity.getId(), itemEntity);
         }
      }

      if (!this.initialized) {
         // при первом тике (включение модуля / вход в мир) просто запоминаем то, что уже лежит,
         // ничего не подсвечивая — иначе все предметы вокруг вспыхнут разом
         this.seenIds.addAll(this.currentEntities.keySet());
         this.initialized = true;
      } else {
         for (it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry<ItemEntity> entry : this.currentEntities.int2ObjectEntrySet()) {
            int entId = entry.getIntKey();
            if (!this.seenIds.contains(entId)) {
               this.seenIds.add(entId);
               FreshDrop.GlowEntry glowEntry = new FreshDrop.GlowEntry(entry.getValue().getPos());
               glowEntry.alpha.setDuration(300L);
               this.glowing.put(entId, glowEntry);
            }
         }
      }

      // убираем подсветку у предметов, которых больше нет в мире (подобрали / деспавн)
      this.glowing.keySet().removeIf(id -> !this.currentEntities.containsKey(id));

      for (it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry<FreshDrop.GlowEntry> e : this.glowing.int2ObjectEntrySet()) {
         FreshDrop.GlowEntry glow = e.getValue();
         ItemEntity itemEntity = this.currentEntities.get(e.getIntKey());
         if (itemEntity != null) {
            glow.updatePos(itemEntity.getPos());
         }
         glow.alpha.update(!glow.timer.finished(durationMillis));
      }

      // окончательно убираем полностью погасшие
      this.glowing.values().removeIf(glow -> glow.alpha.getValue() == 0.0F && glow.timer.finished(durationMillis));

      // чистим seenIds от id, которых больше нет в мире и которые уже не светятся, чтобы набор не рос бесконечно
      this.seenIds.removeIf(entId -> !this.currentEntities.containsKey(entId) && !this.glowing.containsKey(entId));
   }

   static class GlowEntry {
      Vec3d prev;
      Vec3d pos;
      final Timer timer = new Timer();
      final Animation alpha = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);

      GlowEntry(Vec3d pos) {
         this.pos = pos;
         this.prev = pos;
      }

      void updatePos(Vec3d newPos) {
         this.prev = this.pos;
         this.pos = newPos;
      }
   }
}