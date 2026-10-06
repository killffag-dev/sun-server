# UI Animation System — Reference

## Overview

Система анимаций интерфейса SUN обеспечивает централизованное управление всеми микро-анимациями и визуальной физикой ClickGUI. Она позволяет пользователю полностью отключать анимации (переход в мгновенный/instant snap режим), регулировать скорость всех анимаций (0.5x – 2.0x) и настраивать отдельные эффекты интерфейса.

Конфигурация анимаций сохраняется в `client.json` (через `ClientDataFile`), поэтому настройки пользователя персистентны между перезапусками клиента.

---

## Архитектура и пакеты

```
moscow.sun.
├── systems.animation.
│   └── ClientAnimationConfig.java       # Синглтон настроек анимаций, JSON I/O
├── utility.animation.base.
│   ├── Animation.java                   # Базовый класс анимаций (интеграция скорости и мастер-свитча)
│   └── Easing.java                      # Математические функции сглаживания (BACK_OUT, QUARTIC_OUT и др.)
└── ui.menu.
    ├── components.
    │   ├── GuiSettingCard.java          # Карточка настройки "Анимации" в GUI Settings (раскрывается по ПКМ)
    │   ├── GuiSettingType.java          # Enum: APPEARANCE, LANGUAGE, SOUNDS, ANIMATIONS
    │   └── NewModuleCard.java           # Карточка модуля: hover-поднятие, звёздочка, тогл с физикой
    ├── panels.
    │   ├── GuiSettingsPanel.java        # Регистрация карточки ANIMATIONS в списке cards
    │   ├── WindowPanel.java             # Скользящий индикатор (pill) между вкладками категорий
    │   ├── BottomBarPanel.java          # Скользящий бокс между отделами нижнего островка
    │   └── TooltipBoxPanel.java         # Плавное появление/исчезание тултипа на месте (без рулетки)
    ├── skin.
    │   └── MenuSkin.java                # Тень при hover-подъёме, плавный микс цветов трека тогла
    └── dropdown.components.settings.impl.
        └── BooleanSettingComponent.java # Физика растяжения бегунка для чекбоксов настроек модулей
```

---

## `ClientAnimationConfig.java` — Синглтон конфигурации

**Путь:** `src/main/java/moscow/sun/systems/animation/ClientAnimationConfig.java`

Единая точка управления параметрами анимации в рантайме.

### Поля и флаги:

| Поле | Тип | Default | Описание |
|---|---|---|---|
| `animationsEnabled` | `boolean` | `true` | **Master Toggle** — глобальное включение/выключение анимаций. При `false` любые анимации мгновенно переходят в конечное состояние. |
| `buttonHoverLift` | `boolean` | `true` | Поднятие карточек модулей и настроек на `-2px` при наведении курсора + появление мягкой тени глубины. |
| `smoothTabs` | `boolean` | `true` | Плавное скольжение активного индикатора между категориями в шапке окна и между отделами нижнего островка. |
| `smoothTooltip` | `boolean` | `true` | Плавное появление/затухание описания модуля на месте (`offsetY = 0`) вместо вертикального смещения ("рулетки"). |
| `starEffect` | `boolean` | `true` | Эффектный отскок звёздочки (elastic bounce), вращение на 144°, вылет 6 золотистых частиц (sparkle burst) и двойной bloom-glow. |
| `togglePhysics` | `boolean` | `true` | Физическое растяжение бегунка (stretch & squash) переключателей и чекбоксов во время движения. |
| `animationSpeed` | `float` | `1.0F` | Множитель скорости анимаций от `0.5F` (замедленно) до `2.0F` (ускорено). |

### API:
```java
ClientAnimationConfig cfg = ClientAnimationConfig.getInstance();

boolean enabled = cfg.isAnimationsEnabled();
cfg.setAnimationsEnabled(false);

float speed = cfg.getAnimationSpeed();
cfg.setAnimationSpeed(1.5F);

// Сериализация для ClientDataFile
JsonObject json = cfg.toJson();
cfg.fromJson(jsonObject);
```

