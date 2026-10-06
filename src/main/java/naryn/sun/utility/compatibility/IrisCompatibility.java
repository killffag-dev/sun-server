package naryn.sun.utility.compatibility;

import net.fabricmc.loader.api.FabricLoader;

public final class IrisCompatibility {
   private static final boolean IRIS_LOADED = FabricLoader.getInstance().isModLoaded("iris");

   private IrisCompatibility() {
   }

   public static boolean isIrisLoaded() {
      return IRIS_LOADED;
   }

   public static boolean isShadersActive() {
      return IRIS_LOADED && IrisHolder.isShaderPackInUse();
   }

   public static boolean isShadowPass() {
      return IRIS_LOADED && IrisHolder.isRenderingShadowPass();
   }

   private static class IrisHolder {
      private static boolean isShaderPackInUse() {
         try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = irisApiClass.getMethod("getInstance").invoke(null);
            Object result = irisApiClass.getMethod("isShaderPackInUse").invoke(instance);
            return result instanceof Boolean bool && bool;
         } catch (Throwable ignored) {
            return false;
         }
      }

      private static boolean isRenderingShadowPass() {
         try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = irisApiClass.getMethod("getInstance").invoke(null);
            Object result = irisApiClass.getMethod("isRenderingShadowPass").invoke(instance);
            return result instanceof Boolean bool && bool;
         } catch (Throwable ignored) {
            return false;
         }
      }
   }
}
