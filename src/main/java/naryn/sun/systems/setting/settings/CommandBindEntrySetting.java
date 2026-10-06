package naryn.sun.systems.setting.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import naryn.sun.systems.setting.SettingsContainer;
import naryn.sun.systems.setting.impl.AbstractSetting;
import org.jetbrains.annotations.NotNull;

public class CommandBindEntrySetting extends AbstractSetting {
    private int key = -1;
    private String command = "";
    private Runnable deleteAction;

    public CommandBindEntrySetting(@NotNull SettingsContainer parent, String name, int key, String command, Runnable deleteAction) {
        super(parent, name);
        this.key = key;
        this.command = command != null ? command : "";
        this.deleteAction = deleteAction;
    }

    public int getKey() {
        return this.key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public String getCommand() {
        return this.command;
    }

    public void setCommand(String command) {
        this.command = command != null ? command : "";
    }

    public void delete() {
        if (this.deleteAction != null) {
            this.deleteAction.run();
        }
    }

    @Override
    public JsonElement save() {
        JsonObject obj = new JsonObject();
        obj.addProperty("key", this.key);
        obj.addProperty("command", this.command);
        return obj;
    }

    @Override
    public void load(JsonElement element) {
        if (element != null && element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("key")) {
                this.key = obj.get("key").getAsInt();
            }
            if (obj.has("command")) {
                this.command = obj.get("command").getAsString();
            }
        }
    }
}
