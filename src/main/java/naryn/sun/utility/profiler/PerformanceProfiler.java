package naryn.sun.utility.profiler;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import naryn.sun.Sun;
import naryn.sun.utility.interfaces.IMinecraft;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;

/**
 * Высокоточный движок профилирования FPS и анализа первопричин просадок с нулевым влиянием на производительность.
 */
public final class PerformanceProfiler implements IMinecraft {

    private static final PerformanceProfiler INSTANCE = new PerformanceProfiler();

    public static PerformanceProfiler getInstance() {
        return INSTANCE;
    }

    private PerformanceProfiler() {}

    // Состояние сессии
    private volatile boolean active = false;
    private float spikeSensitivityMs = 30.0f;
    private boolean autoOpenReport = true;

    private long sessionStartNanos;
    private long sessionStartWallClock;
    private long frameStartNanos;
    private float avgFrameTimeMs = 0.0f;
    private float lastFrameFps = 60.0f;

    // Глобальные метрики
    private int totalFrames = 0;
    private long totalFrameTimeNanos = 0;
    private long minFrameTimeNanos = Long.MAX_VALUE;
    private long maxFrameTimeNanos = 0;

    // Буфер времени кадров (в наносекундах)
    private static final int MAX_RECORD_FRAMES = 1_000_000;
    private float[] frameTimesMs = new float[0];
    private int frameIndex = 0;

    // Транзиентные покадровые счётчики (сбрасываются каждый кадр)
    private final AtomicInteger chunksLoadedThisFrame = new AtomicInteger();
    private final AtomicInteger packetsReceivedThisFrame = new AtomicInteger();
    private final AtomicInteger chunkPacketsThisFrame = new AtomicInteger();
    private final AtomicInteger entityPacketsThisFrame = new AtomicInteger();
    private final AtomicInteger blockPacketsThisFrame = new AtomicInteger();
    private final AtomicInteger explosionsThisFrame = new AtomicInteger();

    // Кумулятивная статистика подсистем
    private int totalChunkLoads = 0;
    private int totalPacketsReceived = 0;
    private int maxEntitiesInFrame = 0;
    private int maxMobsInFrame = 0;
    private int maxItemsInFrame = 0;
    private int maxPlayersInFrame = 0;
    private int maxParticlesCount = 0;
    private long sumLoadedChunks = 0;
    private int chunkSampleCount = 0;

    // GC метрики
    private long startGcCount = 0;
    private long startGcTimeMs = 0;
    private long lastGcCount = 0;
    private long lastGcTimeMs = 0;

    // Системные метрики ПК (кэшируются периодически для минимизации оверхеда)
    private float cachedSysCpu = 0.0f;
    private float cachedJvmCpu = 0.0f;
    private long cachedSysUsedRamMb = 0;
    private long cachedSysTotalRamMb = 0;
    private long lastSystemSampleNanos = 0;
    private net.minecraft.client.gui.screen.Screen lastScreenInstance = null;
    private String cachedScreenName = "InGame";

    private String getScreenName(MinecraftClient client) {
        if (client == null || client.currentScreen == null) {
            this.lastScreenInstance = null;
            return "InGame";
        }
        net.minecraft.client.gui.screen.Screen screen = client.currentScreen;
        if (screen != this.lastScreenInstance) {
            this.lastScreenInstance = screen;
            this.cachedScreenName = screen.getClass().getSimpleName();
        }
        return this.cachedScreenName;
    }

    // Зафиксированные спайки (топ худших)
    private final List<LagSpikeRecord> worstSpikes = new ArrayList<>();
    private final Map<SpikeCause, Integer> causeBreakdown = new EnumMap<>(SpikeCause.class);
    private final Map<SpikeCause, Double> causeTotalFpsDrop = new EnumMap<>(SpikeCause.class);
    private int criticalSpikesCount = 0;

    // Хранение покадровой истории с защитой от переполнения памяти (ring buffer cap 10,000)
    private static final int MAX_FRAME_RECORDS = 10_000;
    private final Deque<FrameRecord> frameRecords = new ArrayDeque<>(MAX_FRAME_RECORDS);

