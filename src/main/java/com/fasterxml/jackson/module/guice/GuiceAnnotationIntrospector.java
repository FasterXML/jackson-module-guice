package com.fasterxml.jackson.module.guice;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

import javax.inject.Qualifier;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedConstructor;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.AnnotatedWithParams;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.google.inject.BindingAnnotation;
import com.google.inject.Key;

public class GuiceAnnotationIntrospector extends NopAnnotationIntrospector
{
    private static final long serialVersionUID = 1L;

    @Override
    public boolean hasCreatorAnnotation(Annotated a)
    {

        /* We really only want constructors here */
        if (a instanceof AnnotatedConstructor)
        {
            final AnnotatedConstructor c = (AnnotatedConstructor) a;

            /* @JsonCreator *always* has priority */
            if (c.hasAnnotation(JsonCreator.class)) return true;

            /*
             * In case of @Inject annotations, let's check that there are no
             * other constructors that are annotated with @JsonCreator
             */
            if (c.hasAnnotation(javax.inject.Inject.class) ||
                c.hasAnnotation(com.google.inject.Inject.class))
            {

                /* Examine all other constructors */
                Class<?> t = c.getDeclaringClass();
                for (Constructor<?> x: t.getDeclaredConstructors())
                {
                    if (x.isAnnotationPresent(JsonCreator.class)) return false;
                }

                /*
                 * No other constructor has @JsonCreator, we found our
                 * "creator"... Now we have to annotate every parameter with
                 * @JacksonInject, so that Jackson will behave like Guice!
                 */
                for (int i = 0; i < c.getParameterCount(); i++)
                {
                    AnnotatedParameter p = c.getParameter(i);

                    /* Let's ignore any parameter annotated with @JsonProperty */
                    if (p.hasAnnotation(JsonProperty.class)) continue;

                    /* Add @JacsonInject if it's not already there */
                    p.addIfNotPresent(new JacksonInjectImpl());

                }

                /*
                 * We have an @Inject constructor, and have added to all params
                 * we had to annotate the @JacksonInject annotation...
                 * So, we should be done...
                 */
                return true;
            }
        }

        /* Not a constructor or no valid annotation found */
        return false;
    }


    @Override
    public Object findInjectableValueId(AnnotatedMember m)
    {

        /*
         * We check on three kinds of annotations: @JacksonInject for types
         * that were actually created for Jackson, and @Inject (both Guice's
         * and javax.inject) for types that (for example) extend already
         * annotated objects.
         *
         * Postel's law: http://en.wikipedia.org/wiki/Robustness_principle
         */
        if ((m.getAnnotation(JacksonInject.class) == null) &&
            (m.getAnnotation(javax.inject.Inject.class) == null) &&
            (m.getAnnotation(com.google.inject.Inject.class) == null))
        {
            return null;
        }

        final AnnotatedMember q;

        if ((m instanceof AnnotatedField) || (m instanceof AnnotatedParameter))
        {

            /*
             * On fields the @Qualifier annotation and type to inject are the
             * fields itself, so, nothing to do here...
             */
            q = m;

        }
        else if (m instanceof AnnotatedMethod)
        {

            /*
             * For setters , the @Qualifier and type to inject are specified on
             * the parameter. Here, we only consider 1-parameter setters.
             */

            /* Method or constructor, only 1 parameter */
            final AnnotatedWithParams a = (AnnotatedWithParams) m;
            if (a.getParameterCount() != 1) return null;

            /* Parameter 0 */
            q = a.getParameter(0);


        }
        else
        {
            /* Ignore constructors and parameters */
            return null;
        }


        /*
         * Figure out if a @BindingAnnotation or @Qualifier annotation are
         * present on what we're trying to inject.
         */
        Annotation guiceAnnotation = null;
        for (Annotation annotation : q.annotations())
        {
            if (annotation.annotationType().isAnnotationPresent(BindingAnnotation.class) ||
                annotation.annotationType().isAnnotationPresent(Qualifier.class))
            {
                guiceAnnotation = annotation;
                break;
            }
        }

        /* Return our injection key */
        if (guiceAnnotation == null)
        {
            return Key.get(q.getGenericType());
        }
        return Key.get(q.getGenericType(), guiceAnnotation);

    }
}
