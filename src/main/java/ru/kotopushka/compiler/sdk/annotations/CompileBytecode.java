package ru.kotopushka.compiler.sdk.annotations;
import java.lang.annotation.*;
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface CompileBytecode {}