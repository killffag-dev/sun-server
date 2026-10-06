package naryn.sun.ui.menu.components.settings;

import naryn.sun.Sun;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.config.ConfigFile;
import naryn.sun.systems.config.ConfigManager;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.theme.ClientAppearance;
import naryn.sun.ui.components.textfield.TextField;
import naryn.sun.ui.menu.components.settings.configs.ConfigItemRow;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.sounds.ClientSoundManager;
import naryn.sun.utility.time.Timer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Интуитивно понятный раздел управления конфигурациями:
 * - Карточка текущего активного конфига с кнопкой быстрого сохранения изменений;
 * - Панель действий: создание нового конфига, открытие папки файлов, обновление списка;
 * - Список сохранённых конфигов с понятными кнопками (Загрузить, Переименовать, Удалить);
 * - Инлайн-редактирование и защита от случайного удаления.
 */
public class ConfigsRenderer extends SettingRenderer implements ConfigItemRow.RowActionListener {

    private static final float HERO_CARD_H    = 34.0F;
    private static final float ACTION_BAR_H   = 20.0F;
    private static final float CREATE_BOX_H   = 32.0F;
    private static final float GAP_Y          = 7.0F;
    private static final float ROW_GAP        = 5.0F;

    private static final String BTN_CREATE    = "cfg_btn_create";
    private static final String BTN_FOLDER    = "cfg_btn_folder";
    private static final String BTN_SAVE_HERO = "cfg_btn_save_hero";

    private final TextField createField = new TextField(Fonts.REGULAR.getFont(7.5F));
    private final TextField editField = new TextField(Fonts.REGULAR.getFont(7.5F));
    private final Timer refreshTimer = new Timer();
    private final Timer savedFeedbackTimer = new Timer();

    private float lastExpand = 0.0F;
    private boolean creatingNew = false;
    private String editingName = null;
    private String confirmDeleteName = null;

    private ConfigManager manager() {
        return Sun.getInstance().getConfigManager();
    }

    private List<ConfigFile> customConfigs() {
        return manager().getConfigFiles().stream()
                .filter(c -> !c.getFileName().equalsIgnoreCase("autosave"))
                .collect(Collectors.toList());
    }

    @Override
    public void render(UIContext context, float x, float y, float width, float settY, float expand) {
        if (lastExpand <= 0.0F && expand > 0.0F) {
            manager().refresh();
            refreshTimer.reset();
        }
        if (expand > 0.0F && refreshTimer.finished(600L)) {
            manager().refresh();
            refreshTimer.reset();
        }
        lastExpand = expand;

        float innerX = x + 8.0F;
        float innerW = width - 16.0F;
        float curY = settY;

        ConfigFile current = manager().getCurrent();

        // 1. Hero-блок активного конфига (если выбран)
        if (current != null && !current.getFileName().equalsIgnoreCase("autosave")) {
            renderActiveConfigCard(context, innerX, curY, innerW, current);
            curY += HERO_CARD_H + GAP_Y;
        }

        // 2. Панель действий или инлайн-блок создания
        if (!creatingNew) {
            renderActionBar(context, innerX, curY, innerW);
            curY += ACTION_BAR_H + GAP_Y;
        } else {
            renderCreateBox(context, innerX, curY, innerW);
            curY += CREATE_BOX_H + GAP_Y;
        }

        // 3. Заголовок списка конфигов
        Font secFont = Fonts.MEDIUM.getFont(6.5F);
        List<ConfigFile> configs = customConfigs();
        String listTitle = Localizator.translate("menu.gui_settings.configs.all_configs") + " (" + configs.size() + ")";
        context.drawText(secFont, listTitle, innerX + 2.0F, curY + 2.0F, Colors.getTextColor().withAlpha(160.0F));
        curY += secFont.height() + 5.0F;

        // 4. Список файлов
        if (configs.isEmpty()) {
            Font emptyFont = Fonts.REGULAR.getFont(7.0F);
            context.drawText(emptyFont, Localizator.translate("menu.gui_settings.configs.empty"),
                    innerX + 4.0F, curY + 4.0F, Colors.getTextColor().withAlpha(140.0F));
            return;
        }

        for (ConfigFile cfg : configs) {
            String name = cfg.getFileName();
            Animation hoverAnim = getRowHoverAnim("cfg_row_" + name);
            hoverAnim.update(GuiUtility.isHovered(innerX, curY, innerW, ConfigItemRow.ROW_HEIGHT, context.getMouseX(), context.getMouseY()));

            boolean isCurrent = manager().isCurrent(cfg);
            if (name.equals(editingName)) {
                ConfigItemRow.renderEditing(context, innerX, curY, innerW, editField);
            } else if (name.equals(confirmDeleteName)) {
                ConfigItemRow.renderConfirmDelete(context, innerX, curY, innerW, cfg);
            } else {
                ConfigItemRow.renderNormal(context, innerX, curY, innerW, cfg, isCurrent, hoverAnim, hoverAnim);
            }
            curY += ConfigItemRow.ROW_HEIGHT + ROW_GAP;
        }
    }

