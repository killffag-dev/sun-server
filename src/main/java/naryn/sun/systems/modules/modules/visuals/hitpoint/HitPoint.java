package naryn.sun.systems.modules.modules.visuals.hitpoint;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import naryn.sun.access.MCPlayerAccess;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.game.EntityDamageEvent;
import naryn.sun.systems.event.impl.game.InternalAttackEvent;
import naryn.sun.systems.event.impl.render.Render3DEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.ColorSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.FakePlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

@ModuleInfo(name = "Hit Point", category = ModuleCategory.VISUALS, desc = "modules.descriptions.hit_point")
public class HitPoint extends BaseModule {

   private final ModeSetting positionMode = new ModeSetting(this, "modules.settings.hit_point.position");
   private final ModeSetting.Value world = new ModeSetting.Value(this.positionMode, "modules.settings.hit_point.position.world").select();
   private final ModeSetting.Value entity = new ModeSetting.Value(this.positionMode, "modules.settings.hit_point.position.entity");

   private final ModeSetting style = new ModeSetting(this, "modules.settings.hit_point.style");
   private final ModeSetting.Value magicCircle = new ModeSetting.Value(this.style, "modules.settings.hit_point.style.magic_circle").select();
   private final ModeSetting.Value shockwave = new ModeSetting.Value(this.style, "modules.settings.hit_point.style.shockwave");
   private final ModeSetting.Value crossSlash = new ModeSetting.Value(this.style, "modules.settings.hit_point.style.cross_slash");
   private final ModeSetting.Value nova = new ModeSetting.Value(this.style, "modules.settings.hit_point.style.nova");

   private final SliderSetting duration = new SliderSetting(this, "modules.settings.hit_point.duration")
      .min(200.0F).max(2000.0F).step(50.0F).currentValue(750.0F);

   private final SliderSetting size = new SliderSetting(this, "modules.settings.hit_point.size")
      .min(0.1F).max(2.0F).step(0.05F).currentValue(0.6F);

   private final BooleanSetting glow = new BooleanSetting(this, "modules.settings.hit_point.glow").enable();

   private final ColorSetting color = new ColorSetting(this, "modules.settings.hit_point.color").color(Colors.ACCENT);

   private static final class PendingAttack {
      final Entity target;
      final Vec3d hitOffset;
      final Quaternionf camRot;
      final long time;

      PendingAttack(Entity target, Vec3d hitOffset, Quaternionf camRot, long time) {
         this.target = target;
         this.hitOffset = hitOffset;
         this.camRot = camRot;
         this.time = time;
      }
   }

   private final List<HitPointInstance> instances = new ArrayList<>();
   private final Map<Integer, Long> lastHitTimes = new HashMap<>();
   private final Map<Integer, PendingAttack> pendingAttacks = new ConcurrentHashMap<>();

   @Override
   public void onEnable() {
      this.instances.clear();
      this.lastHitTimes.clear();
      this.pendingAttacks.clear();
   }

   @Override
   public void onDisable() {
      this.instances.clear();
      this.lastHitTimes.clear();
      this.pendingAttacks.clear();
   }

   public void spawnHit(Entity target) {
      if (!this.isEnabled() || target == null) {
         return;
      }
      if (this.pendingAttacks.containsKey(target.getId())) {
         this.confirmHit(target);
      } else if (target instanceof FakePlayerEntity) {
         this.spawnInstantHit(target);
      }
   }

   public void onDamagePacket(int entityId) {
      if (!this.isEnabled()) {
         return;
      }
      PendingAttack pending = this.pendingAttacks.get(entityId);
      if (pending != null && pending.target != null) {
         this.confirmHit(pending.target);
      }
   }

   private void handleAttack(Entity target) {
      if (!this.isEnabled()) {
         return;
      }
      if (!MCPlayerAccess.isPresent() || target == null) {
         return;
      }

      ClientPlayerEntity player = MCPlayerAccess.get();
      if (player == null) {
         return;
      }

      // Отчётливый удар: проверка кулдауна атаки игрока.
      // В Minecraft отчётливый удар (полный урон) наносится при полностью заряженном индикаторе атаки (>= 0.85F).
      // Быстрые спам-клики без кулдауна наносят мизерный урон и не считаются отчётливым ударом.
      if (player.getAttackCooldownProgress(0.5F) < 0.85F) {
         return;
      }

      if (target instanceof LivingEntity living) {
         // Мёртвые цели не получают урон
         if (living.isDead() || living.getHealth() <= 0.0F) {
            return;
         }
         // Если цель уже заблокировала удар щитом в направлении атакующего
         if (this.isShieldBlocked(living, player)) {
            return;
         }
      }

      long now = System.currentTimeMillis();
      Long lastHit = this.lastHitTimes.get(target.getId());
      if (lastHit != null && now - lastHit < 300L) {
         return;
      }

      Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
      Quaternionf camRot = camera != null ? new Quaternionf(camera.getRotation()) : new Quaternionf();

      Vec3d hitPos = HitPointRaycast.calculateHit(target, player);
      Vec3d hitOffset = hitPos.subtract(target.getPos());

      this.pendingAttacks.put(target.getId(), new PendingAttack(target, hitOffset, camRot, now));
   }

