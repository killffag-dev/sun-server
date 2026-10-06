package naryn.sun.utility.profiler;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Мягкая динамическая интеграция с Spark профилировщиком.
 * Позволяет запускать замеры аллокаций памяти и CPU без жесткой зависимости.
 */
public final class SparkProfilerIntegration {

    private static final boolean SPARK_LOADED = FabricLoader.getInstance().isModLoaded("spark");

    private SparkProfilerIntegration() {
    }

    public static boolean isSparkLoaded() {
        return SPARK_LOADED;
    }

    public static String getStatusDescription() {
        if (SPARK_LOADED) {
            return "Spark profiler active (use /spark heap /spark sampler)";
        }
        return "Built-in SUN Diagnostics active (Spark not installed)";
    }
}
