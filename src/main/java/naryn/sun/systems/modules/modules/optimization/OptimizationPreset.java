package naryn.sun.systems.modules.modules.optimization;

import naryn.sun.systems.localization.Localizator;

public enum OptimizationPreset {
    ULTRA("modules.settings.optimizer.preset.ultra"),
    MEDIUM("modules.settings.optimizer.preset.medium"),
    LOW("modules.settings.optimizer.preset.low"),
    POTATO("modules.settings.optimizer.preset.potato"),
    CUSTOM("modules.settings.optimizer.preset.custom");

    private final String key;

    OptimizationPreset(String key) {
        this.key = key;
    }

    public String getLabel() {
        return Localizator.translate(key);
    }
}