   public void confirmHit(Entity target) {
      if (!this.isEnabled() || target == null) {
         return;
      }

      int targetId = target.getId();
      PendingAttack pending = this.pendingAttacks.remove(targetId);
      long now = System.currentTimeMillis();

      // Если нет ожидающей атаки от нашего игрока — урон нанесён не нами
      // (или атака не была отчётливой / спам-клик без кулдауна).
      if (pending == null) {
         if (target instanceof FakePlayerEntity) {
            this.spawnInstantHit(target);
         }
         return;
      }

      // Проверка таймаута ожидания подтверждения урона (макс. 500мс сетевого ответа)
      if (now - pending.time > 500L) {
         return;
      }

      Long lastHit = this.lastHitTimes.get(targetId);
      if (lastHit != null && now - lastHit < 300L) {
         return;
      }
      this.lastHitTimes.put(targetId, now);

      Vec3d hitPos = target.getPos().add(pending.hitOffset);
      this.instances.add(new HitPointInstance(target, hitPos, pending.camRot));
   }

   private void spawnInstantHit(Entity target) {
      if (!MCPlayerAccess.isPresent()) return;
      ClientPlayerEntity player = MCPlayerAccess.get();
      if (player == null) return;

      long now = System.currentTimeMillis();
      Long lastHit = this.lastHitTimes.get(target.getId());
      if (lastHit != null && now - lastHit < 300L) {
         return;
      }
      this.lastHitTimes.put(target.getId(), now);

      Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
      Quaternionf camRot = camera != null ? new Quaternionf(camera.getRotation()) : new Quaternionf();
      Vec3d hitPos = HitPointRaycast.calculateHit(target, player);
      this.instances.add(new HitPointInstance(target, hitPos, camRot));
   }

   private boolean isShieldBlocked(LivingEntity target, ClientPlayerEntity attacker) {
      if (!target.isBlocking()) {
         return false;
      }
      Vec3d targetFacing = target.getRotationVector();
      Vec3d toAttacker = attacker.getPos().subtract(target.getPos()).normalize();
      double dot = targetFacing.x * toAttacker.x + targetFacing.z * toAttacker.z;
      return dot > 0.0;
   }

   private final EventListener<InternalAttackEvent> onInternalAttack = event -> {
      this.handleAttack(event.getEntity());
   };

   private final EventListener<EntityDamageEvent> onEntityDamage = event -> {
      if (event.getEntity() != null) {
         this.confirmHit(event.getEntity());
      }
   };

   private final EventListener<Render3DEvent> onRender3D = event -> {
      long now = System.currentTimeMillis();
      if (!this.pendingAttacks.isEmpty()) {
         this.pendingAttacks.entrySet().removeIf(entry -> now - entry.getValue().time > 1000L);
      }

      if (this.instances.isEmpty() || !MCPlayerAccess.isPresent()) {
         return;
      }

      MatrixStack ms = event.getMatrices();
      Camera camera = event.getCamera();
      Vec3d camPos = camera.getPos();
      float tickDelta = event.getTickDelta();

      ColorRGBA col = this.color.getColor();
      float r = col.getRed() / 255.0F;
      float g = col.getGreen() / 255.0F;
      float b = col.getBlue() / 255.0F;
      float baseAlpha = col.getAlpha() / 255.0F;

      float durationMs = this.duration.getCurrentValue();
      float effectSize = this.size.getCurrentValue();
      boolean isEntityMode = this.entity.isSelected();
      boolean isGlow = this.glow.isEnabled();

      // Очистка устаревших записей дебаунса
      this.lastHitTimes.entrySet().removeIf(entry -> now - entry.getValue() > 2000L);

      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.enableDepthTest();
      RenderSystem.enablePolygonOffset();
      RenderSystem.polygonOffset(-2.0F, -2.0F);
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);

      Iterator<HitPointInstance> it = this.instances.iterator();
      while (it.hasNext()) {
         HitPointInstance fx = it.next();
         float progress = fx.getProgress(now, durationMs);
         if (progress >= 1.0F) {
            it.remove();
            continue;
         }

         Vec3d renderPos = fx.getRenderPos(tickDelta, isEntityMode);
         Quaternionf rotation = fx.getRotation(tickDelta, isEntityMode);

         ms.push();
         ms.translate((float) (renderPos.x - camPos.x), (float) (renderPos.y - camPos.y), (float) (renderPos.z - camPos.z));
         ms.multiply(rotation);

         if (this.magicCircle.isSelected()) {
            HitPointRenderer.renderMagicCircle(ms, progress, effectSize, r, g, b, baseAlpha, isGlow);
         } else if (this.shockwave.isSelected()) {
            HitPointRenderer.renderShockwave(ms, progress, effectSize, r, g, b, baseAlpha, isGlow);
         } else if (this.crossSlash.isSelected()) {
            HitPointRenderer.renderCrossSlash(ms, progress, effectSize, r, g, b, baseAlpha, isGlow);
         } else if (this.nova.isSelected()) {
            HitPointRenderer.renderNova(ms, progress, effectSize, r, g, b, baseAlpha, isGlow);
         }

         ms.pop();
      }

      RenderSystem.disablePolygonOffset();
      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   };
}
