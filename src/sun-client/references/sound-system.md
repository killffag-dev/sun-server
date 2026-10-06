# Sound System — Reference

## Overview

Звуковая система полностью самостоятельна: старый модуль `Sounds.java` удалён, все звуки теперь управляются через `ClientSoundManager` (синглтон) из пакета `utility/sounds/`.

Настройки звуков сохраняются в `client.rock` (через `ClientDataFile`), а не в обычном конфиге модулей.

---

## Package: `utility/sounds/`

```
utility/sounds/
├── SoundType.java           # enum: SCROLL, TYPING, MODULE_TOGGLE, BUTTON_CLICK, MENU_OPEN
├── SoundConfig.java         # модель настроек: masterVolume, per-type enabled+volume, customSoundsEnabled
├── ClientSounds.java        # идентификаторы зарегистрированных звуков (константы SoundEvent)
├── ClientSoundInstance.java # extends PositionedSoundInstance — то, что реально проигрывает каждую константу ClientSounds.*
├── ClientSoundManager.java  # синглтон-менеджер — единая точка входа
├── CustomSoundPlayer.java   # воспроизведение WAV-файлов из папки пользователя
├── MusicTracker.java        # другая подсистема: интеграция с медиаплеером ОС для модуля MusicModule — не описана в этом файле
└── LyricsFetcher.java       # другая подсистема: подтягивание текста песни для MusicModule — не описана в этом файле
```

---

## `SoundType.java` — enum типов звуков

```java
public enum SoundType {
    SCROLL,         // прокрутка списка модулей
    TYPING,         // печать в поле поиска
    MODULE_TOGGLE,  // включение/выключение модуля
    BUTTON_CLICK,   // нажатие кнопки UI (Boolean/Button/Mode/Select settings)
    MENU_OPEN       // открытие меню
}
```

---

## `SoundConfig.java` — настройки звуков

Поля:
- `float masterVolume` — глобальная громкость (0.0–1.0), по умолчанию `0.5f`.
- `boolean customSoundsEnabled` — использовать ли пользовательские WAV-файлы из папки `Sun/sounds/`.
- `Map<SoundType, Boolean> enabled` — включён ли каждый тип звука (все по умолчанию `true`).
- `Map<SoundType, Float> volume` — громкость каждого типа (0.0–1.0), по умолчанию `0.7f`.

Методы:
- `toJson()` / `fromJson(JsonObject)` — сериализация для `client.rock`.
- `isEnabled(SoundType)`, `getVolume(SoundType)`, `getMasterVolume()`, `isCustomSoundsEnabled()`, а также setters.

---

## `ClientSounds.java` — зарегистрированные звуки

Все звуки зарегистрированы в `src/main/resources/assets/sun/sounds.json`.

Текущие идентификаторы:
```java
// Интерфейсные звуки
public static final SoundEvent SCROLL   = register("scroll");
public static final SoundEvent TYPING   = register("typing");
public static final SoundEvent TOGGLE_ON  = register("toggle_on");
public static final SoundEvent TOGGLE_OFF = register("toggle_off");
public static final SoundEvent CLICK    = register("click");
public static final SoundEvent OPEN     = register("open");

// ASMR (заготовка, пока не используется ни одним модулем)
public static final SoundEvent ASMR_MARSHMALLOW_1 = register("asmr_marshmallow_1");
public static final SoundEvent ASMR_MARSHMALLOW_2 = register("asmr_marshmallow_2");
public static final SoundEvent ASMR_MARSHMALLOW_3 = register("asmr_marshmallow_3");
public static final SoundEvent ASMR_MARSHMALLOW_4 = register("asmr_marshmallow_4");
public static final SoundEvent ASMR_MARSHMALLOW_5 = register("asmr_marshmallow_5");
```

Файлы `.ogg` лежат в `src/main/resources/assets/sun/sounds/`.
Файлы `.wav` (для `CustomSoundPlayer`) тоже лежат там же.

