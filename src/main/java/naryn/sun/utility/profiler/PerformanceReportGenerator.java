package naryn.sun.utility.profiler;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import naryn.sun.Sun;
import naryn.sun.systems.file.FileManager;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.notifications.NotificationType;
import naryn.sun.systems.setting.Setting;
import naryn.sun.utility.interfaces.IMinecraft;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Util;
import org.lwjgl.opengl.GL11;

/**
 * Генератор детального аналитического отчёта и покадровых логов производительности.
 */
public final class PerformanceReportGenerator implements IMinecraft {

    private PerformanceReportGenerator() {
    }

    public static class ModuleSnapshot {
        public final String name;
        public final String category;
        public final Map<String, String> settings;

        public ModuleSnapshot(String name, String category, Map<String, String> settings) {
            this.name = name;
            this.category = category;
            this.settings = settings;
        }
    }

    /**
     * Снимок системных параметров, параметров мира, сервера и активных модулей клиента.
     */
    public static class SystemSnapshot {
        public final String os;
        public final int cores;
        public final String javaVer;
        public final String gpuVendor;
        public final String gpuRenderer;
        public final String glVersion;
        public final long usedMemMb;
        public final long totalMemMb;
        public final long maxMemMb;
        public final String gcNames;
        public final long sysTotalRamMb;
        public final long sysUsedRamMb;
        public final float sysCpuPercent;
        public final float jvmCpuPercent;
        public final int jvmThreadCount;
        public final List<SystemProcessTracker.ProcessSnapshot> runningProcesses;
        public final int windowWidth;
        public final int windowHeight;
        public final String guiScale;
        public final int renderDistance;
        public final int simDistance;
        public final String maxFps;
        public final boolean vsync;
        public final String graphics;

        // Сервер, мир и игрок
        public final String serverAddress;
        public final int serverPing;
        public final float serverTps;
        public final String dimension;
        public final String biome;
        public final String playerPos;
        public final List<String> activeEffects;
        public final int renderedChunks;
        public final int loadedChunksCount;
        public final List<String> loadedOptimizationMods;
        public final List<ModuleSnapshot> activeModules;

        public SystemSnapshot(String os, int cores, String javaVer, String gpuVendor, String gpuRenderer,
                              String glVersion, long usedMemMb, long totalMemMb, long maxMemMb, String gcNames,
                              long sysTotalRamMb, long sysUsedRamMb, float sysCpuPercent, float jvmCpuPercent,
                              int jvmThreadCount, List<SystemProcessTracker.ProcessSnapshot> runningProcesses,
                              int windowWidth, int windowHeight, String guiScale, int renderDistance,
                              int simDistance, String maxFps, boolean vsync, String graphics,
                              String serverAddress, int serverPing, float serverTps, String dimension,
                              String biome, String playerPos, List<String> activeEffects, int renderedChunks,
                              int loadedChunksCount, List<String> loadedOptimizationMods,
                              List<ModuleSnapshot> activeModules) {
            this.os = os;
            this.cores = cores;
            this.javaVer = javaVer;
            this.gpuVendor = gpuVendor;
            this.gpuRenderer = gpuRenderer;
            this.glVersion = glVersion;
            this.usedMemMb = usedMemMb;
            this.totalMemMb = totalMemMb;
            this.maxMemMb = maxMemMb;
            this.gcNames = gcNames;
            this.sysTotalRamMb = sysTotalRamMb;
            this.sysUsedRamMb = sysUsedRamMb;
            this.sysCpuPercent = sysCpuPercent;
            this.jvmCpuPercent = jvmCpuPercent;
            this.jvmThreadCount = jvmThreadCount;
            this.runningProcesses = runningProcesses;
            this.windowWidth = windowWidth;
            this.windowHeight = windowHeight;
            this.guiScale = guiScale;
            this.renderDistance = renderDistance;
            this.simDistance = simDistance;
            this.maxFps = maxFps;
            this.vsync = vsync;
            this.graphics = graphics;
            this.serverAddress = serverAddress;
            this.serverPing = serverPing;
            this.serverTps = serverTps;
            this.dimension = dimension;
            this.biome = biome;
            this.playerPos = playerPos;
            this.activeEffects = activeEffects;
            this.renderedChunks = renderedChunks;
            this.loadedChunksCount = loadedChunksCount;
            this.loadedOptimizationMods = loadedOptimizationMods;
            this.activeModules = activeModules;
        }

