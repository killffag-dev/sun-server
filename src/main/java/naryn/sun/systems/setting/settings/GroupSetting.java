package naryn.sun.systems.setting.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import lombok.Getter;
import naryn.sun.systems.setting.Setting;
import naryn.sun.systems.setting.SettingsContainer;
import naryn.sun.systems.setting.impl.AbstractSetting;
import org.jetbrains.annotations.NotNull;

@Getter
public class GroupSetting extends AbstractSetting implements SettingsContainer {
    private final List<Setting> settings = new ArrayList<>();

    public GroupSetting(@NotNull SettingsContainer parent, String name, @NotNull BooleanSupplier hideCondition) {
        super(parent, name, hideCondition);
    }

    public GroupSetting(@NotNull SettingsContainer parent, String name) {
        super(parent, name);
    }

    @Override
    public List<Setting> getSettings() {
        return this.settings;
    }

    @Override
    public JsonElement save() {
        JsonObject obj = new JsonObject();
        for (Setting s : this.settings) {
            obj.add(s.getName(), s.save());
        }
        return obj;
    }

    @Override
    public void load(JsonElement element) {
        if (element != null && element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Setting s : this.settings) {
                if (obj.has(s.getName())) {
                    s.load(obj.get(s.getName()));
                }
            }
        }
    }
}
