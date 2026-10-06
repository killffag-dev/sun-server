package naryn.sun.systems.modules.modules.visuals;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import naryn.sun.Sun;
import naryn.sun.framework.base.CustomDrawContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.player.ClientPlayerTickEvent;
import naryn.sun.systems.event.impl.render.HudRenderEvent;
import naryn.sun.systems.event.impl.window.MouseEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.PositionableHudModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.sounds.MusicTracker;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(name = "Music", category = ModuleCategory.VISUALS, desc = "modules.descriptions.music")
public class MusicModule extends PositionableHudModule implements IMinecraft {
   private final BooleanSetting background = new BooleanSetting(this, "hud.background").enable();
   private final BooleanSetting showArtwork = new BooleanSetting(this, "modules.settings.music.artwork").enable();
   private final BooleanSetting showProgress = new BooleanSetting(this, "modules.settings.music.progress").enable();

   private static final float WIDTH = 150.0F;
   private static final float HEIGHT = 40.0F;
   private static final float PAD = 6.0F;
   private static final float ARTWORK_SIZE = 28.0F;

   private static final float PANEL_HEIGHT = 26.0F;
   private static final float BTN_SIZE = 14.0F;
   private static final float BTN_GAP = 10.0F;

   private static final long TOOLTIP_DELAY_MS = 1000L;

   private static final Identifier ICON_PLAY = Sun.id("icons/music/play.png");
   private static final Identifier ICON_PAUSE = Sun.id("icons/music/pause.png");
   private static final Identifier ICON_NEXT = Sun.id("icons/music/next.png");
   private static final Identifier ICON_PREV = Sun.id("icons/music/previous.png");
   private static final Identifier ICON_REPEAT_ALL = Sun.id("icons/music/repeat.png");
   private static final Identifier ICON_REPEAT_ONE = Sun.id("icons/music/repeat1.png");
   private static final Identifier ICON_REPEAT_OFF = Sun.id("icons/music/repeat2.png");

   private final Animation panelAnim = new Animation(180L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private boolean lastPanelVisible = false;

   private float scPrevX, scPrevY, scPlayX, scPlayY, scNextX, scNextY, scRepeatX, scRepeatY, scBtnSize;

   private float scBarX, scBarY, scBarW, scBarH;
   private boolean barActive = false;

   private boolean seekDragging = false;

   // Тултип repeat-кнопки: держим курсор 2 сек -> подпись с текущим режимом
   private long repeatHoverStartMs = -1L;

   private final EventListener<HudRenderEvent> onHudRender = event -> this.draw(event.getContext());

   private final EventListener<MouseEvent> onMouseClick = event -> this.handleMouseClick(event);

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      this.checkBarDrag();
   };

   @Override
   public void renderPreview(CustomDrawContext context) {
      this.draw(context);
   }

   private void checkBarDrag() {
      if (mc.getWindow() == null || !(mc.currentScreen instanceof ChatScreen) || !this.barActive) {
         this.seekDragging = false;
         return;
      }

      long handle = mc.getWindow().getHandle();
      boolean leftDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
      if (!leftDown) {
         this.seekDragging = false;
         return;
      }

      Vector2f mouse = GuiUtility.getMouse();
      double mouseX = mouse.getX();
      double mouseY = mouse.getY();
      boolean overBar = GuiUtility.isHovered((double) this.scBarX, (double) (this.scBarY - 4.0F), (double) this.scBarW, (double) (this.scBarH + 8.0F), mouseX, mouseY);

      if (!this.seekDragging && !overBar) {
         return;
      }
      this.seekDragging = true;

      IMediaSession session = Sun.getInstance().getMusicTracker().getSession();
      if (session == null) {
         return;
      }
      MediaInfo media = session.getMedia();
      if (media == null || media.getDuration() <= 0L || this.scBarW <= 0.0F) {
         return;
      }
      float ratio = Math.min(1.0F, Math.max(0.0F, (float) ((mouseX - this.scBarX) / this.scBarW)));
      safeCall(() -> session.seekTo((long) (ratio * media.getDuration())));
   }

