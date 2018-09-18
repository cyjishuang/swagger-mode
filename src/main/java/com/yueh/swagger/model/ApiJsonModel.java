package com.yueh.swagger.model;

import java.lang.annotation.*;

/**
 * Created by yueh on 2018/9/18.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface  ApiJsonModel {
    String value() default "";
}
