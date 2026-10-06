package naryn.sun.systems.modules.modules.visuals;

import lombok.Getter;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.GroupSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.systems.setting.settings.StringSetting;
import naryn.sun.utility.game.EntityUtility;

@Getter
@ModuleInfo(
    name = "Name Utility",
    category = ModuleCategory.VISUALS,
    desc = "modules.descriptions.name_utility"
)
public class NameUtility extends BaseModule {

    // --- Бокс 1: Скрытие ника ---
    private final GroupSetting hideNickGroup = new GroupSetting(this, "modules.settings.name_utility.group.hide_nick");

    private final BooleanSetting hideNick = new BooleanSetting(this.hideNickGroup, "modules.settings.name_utility.hide_nick");

    private final StringSetting fakeName = new StringSetting(this.hideNickGroup, "modules.settings.name_utility.fake_name", () -> !this.hideNick.isEnabled())
            .text("Player");

    // --- Бокс 2: Ник от 3-го лица ---
    private final GroupSetting thirdPersonGroup = new GroupSetting(this, "modules.settings.name_utility.group.third_person");

    private final BooleanSetting thirdPerson = new BooleanSetting(this.thirdPersonGroup, "modules.settings.name_utility.third_person")
            .enabled(true);

    // --- Бокс 3: Кастомные неймтеги ---
    private final GroupSetting nameTagGroup = new GroupSetting(this, "modules.settings.name_utility.group.nametag");

    private final BooleanSetting nameTag = new BooleanSetting(this.nameTagGroup, "modules.settings.name_utility.nametag")
            .enabled(true);

    private final BooleanSetting showPing = new BooleanSetting(this.nameTagGroup, "modules.settings.name_utility.ping", () -> !this.nameTag.isEnabled())
            .enabled(true);

    private final ModeSetting style = new ModeSetting(this.nameTagGroup, "modules.settings.name_utility.style", () -> !this.nameTag.isEnabled());
    private final ModeSetting.Value styleGlass = new ModeSetting.Value(this.style, "modules.settings.name_utility.style.glass").select();
    private final ModeSetting.Value styleDark = new ModeSetting.Value(this.style, "modules.settings.name_utility.style.dark");

    private final ModeSetting font = new ModeSetting(this.nameTagGroup, "modules.settings.name_utility.font", () -> !this.nameTag.isEnabled());
    private final ModeSetting.Value fontPixel = new ModeSetting.Value(this.font, "modules.settings.name_utility.font.pixel").select();
    private final ModeSetting.Value fontUi = new ModeSetting.Value(this.font, "modules.settings.name_utility.font.ui");

    private final SliderSetting scale = new SliderSetting(this.nameTagGroup, "modules.settings.name_utility.scale", () -> !this.nameTag.isEnabled())
            .min(0.5F)
            .max(1.5F)
            .step(0.05F)
            .currentValue(1.0F)
            .suffix("x");

    public boolean isGlassStyle() {
        return this.styleGlass.isSelected();
    }

    public boolean isUiFont() {
        return this.fontUi.isSelected();
    }

    public boolean canShowThirdPersonNick() {
        return this.thirdPerson.isEnabled() && !mc.options.getPerspective().isFirstPerson();
    }

    public String patchName(String text) {
        if (!this.isEnabled() || !this.hideNick.isEnabled() || text == null) {
            return text;
        }

        String clientUsername = mc.getSession().getUsername();
        String fake = this.fakeName.getText();
        if (fake == null || fake.isEmpty()) {
            fake = "Player";
        }

        if (EntityUtility.isInGame() && mc.player != null) {
            text = text.replace(mc.player.getDisplayName().getString(), fake);
            text = text.replace(mc.player.getName().getString(), fake);
        }

        return text.replace(clientUsername, fake);
    }
}
