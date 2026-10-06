package naryn.sun.systems.modules.modules.optimization;

import lombok.Generated;
import naryn.sun.Sun;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.player.ClientPlayerTickEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.culling.FrustumCuller;
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

    private final naryn.sun.systems.setting.settings.GroupSetting particlesGroup = new naryn.sun.systems.setting.settings.GroupSetting(this, "modules.settings.packet_filter.group.particles");
    private final BooleanSetting dropOffscreenParticles = new BooleanSetting(this.particlesGroup, "modules.settings.packet_filter.drop_offscreen_particles").enable();
    private final BooleanSetting dropObstacleParticles = new BooleanSetting(this.particlesGroup, "modules.settings.packet_filter.drop_obstacle_particles").enable();
    private final SliderSetting maxParticlesPerSecond = new SliderSetting(this.particlesGroup, "modules.settings.packet_filter.max_particles_per_sec")
            .min(50.0F)
            .max(500.0F)
            .step(25.0F)
            .currentValue(200.0F);

    private final naryn.sun.systems.setting.settings.GroupSetting generalGroup = new naryn.sun.systems.setting.settings.GroupSetting(this, "modules.settings.packet_filter.group.general");
    private final BooleanSetting soundLimiter = new BooleanSetting(this.generalGroup, "modules.settings.packet_filter.sound_limiter").enable();
    private final BooleanSetting burstProtection = new BooleanSetting(this.generalGroup, "modules.settings.packet_filter.burst_protection").enable();
    private final BooleanSetting entityThrottling = new BooleanSetting(this.generalGroup, "modules.settings.packet_filter.entity_throttling").enable();
    private final SliderSetting maxSoundsPerTick = new SliderSetting(this.generalGroup, "modules.settings.packet_filter.max_sounds_per_tick")
            .min(2.0F)
            .max(16.0F)
            .step(1.0F)
            .currentValue(6.0F);

    // Счетчики реального времени
    private static final AtomicInteger PARTICLE_COUNTER = new AtomicInteger(0);
    private static long lastParticleResetTime = System.currentTimeMillis();
    private static final it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap<String> TICK_SOUND_COUNTS = new it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap<>();

    static {
        TICK_SOUND_COUNTS.defaultReturnValue(0);
    }

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

        // 2. Проверка выхода за пределы экрана / поля зрения (FOV)
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.gameRenderer != null && mc.gameRenderer.getCamera() != null) {
            Camera camera = mc.gameRenderer.getCamera();
            double camX = camera.getPos().x;
            double camY = camera.getPos().y;
            double camZ = camera.getPos().z;

            double dx = x - camX;
            double dy = y - camY;
            double dz = z - camZ;
            double distSq = dx * dx + dy * dy + dz * dz;

            SmartCull smartCull = Sun.getInstance().getModuleManager().getModule(SmartCull.class);
            if (smartCull != null && smartCull.shouldCullParticle(dx, dy, dz, distSq)) {
                return true;
            } else if (this.dropOffscreenParticles.isEnabled()) {
                FrustumCuller.update(camera);
                if (distSq > 4.0 && !FrustumCuller.isRelativeInside(dx, dy, dz, 0.75)) {
                    return true;
                }
            }

            // 3. Проверка невидимости за стеной / под землей
            if (this.dropObstacleParticles.isEnabled() && mc.world != null) {
                if (distSq > 16.0) {
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
        int current = TICK_SOUND_COUNTS.getInt(soundId);
        if (current >= max) {
            return true;
        }
        TICK_SOUND_COUNTS.put(soundId, current + 1);
        return false;
    }

    @Generated
    public BooleanSetting getDropOffscreenParticles() {
        return this.dropOffscreenParticles;
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
