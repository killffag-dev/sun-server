# SUN Client — Project Context & Architecture Map

> **For AI Agents:** This file is loaded automatically. Read this once to understand the codebase structure and avoid unnecessary filesystem scans (`list_dir`, `find_by_name`, `grep_search`).

---

## 1. Project Overview & Tech Stack
- **Project Name:** SUN (formerly Rockstar refactored)
- **Type:** Minecraft Client / Utility Mod
- **Target Version:** Minecraft `1.21.4` (Stonecutter multi-version system configured under `versions/1.21.4/`)
- **Mod Loader:** Fabric Loader `>=0.16.14`, Fabric API, Loom `1.12.7`
- **Language / Runtime:** Java 21
- **Rendering Stack:** Custom Modern GLSL shaders (`GlProgram`), MSDF text engine, Sodium `0.6.x` compatible
- **Package Root:** `naryn.sun`

---

## 2. CRITICAL: Directory Layout & Legacy Remnants
⚠️ **WARNING:** The root directory contains old leftover folders from a previous decompilation:
- **IGNORE root legacy folders:** `/<root>/framework`, `/<root>/mixin`, `/<root>/protection`, `/<root>/systems`, `/<root>/ui`, and `/<root>/utility`. They are **obsolete** and not compiled.
- **IGNORE scratch/build dirs:** `.gradle/`, `build/`, `run/`, `asset_backup/`, `.tools/`, `temp_skill/`.
- **Vanilla Minecraft Source Reference:** For Minecraft 1.21.4 vanilla code, NEVER scan or search `minecraft_sources/` recursively (22k files) and NEVER use `javap`. Use `minecraft_sources_tree.txt` to lookup the exact file path instantly, then read only that file.
- **ACTIVE CODE LIVES ONLY IN:**
  - `src/main/java/naryn/sun/` (all active Java source code, including `access/` and `protection/client/`)
  - `src/main/resources/` (assets, shaders, mixins, fabric metadata)

---

## 3. Core Architecture Map (`src/main/java/naryn/sun/`)

```
src/main/java/naryn/sun/
├── Sun.java                         # Core Singleton (Sun.INSTANCE), main entry point & service coordinator
│
├── access/                          # Fast static accessors (MCCameraAccess, MCClientAccess, MCPlayerAccess, MCWorldAccess)
│
├── framework/                       # Low-level OpenGL & Rendering Engine
│   ├── base/                        # GL states, texture abstractions
│   ├── msdf/                        # Multi-channel Signed Distance Field font rendering (MSDF)
│   ├── objects/                     # Framebuffers, CustomRenderTarget (HiDPI & downscale support)
│   └── shader/                      # GlProgram base class, MotionBlurProgram, SkyProgram, bloom/blur
│
├── mixin/                           # Fabric Mixin Injections (see sun.mixins.json)
│   ├── accessors/                   # Accessor interfaces (GameRendererAccessor, CameraAccessor, etc.)
│   └── minecraft/                   # Hooks into client, render, entity, world, network, gui
│
├── protection/                      # Active client protection hooks
│   └── client/                      # MinecraftClientMixinProtection, SoundSystemMixinProtection
│
├── systems/                         # Core Client Systems
│   ├── animation/                   # Easing and UI interpolation curves
│   ├── bbmodel/                     # Blockbench 3D model loader & renderer
│   ├── config/                      # ConfigManager, ConfigDropHandler (JSON configuration)
│   ├── event/                       # EventBus / EventManager (@Subscribe event system)
│   ├── file/                        # FileManager (manages .minecraft/sun/ directories)
│   ├── friends/                     # FriendManager (friend list & highlighting)
│   ├── localization/                # Localizator (multi-language string support)
│   ├── notifications/               # NotificationManager (in-game HUD alerts)
│   ├── setting/                     # Settings Architecture (Setting, AbstractSetting, BooleanSetting,
│   │                                # SliderSetting, ModeSetting, SelectSetting, ColorSetting, BindSetting, etc.)
│   ├── target/                      # TargetManager (combat & focus entity tracking)
│   ├── theme/                       # ThemeManager (color palettes and UI themes)
│   ├── waypoints/                   # WayPointsManager (in-world waypoints)
│   └── modules/                     # Modules Engine
│       ├── Module.java              # Base class for all client features
│       ├── ModuleManager.java       # Registry, tick/render dispatching
│       ├── api/ & impl/             # Module categories, metadata, builders
│       ├── constructions/           # Swing animation presets and builders
│       └── modules/                 # Feature implementations:
│           ├── optimization/        # Optimizer, PacketFilter, Profiler, NoRender, OptimizationPresets
│           ├── utility/             # AntiOverlay, AutoAccept, AutoAuth, AutoEat, AutoInvisible, AutoLeave,
│           │                        # AutoSprint, CommandBind, DeathCords, FakePlayer, Freelook, HitSound,
│           │                        # LayoutFix, MineHelper, NameProtect, NoHurtCam, NoInteract, Zoom
│           └── visuals/             # ArmorStatus, CustomFog, FpsPing, FreshDrop, Friends, Fullbright,
│                                    # HeadCosmetics, Hitbox, HitColor, Interface, InventoryView, ItemPhysics,
│                                    # JumpCircle, Keystrokes, KillEffects, MaceHit, MenuModule, MotionBlur,
│                                    # MusicModule, NameUtility, PotionStatus, Prediction, ShulkerPeek, Sky,
│                                    # SkyEntityModule, SwingAnimation, Target, TargetESP, TargetHud, TNTTimer,
│                                    # Trails, ViewModel, World
│
├── ui/                              # User Interface
│   ├── menu/                        # MenuScreen (ClickGUI / client settings window)
│   └── components/                  # Buttons, sliders, checkboxes, color pickers, draggable elements
│
└── utility/                         # Utilities & Helpers
    ├── chunkanimator/               # Chunk animation easing and calculations
    ├── culling/                     # OcclusionCuller (frame-based caching for entities and blocks)
    ├── game/                        # PlayerUtility, ChatUtility, TPSHandler, TitleBarHelper
    ├── interfaces/                  # IMinecraft (shortcut 'mc' for MinecraftClient.getInstance())
    ├── inventory/                   # InventoryUtility, ItemSlot, EnchantmentUtility
    ├── math/                        # FastMath, MathUtility, Vector utils, Chat calculator
    ├── profiler/                    # PerformanceProfiler, PerformanceReportGenerator
    ├── render/                      # DrawUtility (2D primitives, batching, textures, color blending)
    ├── rotations/                   # RotationHandler, RotationUpdateListener (aim & camera math)
    ├── time/                        # StopWatch, Timer utilities
    └── sounds/                      # Sound playback helpers
```

