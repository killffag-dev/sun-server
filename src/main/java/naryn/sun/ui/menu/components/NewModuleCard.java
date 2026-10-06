package naryn.sun.ui.menu.components;

import naryn.sun.Sun;
import naryn.sun.framework.base.CustomComponent;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.modules.Module;
import naryn.sun.systems.setting.Setting;
import naryn.sun.ui.menu.dropdown.components.settings.MenuSettingComponent;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.animation.types.ColorAnimation;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.TextUtility;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.render.ScissorUtility;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.animation.ClientAnimationConfig;
import net.minecraft.util.math.RotationAxis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewModuleCard extends CustomComponent {

    private final Module module;

    private final Animation hoverAnimation   = new Animation(250L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private final Animation enableAnimation  = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private final Animation expandAnimation  = new Animation(300L, 0.0F, Easing.QUARTIC_OUT);
    private final Animation starAnimation    = new Animation(250L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private final Animation starPressAnim    = new Animation(350L, 1.0F, Easing.BACK_OUT);
    private final Animation starSpinAnim     = new Animation(400L, 1.0F, Easing.QUARTIC_OUT);
    private final Animation starBurstAnim    = new Animation(450L, 1.0F, Easing.QUARTIC_OUT);
    private final ColorAnimation toggleColor = new ColorAnimation(300L, new ColorRGBA(255, 255, 255, 13), Easing.FIGMA_EASE_IN_OUT);

    private boolean expanded    = false;
    private boolean bindingMode = false;

    // Отрендеренный (упорядоченный) список компонентов текущего кадра — пересобирается
    // из module.getSettings() при каждом sync'е, но сами объекты компонентов переиспользуются
    // через componentBySetting, чтобы не терять их внутреннее состояние (текст в поле,
    // режим бинда и т.п.) при каждой пересборке.
    private final List<MenuSettingComponent> settingComponents = new ArrayList<>();
    // Кэш уже созданных компонентов по объекту Setting. Нужен модулям с динамическим
    // списком настроек (например CommandBind, который добавляет/удаляет Setting'и в рантайме
    // через addEntry()/removeEntry()) — без этого свежедобавленные настройки не попадают
    // в UI, пока карточка не будет полностью пересоздана заново.
    private final Map<Setting, MenuSettingComponent> componentBySetting = new HashMap<>();

    private static final float CARD_PADDING_X  = 11.0F;
    private static final float CARD_PADDING_Y  = 9.0F;
    private static final float BASE_H          = 46.0F;
    private static final float SETTINGS_SEP_Y  = 7.0F;
    private static final float TOGGLE_W        = 23.0F;
    private static final float TOGGLE_H        = 12.0F;
    private static final float TOGGLE_KNOB     = 8.0F;
    private static final float STAR_W          = 12.0F;
    private static final float STAR_H          = 14.0F;
    private static final float TOGGLE_Y_OFFSET = 17.0F;

    public NewModuleCard(Module module) {
        this.module = module;
    }

    @Override
    public void onInit() {
        syncSettingComponents();
        super.onInit();
    }

    @Override
    public void update(UIContext context) {
        // Дёшево для обычных модулей (список настроек не меняется — просто пересобирает
        // тот же порядок из кэша), но обязательно для модулей вроде CommandBind, у которых
        // module.getSettings() растёт/уменьшается прямо во время открытого меню.
        syncSettingComponents();
        super.update(context);
    }

    /**
     * Приводит {@link #settingComponents} в соответствие с текущим {@code module.getSettings()}.
     * Компоненты для уже существующих Setting'ов переиспользуются из {@link #componentBySetting}
     * (не теряют состояние — введённый текст, режим ожидания бинда и т.п.), для новых Setting'ов
     * создаются через {@link GuiUtility#settinge}, для удалённых — выбрасываются из кэша.
     */
    private void syncSettingComponents() {
        List<Setting> current = module.getSettings();

        settingComponents.clear();
        for (Setting setting : current) {
            MenuSettingComponent comp = componentBySetting.get(setting);
            if (comp == null) {
                comp = GuiUtility.settinge(setting, this);
                if (comp == null) continue;
                componentBySetting.put(setting, comp);
            }
            settingComponents.add(comp);
        }
        componentBySetting.keySet().retainAll(current);
    }

    private float getHeaderHeight() {
        return BASE_H;
    }

    private float getToggleY() {
        return y + TOGGLE_Y_OFFSET;
    }

    @Override
    protected void renderComponent(UIContext context) {
        this.height = getHeight();
        boolean enabled = module.isEnabled();
        float headerHeight = getHeaderHeight();

        enableAnimation.update(enabled ? 1.0F : 0.0F);
        hoverAnimation.update(isHovered(context.getMouseX(), context.getMouseY()));
        expandAnimation.setEasing(expanded ? Easing.QUARTIC_OUT : Easing.FIGMA_EASE_IN_OUT);
        expandAnimation.update(expanded ? 1.0F : 0.0F);
        starAnimation.update(module.isFavorite() ? 1.0F : 0.0F);
        starPressAnim.update(1.0F);
        starSpinAnim.update(1.0F);
        starBurstAnim.update(1.0F);

        toggleColor.update(enabled
                ? Colors.ACCENT.withAlpha(230.0F)
                : new ColorRGBA(255, 255, 255, 13));

        float hover  = hoverAnimation.getValue();
        float expand = expandAnimation.getValue();

        // 2.1 Плавное приподнятие карточки при наведении
        float liftY = ClientAnimationConfig.getInstance().isButtonHoverLift() ? -2.0F * hover : 0.0F;
        boolean lifted = Math.abs(liftY) > 0.01F;
        if (lifted) {
            context.getMatrices().push();
            context.getMatrices().translate(0.0F, liftY, 0.0F);
        }

        try {
            MenuSkin skin = MenuSkin.current();
            skin.renderCard(context, x, y, width, height, BorderRadius.all(8.0F),
                    enabled, enableAnimation.getValue(), hover, 1.0F);

            // 4. Заметная и эффектная анимация звёздочки
            float starScale = starPressAnim.getValue();
            float starCenterX = x + CARD_PADDING_X + STAR_W / 2.0F;
            float starCenterY = y + headerHeight / 2.0F;

            boolean starEffect = ClientAnimationConfig.getInstance().isStarEffect();

            // 4.1 Свечение убрано — показывается только сама звёздочка

            // 4.2 Искры при добавлении в избранное (sparkle burst)
            if (starEffect) {
                float burst = starBurstAnim.getValue();
                if (burst > 0.0F && burst < 0.99F) {
                    float burstDist = 5.0F + 11.0F * burst;
                    float pAlpha = (1.0F - burst) * 0.95F;
                    ColorRGBA sparkleCol = new ColorRGBA(253, 224, 71, (int)(255 * pAlpha));
                    for (int p = 0; p < 6; p++) {
                        double pAngle = (p * Math.PI / 3.0) + (burst * 0.4);
                        float px = (float)(starCenterX + Math.cos(pAngle) * burstDist);
                        float py = (float)(starCenterY + Math.sin(pAngle) * burstDist);
                        float sz = Math.max(0.4F, (1.0F - burst * 0.7F) * 1.2F);
                        context.drawRoundedRect(px - sz, py - sz, sz * 2.0F, sz * 2.0F, BorderRadius.all(sz), sparkleCol);
                    }
                }
            }

            // 4.3 Сама звёздочка с вращением
            ColorRGBA starColor = new ColorRGBA(71, 85, 105, 255)
                    .mix(new ColorRGBA(251, 191, 36, 255), starAnimation.getValue());

            if (starEffect && starSpinAnim.getValue() < 0.99F) {
                context.getMatrices().push();
                context.getMatrices().translate(starCenterX, starCenterY, 0.0F);
                float rot = (1.0F - starSpinAnim.getValue()) * -144.0F;
                context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rot));
                context.getMatrices().translate(-starCenterX, -starCenterY, 0.0F);
                context.drawStar(starCenterX, starCenterY, 4.5F * starScale, 1.9F * starScale, starColor);
                context.getMatrices().pop();
            } else {
                context.drawStar(starCenterX, starCenterY, 4.5F * starScale, 1.9F * starScale, starColor);
            }

        // Название
        float nameAlpha = 0.75F + 0.25F * enableAnimation.getValue();
        Font nameFont = Fonts.MEDIUM.getFont(7.0F);
        context.drawText(nameFont, module.getDisplayName(),
                x + CARD_PADDING_X + STAR_W + 9.0F,
                y + (headerHeight - nameFont.height()) / 2.0F,
                Colors.getTextColor().withAlpha((int)(255 * nameAlpha)));

        // Toggle pill
        float toggleX = x + width - CARD_PADDING_X - TOGGLE_W;
        float toggleY = getToggleY();

        float eVal = enableAnimation.getValue();
        skin.renderToggleTrack(context, toggleX, toggleY, TOGGLE_W, TOGGLE_H,
                BorderRadius.all(TOGGLE_H / 2.0F), enabled, eVal, 1.0F);

        float stretch = ClientAnimationConfig.getInstance().isTogglePhysics()
                ? (float) Math.sin(eVal * Math.PI) * 2.5F : 0.0F;
        float knobW = TOGGLE_KNOB + stretch;
        float knobX = toggleX + 2.0F + (TOGGLE_W - TOGGLE_KNOB - 4.0F) * eVal - (stretch * (eVal > 0.5F ? 0.7F : 0.3F));
        float knobY = toggleY + (TOGGLE_H - TOGGLE_KNOB) / 2.0F;
        skin.renderToggleThumb(context, knobX, knobY, knobW, TOGGLE_KNOB, eVal, 1.0F);

        // Раскрытые настройки
        if (expand > 0.0F) {
            float sepY = y + headerHeight - 1.0F;
            context.drawRect(x + CARD_PADDING_X, sepY, width - CARD_PADDING_X * 2, 0.5F,
                    Colors.getTextColor().withAlpha((int)(10 * expand)));

            float settingsClipH = getSettingsHeight() * expand;
            ScissorUtility.push(context.getMatrices(),
                    x, y + headerHeight, width, settingsClipH);

            float settY = sepY + SETTINGS_SEP_Y;

            int key = module.getKey();
            String bindLabel = bindingMode
                    ? Localizator.translate("menu.module.keybind.press")
                    : (key == -1 ? Localizator.translate("menu.module.keybind.none") : TextUtility.getKeyName(key));

            context.drawText(Fonts.REGULAR.getFont(6.0F), Localizator.translate("menu.module.keybind"),
                    x + CARD_PADDING_X,
                    settY + 4.0F,
                    ColorRGBA.WHITE.withAlpha((int)(200 * expand)));

            float bindPillW = Fonts.REGULAR.getFont(6.0F).width(bindLabel) + 12.0F;
            float bindPillX = x + width - CARD_PADDING_X - bindPillW;

            skin.renderBindPill(context, bindPillX, settY, bindPillW, 14.0F,
                    BorderRadius.all(3.5F), bindingMode, expand);

            context.drawText(Fonts.REGULAR.getFont(6.0F), bindLabel,
                    bindPillX + 6.0F, settY + 4.0F,
                    ColorRGBA.WHITE.withAlpha((int)(225 * expand)));

            settY += 18.0F;

            for (MenuSettingComponent comp : settingComponents) {
                comp.getVisibilityAnimation().update(comp.getSetting().isVisible() ? 1.0F : 0.0F);
                comp.setX(x);
                comp.setY(settY);
                comp.setWidth(width);

                float rowVisibleH = comp.getHeight() * comp.getOpacity();
                if (rowVisibleH > 0.5F) {
                    ScissorUtility.push(context.getMatrices(), x, settY, width, rowVisibleH);
                    comp.render(context);
                    ScissorUtility.pop();
                }
                settY += rowVisibleH;
            }

            ScissorUtility.pop();
        }

        if (isHovered(context.getMouseX(), context.getMouseY())) {
            CursorUtility.set(CursorType.HAND);
        }
        } finally {
            if (lifted) {
                context.getMatrices().pop();
            }
        }
    }

    private float getSettingsHeight() {
        float h = 18.0F;
        for (MenuSettingComponent comp : settingComponents) {
            h += comp.getHeight() * comp.getOpacity();
        }
        return h + SETTINGS_SEP_Y * 2;
    }

    @Override
    public float getHeight() {
        if (!expanded && expandAnimation.getValue() == 0.0F) return getHeaderHeight();
        return getHeaderHeight() + getSettingsHeight() * expandAnimation.getValue();
    }

    public boolean isExpandedVisually() {
        return expanded || expandAnimation.getValue() > 0.0F;
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (!isHovered(mouseX, mouseY)) return;

        float headerHeight = getHeaderHeight();

        if (button == MouseButton.LEFT) {
            if (GuiUtility.isHovered(x + CARD_PADDING_X, y + CARD_PADDING_Y + 3.0,
                    STAR_W, STAR_H, mouseX, mouseY)) {
                boolean fav = !module.isFavorite();
                module.setFavorite(fav);
                if (fav) {
                    starPressAnim.setValue(0.35F);
                    starSpinAnim.setValue(0.0F);
                    starBurstAnim.setValue(0.0F);
                } else {
                    starPressAnim.setValue(0.65F);
                }
                return;
            }

            float toggleX = x + width - CARD_PADDING_X - TOGGLE_W;
            float toggleY = getToggleY();
            if (GuiUtility.isHovered(toggleX, toggleY, TOGGLE_W, TOGGLE_H, mouseX, mouseY)) {
                module.toggle();
                return;
            }

            if (expanded && expandAnimation.getValue() > 0.5F) {
                float bindRowY = y + headerHeight - 1.0F + SETTINGS_SEP_Y;
                String bindText = bindingMode
                        ? Localizator.translate("menu.module.keybind.press")
                        : (module.getKey() == -1 ? Localizator.translate("menu.module.keybind.none") : TextUtility.getKeyName(module.getKey()));
                float bindPillW = Fonts.REGULAR.getFont(6.0F).width(bindText) + 12.0F;
                float bindPillX = x + width - CARD_PADDING_X - bindPillW;
                if (GuiUtility.isHovered(bindPillX, bindRowY, bindPillW, 14.0, mouseX, mouseY)) {
                    bindingMode = true;
                    return;
                }
            }

            float settingsStartY = y + headerHeight;
            if (expanded && mouseY >= settingsStartY) {
                for (MenuSettingComponent comp : settingComponents) {
                    if (comp.getOpacity() > 0.0F) {
                        comp.onMouseClicked(mouseX, mouseY, button);
                    }
                }
                return;
            }

            if (GuiUtility.isHovered(x, y, width, headerHeight, mouseX, mouseY)) {
                module.toggle();
            }

        } else if (button == MouseButton.RIGHT) {
            if (GuiUtility.isHovered(x, y, width, headerHeight, mouseX, mouseY)) {
                expanded = !expanded;
                return;
            }
            if (expanded && mouseY >= y + headerHeight) {
                for (MenuSettingComponent comp : settingComponents) {
                    if (comp.getOpacity() > 0.0F) {
                        comp.onMouseClicked(mouseX, mouseY, button);
                    }
                }
            }
        }
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        if (expanded) {
            for (MenuSettingComponent comp : settingComponents) {
                if (comp.getOpacity() > 0.0F) {
                    comp.onMouseReleased(mouseX, mouseY, button);
                }
            }
        }
        super.onMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onScroll(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (expanded && isHovered(mouseX, mouseY)) {
            for (MenuSettingComponent comp : settingComponents) {
                comp.onScroll(mouseX, mouseY, hAmount, vAmount);
            }
        }
    }

    @Override
    public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindingMode) {
            int newKey = (keyCode == 256 || keyCode == 261) ? -1 : keyCode;

            if (newKey != -1) {
                for (Module m : Sun.getInstance().getModuleManager().getModules()) {
                    if (m != module && m.getKey() == newKey) {
                        m.setKey(-1);
                    }
                }
            }

            module.setKey(newKey);
            bindingMode = false;
            return;
        }
        if (expanded) {
            for (MenuSettingComponent comp : settingComponents) {
                if (comp.getOpacity() > 0.0F) {
                    comp.onKeyPressed(keyCode, scanCode, modifiers);
                }
            }
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (expanded) {
            for (MenuSettingComponent comp : settingComponents) {
                if (comp.getOpacity() > 0.0F) {
                    comp.charTyped(chr, modifiers);
                }
            }
        }
        return super.charTyped(chr, modifiers);
    }

    public boolean isFavorite()    { return module.isFavorite(); }
    public boolean isBindingMode()   { return bindingMode; }
    public Module getModule()        { return module; }

    public String getHoveredTooltip(float mouseX, float mouseY) {
        if (isExpandedVisually()) {
            for (MenuSettingComponent<?> comp : settingComponents) {
                if (comp.getOpacity() > 0.5F && comp.isHovered(mouseX, mouseY)) {
                    String desc = comp.getSetting().getDescription();
                    if (desc != null && !desc.isEmpty()) {
                        return desc;
                    }
                }
            }
        }
        if (isHovered(mouseX, mouseY)) {
            return module.getDescription();
        }
        return "";
    }
}