   private void draw(CustomDrawContext context) {
      MusicTracker tracker = Sun.getInstance().getMusicTracker();
      boolean editing = PositionableHudModule.isEditingActive();
      boolean haveTrack = tracker.haveActiveSession() && tracker.getSession() != null;

      if (!haveTrack && !editing) {
         this.panelAnim.update(0.0F);
         this.lastPanelVisible = false;
         this.barActive = false;
         this.repeatHoverStartMs = -1L;
         return;
      }

      float baseWidth = WIDTH;
      float baseHeight = HEIGHT;
      float x = this.resolveX(20.0F);
      float y = this.resolveY(20.0F);

      Vector2f mouse = GuiUtility.getMouse();
      double mouseX = mouse.getX();
      double mouseY = mouse.getY();
      boolean hoveredNow = (mc.currentScreen instanceof ChatScreen)
            && GuiUtility.isHovered(
                  (double) this.getLastBoundsX(), (double) this.getLastBoundsY(),
                  (double) this.getLastBoundsWidth(), (double) this.getLastBoundsHeight(), mouseX, mouseY);
      this.panelAnim.update(haveTrack && hoveredNow ? 1.0F : 0.0F);
      float panelValue = this.panelAnim.getValue();
      this.lastPanelVisible = panelValue > 0.5F;

      float panelHeight = PANEL_HEIGHT * panelValue;
      float totalHeight = baseHeight + panelHeight;

      this.beginScaledRender(context, baseWidth, totalHeight);

      if (editing) {
         context.drawRoundedRect(x, y, baseWidth, totalHeight, BorderRadius.all(8.0F), Colors.getBackgroundColor().withAlpha(200.0F));
      } else if (this.background.isEnabled()) {
         context.drawClientRect(x, y, baseWidth, totalHeight, 1.0F, 0.0F, 1.0F);
      }

      if (haveTrack) {
         MediaInfo media = tracker.getSession().getMedia();
         ColorRGBA textColor = Colors.getTextColor();
         ColorRGBA accent = tracker.getMediaColor();

         float textX = x + PAD;
         if (this.showArtwork.isEnabled()) {
            Identifier art = tracker.getImage() != null ? tracker.getImage() : Sun.id("icons/music/no_image.png");
            context.drawRoundedTexture(art, x + PAD, y + PAD, ARTWORK_SIZE, ARTWORK_SIZE, BorderRadius.all(4.0F));
            textX = x + PAD + ARTWORK_SIZE + 8.0F;
         }

         float textMaxWidth = baseWidth - (textX - x) - PAD;
         Font titleFont = Fonts.MEDIUM.getFont(7.0F);
         Font artistFont = Fonts.REGULAR.getFont(6.0F);

         context.drawText(titleFont, truncate(titleFont, media.getTitle(), textMaxWidth), textX, y + 8.0F, textColor);
         context.drawText(artistFont, truncate(artistFont, media.getArtist(), textMaxWidth), textX, y + 18.0F, textColor.withAlpha(180.0F));

         float barLocalX = textX;
         float barLocalY = y + baseHeight - 8.0F;
         float barLocalW = textMaxWidth;
         float barLocalH = 2.0F;

         boolean barVisible = this.showProgress.isEnabled() && media.getDuration() > 0L;
         if (barVisible) {
            context.drawRoundedRect(barLocalX, barLocalY, barLocalW, barLocalH, BorderRadius.all(1.0F), textColor.withAlpha(60.0F));
            float progress = Math.min(1.0F, (float) media.getPosition() / (float) media.getDuration());
            context.drawRoundedRect(barLocalX, barLocalY, barLocalW * progress, barLocalH, BorderRadius.all(1.0F), accent);

            float cxBar = this.getLastBoundsX() + this.getLastBoundsWidth() / 2.0F;
            float cyBar = this.getLastBoundsY() + this.getLastBoundsHeight() / 2.0F;
            float scale = this.getLayoutScale();
            this.scBarX = cxBar + (barLocalX - cxBar) * scale;
            this.scBarY = cyBar + (barLocalY - cyBar) * scale;
            this.scBarW = barLocalW * scale;
            this.scBarH = barLocalH * scale;
            this.barActive = true;
         } else {
            this.barActive = false;
         }

         this.updateAndDrawIconsPanel(context, x, y, baseWidth, baseHeight, panelValue, tracker, mouseX, mouseY);
      } else {
         context.drawText(Fonts.REGULAR.getFont(7.0F), "Music", x + PAD, y + PAD + 10.0F, Colors.getTextColor());
         this.barActive = false;
      }

      this.endScaledRender(context);
   }

