package naryn.sun.utility.game;

import java.util.function.Predicate;
import lombok.Generated;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.inventory.group.SlotGroup;
import naryn.sun.utility.inventory.group.SlotGroups;
import naryn.sun.utility.inventory.slots.HotbarSlot;
import naryn.sun.utility.inventory.EnchantmentUtility;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public final class CombatUtility implements IMinecraft {
   public static HotbarSlot getMace() {
      SlotGroup<HotbarSlot> slotsToSearch = SlotGroups.hotbar();
      boolean useWindBurst = mc.player.fallDistance > 2.0F;
      RegistryKey<Enchantment> targetEnchantment = useWindBurst ? Enchantments.WIND_BURST : Enchantments.BREACH;
      HotbarSlot slot = slotsToSearch.findItem(
         (Predicate<ItemStack>)(stack -> !(stack.getItem() instanceof MaceItem) ? false : EnchantmentUtility.getEnchantmentLevel(stack, targetEnchantment) > 0)
      );
      if (slot == null) {
         slot = slotsToSearch.findItem(Items.MACE);
      }
      return slot;
   }

   public static boolean canBreakShield(LivingEntity target) {
      if (mc.player == null || mc.player.isDead()) return false;
      if (target.isDead()) return false;
      HotbarSlot axeSlot = SlotGroups.hotbar().findItem((Predicate<ItemStack>)(itemStack -> itemStack.getItem() instanceof AxeItem));
      if (axeSlot == null) return false;
      Vec3d facingVector = target.getRotationVector();
      Vec3d deltaPos = new Vec3d(target.getPos().getX() - mc.player.getPos().getX(), 0.0, target.getPos().getZ() - mc.player.getPos().getZ());
      return deltaPos.dotProduct(facingVector) < 0.0;
   }

   public static boolean shouldBreakShield(LivingEntity target) {
      return target.isUsingItem() && target.getActiveItem().getItem() instanceof ShieldItem;
   }

   public static void tryBreakShield(LivingEntity target) {
      if (mc.player != null && mc.interactionManager != null) {
         SlotGroup<HotbarSlot> slotsToSearch = SlotGroups.hotbar();
         HotbarSlot slot = slotsToSearch.findItem((Predicate<ItemStack>)(item -> item.getItem() instanceof AxeItem));
         if (slot != null && target instanceof PlayerEntity && target.isUsingItem() && target.getActiveItem().getItem() instanceof ShieldItem) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot.getSlotId()));
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
         }
      }
   }

   @Generated
   private CombatUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}