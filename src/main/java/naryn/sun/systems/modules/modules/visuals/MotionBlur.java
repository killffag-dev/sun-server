package naryn.sun.systems.modules.modules.visuals;

import lombok.Generated;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.game.WorldChangeEvent;
import naryn.sun.systems.event.impl.render.Render3DEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.compatibility.IrisCompatibility;
import naryn.sun.utility.render.DrawUtility;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

@ModuleInfo(name = "Motion Blur", category = ModuleCategory.VISUALS, desc = "modules.descriptions.motion_blur")
public class MotionBlur extends BaseModule {
   private final ModeSetting algorithm = new ModeSetting(this, "modules.settings.motion_blur.algorithm");
   private final ModeSetting.Value backwards = new ModeSetting.Value(this.algorithm, "modules.settings.motion_blur.algorithm.backwards");
   private final ModeSetting.Value centered = new ModeSetting.Value(this.algorithm, "modules.settings.motion_blur.algorithm.centered");

   private final naryn.sun.systems.setting.settings.GroupSetting generalGroup = new naryn.sun.systems.setting.settings.GroupSetting(this, "modules.settings.motion_blur.group.general");
   private final BooleanSetting depthBlur = new BooleanSetting(this.generalGroup, "modules.settings.motion_blur.depth_blur").enable();
   private final BooleanSetting refreshRateScaling = new BooleanSetting(this.generalGroup, "modules.settings.motion_blur.refresh_rate_scaling").enable();
   private final BooleanSetting thirdPerson = new BooleanSetting(this.generalGroup, "modules.settings.motion_blur.third_person").enable();
   private final SliderSetting strength = new SliderSetting(this.generalGroup, "modules.settings.motion_blur.strength")
      .min(0.0F)
      .max(3.0F)
      .step(0.05F)
      .currentValue(1.0F);

   {
      this.centered.select();
   }

   private final Matrix4f prevModelView = new Matrix4f();
   private final Matrix4f prevProjection = new Matrix4f();
   private final Vector3f prevCameraPos = new Vector3f();
   private final Vector3f currentCameraPos = new Vector3f();
   private boolean firstFrame = true;

   private long lastNanoTime;
   private float currentFps;
   private int sampleAmount = 100;

   private long lastMonitorHandle;
   private int refreshRate = 60;
   private long lastRefreshCheck;

   private final EventListener<Render3DEvent> onRender3D = event -> {
      Camera camera = event.getCamera();
      Matrix4f modelView = event.getPositionMatrix();
      Matrix4f projection = event.getProjectionMatrix();
      this.currentCameraPos.set(
         (float) (camera.getPos().x % 30000.0), (float) (camera.getPos().y % 30000.0), (float) (camera.getPos().z % 30000.0)
      );

      long now = System.nanoTime();
      float deltaTime = this.lastNanoTime == 0L ? 0.0F : (now - this.lastNanoTime) / 1.0E9F;
      float deltaTick = deltaTime * 20.0F;
      this.lastNanoTime = now;
      this.currentFps = deltaTime > 0.0F && deltaTime < 1.0F ? 1.0F / deltaTime : 0.0F;

      if (this.shouldRender()) {
         if (!this.firstFrame) {
            this.render(modelView, projection, this.currentCameraPos, deltaTick);
         }

         this.firstFrame = false;
      } else {
         this.firstFrame = true;
      }

      this.prevModelView.set(modelView);
      this.prevProjection.set(projection);
      this.prevCameraPos.set(this.currentCameraPos);
   };

   private final EventListener<WorldChangeEvent> onWorldChange = event -> this.firstFrame = true;

   private boolean shouldRender() {
      if (IrisCompatibility.isShadersActive()) {
         return false;
      }
      if (this.strength.getCurrentValue() <= 0.0F) {
         return false;
      } else {
         return mc.options.getPerspective().isFirstPerson() || this.thirdPerson.isEnabled();
      }
   }

   private void render(Matrix4f modelView, Matrix4f projection, Vector3f cameraPos, float deltaTick) {
      float baseStrength = this.strength.getCurrentValue();
      float scaledStrength = baseStrength;

      if (this.refreshRateScaling.isEnabled()) {
         this.updateRefreshRate();
         float fpsOverRefresh = this.refreshRate > 0 ? this.currentFps / this.refreshRate : 1.0F;
         if (fpsOverRefresh < 1.0F) {
            fpsOverRefresh = 1.0F;
         }

         scaledStrength = baseStrength * fpsOverRefresh;
         if (fpsOverRefresh > 1.0F) {
            this.sampleAmount = (int) (100.0F * fpsOverRefresh);
         }
      } else {
         this.sampleAmount = 100;
      }

      DrawUtility.motionBlurProgram
         .apply(
            modelView,
            this.prevModelView,
            projection,
            this.prevProjection,
            cameraPos,
            this.prevCameraPos,
            scaledStrength,
            this.sampleAmount,
            this.backwards.isSelected() ? 0 : 1,
            this.depthBlur.isEnabled()
         );
   }

   private void updateRefreshRate() {
      long now = System.nanoTime();
      if (now - this.lastRefreshCheck < 1_000_000_000L) {
         return;
      }

      this.lastRefreshCheck = now;
      Window window = mc.getWindow();
      if (window == null) {
         return;
      }

      long handle = window.getHandle();
      long monitor = GLFW.glfwGetWindowMonitor(handle);
      if (monitor == 0L) {
         monitor = this.findMonitorFromWindowPosition(handle, window.getWidth(), window.getHeight());
      }

      if (monitor != this.lastMonitorHandle) {
         this.lastMonitorHandle = monitor;
         GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);
         this.refreshRate = vidMode != null ? vidMode.refreshRate() : 60;
      }
   }

   private long findMonitorFromWindowPosition(long window, int windowWidth, int windowHeight) {
      int[] winX = new int[1];
      int[] winY = new int[1];
      GLFW.glfwGetWindowPos(window, winX, winY);
      int centerX = winX[0] + windowWidth / 2;
      int centerY = winY[0] + windowHeight / 2;

      long result = GLFW.glfwGetPrimaryMonitor();
      PointerBuffer monitors = GLFW.glfwGetMonitors();
      if (monitors != null) {
         for (int i = 0; i < monitors.limit(); i++) {
            long monitor = monitors.get(i);
            int[] mx = new int[1];
            int[] my = new int[1];
            GLFW.glfwGetMonitorPos(monitor, mx, my);
            GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
            if (mode != null) {
               int monitorWidth = mode.width();
               int monitorHeight = mode.height();
               if (centerX >= mx[0] && centerX < mx[0] + monitorWidth && centerY >= my[0] && centerY < my[0] + monitorHeight) {
                  result = monitor;
                  break;
               }
            }
         }
      }

      return result;
   }

   @Override
   public void onEnable() {
      this.firstFrame = true;
      this.lastNanoTime = 0L;
      super.onEnable();
   }

   @Generated
   public BooleanSetting getDepthBlur() {
      return this.depthBlur;
   }

   @Generated
   public BooleanSetting getThirdPerson() {
      return this.thirdPerson;
   }

   @Generated
   public BooleanSetting getRefreshRateScaling() {
      return this.refreshRateScaling;
   }

   @Generated
   public SliderSetting getStrength() {
      return this.strength;
   }

   @Generated
   public ModeSetting getAlgorithm() {
      return this.algorithm;
   }

   @Generated
   public ModeSetting.Value getBackwards() {
      return this.backwards;
   }

   @Generated
   public ModeSetting.Value getCentered() {
      return this.centered;
   }
}