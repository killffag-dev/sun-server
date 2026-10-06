package naryn.sun;

import lombok.Generated;
import naryn.sun.protection.LicenseManager;
import naryn.sun.framework.shader.GlProgram;
import naryn.sun.systems.config.ConfigDropHandler;
import naryn.sun.systems.config.ConfigManager;
import naryn.sun.systems.event.EventIntegration;
import naryn.sun.systems.event.EventManager;
import naryn.sun.systems.event.handlers.ServerConnectionHandler;
import naryn.sun.systems.file.FileManager;
import naryn.sun.systems.friends.FriendManager;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.modules.ModuleManager;
import naryn.sun.systems.modules.constructions.swinganim.SwingManager;
import naryn.sun.systems.modules.constructions.swinganim.presets.SwingPresetManager;
import naryn.sun.systems.modules.listeners.ModuleTickListener;
import naryn.sun.systems.modules.listeners.ModuleWidgetRenderer;
import naryn.sun.systems.notifications.NotificationManager;
import naryn.sun.systems.target.TargetManager;
import naryn.sun.systems.theme.ThemeManager;
import naryn.sun.systems.waypoints.WayPointsManager;
import naryn.sun.ui.menu.MenuScreen;
import naryn.sun.utility.game.TitleBarHelper;
import naryn.sun.utility.game.server.TPSHandler;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.math.calculator.ChatListener;
import naryn.sun.utility.render.DrawUtility;
import naryn.sun.utility.rotations.RotationHandler;
import naryn.sun.utility.rotations.RotationUpdateListener;
import naryn.sun.utility.sounds.MusicTracker;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profilers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.annotations.Initialization;

public enum Sun implements IMinecraft {
   INSTANCE;

   public static final String NAME = "Sun";
   public static final String BUILD_TYPE = "Release";
   public static final String VERSION = "2.0";
   public static final String MOD_ID = "Sun".toLowerCase();
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
   private EventManager eventManager;
   private ThemeManager themeManager;
   private ModuleManager moduleManager;
   private FriendManager friendManager;
   private RotationHandler rotationHandler;
   private TargetManager targetManager;
   private MusicTracker musicTracker;
   private FileManager fileManager;
   private NotificationManager notificationManager;
   private ConfigManager configManager;
   private SwingManager swingManager;
   private TPSHandler tpsHandler;
   private ServerConnectionHandler serverConnectionHandler;
   private WayPointsManager wayPointsManager;
   private SwingPresetManager swingPresetManager;
   private MenuScreen menuScreen;
   private ChatListener chatListener;
   private final MatrixStack reusableRenderMatrices = new MatrixStack();
   private boolean panic;
   private boolean isShutdown;

