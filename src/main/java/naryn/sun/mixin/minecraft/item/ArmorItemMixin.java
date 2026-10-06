package naryn.sun.mixin.minecraft.item;

import naryn.sun.utility.mixins.ArmorItemAddition;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorItem.class)
public abstract class ArmorItemMixin implements ArmorItemAddition {
   @Unique
   private EquipmentType sun$type;
   @Unique
   private ArmorMaterial sun$material;

   @Inject(method = "<init>", at = @At("TAIL"))
   public void saveArgs(ArmorMaterial material, EquipmentType type, Settings settings, CallbackInfo ci) {
      this.sun$type = type;
      this.sun$material = material;
   }

   @Override
   public ArmorMaterial sun$getMaterial() {
      return this.sun$material;
   }

   @Override
   public EquipmentType sun$getType() {
      return this.sun$type;
   }
}
