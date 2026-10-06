# ClickGUI (ui/menu) — Reference

Точка входа — `ui/menu/NewScreen.java` (~420 строк). По архитектуре `NewScreen` — **только тонкий host/координатор**: владеет состоянием экрана (текущий таб, текст поиска, фильтры сортировки, layout data, флаги режима редактирования, color pickers, текст тултипа). Все вспомогательные задачи строго вынесены в отдельные классы:
- **Обработка кликов/ввода:** `ui/menu/NewScreenInputHandler.java`
- **Управление скроллом:** `ui/menu/NewScreenScrollState.java`
- **Поиск и фильтрация модулей:** `ui/menu/ModuleSearchHelper.java`
- **Рендеринг интерфейса:** делегирован статическим рендерерам в `panels/`, `toolbar/`, `widgets/` или `skin/`.
- **Карточки настроек:** `ui/menu/components/GuiSettingCard.java` (~140 строк) делегирует логику в подпакет `ui/menu/components/settings/` (`SettingRenderer`, `AppearanceRenderer`, `LanguageRenderer`, `SoundsRenderer`, `AnimationsRenderer`).

## СТРОГОЕ ПРАВИЛО — читать перед любой правкой `NewScreen.java`

`NewScreen.java` строго ограничен по размеру (держим < 400 строк). Любой новый рисующий код (`context.draw*`, `context.pushMatrix`, blur/rounded-rect/текст) идёт в соответствующий файл `panels/`/`toolbar/`/`widgets/`, никогда инлайново в `NewScreen`. Любая новая обработка ввода добавляется в `NewScreenInputHandler`, логика скролла — в `NewScreenScrollState`, а алгоритмы поиска — в `ModuleSearchHelper`.

## Структура `ui/menu/`

```
ui/menu/
├── NewScreen.java                  # thin host: state coordination, delegates input/scroll/search/rendering
├── NewScreenInputHandler.java      # input handling: mouse clicks, drags, releases for tabs/cards/editor/settings
├── NewScreenScrollState.java       # scroll offsets, targets, smooth animations and scrollbar drag states
├── ModuleSearchHelper.java         # multi-language search matching, visibility filtering (HUD/Render, Movement)
├── MenuScreen.java                 # common base for menu screens (open/close animation)
├── MenuTab.java                    # top-level public enum: VISUALS, UTILITY, FAVORITES (3 вкладки)
├── components/
│   ├── NewModuleCard.java          # module card in the grid (toggle, favorite, settings expansion)
│   ├── GuiSettingCard.java         # GUI settings card shell (~140 lines), delegates to settings/
│   ├── GuiSettingType.java         # setting card type enum
│   └── settings/                   # modular setting renderers
│       ├── SettingRenderer.java    # base abstract renderer with shared toggle/slider/mode components
│       ├── AppearanceRenderer.java # UI & HUD material mode-box selector
│       ├── LanguageRenderer.java   # language selector
│       ├── SoundsRenderer.java     # volume sliders, sound toggles, custom sounds folder
│       └── AnimationsRenderer.java # animation speeds and micro-animation toggles
├── dropdown/components/settings/   # module setting-type components (Boolean/Slider/Color/Bind/Mode/Select/Bezier/Range/Button/String)
├── layout/                         # position/scale editors
│   ├── MenuLayoutData.java         # position+scale of window/bottom bar/tooltip, saved to disk
│   ├── MenuLayoutManager.java      # reads/writes MenuLayoutData (file menu_layout_v2.rock)
│   ├── LayoutEditor.java           # drag/resize of window, bottom bar, and tooltip in layout-edit mode
│   ├── HudModuleLayoutManager.java # save/reset HUD-module positions
│   ├── HudModuleEditor.java        # drag/resize of HUD modules, magnet-snap grid dots + SnapGuide lines
│   └── SnapGuide.java              # snap-line calculation shared by HudModuleEditor
├── panels/                         # rendering only — no click handling, no NewScreen field mutation
│   ├── WindowPanel.java            # main window: frame (skin-driven), header (tabs+search), sort chips, card grid, scrollbar
│   ├── BottomBarPanel.java         # bottom island: Modules / HUD Editor / GUI Settings — fully localized via Localizator (menu.bottom_bar.*)
│   ├── GuiSettingsPanel.java       # "GUI Settings" screen (client color, interface material, language, sounds) — translated via Localizator
│   └── TooltipBoxPanel.java        # positionable module-description box, with the text-switch ("roulette") animation
├── toolbar/
│   └── EditorToolbar.java          # floating "Reset/Done" island for layout-edit and module-edit modes,
│                                    # + draggable collapse "sucker" blob + Shift-magnet hint badge — см. ниже
├── widgets/                        # small reusable rendering primitives, used by panels/toolbar
│   ├── ActionButton.java           # plain centered-text button (bottom bar buttons, Reset/Done)
│   └── SegmentedToggle.java        # selected/unselected toggle — appearance-mode switch, language switch,
│                                    # AND now the sort-chip buttons in WindowPanel (see below)
└── skin/                           # visual parameters that depend on ClientAppearance.Mode (Тёмный/Стекло)
    ├── MenuSkin.java                # interface: blurRadius/blurTint/background/border/rowBackground/hoverBackground
    ├── FacetDarkSkin.java           # FACET_DARK — opaque, low blur
    └── FacetFrostSkin.java          # FACET_FROST ("Стекло") — the original Liquid Glass look, more blur/transparency
```

