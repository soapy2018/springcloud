package com.cqf.annotation;

import java.lang.annotation.*;

/**
 * Created by cqf on 2019/10/9
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SysLogger {
    String value() default "";
}
