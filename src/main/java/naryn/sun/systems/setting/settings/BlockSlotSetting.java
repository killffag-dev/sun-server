package naryn.sun.systems.setting.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import naryn.sun.systems.setting.SettingsContainer;
import naryn.sun.systems.setting.impl.AbstractSetting;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;

public class BlockSlotSetting extends AbstractSetting {

    private final Set<Integer> lockedSlots = new TreeSet<>();

    public BlockSlotSetting(@NotNull SettingsContainer parent, String name, @NotNull BooleanSupplier hideCondition) {
        super(parent, name, hideCondition);
    }

    public BlockSlotSetting(@NotNull SettingsContainer parent, String name) {
        super(parent, name);
    }

    public boolean isLocked(int slot) {
        return lockedSlots.contains(slot);
    }

    public boolean toggleSlot(int slot) {
        if (lockedSlots.contains(slot)) {
            lockedSlots.remove(slot);
            return false;
        } else {
            lockedSlots.add(slot);
            return true;
        }
    }

    public void lock(int slot) {
        lockedSlots.add(slot);
    }

    public void unlock(int slot) {
        lockedSlots.remove(slot);
    }

    public void clear() {
        lockedSlots.clear();
    }

    public Set<Integer> getLockedSlots() {
        return Collections.unmodifiableSet(lockedSlots);
    }

    public int getLockedCount() {
        return lockedSlots.size();
    }

    @Override
    public JsonElement save() {
        JsonArray array = new JsonArray();
        for (int slot : lockedSlots) {
            array.add(slot);
        }
        return array;
    }

    @Override
    public void load(JsonElement element) {
        lockedSlots.clear();
        if (element != null && element.isJsonArray()) {
            for (JsonElement el : element.getAsJsonArray()) {
                try {
                    lockedSlots.add(el.getAsInt());
                } catch (Exception ignored) {
                }
            }
        }
    }
}