---

## `Animation.java` — Интеграция в движок анимаций

**Путь:** `src/main/java/moscow/sun/utility/animation/base/Animation.java`

Базовый класс `Animation` обновлён для прозрачной поддержки глобального выключения и регулировки скорости:

1. **Мгновенный переход (Instant Snap):**
   Если `ClientAnimationConfig.getInstance().isAnimationsEnabled() == false`, в методе `update(float)` текущее значение не интерполируется во времени, а сразу приравнивается к целевому:
   ```java
   if (!ClientAnimationConfig.getInstance().isAnimationsEnabled()) {
       this.value = to;
       return;
   }
   ```
2. **Масштабирование длительности:**
   Длительность шага анимации автоматически масштабируется коэффициентом скорости:
   ```java
   float speed = ClientAnimationConfig.getInstance().getAnimationSpeed();
   long effDuration = Math.max(1L, Math.round(this.duration / (speed > 0.0F ? speed : 1.0F)));
   ```
   Благодаря этому все использующие `Animation` компоненты автоматически ускоряются или замедляются без изменения их собственного кода.

---

## Персистентность: `ClientDataFile.java`

**Путь:** `src/main/java/moscow/sun/systems/file/impl/ClientDataFile.java`

В секции `write()` и `read()` добавлена сериализация объекта `"animations"`:
- При сохранении: `json.add("animations", ClientAnimationConfig.getInstance().toJson());`
- При чтении: `ClientAnimationConfig.getInstance().fromJson(object.getAsJsonObject("animations"));`

---

## Визуальные эффекты ClickGUI

### 1. Поднятие карточек при наведении (Hover Elevation)
- **Где:** `NewModuleCard.java` и `GuiSettingCard.java`.
- **Механика:** При `isHovered(...)` вычисляется `liftY = isButtonHoverLift() ? -2.0F * hoverAnimation.getValue() : 0.0F`.
- Отрисовка оборачивается в матричную трансформацию:
  ```java
  if (lifted) {
      context.getMatrices().push();
      context.getMatrices().translate(0.0F, liftY, 0.0F);
  }
  try {
      // Отрисовка карточки
  } finally {
      if (lifted) context.getMatrices().pop();
  }
  ```
- В `MenuSkin.renderCard()` при `hoverAnim > 0.01F && isButtonHoverLift()` под карточкой рисуется мягкая рассеянная тень:
  ```java
  context.drawShadow(x, y + 2.0F, w, h, 8.0F * hoverAnim, radius, new ColorRGBA(0, 0, 0, (int)(45 * hoverAnim * alpha)));
  ```

### 2. Скользящие индикаторы (Sliding Pills)
- **Категории (`WindowPanel.java`):**
  Вместо независимой отрисовки капсулы под каждым табом вычисляется координата `activeTabX` и ширина `activeTabW` текущей выбранной вкладки. Отрисовывается единый скользящий индикатор через `tabIndicatorX` и `tabIndicatorW` с `Easing.QUARTIC_OUT`.
- **Нижний островок (`BottomBarPanel.java`):**
  Активный отдел (Modules = 0, GUI Settings = 2) отслеживается статическими анимациями `barIndicatorX` и `barIndicatorAlpha`. При переключении отделов плашка плавно скользит между кнопками.

### 3. Плавный тултип на месте (`TooltipBoxPanel.java`)
- Убран вертикальный сдвиг строк (`-shift` и `innerH - shift`).
- Описание модуля отрисовывается строго по центру внутренней области (`offsetY = 0.0F`).
- При смене текста (`switchProgress < 1.0F`) предыдущий текст плавно затухает с альфой `ta * (1.0F - switchProgress)`, а новый текст одновременно проявляется с альфой `ta * switchProgress`.
- При выключенном `isSmoothTooltip()` текст переключается сразу без задержки.

