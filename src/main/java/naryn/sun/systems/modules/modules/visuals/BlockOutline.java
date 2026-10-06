package naryn.sun.systems.modules.modules.visuals;

import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.Render3DEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.modules.modules.visuals.blockoutline.BlockOutlineRenderer;
import naryn.sun.systems.modules.modules.visuals.blockoutline.BlockOutlineState;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.ColorSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;

@ModuleInfo(name = "Block Outline", category = ModuleCategory.VISUALS, desc = "modules.descriptions.block_outline")
public class BlockOutline extends BaseModule {

    // ===== 1. Выбор режимов =====
    private final ModeSetting mode = new ModeSetting(this, "modules.settings.block_outline.mode");
    private final ModeSetting.Value modeOutline = new ModeSetting.Value(this.mode, "modules.settings.block_outline.mode.outline").select();
    private final ModeSetting.Value modeFill = new ModeSetting.Value(this.mode, "modules.settings.block_outline.mode.fill");
    private final ModeSetting.Value modeOutlineFill = new ModeSetting.Value(this.mode, "modules.settings.block_outline.mode.outline_fill");
    private final ModeSetting.Value modeCorners = new ModeSetting.Value(this.mode, "modules.settings.block_outline.mode.corners");

    private final ModeSetting shape = new ModeSetting(this, "modules.settings.block_outline.shape");
    private final ModeSetting.Value shapeVoxel = new ModeSetting.Value(this.shape, "modules.settings.block_outline.shape.voxel").select();
    private final ModeSetting.Value shapeBox = new ModeSetting.Value(this.shape, "modules.settings.block_outline.shape.box");

    private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.block_outline.color_mode");
    private final ModeSetting.Value colorCustom = new ModeSetting.Value(this.colorMode, "modules.settings.block_outline.color_mode.custom").select();
    private final ModeSetting.Value colorRainbow = new ModeSetting.Value(this.colorMode, "modules.settings.block_outline.color_mode.rainbow");

    // ===== 2. Цвет =====
    private final ColorSetting customColor = new ColorSetting(this, "modules.settings.block_outline.custom_color", () -> !this.colorMode.is(this.colorCustom))
        .color(Colors.ACCENT);

    // ===== 3. Тумблеры =====
    private final BooleanSetting depthBuffer = new BooleanSetting(this, "modules.settings.block_outline.depth_buffer").enabled(true);
    private final BooleanSetting smooth = new BooleanSetting(this, "modules.settings.block_outline.smooth").enabled(true);

    // ===== 4. Ползунки =====
    private final SliderSetting lineWidth = new SliderSetting(this, "modules.settings.block_outline.line_width", () -> this.mode.is(this.modeFill))
        .min(1.0F).max(10.0F).step(0.5F).currentValue(2.5F);

    private final SliderSetting fillAlpha = new SliderSetting(this, "modules.settings.block_outline.fill_alpha", () -> this.mode.is(this.modeOutline) || this.mode.is(this.modeCorners))
        .min(5.0F).max(100.0F).step(5.0F).currentValue(30.0F).suffix("%");

    private static final float SMOOTH_SPEED = 20.0F;

    private final BlockOutlineState state = new BlockOutlineState();

    private final EventListener<Render3DEvent> onRender3D = event -> {
        if (mc.player == null || mc.world == null) {
            return;
        }

        this.state.update(
            mc,
            this.smooth.isEnabled(),
            SMOOTH_SPEED
        );

        ColorRGBA activeColor = getActiveColor();

        String selectedMode = "Outline";
        if (this.mode.is(this.modeFill)) {
            selectedMode = "Fill";
        } else if (this.mode.is(this.modeOutlineFill)) {
            selectedMode = "Outline & Fill";
        } else if (this.mode.is(this.modeCorners)) {
            selectedMode = "Corners";
        }

        String selectedShape = this.shape.is(this.shapeBox) ? "Box" : "Voxel";

        BlockOutlineRenderer.render(
            event.getMatrices(),
            this.state,
            event.getCamera(),
            this.depthBuffer.isEnabled(),
            selectedMode,
            selectedShape,
            this.lineWidth.getCurrentValue(),
            this.fillAlpha.getCurrentValue(),
            activeColor
        );
    };

    private ColorRGBA getActiveColor() {
        if (this.colorMode.is(this.colorRainbow)) {
            float hue = (System.currentTimeMillis() % 4000L) / 4000.0F;
            return ColorRGBA.fromHSB(hue, 0.85F, 1.0F);
        }
        return this.customColor.getColor();
    }

    @Override
    public void onDisable() {
        this.state.reset();
    }
}
