package naryn.sun.utility.game;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.util.math.Vec3d;

import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EntityPose;

public class FakePlayerEntity extends OtherClientPlayerEntity {

   // Замороженная на момент captureGhost() поза анимации, углы поворота и конечности.
   // Обычный limbAnimator у призрака всегда ~0 (он ведь не двигается), поэтому
   // рендер-стейт переопределяется этими значениями в PlayerEntityRendererMixin.
   public float capturedPitch;
   public float capturedYaw;
   public float capturedHeadYaw;
   public float capturedBodyYaw;
   public float capturedLimbFrequency;
   public float capturedLimbAmplitude;
   public float capturedHandSwingProgress;
   public float capturedLeaningPitch;
   public EntityPose capturedPose = EntityPose.STANDING;
   public boolean capturedSneaking;
   public boolean capturedGliding;
   public boolean capturedSwimming;
   public BipedEntityModel.ArmPose capturedLeftArmPose = BipedEntityModel.ArmPose.EMPTY;
   public BipedEntityModel.ArmPose capturedRightArmPose = BipedEntityModel.ArmPose.EMPTY;
   public boolean capturedHatVisible = true;
   public boolean capturedJacketVisible = true;
   public boolean capturedLeftPantsVisible = true;
   public boolean capturedRightPantsVisible = true;
   public boolean capturedLeftSleeveVisible = true;
   public boolean capturedRightSleeveVisible = true;
   public boolean capturedCapeVisible = true;
   public long spawnTime = System.currentTimeMillis();

   // Отличает "твёрдый" манекен (модуль FakePlayer - полностью непрозрачный,
   // с видимым ником, обычный слой рендера) от призрачного следа Trails
   // (полупрозрачный, без ника, специальный translucent-слой). По умолчанию
   // false - существующее поведение Trails не меняется ни на бит.
   private boolean solidDummy;

   public FakePlayerEntity(ClientWorld world, GameProfile profile) {
      super(world, profile);
      this.setNoGravity(true);
   }

   public void spawn() {
      this.unsetRemoved();
      this.clientWorld.addEntity(this);
   }

   public void remove() {
      this.clientWorld.removeEntity(this.getId(), RemovalReason.DISCARDED);
      this.onRemoved();
   }

   public void takeKnockback(double strength, double x, double z) {
   }

   public void setSolidDummy(boolean solidDummy) {
      this.solidDummy = solidDummy;
   }

   public boolean isSolidDummy() {
      return this.solidDummy;
   }

   @Override
   public boolean isPushable() {
      // Ни сам манекен никого не толкает, ни его никто не толкает -
      // он должен быть неподвижным ориентиром, а не физическим телом.
      return false;
   }

   @Override
   public void pushAwayFrom(Entity entity) {
   }

   @Override
   public void tickMovement() {
      // Физику/гравитацию/коллизии полностью глушим - манекен обязан стоять
      // ровно там, где заспавнен, и никогда сам никуда не смещаться.
      this.setVelocity(Vec3d.ZERO);
   }

   // Жёстко ставит манекен в заданную точку И синхронизирует все поля предыдущего
   // кадра (prevX, prevY, prevZ, lastRenderX и т.д.). Без этого рендер лерпит
   // между нулевыми prev-координатами и реальной позицией, и модель визуально
   // уезжает к координате 0,0,0 - при том что логика (хп, хитбокс, дистанция)
   // работает по настоящей позиции.
   // Вызывается и на спавне, и каждый тик из модуля FakePlayer.
   public void anchorAt(double x, double y, double z, float yaw) {
      this.setPosition(x, y, z);
      this.setVelocity(Vec3d.ZERO);

      this.prevX = x;
      this.prevY = y;
      this.prevZ = z;

      this.lastRenderX = x;
      this.lastRenderY = y;
      this.lastRenderZ = z;

      this.setYaw(yaw);
      this.prevYaw = yaw;
      this.setPitch(0.0F);
      this.prevPitch = 0.0F;

      this.headYaw = yaw;
      this.prevHeadYaw = yaw;
      this.bodyYaw = yaw;
      this.prevBodyYaw = yaw;
   }
}