        public static SystemSnapshot capture() {
            String os = System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")";
            int cores = Runtime.getRuntime().availableProcessors();
            String javaVer = System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")";

            String gpuVendor = "Unknown";
            String gpuRenderer = "Unknown";
            String glVersion = "Unknown";
            try {
                gpuVendor = GL11.glGetString(GL11.GL_VENDOR);
                gpuRenderer = GL11.glGetString(GL11.GL_RENDERER);
                glVersion = GL11.glGetString(GL11.GL_VERSION);
            } catch (Throwable ignored) {
            }

            long maxMem = MemoryTracker.getMaxMemoryMb();
            long totalMem = MemoryTracker.getTotalMemoryMb();
            long usedMem = MemoryTracker.getUsedMemoryMb();
            String gcNames = MemoryTracker.getGcNames();

            long sysTotalRam = SystemProcessTracker.getSystemTotalMemoryMb();
            long sysUsedRam = SystemProcessTracker.getSystemUsedMemoryMb();
            float sysCpu = SystemProcessTracker.getSystemCpuLoad();
            float jvmCpu = SystemProcessTracker.getProcessCpuLoad();
            int threads = SystemProcessTracker.getJvmThreadCount();
            List<SystemProcessTracker.ProcessSnapshot> processes = SystemProcessTracker.getRunningProcesses();

            int windowWidth = 1920;
            int windowHeight = 1080;
            String guiScale = "Auto";
            int renderDistance = 12;
            int simDistance = 12;
            String maxFps = "Без ограничений";
            boolean vsync = false;
            String graphics = "Fancy";

            String serverAddress = "Одиночная игра / Локальный мир";
            int serverPing = 0;
            float serverTps = 20.0f;
            String dimension = "minecraft:overworld";
            String biome = "Unknown";
            String playerPos = "Неизвестно";
            List<String> activeEffects = new ArrayList<>();
            int renderedChunks = 0;
            int loadedChunksCount = 0;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) {
                try {
                    if (mc.getWindow() != null) {
                        windowWidth = mc.getWindow().getWidth();
                        windowHeight = mc.getWindow().getHeight();
                    }
                    if (mc.options != null) {
                        renderDistance = mc.options.getViewDistance().getValue();
                        simDistance = mc.options.getSimulationDistance().getValue();
                        int fps = mc.options.getMaxFps().getValue();
                        maxFps = fps >= 260 ? "Без ограничений" : String.valueOf(fps);
                        vsync = mc.options.getEnableVsync().getValue();
                        graphics = mc.options.getGraphicsMode().getValue().name();
                        guiScale = String.valueOf(mc.options.getGuiScale().getValue());
                    }
                    if (mc.getCurrentServerEntry() != null) {
                        serverAddress = mc.getCurrentServerEntry().address;
                    }
                    if (mc.getNetworkHandler() != null && mc.player != null) {
                        var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                        if (entry != null) {
                            serverPing = entry.getLatency();
                        }
                    }
                    if (Sun.getInstance().getTpsHandler() != null) {
                        serverTps = Sun.getInstance().getTpsHandler().getTPS();
                    }
                    if (mc.world != null) {
                        dimension = mc.world.getRegistryKey().getValue().toString();
                        if (mc.world.getChunkManager() != null) {
                            loadedChunksCount = mc.world.getChunkManager().getLoadedChunkCount();
                        }
                    }
                    if (mc.worldRenderer != null) {
                        renderedChunks = mc.worldRenderer.getCompletedChunkCount();
                    }
                    if (mc.player != null) {
                        playerPos = String.format("X: %.1f, Y: %.1f, Z: %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
                        if (mc.world != null) {
                            biome = mc.world.getBiome(mc.player.getBlockPos()).getKey().map(k -> k.getValue().toString()).orElse("Unknown");
                        }
                        for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
                            String effectName = effect.getEffectType().getIdAsString();
                            int durationSec = effect.getDuration() / 20;
                            int amp = effect.getAmplifier() + 1;
                            activeEffects.add(String.format("%s %d (%02d:%02d)", effectName, amp, durationSec / 60, durationSec % 60));
                        }
                    }
                } catch (Throwable ignored) {
                }
            }

            // Список оптимизационных модов
            List<String> loadedMods = new ArrayList<>();
            try {
                for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
                    String id = mod.getMetadata().getId();
                    if (id.equals("sodium") || id.equals("iris") || id.equals("lithium") ||
                        id.equals("ferritecore") || id.equals("immediatelyfast") || id.equals("entityculling") ||
                        id.equals("krypton") || id.equals("memoryleakfix") || id.equals("modmenu")) {
                        loadedMods.add(mod.getMetadata().getName() + " (" + mod.getMetadata().getVersion().getFriendlyString() + ")");
                    }
                }
            } catch (Throwable ignored) {
            }

            // Список активных модулей SUN и их настроек
            List<ModuleSnapshot> activeModules = new ArrayList<>();
            try {
                if (Sun.getInstance().getModuleManager() != null) {
                    for (naryn.sun.systems.modules.Module module : Sun.getInstance().getModuleManager().getActiveModules()) {
                        Map<String, String> settingsMap = new LinkedHashMap<>();
                        for (Setting setting : module.getSettings()) {
                            try {
                                com.google.gson.JsonElement saved = setting.save();
                                String val = saved != null ? (saved.isJsonPrimitive() ? saved.getAsString() : saved.toString()) : "";
                                settingsMap.put(setting.getName(), val);
                            } catch (Throwable ignored) {
                            }
                        }
                        activeModules.add(new ModuleSnapshot(
                                module.getName(),
                                module.getCategory() != null ? module.getCategory().name() : "OTHER",
                                settingsMap
                        ));
                    }
                }
            } catch (Throwable ignored) {
            }