    public boolean isActive() { return this.active; }

    public synchronized void start(float spikeSensitivityMs, boolean autoOpenReport) {
        this.spikeSensitivityMs = spikeSensitivityMs;
        this.autoOpenReport = autoOpenReport;
        this.active = true;
        this.sessionStartNanos = System.nanoTime();
        this.sessionStartWallClock = System.currentTimeMillis();
        this.frameStartNanos = this.sessionStartNanos;
        this.lastSystemSampleNanos = 0;
        this.avgFrameTimeMs = 0.0f;
        this.lastFrameFps = 60.0f;
        this.totalFrames = 0;
        this.totalFrameTimeNanos = 0;
        this.minFrameTimeNanos = Long.MAX_VALUE;
        this.maxFrameTimeNanos = 0;
        this.frameTimesMs = new float[60_000];
        this.frameIndex = 0;
        this.chunksLoadedThisFrame.set(0);
        this.packetsReceivedThisFrame.set(0);
        this.chunkPacketsThisFrame.set(0);
        this.entityPacketsThisFrame.set(0);
        this.blockPacketsThisFrame.set(0);
        this.explosionsThisFrame.set(0);
        this.totalChunkLoads = 0;
        this.totalPacketsReceived = 0;
        this.maxEntitiesInFrame = 0;
        this.maxMobsInFrame = 0;
        this.maxItemsInFrame = 0;
        this.maxPlayersInFrame = 0;
        this.maxParticlesCount = 0;
        this.sumLoadedChunks = 0;
        this.chunkSampleCount = 0;
        this.startGcCount = MemoryTracker.getTotalGcCount();
        this.startGcTimeMs = MemoryTracker.getTotalGcTimeMs();
        this.lastGcCount = this.startGcCount;
        this.lastGcTimeMs = this.startGcTimeMs;
        this.cachedSysCpu = SystemProcessTracker.getSystemCpuLoad();
        this.cachedJvmCpu = SystemProcessTracker.getProcessCpuLoad();
        this.cachedSysUsedRamMb = SystemProcessTracker.getSystemUsedMemoryMb();
        this.cachedSysTotalRamMb = SystemProcessTracker.getSystemTotalMemoryMb();
        this.worstSpikes.clear();
        this.causeBreakdown.clear();
        this.causeTotalFpsDrop.clear();
        this.criticalSpikesCount = 0;
        synchronized (this.frameRecords) {
            this.frameRecords.clear();
        }
    }

    public synchronized void stop(File targetDirectory) {
        if (!this.active) return;
        this.active = false;
        long sessionDurationNanos = System.nanoTime() - this.sessionStartNanos;
        PerformanceReportGenerator.SystemSnapshot snapshot = PerformanceReportGenerator.SystemSnapshot.capture();
        SessionData data = calculateSessionData(sessionDurationNanos, snapshot);
        this.frameTimesMs = new float[0];
        try {
            PerformanceReportGenerator.generateAndSaveReport(data, this.spikeSensitivityMs, this.autoOpenReport, targetDirectory);
        } catch (Throwable t) {
            Sun.LOGGER.error("Failed to generate performance report", t);
        }
    }

    public synchronized void stop() {
        this.stop(null);
    }

    public void onFrameStart() {
        if (!this.active) return;
        this.frameStartNanos = System.nanoTime();
    }

