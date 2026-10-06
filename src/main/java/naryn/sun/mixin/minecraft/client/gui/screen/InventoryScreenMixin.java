package naryn.sun.mixin.minecraft.client.gui.screen;

import naryn.sun.Sun;
import naryn.sun.mixin.accessors.ScreenAccessor;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.inventory.ItemSlot;
import naryn.sun.utility.inventory.group.SlotGroup;
import naryn.sun.utility.inventory.group.SlotGroups;
import naryn.sun.utility.inventory.group.impl.ArmorSlotsGroup;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends RecipeBookScreen<PlayerScreenHandler> implements IMinecraft {
   public InventoryScreenMixin(PlayerScreenHandler handler, RecipeBookWidget<?> recipeBook, PlayerInventory inventory, Text title) {
      super(handler, recipeBook, inventory, title);
   }

   @Inject(method = "init", at = @At("TAIL"))
   private void dropButton(CallbackInfo ci) {
      if (!Sun.INSTANCE.isPanic()) {
         ButtonWidget widget = ButtonWidget.builder(Text.of(Localizator.translate("inventory.button.drop_all")), b -> this.dropAll())
            .dimensions(this.x + this.backgroundWidth / 2 - 40, this.y - 20, 80, 18)
            .build();
         ((ScreenAccessor)this).invokeAddDrawableChild(widget);
      }
   }

   @Unique
   private void dropAll() {
      SlotGroup<ItemSlot> slots = SlotGroups.inventory().and(SlotGroups.hotbar()).and(SlotGroups.offhand()).and(new ArmorSlotsGroup());

      for (ItemSlot slot : slots.getSlots()) {
         if (!slot.isEmpty()) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot.getIdForServer(), 1, SlotActionType.THROW, mc.player);
         }
      }
   }
}
