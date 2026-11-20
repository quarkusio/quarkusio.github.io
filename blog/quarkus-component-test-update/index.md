# Quarkus - a component testing update

It's been a while since we [introduced the component testing](https://quarkus.io/blog/quarkus-component-test/) in Quarkus. In this blogpost, we will first quickly summarize the basic principles and then describe some of the new interesting features.

## Quick summary

First, just a quick summary. The component model of Quarkus is built on top of CDI. An idiomatic way to test a Quarkus application is to use the `quarkus-junit5` module and `@QuarkusTest`. However, in this case, a full Quarkus application needs to be built and started. In order to avoid unnecessary rebuilds and restarts the application is shared for multiple tests, unless a [different test profile](https://quarkus.io/guides/getting-started-testing#testing_different_profiles) is used. One of the consequences is that some components (typically `@ApplicationScoped` and `@Singleton` CDI beans) are shared as well. What if you need to test the business logic of a component in isolation, with different states and inputs? For this use case, a plain unit test would make a lot of sense. However, writing unit tests for CDI beans without a running CDI container is often a tedious work. Dependency injection, events, interceptors - all the work has to be done manually and everything needs to be wired together by hand. In Quarkus 3.2, we introduced an experimental feature to ease the testing of CDI components and mocking of their dependencies. It's a JUnit 5 extension that does not start a full Quarkus application but merely the CDI container and the Configuration service.

### The lifecycle

So when exactly does the `QuarkusComponentTest` start the CDI container? It depends on the value of `@org.junit.jupiter.api.TestInstance#lifecycle`. If the test instance lifecycle is `Lifecycle#PER_METHOD` (default) then the container is started during the *before each* test phase and stopped during the *after each* test phase. If the test instance lifecycle is `Lifecycle#PER_CLASS`\` then the container is started during the *before all* test phase and stopped during the *after all* test phase.

### Components under test

When writing a component test, it's essential to understand how the set of *tested components* is built. It's because the *tested components* are treated as real beans, but all *unsatisfied dependencies* are mocked automatically. What does it mean? Imagine that we have a bean `Foo` like this:

```java
package org.example;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Foo {

    @Inject
    Charlie charlie;

    public String ping() {
        return charlie.ping();
    }
}
```

It has one dependency - a bean `Charlie`. Now if you want to write a unit test for `Foo` you need to make sure the `Charlie` dependency is injected and functional. In `QuarkusComponentTest`, if you include `Foo` in the set of tested components but `Charlie` is not included, then a mock is automatically injected into `Foo.charlie`. What's also important is that you can inject the mock directly in the test using the `@InjectMock` annotation and configure the mock in a test method:

```java
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusComponentTest <1>
public class FooTest {

    @Inject
    Foo foo; <2>

    @InjectMock
    Charlie charlieMock; <3>

    @Test
    public void testPing() {
        Mockito.when(charlieMock.ping()).thenReturn("OK"); <4>
        assertEquals("OK", foo.ping());
    }
}
```

1. The `QuarkusComponentTest` annotation registers the JUnit extension.
2. The test injects `Foo` - it's included in the set of tested components. In other words, it's treated as a real CDI bean.
3. The test also injects a mock for `Charlie`. `Charlie` is an *unsatisfied* dependency for which a synthetic `@Singleton` bean is registered automatically. The injected reference is an "unconfigured" Mockito mock.
4. We can leverage the Mockito API in a test method to configure the behavior.

The initial set of tested components is derived from the test class:

1. First, the types of all fields annotated with `@jakarta.inject.Inject` are considered the component types.
2. The types of test methods parameters that are not annotated with `@InjectMock`, `@SkipInject`, or `@org.mockito.Mock` are also considered the component types.
3. Finally, if `@QuarkusComponentTest#addNestedClassesAsComponents()` is set to `true` (it is by default) then all static nested classes declared on the test class are components too.

Additional component classes can be set using `@QuarkusComponentTest#value()` or `QuarkusComponentTestExtensionBuilder#addComponentClasses()`.

## What's new?

1. Quarkus 3.13
   1. Removed the experimental status
2. Quarkus 3.21
   1. Basic support for nested tests
3. Quarkus 3.29
   1. Class loading refactoring
   2. `QuarkusComponentTestCallbacks`
   3. Integration with `quarkus-panache-mock`
   4. Support `@InjectMock` for built-in `Event`

### Class loading refactoring

In the previous versions of `QuarkusComponentTest` it wasn't possible to perform bytecode transformations. As a result, features like [simplified constructor injection](https://quarkus.io/guides/cdi-reference#simplified-constructor-injection) or ability to [handle final classes and methods](https://quarkus.io/guides/cdi-reference#unproxyable_classes_transformation) were not supported. That wasn't ideal because the tested CDI beans may have required changes before being used in a `QuarkusComponentTest`. This limitation is gone! The class loading is now more similar to a real Quarkus application.

### QuarkusComponentTestCallbacks

We also introduced a new SPI - `QuarkusComponentTestCallbacks` - that can be used to contribute additional logic to the `QuarkusComponentTest` extension. There are several callbacks that can be used to modify the behavior before the container is built, after the container is started, etc. It is a service provider, so all you have to do is to create a file located in `META-INF/services/io.quarkus.test.component.QuarkusComponentTestCallbacks` that contains the fully qualified name of your implementation class.

### Integration with `quarkus-panache-mock`

Thanks to class loading refactoring and `QuarkusComponentTestCallbacks` SPI, we're now able to do interesting stuff. Previously, whenever we got a question like: *"What if I use Panache entities with the active record pattern? How I do write a test for a component that is using such entities?"*, we had to admit that it wasn't possible. But it's no longer true. Once you add the `quarkus-panache-mock` module in your application you can write the component test in a similar way as with the [`PanacheMock` API](https://quarkus.io/guides/hibernate-orm-panache#using-the-active-record-pattern).

Given this simple entity:

```java
@Entity
public class Person extends PanacheEntity {

   public String name;

   public Person(String name) {
      this.name = name;
   }

}
```

That is used in a simple bean:

```java
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PersonService {

   public List<Person> getPersons() {
      return Person.listAll();
   }
}
```

You can write a component test like:

```java
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.panache.mock.MockPanacheEntities;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusComponentTest <1>
@MockPanacheEntities(Person.class) <2>
public class PersonServiceTest {

    @Inject
    PersonService personService; <3>
    
    @Test
    public void testGetPersons() { 
        Mockito.when(Person.listAll()).thenReturn(List.of(new Person("Tom")));
        List<Person> list = personService.getPersons();
        assertEquals(1, list.size());
        assertEquals("Tom", list.get(0).name);
    }

}
```

1. The `QuarkusComponentTest` annotation registers the JUnit extension.
2. `@MockPanacheEntities` installs mocks for the given entity classes.
3. The test injects the component under the test - `PersonService`.

### Support `@InjectMock` for built-in `Event`

It is now possible to mock the built-in bean for `jakarta.enterprise.event.Event`.

Given this simple CDI bean:

```java
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

@ApplicationScoped
public class PersonService {

   @Inject
   Event<Person> event;

   void register(Person person) {
      event.fire(person);
      // ... business logic
   }
}
```

You can write a component test like:

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import jakarta.inject.Inject;
import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.InjectMock;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusComponentTest <1>
public class PersonServiceTest {

   @Inject
   PersonService personService; <2>

   @InjectMock
   Event<Person> event; <3>

   @Test
   public void testRegister() {
      personService.register(new Person()); <4>
      Mockito.verify(event, Mockito.times(1)).fire(any()); <5>
   }

}
```

1. The `QuarkusComponentTest` annotation registers the JUnit extension.
2. The test injects the component under the test - `PersonService`.
3. Install the mock for the built-in `Event`.
4. Call the `register()` method that should trigger an event.
5. Verify that the `Event#fire()` method was called exactly once.

### Nested tests

JUnit `@Nested` tests may help to structure more complex test scenarios. However, its support has proven more troublesome than we expected. Still, we do support and test the basic use cases like this:

```java
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import io.quarkus.test.InjectMock;
import io.quarkus.test.component.TestConfigProperty;
import io.quarkus.test.component.QuarkusComponentTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusComponentTest <1>
public class NestedTest {

    @Inject
    Foo foo; <2>

    @InjectMock
    Charlie charlieMock; <3>
    
    @Nested
    class PingTest {
    
       @Test
       public void testPing() {
          Mockito.when(charlieMock.ping()).thenReturn("OK");
          assertEquals("OK", foo.ping());
       }
    }
    
    @Nested
    class PongTest {
    
       @Test
       public void testPong() {
          Mockito.when(charlieMock.pong()).thenReturn("NOK");
          assertEquals("NOK", foo.pong());
       }
    }
}
```

1. The `QuarkusComponentTest` annotation registers the JUnit extension.
2. The test injects the component under the test. `Foo` injects `Charlie`.
3. The test also injects a mock for `Charlie`. The injected reference is an "unconfigured" Mockito mock.

## Conclusion

If you want to test the business logic of your components in isolation, with different configurations and inputs, then `QuarkusComponentTest` is a good choice. It's fast, integrated with continuous testing, and extensible. As always, we are looking forward to your feedback!
