# Localization System (Localizator & .lang) — Reference

Полное руководство по системе локализации Fabric-клиента "SUN" (пакет `moscow.sun.systems.localization`).
Предназначено для любого ИИ-ассистента (включая веб-ИИ без доступа к файловой системе), чтобы понимать устройство переводов, правила добавления ключей и интеграцию с ClickGUI без необходимости читать весь проект.

---

## 1. Архитектура и ключевые классы

### Localizator (`moscow.sun.systems.localization.Localizator`)
Центральный статический класс переводов:
- Хранит карту загруженных переводов: `Map<String, String> translations = new HashMap<>()`.
- Текущий выбранный язык: `Language currentLanguage` (по умолчанию `Language.RU_RU`).
- **Методы получения перевода:**
  - `Localizator.translate(String key)` — возвращает перевод. **КРИТИЧНО:** если ключ не найден в карте переводов, возвращается сам `key` (например, строка `"menu.tab.visuals"` отобразится на экране как есть).
  - `Localizator.translate(String key, Object... args)` — подставляет аргументы через `String.format`.
  - `Localizator.translateOrDefault(String key, String defaultValue)` — fallback на переданное значение по умолчанию, если перевод отсутствует.
- **Смена языка:**
  - `Localizator.setLanguage(Language lang)` — обновляет `currentLanguage`, очищает карту и вызывает `loadTranslations()`.
  - Смена языка инициируется через `ClientAppearance.setLanguage(Language)` при старте игры (чтение `client.rock`) и при клике в селекторе языков GUI Settings (`GuiSettingCard.java`).

### Language (`moscow.sun.systems.localization.Language`)
Enum поддерживаемых языков клиента:
- `RU_RU("ru_ru")` — Русский (язык по умолчанию, `DEFAULT_LANG`).
- `EN_US("en_us")` — English.
*(Примечание: языки `uk_ua` и `pl_pl` удалены из проекта, клиент поддерживает только русскую и английскую локализации).*

---

## 2. Структура файлов и порядок загрузки

Все языковые ресурсы располагаются в директории `src/main/resources/assets/sun/lang/`.

### Модульная организация (чистая схема без монолита):
Для каждого языка выделена отдельная подпапка по коду языка (`ru_ru/` и `en_us/`), содержащая ровно 4 модульных файла:
- `ui/ui.lang` — элементы интерфейса: табы ClickGUI, строка поиска, нижний островок, карточки GUI Settings, тулбар HUD-редактора, бинды, команды.
- `modules/modules.lang` — названия модулей (`modules.names.<module_key>`).
- `settings/settings.lang` — названия и режимы настроек модулей (`modules.settings.<module_key>.<setting_id>`).
- `descriptions/descriptions.lang` — описания модулей для тултипа (`modules.descriptions.<module_key>`).

### Алгоритм загрузки (`Localizator.loadTranslations()`):
1. Очистка текущих переводов (`translations.clear()`).
2. Загрузка происходит строго в один этап через массив `SUB_LANG_FILES = {"ui/ui.lang", "modules/modules.lang", "settings/settings.lang", "descriptions/descriptions.lang"}`:
   Последовательно считывается каждый из 4 модульных файлов по пути `/assets/sun/lang/<lang_code>/<subfile>`.
3. Каждая строка разбирается методом `parseLine`: игнорируются комментарии (`#`) и пустые строки, ключ отделяется от значения первым знаком `=` (`key=value`).

*(Исторический контекст: ранее клиент использовал двухслойную схему, где первым шагом загружался базовый монолитный файл `ru_ru.lang`/`en_us.lang`, а затем модульные файлы накладывались поверх. Монолитные файлы полностью удалены из проекта, остался только один слой чистой модульной загрузки).*

---

## 3. Политика разделения языков (RU / EN)

- **Строгое разделение языков:** При выборе русского языка все элементы меню (вкладки, чипсы фильтрации, строка поиска, кнопки, подсказки, карточки) должны отображаться на русском языке. Исключение — общепринятые технические аббревиатуры (HUD, FPS, CPS, RGB) и названия, введённые пользователем.
- **Никаких захардкоженных строк в коде:** Любой пользовательский текст на экранах GUI и в компонентах должен запрашиваться исключительно через `Localizator.translate(...)`.
- **Синхронность RU и EN:** Каждый новый или изменённый ключ обязан присутствовать как в `ru_ru`, так и в `en_us`.