   @Compile
   @Initialization
   public void initialize() {
      LOGGER.info("Initializing {}...", "Sun");
      LicenseManager.checkLicense();
      this.initializeRenderCompatibility();
      this.musicTracker = new MusicTracker();
      this.wayPointsManager = new WayPointsManager();
      this.wayPointsManager.load();
      this.eventManager = new EventManager();
      this.friendManager = new FriendManager();
      this.themeManager = new ThemeManager();
      this.rotationHandler = new RotationHandler(new RotationUpdateListener());
      this.targetManager = new TargetManager();
      this.fileManager = new FileManager();
      this.moduleManager = new ModuleManager(new ModuleTickListener(), new ModuleWidgetRenderer());
      this.tpsHandler = new TPSHandler();
      this.notificationManager = new NotificationManager();
      this.fileManager.registerClientFiles();
      this.moduleManager.registerModules();
      this.moduleManager.enableModules();
      this.configManager = new ConfigManager();
      this.configManager.handle();
      this.swingManager = new SwingManager();
      this.swingPresetManager = new SwingPresetManager();
      this.swingPresetManager.handle();
      this.fileManager.loadClientFiles();
      ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
         public Identifier getFabricId() {
            return Sun.id("after_shader_load");
         }

         public void reload(ResourceManager manager) {
            GlProgram.loadAndSetupPrograms();
         }
      });
      DrawUtility.initializeShaders();
      Localizator.loadTranslations();
      this.chatListener = new ChatListener();
      this.serverConnectionHandler = new ServerConnectionHandler();
      ConfigDropHandler.init();
      TitleBarHelper.setDarkTitleBar();
      new EventIntegration();
      Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "Sun-Shutdown-Hook"));
      LOGGER.info("{} initialized", "Sun");
   }

   public synchronized void shutdown() {
      if (this.isShutdown) {
         return;
      }
      this.isShutdown = true;
      LOGGER.info("Shutting down...");
      if (!this.isPanic()) {
         // Если активен именованный конфиг — сохраняем его первым
         naryn.sun.systems.config.ConfigFile current = this.configManager.getCurrent();
         if (current != null && !current.getFileName().equalsIgnoreCase("autosave")) {
            current.save();
         }
         // Всегда сохраняем autosave — это гарантирует, что настройки не потеряются
         naryn.sun.systems.config.ConfigFile autosave = this.configManager.getConfig("autosave");
         if (autosave != null) {
            autosave.save();
         }
      }
      this.fileManager.saveClientFiles();
      if (this.wayPointsManager != null) {
         this.wayPointsManager.save();
      }
      if (!this.isPanic()) {
         this.swingPresetManager.getAutoSavePreset().save();
      }
      this.setPanic(false);
   }

      /**
    * Uses Fabric's stable world-render hook instead of injecting into
    * WorldRenderer. Sodium replaces substantial parts of that class, while this
    * event remains the supported integration boundary for both renderers.
    */
   private void initializeRenderCompatibility() {
      boolean sodiumLoaded = FabricLoader.getInstance().isModLoaded("sodium");
      LOGGER.info("Renderer integration: {}", sodiumLoaded ? "Sodium detected" : "vanilla renderer");
      // WorldRendererMixin previously ran at WorldRenderer#render RETURN.
      // END is the matching Fabric API phase; LAST fires earlier in the world
      // pipeline and leaves the old overlays in a different transform state.
      WorldRenderEvents.END.register(context -> {
          if (naryn.sun.utility.compatibility.IrisCompatibility.isShadowPass()) {
              return;
          }
          Profilers.get().swap(MOD_ID + "_renderWorld");
          while (!this.reusableRenderMatrices.isEmpty()) {
              this.reusableRenderMatrices.pop();
          }
          this.reusableRenderMatrices.loadIdentity();
          this.reusableRenderMatrices.multiplyPositionMatrix(context.positionMatrix());
          this.eventManager.triggerEvent(naryn.sun.systems.event.impl.render.Render3DEvent.INSTANCE.set(
             this.reusableRenderMatrices,
             context.positionMatrix(),
             context.projectionMatrix(),
             context.camera(),
             context.tickCounter().getTickDelta(false)
          ));
      });

      // Отдельный, более ранний хук ТОЛЬКО для фоновых full-screen эффектов (Sky).
      // BEFORE_ENTITIES срабатывает после вывода непрозрачного террейна, но до
      // отрисовки любых сущностей — ников, HeadCosmetics и т.д. (см. javadoc
      // Render3DBackgroundEvent). Именно поэтому Sky теперь красит фон ДО них,
      // а не перекрашивает уже готовый кадр ПОСЛЕ (как было на END) — то, что
      // рисуется позже, просто ложится поверх уже готового неба и не может
      // быть им стёрто, независимо от того, пишет оно глубину или нет.
      WorldRenderEvents.BEFORE_ENTITIES.register(context -> {
          if (naryn.sun.utility.compatibility.IrisCompatibility.isShadowPass()) {
              return;
          }
          naryn.sun.utility.math.MathPool.resetFrame();
          Profilers.get().swap(MOD_ID + "_renderSkyBackground");
          while (!this.reusableRenderMatrices.isEmpty()) {
              this.reusableRenderMatrices.pop();
          }
          this.reusableRenderMatrices.loadIdentity();
          this.reusableRenderMatrices.multiplyPositionMatrix(context.positionMatrix());
          this.eventManager.triggerEvent(naryn.sun.systems.event.impl.render.Render3DBackgroundEvent.INSTANCE.set(
             this.reusableRenderMatrices,
             context.positionMatrix(),
             context.projectionMatrix(),
             context.camera(),
             context.tickCounter().getTickDelta(false)
          ));
      });

      // Отключение ванильного черного контура при включенном модуле BlockOutline
      WorldRenderEvents.BLOCK_OUTLINE.register((context, outlineContext) -> {
          if (naryn.sun.utility.compatibility.IrisCompatibility.isShadowPass()) {
              return true;
          }
          if (this.moduleManager != null) {
              naryn.sun.systems.modules.modules.visuals.BlockOutline blockOutline =
                  this.moduleManager.getModule(naryn.sun.systems.modules.modules.visuals.BlockOutline.class);
              if (blockOutline != null && blockOutline.isEnabled()) {
                  return false;
              }
          }
          return true;
      });
   }

   public static Sun getInstance() {
      return INSTANCE;
   }

   public static Identifier id(String path) {
      return Identifier.of(MOD_ID, path);
   }

   @Generated
   public EventManager getEventManager() { return this.eventManager; }
   @Generated
   public ThemeManager getThemeManager() { return this.themeManager; }
   @Generated
   public ModuleManager getModuleManager() { return this.moduleManager; }
   @Generated
   public FriendManager getFriendManager() { return this.friendManager; }
   @Generated
   public RotationHandler getRotationHandler() { return this.rotationHandler; }
   @Generated
   public TargetManager getTargetManager() { return this.targetManager; }
   @Generated
   public MusicTracker getMusicTracker() { return this.musicTracker; }
   @Generated
   public FileManager getFileManager() { return this.fileManager; }
   @Generated
   public NotificationManager getNotificationManager() { return this.notificationManager; }
   @Generated
   public ConfigManager getConfigManager() { return this.configManager; }
   @Generated
   public SwingManager getSwingManager() { return this.swingManager; }
   @Generated
   public TPSHandler getTpsHandler() { return this.tpsHandler; }
   @Generated
   public ServerConnectionHandler getServerConnectionHandler() { return this.serverConnectionHandler; }
   @Generated
   public WayPointsManager getWayPointsManager() { return this.wayPointsManager; }
   @Generated
   public SwingPresetManager getSwingPresetManager() { return this.swingPresetManager; }
   @Generated
   public MenuScreen getMenuScreen() { return this.menuScreen; }
   @Generated
   public ChatListener getChatListener() { return this.chatListener; }
   @Generated
   public void setMenuScreen(MenuScreen menuScreen) { this.menuScreen = menuScreen; }
   @Generated
   public boolean isPanic() { return this.panic; }
   @Generated
   public void setPanic(boolean panic) { this.panic = panic; }
}
