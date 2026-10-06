package naryn.sun.systems.modules.modules.optimization;

import lombok.Generated;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.player.ClientPlayerTickEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.culling.OcclusionCuller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.sound.SoundEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@ModuleInfo(name = "PacketFilter", category = ModuleCategory.OPTIMIZATION, enabledByDefault = true)
public class PacketFilter extends BaseModule {

    private final BooleanSetting dropObstacleParticles = new BooleanSetting(this, "modules.settings.packet_filter.drop_obstacle_particles").enable();
    private final SliderSetting maxParticlesPerSecond = new SliderSetting(this, "modules.settings.packet_filter.max_particles_per_sec")
            .min(50.0F)
            .max(500.0F)
            .step(25.0F)
            .currentValue(200.0F);

    private final BooleanSetting soundLimiter = new BooleanSetting(this, "modules.settings.packet_filter.sound_limiter").enable();
    private final SliderSetting maxSoundsPerTick = new SliderSetting(this, "modules.settings.packet_filter.max_sounds_per_tick")
            .min(2.0F)
            .max(16.0F)
            .step(1.0F)
            .currentValue(6.0F);

    private final BooleanSetting burstProtection = new BooleanSetting(this, "modules.settings.packet_filter.burst_protection").enable();
    private final BooleanSetting entityThrottling = new BooleanSetting(this, "modules.settings.packet_filter.entity_throttling").enable();

    // Счетчики реального времени
    private static final AtomicInteger PARTICLE_COUNTER = new AtomicInteger(0);
    private static long lastParticleResetTime = System.currentTimeMillis();
    private static final Map<String, Integer> TICK_SOUND_COUNTS = new HashMap<>();

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        TICK_SOUND_COUNTS.clear();
        long now = System.currentTimeMillis();
        if (now - lastParticleResetTime >= 1000L) {
            PARTICLE_COUNTER.set(0);
            lastParticleResetTime = now;
        }
    };

    /**
     * Проверяет, следует ли отклонить входящий пакет частиц.
     */
    public boolean shouldDropParticle(double x, double y, double z) {
        if (!isEnabled()) return false;

        // 1. Проверка лимита в секунду
        int current = PARTICLE_COUNTER.incrementAndGet();
        if (current > (int) this.maxParticlesPerSecond.getCurrentValue()) {
            return true;
        }

        // 2. Проверка невидимости за стеной / под землей
        if (this.dropObstacleParticles.isEnabled()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.gameRenderer != null && mc.gameRenderer.getCamera() != null && mc.world != null) {
                Camera camera = mc.gameRenderer.getCamera();
                double camX = camera.getPos().x;
                double camY = camera.getPos().y;
                double camZ = camera.getPos().z;

                double dx = x - camX;
                double dy = y - camY;
                double dz = z - camZ;
                if (dx * dx + dy * dy + dz * dz > 16.0) {
                    if (!OcclusionCuller.isPointVisible(camX, camY, camZ, x, y, z, mc.world)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Проверяет, следует ли отклонить входящий звук при спаме.
     */
    public boolean shouldDropSound(String soundId) {
        if (!isEnabled() || !this.soundLimiter.isEnabled() || soundId == null) {
            return false;
        }
        int max = (int) this.maxSoundsPerTick.getCurrentValue();
        int current = TICK_SOUND_COUNTS.getOrDefault(soundId, 0);
        if (current >= max) {
            return true;
        }
        TICK_SOUND_COUNTS.put(soundId, current + 1);
        return false;
    }

    @Generated
    public BooleanSetting getDropObstacleParticles() {
        return this.dropObstacleParticles;
    }

    @Generated
    public SliderSetting getMaxParticlesPerSecond() {
        return this.maxParticlesPerSecond;
    }

    @Generated
    public BooleanSetting getSoundLimiter() {
        return this.soundLimiter;
    }

    @Generated
    public SliderSetting getMaxSoundsPerTick() {
        return this.maxSoundsPerTick;
    }

    @Generated
    public BooleanSetting getBurstProtection() {
        return this.burstProtection;
    }

    @Generated
    public BooleanSetting getEntityThrottling() {
        return this.entityThrottling;
    }
}
