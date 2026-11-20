# Testing Spring-style applications in Quarkus

Spring developers migrating to Quarkus can keep using `@SpringBootTest` as they do in Spring Boot — no import change required. Under the hood, Quarkus treats `@SpringBootTest` as an alias for [`@QuarkusTest`](/guides/getting-started-testing/index.md): it starts the Quarkus runtime and CDI container, not a Spring Application Context.

> [!IMPORTANT]
> `@SpringBootTest` in Quarkus is cosmetic. It is equivalent to `@QuarkusTest` and starts the Quarkus CDI container (ArC), not a Spring Application Context. Spring classes and annotations are used only for reading metadata.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## Adding the dependency

Add `quarkus-test-spring` to your project as a test-scoped dependency:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-test-spring</artifactId>
    <scope>test</scope>
</dependency>
```

**build.gradle**

```gradle
testImplementation("io.quarkus:quarkus-test-spring")
```

## Writing a test

Use `@SpringBootTest` as you would in a Spring Boot project — the import stays the same:

```java
import org.springframework.boot.test.context.SpringBootTest; // <1>
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest // <2>
public class GreetingServiceTest {

    @Autowired // <3>
    GreetingService greetingService;

    @Test
    public void testGreeting() {
        assertEquals("Hello, Quarkus!", greetingService.greet("Quarkus"));
    }
}
```

1. Keep the original Spring Boot import — no change needed.
2. Quarkus starts the application and makes CDI beans available for injection.
3. Use `@Autowired` if you have `quarkus-spring-di` on the classpath (or standard CDI `@Inject` otherwise).

### HTTP endpoint testing

For testing REST endpoints, use [RestAssured](https://rest-assured.io/) exactly as you would with `@QuarkusTest`:

```java
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@SpringBootTest
public class GreetingControllerTest {

    @Test
    public void testHelloEndpoint() {
        given()
            .when().get("/greeting")
            .then()
                .statusCode(200)
                .body(is("Hello, Quarkus!"));
    }
}
```

### Native executable testing

To run the same tests against the packaged application (JVM or native mode), create a companion class annotated with `@QuarkusIntegrationTest` that extends your test:

```java
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class GreetingControllerIT extends GreetingControllerTest {
}
```

## Differences from Spring Boot's `@SpringBootTest`

`@SpringBootTest` in Quarkus is intentionally simpler than in Spring Boot. The table below summarises the main differences:

| Feature | Spring Boot | Quarkus |
| --- | --- | --- |
| Application context | Spring Application Context | Quarkus CDI container (ArC) |
| Annotation attributes (`webEnvironment`, `classes`, `properties`, `args`) | Supported | Not supported — configure via [Quarkus configuration](/guides/config-reference/index.md) and [test profiles](/guides/getting-started-testing/index.md#testing_different_profiles) |
| Dependency injection in tests | `@Autowired` | `@Autowired` (needs `quarkus-spring-di`) |
| Dev Services (databases, messaging, etc.) | Testcontainers via Spring Boot autoconfiguration | [Quarkus Dev Services](/guides/dev-services/index.md) — zero configuration required |
| MockBean | `@MockBean` | [`@InjectMock`](/guides/getting-started-testing/index.md#mock-support) from `quarkus-junit-mockito` |
| Test profiles | Spring profiles via `@ActiveProfiles` | [`@TestProfile`](/guides/getting-started-testing/index.md#testing_different_profiles) |

## More Spring guides

Quarkus has more Spring compatibility features. See the following guides for more details:

- [Quarkus Extension for Spring DI](/guides/spring-di/index.md)
- [Quarkus Extension for Spring Web API](/guides/spring-web/index.md)
- [Quarkus Extension for Spring Data JPA](/guides/spring-data-jpa/index.md)
- [Quarkus Extension for Spring Data REST](/guides/spring-data-rest/index.md)
- [Quarkus Extension for Spring Security](/guides/spring-security/index.md)
- [Quarkus Extension for Spring Boot properties](/guides/spring-boot-properties/index.md)
- [Quarkus Extension for Spring Cache](/guides/spring-cache/index.md)
- [Quarkus Extension for Spring Scheduled](/guides/spring-scheduled/index.md)
- [Reading properties from Spring Cloud Config Server](/guides/spring-cloud-config-client/index.md)
