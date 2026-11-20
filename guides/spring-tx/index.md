# Quarkus extension for Spring Transaction API

While users are encouraged to use Jakarta `@Transactional` annotations directly, Quarkus provides a compatibility layer for Spring `@Transactional` in the form of the `spring-tx` extension.

This guide explains how a Quarkus application can leverage the Spring `@Transactional` annotation to manage transactions.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

- Some familiarity with the Spring DI extension

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:spring-tx-quickstart \
    --extension='spring-web,spring-di,spring-tx,spring-data-jpa,jdbc-postgresql' \
    --no-code
cd spring-tx-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=spring-tx-quickstart \
    -Dextensions='spring-web,spring-di,spring-tx,spring-data-jpa,jdbc-postgresql' \
    -DnoCode
cd spring-tx-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=spring-tx-quickstart"`

This command generates a project which imports the `spring-tx`, `spring-di`, `spring-web`, and `spring-data-jpa` extensions.

If you already have your Quarkus project configured, you can add the `spring-tx` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add spring-tx
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='spring-tx'
```

**Gradle**

```bash
./gradlew addExtension --extensions='spring-tx'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-spring-tx</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-spring-tx")
```

## Define the entity

Throughout the course of this guide, the following JPA Entity will be used:

```java
package org.acme.spring.tx;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Fruit {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    public Fruit() {
    }

    public Fruit(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

## Define the repository

In a typical Spring Data fashion, create a repository for `Fruit`:

```java
package org.acme.spring.tx;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface FruitRepository extends CrudRepository<Fruit, Long> {

    List<Fruit> findByName(String name);
}
```

## Creating a transactional service

Now let's create a service that uses Spring's `@Transactional` annotation to manage transactions. Create `src/main/java/org/acme/spring/tx/FruitService.java` with the following content:

```java
package org.acme.spring.tx;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FruitService {

    @Autowired
    FruitRepository fruitRepository;

    @Transactional // <1>
    public Fruit addFruit(String name) {
        return fruitRepository.save(new Fruit(name));
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRED) // <2>
    public void addMultipleFruits(List<String> names) {
        for (String name : names) {
            fruitRepository.save(new Fruit(name));
        }
    }
}
```

1. The Spring `@Transactional` annotation is automatically transformed into a Jakarta `@Transactional` annotation at build time.
2. You can specify propagation attributes just like in Spring. `REQUIRED` is the default.

You can also place `@Transactional` at the class level to make all public methods transactional:

```java
package org.acme.spring.tx;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional // <1>
public class TransactionalService {

    public void doSomething() {
        // this method is transactional
    }

    public void doSomethingElse() {
        // this method is also transactional
    }
}
```

1. All public methods of this class are transactional.

## Controlling rollback behavior

Spring's `rollbackFor` and `noRollbackFor` attributes are supported:

```java
package org.acme.spring.tx;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RollbackService {

    @Transactional(rollbackFor = BusinessException.class) // <1>
    public void performBusinessOperation() throws BusinessException {
        // if BusinessException is thrown, the transaction rolls back
    }

    @Transactional(noRollbackFor = NonCriticalException.class) // <2>
    public void performTolerantOperation() throws NonCriticalException {
        // if NonCriticalException is thrown, the transaction still commits
    }
}
```

1. The transaction will roll back if `BusinessException` (or a subclass) is thrown.
2. The transaction will NOT roll back if `NonCriticalException` is thrown.

## Update the Jakarta REST resource

With the repository and service in place, create the Jakarta REST resource that will use the `FruitService`. Create `src/main/java/org/acme/spring/tx/FruitResource.java` with the following content:

```java
package org.acme.spring.tx;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FruitResource {

    @Autowired
    FruitService fruitService;

    @Autowired
    FruitRepository fruitRepository;

    @GetMapping("/fruits")
    public List<Fruit> list() {
        return (List<Fruit>) fruitRepository.findAll();
    }

    @PostMapping("/fruits")
    public Fruit add(@RequestBody Fruit fruit) {
        return fruitService.addFruit(fruit.getName());
    }
}
```

## Package and run the application

> [!NOTE]
> Quarkus Dev Services will automatically start a PostgreSQL container in dev and test modes, so no manual database configuration is needed.

Run the application with:

**CLI**

```bash
quarkus dev
```

**Maven**

```bash
./mvnw quarkus:dev
```

**Gradle**

```bash
./gradlew --console=plain quarkusDev
```

In another terminal, add a fruit:

```bash
curl -X POST -H "Content-Type: application/json" -d '{"name": "Apple"}' http://localhost:8080/fruits
```

Then list all fruits:

```bash
curl http://localhost:8080/fruits
```

As usual, the application can be packaged using:

**CLI**

```bash
quarkus build
```

**Maven**

```bash
./mvnw install
```

**Gradle**

```bash
./gradlew build
```

And executed using `java -jar target/quarkus-app/quarkus-run.jar`.

You can also generate the native executable with:

**CLI**

```bash
quarkus build --native
```

**Maven**

```bash
./mvnw install -Dnative
```

**Gradle**

```bash
./gradlew build -Dquarkus.native.enabled=true
```

## Supported Spring @Transactional features

The table below summarizes the supported attributes:

**Supported Spring @Transactional attributes**

| Attribute | Status | Comments |
| --- | --- | --- |
| `propagation` | Supported | All values except `NESTED`: `REQUIRED` (default), `REQUIRES_NEW`, `SUPPORTS`, `MANDATORY`, `NOT_SUPPORTED`, `NEVER`. `NESTED` causes a build-time error. |
| `rollbackFor` | Supported |  |
| `rollbackForClassName` | Supported |  |
| `noRollbackFor` | Supported |  |
| `noRollbackForClassName` | Supported |  |
| `readOnly` | Unsupported | Ignored with a warning |
| `timeout` / `timeoutString` | Unsupported | Ignored with a warning |
| `isolation` | Unsupported | Only `DEFAULT` is accepted; other values are ignored with a warning |
| `value` / `transactionManager` | Unsupported | Quarkus manages the transaction manager |
| `label` | Unsupported | Ignored with a warning |

## Important Technical Note

Please note that the Spring support in Quarkus does not start a Spring Application Context nor are any Spring infrastructure classes run. Spring classes and annotations are only used for reading metadata and / or are used as user code method return types or parameter types. What that means for end users, is that adding arbitrary Spring libraries will not have any effect. Moreover, Spring infrastructure classes (like `org.springframework.beans.factory.config.BeanPostProcessor` for example) will not be executed.

## More Spring guides

Quarkus has more Spring compatibility features. See the following guides for more details:

- [Quarkus - Extension for Spring DI](/guides/spring-di/index.md)
- [Quarkus - Extension for Spring Web](/guides/spring-web/index.md)
- [Quarkus - Extension for Spring Data JPA](/guides/spring-data-jpa/index.md)
- [Quarkus - Extension for Spring Data REST](/guides/spring-data-rest/index.md)
- [Quarkus - Extension for Spring Security](/guides/spring-security/index.md)
- [Quarkus - Reading properties from Spring Cloud Config Server](/guides/spring-cloud-config-client/index.md)
- [Quarkus - Extension for Spring Boot properties](/guides/spring-boot-properties/index.md)
- [Quarkus - Extension for Spring Scheduled](/guides/spring-scheduled/index.md)
- [Quarkus - Extension for Spring Cache](/guides/spring-cache/index.md)
