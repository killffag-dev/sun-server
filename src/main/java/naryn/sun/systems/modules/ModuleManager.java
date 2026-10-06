package naryn.sun.systems.modules;

import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import naryn.sun.Sun;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.player.ClientPlayerTickEvent;
import naryn.sun.systems.event.impl.render.HudRenderEvent;
import naryn.sun.systems.event.impl.window.KeyPressEvent;
import naryn.sun.systems.event.impl.window.MouseEvent;
import naryn.sun.systems.modules.exception.UnknownModuleException;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.modules.modules.utility.AutoSprint;
import naryn.sun.systems.modules.modules.utility.AntiAFK;
import naryn.sun.systems.modules.modules.utility.BlockSlot;
import naryn.sun.systems.modules.modules.utility.Zoom;
import naryn.sun.systems.modules.modules.utility.Freelook;
import naryn.sun.systems.modules.modules.visuals.Fullbright;
import naryn.sun.systems.modules.modules.visuals.CustomFog;
import naryn.sun.systems.modules.modules.visuals.Friends;
import naryn.sun.systems.modules.modules.visuals.Target;
import naryn.sun.systems.modules.modules.visuals.HeadCosmetics;
import naryn.sun.systems.modules.modules.visuals.HitColor;
import naryn.sun.systems.modules.modules.visuals.KillEffects;
import naryn.sun.systems.modules.modules.visuals.MenuModule;
import naryn.sun.systems.modules.modules.visuals.Prediction;
import naryn.sun.systems.modules.modules.utility.AntiOverlay;
import naryn.sun.systems.modules.modules.utility.NoHurtCam;
import naryn.sun.systems.modules.modules.visuals.SwingAnimation;
import naryn.sun.systems.modules.modules.visuals.TNTTimer;
import naryn.sun.systems.modules.modules.visuals.TargetESP;
import naryn.sun.systems.modules.modules.visuals.ViewModel;
import naryn.sun.systems.modules.modules.visuals.World;
import naryn.sun.systems.modules.modules.visuals.NameUtility;
import naryn.sun.systems.modules.modules.utility.AutoEat;
import naryn.sun.systems.modules.modules.utility.AutoInvisible;
import naryn.sun.systems.modules.modules.utility.AutoLeave;
import naryn.sun.systems.modules.modules.utility.MineHelper;
import naryn.sun.systems.modules.modules.utility.NoInteract;
import naryn.sun.systems.modules.modules.utility.AutoAccept;
import naryn.sun.systems.modules.modules.utility.AutoAuth;
import naryn.sun.systems.modules.modules.utility.DeathCords;
import naryn.sun.systems.modules.modules.utility.LayoutFix;
import naryn.sun.systems.modules.modules.utility.FakePlayer;
import naryn.sun.systems.modules.modules.utility.CommandBind;
import naryn.sun.systems.modules.modules.utility.minigames.MiniGames;
import naryn.sun.systems.modules.modules.utility.aimtrainer.AimTrainer;
import naryn.sun.systems.modules.modules.visuals.Crosshair;
import naryn.sun.systems.modules.modules.optimization.NoRender;
import naryn.sun.systems.modules.modules.optimization.Optimizer;
import naryn.sun.systems.modules.modules.optimization.PacketFilter;
import naryn.sun.systems.modules.modules.optimization.Profiler;
import naryn.sun.systems.modules.modules.optimization.SmartCull;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import ru.kotopushka.compiler.sdk.annotations.CompileBytecode;
import naryn.sun.systems.modules.modules.visuals.Keystrokes;
import naryn.sun.systems.modules.modules.visuals.ArmorStatus;
import naryn.sun.systems.modules.modules.visuals.FpsPing;
import naryn.sun.systems.modules.modules.visuals.PotionStatus;
import naryn.sun.systems.modules.modules.visuals.MaceHit;
import naryn.sun.systems.modules.modules.visuals.ShulkerPeek;
import naryn.sun.systems.modules.modules.visuals.ItemPhysics;
import naryn.sun.systems.modules.modules.visuals.SkyEntityModule;
import naryn.sun.systems.modules.modules.visuals.JumpCircle;
import naryn.sun.systems.modules.modules.visuals.Trails;
import naryn.sun.systems.modules.modules.visuals.MusicModule;
import naryn.sun.systems.modules.modules.visuals.FreshDrop;
import naryn.sun.systems.modules.modules.visuals.InventoryView;
import naryn.sun.systems.modules.modules.visuals.TargetHud;
import naryn.sun.systems.modules.modules.visuals.Sky;
import naryn.sun.systems.modules.modules.visuals.MotionBlur;
import naryn.sun.systems.modules.modules.visuals.BlockOutline;
import naryn.sun.systems.modules.modules.visuals.Hitbox;
import naryn.sun.systems.modules.modules.visuals.HitParticles;
import naryn.sun.systems.modules.modules.visuals.hitpoint.HitPoint;
import naryn.sun.systems.modules.modules.visuals.Waypoints;
import naryn.sun.systems.modules.modules.utility.HitSound;

