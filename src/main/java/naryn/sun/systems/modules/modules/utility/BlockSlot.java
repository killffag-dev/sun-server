package naryn.sun.systems.modules.modules.utility;

import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.network.SendPacketEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BlockSlotSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(
    name = "BlockSlot",
    category = ModuleCategory.UTILITY,
    key = GLFW.GLFW_KEY_UNKNOWN,
    enabledByDefault = false,
    desc = "modules.descriptions.blockslot"
)
public class BlockSlot extends BaseModule {

    private final BlockSlotSetting slots = new BlockSlotSetting(this, "slots");

    public boolean isSlotIndexLocked(int index) {
        return slots.isLocked(index);
    }

    private static java.lang.reflect.Field creativeSlotField = null;
    private static boolean creativeSlotFieldInit = false;

    public static Slot unwrapSlot(Slot slot) {
        if (slot != null && slot.getClass().getSimpleName().equals("CreativeSlot")) {
            try {
                if (!creativeSlotFieldInit) {
                    creativeSlotFieldInit = true;
                    creativeSlotField = slot.getClass().getDeclaredField("slot");
                    creativeSlotField.setAccessible(true);
                }
                if (creativeSlotField != null) {
                    Object inner = creativeSlotField.get(slot);
                    if (inner instanceof Slot innerSlot) {
                        return innerSlot;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return slot;
    }

    public static int getPlayerInventorySlotIndex(Slot slot) {
        slot = unwrapSlot(slot);
        if (slot == null || slot.inventory == null) return -1;
        if (!(slot.inventory instanceof PlayerInventory)) return -1;

        // Броня в PlayerScreenHandler (id 5..8)
        if (slot.id >= 5 && slot.id <= 8 && (slot.getIndex() < 9 || slot.getIndex() > 35 || slot.getClass().getSimpleName().contains("Armor"))) {
            return 39 - (slot.id - 5);
        }

        // Левая рука (id 45 в PlayerScreenHandler или index 40 в PlayerInventory)
        if (slot.id == 45 || slot.getIndex() == 40) {
            return 40;
        }

        // Основной инвентарь (9..35), хотбар (0..8) или броня (36..39)
        int idx = slot.getIndex();
        if (idx >= 0 && idx <= 40) {
            return idx;
        }

        return -1;
    }

    public boolean isSlotLocked(Slot slot) {
        if (!isEnabled() || slot == null) return false;
        int playerSlot = getPlayerInventorySlotIndex(slot);
        if (playerSlot == -1) return false;
        return isSlotIndexLocked(playerSlot);
    }

    public boolean shouldBlockSlotClick(Slot slot, int slotId, int button, SlotActionType actionType) {
        if (!isEnabled()) return false;

        // 1. SWAP: клавиши хотбара 1..9 (button 0..8) или смена руки F (button 40)
        if (actionType == SlotActionType.SWAP) {
            if (button >= 0 && button <= 8 && isSlotIndexLocked(button)) {
                return true;
            }
            if (button == 40 && isSlotIndexLocked(40)) {
                return true;
            }
            if (slot != null && isSlotLocked(slot)) {
                return true;
            }
        }

        // 2. Наведенный слот заблокирован для любых действий (PICKUP, THROW, QUICK_MOVE, CLONE)
        if (slot != null && isSlotLocked(slot)) {
            return true;
        }

        // 3. QUICK_CRAFT (перетаскивание курсором по слотам)
        if (actionType == SlotActionType.QUICK_CRAFT && mc.player != null && mc.player.currentScreenHandler != null) {
            try {
                if (mc.player.currentScreenHandler instanceof naryn.sun.mixin.accessors.ScreenHandlerAccessor accessor) {
                    for (Slot s : accessor.getQuickCraftSlots()) {
                        if (isSlotLocked(s)) {
                            return true;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // 4. PICKUP_ALL (двойной клик, собирающий одинаковые предметы в курсор)
        if (actionType == SlotActionType.PICKUP_ALL && mc.player != null && mc.player.currentScreenHandler != null) {
            ItemStack cursorStack = mc.player.currentScreenHandler.getCursorStack();
            if (!cursorStack.isEmpty()) {
                for (Slot s : mc.player.currentScreenHandler.slots) {
                    if (isSlotLocked(s) && s.hasStack()) {
                        if (ItemStack.areItemsAndComponentsEqual(cursorStack, s.getStack())) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public boolean shouldBlockSlotClick(PlayerEntity player, int syncId, int slotId, int button, SlotActionType actionType) {
        if (!isEnabled() || player == null || player.currentScreenHandler == null) return false;
        Slot slot = (slotId >= 0 && slotId < player.currentScreenHandler.slots.size())
                ? player.currentScreenHandler.getSlot(slotId) : null;
        return shouldBlockSlotClick(slot, slotId, button, actionType);
    }

    private final EventListener<SendPacketEvent> onSendPacket = event -> {
        if (!isEnabled() || mc.player == null) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof PlayerActionC2SPacket actionPacket) {
            PlayerActionC2SPacket.Action action = actionPacket.getAction();
            int selectedSlot = mc.player.getInventory().selectedSlot;
            if (action == PlayerActionC2SPacket.Action.DROP_ITEM || action == PlayerActionC2SPacket.Action.DROP_ALL_ITEMS) {
                if (isSlotIndexLocked(selectedSlot)) {
                    event.cancel();
                }
            } else if (action == PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
                if (isSlotIndexLocked(selectedSlot) || isSlotIndexLocked(40)) {
                    event.cancel();
                }
            }
        }
    };

    public BlockSlotSetting getSlotsSetting() {
        return slots;
    }
}
