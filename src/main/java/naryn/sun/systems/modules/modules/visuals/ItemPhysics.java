package naryn.sun.systems.modules.modules.visuals;

import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;

@ModuleInfo(
    name = "Item Physics",
    category = ModuleCategory.VISUALS,
    desc = "modules.descriptions.item_physics"
)
public class ItemPhysics extends BaseModule {
    // Модуль пассивный (визуальный) — своей логики не требует.
    // Проверка состояния делается напрямую из mixin через:
    // Sun.getInstance().getModuleManager().getModule(ItemPhysics.class).isEnabled()
}