package naryn.sun.access;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.Window;

/**
 * Тонкий слой для клиентского UI и взаимодействия с миром.
 * Player и world намеренно остаются в отдельных адаптерах.
 */
public final class MCClientAccess {

   public static Screen getScreen() {
      return MinecraftClient.getInstance().currentScreen;
   }

   public static void setScreen(Screen screen) {
      MinecraftClient.getInstance().setScreen(screen);
   }

   public static GameOptions getOptions() {
      return MinecraftClient.getInstance().options;
   }

   public static Window getWindow() {
      return MinecraftClient.getInstance().getWindow();
   }

   public static ClientPlayerInteractionManager getInteractionManager() {
      return MinecraftClient.getInstance().interactionManager;
   }

   public static ClientPlayNetworkHandler getNetworkHandler() {
      return MinecraftClient.getInstance().getNetworkHandler();
   }

   public static EntityRenderDispatcher getEntityRenderDispatcher() {
      return MinecraftClient.getInstance().getEntityRenderDispatcher();
   }

   public static BufferBuilderStorage getBufferBuilders() {
      return MinecraftClient.getInstance().getBufferBuilders();
   }

   private MCClientAccess() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
