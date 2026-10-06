package naryn.sun.systems.modules.modules.optimization;

import lombok.Generated;
import naryn.sun.Sun;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.game.WorldChangeEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.GroupSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.culling.OcclusionCuller;
import naryn.sun.utility.profiler.MemoryTracker;

@ModuleInfo(name = "Optimizer", category = ModuleCategory.OPTIMIZATION, enabledByDefault = true)
public class Optimizer extends BaseModule {
   // === 1. Mode First (Top-Level) ===
   private final ModeSetting preset = new ModeSetting(this, "modules.settings.optimizer.preset");
   private final ModeSetting.Value customPreset = new ModeSetting.Value(this.preset, "modules.settings.optimizer.preset.custom");
   private final ModeSetting.Value ultraPreset = new ModeSetting.Value(this.preset, "modules.settings.optimizer.preset.ultra");
   private final ModeSetting.Value mediumPreset = new ModeSetting.Value(this.preset, "modules.settings.optimizer.preset.medium");
   private final ModeSetting.Value lowPreset = new ModeSetting.Value(this.preset, "modules.settings.optimizer.preset.low");
   private final ModeSetting.Value potatoPreset = new ModeSetting.Value(this.preset, "modules.settings.optimizer.preset.potato");

   // === 2. Culling Box ===
   private final GroupSetting cullingGroup = new GroupSetting(this, "modules.settings.optimizer.group.culling");
   private final BooleanSetting frustumCulling = new BooleanSetting(this.cullingGroup, "modules.settings.optimizer.frustum_culling").enable();
   private final BooleanSetting entityOcclusionCulling = new BooleanSetting(this.cullingGroup, "modules.settings.optimizer.entity_occlusion_culling").enable();
   private final BooleanSetting blockEntityOcclusionCulling = new BooleanSetting(this.cullingGroup, "modules.settings.optimizer.block_entity_occlusion_culling").enable();
   private final BooleanSetting entityDistanceCulling = new BooleanSetting(this.cullingGroup, "modules.settings.optimizer.entity_distance_culling").enable();
   private final BooleanSetting playerDistanceCulling = new BooleanSetting(this.cullingGroup, "modules.settings.optimizer.player_distance_culling").enable();
   private final BooleanSetting blockEntityDistanceCulling = new BooleanSetting(this.cullingGroup, "modules.settings.optimizer.block_entity_distance_culling").enable();
   private final SliderSetting cullDistance = new SliderSetting(this.cullingGroup, "modules.settings.optimizer.cull_distance")
      .min(16.0F)
      .max(128.0F)
      .step(4.0F)
      .currentValue(64.0F);
   private final SliderSetting playerCullDistance = new SliderSetting(this.cullingGroup, "modules.settings.optimizer.player_cull_distance")
      .min(16.0F)
      .max(128.0F)
      .step(4.0F)
      .currentValue(64.0F);
   private final SliderSetting blockEntityCullDistance = new SliderSetting(this.cullingGroup, "modules.settings.optimizer.block_entity_cull_distance")
      .min(16.0F)
      .max(96.0F)
      .step(4.0F)
      .currentValue(48.0F);