## Opening/Closing

Меню открывается/закрывается через модуль `"Menu"` (`Sun.getInstance().getModuleManager().getModule("Menu")`) — его keybind и состояние `enabled` управляют `NewScreen`. `MenuScreen.menuAnimation` отвечает за анимацию появления/исчезновения, `closing` — флаг, что экран сейчас закрывается (после завершения анимации вызывается `mc.setScreen(null)`).

## Two Independent Transform Layers

Окно меню и нижняя панель рендерятся с независимыми offset и scale — оба берутся из `MenuLayoutData` (`windowX/Y/Scale`, `bottomBarX/Y/Scale`, `tooltipX/Y/Scale`). Каждый кадр `NewScreen.render()` конвертирует координаты мыши в локальное пространство для каждого слоя отдельно (`localWinMouseX/Y` и `localBotMouseX/Y`), делает push/pop матричной трансформации вокруг рендера каждой панели и передаёт локальные координаты мыши в `WindowPanel`/`BottomBarPanel` для hover-теста — поэтому клик по нижней панели и клик по окну обрабатываются с раздельными наборами координат в `NewScreen.onMouseClicked`.

## Вкладки — только 3, без отдельного Movement-таба

`MenuTab` теперь содержит только `VISUALS`, `UTILITY`, `FAVORITES` — отдельного таба `MOVEMENT` больше нет. `NewScreen.categoryToTab()` маппит `ModuleCategory` так: `VISUALS → VISUALS`, `MOVEMENT/COMBAT/PLAYER/OTHER → UTILITY`. Movement-модули теперь достаются через **фильтр внутри таба Utility** (см. ниже), а не отдельной вкладкой.

## Сортировочные чипсы (новое) — WindowPanel.renderSortChips

Внутри табов `VISUALS` и `UTILITY` над сеткой карточек рисуется бокс сортировки (`WindowPanel.renderSortChips`, стилизован под строку настроек через `MenuSkin.renderSettingRow`), со своими сегмент-кнопками (`SegmentedToggle`):

- `NewScreen.VisualsFilter`: `ALL` / `HUD` / `RENDER` — делит модули таба Visuals по признаку `NewScreen.isHudModule(module)` (проверка `instanceof PositionableHudModule` + список хардкоженных имён: armorstatus, keystrokes, potionstatus, fpsping, watermark, activemodules, hotbar, cooldowns, inventoryview, scoreboard).
- `NewScreen.UtilityFilter`: `ALL` / `MOVEMENT` — делит модули таба Utility по признаку `NewScreen.isMovementModule(module)` (категория `MOVEMENT` ИЛИ имя содержит sprint/speed/fly/freelook/zoom/guimove/noslow/strafe/step/timer/spider/jesus).
- Бокс сортировки **не показывается** на вкладке Favorites и во время активного поиска (`WindowPanel.sortChipsHeight()` возвращает 0 в этих случаях) — оба фильтра при этом игнорируются, показываются все видимые карточки.
- Высота бокса (`SORT_ROW_H` + `SORT_ROW_GAP`) учитывается в `computeTotalHeight()` и скроллится вместе со списком карточек, а не закреплена сверху.
- Хит-тест кликов по чипсам — `WindowPanel.hitTestSortChips()`, вызывается из `NewScreen.onMouseClicked` до хит-теста скроллбара/карточек.
- Локализация бокса сортировки: заголовок `menu.sort`, чипсы переведены через `VisualsFilter` / `UtilityFilter` (`menu.visuals_filter.*`, `menu.utility_filter.*`). Ширина кнопок и стартовый отступ рассчитываются динамически по шрифту (подробнее см. `references/localization.md`).

