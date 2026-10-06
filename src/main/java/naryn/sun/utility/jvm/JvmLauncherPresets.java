package naryn.sun.utility.jvm;

import naryn.sun.utility.interfaces.IMinecraft;

/**
 * Пресеты аргументов JVM для лаунчера и тонкой настройки сборщиков мусора (GC).
 * Оптимизированы под Java 21 и Fabric 1.21.4 для устранения 1% Low FPS дропов и микрофризов.
 */
public final class JvmLauncherPresets implements IMinecraft {

    public record Preset(String id, String nameKey, String descKey, String args, String targetRam) {}

    public static final Preset GENERATIONAL_ZGC = new Preset(
            "gen_zgc",
            "menu.gui_settings.jvm.gen_zgc.title",
            "menu.gui_settings.jvm.gen_zgc.desc",
            "-XX:+UseZGC -XX:+ZGenerational -XX:+UnlockExperimentalVMOptions -XX:+AlwaysPreTouch -XX:+UseNUMA",
            "4G-8G"
    );

    public static final Preset SHENANDOAH_GC = new Preset(
            "shenandoah",
            "menu.gui_settings.jvm.shenandoah.title",
            "menu.gui_settings.jvm.shenandoah.desc",
            "-XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:+UnlockExperimentalVMOptions -XX:+AlwaysPreTouch",
            "4G-6G"
    );

    public static final Preset TUNED_G1GC = new Preset(
            "tuned_g1",
            "menu.gui_settings.jvm.tuned_g1.title",
            "menu.gui_settings.jvm.tuned_g1.desc",
            "-XX:+UseG1GC -XX:MaxGCPauseMillis=5 -XX:G1ReservePercent=15 -XX:InitiatingHeapOccupancyPercent=45 -XX:+AlwaysPreTouch",
            "2G-4G"
    );

    public static final Preset[] ALL_PRESETS = {
            GENERATIONAL_ZGC,
            SHENANDOAH_GC,
            TUNED_G1GC
    };

    private JvmLauncherPresets() {
    }

    public static boolean copyToClipboard(Preset preset) {
        if (mc.keyboard != null && preset != null) {
            mc.keyboard.setClipboard(preset.args());
            return true;
        }
        return false;
    }
}
