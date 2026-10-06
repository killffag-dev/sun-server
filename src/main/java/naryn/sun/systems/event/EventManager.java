package naryn.sun.systems.event;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import naryn.sun.Sun;
import naryn.sun.systems.modules.Module;
import naryn.sun.systems.notifications.NotificationType;

public class EventManager {

   // Сколько раз подряд владелец (обычно Module) должен упасть, прежде чем модуль
   // будет автоматически отключён. Счётчик сбрасывается при первом успешном вызове.
   private static final int MAX_CONSECUTIVE_ERRORS = 5;

   private final ConcurrentHashMap<Type, CopyOnWriteArrayList<EventListener<?>>> listenerMap = new ConcurrentHashMap<>();
   private final Map<Class<?>, Field[]> declaredFieldsCache = new HashMap<>();
   private final Comparator<EventListener<?>> priorityOrder = Comparator.<EventListener<?>>comparingInt(listener -> listener.getPriority()).reversed();
   private final BiConsumer<List<EventListener<?>>, Comparator<EventListener<?>>> sortCallback = List::sort;
   private final Consumer<Throwable> errorHandler = Throwable::printStackTrace;

   // Кому принадлежит конкретный EventListener (владелец = подписчик, обычно объект модуля).
   private final Map<EventListener<?>, Object> listenerOwners = new ConcurrentHashMap<>();

   // Счётчик подряд идущих ошибок. Ключ — владелец сбоя: как правило объект Module,
   // но может быть и сам EventListener, если владельца определить нельзя.
   // Благодаря ключу по владельцу, а не по конкретному listener'у, этим же счётчиком
   // может пользоваться и прямой цикл вызова модулей (см. ModuleTickListener),
   // минуя подписку через subscribe/triggerEvent.
   private final Map<Object, Integer> consecutiveErrors = new ConcurrentHashMap<>();

   public void subscribe(Object subscriber) {
      this.modifyEventListenerState(subscriber, (type, listener) -> {
         this.listenerMap.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(listener);
         this.listenerOwners.put(listener, subscriber);
         this.sortCallback.accept(this.listenerMap.get(type), this.priorityOrder);
      });
   }

   public void unsubscribe(Object subscriber) {
      this.modifyEventListenerState(subscriber, (type, listener) -> {
         CopyOnWriteArrayList<EventListener<?>> listeners = this.listenerMap.get(type);
         if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
               this.listenerMap.remove(type);
            }
         }
         this.listenerOwners.remove(listener);
      });
      // Сбрасываем счётчик ошибок владельца: например при ручном disable/enable модуля
      // из GUI он должен начинать "с чистого листа", а не унаследовать старые ошибки.
      this.consecutiveErrors.remove(subscriber);
   }

   public <T extends Event> void triggerEvent(T event) {
      Type eventType = event.getClass();
      List<EventListener<?>> listeners = this.listenerMap.get(eventType);
      if (listeners != null && !Sun.INSTANCE.isPanic()) {
         for (EventListener<?> listener : listeners) {
            Object owner = this.listenerOwners.get(listener);
            try {
               ((EventListener<T>) listener).onEvent(event);
               this.reportSuccess(owner != null ? owner : listener);
            } catch (Throwable throwable) {
               this.reportFailure(
                  owner != null ? owner : listener,
                  listener.getClass().getSimpleName(),
                  "событие " + event.getClass().getSimpleName(),
                  throwable
               );
            }
         }
      }
   }

   /**
    * Сбрасывает счётчик подряд идущих ошибок для владельца после успешного вызова.
    * Публичный метод — им пользуется не только triggerEvent, но и прямые циклы
    * вызова модулей (например ModuleTickListener), которые не идут через события.
    */
   public void reportSuccess(Object owner) {
      if (owner != null && !this.consecutiveErrors.isEmpty()) {
         this.consecutiveErrors.remove(owner);
      }
   }

   /**
    * Централизованная обработка падения владельца (модуля/системного listener'а).
    * Логирует всегда (консоль/лог-файл). Если owner — Module, дополнительно шлёт
    * уведомление на экран и после MAX_CONSECUTIVE_ERRORS ошибок подряд
    * автоматически отключает модуль (silent = true: без звука и без штатного
    * уведомления BaseModule "модуль выключен" — вместо него своё, с причиной).
    *
    * @param owner       объект, чей вызов упал (обычно Module)
    * @param sourceLabel класс упавшего кода — для лога (listener или сам модуль)
    * @param context     короткое описание контекста вызова ("tick()", "событие X" и т.п.)
    * @param throwable   исключение
    */
   public void reportFailure(Object owner, String sourceLabel, String context, Throwable throwable) {
      String ownerName = owner != null ? owner.getClass().getSimpleName() : "unknown";
      int errorCount = owner != null ? this.consecutiveErrors.merge(owner, 1, Integer::sum) : 1;

      Sun.LOGGER.error(
         "[EventManager] Падение в {} (owner={}, context={}, попытка {}/{})",
         sourceLabel, ownerName, context, errorCount, MAX_CONSECUTIVE_ERRORS, throwable
      );

      if (owner instanceof Module module) {
         Sun.getInstance().getNotificationManager().addNotificationOther(
            NotificationType.ERROR,
            module.getName().replace(" ", ""),
            "Ошибка %d/%d (%s): %s".formatted(errorCount, MAX_CONSECUTIVE_ERRORS, context, describeThrowable(throwable))
         );

         if (errorCount >= MAX_CONSECUTIVE_ERRORS) {
            this.consecutiveErrors.remove(module);
            module.setEnabled(false, true);
            Sun.getInstance().getNotificationManager().addNotificationOther(
               NotificationType.ERROR,
               module.getName().replace(" ", ""),
               "Модуль автоматически отключён: превышен лимит ошибок подряд (" + MAX_CONSECUTIVE_ERRORS + ")"
            );
         }
      } else {
         this.errorHandler.accept(throwable);
      }
   }

   private static String describeThrowable(Throwable throwable) {
      String message = throwable.getMessage();
      String base = throwable.getClass().getSimpleName();
      if (message == null || message.isBlank()) {
         return base;
      }
      String trimmed = message.length() > 80 ? message.substring(0, 80) + "…" : message;
      return base + ": " + trimmed;
   }

   private void modifyEventListenerState(Object o, BiConsumer<Type, EventListener<?>> action) {
      for (Field field : this.getCachedDeclaredFields(o.getClass())) {
         if (field.getType() == EventListener.class) {
            EventListener<?> eventListener = this.getEventListener(o, field);
            if (eventListener != null) {
               Type eventType = ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
               action.accept(eventType, eventListener);
            }
         }
      }
   }

   private Field[] getCachedDeclaredFields(Class<?> clazz) {
      return this.declaredFieldsCache.computeIfAbsent(clazz, Class::getDeclaredFields);
   }

   private EventListener<?> getEventListener(Object o, Field field) {
      boolean accessible = field.canAccess(o);
      field.setAccessible(true);

      Object var5;
      try {
         return (EventListener<?>)field.get(o);
      } catch (IllegalAccessException var9) {
         this.errorHandler.accept(var9);
         var5 = null;
      } finally {
         field.setAccessible(accessible);
      }

      return (EventListener<?>)var5;
   }
}