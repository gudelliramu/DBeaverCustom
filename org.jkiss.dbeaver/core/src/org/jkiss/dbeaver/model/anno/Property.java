package org.jkiss.dbeaver.model.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Property
 */
@Target(value = {ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Property
{

    String name();

    String category() default "";

    String description() default "";

    boolean editable() default false;

    boolean viewable() default false;

    int order() default Integer.MAX_VALUE;
}