public class ModuleManager {
   private final List<Module> modules = new ArrayList<>();
   private final List<Module> activeModules = new java.util.concurrent.CopyOnWriteArrayList<>();
   private final it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<List<Module>> keyBindMap = new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();
   private final java.util.Map<Class<? extends Module>, Module> moduleByClass = new java.util.concurrent.ConcurrentHashMap<>();
   private final java.util.Map<String, Module> moduleByName = new java.util.concurrent.ConcurrentHashMap<>();
   private final EventListener<ClientPlayerTickEvent> tickListener;
   private final EventListener<HudRenderEvent> moduleWidgetRenderer;
   private void handleModuleInput(Module module, int action, Screen screen) {
      if (module.isHoldToActivate()) {
         if (action == 1) {
            if (module instanceof ShulkerPeek) {
               if (!(screen instanceof HandledScreen<?>)) {
                  return;
               }
            } else if (screen != null) {
               return;
            }
            if (!module.isEnabled()) {
               module.enable();
            }
         } else if (action == 0 && module.isEnabled()) {
            module.disable();
         }
      } else if (action == 1) {
         if (module instanceof ShulkerPeek) {
            if (!(screen instanceof HandledScreen<?>)) {
               return;
            }
         } else if (screen != null) {
            return;
         }
         module.toggle();
      }
   }

   private final EventListener<KeyPressEvent> onKeyPress = event -> {
      if (event.isCancelled()) return;
      int key = event.getKey();
      if (key == -1) return;
      List<Module> bound = this.keyBindMap.get(key);
      if (bound == null || bound.isEmpty()) return;
      var screen = MinecraftClient.getInstance().currentScreen;
      for (int i = 0; i < bound.size(); i++) {
         this.handleModuleInput(bound.get(i), event.getAction(), screen);
      }
   };

   private final EventListener<MouseEvent> onMouseButtonPress = event -> {
      if (event.isCancelled()) return;
      int btn = event.getButton();
      if (btn == -1) return;
      List<Module> bound = this.keyBindMap.get(btn);
      if (bound == null || bound.isEmpty()) return;
      var screen = MinecraftClient.getInstance().currentScreen;
      for (int i = 0; i < bound.size(); i++) {
         this.handleModuleInput(bound.get(i), event.getAction(), screen);
      }
   };

   public ModuleManager(EventListener<ClientPlayerTickEvent> tickListener, EventListener<HudRenderEvent> moduleWidgetRenderer) {
      this.tickListener = tickListener;
      this.moduleWidgetRenderer = moduleWidgetRenderer;
      Sun.getInstance().getEventManager().subscribe(this);
   }

   @CompileBytecode
   public void registerModules() {
      this.register(new AutoSprint());
      this.register(new BlockSlot());
      this.register(new Zoom());
this.register(new Freelook());
      this.register(new Fullbright());
      this.register(new MiniGames());
      this.register(new AimTrainer());
      this.register(new CustomFog());
      this.register(new Friends());
      this.register(new Target());
      this.register(new KillEffects());
      this.register(new MenuModule());
      this.register(new Prediction());
      this.register(new AntiOverlay());
      this.register(new NoHurtCam());
      this.register(new SwingAnimation());
      this.register(new TNTTimer());
      this.register(new TargetESP());
      this.register(new ViewModel());
      this.register(new World());
      this.register(new NameUtility());
      this.register(new AutoEat());
      this.register(new AutoInvisible());
      this.register(new AutoLeave());
      this.register(new AntiAFK());
      this.register(new MineHelper());
      this.register(new NoInteract());
      this.register(new AutoAccept());
      this.register(new AutoAuth());
      this.register(new DeathCords());
	  this.register(new Keystrokes());
      this.register(new ArmorStatus());
      this.register(new FpsPing());
	  this.register(new ShulkerPeek());
      this.register(new PotionStatus());
	  this.register(new MaceHit());
	  this.register(new ItemPhysics());
	  this.register(new SkyEntityModule());
	  this.register(new JumpCircle());
	  this.register(new Trails());	
	  this.register(new MusicModule());
	  this.register(new FreshDrop());
	  this.register(new InventoryView());
	  this.register(new TargetHud());
      this.register(new MotionBlur());
	  this.register(new Sky());
	  this.register(new LayoutFix());
      this.register(new HeadCosmetics());
	  this.register(new FakePlayer());
      this.register(new CommandBind());
      this.register(new HitColor());
	  this.register(new Hitbox());
      this.register(new HitSound());
      this.register(new NoRender());
      this.register(new PacketFilter());
      this.register(new Optimizer());
      this.register(new Profiler());
      this.register(new SmartCull());
      this.register(new Crosshair());
      this.register(new HitParticles());
      this.register(new HitPoint());
      this.register(new BlockOutline());
      this.register(new Waypoints());
      this.captureDefaultSnapshot();
   }

