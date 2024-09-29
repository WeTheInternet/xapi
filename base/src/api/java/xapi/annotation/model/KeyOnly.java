package xapi.annotation.model;

import xapi.annotation.mirror.MirroredAnnotation;

import java.lang.annotation.*;

/**
 * KeyOnly:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 02/01/2022 @ 1:55 a.m.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE}) // Must be placed on a property getter method or a type
@MirroredAnnotation
public @interface KeyOnly {
    boolean autoSave() default false;
}
