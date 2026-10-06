# Kill Effects — как добавить новый режим (эффект)

`KillEffects` устроен как `SkyEntity`: главный модуль (`KillEffects.java`) хранит только настройки и жизненный цикл сущностей, а сама визуальная реализация каждого режима лежит отдельным классом в пакете `killeffects/effects/`. Общие для всех эффектов вещи — в `killeffects/KillEffectModel.java` (интерфейс) и `killeffects/KillEffectGeometry.java` (общая геометрия).

Чтобы добавить новый режим (например «Пепел»), нужно сделать **4 шага**.

---

## 1. Новый класс эффекта

Путь: `src\main\java\moscow\sun\systems\modules\modules\visuals\killeffects\effects\<Название>Effect.java`

Пример: `AshEffect.java`

```java
package moscow.sun.systems.modules.modules.visuals.killeffects.effects;

import moscow.sun.systems.modules.modules.visuals.killeffects.KillEffectModel;
import moscow.sun.utility.colors.ColorRGBA;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;

public class AshEffect implements KillEffectModel {

    @Override
    public void onDeath(Box box, ColorRGBA baseColor, int particleCount) {
        // заспавнить particleCount частиц внутри box
    }

    @Override
    public void tick(long now) {
        // обновление / удаление истёкших частиц
    }

    @Override
    public void render(BufferBuilder builder, MatrixStack matrices, long now) {
        // построение геометрии активных частиц в buffer
    }

    @Override
    public boolean isEmpty() {
        return true; // пока список частиц пуст
    }

    @Override
    public long getLifetimeMs() {
        return 2000L; // своё время жизни, не обязано совпадать с другими эффектами
    }
}
```

Внутри пишете свою приватную модель частицы (по образцу `Leaf` в `LeafEffect.java`), свои константы формы/тайминга. Если нужен общий геометрический хелпер (толстая линия, случайная точка в боксе и т.п.) — используйте `KillEffectGeometry`, а не дублируйте. Если хелпер специфичен только для этого эффекта — держите его приватным внутри своего класса, в `KillEffectGeometry` не тащите.

---

## 2. Подключение в `KillEffects.java`

Путь: `src\main\java\moscow\sun\systems\modules\modules\visuals\KillEffects.java`

Четыре точечные правки:

**Импорт:**
```java
import moscow.sun.systems.modules.modules.visuals.killeffects.effects.AshEffect;
```

**Новое значение режима** (без `.select()` — иначе он станет режимом по умолчанию вместо Leaf):
```java
private final ModeSetting.Value ashMode = new ModeSetting.Value(effectType, "modules.settings.kill_effects.type.ash");
```

**Инстанс эффекта:**
```java
private final AshEffect ashEffect = new AshEffect();
```

**Ветка выбора в `getCurrentEffect()`:**
```java
private KillEffectModel getCurrentEffect() {
   if (this.effectType.is(this.leafMode)) return this.leafEffect;
   if (this.effectType.is(this.ashMode)) return this.ashEffect;
   return this.leafEffect;
}
```

Больше ничего в `KillEffects.java` трогать не нужно — `onEntityDeath`/`onGameTick`/`on3DRender` уже работают через `getCurrentEffect()` и не завязаны на конкретный эффект.

---

## 3. Локализация

Путь: `src/main/resources/assets/sun/lang/ru_ru/settings/settings.lang` и `en_us/settings/settings.lang`

Добавить **рядом с существующими** `modules.settings.kill_effects.*` строками (не в конец файла как попало — держите все ключи одного модуля вместе):

```
modules.settings.kill_effects.type.ash=Пепел
```
```
modules.settings.kill_effects.type.ash=Ash
```

Ключ `modules.settings.kill_effects.type` (заголовок дропдауна) уже существует — второй раз добавлять не нужно, он общий для всех значений режима.

---

## 4. Обязательно: чистая пересборка перед проверкой

`Localizator` грузит `.lang` **как classpath-ресурс** (`getResourceAsStream`), а не с диска исходников напрямую, и делает это один раз при старте клиента. Если после правки `.lang`-файла просто перезапустить уже собранный `runClient`/jar без `processResources` — новый перевод не появится, хотя код будет верным.

Порядок проверки:
1. Убедитесь, что правите файл именно по пути `src\main\resources\assets\sun\lang\...` (не случайную копию в другой папке).
2. `./gradlew clean` → `./gradlew runClient` (полная пересборка, не хотсвап).
3. Если лень пересобирать весь клиент — можно свериться напрямую с `versions/1.21.4/build/resources/main/assets/sun/lang/ru_ru/settings/settings.lang`: если новых строк там нет — значит ресурсы не пересобрались, дело не в коде.

---

## Чек-лист

- [ ] Новый класс `<Название>Effect.java` в `killeffects/effects/`, реализует `KillEffectModel`
- [ ] Импорт нового класса в `KillEffects.java`
- [ ] Новый `ModeSetting.Value` (без `.select()`)
- [ ] Новый инстанс эффекта (поле)
- [ ] Новая ветка в `getCurrentEffect()`
- [ ] Новая строка `modules.settings.kill_effects.type.<id>=...` в `ru_ru/settings/settings.lang` и `en_us/settings/settings.lang`
- [ ] Полная пересборка (`clean` + `runClient`) перед проверкой в игре
