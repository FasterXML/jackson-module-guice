package com.fasterxml.jackson.module.guice;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.google.inject.BindingAnnotation;
import com.google.inject.Key;

import java.lang.annotation.Annotation;

public class GuiceAnnotationIntrospector extends NopAnnotationIntrospector
{
    private static final long serialVersionUID = 1L;

    @Override
    public Object findInjectableValueId(AnnotatedMember m)
    {
        // Is this needed?
        if (m.getAnnotation(JacksonInject.class) == null) {
            return null;
        }
        Annotation guiceAnnotation = null;
        for (Annotation annotation : m.annotations()) {
            if (annotation.annotationType().isAnnotationPresent(BindingAnnotation.class)) {
                guiceAnnotation = annotation;
                break;
            }
        }
        if (guiceAnnotation == null) {
            return Key.get(m.getGenericType());
        }
        return Key.get(m.getGenericType(), guiceAnnotation);
    }
}