    private void renderActiveConfigCard(UIContext context, float bx, float by, float bw, ConfigFile current) {
        MenuSkin skin = MenuSkin.current();
        skin.renderSettingBox(context, bx, by, bw, HERO_CARD_H, BorderRadius.all(6.0F), 1.0F);

        ColorRGBA accent = ClientAppearance.getAccent();
        context.drawRoundedBorder(bx, by, bw, HERO_CARD_H, 0.8F, BorderRadius.all(6.0F), accent.withAlpha(140.0F));

        // Кнопка сохранения изменений справа
        Font btnFont = Fonts.MEDIUM.getFont(6.5F);
        boolean recentlySaved = !savedFeedbackTimer.finished(1800L);
        String saveText = recentlySaved
                ? Localizator.translate("menu.gui_settings.configs.saved_success")
                : Localizator.translate("menu.gui_settings.configs.save_changes");

        float saveBtnW = btnFont.width(saveText) + 14.0F;
        float saveBtnH = 20.0F;
        float saveBtnX = bx + bw - saveBtnW - 7.0F;
        float saveBtnY = by + (HERO_CARD_H - saveBtnH) / 2.0F;

        boolean saveHover = GuiUtility.isHovered(saveBtnX, saveBtnY, saveBtnW, saveBtnH, context.getMouseX(), context.getMouseY());
        if (saveHover) CursorUtility.set(CursorType.HAND);

        ColorRGBA btnBg = recentlySaved ? new ColorRGBA(50, 190, 100, 230) : accent;
        context.drawRoundedRect(saveBtnX, saveBtnY, saveBtnW, saveBtnH, BorderRadius.all(4.0F),
                btnBg.withAlpha((int) ((saveHover ? 230 : 180) * 1.0F)));
        context.drawCenteredText(btnFont, saveText, saveBtnX + saveBtnW / 2.0F, saveBtnY + (saveBtnH - btnFont.height()) / 2.0F + 0.5F, ColorRGBA.WHITE);

        // Поясняющие надписи слева: что делает кнопка сохранения
        Font titleFont = Fonts.MEDIUM.getFont(7.0F);
        Font descFont = Fonts.REGULAR.getFont(5.5F);
        float textX = bx + 9.0F;
        float maxTextW = saveBtnX - textX - 6.0F;

        String titleText = Localizator.translate("menu.gui_settings.configs.save_card_title");
        String descText = Localizator.translate("menu.gui_settings.configs.save_card_desc");

        if (titleFont.width(titleText) <= maxTextW) {
            context.drawText(titleFont, titleText, textX, by + 6.5F, Colors.getTextColor());
        } else {
            context.drawFadeoutText(titleFont, titleText, textX, by + 6.5F, Colors.getTextColor(), 0.85F, 1.0F, maxTextW);
        }

        if (descFont.width(descText) <= maxTextW) {
            context.drawText(descFont, descText, textX, by + 18.0F, Colors.getTextColor().withAlpha(150.0F));
        } else {
            context.drawFadeoutText(descFont, descText, textX, by + 18.0F, Colors.getTextColor().withAlpha(150.0F), 0.85F, 1.0F, maxTextW);
        }
    }

    private void renderActionBar(UIContext context, float bx, float by, float bw) {
        float gap = 5.0F;
        float createW = (bw - gap) * 0.56F;
        float folderW = bw - createW - gap;

        // 1. Кнопка создания
        renderActionBtn(context, bx, by, createW, ACTION_BAR_H,
                Localizator.translate("menu.gui_settings.configs.create_button"), BTN_CREATE, true);

        // 2. Кнопка папки
        renderActionBtn(context, bx + createW + gap, by, folderW, ACTION_BAR_H,
                Localizator.translate("menu.gui_settings.configs.open_folder"), BTN_FOLDER, false);
    }

