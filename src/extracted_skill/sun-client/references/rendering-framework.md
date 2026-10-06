# Rendering Framework — Reference

Папка `framework/` — рендеринговый движок клиента. Используется **всем UI**: ClickGUI (единственный активный экран — `NewScreen`), HUD-модулями (обычные `Module`, рисующие через `CustomDrawContext` в своём `onHudRender`), HUD-редактором и компонентами настроек. Без `framework/` ни один экран ничего не нарисует и не получит события мыши в нужном формате.

---

## Архитектура: цепочка наследования для экранов меню

```
MenuModule (onEnable) — точка входа, создаёт NewScreen и вызывает mc.setScreen()
    ↓
NewScreen  (ui/menu/NewScreen.java) — единственный активный экран меню
    ↓ extends
MenuScreen  (ui/menu/MenuScreen.java)
    — хранит menuAnimation (Animation 500ms) и флаг closing
    ↓ extends
CustomScreen  (framework/base/CustomScreen.java)
    — обёртка над MC Screen
    — перехватывает render/mouseClicked/mouseReleased/mouseDragged
    — оборачивает их в UIContext и вызывает абстрактные onMouse* / render(UIContext)
    ↓ использует
UIContext  (framework/base/UIContext.java)
    — extends CustomDrawContext
    — добавляет mouseX, mouseY, delta
    ↓ extends
CustomDrawContext  (framework/base/CustomDrawContext.java)
    — extends MC DrawContext
    — содержит все draw-методы клиента: drawRoundedRect, drawBlurredRect, drawShadow,
      drawSquircle, drawLiquidGlass, drawText (MSDF), drawSprite, drawItem, drawHead и т.д.
```

---

## framework/base/ — базовые классы

### `CustomScreen.java`
- Абстрактный класс, расширяет `net.minecraft.client.gui.screen.Screen`.
- Перехватывает `render(DrawContext, mouseX, mouseY, delta)` → создаёт `UIContext` → вызывает абстрактный `render(UIContext)`.
- Перехватывает `mouseClicked/mouseReleased/mouseDragged` → оборачивает `int button` в `MouseButton` → вызывает `onMouseClicked/onMouseReleased/onMouseDragged`.
- **Все экраны меню наследуются от него** (через `MenuScreen`).

### `UIContext.java`
- Extends `CustomDrawContext`, добавляет `mouseX`, `mouseY`, `delta`.
- Создаётся единожды за кадр в `CustomScreen.render()` через `UIContext.of(context, mouseX, mouseY, delta)`.
- Передаётся во все `render(UIContext context)` вызовы — содержит всё необходимое для рисования и hit-тестов.

### `CustomDrawContext.java`
- Extends `net.minecraft.client.gui.DrawContext`.
- **Главный рисовальщик UI** — содержит все draw-методы:
  | Метод | Что рисует |
  |---|---|
  | `drawRect` | обычный прямоугольник |
  | `drawRoundedRect` | скруглённый прямоугольник (через `DrawUtility`) |
  | `drawRoundedBorder` | только рамка скруглённого прямоугольника |
  | `drawSquircle` | squircle-форма (суперэллипс) |
  | `drawBlurredRect` | скруглённый прямоугольник с матовым блюром фона (Kawase) |
  | `drawLiquidGlass` | Liquid Glass эффект (стеклянная линза с преломлением) |
  | `drawShadow` | SDF-тень (drop shadow) |
  | `drawText` | текст через MSDF-рендерер |
  | `drawFadeoutText` | текст с горизонтальным fade |
  | `drawCenteredText` | центрированный текст |
  | `drawRightText` | текст выровненный вправо |
  | `drawSprite` | спрайт/иконка |
  | `drawTexture` | произвольная текстура |
  | `drawRoundedTexture` | текстура со скруглёнными углами |
  | `drawItem` | предмет Minecraft |
  | `drawHead` | голова скина игрока |
  | `drawClientRect` | универсальный виджет (блюр + glass/minimalism + фон) |
  | `drawLoadingRect` | прогресс-бар |
  | `drawLine`, `drawBezier` | линии |