   // === 3. General & Logic Box ===
   private final GroupSetting generalGroup = new GroupSetting(this, "modules.settings.optimizer.group.general");
   private final BooleanSetting entityShadows = new BooleanSetting(this.generalGroup, "modules.settings.optimizer.entity_shadows").enable();
   private final BooleanSetting hideArmorStands = new BooleanSetting(this.generalGroup, "modules.settings.optimizer.hide_armor_stands");
   private final BooleanSetting disableFireworks = new BooleanSetting(this.generalGroup, "modules.settings.optimizer.disable_fireworks");
   private final BooleanSetting cullParticles = new BooleanSetting(this.generalGroup, "modules.settings.optimizer.cull_particles").enable();
   private final BooleanSetting disableGlint = new BooleanSetting(this.generalGroup, "modules.settings.optimizer.disable_glint");
   private final BooleanSetting staticFluids = new BooleanSetting(this.generalGroup, "modules.settings.optimizer.static_fluids");
   private final BooleanSetting adaptiveMobLod = new BooleanSetting(this.generalGroup, "modules.settings.optimizer.adaptive_mob_lod").enable();
   private final BooleanSetting aggressiveGc = new BooleanSetting(this.generalGroup, "modules.settings.optimizer.aggressive_gc").enable();
   private final naryn.sun.systems.setting.settings.ButtonSetting copyZgc = new naryn.sun.systems.setting.settings.ButtonSetting(this.generalGroup, "modules.settings.optimizer.copy_zgc").action(() -> {
      if (naryn.sun.utility.jvm.JvmLauncherPresets.copyToClipboard(naryn.sun.utility.jvm.JvmLauncherPresets.GENERATIONAL_ZGC)) {
         Sun.getInstance().getNotificationManager().addNotificationOther(
            naryn.sun.systems.notifications.NotificationType.INFO,
            "JVM Tuning",
            naryn.sun.systems.localization.Localizator.translate("menu.gui_settings.jvm.copied")
         );
      }
   });

   {
      this.preset.onChange(this::applyPreset);
      this.customPreset.select();
   }

   private final EventListener<WorldChangeEvent> onWorldChange = event -> {
      OcclusionCuller.clearCache();
      if (this.aggressiveGc.isEnabled()) {
         System.gc();
      }
      MemoryTracker.updateRateIfNeeded();
   };

   public void applyPreset(ModeSetting.Value selected) {
      if (selected == this.ultraPreset) {
         OptimizationPresets.apply(OptimizationPreset.ULTRA);
      } else if (selected == this.mediumPreset) {
         OptimizationPresets.apply(OptimizationPreset.MEDIUM);
      } else if (selected == this.lowPreset) {
         OptimizationPresets.apply(OptimizationPreset.LOW);
      } else if (selected == this.potatoPreset) {
         OptimizationPresets.apply(OptimizationPreset.POTATO);
      }
   }

   @Generated
   public ModeSetting getPreset() {
      return this.preset;
   }

   @Generated
   public BooleanSetting getEntityShadows() {
      return this.entityShadows;
   }

   @Generated
   public BooleanSetting getEntityOcclusionCulling() {
      return this.entityOcclusionCulling;
   }

   @Generated
   public BooleanSetting getBlockEntityOcclusionCulling() {
      return this.blockEntityOcclusionCulling;
   }

   @Generated
   public BooleanSetting getEntityDistanceCulling() {
      return this.entityDistanceCulling;
   }

   @Generated
   public SliderSetting getCullDistance() {
      return this.cullDistance;
   }

   @Generated
   public BooleanSetting getPlayerDistanceCulling() {
      return this.playerDistanceCulling;
   }

   @Generated
   public SliderSetting getPlayerCullDistance() {
      return this.playerCullDistance;
   }

   @Generated
   public BooleanSetting getBlockEntityDistanceCulling() {
      return this.blockEntityDistanceCulling;
   }

   @Generated
   public SliderSetting getBlockEntityCullDistance() {
      return this.blockEntityCullDistance;
   }

   @Generated
   public BooleanSetting getHideArmorStands() {
      return this.hideArmorStands;
   }

   @Generated
   public BooleanSetting getDisableFireworks() {
      return this.disableFireworks;
   }

   @Generated
   public BooleanSetting getCullParticles() {
      return this.cullParticles;
   }

   @Generated
   public BooleanSetting getDisableGlint() {
      return this.disableGlint;
   }

   @Generated
   public BooleanSetting getFrustumCulling() {
      return this.frustumCulling;
   }

   @Generated
   public BooleanSetting getAdaptiveMobLod() {
      return this.adaptiveMobLod;
   }

   @Generated
   public BooleanSetting getStaticFluids() {
      return this.staticFluids;
   }

   @Generated
   public BooleanSetting getAggressiveGc() {
      return this.aggressiveGc;
   }
}
