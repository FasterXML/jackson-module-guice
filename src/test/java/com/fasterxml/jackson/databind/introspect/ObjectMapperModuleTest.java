package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;

/**
 */
public class ObjectMapperModuleTest
{
  @Test
  public void testJacksonInjectThroughGuice() throws Exception
  {
    final Injector injector = Guice.createInjector(
        new ObjectMapperModule(),
        new Module()
        {
          @Override
          public void configure(Binder binder)
          {
            binder.bind(Integer.class).toInstance(1);
            binder.bind(Integer.class).annotatedWith(Names.named("two")).toInstance(2);
            binder.bind(Integer.class).annotatedWith(Ann.class).toInstance(3);
          }
        }
    );

    final ObjectMapper mapper = injector.getInstance(ObjectMapper.class);

    mapper.readValue("{\"four\": 4}", SomeBean.class).verify();
  }

  @Test
  public void testModulesRegisteredThroughNormalInstantiation() throws Exception
  {
    final Injector injector = Guice.createInjector(
        new ObjectMapperModule().registerModule(new IntegerAsBase16Module())
    );

    final ObjectMapper mapper = injector.getInstance(ObjectMapper.class);

    Assert.assertEquals(mapper.writeValueAsString(new Integer(10)), "\"A\"");
  }

  @Test
  public void testModulesRegisteredThroughInjection() throws Exception
  {
    final Injector injector = Guice.createInjector(
        new ObjectMapperModule().registerModule(IntegerAsBase16Module.class)
    );

    final ObjectMapper mapper = injector.getInstance(ObjectMapper.class);

    Assert.assertEquals(mapper.writeValueAsString(new Integer(10)), "\"A\"");
  }

  @Test
  public void testModulesRegisteredThroughInjectionWithAnnotation() throws Exception
  {
    final Injector injector = Guice.createInjector(
        new ObjectMapperModule().registerModule(IntegerAsBase16Module.class, Ann.class),
        new Module()
        {
          @Override
          public void configure(Binder binder)
          {
            binder.bind(IntegerAsBase16Module.class).annotatedWith(Ann.class).to(IntegerAsBase16Module.class);
          }
        }
    );

    final ObjectMapper mapper = injector.getInstance(ObjectMapper.class);

    Assert.assertEquals(mapper.writeValueAsString(new Integer(10)), "\"A\"");
  }

  @Test
  public void testModulesRegisteredThroughInjectionWithNameAnnotation() throws Exception
  {
    final Injector injector = Guice.createInjector(
        new ObjectMapperModule().registerModule(IntegerAsBase16Module.class, Names.named("billy")),
        new Module()
        {
          @Override
          public void configure(Binder binder)
          {
            binder.bind(IntegerAsBase16Module.class)
                  .annotatedWith(Names.named("billy"))
                  .to(IntegerAsBase16Module.class);
          }
        }
    );

    final ObjectMapper mapper = injector.getInstance(ObjectMapper.class);

    Assert.assertEquals(mapper.writeValueAsString(new Integer(10)), "\"A\"");
  }

  @Test
  public void testModulesRegisteredThroughInjectionWithKey() throws Exception
  {
    final Injector injector = Guice.createInjector(
        new ObjectMapperModule().registerModule(Key.get(IntegerAsBase16Module.class))
    );

    final ObjectMapper mapper = injector.getInstance(ObjectMapper.class);

    Assert.assertEquals(mapper.writeValueAsString(new Integer(10)), "\"A\"");
  }

  private static class SomeBean
  {
    @JacksonInject
    private int one;

    @JacksonInject
    @Named("two")
    private int two;

    @JacksonInject
    @Ann
    private int three;

    @JsonProperty
    private int four;

    public boolean verify()
    {
      Assert.assertEquals(1, one);
      Assert.assertEquals(2, two);
      Assert.assertEquals(3, three);
      Assert.assertEquals(4, four);
      return true;
    }
  }

  private static class IntegerAsBase16Module extends SimpleModule
  {
    public IntegerAsBase16Module() {
      super("IntegerAsBase16");

      addSerializer(
          Integer.class,
          new JsonSerializer<Integer>()
          {
            @Override
            public void serialize(
                Integer integer, JsonGenerator jsonGenerator, SerializerProvider serializerProvider
            ) throws IOException, JsonProcessingException
            {
              jsonGenerator.writeString(new BigInteger(String.valueOf(integer)).toString(16).toUpperCase());
            }
          }
      );
    }
  }
}
