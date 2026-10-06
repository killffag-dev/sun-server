# Чек-лист: как подключить любой HUD-модуль к редактору

В проекте есть система HUD-редактора для модулей, рисующих элемент на экране (позиция + масштаб задаются в отдельном UI-редакторе, вкладка "Modules" в HUD Editor). Уже поддерживают это `PotionStatus`, `ArmorStatus`, `FpsPing` — они наследуются от `PositionableHudModule` (`moscow.sun.systems.modules.impl.PositionableHudModule`).

## Шаблон

```java
private final EventListener<HudRenderEvent> onHudRender = event -> this.draw(event.getContext());

@Override
public void renderPreview(CustomDrawContext context) {
    this.draw(context);
}

private void draw(CustomDrawContext context) {
    // вся отрисовка модуля тут:
    // 1. посчитать width/height контента
    // 2. float x = this.resolveX(defaultX); float y = this.resolveY(defaultY);
    // 3. this.beginScaledRender(context, width, height);
    // 4. фон — либо вручную if(PositionableHudModule.isEditingActive()) {...} else if (background.isEnabled()) {...},
    //    либо через this.drawBackground(context, backgroundSetting, x, y, width, height);
    // 5. остальная отрисовка (текст, иконки и т.д.) как было
    // 6. this.endScaledRender(context);
}
```

## Пошагово

1. **Наследование** — `extends PositionableHudModule` вместо `BaseModule` (импорт `moscow.sun.systems.modules.impl.PositionableHudModule` и `moscow.sun.framework.base.CustomDrawContext`).
2. **Разделение рендера** — вынести всё тело `onHudRender` в приватный `draw(CustomDrawContext context)`; сам `onHudRender` — однострочник: `event -> this.draw(event.getContext())`.
3. **renderPreview** — добавить `@Override public void renderPreview(CustomDrawContext context) { this.draw(context); }`.
4. **Позиция** — посчитать `width`/`height` контента ДО вызова resolve, затем `x = this.resolveX(defaultX)`, `y = this.resolveY(defaultY)` (`defaultX`/`defaultY` — то же выражение, что раньше стояло напрямую в `x`/`y`).
5. **Масштабирование и фон** — сразу после resolve вызвать `this.beginScaledRender(context, width, height)`; фон рисовать через `this.drawBackground(context, backgroundSetting, x, y, width, height)` (если логика фона нестандартная — оставить ручной `if(isEditingActive()) {...} else if (background.isEnabled()) {...}`); в конце — `this.endScaledRender(context)` строго после последней команды отрисовки.

## Смысл механизмов

- `resolveX/resolveY` — возвращают сохранённую в редакторе позицию либо дефолтную.
- `beginScaledRender`/`endScaledRender` — оборачивают отрисовку в масштабирование вокруг центра и репортят реальные границы модуля редактору (для хит-теста/рамки).
- `renderPreview(context)` — вызывается редактором HUD-модулей (`HudModuleEditor`) каждый кадр во время редактирования, чтобы нарисовать чёткую копию модуля поверх (размытого ванильным блюром экрана) игрового мира — редактор рисуется в контексте самого Screen, который блюром не затрагивается.
- `PositionableHudModule.isEditingActive()` — статический флаг, `true`, пока открыта вкладка Modules.

## Если у модуля есть собственная анимация/логика позиционирования

Если модуль (например, был замечен на `Keystrokes`) двигается сам по себе или не рисует preview-копию под блюром — это обычно значит, что его внутренняя логика позиции конфликтует с `resolveX/resolveY`. При переводе такого модуля на `PositionableHudModule`:
- разобраться, как совместить собственную анимацию с позицией из редактора, не сломав функциональность модуля (например, анимация может смещать координаты относительно `resolveX/resolveY`-базы, а не заменять её);
- не терять существующее поведение — цель перевода на `PositionableHudModule` только в том, чтобы модуль корректно виделся/двигался в HUD-редакторе, а не в переписывании его логики.

## Общее правило запроса на перевод модуля

Всегда просить у пользователя путь/содержимое файла модуля перед переписыванием (не угадывать текущую логику), отдавать готовый файл целиком с полным путём перед кодом (см. общие правила в SKILL.md).
