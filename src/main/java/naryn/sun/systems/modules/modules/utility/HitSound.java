package naryn.sun.systems.modules.modules.utility;

import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.game.AttackEvent;
import naryn.sun.systems.event.impl.game.PostAttackEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.utility.sounds.ClientSoundManager;

import java.util.Random;

@ModuleInfo(name = "Hit Sound", category = ModuleCategory.UTILITY, desc = "modules.descriptions.hit_sound")
public class HitSound extends BaseModule {
   // === 1. Mode First (Top-Level) ===
   private final ModeSetting pack = new ModeSetting(this, "modules.settings.hit_sound.pack");
   private final ModeSetting.Value crisp = new ModeSetting.Value(this.pack, "modules.settings.hit_sound.pack.crisp").select();
   private final ModeSetting.Value metal = new ModeSetting.Value(this.pack, "modules.settings.hit_sound.pack.metal");
   private final ModeSetting.Value asmr = new ModeSetting.Value(this.pack, "modules.settings.hit_sound.pack.asmr");
   private final ModeSetting.Value slap = new ModeSetting.Value(this.pack, "modules.settings.hit_sound.pack.slap");
   private final ModeSetting.Value crystal = new ModeSetting.Value(this.pack, "modules.settings.hit_sound.pack.crystal");

   // === 2. General & Logic Box ===
   private final BooleanSetting missSound = new BooleanSetting(this, "modules.settings.hit_sound.miss_sound").enable();

   private final Random random = new Random();

   // Дебаунс кастомного звука: SWEEP может прийти вместе с основным звуком удара
   // (CRIT/STRONG/WEAK/KNOCKBACK) в рамках одной атаки — без этого сыграли бы два звука на один удар.
   private static final long HIT_DEBOUNCE_MS = 60L;
   private long lastHitTime = 0L;
   private boolean lastWasCrit = false;
   private boolean lastAttackCrit = false;
   private long lastCritTime = 0L;

   private volatile boolean inAttackCall = false;
   private long lastAttackStartMs = 0L;

   private final EventListener<AttackEvent> onAttackStart = event -> {
      this.inAttackCall = true;
      this.lastAttackStartMs = System.currentTimeMillis();
      if (mc.player != null) {
         this.lastAttackCrit = mc.player.fallDistance > 0.0F
               && !mc.player.isOnGround()
               && !mc.player.isClimbing()
               && !mc.player.isTouchingWater();
      }
   };
   private final EventListener<PostAttackEvent> onAttackEnd = event -> this.inAttackCall = false;

   public boolean isInAttackCall() {
      return this.inAttackCall;
   }

   public boolean isRecentAttack() {
      return System.currentTimeMillis() - this.lastAttackStartMs < 350L;
   }

   public void markCrit() {
      this.lastAttackCrit = true;
      this.lastCritTime = System.currentTimeMillis();
   }

   public boolean isRecentCrit() {
      return (this.lastAttackCrit && isRecentAttack()) || (System.currentTimeMillis() - this.lastCritTime < 350L);
   }

   /**
    * Вызывается, когда сервер реально подтвердил нанесение урона цели
    * (через EntityDamageS2CPacket или получение hurt-звука цели).
    */
   public void onDamageConfirmed(boolean isCrit) {
      if (!this.isEnabled()) return;
      long now = System.currentTimeMillis();
      if (now - this.lastHitTime < HIT_DEBOUNCE_MS) {
         if (isCrit && !this.lastWasCrit) {
            this.lastWasCrit = true;
            playHitSound(true);
         }
         return;
      }
      this.lastHitTime = now;
      this.lastWasCrit = isCrit;
      playHitSound(isCrit);
   }

   public void playHitSound(boolean isCrit) {
      if (mc == null) return;
      mc.execute(() -> {
         String style = "crisp";
         if (metal.isSelected()) style = "metal";
         else if (asmr.isSelected()) style = "asmr";
         else if (slap.isSelected()) style = "slap";
         else if (crystal.isSelected()) style = "crystal";

         String type = isCrit ? "crit" : "hit";
         float pitch = 0.95F + random.nextFloat() * 0.1F;

         ClientSoundManager.getInstance().playOneShot("hitsound_" + style + "_" + type, 1.0F, pitch);
      });
   }

   public void playMissSound() {
      if (!this.isEnabled() || !this.missSound.isEnabled()) return;
      if (mc == null) return;
      mc.execute(() -> {
         float pitch = 0.95F + random.nextFloat() * 0.1F;
         ClientSoundManager.getInstance().playOneShot("hitsound_miss", 0.8F, pitch);
      });
   }

   public boolean isMissSoundEnabled() {
      return this.missSound.isEnabled();
   }
}