    private void renderCreateBox(UIContext context, float bx, float by, float bw) {
        MenuSkin.current().renderSettingBox(context, bx, by, bw, CREATE_BOX_H, BorderRadius.all(6.0F), 1.0F);

        Font btnFont = Fonts.MEDIUM.getFont(6.5F);
        String confirmText = Localizator.translate("menu.gui_settings.configs.create_confirm");
        String cancelText = Localizator.translate("menu.gui_settings.configs.cancel");
        float confirmW = btnFont.width(confirmText) + 14.0F;
        float cancelW = btnFont.width(cancelText) + 12.0F;
        float btnH = 20.0F;
        float btnY = by + (CREATE_BOX_H - btnH) / 2.0F;

        float cancelX = bx + bw - cancelW - 6.0F;
        float confirmX = cancelX - confirmW - 4.0F;

        float fieldX = bx + 8.0F;
        float fieldW = confirmX - fieldX - 8.0F;
        float fieldH = 18.0F;
        float fieldY = by + (CREATE_BOX_H - fieldH) / 2.0F;

        createField.set(fieldX, fieldY, fieldW, fieldH);
        createField.setAlpha(1.0F);
        createField.setTextColor(Colors.getTextColor());
        createField.setPreview(Localizator.translate("menu.gui_settings.configs.name_placeholder"));
        createField.render(context);

        // Кнопка Создать
        boolean confirmHover = GuiUtility.isHovered(confirmX, btnY, confirmW, btnH, context.getMouseX(), context.getMouseY());
        if (confirmHover) CursorUtility.set(CursorType.HAND);
        ColorRGBA accent = ClientAppearance.getAccent();
        context.drawRoundedRect(confirmX, btnY, confirmW, btnH, BorderRadius.all(4.0F),
                accent.withAlpha((int) ((confirmHover ? 230 : 190) * 1.0F)));
        context.drawCenteredText(btnFont, confirmText, confirmX + confirmW / 2.0F, btnY + (btnH - btnFont.height()) / 2.0F + 0.5F, ColorRGBA.WHITE);

        // Кнопка Отмена
        boolean cancelHover = GuiUtility.isHovered(cancelX, btnY, cancelW, btnH, context.getMouseX(), context.getMouseY());
        if (cancelHover) CursorUtility.set(CursorType.HAND);
        MenuSkin.current().renderButtonBox(context, cancelX, btnY, cancelW, btnH, BorderRadius.all(4.0F), cancelHover ? 1.0F : 0.0F, 1.0F);
        context.drawCenteredText(btnFont, cancelText, cancelX + cancelW / 2.0F, btnY + (btnH - btnFont.height()) / 2.0F + 0.5F,
                Colors.getTextColor().withAlpha(cancelHover ? 255.0F : 190.0F));
    }

    private void renderActionBtn(UIContext context, float bx, float by, float bw, float bh, String label, String animKey, boolean accent) {
        boolean hovered = GuiUtility.isHovered(bx, by, bw, bh, context.getMouseX(), context.getMouseY());
        Animation hoverAnim = getRowHoverAnim(animKey);
        hoverAnim.update(hovered);
        if (hovered) CursorUtility.set(CursorType.HAND);

        Font btnFont = Fonts.MEDIUM.getFont(6.5F);
        if (accent) {
            ColorRGBA accColor = ClientAppearance.getAccent();
            context.drawRoundedRect(bx, by, bw, bh, BorderRadius.all(4.5F),
                    accColor.withAlpha((int) ((hovered ? 210 : 170) * 1.0F)));
            context.drawCenteredText(btnFont, label, bx + bw / 2.0F, by + (bh - btnFont.height()) / 2.0F + 0.5F, ColorRGBA.WHITE);
        } else {
            MenuSkin.current().renderButtonBox(context, bx, by, bw, bh, BorderRadius.all(4.5F), hoverAnim.getValue(), 1.0F);
            context.drawCenteredText(btnFont, label, bx + bw / 2.0F, by + (bh - btnFont.height()) / 2.0F + 0.5F,
                    Colors.getTextColor().withAlpha(255.0F * (0.80F + 0.20F * hoverAnim.getValue())));
        }
    }