- Делегирует всю реализацию в `DrawUtility` (из `utility/render/`), сам не содержит GL-кода.

---

## framework/shader/ — шейдеры

### `GlProgram.java`
- Базовый класс для всех GLSL-шейдерных программ клиента.
- Загружает шейдер по `Identifier`, компилирует, предоставляет `use()`, `findUniform()`, `setup()`.

### `framework/shader/impl/BlurProgram.java`
- **Движок UI-блюра** (матовый блюр фона под окнами меню и HUD-элементами).
- Реализует **Kawase Blur** в два прохода (downscale → upscale), **3 итерации** на каждый проход.
- Читает главный MC-фреймбуфер (`mc.getFramebuffer()`), рендерит в два промежуточных `CustomRenderTarget` (`CACHE` и `BUFFER`).
- Результат доступен через `BlurProgram.getTexture()` — используется когда `drawBlurredRect` рисует скруглённый "кусок" размытого фона.
- Параметры: `blurOffset` (резкость), `blurDownscale` (понижение разрешения для производительности, по умолчанию 0.5).
- Обновляется каждые 25мс (таймер).

### `framework/shader/impl/KawaseBlurProgram.java`
- Низкоуровневая обёртка над GLSL Kawase-шейдером.
- Uniforms: `Resolution`, `Offset`, `Saturation`, `TintIntensity`, `TintColor`.
- `BlurProgram` создаёт **два экземпляра**: `kawaseDownProgram` (шейдер `kawase_down`) и `kawaseUpProgram` (шейдер `kawase_up`).
- TintIntensity сейчас всегда `0.0` — возможность окрашивания блюра не используется.

---

## framework/msdf/ — шрифты

MSDF (Multi-channel Signed Distance Field) — технология рендеринга шрифтов, позволяющая масштабировать текст без пикселизации.

| Файл | Роль |
|---|---|
| `Font.java` | обёртка вокруг `MsdfFont` + размер (size) |
| `Fonts.java` | enum/registry всех шрифтов клиента (`REGULAR`, `MEDIUM`, и т.д.) + фабричный метод `getFont(float size)` |
| `MsdfFont.java` | загрузка `.fnt`-атласа, кэш глифов, измерение ширины/высоты строки |
| `MsdfGlyph.java` | данные одного глифа (UV-координаты, metrics) |
| `MsdfRenderer.java` | GL-рендер: батчинг вершин для текста, вызов шейдера |
| `FontData.java` | структура данных атласа |
| `FormattedTextProcessor.java` | поддержка форматирования (цвет, стиль) внутри строк |
| `ResourceProvider.java` | загрузка ресурсов шрифта из ассетов |

- Все `context.drawText(...)` вызовы в UI проходят через `MsdfRenderer.renderText(...)`.
- Используется в `CustomDrawContext`, всех панелях ClickGUI и во всех HUD-модулях (рисуют текст через тот же `context.drawText(...)`).

---

## framework/objects/ — вспомогательные объекты

| Файл | Роль |
|---|---|
| `BorderRadius.java` | радиусы четырёх углов прямоугольника (topLeft, topRight, bottomLeft, bottomRight). Фабричные методы: `BorderRadius.all(r)`, `BorderRadius.top(r)` и т.д. Передаётся в большинство `draw*` методов. |
| `MouseButton.java` | enum `LEFT`, `RIGHT`, `MIDDLE` + `fromButtonIndex(int)` для конвертации MC int → типизированный enum. |
| `gradient/` | вспомогательные объекты для градиентов (используются в `drawRoundedRect` с Gradient-вариантом) |

---

## Два независимых блюра

В клиенте **два полностью независимых** блюра, не связанных между собой:

### 1. UI-блюр (Kawase) — для меню и HUD
- **Файлы:** `framework/shader/impl/BlurProgram.java` + `framework/shader/impl/KawaseBlurProgram.java`
- **Что делает:** размывает **фон под UI-элементом** (окно меню, HUD-карточки, виджеты)
- **Вызывается через:** `context.drawBlurredRect(x, y, w, h, blurRadius, borderRadius, color)` → `CustomDrawContext` → `DrawUtility.drawBlur()` → `BlurProgram`
- **Пространство работы:** 2D GUI (scaled screen coordinates)
- **Примеры использования:**
  - `NewScreen.java` строки 137-138: матовый блюр фона за окном меню
  - `PositionableHudModule.drawBackground(...)`: блюр/стеклянный фон HUD-модулей (`ArmorStatus`, `PotionStatus`, `FpsPing` и т.д.), когда включена настройка фона — см. `references/hud-editor-checklist.md`