---

## 4. Key Resources (`src/main/resources/`)
- `fabric.mod.json`: Mod ID `sun`, dependencies, library jars (`discord-rpc.jar`).
- `sun.mixins.json`: Mixin declarations for client and common targets.
- `sun.accesswidener`: Access widener rules for vanilla Minecraft fields/methods.
- `assets/sun/shaders/`: Custom GLSL vertex & fragment shaders (`motion_blur/`, `sky/`, `bloom/`, etc.).
- `assets/sun/fonts/msdf/`: Pre-generated MSDF font atlases (`.png` + `.json`). Raw `.otf` excluded from jar.

---

## 5. Build, Run & Verification Commands
- **Compile Java:** `.\gradlew.bat :1.21.4:compileJava`
- **Build Jar:** `.\gradlew.bat :1.21.4:build` (Outputs: `build/libs/sun-1.21.4-1.0.0.jar`)
- **Launch Client:** `.\gradlew.bat :1.21.4:runClient`
- **Target switching (Stonecutter):** Root `settings.gradle` and `versions/1.21.4/gradle.properties`.

---

## 6. Coding Invariants & Conventions
1. **Never touch root legacy dirs:** Modify only files within `src/main/java/` and `src/main/resources/`.
2. **Sodium Compatibility:** Do not break the Sodium rendering pipeline. Render hooks must use standard Fabric API events (`WorldRenderEvents`) or surgical Mixins.
3. **No Garbage in Render Loops:** Avoid object allocations inside per-frame render routines (`DrawUtility`, `GlProgram.draw()`, `MotionBlurProgram`, `OcclusionCuller`). Reuse static/ThreadLocal buffers.
4. **Modules:** New features must extend `Module` (via `BaseModule`), annotate with `@ModuleInfo`, and register in `ModuleManager`.
5. **Localization:** String labels in modules should register through `Localizator` or language keys.
6. **Strict File Size Cap (< 350-400 Lines) & Decomposition:** NEVER create or bloat classes beyond 350-400 lines. Always extract separate responsibilities into focused classes (e.g. `*InputHandler`, `*ScrollState`, `*Renderer`, `*Helper`).
7. **Strict Scope (No Unsolicited Additions):** Implement strictly what was requested. Never add unasked-for visual effects, background tints, extra colors, or unnecessary settings.
8. **Strict Dual Localization (RU/EN):** Keep Russian and English completely separated without English words leaking when RU is active. Keys follow `modules.settings.<module_key>.<setting_id>`.
9. **Performance & Render Safety:** For visual features (particles, custom 3D geometry, shaders), assess impact on FPS and Sodium pipeline before coding. Always use `Render3DBackgroundEvent` (before entities) for full-screen/sky backgrounds to prevent entity nametag clipping.
10. **Module Settings Architecture & Box-in-Box Protocol (Strict Hierarchy):**
    When declaring settings inside any `Module` (`BaseModule`), NEVER dump loose settings in flat unstructured lists. Always adhere to this strict 4-step hierarchy:
    - **1. Modes First:** If the module has modes, style presets, or core algorithms (`ModeSetting`), they MUST always be declared at the very top (Top-Level Root of the card).
    - **2. Mode-Specific Settings Box:** Parameters configuring the active mode/algorithm must follow directly below in a dedicated container (`GroupSetting`).
    - **3. General & Logic Box:** All remaining general settings, behavior toggles, targets, filters, delays, and logic follow next in their own container (`GroupSetting`).
    - **4. Colors at the Very Bottom:** Color pickers (`ColorSetting`), palettes, and gradients MUST always be placed at the very bottom in the lowest box (unless explicitly tied to a higher-priority mode sub-box).
    - **Internal Order Inside Any Container:** 1) `BooleanSetting` (toggles), 2) `ModeSetting`/`SelectSetting` (selectors), 3) `SliderSetting`/`RangeSetting` (numbers), 4) `ColorSetting` (colors), 5) `BindSetting`/`StringSetting`.

