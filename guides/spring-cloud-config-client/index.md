# Reading properties from Spring Cloud Config Server

This guide explains how your Quarkus application can read configuration properties at runtime from the [Spring Cloud Config Server](https://cloud.spring.io/spring-cloud-config).

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step.

## Stand up a Config Server

To stand up the Config Server required for this guide, please follow the instructions outlined [here](https://github.com/spring-guides/gs-centralized-configuration#stand-up-a-config-server). The end result of that process is a running Config Server that will provide the `Hello world` value for a configuration property named `message` when the application querying the server is named `a-bootiful-client`.

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:spring-cloud-config-quickstart \
    --extension='rest,spring-cloud-config-client' \
    --no-code
cd spring-cloud-config-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=spring-cloud-config-quickstart \
    -Dextensions='rest,spring-cloud-config-client' \
    -DnoCode
cd spring-cloud-config-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=spring-cloud-config-quickstart"`

This command generates a project which imports the `spring-cloud-config-client` extension.

If you already have your Quarkus project configured, you can add the `spring-cloud-config-client` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add spring-cloud-config-client
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='spring-cloud-config-client'
```

**Gradle**

```bash
./gradlew addExtension --extensions='spring-cloud-config-client'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-spring-cloud-config-client</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-spring-cloud-config-client")
```

## GreetingController

First, create a simple `GreetingResource` Jakarta REST resource in the `src/main/java/org/acme/spring/cloud/config/client/GreetingResource.java` file that looks like:

```java
package org.acme.spring.spring.cloud.config.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }
}
```

As we want to use configuration properties obtained from the Config Server, we will update the `GreetingResource` to inject the `message` property. The updated code will look like this:

```java
package org.acme.spring.spring.cloud.config.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/hello")
public class GreetingResource {

