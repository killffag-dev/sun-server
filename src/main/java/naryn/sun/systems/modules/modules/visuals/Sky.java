package naryn.sun.systems.modules.modules.visuals;

import lombok.Generated;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.Render3DBackgroundEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.render.DrawUtility;
import org.joml.Matrix4f;

@ModuleInfo(name = "Sky", category = ModuleCategory.VISUALS, desc = "modules.descriptions.sky")
public class Sky extends BaseModule {

   private final ModeSetting mode = new ModeSetting(this, "modules.settings.sky.mode");
   private final ModeSetting.Value aquaMode = new ModeSetting.Value(this.mode, "modules.settings.sky.mode.aqua");
   private final ModeSetting.Value magmaMode = new ModeSetting.Value(this.mode, "modules.settings.sky.mode.magma");

   private final ModeSetting quality = new ModeSetting(this, "modules.settings.sky.quality");
   private final ModeSetting.Value highQuality = new ModeSetting.Value(this.quality, "modules.settings.sky.quality.high");
   private final ModeSetting.Value lowQuality = new ModeSetting.Value(this.quality, "modules.settings.sky.quality.low");

   private final naryn.sun.systems.setting.settings.GroupSetting generalGroup = new naryn.sun.systems.setting.settings.GroupSetting(this, "modules.settings.sky.group.general");
   private final BooleanSetting secondLayer = new BooleanSetting(this.generalGroup, "modules.settings.sky.second_layer");

   private final SliderSetting intensity = new SliderSetting(this.generalGroup, "modules.settings.sky.intensity")
      .min(0.0F)
      .max(3.0F)
      .step(0.05F)
      .currentValue(1.0F);
   private final SliderSetting speed = new SliderSetting(this.generalGroup, "modules.settings.sky.speed")
      .min(0.0F)
      .max(3.0F)
      .step(0.05F)
      .currentValue(1.0F);
   private final SliderSetting scale = new SliderSetting(this.generalGroup, "modules.settings.sky.scale")
      .min(0.1F)
      .max(5.0F)
      .step(0.05F)
      .currentValue(1.0F);

   {
      this.highQuality.select();
   }

   private float time;
   private long lastNanoTime;

   // ВАЖНО: слушает Render3DBackgroundEvent (WorldRenderEvents.BEFORE_ENTITIES),
   // а НЕ общий Render3DEvent (WorldRenderEvents.END), на котором сидят остальные
   // визуальные модули (Trails, FreshDrop, TargetESP и т.д.). Sky обязан рисовать
   // фон ДО любых сущностей за кадр — иначе (как было раньше) он перекрашивает уже
   // готовый кадр и стирает всё, что не пишет глубину (ники, HeadCosmetics,
   // полупрозрачные оверлеи). Подробности — javadoc Render3DBackgroundEvent и
   // references/entity-overlay-rendering.md (п.6-7) в sun-client skill.
   //
   // getPriority() больше не переопределяется: раньше приоритет 100 был нужен,
   // чтобы Sky гарантированно рисовался раньше depthMask(false)-модулей на общем
   // Render3DEvent — теперь Sky вообще не на этой очереди, конкурировать не с кем.
   private final EventListener<Render3DBackgroundEvent> onRenderBackground = new EventListener<Render3DBackgroundEvent>() {
      @Override
      public void onEvent(Render3DBackgroundEvent event) {
         Matrix4f modelView = event.getPositionMatrix();
         Matrix4f projection = event.getProjectionMatrix();

         if (naryn.sun.utility.compatibility.IrisCompatibility.isShadersActive()) {
            return;
         }

         long now = System.nanoTime();
         float deltaTime = Sky.this.lastNanoTime == 0L ? 0.0F : (now - Sky.this.lastNanoTime) / 1.0E9F;
         Sky.this.lastNanoTime = now;
         Sky.this.time += deltaTime;

         int modeIndex = Sky.this.magmaMode.isSelected() ? 1 : 0;
         int octaves = Sky.this.lowQuality.isSelected() ? 2 : 4;
         boolean hasSecondLayer = Sky.this.secondLayer.isEnabled();

         DrawUtility.skyProgram
            .apply(
               modelView,
               projection,
               Sky.this.time,
               Sky.this.speed.getCurrentValue(),
               Sky.this.scale.getCurrentValue(),
               Sky.this.intensity.getCurrentValue(),
               modeIndex,
               octaves,
               hasSecondLayer
            );
      }
   };

   @Override
   public void onEnable() {
      this.time = 0.0F;
      this.lastNanoTime = 0L;
      super.onEnable();
   }

   @Generated
   public ModeSetting getMode() {
      return this.mode;
   }

   @Generated
   public ModeSetting getQuality() {
      return this.quality;
   }

   @Generated
   public BooleanSetting getSecondLayer() {
      return this.secondLayer;
   }

   @Generated
   public SliderSetting getIntensity() {
      return this.intensity;
   }

   @Generated
   public SliderSetting getSpeed() {
      return this.speed;
   }

   @Generated
   public SliderSetting getScale() {
      return this.scale;
   }
}