---

## 7. Tool & Token Efficiency Protocol (MANDATORY FOR ALL AGENTS)
1. **Zero Blind Exploration:** NEVER run `list_dir`, `find_by_name`, or `grep_search` to discover paths or packages already documented in Sections 2-4. Jump directly to target files.
2. **Targeted File Reads:** NEVER read entire large files (>80 lines) without specifying `StartLine` and `EndLine`. Always read only the specific method, interface, or block needed.
3. **Quiet Terminal Execution:** Always run Gradle commands in quiet mode:
   - Compile: `.\gradlew.bat :1.21.4:compileJava -q`
   - Build: `.\gradlew.bat :1.21.4:build -q`
   Never dump verbose build or execution logs into the conversation context.
4. **Concise Action-Driven Responses:** Do not narrate obvious code lines or summarize unchanged code. State the specific change concisely and apply it via tools.
5. **Ultra-Brief Plans:** When proposing or documenting an implementation plan, give only the bare essence: short bullet points of exact changes and files. No fluff, no obvious descriptions, no long preambles.
6. **ASCII / Text Visualizations:** When visualizing UI layouts, flows, or architectures, use compact ASCII/symbol diagrams (e.g., box frames `[ Button ]`, ASCII art, or arrows `A -> B -> C`). Do not generate bulky markdown tables or external visual boilerplate.
7. **Minimalist Completion Summaries:** When reporting finished work, keep the summary strictly to 2-4 brief bullet points: what was done, files changed, and verification result. Never write multi-paragraph recaps, never recite code line-by-line, and avoid any conversational filler.
8. **No Bytecode Decompilation or Blind Minecraft Scans:** NEVER run `javap`. NEVER recursively scan `minecraft_sources/` or `.gradle/`. Instead, lookup the class in `minecraft_sources_tree.txt` using `Select-String -Path minecraft_sources_tree.txt -Pattern "ClassName:"`, which takes <0.1s, then open only the found path.

---

## 8. Change Logging & Progress Safety Protocol (`changelog/`)
1. **No Remote Git Push:** NEVER run `git push`. GitHub remote is unlinked to prevent hangs, authentication prompts, and token burn.
2. **Mandatory Changelog Entry:** After completing any feature, bugfix, or setting change (and verifying via quiet Gradle compile), ALWAYS append a concise entry to `changelog/CHANGELOG.md` with:
   - Timestamp (`[YYYY-MM-DD HH:MM]`) and task/module name
   - What was done / fixed
   - List of modified files
3. **Local Git Safety (Optional / Local Only):** If performing risky refactoring, you may create a local Git commit (`git commit -m "..."`) for rollback capability. Never attempt remote synchronization.
4. **Database Git Exclusion:** Live server databases (`server/database.json`, `server/database.sqlite*`, backups) are ignored in `.gitignore` and must never be committed to avoid overwriting production users and balances.

---

## 9. Launch Roadmap & Client Infrastructure (Tasks 15.8 – 15.12)

