package naryn.sun.systems.notifications;

import lombok.Generated;
import naryn.sun.Sun;
import naryn.sun.framework.base.CustomDrawContext;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.render.RenderUtility;
import naryn.sun.utility.time.Timer;

public class NotificationOther {
   private final NotificationType type;
   private final String title;
   private final String desc;
   private final Timer timer = new Timer();
   private final long duration;
   private final Animation animation = new Animation(280L, Easing.CUBIC_OUT);
   private final Animation showing = new Animation(260L, Easing.CUBIC_OUT);
   private final Animation animY = new Animation(240L, Easing.CUBIC_OUT);

   public NotificationOther(NotificationType type, String title, String desc) {
      this.type = type;
      this.title = title;
      this.desc = desc;
      this.duration = 2400L;
   }

   private static final float PAD = 6.0F;
   private static final float ICON_SIZE = 16.0F;
   private static final float GAP = 6.0F;
   private static final float HEIGHT = 28.0F;

   public void draw(CustomDrawContext context, float off) {
      float titleWidth = Fonts.BOLD.getFont(7.0F).width(this.title);
      float descWidth = Fonts.MEDIUM.getFont(6.0F).width(this.desc);
      float width = PAD + ICON_SIZE + GAP + Math.max(titleWidth, descWidth) + PAD + 4.0F;
      float height = HEIGHT;

      float animVal = this.animation.getValue();
      if (animVal <= 0.001F) return;

      this.animY.setEasing(Easing.CUBIC_OUT);
      this.animY.setDuration(240L);

      float x = context.getScaledWindowWidth() / 2.0F - width / 2.0F;
      // Плавное появление со сдвигом вверх (slide-in) и микро-масштабированием
      float slideY = (1.0F - animVal) * 12.0F;
      float y = context.getScaledWindowHeight() - 90.0F - this.animY.update(off) + slideY;

      float scale = 0.94F + 0.06F * animVal;
      RenderUtility.scale(context.getMatrices(), x + width / 2.0F, y + height / 2.0F, scale);

      // Форма, подложка, радиус скругления и блюр в точности как у HUD-модулей клиента
      context.drawClientRect(x, y, width, height, animVal, 0.0F, 1.0F);

      // Бэйдж иконки с цветом типа уведомления
      BorderRadius iconRadius = BorderRadius.all(4.0F);
      context.drawRoundedRect(x + PAD, y + PAD, ICON_SIZE, ICON_SIZE, iconRadius,
            this.type.getColor().withAlpha((int) (24 * animVal)));

      // Иконка типа
      context.drawTexture(
         Sun.id("icons/" + this.type.getName() + ".png"),
         x + PAD + 3.0F,
         y + PAD + 3.0F,
         10.0F,
         10.0F,
         this.type.getColor().withAlpha((int) (245 * animVal))
      );

      // Тексты
      float textX = x + PAD + ICON_SIZE + GAP;
      int textAlpha = (int) (255.0F * animVal);
      context.drawText(Fonts.BOLD.getFont(7.0F), this.title, textX, y + 6.0F, Colors.getTextColor().withAlpha(textAlpha));
      context.drawText(Fonts.MEDIUM.getFont(6.0F), this.desc, textX, y + 16.0F, Colors.getTextColor().withAlpha((int) (textAlpha * 0.75F)));

      RenderUtility.end(context.getMatrices());
   }

   public void update() {
      boolean finished = this.timer.finished(this.duration);
      this.animation.setDuration(finished ? 240L : 280L);
      this.animation.setEasing(finished ? Easing.CUBIC_IN : Easing.CUBIC_OUT);
      this.animation.update(finished ? 0.0F : 1.0F);
   }

   public boolean isFinished() {
      return this.animation.getValue() == 0.0F && this.timer.finished(this.duration);
   }

   @Generated
   public NotificationType getType() {
      return this.type;
   }

   @Generated
   public String getTitle() {
      return this.title;
   }

   @Generated
   public String getDesc() {
      return this.desc;
   }

   @Generated
   public Timer getTimer() {
      return this.timer;
   }

   @Generated
   public long getDuration() {
      return this.duration;
   }

   @Generated
   public Animation getAnimation() {
      return this.animation;
   }

   @Generated
   public Animation getShowing() {
      return this.showing;
   }

   @Generated
   public Animation getAnimY() {
      return this.animY;
   }
}
