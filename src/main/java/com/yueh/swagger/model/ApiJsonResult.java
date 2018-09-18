package com.yueh.swagger.model;

/**
 * Created by yueh on 2018/9/7.
 */


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static xin.bee.global.GlobalString.RESULT_TYPE_NORMAL;

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiJsonResult {

    String[] value();

    String name() default "";

    String type() default RESULT_TYPE_NORMAL;


}