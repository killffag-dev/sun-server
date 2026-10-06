package naryn.sun.systems.modules.modules.utility;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.UUID;
import naryn.sun.Sun;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.game.EntityDeathEvent;
import naryn.sun.systems.event.impl.game.InternalAttackEvent;
import naryn.sun.systems.event.impl.player.ClientPlayerTickEvent;
import naryn.sun.systems.localization.Language;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.modules.modules.visuals.HitParticles;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.GroupSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.game.FakePlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Fake Player", category = ModuleCategory.UTILITY)
public class FakePlayer extends BaseModule {

   // Пауза между "настоящей" смертью и удалением сущности: нужна только чтобы
   // доиграла ванильная анимация падения тела и килл-эффект. После неё манекен
   // удаляется полностью и модуль выключается - второй манекен никогда не
   // существует одновременно с первым, мёртвых "0.0 хп" целей не остаётся.
   private static final long DEATH_CLEANUP_MS = 1500L;

   // === 1. General & Logic Box ===
   private final GroupSetting generalGroup = new GroupSetting(this, "modules.settings.fake_player.group.general");
   private final BooleanSetting immortal = new BooleanSetting(this.generalGroup, "modules.settings.fake_player.immortal");
   private final SliderSetting hp = new SliderSetting(this.generalGroup, "modules.settings.fake_player.hp", () -> this.immortal.isEnabled())
      .min(2.0F).max(40.0F).step(1.0F).currentValue(20.0F);
   private final SliderSetting totems = new SliderSetting(this.generalGroup, "modules.settings.fake_player.totems", () -> this.immortal.isEnabled())
      .min(0.0F).max(10.0F).step(1.0F).currentValue(0.0F);

   private FakePlayerEntity entity;
   private float currentHealth;
   private float maxHealth;
   private int totemsLeft;

   private boolean dead;
   private long cleanupAtMs;

   // Координатный якорь: точка, в которой манекен обязан стоять всё время жизни.
   private double anchorX;
   private double anchorY;
   private double anchorZ;
   private float anchorYaw;

   @Override
   public void onEnable() {
      this.spawn();
   }

   @Override
   public void onDisable() {
      this.despawn();
   }

   private void spawn() {
      if (mc.player == null || mc.world == null) {
         this.setEnabled(false, true);
         return;
      }

      // Страховка от дубля: если по какой-то причине старая сущность ещё жива,
      // она удаляется до создания новой. Одновременно существует ровно один манекен.
      this.despawn();

      String displayName = Localizator.getCurrentLanguage() == Language.RU_RU ? "Фейковый игрок" : "Fake player";

      // Профилю манекена НЕЛЬЗЯ давать UUID реального игрока - клиентский
      // EntityLookup индексирует сущности по UUID, и при совпадении с уже
      // существующим ClientPlayerEntity ванильно удаляет добавляемую сущность
      // ("Duplicate entity UUID" в логе) сразу после спавна. Скин при этом
      // всё равно подтягивается корректно - он идёт через properties (текстуры),
      // скопированные из профиля ниже, а не через сам UUID.
      GameProfile sourceProfile = mc.player.getGameProfile();
      GameProfile profile = new GameProfile(UUID.randomUUID(), displayName);
      for (Property property : sourceProfile.getProperties().values()) {
         profile.getProperties().put(property.name(), property);
      }

      this.entity = new FakePlayerEntity((ClientWorld) mc.world, profile);
      this.entity.setSolidDummy(true);

      this.anchorX = mc.player.getX();
      this.anchorY = mc.player.getY();
      this.anchorZ = mc.player.getZ();
      this.anchorYaw = mc.player.getYaw();
      this.entity.anchorAt(this.anchorX, this.anchorY, this.anchorZ, this.anchorYaw);

      this.maxHealth = this.hp.getCurrentValue();
      this.entity.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(this.maxHealth);
      this.currentHealth = this.maxHealth;
      this.entity.setHealth(this.currentHealth);

      this.totemsLeft = Math.round(this.totems.getCurrentValue());
      this.dead = false;

      this.entity.spawn();
   }

   private void despawn() {
      if (this.entity != null) {
         this.entity.remove();
         this.entity = null;
      }
      this.dead = false;
   }

   private final EventListener<InternalAttackEvent> onAttack = event -> {
      if (this.entity == null || this.dead || event.getEntity() != this.entity || mc.player == null) {
         return;
      }

      // Игнорируем спам-клики: пока цель находится в кулдауне урона (10 тиков = 500мс),
      // урон не проходит, звуки и частицы не спамятся
      if (this.entity.hurtTime > 0) {
         return;
      }

      boolean isCrit = mc.player.fallDistance > 0.0F
         && !mc.player.isOnGround()
         && !mc.player.isClimbing()
         && !mc.player.isTouchingWater()
         && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
         && !mc.player.hasVehicle()
         && !mc.player.isSprinting();

      ItemStack weaponStack = mc.player.getMainHandStack();
      DamageSource damageSource = this.entity.getWorld().getDamageSources().playerAttack(mc.player);

      final float[] weaponDamage = {(float) mc.player.getAttributeBaseValue(EntityAttributes.ATTACK_DAMAGE)};
      weaponStack.applyAttributeModifiers(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
         if (attribute.matches(EntityAttributes.ATTACK_DAMAGE)) {
            if (modifier.operation() == EntityAttributeModifier.Operation.ADD_VALUE) {
               weaponDamage[0] += (float) modifier.value();
            }
         }
      });

      float damage = weaponDamage[0];
      float cooldown = mc.player.getAttackCooldownProgress(0.5F);
      damage *= 0.2F + cooldown * cooldown * 0.8F;
      damage += weaponStack.getItem().getBonusAttackDamage(this.entity, damage, damageSource);