    @Override
    public void handleClick(float x, float y, float width, float settY, double mouseX, double mouseY) {
        float innerX = x + 8.0F;
        float innerW = width - 16.0F;
        float curY = settY;

        ConfigFile current = manager().getCurrent();

        // 1. Клик по кнопке сохранения активного конфига
        if (current != null && !current.getFileName().equalsIgnoreCase("autosave")) {
            Font btnFont = Fonts.MEDIUM.getFont(6.5F);
            boolean recentlySaved = !savedFeedbackTimer.finished(1800L);
            String saveText = recentlySaved
                    ? Localizator.translate("menu.gui_settings.configs.saved_success")
                    : Localizator.translate("menu.gui_settings.configs.save_changes");
            float saveBtnW = btnFont.width(saveText) + 14.0F;
            float saveBtnH = 20.0F;
            float saveBtnX = innerX + innerW - saveBtnW - 7.0F;
            float saveBtnY = curY + (HERO_CARD_H - saveBtnH) / 2.0F;

            if (GuiUtility.isHovered(saveBtnX, saveBtnY, saveBtnW, saveBtnH, mouseX, mouseY)) {
                current.save();
                savedFeedbackTimer.reset();
                ClientSoundManager.getInstance().playButtonClick();
                return;
            }
            curY += HERO_CARD_H + GAP_Y;
        }

        // 2. Клик по панели действий / инлайн-созданию
        if (!creatingNew) {
            float gap = 5.0F;
            float createW = (innerW - gap) * 0.56F;
            float folderW = innerW - createW - gap;

            if (GuiUtility.isHovered(innerX, curY, createW, ACTION_BAR_H, mouseX, mouseY)) {
                creatingNew = true;
                editingName = null;
                confirmDeleteName = null;
                createField.clear();
                createField.setFocused(true);
                ClientSoundManager.getInstance().playButtonClick();
                return;
            }
            if (GuiUtility.isHovered(innerX + createW + gap, curY, folderW, ACTION_BAR_H, mouseX, mouseY)) {
                openConfigsFolderAsync();
                ClientSoundManager.getInstance().playButtonClick();
                return;
            }
            curY += ACTION_BAR_H + GAP_Y;
        } else {
            Font btnFont = Fonts.MEDIUM.getFont(6.5F);
            String confirmText = Localizator.translate("menu.gui_settings.configs.create_confirm");
            String cancelText = Localizator.translate("menu.gui_settings.configs.cancel");
            float confirmW = btnFont.width(confirmText) + 14.0F;
            float cancelW = btnFont.width(cancelText) + 12.0F;
            float btnH = 20.0F;
            float btnY = curY + (CREATE_BOX_H - btnH) / 2.0F;
            float cancelX = innerX + innerW - cancelW - 6.0F;
            float confirmX = cancelX - confirmW - 4.0F;

            if (GuiUtility.isHovered(confirmX, btnY, confirmW, btnH, mouseX, mouseY)) {
                confirmCreate();
                ClientSoundManager.getInstance().playButtonClick();
                return;
            }
            if (GuiUtility.isHovered(cancelX, btnY, cancelW, btnH, mouseX, mouseY)) {
                creatingNew = false;
                createField.clear();
                ClientSoundManager.getInstance().playButtonClick();
                return;
            }

            createField.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
            if (!createField.isFocused() && !GuiUtility.isHovered(innerX, curY, innerW, CREATE_BOX_H, mouseX, mouseY)) {
                creatingNew = false;
                createField.clear();
            }
            return;
        }

        // Пропуск заголовка списка
        Font secFont = Fonts.MEDIUM.getFont(6.5F);
        curY += secFont.height() + 5.0F;

        // 3. Клик по строкам конфигов
        List<ConfigFile> configs = customConfigs();
        for (ConfigFile cfg : configs) {
            String name = cfg.getFileName();
            boolean isCurrent = manager().isCurrent(cfg);
            if (name.equals(editingName)) {
                if (ConfigItemRow.handleClickEditing(innerX, curY, innerW, editField, mouseX, mouseY, this)) return;
            } else if (name.equals(confirmDeleteName)) {
                if (ConfigItemRow.handleClickDelete(innerX, curY, innerW, cfg, mouseX, mouseY, this)) return;
            } else {
                if (ConfigItemRow.handleClickNormal(innerX, curY, innerW, cfg, isCurrent, mouseX, mouseY, this)) return;
            }
            curY += ConfigItemRow.ROW_HEIGHT + ROW_GAP;
        }
    }

    @Override
    public void onLoad(ConfigFile cfg) {
        ConfigFile current = manager().getCurrent();
        if (current != null && !current.getFileName().equalsIgnoreCase("autosave")) {
            current.save();
        }
        cfg.load();
        Sun.getInstance().getFileManager().saveClientFiles();
        ClientSoundManager.getInstance().playButtonClick();
    }

    @Override
    public void onSave(ConfigFile cfg) {
        cfg.save();
        savedFeedbackTimer.reset();
        ClientSoundManager.getInstance().playButtonClick();
    }