## Module List — WindowPanel

- Карточки создаются в `NewScreen.init()` — одна `NewModuleCard` на каждый `Module` из `Sun.getInstance().getModuleManager().getModules()`.
- Поиск (`searchQuery`) фильтрует по всем табам сразу по имени модуля (`NewScreen.getVisibleCards()`); таб `FAVORITES` фильтрует по `NewModuleCard.isFavorite()`; иначе (Visuals/Utility) дополнительно применяется активный `VisualsFilter`/`UtilityFilter`.
- `WindowPanel.renderHeader()` рисует табы/поиск, `WindowPanel.renderCardGrid()` рисует бокс сортировки + сетку модулей (возвращает строку описания наведённой карточки, которую `NewScreen` присваивает тултипу) — но фактический hit-test клика по табу/фокуса поиска/чипсам сортировки/клика по карточке живёт в `NewScreen.onMouseClicked`, дублируя те же геометрические константы (`TOP_PAD_X`, `TAB_PAD_X`, `SORT_START_X_OFFSET` и т.д.), а не общий объект `Rect`. При изменении одной стороны — синхронизировать константы вручную.
- Скроллбар (`WindowPanel.renderScrollbar`) теперь **перетаскиваемый** (drag) — не только клик по дорожке, но и захват ползунка мышью (`scrollbarDragging`/`scrollbarDragOffset` в `NewScreen`, обработка в `onMouseDragged`/`onMouseReleased`).

## Нижняя панель — BottomBarPanel (изменённый состав кнопок)

Три кнопки, порядок и назначение **изменились** относительно более старой версии:

1. **`Modules`** — возвращает из GUI Settings обратно в сетку модулей (`guiSettingsVisible = false`).
2. **`HUD Editor`** — при наведении раскрывается на две половины: **`Modules`** (переключает в `moduleEditMode` — редактирование позиций HUD-элементов) и **`Interface`** (переключает в `layoutEditMode` — редактирование позиции/масштаба самого окна/нижней панели/тултипа).
3. **`GUI Settings`** — открывает `GuiSettingsPanel`.

Кнопка **`Configs`, которая раньше была третьей и ни на что не влияла, убрана полностью** — на её месте теперь рабочая `GUI Settings`, а `Modules` занял освободившееся первое место. Если в переписке или в старых заметках встречается "Configs" — это устаревшее состояние, в текущей версии кнопки нет.

Лейблы `BottomBarPanel` (`Modules`, `HUD Editor`, `GUI Settings`, popup `Modules`/`Interface`) переведены через `Localizator` (ключи `menu.bottom_bar.modules`, `menu.bottom_bar.hud_editor`, `menu.bottom_bar.gui_settings`, `menu.bottom_bar.interface`) — в коде используются массивы `LABEL_KEYS` и `POPUP_KEYS`, никакого хардкода.

## EditorToolbar — существенно расширен: collapse-капля + Shift-хинт

Раньше `EditorToolbar` был просто плавающим островком "Reset/Done". Сейчас у него три визуальных элемента:

1. **Островок Reset/Done** — как раньше, но теперь плавно сворачивается (`collapseAnim`) в маленькую капсулу вместо простого показа/скрытия.
2. **"Капля"-переключатель (sucker)** — маленькая капсула `SUCKER_W×SUCKER_H`, изначально прилеплена снизу к островку (соединена визуальным "язычком"), но её **можно перетащить в любое место экрана** (`suckerDragging`/`suckerCustomPos`/`suckerX`/`suckerY`). Клик без перетаскивания переключает `collapsed` — сворачивает/разворачивает основной островок Reset/Done. Индикатор-точка внутри капли меняет цвет: зелёный, когда свёрнуто, акцентный — когда развёрнуто. Подпись — `menu.editor.show`/`menu.editor.hide`.
3. **Плашка-подсказка про Shift** внизу экрана (`renderShiftHint`) — текст из ключа `menu.editor.shift_hint` ("Зажмите Shift для отключения магнита"), сворачивается вместе с основным островком. Относится к магнитной привязке в `HudModuleEditor`/`SnapGuide` — зажатый Shift отключает snap при перетаскивании HUD-модуля.