   private com.google.gson.JsonArray defaultSnapshot;

   public void captureDefaultSnapshot() {
      com.google.gson.JsonArray array = new com.google.gson.JsonArray();
      for (Module module : this.modules) {
         com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
         obj.addProperty("name", module.getName());
         obj.addProperty("enabled", module.getInfo().enabledByDefault());
         obj.addProperty("key", module.getInfo().key());
         obj.addProperty("favorite", false);
         com.google.gson.JsonObject settingsObj = new com.google.gson.JsonObject();
         for (naryn.sun.systems.setting.Setting setting : module.getSettings()) {
            settingsObj.add(setting.getName(), setting.save());
         }
         obj.add("settings", settingsObj);
         array.add(obj);
      }
      this.defaultSnapshot = array;
   }

   public void resetToDefaults() {
      if (this.defaultSnapshot == null) {
         return;
      }
      for (com.google.gson.JsonElement element : this.defaultSnapshot) {
         com.google.gson.JsonObject obj = element.getAsJsonObject();
         String name = obj.get("name").getAsString();
         try {
            Module module = this.getModule(name);
            boolean enabled = obj.has("enabled") && obj.get("enabled").getAsBoolean();
            int key = obj.has("key") ? obj.get("key").getAsInt() : -1;
            boolean favorite = obj.has("favorite") && obj.get("favorite").getAsBoolean();

            if (!(module instanceof MenuModule)) {
               module.setEnabled(enabled, true);
            }
            module.setKey(key);
            module.setFavorite(favorite);

            if (obj.has("settings")) {
               com.google.gson.JsonObject settingsObj = obj.getAsJsonObject("settings");
               for (naryn.sun.systems.setting.Setting setting : module.getSettings()) {
                  if (settingsObj.has(setting.getName())) {
                     try {
                        setting.load(settingsObj.get(setting.getName()));
                     } catch (Exception ignored) {
                     }
                  }
               }
            }
         } catch (UnknownModuleException ignored) {
         }
      }
   }

   @CompileBytecode
   public void enableModules() {
      for (Module module : this.modules) {
         if (module.getInfo().enabledByDefault()) {
            module.enable();
         }
      }
   }

   public void register(BaseModule module) {
      this.modules.add(module);
      this.moduleByClass.put(module.getClass(), module);
      this.moduleByName.put(module.getName().toLowerCase().replace(" ", ""), module);
      if (module.getKey() != -1) {
         this.updateKeyBind(module, -1, module.getKey());
      }
      if (module.isEnabled()) {
         this.onModuleEnabled(module);
      }
   }

   public void updateKeyBind(Module module, int oldKey, int newKey) {
      if (oldKey != -1) {
         List<Module> list = this.keyBindMap.get(oldKey);
         if (list != null) {
            list.remove(module);
            if (list.isEmpty()) {
               this.keyBindMap.remove(oldKey);
            }
         }
      }
      if (newKey != -1) {
         List<Module> list = this.keyBindMap.computeIfAbsent(newKey, k -> new ArrayList<>());
         if (!list.contains(module)) {
            list.add(module);
         }
      }
   }

   public void onModuleEnabled(Module module) {
      if (!this.activeModules.contains(module)) {
         this.activeModules.add(module);
      }
   }

   public void onModuleDisabled(Module module) {
      this.activeModules.remove(module);
   }

   public List<Module> getActiveModules() {
      return this.activeModules;
   }

   @SuppressWarnings("unchecked")
   public <T extends Module> T getModule(String name) {
      String key = name.toLowerCase().replace(" ", "");
      Module cached = this.moduleByName.get(key);
      if (cached != null) {
         return (T) cached;
      }
      for (Module module : this.modules) {
         if (module.getName().replace(" ", "").equalsIgnoreCase(name) || module.getName().equalsIgnoreCase(name)) {
            this.moduleByName.put(key, module);
            return (T) module;
         }
      }
      throw new UnknownModuleException(name);
   }

   @SuppressWarnings("unchecked")
   public <T extends Module> T getModule(Class<T> clazz) {
      Module cached = this.moduleByClass.get(clazz);
      if (cached != null) {
         return (T) cached;
      }
      for (Module module : this.modules) {
         if (clazz.isInstance(module)) {
            this.moduleByClass.put(clazz, module);
            return (T) module;
         }
      }
      throw new UnknownModuleException(clazz.getSimpleName());
   }

   @Generated
   public List<Module> getModules() {
      return this.modules;
   }

   @Generated
   public EventListener<ClientPlayerTickEvent> getTickListener() {
      return this.tickListener;
   }

   @Generated
   public EventListener<HudRenderEvent> getModuleWidgetRenderer() {
      return this.moduleWidgetRenderer;
   }

   @Generated
   public EventListener<KeyPressEvent> getOnKeyPress() {
      return this.onKeyPress;
   }

   @Generated
   public EventListener<MouseEvent> getOnMouseButtonPress() {
      return this.onMouseButtonPress;
   }
}