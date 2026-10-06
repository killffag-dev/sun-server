package naryn.sun.systems.modules.modules.optimization;

import naryn.sun.Sun;

public final class OptimizationPresets {

    private static OptimizationPreset currentPreset = OptimizationPreset.CUSTOM;

    private OptimizationPresets() {
    }

    public static OptimizationPreset getCurrentPreset() {
        return currentPreset;
    }

    public static void apply(OptimizationPreset preset) {
        currentPreset = preset;
        Optimizer optimizer = Sun.getInstance().getModuleManager().getModule(Optimizer.class);
        NoRender noRender = Sun.getInstance().getModuleManager().getModule(NoRender.class);
        PacketFilter packetFilter = Sun.getInstance().getModuleManager().getModule(PacketFilter.class);

        if (preset == OptimizationPreset.ULTRA) {
            if (optimizer != null) {
                optimizer.enable();
                optimizer.getEntityShadows().enable();
                optimizer.getEntityOcclusionCulling().enable();
                optimizer.getBlockEntityOcclusionCulling().enable();
                optimizer.getEntityDistanceCulling().enable();
                optimizer.getCullDistance().setCurrentValue(128.0F);
                optimizer.getPlayerDistanceCulling().enable();
                optimizer.getPlayerCullDistance().setCurrentValue(128.0F);
                optimizer.getBlockEntityDistanceCulling().enable();
                optimizer.getBlockEntityCullDistance().setCurrentValue(96.0F);
                optimizer.getHideArmorStands().enabled(false);
                optimizer.getDisableFireworks().enabled(false);
                optimizer.getCullParticles().enabled(false);
                optimizer.getDisableGlint().enabled(false);
                optimizer.getStaticFluids().enabled(false);
            }
            if (noRender != null) {
                noRender.disable();
            }
            if (packetFilter != null) {
                packetFilter.enable();
                packetFilter.getDropObstacleParticles().enable();
                packetFilter.getMaxParticlesPerSecond().setCurrentValue(300.0F);
                packetFilter.getSoundLimiter().enable();
                packetFilter.getMaxSoundsPerTick().setCurrentValue(12.0F);
                packetFilter.getBurstProtection().enable();
                packetFilter.getEntityThrottling().enabled(false);
            }
        } else if (preset == OptimizationPreset.MEDIUM) {
            if (optimizer != null) {
                optimizer.enable();
                optimizer.getEntityShadows().enable();
                optimizer.getEntityOcclusionCulling().enable();
                optimizer.getBlockEntityOcclusionCulling().enable();
                optimizer.getEntityDistanceCulling().enable();
                optimizer.getCullDistance().setCurrentValue(64.0F);
                optimizer.getPlayerDistanceCulling().enable();
                optimizer.getPlayerCullDistance().setCurrentValue(64.0F);
                optimizer.getBlockEntityDistanceCulling().enable();
                optimizer.getBlockEntityCullDistance().setCurrentValue(48.0F);
                optimizer.getHideArmorStands().enabled(false);
                optimizer.getDisableFireworks().enabled(false);
                optimizer.getCullParticles().enable();
                optimizer.getDisableGlint().enabled(false);
                optimizer.getStaticFluids().enabled(false);
            }
            if (noRender != null) {
                noRender.enable();
                noRender.getElderGuardian().select();
                noRender.getDragonBreath().select();
                noRender.getBubbles().select();
            }
            if (packetFilter != null) {
                packetFilter.enable();
                packetFilter.getDropObstacleParticles().enable();
                packetFilter.getMaxParticlesPerSecond().setCurrentValue(200.0F);
                packetFilter.getSoundLimiter().enable();
                packetFilter.getMaxSoundsPerTick().setCurrentValue(8.0F);
                packetFilter.getBurstProtection().enable();
                packetFilter.getEntityThrottling().enable();
            }
        } else if (preset == OptimizationPreset.LOW) {
            if (optimizer != null) {
                optimizer.enable();
                optimizer.getEntityShadows().enabled(false);
                optimizer.getEntityOcclusionCulling().enable();
                optimizer.getBlockEntityOcclusionCulling().enable();
                optimizer.getEntityDistanceCulling().enable();
                optimizer.getCullDistance().setCurrentValue(32.0F);
                optimizer.getPlayerDistanceCulling().enable();
                optimizer.getPlayerCullDistance().setCurrentValue(32.0F);
                optimizer.getBlockEntityDistanceCulling().enable();
                optimizer.getBlockEntityCullDistance().setCurrentValue(32.0F);
                optimizer.getHideArmorStands().enable();
                optimizer.getDisableFireworks().enable();
                optimizer.getCullParticles().enable();
                optimizer.getDisableGlint().enable();
                optimizer.getStaticFluids().enable();
            }
            if (noRender != null) {
                noRender.enable();
                noRender.getFireworks().select();
                noRender.getExplosions().select();
                noRender.getTotem().select();
                noRender.getBubbles().select();
                noRender.getPotions().select();
                noRender.getElderGuardian().select();
                noRender.getDragonBreath().select();
            }
            if (packetFilter != null) {
                packetFilter.enable();
                packetFilter.getDropObstacleParticles().enable();
                packetFilter.getMaxParticlesPerSecond().setCurrentValue(100.0F);
                packetFilter.getSoundLimiter().enable();
                packetFilter.getMaxSoundsPerTick().setCurrentValue(4.0F);
                packetFilter.getBurstProtection().enable();
                packetFilter.getEntityThrottling().enable();
            }
        } else if (preset == OptimizationPreset.POTATO) {
            if (optimizer != null) {
                optimizer.enable();
                optimizer.getEntityShadows().enabled(false);
                optimizer.getEntityOcclusionCulling().enable();
                optimizer.getBlockEntityOcclusionCulling().enable();
                optimizer.getEntityDistanceCulling().enable();
                optimizer.getCullDistance().setCurrentValue(16.0F);
                optimizer.getPlayerDistanceCulling().enable();
                optimizer.getPlayerCullDistance().setCurrentValue(24.0F);
                optimizer.getBlockEntityDistanceCulling().enable();
                optimizer.getBlockEntityCullDistance().setCurrentValue(16.0F);
                optimizer.getHideArmorStands().enable();
                optimizer.getDisableFireworks().enable();
                optimizer.getCullParticles().enable();
                optimizer.getDisableGlint().enable();
                optimizer.getStaticFluids().enable();
                optimizer.getAggressiveGc().enable();
            }
            if (noRender != null) {
                noRender.enable();
                noRender.getFireworks().select();
                noRender.getExplosions().select();
                noRender.getTotem().select();
                noRender.getBubbles().select();
                noRender.getPotions().select();
                noRender.getBlockBreak().select();
                noRender.getRain().select();
                noRender.getCrits().select();
                noRender.getElderGuardian().select();
                noRender.getDragonBreath().select();
                noRender.getHearts().select();
            }
            if (packetFilter != null) {
                packetFilter.enable();
                packetFilter.getDropObstacleParticles().enable();
                packetFilter.getMaxParticlesPerSecond().setCurrentValue(50.0F);
                packetFilter.getSoundLimiter().enable();
                packetFilter.getMaxSoundsPerTick().setCurrentValue(2.0F);
                packetFilter.getBurstProtection().enable();
                packetFilter.getEntityThrottling().enable();
            }
        }
    }
}
