package naryn.sun.systems.setting.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.function.BooleanSupplier;
import naryn.sun.systems.setting.SettingsContainer;
import naryn.sun.systems.setting.impl.AbstractSetting;
import org.jetbrains.annotations.NotNull;

public class FriendListSetting extends AbstractSetting {
    public FriendListSetting(@NotNull SettingsContainer parent, String name, @NotNull BooleanSupplier hideCondition) {
        super(parent, name, hideCondition);
    }

    public FriendListSetting(@NotNull SettingsContainer parent, String name) {
        super(parent, name);
    }

    @Override
    public JsonElement save() {
        return new JsonObject();
    }

    @Override
    public void load(JsonElement element) {
    }
}
