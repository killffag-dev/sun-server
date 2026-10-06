package naryn.sun.mixin.minecraft.screen;

import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.utility.BlockSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin {

    @Inject(method = "canInsert", at = @At("HEAD"), cancellable = true)
    private void onCanInsert(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        BlockSlot blockSlot = Sun.getInstance().getModuleManager().getModule(BlockSlot.class);
        if (blockSlot != null && blockSlot.isEnabled() && blockSlot.isSlotLocked((Slot) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canTakeItems", at = @At("HEAD"), cancellable = true)
    private void onCanTakeItems(PlayerEntity playerEntity, CallbackInfoReturnable<Boolean> cir) {
        BlockSlot blockSlot = Sun.getInstance().getModuleManager().getModule(BlockSlot.class);
        if (blockSlot != null && blockSlot.isEnabled() && blockSlot.isSlotLocked((Slot) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
