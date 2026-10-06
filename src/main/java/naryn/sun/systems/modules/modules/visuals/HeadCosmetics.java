package naryn.sun.systems.modules.modules.visuals;

import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.ColorSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;

@ModuleInfo(name = "Head Cosmetics", category = ModuleCategory.VISUALS, desc = "Косметические предметы на голове игрока")
public class HeadCosmetics extends BaseModule {

    // Насколько приподнять ник над стандартной позицией, чтобы косметика его не
    // перекрывала. Значения ориентировочные (China Hat/Halo примерно одной высоты
    // над головой — см. Y_OFFSET в соответствующих FeatureRenderer'ах), подбирались
    // "на глаз", можно подкрутить после проверки в игре.
    private static final float CHINA_HAT_NAME_LABEL_OFFSET = 0.5F;
    private static final float HALO_NAME_LABEL_OFFSET = 0.55F;

    // Отдел режимов — сюда в будущем добавляются новые головные косметики как ModeSetting.Value.
    // "None" убран как бесполезный: выключение косметики уже делается тумблером самого модуля.
    private final ModeSetting mode = new ModeSetting(this, "modules.settings.head_cosmetics.mode");
    private final ModeSetting.Value chinaHatMode = new ModeSetting.Value(this.mode, "modules.settings.head_cosmetics.mode.china_hat");
    private final ModeSetting.Value haloMode = new ModeSetting.Value(this.mode, "modules.settings.head_cosmetics.mode.halo");

    // Кому видна косметика: всем игрокам вокруг (как раньше, по умолчанию)
    // или только самому себе (в третьем лице/через отражения и т.п.).
    private final ModeSetting visibility = new ModeSetting(this, "modules.settings.head_cosmetics.visibility");
    private final ModeSetting.Value everyoneVisibility = new ModeSetting.Value(this.visibility, "modules.settings.head_cosmetics.visibility.everyone");
    private final ModeSetting.Value onlyMeVisibility = new ModeSetting.Value(this.visibility, "modules.settings.head_cosmetics.visibility.only_me");

    private final ColorSetting chinaHatColor = new ColorSetting(
        this, "modules.settings.head_cosmetics.china_hat_color", () -> !this.chinaHatMode.isSelected()
    ).color(Colors.ACCENT).alpha(true);

    // У Нимба намеренно нет ColorSetting: цвет фиксированный (ангельский бело-жёлтый),
    // пользователь не может его перекрасить — в отличие от остальной косметики этого модуля.

    public boolean isChinaHatSelected() {
        return this.isEnabled() && this.chinaHatMode.isSelected();
    }

    public boolean isHaloSelected() {
        return this.isEnabled() && this.haloMode.isSelected();
    }

    public ColorRGBA getChinaHatColor() {
        return this.chinaHatColor.getColor();
    }

    /**
     * true — режим "только у меня": FeatureRenderer'ы должны рисовать косметику
     * исключительно на локальном игроке, скрывая её на остальных сущностях.
     */
    public boolean isOnlyMeVisible() {
        return this.onlyMeVisibility.isSelected();
    }

    /**
     * На сколько нужно приподнять ник над стандартной позицией для конкретной
     * сущности, чтобы надетая на неё косметика этого модуля его не перекрывала.
     * 0 — если для этой сущности косметика вообще не будет отрисована (модуль
     * выключен/косметика не выбрана, либо активен режим "только у меня", а
     * сущность — не локальный игрок).
     */
    public float getNameLabelExtraOffset(Entity entity) {
        if (!this.isChinaHatSelected() && !this.isHaloSelected()) {
            return 0.0F;
        }

        if (this.isOnlyMeVisible()) {
            ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;
            if (clientPlayer == null || entity != clientPlayer) {
                return 0.0F;
            }
        }

        return this.isHaloSelected() ? HALO_NAME_LABEL_OFFSET : CHINA_HAT_NAME_LABEL_OFFSET;
    }
}