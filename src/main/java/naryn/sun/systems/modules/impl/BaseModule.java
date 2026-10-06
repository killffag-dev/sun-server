package naryn.sun.systems.modules.impl;

import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import naryn.sun.Sun;
import naryn.sun.systems.modules.Module;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.modules.visuals.MenuModule;
import naryn.sun.systems.setting.Setting;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.sounds.ClientSoundManager;

public abstract class BaseModule implements Module {
   private final ModuleInfo info = this.getClass().getAnnotation(ModuleInfo.class);
   private int key;
   private ModuleCategory category;
   private boolean enabled;
   private boolean hidden;
   private boolean favorite;
   private String name;
   private List<Setting> settings = new ArrayList<>();
   private final Animation keybindsAnimation = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);

   public BaseModule() {
      this.name = this.info.name();
      this.category = this.info.category();
      this.key = this.info.key();
   }

   @Override
   public void toggle() {
      this.setEnabled(!this.enabled, false);
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void tick() {
   }

   @Override
   public void disable() {
      this.setEnabled(false, false);
   }

   @Override
   public void enable() {
      this.setEnabled(true, false);
   }

   @Override
   public void setEnabled(boolean newState, boolean silent) {
      if (this.enabled != newState) {
         this.enabled = newState;
         if (!(this instanceof MenuModule) && !silent) {
            ClientSoundManager.getInstance().playModuleToggle(this.enabled);
         }

         if (this.enabled) {
            Sun.getInstance().getEventManager().subscribe(this);
            this.onEnable();
            if (Sun.getInstance() != null && Sun.getInstance().getModuleManager() != null) {
               Sun.getInstance().getModuleManager().onModuleEnabled(this);
            }
         } else {
            Sun.getInstance().getEventManager().unsubscribe(this);
            this.onDisable();
            if (Sun.getInstance() != null && Sun.getInstance().getModuleManager() != null) {
               Sun.getInstance().getModuleManager().onModuleDisabled(this);
            }
         }
      }
   }

   public String getSettingName(String key) {
      return "modules.settings." + this.getName().toLowerCase().replace(" ", "_") + "." + key;
   }

   @Generated
   @Override
   public ModuleInfo getInfo() {
      return this.info;
   }

   @Override
   public boolean isHoldToActivate() {
      return this.info != null && this.info.holdToActivate();
   }

   @Generated
   @Override
   public int getKey() {
      return this.key;
   }

   @Generated
   @Override
   public ModuleCategory getCategory() {
      return this.category;
   }

   @Generated
   @Override
   public boolean isEnabled() {
      return this.enabled;
   }

   @Generated
   @Override
   public boolean isHidden() {
      return this.hidden;
   }

   @Generated
   @Override
   public boolean isFavorite() {
      return this.favorite;
   }

   @Generated
   @Override
   public void setFavorite(boolean favorite) {
      this.favorite = favorite;
   }

   @Generated
   @Override
   public String getName() {
      return this.name;
   }

   @Generated
   @Override
   public List<Setting> getSettings() {
      return this.settings;
   }

   @Generated
   @Override
   public Animation getKeybindsAnimation() {
      return this.keybindsAnimation;
   }

   @Generated
   @Override
   public void setKey(int key) {
      int oldKey = this.key;
      this.key = key;
      if (Sun.getInstance() != null && Sun.getInstance().getModuleManager() != null) {
         Sun.getInstance().getModuleManager().updateKeyBind(this, oldKey, key);
      }
   }

   @Generated
   public void setCategory(ModuleCategory category) {
      this.category = category;
   }

   @Generated
   public void setEnabled(boolean enabled) {
      this.setEnabled(enabled, false);
   }

   @Generated
   public void setHidden(boolean hidden) {
      this.hidden = hidden;
   }

   @Generated
   public void setName(String name) {
      this.name = name;
   }

   @Generated
   public void setSettings(List<Setting> settings) {
      this.settings = settings;
   }
}