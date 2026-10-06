package naryn.sun.mixin.minecraft.screen;

import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.utility.BlockSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {
    @Shadow
    @Final
    public DefaultedList<Slot> slots;

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void onSlotClickHook(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        BlockSlot blockSlot = Sun.getInstance().getModuleManager().getModule(BlockSlot.class);
        if (blockSlot != null && blockSlot.isEnabled()) {
            Slot slot = (slotIndex >= 0 && slotIndex < this.slots.size()) ? this.slots.get(slotIndex) : null;
            if (blockSlot.shouldBlockSlotClick(slot, slotIndex, button, actionType)) {
                ci.cancel();
            }
        }
    }
}
