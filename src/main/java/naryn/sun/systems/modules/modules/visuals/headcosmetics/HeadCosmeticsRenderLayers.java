package naryn.sun.systems.modules.modules.visuals.headcosmetics;

import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormats;

/**
 * Кастомные RenderLayer для FeatureRenderer-оверлеев на голове игрока (China Hat, Halo).
 *
 * Раньше эти оверлеи рисовались напрямую через Tesselator с RenderSystem.depthMask(false),
 * чтобы не блокировать поздний flush ника игрока (см. references/entity-overlay-rendering.md,
 * п.5). Побочный эффект этого трюка — модуль Sky (и любой другой depth==1.0-пост-процесс)
 * стирал их на фоне открытого неба, потому что их настоящая глубина никогда не записывалась
 * (см. references/entity-overlay-rendering.md, п.6).
 *
 * Через нормальный RenderLayer геометрия попадает в тот же батч vertexConsumers, что ник,
 * броня и предмет в руке — порядок отрисовки и запись глубины решает сама игра, никаких
 * depthMask-трюков не нужно.
 *
 * Сигнатуры (RenderLayer.of, MultiPhaseParameters.Builder, RenderPhase.NO_TEXTURE и т.д.)
 * сверены построчно с декомпилированным RenderLayer.java/RenderPhase.java для Minecraft
 * 1.21.4 Yarn build.8 — не гадание, взято из реального байткода проекта.
 */
public final class HeadCosmeticsRenderLayers {

   private static final int FAN_BUFFER_SIZE = 256;
   private static final int TORUS_BUFFER_SIZE = 1536;

   /**
    * Боковая поверхность конуса (China Hat) — TRIANGLE_FAN, без текстуры, translucent,
    * пишет глубину как обычная translucent-геометрия (ALL_MASK по умолчанию в Builder —
    * см. ENTITY_TRANSLUCENT, там он тоже не переопределяется явно).
    */
   public static final RenderLayer HAT_CONE_SIDE = RenderLayer.of(
      "sun_hat_cone_side",
      VertexFormats.POSITION_COLOR,
      VertexFormat.DrawMode.TRIANGLE_FAN,
      FAN_BUFFER_SIZE,
      false,
      true,
      RenderLayer.MultiPhaseParameters.builder()
         .program(RenderPhase.POSITION_COLOR_PROGRAM)
         .texture(RenderPhase.NO_TEXTURE)
         .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
         .cull(RenderPhase.DISABLE_CULLING)
         .lightmap(RenderPhase.DISABLE_LIGHTMAP)
         .overlay(RenderPhase.DISABLE_OVERLAY_COLOR)
         .build(false)
   );

   /**
    * Дно конуса — ОТДЕЛЬНЫЙ RenderLayer, не переиспользовать HAT_CONE_SIDE. TRIANGLE_FAN
    * не поддерживает несколько независимых вееров в одном буфере: второй веер продолжил бы
    * первый вместо того чтобы начать новый (см. references/entity-overlay-rendering.md, п.1
    * — там же объяснение, почему у дна обратный порядок обхода вершин). Разные RenderLayer-
    * инстансы всегда попадают в разные draw call'ы, даже с полностью одинаковыми настройками —
    * этого достаточно, чтобы не мешать друг другу.
    */
   public static final RenderLayer HAT_CONE_BOTTOM = RenderLayer.of(
      "sun_hat_cone_bottom",
      VertexFormats.POSITION_COLOR,
      VertexFormat.DrawMode.TRIANGLE_FAN,
      FAN_BUFFER_SIZE,
      false,
      true,
      RenderLayer.MultiPhaseParameters.builder()
         .program(RenderPhase.POSITION_COLOR_PROGRAM)
         .texture(RenderPhase.NO_TEXTURE)
         .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
         .cull(RenderPhase.DISABLE_CULLING)
         .lightmap(RenderPhase.DISABLE_LIGHTMAP)
         .overlay(RenderPhase.DISABLE_OVERLAY_COLOR)
         .build(false)
   );

   /**
    * Тело тора (Halo) — QUADS, без текстуры, translucent. В отличие от TRIANGLE_FAN, QUADS
    * можно спокойно накапливать сколько угодно раз подряд в одном буфере (каждые 4 вершины —
    * независимый квад), поэтому одного RenderLayer достаточно на весь тор.
    *
    * ВАЖНО: свечение (drawGlow, billboard-квады на bloom.png) сюда НЕ входит и всё ещё
    * рисуется старым способом (Tesselator + depthMask(false)) — для него нужен ещё один
    * RenderPhase.ShaderProgram под текстурированный аддитивный квад, точное имя которого
    * не подтверждено декомпиляцией. Значит свечение всё ещё потенциально стирается модулем
    * Sky на фоне открытого неба, как и было — это открытый пункт, не часть этого фикса.
    */
   public static final RenderLayer HALO_TORUS = RenderLayer.of(
      "sun_halo_torus",
      VertexFormats.POSITION_COLOR,
      VertexFormat.DrawMode.QUADS,
      TORUS_BUFFER_SIZE,
      false,
      true,
      RenderLayer.MultiPhaseParameters.builder()
         .program(RenderPhase.POSITION_COLOR_PROGRAM)
         .texture(RenderPhase.NO_TEXTURE)
         .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
         .cull(RenderPhase.DISABLE_CULLING)
         .lightmap(RenderPhase.DISABLE_LIGHTMAP)
         .overlay(RenderPhase.DISABLE_OVERLAY_COLOR)
         .build(false)
   );

   public static final RenderLayer HAT_CONE_SIDE_OUTLINE = RenderLayer.of(
      "sun_hat_cone_side_outline",
      VertexFormats.POSITION_COLOR,
      VertexFormat.DrawMode.TRIANGLE_FAN,
      FAN_BUFFER_SIZE,
      false,
      false,
      RenderLayer.MultiPhaseParameters.builder()
         .program(RenderPhase.POSITION_COLOR_PROGRAM)
         .cull(RenderPhase.DISABLE_CULLING)
         .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
         .target(RenderPhase.OUTLINE_TARGET)
         .build(RenderLayer.OutlineMode.IS_OUTLINE)
   );

   public static final RenderLayer HAT_CONE_BOTTOM_OUTLINE = RenderLayer.of(
      "sun_hat_cone_bottom_outline",
      VertexFormats.POSITION_COLOR,
      VertexFormat.DrawMode.TRIANGLE_FAN,
      FAN_BUFFER_SIZE,
      false,
      false,
      RenderLayer.MultiPhaseParameters.builder()
         .program(RenderPhase.POSITION_COLOR_PROGRAM)
         .cull(RenderPhase.DISABLE_CULLING)
         .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
         .target(RenderPhase.OUTLINE_TARGET)
         .build(RenderLayer.OutlineMode.IS_OUTLINE)
   );

   public static final RenderLayer HALO_TORUS_OUTLINE = RenderLayer.of(
      "sun_halo_torus_outline",
      VertexFormats.POSITION_COLOR,
      VertexFormat.DrawMode.QUADS,
      TORUS_BUFFER_SIZE,
      false,
      false,
      RenderLayer.MultiPhaseParameters.builder()
         .program(RenderPhase.POSITION_COLOR_PROGRAM)
         .cull(RenderPhase.DISABLE_CULLING)
         .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
         .target(RenderPhase.OUTLINE_TARGET)
         .build(RenderLayer.OutlineMode.IS_OUTLINE)
   );

   private HeadCosmeticsRenderLayers() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}