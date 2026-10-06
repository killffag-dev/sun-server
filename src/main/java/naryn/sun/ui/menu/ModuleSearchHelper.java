package naryn.sun.ui.menu;

import naryn.sun.systems.localization.Language;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.modules.Module;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.impl.PositionableHudModule;
import naryn.sun.ui.menu.components.NewModuleCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Хелпер для поиска и фильтрации модулей в ClickGUI.
 * Вынесен из NewScreen для снижения размера класса.
 */
public final class ModuleSearchHelper {

    private ModuleSearchHelper() {}

    public static boolean isHudModule(Module module) {
        if (module instanceof PositionableHudModule) return true;
        String name = module.getName().toLowerCase();
        return name.contains("hud") || name.equals("armorstatus") || name.equals("keystrokes")
                || name.equals("potionstatus") || name.equals("fpsping") || name.equals("watermark")
                || name.equals("activemodules") || name.equals("hotbar") || name.equals("cooldowns")
                || name.equals("inventoryview") || name.equals("scoreboard");
    }

    public static boolean isMovementModule(Module module) {
        String name = module.getName().toLowerCase();
        return name.contains("sprint") || name.contains("speed") || name.contains("fly")
                || name.contains("freelook") || name.contains("zoom") || name.contains("guimove")
                || name.contains("noslow") || name.contains("strafe") || name.contains("step")
                || name.contains("timer") || name.contains("spider") || name.contains("jesus");
    }

    public static boolean matchesSearch(Module module, String query) {
        if (query == null || query.isEmpty()) return true;

        List<String> queryVariants = SearchTextHelper.getSearchVariants(query);

        String rawName = module.getName().toLowerCase();
        String rawNameClean = rawName.replace(" ", "").replace("_", "");

        String displayName = module.getDisplayName().toLowerCase();
        String displayNameClean = displayName.replace(" ", "").replace("_", "");

        String key = "modules.names." + module.getName().toLowerCase().replace(" ", "_");
        String ru = Localizator.translate(Language.RU_RU, key);
        String ruLow = ru != null ? ru.toLowerCase() : "";
        String ruClean = ruLow.replace(" ", "").replace("_", "");

        String en = Localizator.translate(Language.EN_US, key);
        String enLow = en != null ? en.toLowerCase() : "";
        String enClean = enLow.replace(" ", "").replace("_", "");

        for (String q : queryVariants) {
            String qClean = q.replace(" ", "").replace("_", "");
            if (rawName.contains(q) || rawNameClean.contains(qClean)) return true;
            if (displayName.contains(q) || displayNameClean.contains(qClean)) return true;
            if (!ruLow.isEmpty() && (ruLow.contains(q) || ruClean.contains(qClean))) return true;
            if (!enLow.isEmpty() && (enLow.contains(q) || enClean.contains(qClean))) return true;
        }

        return false;
    }

    /**
     * Возвращает видимые карточки с учётом поиска, таба и фильтра.
     */
    public static List<NewModuleCard> getVisibleCards(
            Map<MenuTab, List<NewModuleCard>> tabCards,
            MenuTab currentTab,
            String searchQuery,
            NewScreen.VisualsFilter visualsFilter,
            NewScreen.UtilityFilter utilityFilter) {

        if (!searchQuery.isEmpty()) {
            List<NewModuleCard> r = new ArrayList<>();
            for (List<NewModuleCard> l : tabCards.values())
                for (NewModuleCard c : l)
                    if (matchesSearch(c.getModule(), searchQuery)) r.add(c);
            return r;
        }
        if (currentTab == MenuTab.FAVORITES) {
            List<NewModuleCard> r = new ArrayList<>();
            for (List<NewModuleCard> l : tabCards.values())
                for (NewModuleCard c : l)
                    if (c.isFavorite()) r.add(c);
            return r;
        }
        List<NewModuleCard> base = tabCards.getOrDefault(currentTab, List.of());
        if (currentTab == MenuTab.VISUALS && visualsFilter != NewScreen.VisualsFilter.ALL) {
            List<NewModuleCard> r = new ArrayList<>();
            for (NewModuleCard c : base) {
                boolean hud = isHudModule(c.getModule());
                if (visualsFilter == NewScreen.VisualsFilter.HUD && hud) r.add(c);
                if (visualsFilter == NewScreen.VisualsFilter.RENDER && !hud) r.add(c);
            }
            return r;
        }
        if (currentTab == MenuTab.UTILITY && utilityFilter != NewScreen.UtilityFilter.ALL) {
            List<NewModuleCard> r = new ArrayList<>();
            for (NewModuleCard c : base) {
                boolean mov = isMovementModule(c.getModule());
                if (utilityFilter == NewScreen.UtilityFilter.MOVEMENT && mov) r.add(c);
            }
            return r;
        }
        return base;
    }
}
