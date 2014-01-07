package com.fasterxml.jackson.module.guice;

import java.lang.annotation.Annotation;

import javax.inject.Named;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.google.inject.BindingAnnotation;
import com.google.inject.Key;

public class GuiceAnnotationIntrospector extends NopAnnotationIntrospector {
	private static final long serialVersionUID = 1L;

	@Override
	public Object findInjectableValueId(final AnnotatedMember m) {
		// Is this needed?
		if (m.getAnnotation(JacksonInject.class) == null) {
			return null;
		}
		Annotation guiceAnnotation = null;
		for (Annotation annotation : m.annotations()) {

			// Explicit check on javax.inject.Named annotation
			if (annotation.annotationType()
					.equals(Named.class)) {
				guiceAnnotation = annotation;
				break;
			}

			// Binding annotation (Google inject based)
			if (annotation.annotationType()
					.isAnnotationPresent(BindingAnnotation.class)) {
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
