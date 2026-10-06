package naryn.sun.utility.profiler;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Диагностический инструмент для мониторинга памяти и сборщика мусора (GC) в реальном времени.
 * Позволяет отслеживать нагрузку на Heap, задержки GC (GC pauses) и тип используемого сборщика.
 */
public final class MemoryTracker {

    private static long lastSampleTime = System.currentTimeMillis();
    private static long lastUsedMemory = getUsedMemoryBytes();
    private static double allocationRateMbPerSec = 0.0;

    private MemoryTracker() {
    }

    public static long getUsedMemoryBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    public static long getMaxMemoryBytes() {
        return Runtime.getRuntime().maxMemory();
    }

    public static long getTotalMemoryBytes() {
        return Runtime.getRuntime().totalMemory();
    }

    public static long getUsedMemoryMb() {
        return getUsedMemoryBytes() / (1024L * 1024L);
    }

    public static long getMaxMemoryMb() {
        return getMaxMemoryBytes() / (1024L * 1024L);
    }

    public static long getTotalMemoryMb() {
        return getTotalMemoryBytes() / (1024L * 1024L);
    }

    public static int getMemoryUsagePercent() {
        long max = getMaxMemoryBytes();
        if (max <= 0) return 0;
        return (int) ((getUsedMemoryBytes() * 100L) / max);
    }

    public static String getGcNames() {
        List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
        if (beans.isEmpty()) {
            return "Unknown";
        }
        StringBuilder sb = new StringBuilder();
        for (GarbageCollectorMXBean bean : beans) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(bean.getName());
        }
        return sb.toString();
    }

    public static long getTotalGcCount() {
        long count = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long c = bean.getCollectionCount();
            if (c > 0) {
                count += c;
            }
        }
        return count;
    }

    public static long getTotalGcTimeMs() {
        long time = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long t = bean.getCollectionTime();
            if (t > 0) {
                time += t;
            }
        }
        return time;
    }

    public static double getAllocationRateMbPerSec() {
        updateRateIfNeeded();
        return allocationRateMbPerSec;
    }

    public static synchronized void updateRateIfNeeded() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastSampleTime;
        if (elapsed >= 1000L) {
            long currentUsed = getUsedMemoryBytes();
            long diff = currentUsed - lastUsedMemory;
            if (diff > 0) {
                allocationRateMbPerSec = (diff / (1024.0 * 1024.0)) / (elapsed / 1000.0);
            }
            lastUsedMemory = currentUsed;
            lastSampleTime = now;
        }
    }

    public static String getSummary() {
        return String.format(
                "Heap: %dMB / %dMB (%d%%) | GC: %s (Pauses: %dms / %d collections)",
                getUsedMemoryMb(),
                getMaxMemoryMb(),
                getMemoryUsagePercent(),
                getGcNames(),
                getTotalGcTimeMs(),
                getTotalGcCount()
        );
    }
}