    @Override
    public void onStartRename(ConfigFile cfg) {
        creatingNew = false;
        confirmDeleteName = null;
        editingName = cfg.getFileName();
        editField.clear();
        editField.paste(cfg.getFileName());
        editField.setFocused(true);
        ClientSoundManager.getInstance().playButtonClick();
    }

    @Override
    public void onConfirmRename() {
        if (editingName == null) return;
        ConfigFile cfg = manager().getConfig(editingName, false);
        String newName = editField.getBuiltText().trim();
        if (cfg != null && !newName.isEmpty() && !newName.equalsIgnoreCase("autosave") && !newName.equalsIgnoreCase(editingName)) {
            cfg.rename(newName);
        }
        editingName = null;
        editField.clear();
        ClientSoundManager.getInstance().playButtonClick();
    }

    @Override
    public void onCancelRename() {
        editingName = null;
        editField.clear();
        ClientSoundManager.getInstance().playButtonClick();
    }

    @Override
    public void onStartDelete(ConfigFile cfg) {
        confirmDeleteName = cfg.getFileName();
        editingName = null;
        ClientSoundManager.getInstance().playButtonClick();
    }

    @Override
    public void onConfirmDelete(ConfigFile cfg) {
        cfg.delete();
        confirmDeleteName = null;
        ClientSoundManager.getInstance().playButtonClick();
    }

    @Override
    public void onCancelDelete() {
        confirmDeleteName = null;
        ClientSoundManager.getInstance().playButtonClick();
    }

    private void confirmCreate() {
        String typed = createField.getBuiltText().trim();
        String name = typed.isEmpty() ? generateDefaultName() : typed;
        if (name.equalsIgnoreCase("autosave")) return;

        // 1. Сохраняем текущий конфиг перед созданием нового
        ConfigFile current = manager().getCurrent();
        if (current != null && !current.getFileName().equalsIgnoreCase("autosave")) {
            current.save();
        }

        // 2. Сбрасываем все модули и настройки к дефолтным (чистый лист)
        Sun.getInstance().getModuleManager().resetToDefaults();

        // 3. Создаём новый конфиг с чистыми настройками и активируем его
        ConfigFile existing = manager().getConfig(name, true);
        if (existing != null) {
            manager().setCurrent(existing);
            existing.save();
        } else {
            ConfigFile newCfg = manager().createConfig(name);
            if (newCfg != null) {
                manager().setCurrent(newCfg);
            }
        }
        Sun.getInstance().getFileManager().saveClientFiles();

        createField.clear();
        creatingNew = false;
        ClientSoundManager.getInstance().playButtonClick();
    }

    private String generateDefaultName() {
        String base = Localizator.translate("menu.gui_settings.configs.default_name");
        if (manager().getConfig(base, false) == null) {
            return base;
        }
        int n = 2;
        while (manager().getConfig(base + " " + n, false) != null) {
            n++;
        }
        return base + " " + n;
    }

    private void openConfigsFolderAsync() {
        Thread t = new Thread(() -> manager().directionConfig(), "sun-configs-folder");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
        boolean enter = keyCode == 257 || keyCode == 335;
        if (creatingNew) {
            if (enter) confirmCreate();
            else if (keyCode == 256) { creatingNew = false; createField.clear(); }
            else createField.onKeyPressed(keyCode, scanCode, modifiers);
            return;
        }
        if (editingName != null) {
            if (enter) onConfirmRename();
            else if (keyCode == 256) onCancelRename();
            else editField.onKeyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (creatingNew) return createField.charTyped(chr, modifiers);
        if (editingName != null) return editField.charTyped(chr, modifiers);
        return false;
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        createField.onMouseReleased(mouseX, mouseY, button);
        editField.onMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public float getContentHeight() {
        float h = SETTINGS_SEP_Y;
        ConfigFile current = manager().getCurrent();
        if (current != null && !current.getFileName().equalsIgnoreCase("autosave")) {
            h += HERO_CARD_H + GAP_Y;
        }

        h += (creatingNew ? CREATE_BOX_H : ACTION_BAR_H) + GAP_Y;

        Font secFont = Fonts.MEDIUM.getFont(6.5F);
        h += secFont.height() + 5.0F;

        List<ConfigFile> configs = customConfigs();
        if (configs.isEmpty()) {
            h += 24.0F;
        } else {
            h += configs.size() * (ConfigItemRow.ROW_HEIGHT + ROW_GAP);
        }

        h += CARD_BOTTOM_PAD;
        return h;
    }
}