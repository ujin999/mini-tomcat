package com.example.minitomcat.annotation;

import com.example.minitomcat.http.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WebServlet {

    String value();

    HttpMethod method() default HttpMethod.GET;
}
