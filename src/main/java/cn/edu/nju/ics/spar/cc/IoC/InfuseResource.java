package cn.edu.nju.ics.spar.cc.IoC;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark fields in user-defined classes (e.g., Bfunction)
 * that should be injected with engine-provided services.
 * Injection is performed by matching the field type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InfuseResource {
    // No parameters - injection is purely type-based
}

