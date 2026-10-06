package naryn.sun.systems.modules.modules.visuals;

import java.util.ArrayList;
import java.util.List;
import naryn.sun.framework.base.CustomDrawContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.HudRenderEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.PositionableHudModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.interfaces.IMinecraft;
import net.minecraft.item.ItemStack;

@ModuleInfo(name = "Armor Status", category = ModuleCategory.VISUALS, desc = "Отображает прочность брони и предмета в руке")
public class ArmorStatus extends PositionableHudModule implements IMinecraft {

    private final BooleanSetting background = new BooleanSetting(this, "hud.background").enable();

    // головa, грудь, ноги, ботинки
    private static final int[] ARMOR_SLOTS = {39, 38, 37, 36};
    private static final float ICON = 16.0F;
    private static final float GAP  = 4.0F;
    private static final float PAD  = 6.0F;

    private final EventListener<HudRenderEvent> onHudRender = event -> this.draw(event.getContext());

    @Override
    public void renderPreview(CustomDrawContext context) {
        this.draw(context);
    }

    private void draw(CustomDrawContext context) {
        if (mc.player == null) return;

        boolean editing = PositionableHudModule.isEditingActive();

        List<ItemStack> stacks = new ArrayList<>();
        for (int slot : ARMOR_SLOTS) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (!stack.isEmpty()) stacks.add(stack);
        }
        ItemStack mainHand = mc.player.getMainHandStack();
        if (!mainHand.isEmpty()) stacks.add(mainHand);

        if (stacks.isEmpty() && !editing) return;

        Font font = Fonts.REGULAR.getFont(7.0F);
        int count = editing && stacks.isEmpty() ? 1 : stacks.size();
        float width  = PAD * 2 + ICON + 26.0F;
        float height = PAD * 2 + count * (ICON + GAP) - GAP;

        float sw = mc.getWindow().getScaledWidth();
        float x = this.resolveX(sw - width - 20.0F);
        float y = this.resolveY(20.0F);

        this.beginScaledRender(context, width, height);

        if (editing) {
            context.drawRoundedRect(x, y, width, height, BorderRadius.all(6.0F), Colors.getBackgroundColor().withAlpha(200.0F));
        } else if (this.background.isEnabled()) {
            context.drawClientRect(x, y, width, height, 1.0F, 0.0F, 1.0F);
        }

        float rowY = y + PAD;
        for (ItemStack stack : stacks) {
            rowY = this.drawItemRow(context, font, x + PAD, rowY, stack);
        }

        this.endScaledRender(context);
    }

    private float drawItemRow(CustomDrawContext context, Font font, float x, float y, ItemStack stack) {
        context.drawItem(stack, x, y, 1.0F);
        if (stack.isDamageable()) {
            int max = stack.getMaxDamage();
            int remaining = max - stack.getDamage();
            String text = String.valueOf(remaining);
            ColorRGBA color = this.getDurabilityColor(remaining, max);
            context.drawText(font, text, x + ICON + 4.0F, y + (ICON - font.height()) / 2.0F, color);
        }
        return y + ICON + GAP;
    }

    private ColorRGBA getDurabilityColor(int remaining, int max) {
        float ratio = (float) remaining / max;
        if (ratio > 0.5F) return new ColorRGBA(80, 220, 120, 255);
        if (ratio > 0.2F) return new ColorRGBA(230, 200, 70, 255);
        return new ColorRGBA(220, 70, 70, 255);
    }
}