---

## 4. Карта префиксов ключей и модульных файлов

Подробное устройство компонентов ClickGUI описано в `references/clickgui.md`. Для распределения ключей по файлам используется следующее разделение:

| Префикс ключа | Компонент / Назначение | Модульный файл |
| :--- | :--- | :--- |
| `menu.tab.*` | Верхние табы навигации NewScreen (Visuals, Utility, Favorites) | `ui/ui.lang` |
| `menu.search.*` | Поле поиска в шапке окна (`placeholder`) | `ui/ui.lang` |
| `menu.sort` | Заголовок панели сортировки модулей | `ui/ui.lang` |
| `menu.*_filter.*` | Фильтры вкладок: `visuals_filter.*` (HUD, Render), `utility_filter.*` (Movement) | `ui/ui.lang` |
| `menu.bottom_bar.*` | Кнопки нижнего островка (Modules, HUD Editor, GUI Settings, Interface) | `ui/ui.lang` |
| `menu.category.*` | Категории модулей в селекторах (Combat, Movement, Visuals, Player, Other) | `ui/ui.lang` |
| `gui.setting.*` | Названия и описания карточек разделов GUI Settings (appearance, language, sounds, animations) | `ui/ui.lang` |
| `menu.gui_settings.*` | Внутренние контролы GUI Settings: выбор темы/языка, звуки, параметры анимаций | `ui/ui.lang` |
| `menu.module.keybind*` | Подписи назначения клавиш на карточке модуля (Keybind, None, Press...) | `ui/ui.lang` |
| `menu.toolbar.*` | Кнопки тулбара HUD-редактора (`reset`, `done`) | `ui/ui.lang` |
| `menu.editor.*` | Вспомогательные элементы HUD-редактора (`show`, `hide`, `shift_hint` магнита) | `ui/ui.lang` |
| `key.*` | Названия кнопок мыши и спец-клавиш (`mouse.lmb`, `mouse.rmb`, `none`) | `ui/ui.lang` |
| `commands.*` | Тексты и описания чат-команд клиента | `ui/ui.lang` |
| `modules.names.*` | Отображаемые имена всех модулей клиента (`modules.names.<module_key>`) | `modules/modules.lang` |
| `modules.settings.<mod>.*` | Названия настроек, чекбоксов, слайдеров и режимов для модуля `<mod>` | `settings/settings.lang` |
| `hud.background` | Общий ключ прозрачности подложки для HUD-модулей | `settings/settings.lang` |
| `modules.descriptions.*` | Описания модулей для плавающего тултипа (`modules.descriptions.<module_key>`) | `descriptions/descriptions.lang` |

---

## 5. Памятка разработчику и частые ошибки

1. **После изменения .lang-файлов необходима сборка/перезагрузка ресурсов:** В dev-среде Fabric `Localizator` грузит файлы через `getResourceAsStream`. Если просто запустить игру без задачи `processResources` (или полной пересборки), изменения не попадут в `build/resources` и не будут видны в игре.
2. **Только модульные файлы:** Не пытайся искать или создавать монолитные `ru_ru.lang`/`en_us.lang` в корне `lang/`. Ключ добавляется строго в соответствующий модульный подфайл (`ui/ui.lang`, `modules/modules.lang`, `settings/settings.lang` или `descriptions/descriptions.lang`).
3. **Автоматическое имя описания:** Описание модуля не передаётся в конструктор `ModuleInfo` — оно резолвится автоматически по ключу `modules.descriptions.<module_key>`.
4. **Динамическая вёрстка под разную длину слов:** Русские слова часто существенно длиннее английских («Сортировка» vs «Sort», «Внешний вид» vs «Appearance»). Никогда не зашивай фиксированные координаты X или фиксированную ширину плашек без учёта ширины текста через `font.width(text)`.
5. **Не путать единственное и множественное число:** В NewScreen используется префикс `menu.tab.*`, а не `menu.tabs.*` (хотя алиасы могут существовать, всегда используй канонический `menu.tab.*`).