      if (isCrit) {
         damage *= 1.5F;
      }

      // Звук удара: для FakePlayer при включенном HitSound напрямую воспроизводим кастомный звук,
      // так как FakePlayer локальный и серверных пакетов подтверждения урона нет.
      // Если HitSound выключен — играем ванильный звук атаки и звук получения урона игроком.
      HitSound hitSound = Sun.getInstance().getModuleManager().getModule(HitSound.class);
      if (hitSound != null && hitSound.isEnabled()) {
         hitSound.onDamageConfirmed(isCrit);
      } else {
         this.playAttackSound(isCrit);
         if (mc.world != null) {
            mc.world.playSound(mc.player, this.entity.getBlockPos(), SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0F, 1.0F);
         }
      }

      // Частицы: спавн эффектов из HitParticles на FakePlayer
      HitParticles hitParticles = Sun.getInstance().getModuleManager().getModule(HitParticles.class);
      if (hitParticles != null && hitParticles.isEnabled()) {
         hitParticles.spawnParticles(this.entity);
      }

      // Эффект места удара: спавн HitPoint на FakePlayer
      naryn.sun.systems.modules.modules.visuals.hitpoint.HitPoint hitPoint = Sun.getInstance().getModuleManager().getModule(naryn.sun.systems.modules.modules.visuals.hitpoint.HitPoint.class);
      if (hitPoint != null && hitPoint.isEnabled()) {
         hitPoint.spawnHit(this.entity);
      }

      // Сброс счётчика ПОСЛЕ звука и расчёта - playAttackSound тоже читает
      // getAttackCooldownProgress, ему нужно то же значение, что было до сброса.
      mc.player.resetLastAttackedTicks();

      this.applyDamage(damage, isCrit);
   };

   // Криты (доп. урон/частицы) - существующая логика выше, не трогал.
   // Здесь только сам звук удара: "Разящий удар" (ATTACK_STRONG), когда
   // перезарядка оружия полностью восстановлена, иначе обычный ATTACK_WEAK -
   // как в ванильной атаке ближнего боя.
   private void playAttackSound(boolean isCrit) {
      net.minecraft.sound.SoundEvent sound;
      if (isCrit) {
         sound = SoundEvents.ENTITY_PLAYER_ATTACK_CRIT;
      } else if (mc.player.getAttackCooldownProgress(0.5F) > 0.9F) {
         sound = SoundEvents.ENTITY_PLAYER_ATTACK_STRONG;
      } else {
         sound = SoundEvents.ENTITY_PLAYER_ATTACK_WEAK;
      }
      mc.world.playSound(mc.player, this.entity.getBlockPos(), sound, SoundCategory.PLAYERS, 1.0F, 1.0F);
   }

   private void applyDamage(float amount, boolean isCrit) {
      if (this.immortal.isEnabled()) {
         this.entity.animateDamage(0.0F);
         if (isCrit) {
            this.spawnCritParticles();
         }
         return;
      }

      float remaining = this.currentHealth - amount;

      if (remaining > 0.0F) {
         this.currentHealth = remaining;
         this.entity.setHealth(this.currentHealth);
         this.entity.animateDamage(0.0F);
         if (isCrit) {
            this.spawnCritParticles();
         }
         return;
      }

      if (this.totemsLeft > 0) {
         this.totemsLeft--;
         this.currentHealth = this.maxHealth;
         this.entity.setHealth(this.currentHealth);
         this.playTotemEffect();
         return;
      }

      this.currentHealth = 0.0F;
      this.entity.setHealth(0.0F);
      this.entity.handleStatus((byte) 3); // ванильный статус "death"

      Sun.getInstance()
         .getEventManager()
         .triggerEvent(new EntityDeathEvent(this.entity, this.entity.getWorld().getDamageSources().playerAttack(mc.player)));

      this.dead = true;
      this.cleanupAtMs = System.currentTimeMillis() + DEATH_CLEANUP_MS;
   }

   private void spawnCritParticles() {
      Vec3d pos = this.entity.getPos().add(0.0, this.entity.getHeight() * 0.6, 0.0);
      for (int i = 0; i < 6; i++) {
         double ox = (mc.world.random.nextDouble() - 0.5) * 0.6;
         double oy = mc.world.random.nextDouble() * 0.5;
         double oz = (mc.world.random.nextDouble() - 0.5) * 0.6;
         mc.world.addParticle(ParticleTypes.CRIT, pos.x + ox, pos.y + oy, pos.z + oz, 0.0, 0.0, 0.0);
      }
   }

   private void playTotemEffect() {
      Vec3d pos = this.entity.getPos().add(0.0, this.entity.getHeight() * 0.5, 0.0);
      for (int i = 0; i < 30; i++) {
         double ox = (mc.world.random.nextDouble() - 0.5) * 0.8;
         double oy = (mc.world.random.nextDouble() - 0.5) * 1.2;
         double oz = (mc.world.random.nextDouble() - 0.5) * 0.8;
         mc.world.addParticle(ParticleTypes.TOTEM_OF_UNDYING, pos.x + ox, pos.y + oy, pos.z + oz, ox, oy, oz);
      }
      mc.world.playSound(mc.player, this.entity.getBlockPos(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);
   }

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (this.entity == null) {
         return;
      }

      if (this.dead) {
         if (System.currentTimeMillis() >= this.cleanupAtMs) {
            this.despawn();
            this.setEnabled(false, true);
         }
         return;
      }

      this.entity.anchorAt(this.anchorX, this.anchorY, this.anchorZ, this.anchorYaw);
   };
}