### 2. Motion Blur — для 3D-мира
- **Файлы:** `src/main/java/moscow/sun/framework/shader/impl/MotionBlurProgram.java` + `src/main/java/moscow/sun/systems/modules/modules/visuals/MotionBlur.java`
- **Что делает:** блюр движения в **3D-пространстве** (эффект смазывания при повороте камеры)
- **Вызывается через:** модуль `MotionBlur` на событии `Render3DEvent`
- **Пространство работы:** работает с матрицами камеры (`modelView`, `projection`), использует depth-буфер
- **Алгоритмы:** `Назад` (backwards) и `По центру` (centered)
- **Настройки:** сила (`strength`), blur по глубине (`depthBlur`), от третьего лица (`thirdPerson`), масштабирование под частоту обновления монитора (`refreshRateScaling`)

---

## Система теней (ShadowBatching)

`context.drawShadow(x, y, w, h, softness, borderRadius, color)` — рисует SDF-тень (не блюр).

- **Путь:** `CustomDrawContext.drawShadow()` → `DrawUtility.drawShadow()` → `utility/render/batching/impl/ShadowBatching.java`
- **Реализация:** использует `rectangleProgram` с uniform `Smoothness` — Signed Distance Field, вычисляемый в шейдере
- **Параметры:** `softness` — мягкость тени (чем больше, тем размытее), `borderRadius` — скругление тени, умножается на `3.0F` внутри ShadowBatching
- **Где используется:** `FacetDarkSkin.java` — двойные неоморфные тени (тёмная снизу-справа + светлая сверху-слева) на карточках и треках; `Popup.java`, `ColorPicker.java` — тень под всплывающим окном
- **В `NewScreen` (редизайн) тени нет** — там используется только `drawBlurredRect` + полупрозрачный `drawRoundedRect`

---

## Batching-система (utility/render/batching/impl/)

Для производительности несколько однотипных примитивов группируются (batch) перед отправкой в GPU. Каждый батч — отдельный класс:

| Файл | Что батчит |
|---|---|
| `BlurBatching.java` | заглушка для blur-областей (draw() — no-op, реальный blur через `BlurProgram`) |
| `ShadowBatching.java` | SDF-тени (`drawShadow`) |
| `RoundedRectBatching.java` | скруглённые прямоугольники |
| `SquircleBatching.java` | squircle-формы |
| `FontBatching.java` | MSDF-текст |
| `IconBatching.java` | иконки/спрайты |
| `FadeOutBatching.java` | текст с горизонтальным fade |
| `RectBatching.java` | обычные прямоугольники |

---

## Кто использует framework/

| Потребитель | Что берёт из framework/ |
|---|---|
| `ui/menu/NewScreen.java` | `UIContext`, `Fonts`, `BorderRadius`, `MouseButton` — **весь рендер через framework** |
| `ui/menu/MenuScreen.java` | `CustomScreen` (наследование) |
| HUD-модули (`extends PositionableHudModule`, напр. `TargetHud`, `ArmorStatus`, `PotionStatus`, `FpsPing`) | получают `CustomDrawContext` прямо в `HudRenderEvent`, рисуют напрямую — отдельного `HudElement`-слоя framework не оборачивает |
| `framework/base/CustomDrawContext.java` | `BlurProgram` (через `DrawUtility.drawBlur`) — физически вызывает шейдер |
| Все панели ClickGUI | через `UIContext` получают все draw-методы |
| Компоненты настроек | `BorderRadius`, `MouseButton`, `UIContext` |

---

## Открытие/закрытие ClickGUI — полная цепочка

