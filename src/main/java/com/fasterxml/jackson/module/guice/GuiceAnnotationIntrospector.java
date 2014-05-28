package com.fasterxml.jackson.module.guice;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.databind.introspect.*;
import com.google.inject.BindingAnnotation;
import com.google.inject.Key;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import javax.inject.Qualifier;

public class GuiceAnnotationIntrospector extends NopAnnotationIntrospector
{
    private static final long serialVersionUID = 1L;

    @Override
    public Object findInjectableValueId(AnnotatedMember m)
    {
        // Is this needed?
        if (m.getAnnotation(JacksonInject.class) == null)
        {
            return null;
        }


        final AnnotatedMember guiceMember;
        final Annotation guiceAnnotation;

        if ((m instanceof AnnotatedField) || (m instanceof AnnotatedParameter))
        {
           /*
            * On fields and parameters the @Qualifier annotation and type to
            * inject are the member itself, so, nothing to do here...
            */
            guiceMember = m;
            guiceAnnotation = this.findBindingAnnotation(m.annotations());
        }
        else if (m instanceof AnnotatedMethod)
        {
           /*
            * For method injection, the @Qualifier and type to inject are
            * specified on the parameter. Here, we only consider methods with
            * a single parameter.
            */
           final AnnotatedMethod a = (AnnotatedMethod) m;
           if (a.getParameterCount() != 1) return null;

           /*
            * Jackson does not *YET* give us parameter annotations on methods,
            * only on constructors, henceforth we have to do a bit of work
            * ourselves!
            */
            guiceMember = a.getParameter(0);
            final Annotation[] annotations = a.getMember().getParameterAnnotations()[0];
            guiceAnnotation = findBindingAnnotation(Arrays.asList(annotations));
        }
        else
        {
            /* Ignore constructors */
            return null;
        }

        /*
         * Depending on whether we have an annotation (or not) return the
         * correct Guice key that Jackson will use to query the Injector.
         */
        if (guiceAnnotation == null)
        {
            return Key.get(guiceMember.getGenericType());
        }
        return Key.get(guiceMember.getGenericType(), guiceAnnotation);
    }


    /*
     * We want to figure out if a @BindingAnnotation or @Qualifier
     * annotation are present on what we're trying to inject.
     * Those annotations are only possible on fields or parameters.
     */
    private Annotation findBindingAnnotation(Iterable<Annotation> annotations)
    {
        for (Annotation annotation : annotations)
        {
            // Check on guice (BindingAnnotation) & javax (Qualifier) based injections
            if (annotation.annotationType().isAnnotationPresent(BindingAnnotation.class) ||
                annotation.annotationType().isAnnotationPresent(Qualifier.class))
            {
                return annotation;
            }
        }
        return null;
    }
}