**Известный нюанс в коде**: и лейбл капли, и текст Shift-подсказки продублированы хардкодом сразу после вызова `Localizator.translate(...)` — если перевод возвращает саму строку ключа (т.е. не нашёлся), код вручную подставляет русский текст (`if (suckerLabel.equals("menu.editor.show")) suckerLabel = "Показать";` и аналогично для hide/shift_hint). Ключи фактически есть в `ui/ui.lang` для обоих языков (`ru_ru` и `en_us`), так что это выглядит как защитный костыль на случай проблем загрузки локализации, а не обязательный путь — но если правишь эти ключи, обнови оба места (сам `.lang` и хардкод-fallback в `EditorToolbar.java`), иначе будет расхождение.

**Дополнительно**: ключи `menu.toolbar.reset`/`menu.toolbar.done` расположены в `ui/ui.lang`.

## HudModuleEditor — визуальные добавления

Логика перетаскивания/ресайза HUD-модулей (`Mode.MOVE`/`Mode.RESIZE`, ручка в правом нижнем углу, snap через `SnapGuide.calculateSnap`) не изменилась по сути, но при перетаскивании (`isDraggingMove`) теперь плавно проявляется **фоновая сетка точек-гайдов** (`GUIDE_DOT_SPACING = 16`, `guideDotsAnim`) поверх всего экрана — чисто визуальный ориентир, не влияет на сам snap (snap считается тем же `SnapGuide.calculateSnap` от границ экрана и других модулей, как раньше).

### Localization inside the ClickGUI
 
- **Вся текстовая составляющая ClickGUI** переведена через `Localizator.translate(...)`: табы `NewScreen` (`menu.tab.*`), строка поиска (`menu.search.placeholder`), карточки модулей (`modules.names.*`, `menu.module.keybind.*`), бокс сортировки (`menu.sort`, `menu.visuals_filter.*`, `menu.utility_filter.*`), нижний островок `BottomBarPanel` (`menu.bottom_bar.*`), карточки `GuiSettingsPanel` (`gui.setting.*.name`, `gui.setting.*.desc`, `menu.gui_settings.*`), кнопки тулбара `EditorToolbar` (`menu.toolbar.*`, `menu.editor.*`).
- Полное руководство по структуре ключей, модульной загрузке .lang-файлов и правилам добавления переводов вынесено в отдельный документ: **`references/localization.md`**.

### Tooltip — TooltipBoxPanel

Бокс описания модуля не привязан к курсору — у него своя позиция и масштаб в `MenuLayoutData.tooltipX/Y/Scale`, двигается тем же `LayoutEditor`, что окно/нижняя панель. `NewScreen` обновляет `tooltipText` раз в кадр и, когда он меняется, перезапускает анимацию переключения (`tooltipSwitchAnim`). `TooltipBoxPanel.render()` рисует **плавное появление и растворение строго на месте** (`offsetY = 0.0F`): при смене текста старый плавно растворяется, а новый проявляется без вертикального смещения ("рулетки" больше нет).

## Layout Editors

`LayoutEditor` двигает и масштабирует три элемента (`LayoutEditor.Element`: `WINDOW`, `BOTTOM_BAR`, `TOOLTIP`), не даёт им перекрываться (`collidesWithOther`), сохраняет результат через `MenuLayoutManager`. `HudModuleEditor` работает так же для позиций HUD-модулей (`PositionableHudModule`), сохраняется через `HudModuleLayoutManager`. Оба редактора рисуются одним `EditorToolbar.render()` (кнопки "Reset"/"Done" + капля + Shift-хинт, см. выше); фактическое поведение reset/save/exit для каждого режима реализовано отдельно в `NewScreen.handleEditorClick()` (layout mode) и `NewScreen.handleModuleEditorClick()` (module mode) — сам `EditorToolbar` только рендерит и отдаёт константы `WIDTH`/`HEIGHT`/`TOP_OFFSET` для hit-теста.

## Visual mode — MenuSkin (Тёмный/Стекло)

`ClientAppearance.Mode` (`FACET_DARK`/`FACET_FROST`) читается в `MenuSkin.current()` и управляет всеми визуальными параметрами GUI:

