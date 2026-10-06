package naryn.sun.systems.modules.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleInfo {
   String name();

   ModuleCategory category();

   int key() default -1;

   boolean disableOnQuit() default false;

   boolean enabledByDefault() default false;

   boolean holdToActivate() default false;

   String desc() default "У этой функции нет описания";
}
