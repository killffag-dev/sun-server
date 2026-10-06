package naryn.sun.ui.menu.dropdown.components.settings.impl;

import naryn.sun.framework.base.CustomComponent;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.setting.Setting;
import naryn.sun.systems.setting.settings.GroupSetting;
import naryn.sun.ui.menu.dropdown.components.settings.MenuSettingComponent;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.render.ScissorUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupSettingComponent extends MenuSettingComponent<GroupSetting> {

    private final List<MenuSettingComponent<?>> childComponents = new ArrayList<>();
    private final Map<Setting, MenuSettingComponent<?>> componentBySetting = new HashMap<>();

    private static final float HEADER_HEIGHT = 17.0F;
    private static final float BOX_PAD_X = 8.0F;
    private static final float BOX_PAD_Y = 4.0F;
    private static final float BOTTOM_GAP = 8.0F;

    public GroupSettingComponent(GroupSetting setting, CustomComponent parent) {
        super(setting, parent);
    }

    @Override
    public void onInit() {
        syncChildren();
        super.onInit();
    }

    @Override
    public void update(UIContext context) {
        syncChildren();
        float boxX = this.x + BOX_PAD_X;
        float boxW = this.width - BOX_PAD_X * 2.0F;
        float boxTop = this.y + HEADER_HEIGHT;
        float curY = boxTop + BOX_PAD_Y;
        for (MenuSettingComponent<?> child : childComponents) {
            child.getVisibilityAnimation().update(child.getSetting().isVisible() ? 1.0F : 0.0F);
            child.setX(boxX);
            child.setWidth(boxW);
            child.setY(curY);
            child.update(context);
            curY += child.getHeight() * child.getOpacity();
        }
        super.update(context);
    }

    private void syncChildren() {
        List<Setting> current = this.setting.getSettings();
        childComponents.clear();
        for (Setting s : current) {
            MenuSettingComponent<?> comp = componentBySetting.get(s);
            if (comp == null) {
                comp = GuiUtility.settinge(s, this);
                if (comp == null) continue;
                componentBySetting.put(s, comp);
            }
            childComponents.add(comp);
        }
        componentBySetting.keySet().retainAll(current);
    }

    private float getInnerChildrenHeight() {
        float h = 0.0F;
        for (MenuSettingComponent<?> child : childComponents) {
            h += child.getHeight() * child.getOpacity();
        }
        return h;
    }

    @Override
    public float getHeight() {
        float innerH = getInnerChildrenHeight();
        float boxH = innerH > 0.5F ? (innerH + BOX_PAD_Y * 2.0F) : 0.0F;
        return (HEADER_HEIGHT + boxH + BOTTOM_GAP) * getOpacity();
    }

    @Override
    protected void renderComponent(UIContext context) {
        float opacity = getOpacity();
        if (opacity <= 0.01F) return;

        // 1. Заголовок группы
        Font titleFont = Fonts.REGULAR.getFont(8.0F);
        String title = Localizator.translate(this.setting.getName());
        context.drawText(titleFont, title, this.x + 10.0F, this.y + 4.0F,
                Colors.getTextColor().withAlpha((int)(220 * opacity)));

        float innerH = getInnerChildrenHeight();
        if (innerH <= 0.5F) return;

        // 2. Рамка и подложка бокса
        float boxX = this.x + BOX_PAD_X;
        float boxW = this.width - BOX_PAD_X * 2.0F;
        float boxTop = this.y + HEADER_HEIGHT;
        float boxH = innerH + BOX_PAD_Y * 2.0F;

        MenuSkin.current().renderSettingBox(
                context, boxX, boxTop, boxW, boxH,
                BorderRadius.all(6.0F), opacity
        );

        // 3. Отрисовка вложенных компонентов внутри бокса
        float curY = boxTop + BOX_PAD_Y;
        for (MenuSettingComponent<?> child : childComponents) {
            child.getVisibilityAnimation().update(child.getSetting().isVisible() ? 1.0F : 0.0F);
            child.setX(boxX);
            child.setWidth(boxW);
            child.setY(curY);

            float rowH = child.getHeight() * child.getOpacity();
            if (rowH > 0.5F) {
                ScissorUtility.push(context.getMatrices(), boxX, curY, boxW, rowH);
                child.render(context);
                ScissorUtility.pop();
            }
            curY += rowH;
        }
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        for (MenuSettingComponent<?> child : childComponents) {
            if (child.getOpacity() > 0.0F) {
                child.onMouseClicked(mouseX, mouseY, button);
            }
        }
        super.onMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        for (MenuSettingComponent<?> child : childComponents) {
            if (child.getOpacity() > 0.0F) {
                child.onMouseReleased(mouseX, mouseY, button);
            }
        }
        super.onMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onScroll(double mouseX, double mouseY, double hAmount, double vAmount) {
        for (MenuSettingComponent<?> child : childComponents) {
            child.onScroll(mouseX, mouseY, hAmount, vAmount);
        }
        super.onScroll(mouseX, mouseY, hAmount, vAmount);
    }

    @Override
    public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
        for (MenuSettingComponent<?> child : childComponents) {
            if (child.getOpacity() > 0.0F) {
                child.onKeyPressed(keyCode, scanCode, modifiers);
            }
        }
        super.onKeyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        boolean handled = false;
        for (MenuSettingComponent<?> child : childComponents) {
            if (child.getOpacity() > 0.0F) {
                handled |= child.charTyped(chr, modifiers);
            }
        }
        return handled || super.charTyped(chr, modifiers);
    }

    public String getHoveredTooltip(float mouseX, float mouseY) {
        for (MenuSettingComponent<?> child : childComponents) {
            if (child.getOpacity() > 0.5F && child.isHovered(mouseX, mouseY)) {
                String desc = child.getSetting().getDescription();
                if (desc != null && !desc.isEmpty()) {
                    return desc;
                }
            }
        }
        return null;
    }
}
