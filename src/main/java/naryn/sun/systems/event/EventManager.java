package naryn.sun.systems.event;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
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

   private static final int MAX_CONSECUTIVE_ERRORS = 5;

   public static final class ListenerEntry {
      public final EventListener<?> listener;
      public final Object owner;
      public final int priority;

      public ListenerEntry(EventListener<?> listener, Object owner) {
         this.listener = listener;
         this.owner = owner;
         this.priority = listener.getPriority();
      }
   }

   private static final class ListenerField {
      final Field field;
      final Type eventType;

      ListenerField(Field field, Type eventType) {
         this.field = field;
         this.eventType = eventType;
         this.field.setAccessible(true);
      }
   }

   private final ConcurrentHashMap<Type, CopyOnWriteArrayList<ListenerEntry>> listenerMap = new ConcurrentHashMap<>();
   private final Map<Class<?>, List<ListenerField>> listenerFieldsCache = new ConcurrentHashMap<>();
   private final Comparator<ListenerEntry> priorityOrder = Comparator.<ListenerEntry>comparingInt(entry -> entry.priority).reversed();
   private final Consumer<Throwable> errorHandler = Throwable::printStackTrace;

   private final Map<Object, Integer> consecutiveErrors = new ConcurrentHashMap<>();
   private volatile boolean hasErrors = false;

   public void subscribe(Object subscriber) {
      this.modifyEventListenerState(subscriber, (type, listener) -> {
         ListenerEntry entry = new ListenerEntry(listener, subscriber);
         CopyOnWriteArrayList<ListenerEntry> list = this.listenerMap.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>());
         list.add(entry);
         list.sort(this.priorityOrder);
      });
   }

   public void unsubscribe(Object subscriber) {
      this.modifyEventListenerState(subscriber, (type, listener) -> {
         CopyOnWriteArrayList<ListenerEntry> list = this.listenerMap.get(type);
         if (list != null) {
            list.removeIf(entry -> entry.listener == listener);
            if (list.isEmpty()) {
               this.listenerMap.remove(type);
            }
         }
      });
      this.consecutiveErrors.remove(subscriber);
      this.hasErrors = !this.consecutiveErrors.isEmpty();
   }

   @SuppressWarnings("unchecked")
   public <T extends Event> void triggerEvent(T event) {
      Type eventType = event.getClass();
      List<ListenerEntry> listeners = this.listenerMap.get(eventType);
      if (listeners != null && !Sun.INSTANCE.isPanic()) {
         for (ListenerEntry entry : listeners) {
            try {
               ((EventListener<T>) entry.listener).onEvent(event);
               if (this.hasErrors) {
                  this.reportSuccess(entry.owner);
               }
            } catch (Throwable throwable) {
               this.reportFailure(
                  entry.owner,
                  entry.listener.getClass().getSimpleName(),
                  "событие " + event.getClass().getSimpleName(),
                  throwable
               );
            }
         }
      }
   }

   public void reportSuccess(Object owner) {
      if (owner != null && !this.consecutiveErrors.isEmpty()) {
         this.consecutiveErrors.remove(owner);
         this.hasErrors = !this.consecutiveErrors.isEmpty();
      }
   }

   public void reportFailure(Object owner, String sourceLabel, String context, Throwable throwable) {
      this.hasErrors = true;
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
            this.hasErrors = !this.consecutiveErrors.isEmpty();
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
      for (ListenerField lf : this.getListenerFields(o.getClass())) {
         try {
            EventListener<?> eventListener = (EventListener<?>) lf.field.get(o);
            if (eventListener != null) {
               action.accept(lf.eventType, eventListener);
            }
         } catch (IllegalAccessException e) {
            this.errorHandler.accept(e);
         }
      }
   }

   private List<ListenerField> getListenerFields(Class<?> clazz) {
      return this.listenerFieldsCache.computeIfAbsent(clazz, c -> {
         List<ListenerField> list = new ArrayList<>();
         for (Field field : c.getDeclaredFields()) {
            if (field.getType() == EventListener.class) {
               Type genericType = field.getGenericType();
               if (genericType instanceof ParameterizedType pt) {
                  Type[] typeArgs = pt.getActualTypeArguments();
                  if (typeArgs.length > 0) {
                     list.add(new ListenerField(field, typeArgs[0]));
                  }
               }
            }
         }
         return list;
      });
   }
}