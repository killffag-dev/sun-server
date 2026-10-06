package naryn.sun.systems.notifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Generated;
import naryn.sun.Sun;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.HudRenderEvent;

public class NotificationManager {
   private static final int MAX_NOTIFICATIONS = 8;
   private final List<NotificationOther> notificationsOther = new ArrayList<>();

   private final EventListener<HudRenderEvent> onHudRenderEvent = event -> {
      synchronized (this.notificationsOther) {
         if (this.notificationsOther.isEmpty()) {
            return;
         }

         float off = 0.0F;
         for (int i = 0; i < this.notificationsOther.size(); i++) {
            NotificationOther notification = this.notificationsOther.get(i);
            if (notification.isFinished()) {
               this.notificationsOther.remove(i);
               i--;
               continue;
            }
            notification.update();
            notification.draw(event.getContext(), off);
            if (notification.getAnimation().getValue() >= 0.5F || !notification.getTimer().finished(notification.getDuration())) {
               off += 34.0F;
            }
         }
      }
   };

   public NotificationManager() {
      Sun.getInstance().getEventManager().subscribe(this);
   }

   public void addNotification(NotificationType type, String text) {
      addNotificationOther(type, "SUN", text);
   }

   public void addNotificationOther(NotificationType type, String title, String desc) {
      synchronized (this.notificationsOther) {
         if (this.notificationsOther.size() >= MAX_NOTIFICATIONS) {
            this.notificationsOther.remove(0);
         }
         this.notificationsOther.add(new NotificationOther(type, title, desc));
      }
   }

   public List<Notification> getNotifications() {
      return Collections.emptyList();
   }

   @Generated
   public List<NotificationOther> getNotificationsOther() {
      return this.notificationsOther;
   }
}