1. **Панели окна и тулбары:** `renderPanelShell()`, `renderRecessedChip()`, `renderSettingRow()` (используется и `GuiSettingsPanel`, и новым боксом сортировки), `blurRadius()`, `blurTint()`, `background()`, `border()`, `rowBackground()`, `hoverBackground()`.
2. **Табы/поиск:** `renderActiveTab()`, `tabTextColor()`, `renderSearchFocus()`.
3. **Интерактивные контролы (тумблеры, слайдеры, бинды):** `renderToggleTrack()` (с плавным `mix` цветов трека), `renderToggleThumb()` (с поддержкой растяжения по ширине для физики переключателей), `renderSliderTrack()`, `renderSliderFill()`, `renderSliderThumb()`, `renderBindChip()`.

- `FacetDarkSkin.java` — **Dark Neomorphism:** сплошные графитовые фоны (`#171820`), двойные микро-тени (тёмная снизу-справа + светлый блик сверху-слева для выпуклых; обратные inset-тени для вдавленных треков), без блюра.
- `FacetFrostSkin.java` — **Glassmorphic Neomorphism:** полупрозрачные фоны с шейдерным блюром заднего плана (Kawase), белые световые фаски (1px specular highlights), светящиеся неоморфные бегунки и стеклянные линзы/капсулы (`squircle`, `drawLiquidGlass`).

`MenuSkin.current()` управляет также background/border/blur цветами, используемыми `WindowPanel.renderFrame()`, `BottomBarPanel.render()`, боксом сортировки и рядами `GuiSettingsPanel`. При изменении внешнего вида любого режима — редактировать только `FacetDarkSkin.java`/`FacetFrostSkin.java`.

**Строгое правило:** сами панели (`WindowPanel`, `BottomBarPanel`) и компоненты настроек (`BooleanSettingComponent`, `SliderSettingComponent`, `BindSettingComponent`, `NewModuleCard`) **никогда не содержат цветовые литералы режимов и не вызывают `Interface.glass()` напрямую** — весь рендеринг делегируется методам `MenuSkin.current()`.


## GuiSettingsPanel — актуальный состав

GuiSettingsPanel.java — статический класс (все методы static, нет instance). Рендерит карточки экрана GUI Settings, который открывается кнопкой «GUI Settings» в нижней панели. Карточки имеют тип `GuiSettingCard` и разворачиваются по клику ПКМ.

### Карточки настроек (`GuiSettingType`):

1. **Материал интерфейса** (`APPEARANCE`) — переключатель FacetDarkSkin / FacetFrostSkin (через ClientAppearance.Mode).
2. **Язык** (`LANGUAGE`) — переключатель языков (ru/en/uk/pl) через SegmentedToggle.
3. **Звуки** (`SOUNDS`) — блок звуков интерфейса и кастомных звуков. Подробности в `references/sound-system.md`.
4. **Анимации** (`ANIMATIONS`) — блок управления анимациями интерфейса: мастер-переключатель, 5 под-тумблеров (поднятие кнопок, плавные вкладки, плавный тултип, эффект звёздочки, физика переключателей) и слайдер скорости (0.5x – 2.0x). Подробности в `references/animation-system.md`.

### Скроллинг GuiSettings:

NewScreen хранит отдельные поля для скролла этой панели:
`java
float guiSettingsScrollOffset;
float guiSettingsScrollTarget;
boolean guiSettingsDragging;
float guiSettingsDragOffset;
`

### Методы делегирования из NewScreen:

`java
GuiSettingsPanel.render(context, panelX, panelY, panelW, mouseX, mouseY, scrollOffset);
GuiSettingsPanel.handleMouseClicked(mouseX, mouseY, button, scrollOffset);
GuiSettingsPanel.handleMouseDragged(mouseX, mouseY, deltaX, deltaY, scrollOffset);
GuiSettingsPanel.handleMouseReleased(mouseX, mouseY, button);
`
## Module Settings in the Menu

Компоненты в `ui/menu/dropdown/components/settings/impl/` (`BooleanSettingComponent`, `SliderSettingComponent`, `ColorSettingComponent`, `BezierSettingComponent`, `RangeSettingComponent`, `ButtonSettingComponent`, `StringSettingComponent`, `ModeSettingComponent`, `SelectSettingComponent`, `BindSettingComponent`) рендерятся внутри развёрнутой `NewModuleCard`, каждый обрабатывает один тип `Setting` из `systems/setting/settings/`. `ColorSettingComponent` по клику создаёт `ColorPicker` и добавляет его в общий список через `Sun.getInstance().getMenuScreen() instanceof NewScreen newScreen → newScreen.getColorPickers()` — color pickers хранятся и рендерятся в `NewScreen` поверх всего интерфейса, в реальных экранных координатах, независимо от масштаба окна или нижней панели.
