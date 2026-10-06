package naryn.sun.systems.modules.modules.utility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.window.KeyPressEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.Setting;
import naryn.sun.systems.setting.settings.ButtonSetting;
import naryn.sun.systems.setting.settings.CommandBindEntrySetting;
import naryn.sun.systems.setting.settings.StringSetting;

@ModuleInfo(name = "Command Bind", category = ModuleCategory.UTILITY)
public class CommandBind extends BaseModule {

    // Источник истины: весь список шаблонов лежит здесь одним JSON-массивом.
    // Настройка всегда скрыта (hideCondition = true) — это не UI-элемент,
    // а просто носитель данных, который штатно проходит через ConfigFile.
    // .text("") обязателен: NewModuleCard всё равно строит для неё
    // StringSettingComponent даже когда она скрыта, а тот безусловно
    // делает textField.paste(getText()) — null здесь роняет NPE
    // прямо в NewScreen.init() и ломает построение карточек модулей.
    private final StringSetting data = new StringSetting(this, this.getSettingName("data"), () -> true) {
        {
            this.text("");
        }

        @Override
        public JsonElement save() {
            JsonArray array = new JsonArray();
            for (CommandBindEntrySetting entry : CommandBind.this.entries) {
                JsonObject obj = new JsonObject();
                obj.addProperty("key", entry.getKey());
                obj.addProperty("command", entry.getCommand());
                array.add(obj);
            }
            return array;
        }

        @Override
        public void load(JsonElement element) {
            // Пока ConfigFile идёт своим for-each по старому объекту-списку настроек,
            // мы подменяем ссылку на новый список — новые записи добавляются уже туда,
            // старый итератор их не увидит и не упадёт с ConcurrentModificationException.
            CommandBind.this.entries.clear();
            CommandBind.this.setSettings(new ArrayList<>(CommandBind.this.getSettings()));

            if (element != null && element.isJsonArray()) {
                for (JsonElement el : element.getAsJsonArray()) {
                    JsonObject obj = el.getAsJsonObject();
                    int key = obj.has("key") ? obj.get("key").getAsInt() : -1;
                    String command = obj.has("command") ? obj.get("command").getAsString() : "";
                    CommandBind.this.addEntry(key, command);
                }
            }
        }
    };

    private final ButtonSetting addTemplate = new ButtonSetting(this, this.getSettingName("add_template"))
        .action(() -> this.addEntry(-1, ""));

    private final List<CommandBindEntrySetting> entries = new ArrayList<>();

    private final EventListener<KeyPressEvent> onKeyPress = event -> {
        if (event.getAction() != 1 || mc.currentScreen != null || mc.player == null) {
            return;
        }

        for (CommandBindEntrySetting entry : new ArrayList<>(this.entries)) {
            int key = entry.getKey();
            if (key != -1 && key == event.getKey()) {
                this.send(entry.getCommand());
            }
        }
    };

    private void send(String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        if (text.startsWith("/")) {
            mc.player.networkHandler.sendChatCommand(text.substring(1));
        } else {
            mc.player.networkHandler.sendChatMessage(text);
        }
    }

    private void addEntry(int key, String command) {
        this.setSettings(new ArrayList<>(this.getSettings()));

        CommandBindEntrySetting[] holder = new CommandBindEntrySetting[1];
        CommandBindEntrySetting entry = new CommandBindEntrySetting(
            this,
            this.getSettingName("entry") + "_" + System.identityHashCode(holder),
            key,
            command,
            () -> this.removeEntry(holder[0])
        );
        holder[0] = entry;

        this.entries.add(entry);
    }

    private void removeEntry(CommandBindEntrySetting entry) {
        this.entries.remove(entry);

        List<Setting> settings = new ArrayList<>(this.getSettings());
        settings.remove(entry);
        this.setSettings(settings);
    }
}