**Важно (проверено по коду и `sounds.json`):** модуля `ASMRBlocks.java` в проекте не существует — это ещё не реализованная фича. Константы `ASMR_MARSHMALLOW_1..5` объявлены и зарегистрированы в `sounds.json` (`sun:asmr/marshmallow/press_1..5`), но нигде в коде не вызываются. Из пяти ассетов реально лежит на диске только `sounds/asmr/marshmallow/press_1.ogg` — `press_2.ogg`...`press_5.ogg` отсутствуют, хотя прописаны в `sounds.json`. При реализации ASMR-фичи: доложить недостающие `.ogg`, создать сам модуль (вероятно в `systems/modules/modules/visuals/`) и подключить его к `ClientSounds.ASMR_MARSHMALLOW_*` — готового кода для этого пока нет, только заготовка констант. Точка опоры уже есть — `moscow.sun.access.MCWorldAccess` (проверка воздуха/коллизий над блоком), её doc-комментарий сам ссылается на будущий "ASMRBlocks".

---

## `ClientSoundManager.java` — синглтон

**Получение инстанса:**
```java
ClientSoundManager manager = ClientSoundManager.getInstance();
```

**Методы воспроизведения:**
```java
manager.playScroll();                      // SCROLL
manager.playTyping();                      // TYPING (дебаунс 20 мс)
manager.playModuleToggle(boolean enabled); // TOGGLE_ON или TOGGLE_OFF
manager.playButtonClick();                 // BUTTON_CLICK
manager.playMenuOpen();                    // MENU_OPEN
```

**Дополнительные:**
```java
manager.playSoundPreview(SoundType type); // превью в GuiSettingsPanel
manager.openSoundsFolder();              // открывает папку Sun/sounds/ в проводнике
manager.getConfig();                     // SoundConfig — текущие настройки
```

**Дебаунс:** SCROLL — 30 мс, TYPING — 20 мс (предотвращает спам звуков при быстром скролле/печати).

**Питч:** при воспроизведении добавляется случайный питч для вариативности.

**Логика воспроизведения:**
1. Если `customSoundsEnabled = true`, ищет файл `{Sun/sounds/<soundname>.wav}` и воспроизводит через `CustomSoundPlayer` (javax.sound.sampled).
2. Иначе воспроизводит через `ClientSoundInstance.play(ClientSounds.*, volume)` -> `MinecraftClient.getSoundManager().play()` с `SoundCategory.MASTER`, `AttenuationType.NONE`.

---

## `CustomSoundPlayer.java` — пользовательские звуки

Воспроизводит WAV-файлы из папки `{Minecraft game dir}/Sun/sounds/` через `javax.sound.sampled`.

Пользователь может положить свои файлы туда, назвав их так же как встроенные:
`scroll.wav`, `typing.wav`, `toggle_on.wav`, `toggle_off.wav`, `click.wav`, `open.wav`

---

## Где используются звуки (call sites)

| Место | Метод |
|-------|-------|
| `BaseModule.setEnabled()` | `playModuleToggle(enabled)` |
| `ConfigFile.load()` | `playMenuOpen()` (при загрузке конфига) |
| `MenuModule.onEnable()` | `playMenuOpen()` |
| `TextField.charTyped()` / `onKeyPressed()` | `playTyping()` |
| `NewScreen.mouseScrolled()` | `playScroll()` |
| `NewScreen` (tab click, sort chip, bottom bar) | `playButtonClick()` |
| `BooleanSettingComponent.onClick()` | `playButtonClick()` |
| `ButtonSettingComponent.onAction()` | `playButtonClick()` |
| `ModeSettingComponent.onSelect()` | `playButtonClick()` |
| `SelectSettingComponent.onToggle()` | `playButtonClick()` |

---

## GuiSettingsPanel — блок звуков

В `GuiSettingsPanel.java` есть блок «Звуки» (после блока «Язык»).

### Состав UI блока звуков:

1. **Заголовок «Звуки»** + кнопка «Открыть папку звуков» — вызывает `manager.openSoundsFolder()`.
2. **Мастер-слайдер** «Громкость» — `masterVolume` (0–100%).
3. **Тумблер «Custom звуки»** — `customSoundsEnabled`.
4. **5 строк** (по одной на каждый SoundType) — toggle (вкл/выкл) + слайдер (громкость) + кнопка превью.

