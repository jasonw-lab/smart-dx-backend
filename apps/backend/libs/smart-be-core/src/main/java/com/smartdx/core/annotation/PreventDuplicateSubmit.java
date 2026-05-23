package com.smartdx.core.annotation;

import java.lang.annotation.*;

/**
 * Prevent duplicate submit annotation
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PreventDuplicateSubmit {

    /**
     * Lock timeout in seconds
     */
    int expire() default 5;
}
