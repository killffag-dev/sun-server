# SUN Client тАФ Project Context & Architecture Map

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
тЪая╕П **WARNING:** The root directory contains old leftover folders from a previous decompilation:
- **IGNORE root legacy folders:** `/<root>/framework`, `/<root>/mixin`, `/<root>/protection`, `/<root>/systems`, `/<root>/ui`, and `/<root>/utility`. They are **obsolete** and not compiled.
- **IGNORE scratch/build dirs:** `.gradle/`, `build/`, `run/`, `asset_backup/`, `.tools/`, `minecraft_sources/`, `temp_skill/`.
- **ACTIVE CODE LIVES ONLY IN:**
  - `src/main/java/naryn/sun/` (all active Java source code, including `access/` and `protection/client/`)
  - `src/main/resources/` (assets, shaders, mixins, fabric metadata)

---

## 3. Core Architecture Map (`src/main/java/naryn/sun/`)

```
src/main/java/naryn/sun/
тФЬтФАтФА Sun.java                         # Core Singleton (Sun.INSTANCE), main entry point & service coordinator
тФВ
тФЬтФАтФА access/                          # Fast static accessors (MCCameraAccess, MCClientAccess, MCPlayerAccess, MCWorldAccess)
тФВ
тФЬтФАтФА framework/                       # Low-level OpenGL & Rendering Engine
тФВ   тФЬтФАтФА base/                        # GL states, texture abstractions
тФВ   тФЬтФАтФА msdf/                        # Multi-channel Signed Distance Field font rendering (MSDF)
тФВ   тФЬтФАтФА objects/                     # Framebuffers, CustomRenderTarget (HiDPI & downscale support)
тФВ   тФФтФАтФА shader/                      # GlProgram base class, MotionBlurProgram, SkyProgram, bloom/blur
тФВ
тФЬтФАтФА mixin/                           # Fabric Mixin Injections (see sun.mixins.json)
тФВ   тФЬтФАтФА accessors/                   # Accessor interfaces (GameRendererAccessor, CameraAccessor, etc.)
тФВ   тФФтФАтФА minecraft/                   # Hooks into client, render, entity, world, network, gui
тФВ
тФЬтФАтФА protection/                      # Active client protection hooks
тФВ   тФФтФАтФА client/                      # MinecraftClientMixinProtection, SoundSystemMixinProtection
тФВ
тФЬтФАтФА systems/                         # Core Client Systems
тФВ   тФЬтФАтФА animation/                   # Easing and UI interpolation curves
тФВ   тФЬтФАтФА bbmodel/                     # Blockbench 3D model loader & renderer
тФВ   тФЬтФАтФА config/                      # ConfigManager, ConfigDropHandler (JSON configuration)
тФВ   тФЬтФАтФА event/                       # EventBus / EventManager (@Subscribe event system)
тФВ   тФЬтФАтФА file/                        # FileManager (manages .minecraft/sun/ directories)
тФВ   тФЬтФАтФА friends/                     # FriendManager (friend list & highlighting)
тФВ   тФЬтФАтФА localization/                # Localizator (multi-language string support)
тФВ   тФЬтФАтФА notifications/               # NotificationManager (in-game HUD alerts)
тФВ   тФЬтФАтФА setting/                     # Settings Architecture (Setting, AbstractSetting, BooleanSetting,
тФВ   тФВ                                # SliderSetting, ModeSetting, SelectSetting, ColorSetting, BindSetting, etc.)
тФВ   тФЬтФАтФА target/                      # TargetManager (combat & focus entity tracking)
тФВ   тФЬтФАтФА theme/                       # ThemeManager (color palettes and UI themes)
тФВ   тФЬтФАтФА waypoints/                   # WayPointsManager (in-world waypoints)
тФВ   тФФтФАтФА modules/                     # Modules Engine
тФВ       тФЬтФАтФА Module.java              # Base class for all client features
тФВ       тФЬтФАтФА ModuleManager.java       # Registry, tick/render dispatching
тФВ       тФЬтФАтФА api/ & impl/             # Module categories, metadata, builders
тФВ       тФЬтФАтФА constructions/           # Swing animation presets and builders
тФВ       тФФтФАтФА modules/                 # Feature implementations:
тФВ           тФЬтФАтФА optimization/        # Optimizer, PacketFilter, Profiler, NoRender, OptimizationPresets
тФВ           тФЬтФАтФА utility/             # AntiOverlay, AutoAccept, AutoAuth, AutoEat, AutoInvisible, AutoLeave,
тФВ           тФВ                        # AutoSprint, CommandBind, DeathCords, FakePlayer, Freelook, HitSound,
тФВ           тФВ                        # LayoutFix, MineHelper, NameProtect, NoHurtCam, NoInteract, Zoom
тФВ           тФФтФАтФА visuals/             # ArmorStatus, CustomFog, FpsPing, FreshDrop, Friends, Fullbright,
тФВ                                    # HeadCosmetics, Hitbox, HitColor, Interface, InventoryView, ItemPhysics,
тФВ                                    # JumpCircle, Keystrokes, KillEffects, MaceHit, MenuModule, MotionBlur,
тФВ                                    # MusicModule, NameUtility, PotionStatus, Prediction, ShulkerPeek, Sky,
тФВ                                    # SkyEntityModule, SwingAnimation, Target, TargetESP, TargetHud, TNTTimer,
тФВ                                    # Trails, ViewModel, World
тФВ
тФЬтФАтФА ui/                              # User Interface
тФВ   тФЬтФАтФА menu/                        # MenuScreen (ClickGUI / client settings window)
тФВ   тФФтФАтФА components/                  # Buttons, sliders, checkboxes, color pickers, draggable elements
тФВ
тФФтФАтФА utility/                         # Utilities & Helpers
    тФЬтФАтФА chunkanimator/               # Chunk animation easing and calculations
    тФЬтФАтФА culling/                     # OcclusionCuller (frame-based caching for entities and blocks)
    тФЬтФАтФА game/                        # PlayerUtility, ChatUtility, TPSHandler, TitleBarHelper
    тФЬтФАтФА interfaces/                  # IMinecraft (shortcut 'mc' for MinecraftClient.getInstance())
    тФЬтФАтФА inventory/                   # InventoryUtility, ItemSlot, EnchantmentUtility
    тФЬтФАтФА math/                        # FastMath, MathUtility, Vector utils, Chat calculator
    тФЬтФАтФА profiler/                    # PerformanceProfiler, PerformanceReportGenerator
    тФЬтФАтФА render/                      # DrawUtility (2D primitives, batching, textures, color blending)
    тФЬтФАтФА rotations/                   # RotationHandler, RotationUpdateListener (aim & camera math)
    тФЬтФАтФА time/                        # StopWatch, Timer utilities
    тФФтФАтФА sounds/                      # Sound playback helpers
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

---

## 8. Git Version Control & Progress Safety Protocol
1. **Safety Commits Before Risky Edits:** Before performing refactoring, removing code, or reworking working modules, ensure the current working state is committed.
2. **Atomic Commits After Completion:** After successfully completing any feature, bugfix, or setting change (and verifying via quiet Gradle compile), create a concise, descriptive Git commit.
3. **History Inspection & Rollback:** When restoring or reviewing previous implementations, use Git history (`git log`, `git show`, `git diff`) to inspect exact earlier code states.
4. **Remote Synchronization:** Push commits to `origin main` to keep the GitHub repository continuously updated.


