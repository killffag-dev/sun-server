package ru.kotopushka.compiler.sdk.annotations;
import java.lang.annotation.*;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface VMProtect {
    VMProtectType type() default VMProtectType.NONE;
    VMProtectType value() default VMProtectType.NONE;
}