package naryn.sun.utility.profiler;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.*;

/**
 * Утилита для высокоточного мониторинга системных ресурсов ПК:
 * загрузка процессора (CPU), оперативной памяти (RAM), активных процессов ОС и потоков JVM.
 */
public final class SystemProcessTracker {

    private SystemProcessTracker() {
    }

    public static class ProcessSnapshot {
        public final long pid;
        public final String name;
        public final long cpuMillis;
        public final String commandLine;

        public ProcessSnapshot(long pid, String name, long cpuMillis, String commandLine) {
            this.pid = pid;
            this.name = name;
            this.cpuMillis = cpuMillis;
            this.commandLine = commandLine;
        }
    }

    /**
     * Получает снимок всех активных процессов на ПК.
     */
    public static List<ProcessSnapshot> getRunningProcesses() {
        List<ProcessSnapshot> processes = new ArrayList<>();
        try {
            ProcessHandle.allProcesses().forEach(ph -> {
                try {
                    ProcessHandle.Info info = ph.info();
                    String cmd = info.command().orElse(info.commandLine().orElse(""));
                    String name;
                    if (!cmd.isEmpty()) {
                        name = new File(cmd).getName();
                    } else {
                        name = "PID " + ph.pid();
                    }
                    Optional<Duration> cpu = info.totalCpuDuration();
                    long cpuMs = cpu.map(Duration::toMillis).orElse(0L);
                    String cmdLine = info.commandLine().orElse(name);
                    processes.add(new ProcessSnapshot(ph.pid(), name, cpuMs, cmdLine));
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }
        processes.sort((a, b) -> Long.compare(b.cpuMillis, a.cpuMillis));
        return processes;
    }

    /**
     * Возвращает общую загрузку CPU системы в процентах (0.0 - 100.0).
     */
    public static float getSystemCpuLoad() {
        try {
            java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
                double load = sunOs.getCpuLoad();
                if (load >= 0.0) {
                    return (float) (load * 100.0);
                }
            }
        } catch (Throwable ignored) {
        }
        return 0.0f;
    }

    /**
     * Возвращает загрузку CPU процессом Minecraft/JVM в процентах (0.0 - 100.0).
     */
    public static float getProcessCpuLoad() {
        try {
            java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
                double load = sunOs.getProcessCpuLoad();
                if (load >= 0.0) {
                    return (float) (load * 100.0);
                }
            }
        } catch (Throwable ignored) {
        }
        return 0.0f;
    }

    /**
     * Общий объем оперативной памяти ПК в МБ.
     */
    public static long getSystemTotalMemoryMb() {
        try {
            java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
                return sunOs.getTotalMemorySize() / (1024L * 1024L);
            }
        } catch (Throwable ignored) {
        }
        return -1L;
    }

    /**
     * Объем занятой оперативной памяти ПК в МБ.
     */
    public static long getSystemUsedMemoryMb() {
        try {
            java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
                long total = sunOs.getTotalMemorySize();
                long free = sunOs.getFreeMemorySize();
                if (total > 0 && free >= 0) {
                    return (total - free) / (1024L * 1024L);
                }
            }
        } catch (Throwable ignored) {
        }
        return -1L;
    }

    /**
     * Количество активных потоков в JVM.
     */
    public static int getJvmThreadCount() {
        try {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            return threadBean.getThreadCount();
        } catch (Throwable ignored) {
            return -1;
        }
    }
}
