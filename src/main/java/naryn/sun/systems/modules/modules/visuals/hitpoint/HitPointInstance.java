package naryn.sun.systems.modules.modules.visuals.hitpoint;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

public class HitPointInstance {

   private final Entity target;
   private final Vec3d worldHitPos;
   private final double localRight;
   private final double localForward;
   private final double localY;
   private final float initialTargetYaw;
   private final Quaternionf spawnRotation;
   private final long spawnTime;
   private Vec3d lastRenderPos;

   public HitPointInstance(Entity target, Vec3d hitPos, Quaternionf cameraRotation) {
      this.target = target;
      this.worldHitPos = hitPos;
      this.spawnTime = System.currentTimeMillis();
      this.lastRenderPos = hitPos;
      this.spawnRotation = new Quaternionf(cameraRotation);

      float tYaw = 0.0F;
      if (target instanceof LivingEntity living) {
         tYaw = living.bodyYaw;
      } else if (target != null) {
         tYaw = target.getYaw();
      }
      this.initialTargetYaw = tYaw;

      if (target != null) {
         Vec3d offset = hitPos.subtract(target.getPos());
         double rad = Math.toRadians(tYaw);
         double sin = Math.sin(rad);
         double cos = Math.cos(rad);
         this.localRight = offset.x * cos + offset.z * sin;
         this.localForward = -offset.x * sin + offset.z * cos;
         this.localY = offset.y;
      } else {
         this.localRight = 0.0;
         this.localForward = 0.0;
         this.localY = 0.0;
      }
   }

   public Vec3d getRenderPos(float tickDelta, boolean entityMode) {
      if (!entityMode || this.target == null) {
         return this.worldHitPos;
      }

      if (!this.target.isAlive() && this.lastRenderPos != null) {
         return this.lastRenderPos;
      }

      double tx = MathHelper.lerp(tickDelta, this.target.prevX, this.target.getX());
      double ty = MathHelper.lerp(tickDelta, this.target.prevY, this.target.getY());
      double tz = MathHelper.lerp(tickDelta, this.target.prevZ, this.target.getZ());

      float curYaw;
      if (this.target instanceof LivingEntity living) {
         curYaw = MathHelper.lerpAngleDegrees(tickDelta, living.prevBodyYaw, living.bodyYaw);
      } else {
         curYaw = MathHelper.lerpAngleDegrees(tickDelta, this.target.prevYaw, this.target.getYaw());
      }

      double curRad = Math.toRadians(curYaw);
      double curSin = Math.sin(curRad);
      double curCos = Math.cos(curRad);

      double rotX = this.localRight * curCos - this.localForward * curSin;
      double rotZ = this.localRight * curSin + this.localForward * curCos;

      Vec3d pos = new Vec3d(tx + rotX, ty + this.localY, tz + rotZ);
      this.lastRenderPos = pos;
      return pos;
   }

   public Quaternionf getRotation(float tickDelta, boolean entityMode) {
      if (!entityMode || this.target == null) {
         return this.spawnRotation;
      }

      float curYaw;
      if (this.target instanceof LivingEntity living) {
         curYaw = MathHelper.lerpAngleDegrees(tickDelta, living.prevBodyYaw, living.bodyYaw);
      } else {
         curYaw = MathHelper.lerpAngleDegrees(tickDelta, this.target.prevYaw, this.target.getYaw());
      }

      float deltaYaw = curYaw - this.initialTargetYaw;
      if (Math.abs(deltaYaw) < 0.01F) {
         return this.spawnRotation;
      }

      // Вращение нормали плоскости вместе с телом цели вокруг мировой оси Y
      return new Quaternionf().rotationY((float) Math.toRadians(-deltaYaw)).mul(this.spawnRotation);
   }

   public float getProgress(long now, float durationMs) {
      return (now - this.spawnTime) / durationMs;
   }
}