### 4. Заметная анимация звёздочки (`NewModuleCard.java`)
- **Bounce:** `starPressAnim` использует `Easing.BACK_OUT` с длительностью 350 мс. При клике сбрасывается в `0.35F`, совершает пружинный отскок с вылетом за пределы 1.0 (до ~1.15) и стабилизируется на 1.0.
- **Вращение:** `starSpinAnim` (400 мс, `QUARTIC_OUT`) вращает звёздочку вокруг центра на 144° (`RotationAxis.POSITIVE_Z.rotationDegrees((1.0F - progress) * -144.0F)`).
- **Sparkle Burst:** `starBurstAnim` (450 мс) порождает 6 золотистых частиц `ColorRGBA(253, 224, 71, ...)`, разлетающихся радиально от центра звезды на расстояние `5.0F + 11.0F * burst` и растворяющихся.
- **Bloom Glow:** Двойное свечение (внешнее 16px с альфой 45 и внутреннее 9px с альфой 85) в тёплых янтарных тонах.

### 5. Физика переключателей (Toggle Physics)
- **Где:** `MenuSkin.renderToggleTrack`, `renderToggleThumb`, `NewModuleCard`, `GuiSettingCard`, `BooleanSettingComponent`.
- **Плавный цвет трека:** вместо скачкообразного переключения цвета трек интерполирует цвет через `offBg.mix(onBg, enableAnim)`.
- **Stretch & Squash:** бегунок тогла во время перемещения динамически растягивается по горизонтали:
  ```java
  float stretch = isTogglePhysics() ? (float) Math.sin(enableVal * Math.PI) * 2.5F : 0.0F;
  float knobW = TOGGLE_KNOB + stretch;
  float knobX = toggleX + 2.0F + (TOGGLE_W - TOGGLE_KNOB - 4.0F) * enableVal - (stretch * (enableVal > 0.5F ? 0.7F : 0.3F));
  ```

---

## Карточка в GUI Settings: `GuiSettingCard.java`

- **Тип:** `GuiSettingType.ANIMATIONS` (название `gui.setting.animations.name`, описание `gui.setting.animations.desc`).
- В свёрнутом виде: надпись «Анимации» по центру карточки.
- В развёрнутом виде (ПКМ):
  1. Мастер-переключатель «Включить анимации» (`menu.gui_settings.animations.master`).
  2. 5 под-переключателей конкретных эффектов (поднятие кнопок, плавные вкладки, плавный тултип, эффект звёздочки, физика переключателей).
  3. Слайдер «Скорость анимаций» от 0.5x до 2.0x (`animSpeedDragging`, обработка в `onMouseDragged`, сохранение в `onMouseReleased`).

---

## Локализация (Ключи)

Файлы: `src/main/resources/assets/sun/lang/ru_ru/ui/ui.lang` и `en_us/ui/ui.lang`.

| Ключ | Значение (RU) | Значение (EN) |
|---|---|---|
| `gui.setting.animations.name` | Анимации | Animations |
| `gui.setting.animations.desc` | Плавность интерфейса и эффекты | Interface smoothness and effects |
| `menu.gui_settings.animations.title` | Анимации | Animations |
| `menu.gui_settings.animations.master` | Включить анимации | Enable animations |
| `menu.gui_settings.animations.button_hover` | Поднятие при наведении | Hover button elevation |
| `menu.gui_settings.animations.smooth_tabs` | Плавные вкладки | Smooth tab switching |
| `menu.gui_settings.animations.smooth_tooltip` | Плавный тултип | Smooth tooltip |
| `menu.gui_settings.animations.star_effect` | Эффект звёздочки | Star pop effect |
| `menu.gui_settings.animations.toggle_physics` | Физика переключателей | Toggle switch physics |
| `menu.gui_settings.animations.speed` | Скорость анимаций | Animation speed |
