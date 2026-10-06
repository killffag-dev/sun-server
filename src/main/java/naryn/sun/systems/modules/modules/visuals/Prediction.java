package naryn.sun.systems.modules.modules.visuals;

import java.util.ArrayList;
import java.util.List;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.HudRenderEvent;
import naryn.sun.systems.event.impl.render.Render3DEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.modules.modules.visuals.prediction.PredictionHudRenderer;
import naryn.sun.systems.modules.modules.visuals.prediction.ProjectilePhysics;
import naryn.sun.systems.modules.modules.visuals.prediction.Trajectory3DRenderer;
import naryn.sun.systems.modules.modules.visuals.prediction.TrajectoryData;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.ColorSetting;
import naryn.sun.systems.setting.settings.SelectSetting;
import naryn.sun.systems.setting.settings.shared.PredicateValue;
import naryn.sun.utility.colors.ColorRGBA;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.AbstractWindChargeEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;

@ModuleInfo(name = "Prediction", category = ModuleCategory.VISUALS)
public class Prediction extends BaseModule {

    private final List<TrajectoryData> trajectories = new ArrayList<>();

    private final SelectSetting entities = new SelectSetting(this, "modules.settings.prediction.entities");
    private final BooleanSetting trajectory = new BooleanSetting(this, "modules.settings.prediction.trajectory").enable();
    private final BooleanSetting landing = new BooleanSetting(this, "modules.settings.prediction.landing").enable();
    private final ColorSetting color = new ColorSetting(this, "modules.settings.prediction.color")
        .color(new ColorRGBA(255.0F, 230.0F, 80.0F, 255.0F));

    private final EventListener<HudRenderEvent> onRender2D = event -> {
        PredictionHudRenderer.render(event.getContext(), this.trajectories);
    };

    private final EventListener<Render3DEvent> onRender3D = event -> {
        Trajectory3DRenderer.render(
            event.getMatrices(),
            event.getTickDelta(),
            this.trajectories,
            this.trajectory.isEnabled(),
            this.landing.isEnabled(),
            this.color.getColor()
        );
    };

    public Prediction() {
        new PredicateValue<Entity>(this.entities, "modules.settings.prediction.entities.pearls", entity -> entity instanceof EnderPearlEntity).select();
        new PredicateValue<Entity>(this.entities, "modules.settings.prediction.entities.tridents", entity -> entity instanceof TridentEntity).select();
        new PredicateValue<Entity>(this.entities, "modules.settings.prediction.entities.snowballs", entity -> entity instanceof SnowballEntity).select();
        new PredicateValue<Entity>(this.entities, "modules.settings.prediction.entities.arrows", entity -> entity instanceof ArrowEntity).select();
        new PredicateValue<Entity>(this.entities, "modules.settings.prediction.entities.potions", entity -> entity instanceof PotionEntity || entity instanceof AreaEffectCloudEntity).select();
        new PredicateValue<Entity>(this.entities, "modules.settings.prediction.entities.wind_charges", entity -> entity instanceof AbstractWindChargeEntity).select();
        new PredicateValue<Entity>(this.entities, "modules.settings.prediction.entities.items", entity -> entity instanceof ItemEntity);
    }

    @Override
    public void tick() {
        this.trajectories.clear();

        if (this.trajectory.isEnabled() || this.landing.isEnabled()) {
            ProjectilePhysics.collectInHandTrajectories(this.trajectories);
            ProjectilePhysics.collectWorldTrajectories(this.entities, this.trajectories);
        }
    }
}