| Task | Title | Description & Implementation Details | Status |
| :--- | :--- | :--- | :--- |
| **15.8** | **Бэкенд & Экономика (API)** | Сервер на Node.js 22 (`server/server.js`): гибридная СУБД SQLite (Node 22 native `node:sqlite` + `better-sqlite3` + AES-256 шифрование записей + аварийный JSON снапшот), веб-админка `/admin` с графическим интерфейсом. Бесшовная авторизация **One-Click Connect** (`/api/auth/pair/start`, `/api/auth/pair/confirm`, `/api/auth/pair/unlink`, `/api/sparks-balance`). База данных защищена постоянным диском **Persistent Volume** (`/data` / `sun-server-volume`) от сброса при деплоях. Выдача косметики и баланс **Спарксов (Sparks)**. Бесплатная основа: без HWID, без банов, без принудительных подписок. | ✅ **Готово** |
| **15.9** | **Сайт & Личный Кабинет** | Веб-интерфейс для пользователей: профиль (`site/profile.html`), загрузка аватарок/баннеров, страница сопряжения в 1 клик (`site/link.html`). Устаревший ручной ввод 6-значного кода полностью удалён, статус `clientLinked` синхронизирован, добавлена кнопка «Отвязать», управление косметикой и балансом Sparks. | ✅ **Готово** |
| **15.10** | **Платёжный агрегатор** | Подключение приёма платежей (СБП/карты/P2P для пополнения Спарксов: Aaio/Lava/Payok). | ⏳ В планах |
| **15.11** | **Встраивание в клиент SUN** | Интеграция с сервером (`ServerConfig`, `LinkCodeScreen`, `SparksManager`): One-Click вход через браузер по кнопке «Войти», отображение баланса Спарксов в главном меню, подгрузка косметики через API. *(Модуль `LicenseManager.java` удален)*. | ✅ **Готово** |
| **15.12** | **Аудит 51 модуля & баг-лист** | Составление реестра известных багов по всем модулям, приоритизация и устранение Critical/Crash ошибок. | ⏳ В планах |




## 10. АНТИ-УДАЛЕНИЕ И ЗАЩИТА ФАЙЛОВ (CRITICAL SAFETY PROTOCOL)
1. **Абсолютный запрет на удаление:** Ни при каких обстоятельствах агентам не разрешается удалять файлы агента NVM, файлы проекта клиента (исходные коды, ресурсы), файлы сборки, а также любые файлы сайта и сервера.
2. **Защита инфраструктуры:** Серверная часть, веб-сайт и ядро клиента защищены от деструктивных действий. Любые задачи, предполагающие риск для этих файлов, должны быть отклонены.
3. **Ограничение прав ИИ:** Агентам запрещено применять команды rm, Remove-Item или инструменты удаления к корню проекта, папкам сервера и сайта.

---

## 11. Релиз новых версий мода & Протокол обновлений (Release Protocol)
⚠️ **ВАЖНО:** Простое переименование `.jar` файла на диске **НЕ МЕНЯЕТ** версию мода! Версия зашита в байт-код и манифест.

### Порядок выпуска новой версии:
1. **Обновить версию в коде клиента:**
   - `src/main/java/naryn/sun/Sun.java`: обновить поле `public static final String VERSION = "X.Y";` (например, `"2.1"`).
   - `src/main/resources/fabric.mod.json`: обновить поле `"version": "X.Y.Z"`.
2. **Собрать релизный `.jar`:**
   - Запустить тихую сборку: `.\gradlew.bat :1.21.4:build -q`
   - Скомпилированный файл мода находится в: `build/libs/` (или `versions/1.21.4/build/libs/`).
3. **Разместить файл на загрузку:**
   - Загрузить собранный `.jar` на сайт/хостинг по адресу, на который ведет кнопка скачивания (`/#download`).
4. **Активировать уведомление в админке (`/admin`):**
   - Нажать **«О новой версии»** (или создать объявление с включенной галочкой «Обязательное обновление клиента»).
   - Указать номер версии: строго соответствующий или выше (например, `2.1`).
   - Написать чейнджлог / текст обновления и отправить.
5. **Как реагирует клиент:**
   - В `AnnouncementManager.java` и `UpdateManager.java` работает семантическая проверка `isRemoteVersionNewer(serverVersion, Sun.VERSION)`.
   - Игрокам на старых версиях показывается всплывающее окно в главном меню с кнопкой «Обновить».
   - Как только игрок скачает и запустит новый `.jar`, где вшита актуальная версия `Sun.VERSION`, уведомление автоматически исчезнет.
   - В админке доступна кнопка **«Снять объявление»**, которая мгновенно отзывает уведомление у всех пользователей.