            return new SystemSnapshot(os, cores, javaVer, gpuVendor, gpuRenderer, glVersion,
                    usedMem, totalMem, maxMem, gcNames, sysTotalRam, sysUsedRam, sysCpu, jvmCpu, threads,
                    processes, windowWidth, windowHeight, guiScale, renderDistance, simDistance, maxFps, vsync, graphics,
                    serverAddress, serverPing, serverTps, dimension, biome, playerPos, activeEffects,
                    renderedChunks, loadedChunksCount, loadedMods, activeModules);
        }
    }

    public static void generateAndSaveReport(PerformanceProfiler.SessionData session, float spikeThresholdMs, boolean autoOpen, File targetDir) {
        new Thread(() -> {
            try {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                String fileName = "SUN_Performance_Log_" + timestamp + ".txt";

                File chosenDir = targetDir;
                if (chosenDir != null && !chosenDir.exists()) {
                    try {
                        chosenDir.mkdirs();
                    } catch (Throwable ignored) {
                    }
                }

                if (chosenDir == null || !chosenDir.exists() || !chosenDir.isDirectory() || !chosenDir.canWrite()) {
                    File desktopDir = new File(System.getProperty("user.home"), "Desktop");
                    if (!desktopDir.exists() || !desktopDir.isDirectory() || !desktopDir.canWrite()) {
                        File oneDriveDesktop = new File(System.getProperty("user.home"), "OneDrive" + File.separator + "Desktop");
                        if (oneDriveDesktop.exists() && oneDriveDesktop.isDirectory() && oneDriveDesktop.canWrite()) {
                            chosenDir = oneDriveDesktop;
                        } else {
                            File sunReports = new File(FileManager.DIRECTORY, "reports");
                            if (!sunReports.exists()) sunReports.mkdirs();
                            chosenDir = sunReports;
                        }
                    } else {
                        chosenDir = desktopDir;
                    }
                }

                File reportFile = new File(chosenDir, fileName);
                String content = buildReportText(session, spikeThresholdMs);

                try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile, StandardCharsets.UTF_8))) {
                    writer.print(content);
                }

                // Резервная копия в Sun/reports
                try {
                    File backupDir = new File(FileManager.DIRECTORY, "reports");
                    if (!backupDir.exists()) backupDir.mkdirs();
                    File backupFile = new File(backupDir, fileName);
                    if (!backupFile.getAbsolutePath().equals(reportFile.getAbsolutePath())) {
                        try (PrintWriter writer = new PrintWriter(new FileWriter(backupFile, StandardCharsets.UTF_8))) {
                            writer.print(content);
                        }
                    }
                } catch (Throwable ignored) {
                }

                Sun.LOGGER.info("Performance report successfully saved to: {}", reportFile.getAbsolutePath());

                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) {
                    mc.execute(() -> {
                        try {
                            Sun.getInstance().getNotificationManager().addNotification(
                                    NotificationType.SUCCESS,
                                    "Лог сохранен: " + reportFile.getName()
                            );
                        } catch (Throwable ignored) {
                        }
                    });
                }

                if (autoOpen && reportFile.exists()) {
                    try {
                        Util.getOperatingSystem().open(reportFile);
                    } catch (Throwable t) {
                        Sun.LOGGER.warn("Failed to auto-open report file", t);
                    }
                }

            } catch (Exception e) {
                Sun.LOGGER.error("Failed to generate performance report", e);
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) {
                    mc.execute(() -> {
                        try {
                            Sun.getInstance().getNotificationManager().addNotification(
                                    NotificationType.ERROR,
                                    "Ошибка при сохранении отчета!"
                            );
                        } catch (Throwable ignored) {
                        }
                    });
                }
            }
        }, "SUN-Report-Writer").start();
    }

    private static String buildReportText(PerformanceProfiler.SessionData session, float spikeThresholdMs) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        SimpleDateFormat timeOnlyDf = new SimpleDateFormat("HH:mm:ss.SSS");

        double sessionDurationSec = session.durationNanos / 1_000_000_000.0;
        int minutes = (int) (sessionDurationSec / 60);
        double seconds = sessionDurationSec % 60;

        sb.append("========================================================================================================================\n");
        sb.append("🔍 SUN CLIENT 2.0 — ПОЛНЫЙ ДИАГНОСТИЧЕСКИЙ ЛОГ ПРОИЗВОДИТЕЛЬНОСТИ, МОДУЛЕЙ И СИСТЕМЫ ПК\n");
        sb.append("========================================================================================================================\n");
        sb.append(String.format("Время старта записи:        %s\n", df.format(new Date(session.sessionStartWallClock))));
        sb.append(String.format("Время окончания записи:     %s\n", df.format(new Date())));
        sb.append(String.format("Длительность сессии:        %02d:%05.2f (%d сек)\n", minutes, seconds, (long) sessionDurationSec));
        sb.append(String.format("Всего зафиксировано кадров: %,d\n", session.totalFrames));
        sb.append(String.format("Порог фиксации микрофризов: %.1f ms (< %.0f FPS)\n\n", spikeThresholdMs, 1000.0 / spikeThresholdMs));

        SystemSnapshot sys = session.systemSnapshot;

        // 1. ХАРАКТЕРИСТИКИ ПК И НАСТРОЙКИ
        sb.append("========================================================================================================================\n");
        sb.append("💻 1. ХАРАКТЕРИСТИКИ ПК, НАСТРОЙКИ КЛИЕНТА И СОСТОЯНИЕ РЕСУРСОВ\n");
        sb.append("========================================================================================================================\n");
        if (sys != null) {
            sb.append(String.format("• Операционная система:     %s\n", sys.os));
            sb.append(String.format("• Процессор (CPU):          %d логических ядер (Нагрузка системы: %.1f%%, Minecraft: %.1f%%)\n",
                    sys.cores, sys.sysCpuPercent, sys.jvmCpuPercent));
            sb.append(String.format("• Видеокарта (GPU):         %s (%s)\n", sys.gpuRenderer, sys.gpuVendor));
            sb.append(String.format("• Драйвер OpenGL:           %s\n", sys.glVersion));
            sb.append(String.format("• Версия Java (JVM):        %s | Активных потоков: %d\n", sys.javaVer, sys.jvmThreadCount));
            sb.append(String.format("• Память Heap игры:         %d MB занято / %d MB выделено (Макс: %d MB)\n", sys.usedMemMb, sys.totalMemMb, sys.maxMemMb));
            sb.append(String.format("• Оперативная память ПК:    %d MB занято / %d MB всего (Загрузка RAM: %.1f%%)\n",
                    sys.sysUsedRamMb, sys.sysTotalRamMb, sys.sysTotalRamMb > 0 ? (sys.sysUsedRamMb * 100.0 / sys.sysTotalRamMb) : 0));
            sb.append(String.format("• Сборщик мусора (GC):      %s\n", sys.gcNames));
            sb.append(String.format("• Разрешение экрана:        %dx%d (Интерфейс: %s)\n", sys.windowWidth, sys.windowHeight, sys.guiScale));
            sb.append(String.format("• Дистанция прорисовки:     %d чанков | Симуляция: %d чанков\n", sys.renderDistance, sys.simDistance));
            sb.append(String.format("• Режим графики:            %s | VSync: %s | Лимит FPS: %s\n",
                    sys.graphics, sys.vsync ? "ВКЛ" : "ВЫКЛ", sys.maxFps));
            if (!sys.loadedOptimizationMods.isEmpty()) {
                sb.append(String.format("• Установленные моды:       %s\n", String.join(", ", sys.loadedOptimizationMods)));
            }
        }
        sb.append("\n");

        // 2. СЕРВЕР, МИР И ПОЗИЦИЯ ИГРОКА
        sb.append("========================================================================================================================\n");
        sb.append("🌐 2. СЕРВЕР, ИГРОВОЙ МИР И ПОЛОЖЕНИЕ ИГРОКА\n");
        sb.append("========================================================================================================================\n");
        if (sys != null) {
            sb.append(String.format("• Сервер / Режим:           %s\n", sys.serverAddress));
            sb.append(String.format("• Пинг до сервера:          %d ms | TPS сервера: %.1f\n", sys.serverPing, sys.serverTps));
            sb.append(String.format("• Измерение и биом:         %s | %s\n", sys.dimension, sys.biome));
            sb.append(String.format("• Координаты игрока:        %s\n", sys.playerPos));
            sb.append(String.format("• Чанки в памяти / в кадре: Загружено: %d | Отрисовано: %d\n", sys.loadedChunksCount, sys.renderedChunks));
            sb.append(String.format("• Активные эффекты зелий:   %s\n", sys.activeEffects.isEmpty() ? "Нет" : String.join(", ", sys.activeEffects)));
        }
        sb.append("\n");

        // 3. АКТИВНЫЕ МОДУЛИ SUN И ИХ НАСТРОЙКИ
        sb.append("========================================================================================================================\n");
        sb.append("🧩 3. ВКЛЮЧЕННЫЕ МОДУЛИ SUN CLIENT И ИХ ПАРАМЕТРЫ ВО ВРЕМЯ ЗАПИСИ\n");
        sb.append("========================================================================================================================\n");
        if (sys != null && sys.activeModules != null && !sys.activeModules.isEmpty()) {
            sb.append(String.format("Всего активно модулей: %d\n\n", sys.activeModules.size()));
            for (ModuleSnapshot mod : sys.activeModules) {
                sb.append(String.format("• [%s] %s (%s):\n", mod.category, mod.name, mod.settings.isEmpty() ? "дефолтные параметры" : "пользовательские параметры"));
                if (!mod.settings.isEmpty()) {
                    List<String> settingLines = new ArrayList<>();
                    for (Map.Entry<String, String> s : mod.settings.entrySet()) {
                        String shortKey = s.getKey().replace("modules.settings.", "");
                        settingLines.add(String.format("%s = %s", shortKey, s.getValue()));
                    }
                    sb.append("     └─ ").append(String.join(" | ", settingLines)).append("\n");
                }
            }
            sb.append("\n");
        } else {
            sb.append("Все сторонние модули были выключены.\n\n");
        }

        // 4. АКТИВНЫЕ ФОНОВЫЕ ПРОЦЕССЫ НА ПК
        sb.append("========================================================================================================================\n");
        sb.append("🖥️ 4. АКТИВНЫЕ ПРОЦЕССЫ И ПРОГРАММЫ НА ПК (TOP CPU CONSUMERS В WINDOWS)\n");
        sb.append("========================================================================================================================\n");
        if (sys != null && sys.runningProcesses != null && !sys.runningProcesses.isEmpty()) {
            sb.append("Список запущенных процессов ОС для выявления внешних источников лагов (браузеры, Discord, антивирус, запись):\n\n");
            sb.append(String.format("%-10s | %-32s | %-16s | %s\n", "PID", "ПРОЦЕСС", "CPU TIME (SEC)", "КОМАНДА / ПУТЬ"));
            sb.append("------------------------------------------------------------------------------------------------------------------------\n");
            int count = 0;
            for (SystemProcessTracker.ProcessSnapshot proc : sys.runningProcesses) {
                if (count++ >= 50) break;
                double cpuSec = proc.cpuMillis / 1000.0;
                String cmd = proc.commandLine;
                if (cmd.length() > 60) cmd = cmd.substring(0, 57) + "...";
                sb.append(String.format("%-10d | %-32s | %13.2f с | %s\n", proc.pid, proc.name, cpuSec, cmd));
            }
            sb.append("\n");
        } else {
            sb.append("Информация о фоновых процессах ОС недоступна.\n\n");
        }

        // 5. ИТОГОВАЯ СВОДКА FPS
        sb.append("========================================================================================================================\n");
        sb.append("📊 5. ИТОГОВАЯ СВОДКА FPS И СТАБИЛЬНОСТИ\n");
        sb.append("========================================================================================================================\n");
        sb.append(String.format("• Средний FPS:                %6.1f FPS (время кадра: %.2f ms)\n", session.avgFps, session.avgFrameTimeMs));
        sb.append(String.format("• 1%% Low FPS (Редкие лаги):    %6.1f FPS (время кадра: %.2f ms)\n", session.onePercentLowFps, session.onePercentLowMs));
        sb.append(String.format("• 0.1%% Low FPS (Микрофризы):  %6.1f FPS (время кадра: %.2f ms)\n", session.pointOnePercentLowFps, session.pointOnePercentLowMs));
        sb.append(String.format("• Минимальный зафиксированный: %6.1f FPS (кадр: %.2f ms)\n", session.minFps, session.maxFrameTimeMs));
        sb.append(String.format("• Максимальный FPS:           %6.1f FPS (кадр: %.2f ms)\n", session.maxFps, session.minFrameTimeMs));
        sb.append(String.format("• Дисперсия фреймтайма (Jitter): %.2f ms\n", session.frameTimeVarianceMs));
        sb.append(String.format("• Индекс стабильности FPS:    %.1f%%\n", session.stabilityPercent));
        sb.append(String.format("• Всего лаг-спайков:          %d (из них критических >50ms: %d)\n\n", session.totalSpikes, session.criticalSpikes));

        // 6. ПОСЕКУНДНАЯ ДИАГНОСТИЧЕСКАЯ ХРОНОЛОГИЯ
        sb.append("========================================================================================================================\n");
        sb.append("⏱️ 6. ПОСЕКУНДНАЯ ХРОНОЛОГИЯ СОСТОЯНИЯ ИГРЫ И НАГРУЗКИ ПК (SECOND-BY-SECOND LOG)\n");
        sb.append("========================================================================================================================\n");
        buildSecondBySecondTimeline(sb, session);
        sb.append("\n");

        // 7. ДЕТАЛЬНЫЙ ЖУРНАЛ ВСЕХ МИКРОФРИЗОВ И ПРОСАДОК FPS
        sb.append("========================================================================================================================\n");
        sb.append("⚠️ 7. ХРОНОЛОГИЧЕСКИЙ ЖУРНАЛ ВСЕХ ПРОСАДОК FPS И МИКРОФРИЗОВ (INCIDENT TIMELINE)\n");
        sb.append("========================================================================================================================\n");
        buildSpikeIncidentsLog(sb, session, timeOnlyDf);
        sb.append("\n");

        // 8. ПОЛНЫЙ ПОКАДРОВЫЙ ЛОГ КАЖДОГО КАДРА
        sb.append("========================================================================================================================\n");
        sb.append("📋 8. ПОЛНЫЙ ПОКАДРОВЫЙ ЛОГ (МИЛЛИСЕКУНДНАЯ ТАБЛИЦА КАЖДОГО КАДРА СЕССИИ)\n");
        sb.append("========================================================================================================================\n");
        buildPerFrameLogTable(sb, session, timeOnlyDf);
        sb.append("\n");

        // 9. РЕЙТИНГ ПЕРВОПРИЧИН И РЕКОМЕНДАЦИИ
        sb.append("========================================================================================================================\n");
        sb.append("🎯 9. РЕЙТИНГ ПЕРВОПРИЧИН И РЕКОМЕНДАЦИИ ПО УСТРАНЕНИЮ ЛАГОВ\n");
        sb.append("========================================================================================================================\n");
        for (Map.Entry<PerformanceProfiler.SpikeCause, Integer> entry : session.causeBreakdown.entrySet()) {
            PerformanceProfiler.SpikeCause cause = entry.getKey();
            if (cause == PerformanceProfiler.SpikeCause.NONE) continue;
            int count = entry.getValue();
            double percent = (count * 100.0) / Math.max(1, session.totalSpikes);
            double avgDrop = session.causeAvgDropFps.getOrDefault(cause, 0.0);
            sb.append(String.format(" [%5.1f%%] %s: %d инцидентов (Средняя потеря: -%.1f FPS)\n",
                    percent, cause.displayName, count, avgDrop));
            sb.append(String.format("         -> Механизм: %s\n", cause.description));
        }
        sb.append("\n");

        List<String> recs = generateRecommendations(session);
        for (int i = 0; i < recs.size(); i++) {
            sb.append(String.format("%d. %s\n\n", i + 1, recs.get(i)));
        }

        sb.append("========================================================================================================================\n");
        sb.append("Отчёт сгенерирован встроенным модулем Profiler (SUN Client 2.0)\n");
        sb.append("========================================================================================================================\n");

        return sb.toString();
    }

    private static void buildSecondBySecondTimeline(StringBuilder sb, PerformanceProfiler.SessionData session) {
        if (session.allFrames.isEmpty()) {
            sb.append("Нет данных о кадрах.\n");
            return;
        }

        Map<Long, List<PerformanceProfiler.FrameRecord>> secondBuckets = new TreeMap<>();
        for (PerformanceProfiler.FrameRecord f : session.allFrames) {
            long sec = f.timestampMs / 1000L;
            secondBuckets.computeIfAbsent(sec, k -> new ArrayList<>()).add(f);
        }

        for (Map.Entry<Long, List<PerformanceProfiler.FrameRecord>> entry : secondBuckets.entrySet()) {
            long sec = entry.getKey();
            List<PerformanceProfiler.FrameRecord> frames = entry.getValue();
            int frameCount = frames.size();
            if (frameCount == 0) continue;

            float minFps = Float.MAX_VALUE;
            float maxFps = 0.0f;
            float sumFrameTime = 0.0f;
            int totalChunks = 0;
            int totalPackets = 0;
            int maxEntities = 0;
            long totalGcPause = 0;
            float lastSysCpu = 0;
            float lastJvmCpu = 0;
            long lastRamUsed = 0;
            long lastRamTotal = 0;
            PerformanceProfiler.FrameRecord worstFrame = null;

            for (PerformanceProfiler.FrameRecord f : frames) {
                sumFrameTime += f.frameTimeMs;
                if (f.instantFps < minFps) minFps = f.instantFps;
                if (f.instantFps > maxFps) maxFps = f.instantFps;
                totalChunks += f.chunksLoaded;
                totalPackets += f.packetsCount;
                maxEntities = Math.max(maxEntities, f.totalEntities);
                if (f.gcOccurred) {
                    totalGcPause += f.gcPauseMs;
                }
                lastSysCpu = f.sysCpuPercent;
                lastJvmCpu = f.jvmCpuPercent;
                lastRamUsed = f.sysUsedRamMb;
                lastRamTotal = f.sysTotalRamMb;
                if (worstFrame == null || f.frameTimeMs > worstFrame.frameTimeMs) {
                    worstFrame = f;
                }
            }

            float avgFps = sumFrameTime > 0 ? (frameCount * 1000.0f / sumFrameTime) : 0;
            int min = (int) (sec / 60);
            long s = sec % 60;

            String freezeWarning = "";
            if (worstFrame != null && worstFrame.frameTimeMs >= 40.0f) {
                freezeWarning = String.format(" 🔴 [МИКРОФРИЗ: %.0f->%.0f FPS (кадр %.1f ms) - %s]",
                        worstFrame.fpsBefore, worstFrame.instantFps, worstFrame.frameTimeMs, worstFrame.cause.displayName);
            } else if (worstFrame != null && worstFrame.frameTimeMs >= 25.0f) {
                freezeWarning = String.format(" 🟡 [ПРОСАДКА: %.0f->%.0f FPS (кадр %.1f ms) - %s]",
                        worstFrame.fpsBefore, worstFrame.instantFps, worstFrame.frameTimeMs, worstFrame.cause.displayName);
            }

            sb.append(String.format("[%02d:%02d] | Ср. FPS: %5.1f (Мин: %5.1f | Макс: %5.1f) | Кадров: %3d | CPU ПК: %4.1f%% (Игра: %4.1f%%) | RAM ПК: %d/%d MB\n",
                    min, s, avgFps, minFps, maxFps, frameCount, lastSysCpu, lastJvmCpu, lastRamUsed, lastRamTotal));
            sb.append(String.format("        -> События: Чанков: %d | Пакетов: %d | Сущностей: %d | GC Пауза: %d ms%s\n",
                    totalChunks, totalPackets, maxEntities, totalGcPause, freezeWarning));
        }
    }

    private static void buildSpikeIncidentsLog(StringBuilder sb, PerformanceProfiler.SessionData session, SimpleDateFormat timeDf) {
        List<PerformanceProfiler.LagSpikeRecord> sortedSpikes = new ArrayList<>(session.worstSpikes);
        sortedSpikes.sort(Comparator.comparingLong(a -> a.timestampMs));

        if (sortedSpikes.isEmpty()) {
            sb.append("За время сессии критических микрофризов не зафиксировано.\n");
            return;
        }

        int index = 1;
        for (PerformanceProfiler.LagSpikeRecord spike : sortedSpikes) {
            long offsetMs = spike.timestampMs;
            int min = (int) (offsetMs / 60000);
            double sec = (offsetMs % 60000) / 1000.0;
            String clockTime = timeDf.format(new Date(spike.wallClockTimeMs));

            String severity;
            if (spike.frameTimeMs >= 100.0f) severity = "🔴 КРИТИЧЕСКИЙ ФРИЗ (>100ms / <10 FPS)";
            else if (spike.frameTimeMs >= 50.0f) severity = "🟠 ТЯЖЁЛЫЙ ЛАГ (>50ms / <20 FPS)";
            else severity = "🟡 МИКРОФРИЗ (>30ms)";

            sb.append(String.format("#%03d [%s | +%02d:%05.2f] %s: %.0f FPS -> %.0f FPS (кадр: %.1f ms)\n",
                    index++, clockTime, min, sec, severity, spike.fpsBefore, spike.instantFps, spike.frameTimeMs));
            sb.append(String.format("    🎯 ГЛАВНАЯ ПРИЧИНА: %s (%s)\n", spike.primaryCause.displayName, spike.primaryCause.description));

            sb.append(String.format("    🎮 В ИГРЕ: Память Heap: %d MB | Пауза GC: %d ms | Чанков загружено: %d (Всего: %d) | Сущностей: %d (Игроков: %d, Мобов: %d, Дропа: %d, Стоек: %d)\n",
                    spike.usedHeapMb, spike.gcPauseMs, spike.chunksLoaded, spike.totalLoadedChunks, spike.totalEntities,
                    spike.playersCount, spike.mobsCount, spike.itemsCount, spike.armorStandsCount));
            sb.append(String.format("    🌐 СЕТЬ / ЭКРАН: Пакетов принято: %d (Чанки: %d, Блоки: %d, Сущности: %d, Взрывы: %d) | Экран: %s\n",
                    spike.packetsCount, spike.chunkPackets, spike.blockPackets, spike.entityPackets, spike.explosionsCount, spike.screenName));
            sb.append(String.format("    💻 НА ПК: Нагрузка CPU системы: %.1f%% (Minecraft: %.1f%%) | RAM ПК: %d MB / %d MB\n\n",
                    spike.sysCpuPercent, spike.jvmCpuPercent, spike.sysUsedRamMb, spike.sysTotalRamMb));
        }
    }

    private static void buildPerFrameLogTable(StringBuilder sb, PerformanceProfiler.SessionData session, SimpleDateFormat timeDf) {
        if (session.allFrames.isEmpty()) {
            sb.append("Покадровые данные отсутствуют.\n");
            return;
        }

        sb.append(String.format("%-12s | %-10s | %-11s | %-10s | %-9s | %-28s | %-8s | %-7s | %-6s | %-8s | %-7s | %-12s | %-12s\n",
                "ВРЕМЯ", "ТАЙМКОД", "ВРЕМЯ КАДРА", "FPS", "ПАДЕНИЕ", "ПРИЧИНА", "GC ПАУЗА", "HEAP MB", "ЧАНКИ", "СУЩНОСТИ", "ПАКЕТЫ", "CPU%(SYS/MC)", "ЭКРАН"));
        sb.append("--------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");

        for (PerformanceProfiler.FrameRecord f : session.allFrames) {
            String clockTime = timeDf.format(new Date(f.wallClockTimeMs));
            long offsetMs = f.timestampMs;
            int min = (int) (offsetMs / 60000);
            double sec = (offsetMs % 60000) / 1000.0;
            String timecode = String.format("%02d:%05.2f", min, sec);
            float drop = Math.max(0.0f, f.fpsBefore - f.instantFps);
            String dropStr = drop > 0.5f ? String.format("-%4.1f", drop) : "0.0";
            String cpuStr = String.format("%.0f/%.0f%%", f.sysCpuPercent, f.jvmCpuPercent);
            String causeName = f.cause == PerformanceProfiler.SpikeCause.NONE ? "-" : f.cause.displayName;

            sb.append(String.format("%-12s | %-10s | %8.2f ms | %7.1f FPS | %-9s | %-28s | %6d ms | %5d MB | %5d | %8d | %7d | %-12s | %-12s\n",
                    clockTime, timecode, f.frameTimeMs, f.instantFps, dropStr, causeName,
                    f.gcPauseMs, f.usedHeapMb, f.chunksLoaded, f.totalEntities, f.packetsCount, cpuStr, f.screenName));
        }
    }

    private static List<String> generateRecommendations(PerformanceProfiler.SessionData session) {
        List<String> recs = new ArrayList<>();

        int chunkSpikes = session.causeBreakdown.getOrDefault(PerformanceProfiler.SpikeCause.CHUNK_LOADING, 0);
        int gcSpikes = session.causeBreakdown.getOrDefault(PerformanceProfiler.SpikeCause.GC_PAUSE, 0);
        int entitySpikes = session.causeBreakdown.getOrDefault(PerformanceProfiler.SpikeCause.ENTITIES_DENSITY, 0);
        int particleSpikes = session.causeBreakdown.getOrDefault(PerformanceProfiler.SpikeCause.PARTICLE_BURST, 0);
        int highCpuSpikes = session.causeBreakdown.getOrDefault(PerformanceProfiler.SpikeCause.HIGH_SYSTEM_CPU, 0);

        if (gcSpikes > 0 || session.totalGcTimeMs > 200) {
            recs.add("[ПАУЗЫ СБОРЩИКА МУСОРА (GC STOP-THE-WORLD)]\n" +
                    "   Зафиксированы микрофризы из-за сборки мусора в оперативной памяти.\n" +
                    "   -> Включите флаги Generational ZGC в параметрах запуска Java 21:\n" +
                    "      -XX:+UseZGC -XX:+ZGenerational -XX:+AlwaysPreTouch\n" +
                    "   -> Это снизит задержки GC до <1 ms и полностью устранит просадки FPS от памяти.");
        }

        if (highCpuSpikes > 0) {
            recs.add("[ФОНОВАЯ НАГРУЗКА ПРОЦЕССОРА ПК]\n" +
                    "   Зафиксированы просадки, вызванные сторонними процессами Windows (загрузка CPU системы >85%).\n" +
                    "   -> Проверьте раздел №4 отчёта (список процессов) и закройте ресурсоёмкие программы (браузеры, Discord с аппаратным ускорением, лишние фоновые утилиты).");
        }

        if (chunkSpikes > 0) {
            recs.add("[ПРОГРУЗКА ЧАНКОВ И ПЕРЕСТРОЙКА МЕШЕЙ]\n" +
                    "   Зафиксированы просадки при загрузке новых секций мира.\n" +
                    "   -> Снизьте дистанцию симуляции до 6-8 чанков в Настройках Графики.\n" +
                    "   -> В модуле Optimizer включите оптимизацию рендеринга мира.");
        }

        if (entitySpikes > 0 || session.maxEntitiesInFrame > 150) {
            recs.add("[СКОПЛЕНИЕ СУЩНОСТЕЙ И СПАМ ДРОПА]\n" +
                    "   Просадки происходят из-за большого числа мобов, игроков или выпавших предметов.\n" +
                    "   -> Включите в модуле 'Optimizer' опции 'Entity Distance Culling' и 'Hide Armor Stands'.\n" +
                    "   -> Включите 'Disable Fireworks' для защиты от фейерверк-лагов.");
        }

        if (particleSpikes > 0 || session.maxParticlesCount > 200) {
            recs.add("[ЧАСТИЦЫ И ЭФФЕКТЫ ВЗРЫВОВ]\n" +
                    "   Всплески частиц от зелий, взрывов или кристаллов вызывают падение кадров.\n" +
                    "   -> Включите в модуле 'Optimizer' опцию 'Cull Particles Behind Walls'.");
        }

        if (recs.isEmpty()) {
            recs.add("[ОБЩАЯ СТАБИЛЬНОСТЬ]\n" +
                    "   Система работает стабильно. Для поддержания максимальной плавности держите включенным модуль Optimizer.");
        }

        return recs;
    }
}