Статические методы делегирования (`render`, `handleMouseClicked`, `handleMouseDragged`, `handleMouseReleased`, `getTotalHeight()`) и скроллинг панели в `NewScreen` — см. `references/clickgui.md`, раздел "GuiSettingsPanel — актуальный состав". Здесь не дублируется.

### Enum `DraggingSlider` (static field в GuiSettingsPanel):

Отслеживает какой слайдер сейчас перетаскивается:
`NONE`, `MASTER`, `SCROLL`, `TYPING`, `MODULE_TOGGLE`, `BUTTON_CLICK`, `MENU_OPEN`

Поля скролла панели (`guiSettingsScrollOffset` и т.д.) — тоже в `clickgui.md`.

---

## Сохранение настроек звуков

Настройки звуков (`SoundConfig`) живут в `client.rock` через `ClientDataFile`:

```java
// write
json.add("sounds", ClientSoundManager.getInstance().getConfig().toJson());
// read
if (json.has("sounds")) {
    ClientSoundManager.getInstance().getConfig().fromJson(json.getAsJsonObject("sounds"));
}
```

Не ищи настройки звуков в `autosave.rock` или других `.rock` файлах.

---

## Звуковые ассеты

**Расположение:** `src/main/resources/assets/sun/sounds/`

| Файл | Назначение |
|------|-----------|
| `scroll.ogg` / `scroll.wav` | звук прокрутки |
| `typing.ogg` / `typing.wav` | звук печатания |
| `toggle_on.ogg` / `toggle_on.wav` | включение модуля |
| `toggle_off.ogg` / `toggle_off.wav` | выключение модуля |
| `click.ogg` / `click.wav` | нажатие кнопки |
| `open.ogg` / `open.wav` | открытие меню |
| `asmr/marshmallow/press_1.ogg` | единственный реально существующий ASMR-файл; `press_2..5.ogg` прописаны в `sounds.json`, но отсутствуют на диске |

При добавлении нового звука: `.ogg` в папку + запись в `sounds.json` + константа в `ClientSounds.java` + `.wav` для custom.

---

## Локализация

Ключи в `ru_ru/ui/ui.lang` и `en_us/ui/ui.lang`:

```
menu.gui_settings.sounds.title       = Звуки
menu.gui_settings.sounds.master      = Громкость
menu.gui_settings.sounds.custom      = Custom звуки
menu.gui_settings.sounds.open_folder = Открыть папку звуков
menu.gui_settings.sounds.scroll      = Скролл
menu.gui_settings.sounds.typing      = Печатание
menu.gui_settings.sounds.toggle      = Включение модуля
menu.gui_settings.sounds.click       = Нажатие кнопки
menu.gui_settings.sounds.menu_open   = Открытие меню
```

---

## Что было удалено

- `systems/modules/modules/other/Sounds.java` — **удалён**, импорт убран из `ModuleManager.java`.
- Старые файлы: `applepay.ogg`, `clickgui_open.ogg`, `critical.ogg`, `toggle.ogg` (старый), `typing.ogg` (старый).

---

## Правила при работе со звуковой системой

1. Новый звук в UI → вызывать через `ClientSoundManager.getInstance().play*()`, не напрямую через `MinecraftClient.getSoundManager()`.
2. Новый тип звука → добавить в `SoundType`, дефолты в `SoundConfig`, строку в `GuiSettingsPanel`, ключи в `ui/ui.lang` для обоих языков (`ru_ru` и `en_us`).
3. Новый звуковой файл → `.ogg` + `sounds.json` + `ClientSounds` + `.wav`.
4. ASMR-звуки (`ASMR_MARSHMALLOW_1..5`) — заготовка без модуля-потребителя, никакого `ASMRBlocks.java` пока нет; не путать реальные имена констант со старым `asmr_1..5`.
5. Настройки звуков живут в `client.rock`, не в `autosave.rock`.
