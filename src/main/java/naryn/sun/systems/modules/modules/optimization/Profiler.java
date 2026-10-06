package naryn.sun.systems.modules.modules.optimization;

import java.io.File;
import lombok.Generated;
import naryn.sun.Sun;
import naryn.sun.systems.file.FileManager;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.notifications.NotificationType;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.ButtonSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.systems.setting.settings.StringSetting;
import naryn.sun.utility.profiler.PerformanceProfiler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

@ModuleInfo(name = "Profiler", category = ModuleCategory.OPTIMIZATION)
public class Profiler extends BaseModule {

    private final SliderSetting spikeThreshold = new SliderSetting(this, "modules.settings.profiler.spike_threshold")
            .min(15.0F)
            .max(100.0F)
            .step(5.0F)
            .currentValue(30.0F);

    private final ModeSetting saveLocation = new ModeSetting(this, "modules.settings.profiler.save_location");
    private final ModeSetting.Value modeDesktop = new ModeSetting.Value(this.saveLocation, "modules.settings.profiler.save_location.desktop");
    private final ModeSetting.Value modeClient = new ModeSetting.Value(this.saveLocation, "modules.settings.profiler.save_location.client");
    private final ModeSetting.Value modeCustom = new ModeSetting.Value(this.saveLocation, "modules.settings.profiler.save_location.custom");

    private final StringSetting customPath = new StringSetting(this, "modules.settings.profiler.custom_path", () -> !this.saveLocation.is(this.modeCustom))
            .text("");

    private final ButtonSetting selectFolder = new ButtonSetting(this, "modules.settings.profiler.select_folder")
            .action(() -> {
                new Thread(() -> {
                    try {
                        String current = this.customPath.getText();
                        String initial = (current != null && !current.trim().isEmpty()) ? current.trim() : System.getProperty("user.home");
                        String selected = TinyFileDialogs.tinyfd_selectFolderDialog("Выберите папку для отчетов SUN", initial);
                        if (selected != null && !selected.trim().isEmpty()) {
                            String trimmed = selected.trim();
                            this.customPath.text(trimmed);
                            this.saveLocation.setValue(this.modeCustom);
                            MinecraftClient mc = MinecraftClient.getInstance();
                            if (mc != null) {
                                mc.execute(() -> {
                                    Sun.getInstance().getNotificationManager().addNotification(
                                            NotificationType.SUCCESS,
                                            "Папка выбрана: " + new File(trimmed).getName()
                                    );
                                });
                            }
                        }
                    } catch (Throwable t) {
                        Sun.LOGGER.error("Failed to select folder", t);
                    }
                }, "SUN-Folder-Chooser").start();
            });

    private final ButtonSetting openFolder = new ButtonSetting(this, "modules.settings.profiler.open_folder")
            .action(() -> {
                File dir = getTargetDirectory();
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                Util.getOperatingSystem().open(dir);
            });

    private final BooleanSetting autoOpenReport = new BooleanSetting(this, "modules.settings.profiler.auto_open_report").enable();

    @Override
    public void onEnable() {
        PerformanceProfiler.getInstance().start(this.spikeThreshold.getCurrentValue(), this.autoOpenReport.isEnabled());
    }

    @Override
    public void onDisable() {
        PerformanceProfiler.getInstance().stop(getTargetDirectory());
    }

    public File getTargetDirectory() {
        if (this.saveLocation.is(this.modeClient)) {
            File dir = new File(FileManager.DIRECTORY, "reports");
            if (!dir.exists()) dir.mkdirs();
            return dir;
        } else if (this.saveLocation.is(this.modeCustom)) {
            String path = this.customPath.getText();
            if (path != null && !path.trim().isEmpty()) {
                File dir = new File(path.trim());
                if (!dir.exists()) dir.mkdirs();
                return dir;
            }
        }

        // Default: Desktop
        File desktopDir = new File(System.getProperty("user.home"), "Desktop");
        if (!desktopDir.exists() || !desktopDir.isDirectory()) {
            File oneDriveDesktop = new File(System.getProperty("user.home"), "OneDrive" + File.separator + "Desktop");
            if (oneDriveDesktop.exists() && oneDriveDesktop.isDirectory()) {
                return oneDriveDesktop;
            }
            File fallback = new File(FileManager.DIRECTORY, "reports");
            if (!fallback.exists()) fallback.mkdirs();
            return fallback;
        }
        return desktopDir;
    }

    @Generated
    public SliderSetting getSpikeThreshold() {
        return this.spikeThreshold;
    }

    @Generated
    public ModeSetting getSaveLocation() {
        return this.saveLocation;
    }

    @Generated
    public StringSetting getCustomPath() {
        return this.customPath;
    }

    @Generated
    public ButtonSetting getSelectFolder() {
        return this.selectFolder;
    }

    @Generated
    public ButtonSetting getOpenFolder() {
        return this.openFolder;
    }

    @Generated
    public BooleanSetting getAutoOpenReport() {
        return this.autoOpenReport;
    }
}
