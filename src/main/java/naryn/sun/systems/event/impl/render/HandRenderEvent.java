package naryn.sun.systems.event.impl.render;

import lombok.Generated;
import naryn.sun.systems.event.EventCancellable;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;

public class HandRenderEvent extends EventCancellable {
   public static final HandRenderEvent INSTANCE = new HandRenderEvent();

   private Arm arm;
   private float swingProgress;
   private ItemStack itemStack;
   private float equipProgress;
   private MatrixStack matrices;

   public HandRenderEvent() {
   }

   @Generated
   public HandRenderEvent(Arm arm, float swingProgress, ItemStack itemStack, float equipProgress, MatrixStack matrices) {
      this.set(arm, swingProgress, itemStack, equipProgress, matrices);
   }

   public HandRenderEvent set(Arm arm, float swingProgress, ItemStack itemStack, float equipProgress, MatrixStack matrices) {
      this.arm = arm;
      this.swingProgress = swingProgress;
      this.itemStack = itemStack;
      this.equipProgress = equipProgress;
      this.matrices = matrices;
      this.setCancelled(false);
      return this;
   }

   @Generated
   public Arm getArm() {
      return this.arm;
   }

   @Generated
   public float getSwingProgress() {
      return this.swingProgress;
   }

   @Generated
   public ItemStack getItemStack() {
      return this.itemStack;
   }

   @Generated
   public float getEquipProgress() {
      return this.equipProgress;
   }

   @Generated
   public MatrixStack getMatrices() {
      return this.matrices;
   }
}