   private void updateAndDrawIconsPanel(CustomDrawContext context, float x, float y, float width, float baseHeight,
                                         float panelValue, MusicTracker tracker,
                                         double mouseX, double mouseY) {
      if (panelValue <= 0.01F) {
         this.repeatHoverStartMs = -1L;
         return;
      }

      IMediaSession session = tracker.getSession();
      MediaInfo media = session != null ? session.getMedia() : null;

      float scale = this.getLayoutScale();
      float cx = this.getLastBoundsX() + this.getLastBoundsWidth() / 2.0F;
      float cy = this.getLastBoundsY() + this.getLastBoundsHeight() / 2.0F;

      float panelLocalY = y + baseHeight;

      float rowWidth = BTN_SIZE * 4.0F + BTN_GAP * 3.0F;
      float rowLocalX = x + (width - rowWidth) / 2.0F;
      float rowLocalY = panelLocalY + (PANEL_HEIGHT - BTN_SIZE) / 2.0F;

      float prevLocalX = rowLocalX;
      float playLocalX = prevLocalX + BTN_SIZE + BTN_GAP;
      float nextLocalX = playLocalX + BTN_SIZE + BTN_GAP;
      float repeatLocalX = nextLocalX + BTN_SIZE + BTN_GAP;

      this.scPrevX = cx + (prevLocalX - cx) * scale;
      this.scPrevY = cy + (rowLocalY - cy) * scale;
      this.scPlayX = cx + (playLocalX - cx) * scale;
      this.scPlayY = this.scPrevY;
      this.scNextX = cx + (nextLocalX - cx) * scale;
      this.scNextY = this.scPrevY;
      this.scRepeatX = cx + (repeatLocalX - cx) * scale;
      this.scRepeatY = this.scPrevY;
      this.scBtnSize = BTN_SIZE * scale;

      boolean overPrev = GuiUtility.isHovered((double) this.scPrevX, (double) this.scPrevY, (double) this.scBtnSize, (double) this.scBtnSize, mouseX, mouseY);
      boolean overPlay = GuiUtility.isHovered((double) this.scPlayX, (double) this.scPlayY, (double) this.scBtnSize, (double) this.scBtnSize, mouseX, mouseY);
      boolean overNext = GuiUtility.isHovered((double) this.scNextX, (double) this.scNextY, (double) this.scBtnSize, (double) this.scBtnSize, mouseX, mouseY);
      boolean overRepeat = GuiUtility.isHovered((double) this.scRepeatX, (double) this.scRepeatY, (double) this.scBtnSize, (double) this.scBtnSize, mouseX, mouseY);

      boolean playing = media != null && media.isPlaying();
      Identifier playIcon = playing ? ICON_PAUSE : ICON_PLAY;

      int repeatState = this.getRepeatState(session); // 0 = off, 1 = all, 2 = one
      Identifier repeatIcon = switch (repeatState) {
         case 2 -> ICON_REPEAT_ONE;
         case 1 -> ICON_REPEAT_ALL;
         default -> ICON_REPEAT_OFF;
      };

      this.drawControlIcon(context, ICON_PREV, prevLocalX, rowLocalY, overPrev, panelValue);
      this.drawControlIcon(context, playIcon, playLocalX, rowLocalY, overPlay, panelValue);
      this.drawControlIcon(context, ICON_NEXT, nextLocalX, rowLocalY, overNext, panelValue);
      this.drawControlIcon(context, repeatIcon, repeatLocalX, rowLocalY, overRepeat, panelValue);

      // ---- Тултип repeat: 2 сек наведения -> подпись текущего режима ----
      if (overRepeat) {
         long now = System.currentTimeMillis();
         if (this.repeatHoverStartMs < 0L) {
            this.repeatHoverStartMs = now;
         } else if (now - this.repeatHoverStartMs >= TOOLTIP_DELAY_MS) {
            String label = switch (repeatState) {
               case 2 -> "Повтор трека";
               case 1 -> "Повтор плейлиста";
               default -> "Повтор выключен";
            };
            this.drawTooltip(context, label, repeatLocalX + BTN_SIZE / 2.0F, rowLocalY, panelValue);
         }
      } else {
         this.repeatHoverStartMs = -1L;
      }
   }

   private void drawTooltip(CustomDrawContext context, String text, float centerX, float aboveY, float alphaMul) {
      Font font = Fonts.REGULAR.getFont(6.0F);
      float textWidth = font.width(text);
      float boxPadX = 5.0F;
      float boxWidth = textWidth + boxPadX * 2.0F;
      float boxHeight = 12.0F;
      float boxY = aboveY - boxHeight - 4.0F;
      float boxX = centerX - boxWidth / 2.0F;

      context.drawRoundedRect(boxX, boxY, boxWidth, boxHeight, BorderRadius.all(4.0F), new ColorRGBA(10, 10, 16, (int) (230 * alphaMul)));
      context.drawCenteredText(font, text, centerX, boxY + 3.0F, Colors.getTextColor().withAlpha(255.0F * alphaMul));
   }