    public void onFrameEnd() {
        if (!this.active) return;
        long now = System.nanoTime();
        long wallClockNow = System.currentTimeMillis();
        long frameDurationNanos = now - this.frameStartNanos;
        if (frameDurationNanos <= 0) frameDurationNanos = 1;
        float frameTimeMs = (float) (frameDurationNanos / 1_000_000.0);
        float currentFps = 1000.0f / frameTimeMs;

        this.totalFrames++;
        this.totalFrameTimeNanos += frameDurationNanos;
        if (frameDurationNanos < this.minFrameTimeNanos) this.minFrameTimeNanos = frameDurationNanos;
        if (frameDurationNanos > this.maxFrameTimeNanos) this.maxFrameTimeNanos = frameDurationNanos;

        if (this.frameIndex < MAX_RECORD_FRAMES) {
            if (this.frameIndex >= this.frameTimesMs.length) {
                float[] expanded = new float[Math.min(MAX_RECORD_FRAMES, this.frameTimesMs.length * 2)];
                System.arraycopy(this.frameTimesMs, 0, expanded, 0, this.frameTimesMs.length);
                this.frameTimesMs = expanded;
            }
            this.frameTimesMs[this.frameIndex++] = frameTimeMs;
        }

        if (this.avgFrameTimeMs == 0.0f) this.avgFrameTimeMs = frameTimeMs;
        else this.avgFrameTimeMs = this.avgFrameTimeMs * 0.95f + frameTimeMs * 0.05f;

        // Периодическое обновление загрузки CPU и RAM ПК (раз в 50 мс)
        if (now - this.lastSystemSampleNanos >= 50_000_000L) {
            this.cachedSysCpu = SystemProcessTracker.getSystemCpuLoad();
            this.cachedJvmCpu = SystemProcessTracker.getProcessCpuLoad();
            this.cachedSysUsedRamMb = SystemProcessTracker.getSystemUsedMemoryMb();
            this.cachedSysTotalRamMb = SystemProcessTracker.getSystemTotalMemoryMb();
            this.lastSystemSampleNanos = now;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int totalEntities = 0, players = 0, mobs = 0, items = 0, armorStands = 0, loadedChunks = 0;
        if (client != null && client.world != null) {
            for (Entity e : client.world.getEntities()) {
                totalEntities++;
                if (e instanceof PlayerEntity) players++;
                else if (e instanceof HostileEntity || e instanceof PassiveEntity) mobs++;
                else if (e instanceof ItemEntity) items++;
                else if (e instanceof ArmorStandEntity) armorStands++;
            }
            this.maxEntitiesInFrame = Math.max(this.maxEntitiesInFrame, totalEntities);
            this.maxMobsInFrame = Math.max(this.maxMobsInFrame, mobs);
            this.maxItemsInFrame = Math.max(this.maxItemsInFrame, items);
            this.maxPlayersInFrame = Math.max(this.maxPlayersInFrame, players);
            if (client.world.getChunkManager() != null) {
                loadedChunks = client.world.getChunkManager().getLoadedChunkCount();
                this.sumLoadedChunks += loadedChunks;
                this.chunkSampleCount++;
            }
        }

        long currentGcCount = MemoryTracker.getTotalGcCount();
        long currentGcTime = MemoryTracker.getTotalGcTimeMs();
        long gcCountDelta = currentGcCount - this.lastGcCount;
        long gcTimeDelta = currentGcTime - this.lastGcTimeMs;
        this.lastGcCount = currentGcCount;
        this.lastGcTimeMs = currentGcTime;
        long heapUsedMb = MemoryTracker.getUsedMemoryMb();

        int chunksLoaded = this.chunksLoadedThisFrame.getAndSet(0);
        int packets = this.packetsReceivedThisFrame.getAndSet(0);
        int chunkPackets = this.chunkPacketsThisFrame.getAndSet(0);
        int entityPackets = this.entityPacketsThisFrame.getAndSet(0);
        int blockPackets = this.blockPacketsThisFrame.getAndSet(0);
        int explosions = this.explosionsThisFrame.getAndSet(0);
        this.totalChunkLoads += chunksLoaded;
        this.totalPacketsReceived += packets;

        boolean isSpike = frameTimeMs >= this.spikeSensitivityMs || (frameTimeMs >= 22.0f && frameTimeMs > this.avgFrameTimeMs * 1.5f);
        SpikeCause cause = SpikeCause.NONE;
        if (gcCountDelta > 0 || gcTimeDelta > 3) {
            cause = SpikeCause.GC_PAUSE;
        } else if (chunksLoaded > 0 || chunkPackets > 0) {
            cause = SpikeCause.CHUNK_LOADING;
        } else if (items > 100 || mobs > 70 || totalEntities > 180) {
            cause = SpikeCause.ENTITIES_DENSITY;
        } else if (explosions > 0) {
            cause = SpikeCause.PARTICLE_BURST;
        } else if (packets > 20) {
            cause = SpikeCause.NETWORK_BURST;
        } else if (client != null && client.currentScreen != null) {
            cause = SpikeCause.GUI_OVERHEAD;
        } else if (this.cachedSysCpu > 85.0f && (this.cachedSysCpu - this.cachedJvmCpu) > 40.0f) {
            cause = SpikeCause.HIGH_SYSTEM_CPU;
        } else if (isSpike) {
            cause = SpikeCause.GPU_TERRAIN_STALL;
        }

        String screenName = this.getScreenName(client);
        long offsetMs = (now - this.sessionStartNanos) / 1_000_000L;

        if (isSpike) {
            if (frameTimeMs >= 50.0f) this.criticalSpikesCount++;
            float fpsDrop = Math.max(0.0f, this.lastFrameFps - currentFps);
            this.causeBreakdown.put(cause, this.causeBreakdown.getOrDefault(cause, 0) + 1);
            this.causeTotalFpsDrop.put(cause, this.causeTotalFpsDrop.getOrDefault(cause, 0.0) + fpsDrop);
            LagSpikeRecord record = new LagSpikeRecord(
                    offsetMs, wallClockNow, frameTimeMs, currentFps, this.lastFrameFps,
                    chunksLoaded, loadedChunks, totalEntities, players, mobs, items, armorStands, 0,
                    gcCountDelta > 0, gcTimeDelta, heapUsedMb, packets, chunkPackets, blockPackets, entityPackets,
                    explosions, this.cachedSysCpu, this.cachedJvmCpu, this.cachedSysUsedRamMb, this.cachedSysTotalRamMb,
                    screenName, cause);
            addWorstSpike(record);
        }

        // Фиксация абсолютно каждого кадра со всеми деталями игры и ПК
        FrameRecord frameRecord = new FrameRecord(
                offsetMs, wallClockNow, frameTimeMs, currentFps, this.lastFrameFps, cause,
                chunksLoaded, loadedChunks, totalEntities, players, mobs, items, armorStands, 0,
                gcCountDelta > 0, gcTimeDelta, heapUsedMb, packets, chunkPackets, blockPackets, entityPackets,
                explosions, this.cachedSysCpu, this.cachedJvmCpu, this.cachedSysUsedRamMb, this.cachedSysTotalRamMb,
                screenName);
        synchronized (this.frameRecords) {
            if (this.frameRecords.size() >= MAX_FRAME_RECORDS) {
                this.frameRecords.pollFirst();
            }
            this.frameRecords.addLast(frameRecord);
        }

        this.lastFrameFps = currentFps;
    }

    private synchronized void addWorstSpike(LagSpikeRecord spike) {
        if (this.worstSpikes.size() < 250) {
            this.worstSpikes.add(spike);
            this.worstSpikes.sort((a, b) -> Float.compare(b.frameTimeMs, a.frameTimeMs));
        } else if (spike.frameTimeMs > this.worstSpikes.get(this.worstSpikes.size() - 1).frameTimeMs) {
            this.worstSpikes.remove(this.worstSpikes.size() - 1);
            this.worstSpikes.add(spike);
            this.worstSpikes.sort((a, b) -> Float.compare(b.frameTimeMs, a.frameTimeMs));
        }
    }

    public void onChunkLoad() { if (this.active) this.chunksLoadedThisFrame.incrementAndGet(); }

    public void onPacketReceived(Packet<?> packet) {
        if (!this.active) return;
        this.packetsReceivedThisFrame.incrementAndGet();
        if (packet instanceof ChunkDataS2CPacket) this.chunkPacketsThisFrame.incrementAndGet();
        else if (packet instanceof EntitySpawnS2CPacket) this.entityPacketsThisFrame.incrementAndGet();
        else if (packet instanceof BlockUpdateS2CPacket) this.blockPacketsThisFrame.incrementAndGet();
        else if (packet instanceof ExplosionS2CPacket) this.explosionsThisFrame.incrementAndGet();
    }

    public void onExplosion() { if (this.active) this.explosionsThisFrame.incrementAndGet(); }

    private SessionData calculateSessionData(long sessionDurationNanos, PerformanceReportGenerator.SystemSnapshot systemSnapshot) {
        int samplesCount = Math.min(this.frameIndex, this.frameTimesMs.length);
        float[] validSamples = new float[samplesCount];
        System.arraycopy(this.frameTimesMs, 0, validSamples, 0, samplesCount);
        Arrays.sort(validSamples);
        double avgFps = 0.0, avgFrameTimeMs = 0.0, onePercentLowFps = 0.0, onePercentLowMs = 0.0,
               pointOnePercentLowFps = 0.0, pointOnePercentLowMs = 0.0, minFps = 0.0, maxFps = 0.0,
               varianceMs = 0.0, stabilityPercent = 100.0;
        if (samplesCount > 0) {
            avgFrameTimeMs = (this.totalFrameTimeNanos / (double) samplesCount) / 1_000_000.0;
            avgFps = avgFrameTimeMs > 0 ? 1000.0 / avgFrameTimeMs : 0;
            int idx1 = Math.min(samplesCount - 1, (int) (samplesCount * 0.99));
            onePercentLowMs = validSamples[idx1];
            onePercentLowFps = onePercentLowMs > 0 ? 1000.0 / onePercentLowMs : 0;
            int idx01 = Math.min(samplesCount - 1, (int) (samplesCount * 0.999));
            pointOnePercentLowMs = validSamples[idx01];
            pointOnePercentLowFps = pointOnePercentLowMs > 0 ? 1000.0 / pointOnePercentLowMs : 0;
            minFps = this.maxFrameTimeNanos > 0 ? 1_000_000_000.0 / this.maxFrameTimeNanos : 0;
            maxFps = this.minFrameTimeNanos > 0 ? 1_000_000_000.0 / this.minFrameTimeNanos : 0;
            double sumSq = 0.0;
            for (float s : validSamples) { double d = s - avgFrameTimeMs; sumSq += d * d; }
            varianceMs = Math.sqrt(sumSq / samplesCount);
            if (avgFps > 0) stabilityPercent = Math.min(100.0, Math.max(0.0, (onePercentLowFps / avgFps) * 100.0));
        }
        Map<SpikeCause, Double> causeAvgDropFps = new EnumMap<>(SpikeCause.class);
        for (Map.Entry<SpikeCause, Integer> e : this.causeBreakdown.entrySet()) {
            double totalDrop = this.causeTotalFpsDrop.getOrDefault(e.getKey(), 0.0);
            causeAvgDropFps.put(e.getKey(), e.getValue() > 0 ? totalDrop / e.getValue() : 0.0);
        }
        int totalGcCollections = (int) (MemoryTracker.getTotalGcCount() - this.startGcCount);
        long totalGcTimeMs = MemoryTracker.getTotalGcTimeMs() - this.startGcTimeMs;
        int avgLoadedChunks = this.chunkSampleCount > 0 ? (int) (this.sumLoadedChunks / this.chunkSampleCount) : 0;
        int totalSpikes = this.causeBreakdown.values().stream().mapToInt(Integer::intValue).sum();
        List<FrameRecord> allFramesCopy;
        synchronized (this.frameRecords) {
            allFramesCopy = new ArrayList<>(this.frameRecords);
        }
        return new SessionData(
                systemSnapshot,
                sessionDurationNanos,
                this.sessionStartWallClock,
                this.totalFrames,
                avgFps,
                avgFrameTimeMs,
                onePercentLowFps,
                onePercentLowMs,
                pointOnePercentLowFps,
                pointOnePercentLowMs,
                minFps,
                maxFps,
                this.minFrameTimeNanos / 1_000_000.0,
                this.maxFrameTimeNanos / 1_000_000.0,
                varianceMs,
                stabilityPercent,
                totalSpikes,
                this.criticalSpikesCount,
                this.totalChunkLoads,
                avgLoadedChunks,
                this.totalPacketsReceived,
                totalGcCollections,
                totalGcTimeMs,
                this.maxEntitiesInFrame,
                this.maxMobsInFrame,
                this.maxItemsInFrame,
                this.maxPlayersInFrame,
                this.maxParticlesCount,
                new ArrayList<>(this.worstSpikes),
                new EnumMap<>(this.causeBreakdown),
                causeAvgDropFps,
                allFramesCopy);
    }

    public enum SpikeCause {
        NONE("Норма", "Кадр обработан в штатном режиме"),
        CHUNK_LOADING("Прогрузка чанков / мешей", "Приём новых секций мира, загрузка блоков и перестройка мешей"),
        GC_PAUSE("Пауза сборщика мусора (GC)", "Кратковременная остановка потоков игры (Stop-The-World) для очистки Heap"),
        ENTITIES_DENSITY("Скопление сущностей / дропа", "Большое число мобов, игроков или выпавших предметов в поле зрения"),
        PARTICLE_BURST("Всплеск частиц / взрывы", "Массовый спавн частиц от зелий, ударов булавой, кристаллов или взрывов"),
        NETWORK_BURST("Всплеск сетевых пакетов", "Пиковый приём входящего сетевого трафика от сервера"),
        GUI_OVERHEAD("Нагрузка интерфейса / меню", "Отрисовка открытых экранов инвентаря, сундуков или меню"),
        HIGH_SYSTEM_CPU("Фоновая нагрузка процессора ПК", "Сторонние процессы Windows (браузер, антивирус, запись) загружают CPU"),
        GPU_TERRAIN_STALL("Нагрузка видеокарты (GPU / Shaders)", "Сложная геометрия, шейдеры, тени или пропускная способность видеокарты");

        public final String displayName;
        public final String description;
        SpikeCause(String name, String desc) { this.displayName = name; this.description = desc; }
    }

    public static class LagSpikeRecord {
        public final long timestampMs;
        public final long wallClockTimeMs;
        public final float frameTimeMs;
        public final float instantFps;
        public final float fpsBefore;
        public final int chunksLoaded;
        public final int totalLoadedChunks;
        public final int totalEntities;
        public final int playersCount;
        public final int mobsCount;
        public final int itemsCount;
        public final int armorStandsCount;
        public final int particlesCount;
        public final boolean gcOccurred;
        public final long gcPauseMs;
        public final long usedHeapMb;
        public final int packetsCount;
        public final int chunkPackets;
        public final int blockPackets;
        public final int entityPackets;
        public final int explosionsCount;
        public final float sysCpuPercent;
        public final float jvmCpuPercent;
        public final long sysUsedRamMb;
        public final long sysTotalRamMb;
        public final String screenName;
        public final SpikeCause primaryCause;

        public LagSpikeRecord(long timestampMs, long wallClockTimeMs, float frameTimeMs, float instantFps, float fpsBefore,
                              int chunksLoaded, int totalLoadedChunks, int totalEntities, int playersCount,
                              int mobsCount, int itemsCount, int armorStandsCount, int particlesCount,
                              boolean gcOccurred, long gcPauseMs, long usedHeapMb, int packetsCount, int chunkPackets,
                              int blockPackets, int entityPackets, int explosionsCount, float sysCpuPercent,
                              float jvmCpuPercent, long sysUsedRamMb, long sysTotalRamMb,
                              String screenName, SpikeCause primaryCause) {
            this.timestampMs = timestampMs;
            this.wallClockTimeMs = wallClockTimeMs;
            this.frameTimeMs = frameTimeMs;
            this.instantFps = instantFps;
            this.fpsBefore = fpsBefore;
            this.chunksLoaded = chunksLoaded;
            this.totalLoadedChunks = totalLoadedChunks;
            this.totalEntities = totalEntities;
            this.playersCount = playersCount;
            this.mobsCount = mobsCount;
            this.itemsCount = itemsCount;
            this.armorStandsCount = armorStandsCount;
            this.particlesCount = particlesCount;
            this.gcOccurred = gcOccurred;
            this.gcPauseMs = gcPauseMs;
            this.usedHeapMb = usedHeapMb;
            this.packetsCount = packetsCount;
            this.chunkPackets = chunkPackets;
            this.blockPackets = blockPackets;
            this.entityPackets = entityPackets;
            this.explosionsCount = explosionsCount;
            this.sysCpuPercent = sysCpuPercent;
            this.jvmCpuPercent = jvmCpuPercent;
            this.sysUsedRamMb = sysUsedRamMb;
            this.sysTotalRamMb = sysTotalRamMb;
            this.screenName = screenName;
            this.primaryCause = primaryCause;
        }
    }

    public static class FrameRecord {
        public final long timestampMs;
        public final long wallClockTimeMs;
        public final float frameTimeMs;
        public final float instantFps;
        public final float fpsBefore;
        public final SpikeCause cause;
        public final int chunksLoaded;
        public final int totalLoadedChunks;
        public final int totalEntities;
        public final int playersCount;
        public final int mobsCount;
        public final int itemsCount;
        public final int armorStandsCount;
        public final int particlesCount;
        public final boolean gcOccurred;
        public final long gcPauseMs;
        public final long usedHeapMb;
        public final int packetsCount;
        public final int chunkPackets;
        public final int blockPackets;
        public final int entityPackets;
        public final int explosionsCount;
        public final float sysCpuPercent;
        public final float jvmCpuPercent;
        public final long sysUsedRamMb;
        public final long sysTotalRamMb;
        public final String screenName;

        public FrameRecord(long timestampMs, long wallClockTimeMs, float frameTimeMs, float instantFps, float fpsBefore,
                           SpikeCause cause, int chunksLoaded, int totalLoadedChunks, int totalEntities,
                           int playersCount, int mobsCount, int itemsCount, int armorStandsCount, int particlesCount,
                           boolean gcOccurred, long gcPauseMs, long usedHeapMb, int packetsCount, int chunkPackets,
                           int blockPackets, int entityPackets, int explosionsCount, float sysCpuPercent,
                           float jvmCpuPercent, long sysUsedRamMb, long sysTotalRamMb, String screenName) {
            this.timestampMs = timestampMs;
            this.wallClockTimeMs = wallClockTimeMs;
            this.frameTimeMs = frameTimeMs;
            this.instantFps = instantFps;
            this.fpsBefore = fpsBefore;
            this.cause = cause;
            this.chunksLoaded = chunksLoaded;
            this.totalLoadedChunks = totalLoadedChunks;
            this.totalEntities = totalEntities;
            this.playersCount = playersCount;
            this.mobsCount = mobsCount;
            this.itemsCount = itemsCount;
            this.armorStandsCount = armorStandsCount;
            this.particlesCount = particlesCount;
            this.gcOccurred = gcOccurred;
            this.gcPauseMs = gcPauseMs;
            this.usedHeapMb = usedHeapMb;
            this.packetsCount = packetsCount;
            this.chunkPackets = chunkPackets;
            this.blockPackets = blockPackets;
            this.entityPackets = entityPackets;
            this.explosionsCount = explosionsCount;
            this.sysCpuPercent = sysCpuPercent;
            this.jvmCpuPercent = jvmCpuPercent;
            this.sysUsedRamMb = sysUsedRamMb;
            this.sysTotalRamMb = sysTotalRamMb;
            this.screenName = screenName;
        }
    }

    public static class SessionData {
        public final PerformanceReportGenerator.SystemSnapshot systemSnapshot;
        public final long durationNanos;
        public final long sessionStartWallClock;
        public final int totalFrames;
        public final double avgFps;
        public final double avgFrameTimeMs;
        public final double onePercentLowFps;
        public final double onePercentLowMs;
        public final double pointOnePercentLowFps;
        public final double pointOnePercentLowMs;
        public final double minFps;
        public final double maxFps;
        public final double minFrameTimeMs;
        public final double maxFrameTimeMs;
        public final double frameTimeVarianceMs;
        public final double stabilityPercent;
        public final int totalSpikes;
        public final int criticalSpikes;
        public final int totalChunkLoads;
        public final int avgLoadedChunks;
        public final int totalPacketsReceived;
        public final int gcCollectionCount;
        public final long totalGcTimeMs;
        public final int maxEntitiesInFrame;
        public final int maxMobsInFrame;
        public final int maxItemsInFrame;
        public final int maxPlayersInFrame;
        public final int maxParticlesCount;
        public final List<LagSpikeRecord> worstSpikes;
        public final Map<SpikeCause, Integer> causeBreakdown;
        public final Map<SpikeCause, Double> causeAvgDropFps;
        public final List<FrameRecord> allFrames;

        public SessionData(PerformanceReportGenerator.SystemSnapshot systemSnapshot,
                           long durationNanos, long sessionStartWallClock, int totalFrames, double avgFps, double avgFrameTimeMs,
                           double onePercentLowFps, double onePercentLowMs, double pointOnePercentLowFps,
                           double pointOnePercentLowMs, double minFps, double maxFps, double minFrameTimeMs,
                           double maxFrameTimeMs, double frameTimeVarianceMs, double stabilityPercent,
                           int totalSpikes, int criticalSpikes, int totalChunkLoads, int avgLoadedChunks,
                           int totalPacketsReceived, int gcCollectionCount, long totalGcTimeMs,
                           int maxEntitiesInFrame, int maxMobsInFrame, int maxItemsInFrame,
                           int maxPlayersInFrame, int maxParticlesCount, List<LagSpikeRecord> worstSpikes,
                           Map<SpikeCause, Integer> causeBreakdown, Map<SpikeCause, Double> causeAvgDropFps,
                           List<FrameRecord> allFrames) {
            this.systemSnapshot = systemSnapshot;
            this.durationNanos = durationNanos;
            this.sessionStartWallClock = sessionStartWallClock;
            this.totalFrames = totalFrames;
            this.avgFps = avgFps;
            this.avgFrameTimeMs = avgFrameTimeMs;
            this.onePercentLowFps = onePercentLowFps;
            this.onePercentLowMs = onePercentLowMs;
            this.pointOnePercentLowFps = pointOnePercentLowFps;
            this.pointOnePercentLowMs = pointOnePercentLowMs;
            this.minFps = minFps;
            this.maxFps = maxFps;
            this.minFrameTimeMs = minFrameTimeMs;
            this.maxFrameTimeMs = maxFrameTimeMs;
            this.frameTimeVarianceMs = frameTimeVarianceMs;
            this.stabilityPercent = stabilityPercent;
            this.totalSpikes = totalSpikes;
            this.criticalSpikes = criticalSpikes;
            this.totalChunkLoads = totalChunkLoads;
            this.avgLoadedChunks = avgLoadedChunks;
            this.totalPacketsReceived = totalPacketsReceived;
            this.gcCollectionCount = gcCollectionCount;
            this.totalGcTimeMs = totalGcTimeMs;
            this.maxEntitiesInFrame = maxEntitiesInFrame;
            this.maxMobsInFrame = maxMobsInFrame;
            this.maxItemsInFrame = maxItemsInFrame;
            this.maxPlayersInFrame = maxPlayersInFrame;
            this.maxParticlesCount = maxParticlesCount;
            this.worstSpikes = worstSpikes;
            this.causeBreakdown = causeBreakdown;
            this.causeAvgDropFps = causeAvgDropFps;
            this.allFrames = allFrames;
        }
    }
}