    @ConfigProperty(name = "message", defaultValue="hello default")
    String message;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return message;
    }
}
```

## Configuring the application

Quarkus provides various configuration knobs under the `quarkus.spring-cloud-config` root. For the purposes of this guide, our Quarkus application is going to be configured in `application.properties` as follows:

```properties
# use the same name as the application name that was configured when standing up the Config Server
quarkus.application.name=a-bootiful-client
# enable retrieval of configuration from the Config Server - this is off by default
quarkus.spring-cloud-config.enabled=true
# configure the URL where the Config Server listens to HTTP requests - this could have been left out since http://localhost:8888 is the default
quarkus.spring-cloud-config.url=http://localhost:8888
```

> [!TIP]
> If you are using Gradle, the Gradle setting `rootProject.name` has precedence over `quarkus.application.name` so be sure to set the Gradle property to the application name you want the Spring Cloud Config server to see.

## Package and run the application

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

Open your browser to <http://localhost:8080/greeting>.

The result should be: `Hello world` as it is the value obtained from the Spring Cloud Config server.

## Run the application as a native executable

You can of course create a native image using the instructions of the [Building a native executable guide](/guides/building-native-image/index.md).

## More Spring guides

Quarkus has more Spring compatibility features. See the following guides for more details:

- [Quarkus - Extension for Spring DI](/guides/spring-di/index.md)
- [Quarkus - Extension for Spring Web](/guides/spring-web/index.md)
- [Quarkus - Extension for Spring Data JPA](/guides/spring-data-jpa/index.md)
- [Quarkus - Extension for Spring Data REST](/guides/spring-data-rest/index.md)
- [Quarkus - Extension for Spring Security](/guides/spring-security/index.md)
- [Quarkus - Extension for Spring Boot properties](/guides/spring-boot-properties/index.md)
- [Quarkus - Extension for Spring Cache](/guides/spring-cache/index.md)
- [Quarkus - Extension for Spring Scheduled](/guides/spring-scheduled/index.md)
- [Quarkus - Extension for Spring Transaction](/guides/spring-tx/index.md)
- [Testing Spring-style Applications in Quarkus](/guides/spring-test/index.md)

<a id="spring-cloud-config-client-configuration-reference"></a>
## Spring Cloud Config Client Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-enabled"></a> [`quarkus.spring-cloud-config.enabled`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-enabled) config_property_copy_button:+++quarkus.spring-cloud-config.enabled+++[]<br><br>If enabled, will try to read the configuration from a Spring Cloud Config Server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-fail-fast"></a> [`quarkus.spring-cloud-config.fail-fast`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-fail-fast) config_property_copy_button:+++quarkus.spring-cloud-config.fail-fast+++[]<br><br>If set to true, the application will not stand up if it cannot obtain configuration from the Config Server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_FAIL_FAST+++[] | boolean | `false` |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-url"></a> [`quarkus.spring-cloud-config.url`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-url) config_property_copy_button:+++quarkus.spring-cloud-config.url+++[]<br><br>The Base URI where the Spring Cloud Config Server is available<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_URL+++[] | string | `http://localhost:8888` |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-name"></a> [`quarkus.spring-cloud-config.name`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-name) config_property_copy_button:+++quarkus.spring-cloud-config.name+++[]<br><br>Name of the application on Spring Cloud Config server. Could be a list of names to load multiple files (value separated by a comma)<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_NAME+++[] | string | `${quarkus.application.name:}` |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-label"></a> [`quarkus.spring-cloud-config.label`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-label) config_property_copy_button:+++quarkus.spring-cloud-config.label+++[]<br><br>The label to be used to pull remote configuration properties. The default is set on the Spring Cloud Config Server (generally "master" when the server uses a Git backend).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_LABEL+++[] | string |  |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-connection-timeout"></a> [`quarkus.spring-cloud-config.connection-timeout`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-connection-timeout) config_property_copy_button:+++quarkus.spring-cloud-config.connection-timeout+++[]<br><br>The amount of time to wait when initially establishing a connection before giving up and timing out.<br><br>Specify `0` to wait indefinitely.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_CONNECTION_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-spring-cloud-config-client_quarkus-spring-cloud-config) | `10S` |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-read-timeout"></a> [`quarkus.spring-cloud-config.read-timeout`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-read-timeout) config_property_copy_button:+++quarkus.spring-cloud-config.read-timeout+++[]<br><br>The amount of time to wait for a read on a socket before an exception is thrown.<br><br>Specify `0` to wait indefinitely.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_READ_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-spring-cloud-config-client_quarkus-spring-cloud-config) | `60S` |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-username"></a> [`quarkus.spring-cloud-config.username`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-username) config_property_copy_button:+++quarkus.spring-cloud-config.username+++[]<br><br>The username to be used if the Config Server has BASIC Auth enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_USERNAME+++[] | string |  |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-password"></a> [`quarkus.spring-cloud-config.password`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-password) config_property_copy_button:+++quarkus.spring-cloud-config.password+++[]<br><br>The password to be used if the Config Server has BASIC Auth enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_PASSWORD+++[] | string |  |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-trust-store"></a> [`quarkus.spring-cloud-config.trust-store`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-trust-store) config_property_copy_button:+++quarkus.spring-cloud-config.trust-store+++[]<br><br>TrustStore to be used containing the SSL certificate used by the Config server Can be either a classpath resource or a file system path<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_TRUST_STORE+++[] | path |  |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-trust-store-password"></a> [`quarkus.spring-cloud-config.trust-store-password`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-trust-store-password) config_property_copy_button:+++quarkus.spring-cloud-config.trust-store-password+++[]<br><br>Password of TrustStore to be used containing the SSL certificate used by the Config server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_TRUST_STORE_PASSWORD+++[] | string |  |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-key-store"></a> [`quarkus.spring-cloud-config.key-store`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-key-store) config_property_copy_button:+++quarkus.spring-cloud-config.key-store+++[]<br><br>KeyStore to be used containing the SSL certificate for authentication with the Config server Can be either a classpath resource or a file system path<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_KEY_STORE+++[] | path |  |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-key-store-password"></a> [`quarkus.spring-cloud-config.key-store-password`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-key-store-password) config_property_copy_button:+++quarkus.spring-cloud-config.key-store-password+++[]<br><br>Password of KeyStore to be used containing the SSL certificate for authentication with the Config server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_KEY_STORE_PASSWORD+++[] | string |  |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-key-password"></a> [`quarkus.spring-cloud-config.key-password`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-key-password) config_property_copy_button:+++quarkus.spring-cloud-config.key-password+++[]<br><br>Password to recover key from KeyStore for SSL client authentication with the Config server If no value is provided, the key-store-password will be used<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_KEY_PASSWORD+++[] | string |  |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-trust-certs"></a> [`quarkus.spring-cloud-config.trust-certs`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-trust-certs) config_property_copy_button:+++quarkus.spring-cloud-config.trust-certs+++[]<br><br>When using HTTPS and no keyStore has been specified, whether to trust all certificates<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_TRUST_CERTS+++[] | boolean | `${quarkus.tls.trust-all:false}` |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-headers-header-name"></a> [`quarkus.spring-cloud-config.headers."header-name"`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-headers-header-name) config_property_copy_button:+++quarkus.spring-cloud-config.headers."header-name"+++[]<br><br>Custom headers to pass the Spring Cloud Config Server when performing the HTTP request<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_HEADERS__HEADER_NAME_+++[] | Map\<String,String> |  |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-profiles"></a> [`quarkus.spring-cloud-config.profiles`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-profiles) config_property_copy_button:+++quarkus.spring-cloud-config.profiles+++[]<br><br>The profiles to use for lookup<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_PROFILES+++[] | list of string |  |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-ordinal"></a> [`quarkus.spring-cloud-config.ordinal`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-ordinal) config_property_copy_button:+++quarkus.spring-cloud-config.ordinal+++[]<br><br>Microprofile Config ordinal.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_ORDINAL+++[] | int | `450` |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-discovery-enabled"></a> [`quarkus.spring-cloud-config.discovery.enabled`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-discovery-enabled) config_property_copy_button:+++quarkus.spring-cloud-config.discovery.enabled+++[]<br><br>Enable discovery of the Spring Cloud Config Server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_DISCOVERY_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-discovery-service-id"></a> [`quarkus.spring-cloud-config.discovery.service-id`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-discovery-service-id) config_property_copy_button:+++quarkus.spring-cloud-config.discovery.service-id+++[]<br><br>The service ID to use when discovering the Spring Cloud Config Server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_DISCOVERY_SERVICE_ID+++[] | string |  |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-discovery-eureka-config-service-url-service-url"></a> [`quarkus.spring-cloud-config.discovery.eureka-config.service-url."service-url"`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-discovery-eureka-config-service-url-service-url) config_property_copy_button:+++quarkus.spring-cloud-config.discovery.eureka-config.service-url."service-url"+++[]<br><br>The service URL to use to specify Eureka server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_DISCOVERY_EUREKA_CONFIG_SERVICE_URL__SERVICE_URL_+++[] | Map\<String,String> |  |
| <a id="quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-discovery-eureka-config-registry-fetch-interval-seconds"></a> [`quarkus.spring-cloud-config.discovery.eureka-config.registry-fetch-interval-seconds`](#quarkus-spring-cloud-config-client_quarkus-spring-cloud-config-discovery-eureka-config-registry-fetch-interval-seconds) config_property_copy_button:+++quarkus.spring-cloud-config.discovery.eureka-config.registry-fetch-interval-seconds+++[]<br><br>Indicates how often(in seconds) to fetch the registry information from the eureka server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_CLOUD_CONFIG_DISCOVERY_EUREKA_CONFIG_REGISTRY_FETCH_INTERVAL_SECONDS+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-spring-cloud-config-client_quarkus-spring-cloud-config) | `30S` |

<a id="duration-note-anchor-quarkus-spring-cloud-config-client_quarkus-spring-cloud-config"></a>

> [!NOTE]
> **About the Duration format**
>
> To write duration values, use the standard `java.time.Duration` format. See the [Duration#parse() Java API documentation](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html#parse(java.lang.CharSequence)) for more information.
>
> You can also use a simplified format, starting with a number:
>
> - If the value is only a number, it represents time in seconds.
> - If the value is a number followed by `ms`, it represents time in milliseconds.
>
> In other cases, the simplified format is translated to the `java.time.Duration` format for parsing:
>
> - If the value is a number followed by `h`, `m`, or `s`, it is prefixed with `PT`.
> - If the value is a number followed by `d`, it is prefixed with `P`.