   private void drawControlIcon(CustomDrawContext context, Identifier icon, float localX, float localY, boolean hovered, float alphaMul) {
      if (hovered) {
         context.drawRoundedRect(localX - 2.0F, localY - 2.0F, BTN_SIZE + 4.0F, BTN_SIZE + 4.0F, BorderRadius.all(4.0F), new ColorRGBA(255, 255, 255, (int) (35 * alphaMul)));
      }
      context.drawRoundedTexture(icon, localX, localY, BTN_SIZE, BTN_SIZE, BorderRadius.all(0.0F));
   }

   private void handleMouseClick(MouseEvent event) {
      if (!this.isEnabled() || !(mc.currentScreen instanceof ChatScreen)) {
         return;
      }
      if (event.getButton() != 0 || event.getAction() != 1) {
         return;
      }

      MusicTracker tracker = Sun.getInstance().getMusicTracker();
      IMediaSession session = tracker.getSession();
      if (session == null) {
         return;
      }

      Vector2f mouse = GuiUtility.getMouse();
      double mouseX = mouse.getX();
      double mouseY = mouse.getY();

      if (this.lastPanelVisible) {
         if (GuiUtility.isHovered((double) this.scPrevX, (double) this.scPrevY, (double) this.scBtnSize, (double) this.scBtnSize, mouseX, mouseY)) {
            safeCall(session::previous);
            return;
         }
         if (GuiUtility.isHovered((double) this.scPlayX, (double) this.scPlayY, (double) this.scBtnSize, (double) this.scBtnSize, mouseX, mouseY)) {
            safeCall(session::playPause);
            return;
         }
         if (GuiUtility.isHovered((double) this.scNextX, (double) this.scNextY, (double) this.scBtnSize, (double) this.scBtnSize, mouseX, mouseY)) {
            safeCall(session::next);
            return;
         }
         if (GuiUtility.isHovered((double) this.scRepeatX, (double) this.scRepeatY, (double) this.scBtnSize, (double) this.scBtnSize, mouseX, mouseY)) {
            safeCall(session::swapCycle);
            return;
         }
      }

      if (this.barActive && GuiUtility.isHovered((double) this.scBarX, (double) (this.scBarY - 4.0F), (double) this.scBarW, (double) (this.scBarH + 8.0F), mouseX, mouseY)) {
         MediaInfo media = session.getMedia();
         if (media != null && media.getDuration() > 0L && this.scBarW > 0.0F) {
            float ratio = Math.min(1.0F, Math.max(0.0F, (float) ((mouseX - this.scBarX) / this.scBarW)));
            long targetMs = (long) (ratio * media.getDuration());
            safeCall(() -> session.seekTo(targetMs));
         }
      }
   }

   private static void safeCall(Runnable action) {
      try {
         action.run();
      } catch (UnsatisfiedLinkError | Exception e) {
         Sun.LOGGER.error("Music control action failed: {}", e.getMessage());
      }
   }

   /**
    * @return 0 = повтор выключен, 1 = повтор плейлиста, 2 = повтор одного трека.
    * ПРЕДПОЛОЖЕНИЕ по нативным значениям getCycleType() — не подтверждено кодом
    * DLL. Если иконки показывают не те режимы, скажи какие реальные числа
    * приходят для каждого состояния, поправлю маппинг в одну строку.
    */
   private int getRepeatState(IMediaSession session) {
      if (session == null) {
         return 0;
      }
      try {
         int type = session.getCycleType();
         if (type == 2) {
            return 2;
         }
         if (type == 1) {
            return 1;
         }
         return 0;
      } catch (Exception e) {
         return 0;
      }
   }

   private static String truncate(Font font, String text, float maxWidth) {
      if (text == null || text.isEmpty()) {
         return "";
      }
      if (font.width(text) <= maxWidth) {
         return text;
      }
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < text.length(); i++) {
         String candidate = sb.toString() + text.charAt(i) + "...";
         if (font.width(candidate) > maxWidth) {
            break;
         }
         sb.append(text.charAt(i));
      }
      return sb + "...";
   }
}