```
Игрок нажимает keybind (key=344 по умолчанию, настраивается)
    ↓
MenuModule.onEnable()  [systems/modules/modules/visuals/MenuModule.java]
    — если текущий screen ещё не NewScreen: screen = new NewScreen(); Sun.getInstance().setMenuScreen(screen);
    — mc.setScreen(screen)
    — ClientSoundManager.getInstance().playMenuOpen()  → внутри ClientSounds.OPEN.play(...)
    ↓
NewScreen (ui/menu/NewScreen.java) — единственный активный экран ClickGUI
    — extends MenuScreen → extends CustomScreen → extends MC Screen

MenuModule.onDisable()
    — если mc.currentScreen instanceof MenuScreen → mc.setScreen(null)
    — Sun.getInstance().getMenuScreen().setClosing(true)
    — MenuScreen.menuAnimation запускает анимацию закрытия
    — когда animation.getValue() == 0.0F → NewScreen.render() сам вызывает mc.setScreen(null)
```

**Важно (проверено по коду и list.txt):** `ModernScreen` и `dropdown/DropDownScreen` как отдельные экраны в проекте не существуют. Единственный экран ClickGUI — `NewScreen`. Папка `ui/menu/dropdown/` содержит только компоненты для рендера значений настроек модуля внутри развёрнутой `NewModuleCard` (см. `references/clickgui.md`, раздел "Module Settings in the Menu») — это не альтернативный стиль меню. Класс `Rockstar` в коде тоже не существует — точка входа называется `Sun` (`moscow.sun.Sun`), синглтон `Sun.getInstance()`.

---

## Визуальный редизайн NewScreen — матовый блюр и тёмный фон

Реализован в `ui/menu/NewScreen.java` (код живёт в `src/ui/menu/`, draw-методы — в `framework/`):

```java
// 1. Затемнение всего экрана
context.drawRect(0, 0, this.width, this.height, new ColorRGBA(6, 6, 12, 180 * alpha));

// 2. Матовый блюр под окном (Kawase Blur через framework)
context.drawBlurredRect(winX, winY, WIN_W, WIN_H, 30.0F, BorderRadius.all(WIN_R), WHITE.withAlpha(60 * alpha));

// 3. Тёмный прямоугольник поверх блюра
context.drawRoundedRect(winX, winY, WIN_W, WIN_H, BorderRadius.all(WIN_R), new ColorRGBA(13, 13, 23, 220 * alpha));

// 4. Тонкая белая рамка (1px border)
context.drawRoundedBorder(winX, winY, WIN_W, WIN_H, 0.5F, BorderRadius.all(WIN_R), WHITE.withAlpha(10 * alpha));
```

**Неоморфные тени (`FacetDarkSkin`)** используют `drawShadow` дважды подряд — тёмная снизу-справа + светлая сверху-слева, для эффекта "выпуклой" панели:
```java
context.drawShadow(x + off, y + off, w, h, soft, radius, neumorphDark(alpha));
context.drawShadow(x - off, y - off, w, h, soft, radius, neumorphLight(alpha));
```

---

## HUD и framework/

Отдельного класса-обёртки для HUD-виджетов (никакого `Hud`/`HudElement`) в проекте нет. Каждый HUD-модуль — обычный `Module` (обычно `extends PositionableHudModule`), который сам подписывается на рендер и использует `framework/` напрямую:

```java
private final EventListener<HudRenderEvent> onHudRender = event -> this.draw(event.getContext());
```

- `event.getContext()` отдаёт `CustomDrawContext` — от него доступны все draw-методы того же framework, что и у ClickGUI.
- Позиция считается через `resolveX/resolveY` (`PositionableHudModule`), фон — через `drawBackground(...)` (блюр/стекло вне редактора, плоский фон во время редактирования).
- Реальные примеры HUD-модулей: `TargetHud`, `ArmorStatus`, `PotionStatus`, `FpsPing`. Подробный пошаговый чек-лист подключения — `references/hud-editor-checklist.md`.
- Поскольку рендер идёт через обычную подписку `EventListener<HudRenderEvent>`, падения внутри HUD-модуля ловятся тем же `EventManager`, что и у любого другого модуля (см. `references/architecture.md`, "Module Error Isolation").
