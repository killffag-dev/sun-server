package naryn.sun.ui.menu.components;

import naryn.sun.framework.base.CustomComponent;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.animation.ClientAnimationConfig;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.ui.menu.components.settings.AppearanceRenderer;
import naryn.sun.ui.menu.components.settings.AnimationsRenderer;
import naryn.sun.ui.menu.components.settings.ConfigsRenderer;
import naryn.sun.ui.menu.components.settings.LanguageRenderer;
import naryn.sun.ui.menu.components.settings.SettingRenderer;
import naryn.sun.ui.menu.components.settings.SoundsRenderer;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.render.ScissorUtility;

/**
 * Карточка настройки GUI Settings, аналогичная NewModuleCard.
 * В свёрнутом виде — название по центру (горизонтально и вертикально).
 * ПКМ раскрывает настройки внутри со стандартными компонентами Modules.
 *
 * Логика рендеринга и обработки кликов каждого типа настроек
 * делегирована в {@link SettingRenderer} и его наследники.
 */
public class GuiSettingCard extends CustomComponent {

    private final GuiSettingType type;
    private final SettingRenderer renderer;

    private final Animation hoverAnimation  = new Animation(250L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private final Animation expandAnimation = new Animation(300L, 0.0F, Easing.QUARTIC_OUT);

    private boolean expanded = false;

    private static final float HEADER_H       = 46.0F;
    private static final float SETTINGS_SEP_Y = 7.0F;

    public GuiSettingCard(GuiSettingType type) {
        this.type = type;
        this.renderer = createRenderer(type);
    }

    private static SettingRenderer createRenderer(GuiSettingType type) {
        return switch (type) {
            case APPEARANCE -> new AppearanceRenderer();
            case BACKGROUND -> new naryn.sun.ui.menu.components.settings.BackgroundSettingRenderer();
            case PALETTE    -> new naryn.sun.ui.menu.components.settings.PaletteRenderer();
            case LANGUAGE   -> new LanguageRenderer();
            case SOUNDS     -> new SoundsRenderer();
            case ANIMATIONS -> new AnimationsRenderer();
            case CONFIGS    -> new ConfigsRenderer();
            case JVM_PRESETS -> new naryn.sun.ui.menu.components.settings.JvmPresetsRenderer();
            case CREDITS    -> new naryn.sun.ui.menu.components.settings.CreditsRenderer();
        };
    }

    public GuiSettingType getType() {
        return type;
    }

    public boolean isExpandedVisually() {
        return expanded || expandAnimation.getValue() > 0.0F;
    }

    @Override
    protected void renderComponent(UIContext context) {
        this.height = getHeight();

        hoverAnimation.update(isHovered(context.getMouseX(), context.getMouseY()));
        expandAnimation.setEasing(expanded ? Easing.QUARTIC_OUT : Easing.FIGMA_EASE_IN_OUT);
        expandAnimation.update(expanded ? 1.0F : 0.0F);

        float hover  = hoverAnimation.getValue();
        float expand = expandAnimation.getValue();

        // Поднятие карточки при наведении
        float liftY = ClientAnimationConfig.getInstance().isButtonHoverLift() ? -2.0F * hover : 0.0F;
        boolean lifted = Math.abs(liftY) > 0.01F;
        if (lifted) {
            context.getMatrices().push();
            context.getMatrices().translate(0.0F, liftY, 0.0F);
        }

        try {
            // 1. Фон карточки
            MenuSkin skin = MenuSkin.current();
            skin.renderCard(context, x, y, width, height, BorderRadius.all(8.0F),
                    false, 0.0F, hover, 1.0F);

            // 2. Название карточки по центру
            Font nameFont = Fonts.MEDIUM.getFont(7.0F);
            float titleCenterY = y + (HEADER_H - nameFont.height()) / 2.0F;
            float titleCenterX = x + width / 2.0F;

            context.drawCenteredText(nameFont, type.getDisplayName(),
                    titleCenterX, titleCenterY,
                    Colors.getTextColor().withAlpha((int)(255 * (0.80F + 0.20F * hover))));

            // 3. Раскрытые настройки — делегируем в renderer
            if (expand > 0.0F) {
                float sepY = y + HEADER_H - 1.0F;
                context.drawRect(x + 11.0F, sepY, width - 22.0F, 0.5F,
                        Colors.getTextColor().withAlpha((int)(10 * expand)));

                float settingsClipH = renderer.getContentHeight() * expand;
                ScissorUtility.push(context.getMatrices(), x, y + HEADER_H, width, settingsClipH);

                float settY = sepY + SETTINGS_SEP_Y;
                renderer.render(context, x, y, width, settY, expand);

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

    @Override
    public float getHeight() {
        if (!expanded && expandAnimation.getValue() == 0.0F) return HEADER_H;
        return HEADER_H + renderer.getContentHeight() * expandAnimation.getValue();
    }

    // ==================== INPUT ====================

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (!isHovered(mouseX, mouseY)) return;

        // ПКМ, ЛКМ, СКМ: раскрыть/свернуть при клике по шапке карточки
        if (GuiUtility.isHovered(x, y, width, HEADER_H, mouseX, mouseY)) {
            if (button == MouseButton.RIGHT || button == MouseButton.LEFT || button == MouseButton.MIDDLE) {
                expanded = !expanded;
                return;
            }
        }

        if (button == MouseButton.LEFT && expanded && expandAnimation.getValue() > 0.5F) {
            float settY = y + HEADER_H - 1.0F + SETTINGS_SEP_Y;
            renderer.handleClick(x, y, width, settY, mouseX, mouseY);
        }
    }

    public void onMouseDragged(double mouseX, double mouseY) {
        renderer.onMouseDragged(x, width, mouseX, mouseY);
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        renderer.onMouseReleased(mouseX, mouseY, button);
        super.onMouseReleased(mouseX, mouseY, button);
    }

    public boolean isDragging() {
        return renderer.isDragging();
    }

    /** Клавиатурный ввод — доходит только до раскрытых карточек (для текстовых полей внутри renderer'а). */
    public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (isExpandedVisually()) {
            renderer.onKeyPressed(keyCode, scanCode, modifiers);
        }
    }

    /** Ввод символа — доходит только до раскрытых карточек. */
    public boolean charTyped(char chr, int modifiers) {
        if (isExpandedVisually()) {
            return renderer.charTyped(chr, modifiers);
        }
        return false;
    }
}