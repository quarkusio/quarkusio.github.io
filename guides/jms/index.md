# Using JMS

This guide demonstrates how your Quarkus application can use JMS messaging via the Apache Qpid JMS AMQP client, or alternatively the Apache ActiveMQ Artemis JMS client.

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered preview.
>
> In *preview*, backward compatibility and presence in the ecosystem is not guaranteed. Specific improvements might require changing configuration or APIs, and plans to become *stable* are under way. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

- A running Artemis server, or Docker to start one

## Architecture

In this guide, we are going to generate (random) prices in one component. These prices are written to a queue (`prices`) using a JMS client. Another component reads from the `prices` queue and stores the latest price. The data can be fetched from a browser using a fetch button from a Jakarta REST resource.

The guide can be used either via the Apache Qpid JMS AMQP client as detailed immediately below, or alternatively with the Apache ActiveMQ Artemis JMS client given some different configuration as [detailed later](#artemis-jms).

<a id="qpid-jms-amqp"></a>
## Qpid JMS - AMQP

In the detailed steps below we will use the [Apache Qpid JMS](https://qpid.apache.org/components/jms/) client via the [Quarkus Qpid JMS extension](https://github.com/amqphub/quarkus-qpid-jms/). Qpid JMS uses the AMQP 1.0 ISO standard as its wire protocol, allowing it to be used with a variety of AMQP 1.0 servers and services such as ActiveMQ Artemis, ActiveMQ 5, Qpid Broker-J, Qpid Dispatch router, Azure Service Bus, and more.

### Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/amqphub/quarkus-qpid-jms-quickstart.git`, or download an [archive](https://github.com/amqphub/quarkus-qpid-jms-quickstart/archive/main.zip).

### Creating the Maven Project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:jms-quickstart \
    --extension='rest,qpid-jms' \
    --no-code
cd jms-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=jms-quickstart \
    -Dextensions='rest,qpid-jms' \
    -DnoCode
cd jms-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=jms-quickstart"`

This command generates a new project importing the quarkus-qpid-jms extension:

**pom.xml**

```xml
<dependency>
    <groupId>org.amqphub.quarkus</groupId>
    <artifactId>quarkus-qpid-jms</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("org.amqphub.quarkus:quarkus-qpid-jms")
```

<a id="starting-the-broker"></a>
### Starting the broker

Then, we need an AMQP broker. In this case we will use an Apache ActiveMQ Artemis server. You can follow the instructions from the [Apache Artemis website](https://activemq.apache.org/components/artemis/) or start a broker via docker using the [ArtemisCloud](https://artemiscloud.io/) container image:

```bash
docker run -it --rm -p 8161:8161 -p 61616:61616 -p 5672:5672 -e AMQ_USER=quarkus -e AMQ_PASSWORD=quarkus quay.io/arkmq-org/arkmq-org-broker:artemis.2.55.0
```

### The price producer

Create the `src/main/java/org/acme/jms/PriceProducer.java` file, with the following content:

```java
package org.acme.jms;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

/**
 * A bean producing random prices every 5 seconds and sending them to the prices JMS queue.
 */
@ApplicationScoped
public class PriceProducer implements Runnable {

    @Inject
    ConnectionFactory connectionFactory;

    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    void onStart(@Observes StartupEvent ev) {
        scheduler.scheduleWithFixedDelay(this, 0L, 5L, TimeUnit.SECONDS);
    }

    void onStop(@Observes ShutdownEvent ev) {
        scheduler.shutdown();
    }

    @Override
    public void run() {
        try (JMSContext context = connectionFactory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
            context.createProducer().send(context.createQueue("prices"), Integer.toString(random.nextInt(100)));
        }
    }
}
```

### The price consumer

The price consumer reads the prices from JMS, and stores the last one. Create the `src/main/java/org/acme/jms/PriceConsumer.java` file with the following content:

```java
package org.acme.jms;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

/**
 * A bean consuming prices from the JMS queue.
 */
@ApplicationScoped
public class PriceConsumer implements Runnable {

    @Inject
    ConnectionFactory connectionFactory;

    private final ExecutorService scheduler = Executors.newSingleThreadExecutor();

    private volatile String lastPrice;

    public String getLastPrice() {
        return lastPrice;
    }

    void onStart(@Observes StartupEvent ev) {
        scheduler.submit(this);
    }

    void onStop(@Observes ShutdownEvent ev) {
        scheduler.shutdown();
    }

    @Override
    public void run() {
        try (JMSContext context = connectionFactory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
            JMSConsumer consumer = context.createConsumer(context.createQueue("prices"));
            while (true) {
                Message message = consumer.receive();
                if (message == null) return;
                lastPrice = message.getBody(String.class);
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
```

### The price resource

Finally, let's create a simple Jakarta REST resource to show the last price. Create the `src/main/java/org/acme/jms/PriceResource.java` file with the following content:

```java
package org.acme.jms;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * A simple resource showing the last price.
 */
@Path("/prices")
public class PriceResource {

    @Inject
    PriceConsumer consumer;

    @GET
    @Path("last")
    @Produces(MediaType.TEXT_PLAIN)
    public String last() {
        return consumer.getLastPrice();
    }
}
```

### The HTML page

Final touch, the HTML page reading the converted prices using SSE.

Create the `src/main/resources/META-INF/resources/prices.html` file, with the following content:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Prices</title>

    <link rel="stylesheet" type="text/css"
          href="https://cdnjs.cloudflare.com/ajax/libs/patternfly/3.24.0/css/patternfly.min.css">
    <link rel="stylesheet" type="text/css"
          href="https://cdnjs.cloudflare.com/ajax/libs/patternfly/3.24.0/css/patternfly-additions.min.css">
</head>
<body>
<div class="container">

    <h2>Last price</h2>
    <div class="row">
    <p class="col-md-12"><button id="fetch">Fetch</button>The last price is <strong><span id="content">N/A</span>&nbsp;&euro;</strong>.</p>
    </div>
</div>
</body>
<script>
    document.getElementById("fetch").addEventListener("click", function() {
        fetch("/prices/last").then(function (response) {
            response.text().then(function (text) {
                document.getElementById("content").textContent = text;
            })
        })
    })
</script>
</html>
```

Nothing spectacular here. On each fetch, it updates the page.

### Configure the Qpid JMS properties

We need to configure the Qpid JMS properties used by the extension when injecting the ConnectionFactory.

This is done in the `src/main/resources/application.properties` file.

```properties
# Configures the Qpid JMS properties.
quarkus.qpid-jms.url=amqp://localhost:5672
quarkus.qpid-jms.username=quarkus
quarkus.qpid-jms.password=quarkus
```

More detail about the configuration are available in the [Quarkus Qpid JMS](https://github.com/amqphub/quarkus-qpid-jms#configuration) documentation.

<a id="get-it-running"></a>
### Get it running

If you followed the instructions, you should have the Artemis server running. Then, you just need to run the application using:

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

Open `http://localhost:8080/prices.html` in your browser.

### Running Native

You can build the native executable with:

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

Or, if you don't have GraalVM installed, you can instead use Docker to build the native executable using:

**CLI**

```bash
quarkus build --native --no-tests -Dquarkus.native.container-build=true
# The --no-tests flag is required only on Windows and macOS.
```

**Maven**

```bash
./mvnw install -Dnative -DskipTests -Dquarkus.native.container-build=true
```

**Gradle**

```bash
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

and then run with:

```bash
./target/jms-quickstart-1.0.0-SNAPSHOT-runner
```

Open `http://localhost:8080/prices.html` in your browser.

---

<a id="artemis-jms"></a>
## Artemis JMS

The above steps detailed using the Qpid JMS AMQP client, however the guide can also be used with the Artemis JMS client. Many of the individual steps are exactly as previously [detailed above for Qpid JMS](#qpid-jms-amqp). The individual component code is the same. The only differences are in the dependency for the initial project creation, and the configuration properties used. These changes are detailed below and should be substituted for the equivalent step during the sequence above.

### Solution

You can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The Artemis JMS solution is located in the `jms-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/jms-quickstart).

### Creating the Maven Project

Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:jms-quickstart \
    --extension='rest,artemis-jms' \
    --no-code
cd jms-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=jms-quickstart \
    -Dextensions='rest,artemis-jms' \
    -DnoCode
cd jms-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=jms-quickstart"`

This creates a new project importing the quarkus-artemis-jms extension:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkiverse.artemis</groupId>
    <artifactId>quarkus-artemis-jms</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkiverse.artemis:quarkus-artemis-jms")
```

With the project created, you can resume from [Starting the broker](#starting-the-broker) in the detailed steps above and proceed until configuring the `application.properties` file, when you should use the Artemis configuration below instead.

### Configure the Artemis properties

We need to configure the Artemis connection properties. This is done in the `src/main/resources/application.properties` file.

```properties
# Configures the Artemis properties.
quarkus.artemis.enabled=true
quarkus.artemis.url=tcp://localhost:61616
quarkus.artemis.username=quarkus
quarkus.artemis.password=quarkus
```

> [!NOTE]
> The `quarkus.artemis.enabled` property is a build-time setting that must be set to `true` for the `ConnectionFactory` bean to be created. Without it, the extension will not produce a `ConnectionFactory` and injection will fail with an `UnsatisfiedResolutionException`.

With the Artemis properties configured, you can resume the steps above from [Get it running](#get-it-running).

### Configuration Reference

To know more about how to configure the Artemis JMS client, have a look at [the documentation of the extension](https://docs.quarkiverse.io/quarkus-artemis/dev/index.html).
