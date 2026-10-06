package naryn.sun.utility.sounds;

import com.google.gson.JsonObject;
import java.util.EnumMap;
import java.util.Map;

public class SoundConfig {
    private float masterVolume = 1.0F;
    private boolean customSoundsEnabled = false;
    private final Map<SoundType, Boolean> enabledMap = new EnumMap<>(SoundType.class);
    private final Map<SoundType, Float> volumeMap = new EnumMap<>(SoundType.class);

    public SoundConfig() {
        resetDefaults();
    }

    public void resetDefaults() {
        this.masterVolume = 1.0F;
        this.customSoundsEnabled = false;
        for (SoundType type : SoundType.values()) {
            this.enabledMap.put(type, true);
            this.volumeMap.put(type, type.getDefaultVolume());
        }
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    public void setMasterVolume(float masterVolume) {
        this.masterVolume = Math.max(0.0F, Math.min(1.0F, masterVolume));
    }

    public boolean isCustomSoundsEnabled() {
        return customSoundsEnabled;
    }

    public void setCustomSoundsEnabled(boolean customSoundsEnabled) {
        this.customSoundsEnabled = customSoundsEnabled;
    }

    public boolean isEnabled(SoundType type) {
        return enabledMap.getOrDefault(type, true);
    }

    public void setEnabled(SoundType type, boolean enabled) {
        enabledMap.put(type, enabled);
    }

    public float getVolume(SoundType type) {
        return volumeMap.getOrDefault(type, type.getDefaultVolume());
    }

    public void setVolume(SoundType type, float volume) {
        volumeMap.put(type, Math.max(0.0F, Math.min(1.0F, volume)));
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("masterVolume", masterVolume);
        json.addProperty("customSoundsEnabled", customSoundsEnabled);

        JsonObject typesJson = new JsonObject();
        for (SoundType type : SoundType.values()) {
            JsonObject item = new JsonObject();
            item.addProperty("enabled", isEnabled(type));
            item.addProperty("volume", getVolume(type));
            typesJson.add(type.name(), item);
        }
        json.add("types", typesJson);
        return json;
    }

    public void fromJson(JsonObject json) {
        if (json == null) return;
        if (json.has("masterVolume")) {
            setMasterVolume(json.get("masterVolume").getAsFloat());
        }
        if (json.has("customSoundsEnabled")) {
            setCustomSoundsEnabled(json.get("customSoundsEnabled").getAsBoolean());
        }
        if (json.has("types") && json.get("types").isJsonObject()) {
            JsonObject typesJson = json.getAsJsonObject("types");
            for (SoundType type : SoundType.values()) {
                if (typesJson.has(type.name()) && typesJson.get(type.name()).isJsonObject()) {
                    JsonObject item = typesJson.getAsJsonObject(type.name());
                    if (item.has("enabled")) {
                        setEnabled(type, item.get("enabled").getAsBoolean());
                    }
                    if (item.has("volume")) {
                        setVolume(type, item.get("volume").getAsFloat());
                    }
                }
            }
        }
    }
}
