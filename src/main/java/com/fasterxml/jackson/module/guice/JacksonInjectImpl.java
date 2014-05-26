package com.fasterxml.jackson.module.guice;

import java.io.Serializable;
import java.lang.annotation.Annotation;

import com.fasterxml.jackson.annotation.JacksonInject;

class JacksonInjectImpl implements JacksonInject, Serializable
{

    private static final long serialVersionUID = 1L;
    private final String value;

    JacksonInjectImpl()
    {
        this("");
    }

    private JacksonInjectImpl(String value)
    {
        if (value == null) throw new NullPointerException("Null @JacksonInject value");
        this.value = value;
    }

    @Override
    public String value()
    {
        return value;
    }

    @Override
    public int hashCode()
    {
        /* The hashCode value is specified in java.lang.annotations.Annotation */
        return (127 * "value".hashCode()) ^ value.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o instanceof JacksonInject)
        {
            JacksonInject other = (JacksonInject) o;
            return value.equals(other.value());
        }
        return false;
    }

    @Override
    public String toString()
    {
        return "@" + JacksonInject.class.getName() + "(value=" + value + ")";
    }

    @Override
    public Class<? extends Annotation> annotationType()
    {
        return JacksonInject.class;
    }

}