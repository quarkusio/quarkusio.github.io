# Using OpenAPI and Swagger UI

This guide explains how your Quarkus application can expose its API description through an OpenAPI specification and how you can test it via a user-friendly UI named Swagger UI.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## Architecture

In this guide, we create a straightforward REST application to demonstrate how fast you can expose your API specification and benefit from a user interface to test it.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can skip right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `openapi-swaggerui-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/openapi-swaggerui-quickstart).

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:openapi-swaggerui-quickstart \
    --extension='rest-jackson' \
    --no-code
cd openapi-swaggerui-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=openapi-swaggerui-quickstart \
    -Dextensions='rest-jackson' \
    -DnoCode
cd openapi-swaggerui-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=openapi-swaggerui-quickstart"`

## Expose a REST Resource

We will create a `Fruit` bean and a `FruitResource` REST resource (feel free to take a look to the [Writing JSON REST services guide](/guides/rest-json/index.md) if your want more details on how to build a REST API with Quarkus).

```java
package org.acme.openapi.swaggerui;

public class Fruit {

    public String name;
    public String description;

    public Fruit() {
    }

    public Fruit(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
```

```java
package org.acme.openapi.swaggerui;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

@Path("/fruits")
public class FruitResource {

    private Set<Fruit> fruits = Collections.newSetFromMap(Collections.synchronizedMap(new LinkedHashMap<>()));

    public FruitResource() {
        fruits.add(new Fruit("Apple", "Winter fruit"));
        fruits.add(new Fruit("Pineapple", "Tropical fruit"));
    }

    @GET
    public Set<Fruit> list() {
        return fruits;
    }

    @POST
    public Set<Fruit> add(Fruit fruit) {
        fruits.add(fruit);
        return fruits;
    }

    @DELETE
    public Set<Fruit> delete(Fruit fruit) {
        fruits.removeIf(existingFruit -> existingFruit.name.contentEquals(fruit.name));
        return fruits;
    }
}
```

You can also create a test:

```java
package org.acme.openapi.swaggerui;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;

@QuarkusTest
public class FruitResourceTest {

    @Test
    public void testList() {
        given()
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .body("$.size()", is(2),
                        "name", containsInAnyOrder("Apple", "Pineapple"),
                        "description", containsInAnyOrder("Winter fruit", "Tropical fruit"));
    }

    @Test
    public void testAdd() {
        given()
                .body("{\"name\": \"Pear\", \"description\": \"Winter fruit\"}")
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .post("/fruits")
                .then()
                .statusCode(200)
                .body("$.size()", is(3),
                        "name", containsInAnyOrder("Apple", "Pineapple", "Pear"),
                        "description", containsInAnyOrder("Winter fruit", "Tropical fruit", "Winter fruit"));

        given()
                .body("{\"name\": \"Pear\", \"description\": \"Winter fruit\"}")
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .delete("/fruits")
                .then()
                .statusCode(200)
                .body("$.size()", is(2),
                        "name", containsInAnyOrder("Apple", "Pineapple"),
                        "description", containsInAnyOrder("Winter fruit", "Tropical fruit"));
    }
}
```

## Expose OpenAPI Specifications

Quarkus provides the [SmallRye OpenAPI](https://github.com/smallrye/smallrye-open-api/) extension compliant with the [MicroProfile OpenAPI](https://github.com/eclipse/microprofile-open-api/) specification in order to generate your API [OpenAPI v3 specification](https://spec.openapis.org/oas/v3.1.0.html).

You just need to add the `openapi` extension to your Quarkus application:

**CLI**

```bash
quarkus extension add quarkus-smallrye-openapi
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='quarkus-smallrye-openapi'
```

**Gradle**

```bash
./gradlew addExtension --extensions='quarkus-smallrye-openapi'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-openapi</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-smallrye-openapi")
```

Now, we are ready to run our application:

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

Once your application is started, you can make a request to the default `/q/openapi` endpoint:

```shell
$ curl http://localhost:8080/q/openapi
openapi: 3.1.0
info:
  title: Generated API
  version: "1.0"
paths:
  /fruits:
    get:
      responses:
        200:
          description: OK
          content:
            application/json: {}
    post:
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/Fruit'
      responses:
        200:
          description: OK
          content:
            application/json: {}
    delete:
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/Fruit'
      responses:
        200:
          description: OK
          content:
            application/json: {}
components:
  schemas:
    Fruit:
      properties:
        description:
          type: string
        name:
          type: string
```

> [!NOTE]
> If you do not like the default endpoint location `/q/openapi`, you can change it by adding the following configuration to your `application.properties`:
>
> ```properties
> quarkus.smallrye-openapi.path=/swagger
> ```

> [!NOTE]
> You can request the OpenAPI in JSON format using the `format` query parameter. For example:
>
> ```properties
> /q/openapi?format=json
> ```

Hit `CTRL+C` to stop the application.

## Providing Application Level OpenAPI Annotations

There are some MicroProfile OpenAPI annotations which describe global API information, such as the following:

- API Title
- API Description
- Version
- Contact Information
- License

All of this information (and more) can be included in your Java code by using appropriate OpenAPI annotations on a Jakarta REST `Application` class.  Because a Jakarta REST `Application` class is not required in Quarkus, you will likely have to create one.  It can simply be an empty class that extends `jakarta.ws.rs.core.Application`.  This empty class can then be annotated with various OpenAPI annotations such as `@OpenAPIDefinition`.  For example:

```java
@OpenAPIDefinition(
    tags = {
            @Tag(name="widget", description="Widget operations."),
            @Tag(name="gasket", description="Operations related to gaskets")
    },
    info = @Info(
        title="Example API",
        version = "1.0.1",
        contact = @Contact(
            name = "Example API Support",
            url = "http://exampleurl.com/contact",
            email = "techsupport@example.com"),
        license = @License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0.html"))
)
public class ExampleApiApplication extends Application {
}
```

Another option, that is a feature provided by SmallRye and not part of the specification, is to use configuration to add this global API information. This way, you do not need to have a Jakarta REST `Application` class, and you can name the API differently per environment.

For example, add the following to your `application.properties`:

```properties
quarkus.smallrye-openapi.info-title=Example API
%dev.quarkus.smallrye-openapi.info-title=Example API (development)
%test.quarkus.smallrye-openapi.info-title=Example API (test)
quarkus.smallrye-openapi.info-version=1.0.1
quarkus.smallrye-openapi.info-description=Just an example service
quarkus.smallrye-openapi.info-terms-of-service=Your terms here
quarkus.smallrye-openapi.info-contact-email=techsupport@example.com
quarkus.smallrye-openapi.info-contact-name=Example API Support
quarkus.smallrye-openapi.info-contact-url=http://exampleurl.com/contact
quarkus.smallrye-openapi.info-license-name=Apache 2.0
quarkus.smallrye-openapi.info-license-url=https://www.apache.org/licenses/LICENSE-2.0.html
```

This will give you similar information as the `@OpenAPIDefinition` example above.

## Enhancing the OpenAPI Schema with Filters

You can change the Generated OpenAPI Schema using one or more filter. Filters can run during build time, or at runtime.

```java
/**
 * Filter to add custom elements
 */
@OpenApiFilter(stages = {OpenApiFilter.RunStage.BUILD, OpenApiFilter.RunStage.RUNTIME_PER_REQUEST}) // <1>
public class MyMultiStageFilter implements OASFilter { // <2>

    private IndexView view;

    public MyMultiStageFilter(IndexView view) { // <3>
        this.view = view;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) { // <4>
        Collection<ClassInfo> knownClasses = this.view.getKnownClasses();
        Info info = OASFactory.createInfo();
        info.setDescription("Created from Annotated Buildtime filter with " + knownClasses.size() + " known indexed classes");
        openAPI.setInfo(info);
    }

}
```

1. Annotate the class with `@OpenApiFilter` and use the `stages` field with one or more of `BUILD`, `RUNTIME_STARTUP`, or `RUNTIME_PER_REQUEST`. This filter runs once during build, and once every time the OpenAPI document is requested.
2. Implement OASFilter and override any of its methods
3. Filters which run during the build of the application - i.e. `BUILD` Stage filters (See also [\[Advanced interactions of RunStages\]](<#Advanced interactions of RunStages>)) - the Jandex index can be passed in the constructor. During application runtime, the Jandex index will **not** be null. However it will be empty, i.e it won't contain info about any classes or annotations.
4. Get a hold of the generated `OpenAPI` Schema, and enhance as required

Remember that setting fields on the schema will override what has been generated, you might want to get and add to (so modify). Also know that the generated values might be null, so you will have to check for that.

### Overview of RunStages

| Stage | Meaning |
| --- | --- |
| BUILD | The filter executes at build time. |
| RUNTIME_STARTUP | The filter executes (eagerly) at application startup. |
| RUNTIME_PER_REQUEST | The filter executes each time the OpenAPI document is requested. |
| RUN | DEPRECATED. The filter executes at stage `RUNTIME_STARTUP` if `quarkus.smallrye-openapi.always-run-filter` is set to `false`, or at stage `RUNTIME_PER_REQUEST` if `quarkus.smallrye-openapi.always-run-filter` is set to `true`. (See also [\[Advanced interactions of RunStages\]](<#Advanced interactions of RunStages>).) |
| BOTH | DEPRECATED. The filter executes as if both the `BUILD` and `RUN` stages were specified. |

> [!NOTE]
> The `quarkus.smallrye-openapi.always-run-filter` configuration property is deprecated. Annotate your filters with `@OpenApiFilter(stages = OpenApiFilter.RunStage.RUNTIME_PER_REQUEST)` instead.

### Advanced interactions of RunStages

While the `BUILD` RunStage is expected to run at build time - as the name already suggests - there are additional RunStages that also execute at build time.

The `quarkus-smallrye-openapi` extension saves the OpenAPI Document to a file when the `quarkus.smallrye-openapi.store-schema-directory` is configured. This saved version of the OpenAPI Document is useful for downstream consumers.

To produce an OpenAPI Document which possibly **could** match the one produced during runtime, the `RUN` and `RUNTIME_STARTUP` RunStages are executed at build time - in a separate step from the `BUILD` RunStage. Any exception which occurs during this additional processing step is ignored, i.e. if one of the filters throws, none will have any effect.

This additional processing step is done even if `quarkus.smallrye-openapi.store-schema-directory` is **NOT** configured.

### The MicroProfile OpenAPI way

The Microprofile OpenAPI specification defines that **one** OASFilter implementation can be specified using the `mp.openapi.filter` configuration property. This filter will be run during RunStage `RUN`.

## Loading OpenAPI Schema From Static Files

Instead of dynamically creating OpenAPI schemas from annotation scanning, Quarkus also supports serving static OpenAPI documents. The static file to serve must be a valid document conforming to the [OpenAPI specification](https://swagger.io/docs/specification). An OpenAPI document that conforms to the  OpenAPI Specification is itself a valid JSON object, that can be represented in `yaml` or `json` formats.

To see this in action, we'll put OpenAPI documentation under `META-INF/openapi.yaml` for our `/fruits` endpoints. Quarkus also supports alternative [OpenAPI document paths](#open-document-paths) if you prefer.

```yaml
openapi: 3.1.0
info:
  title: Static OpenAPI document of fruits resource
  description: Fruit resources Open API documentation
  version: "1.0"

servers:
  - url: http://localhost:8080/q/openapi
    description: Optional dev mode server description

paths:
  /fruits:
    get:
      responses:
        200:
          description: OK - fruits list
          content:
            application/json: {}
    post:
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/Fruit'
      responses:
        200:
          description: new fruit resource created
          content:
            application/json: {}
    delete:
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/Fruit'
      responses:
        200:
          description: OK - fruit resource deleted
          content:
            application/json: {}
components:
  schemas:
    Fruit:
      properties:
        description:
          type: string
        name:
          type: string
```

By default, a request to `/q/openapi` will serve the combined OpenAPI document from the static file and the model generated from application endpoints code. We can however change this to only serve the static OpenAPI document by adding `mp.openapi.scan.disable=true` configuration into `application.properties`.

Now, a request to `/q/openapi` endpoint will serve the static OpenAPI document instead of the generated one.

<a id="open-document-paths"></a>

> [!TIP]
> **About OpenAPI document paths**
>
> Quarkus supports various paths to store your OpenAPI document under. We recommend you place it under `META-INF/openapi.yml`. Alternative paths are:
>
> - `META-INF/openapi.yaml`
> - `META-INF/openapi.yml`
> - `META-INF/openapi.json`
> - `WEB-INF/classes/META-INF/openapi.yml`
> - `WEB-INF/classes/META-INF/openapi.yaml`
> - `WEB-INF/classes/META-INF/openapi.json`
>
> Live reload of static OpenAPI document is supported during development. A modification to your OpenAPI document will be picked up on fly by Quarkus.

## Changing the OpenAPI version

By default, when the document is generated, the OpenAPI version used will be `3.1.0`. If you use a static file as mentioned above, the version in the file will be used. You can also define the version in SmallRye using the following configuration:

```properties
quarkus.smallrye-openapi.open-api-version=3.0.4
```

This might be useful if your API goes through a Gateway that needs a certain version.

> [!NOTE]
> Changing the OpenAPI version between `3.0.x` and `3.1.x` versions will result in changes to the rendered document to satisfy the requirements of the chosen version. A good starting point to learn about the differences between OpenAPI 3.0 and 3.1 is the [OpenAPI Initiative](https://www.openapis.org/blog/2021/02/16/migrating-from-openapi-3-0-to-3-1-0).

## Auto-generation of Operation Id

The [Operation Id](https://swagger.io/docs/specification/paths-and-operations/) can be set using the `@Operation` annotation, and is in many cases useful when using a tool to generate a client stub from the schema. The Operation Id is typically used for the method name in the client stub. In SmallRye, you can auto-generate this Operation Id by using the following configuration:

```properties
quarkus.smallrye-openapi.operation-id-strategy=METHOD
```

Now you do not need to use the `@Operation` annotation. While generating the document, the method name will be used for the Operation Id.

**The strategies available for generating the Operation Id**

| Property value | Description |
| --- | --- |
| `METHOD` | Use the method name. |
| `CLASS_METHOD` | Use the class name (without the package) plus the method. |
| `PACKAGE_CLASS_METHOD` | Use the class name plus the method name. |

<a id="generating-multiple-openapi-documents"></a>
## Generating multiple OpenAPI documents

Multiple OpenAPI documents can be generated. Each document has its own distinct configuration, and can be configured to use a different set of REST resources.

For example, suppose you have resource methods to retrieve users and orders in the application.

```java
@Path("/api")
public class MyResource {
    @Path("/users")
    @GET
    public List<UserDTO> listUsers() {
        // ...
    }

    @Path("/orders")
    @GET
    public List<OrderDTO> listOrders() {
        // ...
    }
}
```

Normally these two resource methods would end up being documented alongside each other in the same OpenAPI document. These can be visually separated in the same OpenAPI document using tags. For larger deployments with multiple resources, however, this will still lead to:

- A possibly unusable OpenAPI document, as the documentation grows in size with each added resource method
- Potentially increased size or complexity for automatically generated API clients

Therefore, resource methods can be separated into distinct OpenAPI documents. Only a few annotations and a bit of configuration are needed.

The general idea is to group the resource methods into distinct profiles. This is achieved using an `@Extension(name = "x-smallrye-profile-<profile-name>", value = "")` annotation. The `x-smallrye-profile-` part is a prefix, and `<profile-name>` is a name for the profile which is freely definable.

The resource with added extensions now looks like this:

```java
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;

@Path("/api")
public class MyResource {
    @Path("/users")
    @GET
    @Extension(name = "x-smallrye-profile-user", value = "")
    public List<UserDTO> listUsers() {
        // ...
    }

    @Path("/orders")
    @GET
    @Extension(name = "x-smallrye-profile-order", value = "")
    public List<OrderDTO> listOrders() {
        // ...
    }
}
```

In the application.properties file, add the following build time configuration.

```properties
quarkus.smallrye-openapi.info-title=Combined documentation
quarkus.smallrye-openapi.user.scan-profiles=user <1>
quarkus.smallrye-openapi.order.scan-profiles=order
```

1. Note that in this example, both the profile name and the document name match. They are, however, both freely definable.

This will trigger Quarkus to create 3 OpenAPI documents.

- One where all operations are present. By default, served under `/q/openapi`
- One where only the user operations are present. By default, served under `/q/openapi-user`
- One where only the order operations are present. By default, served under `/q/openapi-order`

Exclusions are also possible using `quarkus.smallrye-openapi.<document-name>.scan-exclude-profiles`.

The `@Extension` annotation can be placed on resource classes and resource methods. However, the list of extensions defined at both places will **not** be combined. The effective extensions of an OpenAPI operation are either the list of extensions from the resource method, or if none are present the list of extensions from the resource class.

Multiple profiles can be specified for the `scan-profiles` and `scan-exclude-profiles` configuration properties. Multiple profiles can be specified on resource classes and resource methods by using multiple `@Extension` annotations. The `x-smallrye-profile` extensions are **not** included in the final OpenAPI documents.

### Filtering of operations

As shown in the previous section, a profile for an operation can be defined using an `@Extension`. Operations are filtered into documents based on their profiles:

- Operations without any profile are always **included**.
- If `scan-exclude-profiles` is configured: operations are **excluded** if any of their profiles match.
- If `scan-profiles` is configured and `scan-exclude-profiles` is **not** configured: operations are **included** only if at least one profile matches the `scan-profiles`.
- If both `scan-exclude-profiles` and `scan-profiles` are configured, **only** `scan-exclude-profiles` is checked.
- If neither is configured: all operations are **included**.

### Cleanup

The generated OpenAPI documents by default include every schema, even those referenced by operations which are excluded. The following configuration removes any unused schema.

```properties
mp.openapi.extensions.smallrye.remove-unused-schemas.enable=true
```

### Configuration applicability

The default document and all of the named documents support the same configuration. There is a minor pitfall with the MicroProfile OpenAPI configuration though - MicroProfile-specific configuration properties apply to all documents; see the following examples.

```properties
quarkus.smallrye-openapi.servers=https://api.example.com
```

Config properties which are set for the default document are applied only for the default document, i.e. the OpenAPI document served at `/q/openapi`.

```properties
quarkus.smallrye-openapi.<document-name>.servers=https://api.example.com
```

Config properties which are set for a specific document are applied only for this specific document, i.e. the OpenAPI document served at `/q/openapi-<document-name>`.

```properties
mp.openapi.servers=https://api.example.com
```

MicroProfile OpenAPI config properties are applied to **ALL** OpenAPI documents.

### Filters

By default, OpenAPI filters apply to all documents. This can be changed by configuring `@OpenApiFilter#documentNames`.

```java
import io.quarkus.smallrye.openapi.OpenApiFilter;
import org.eclipse.microprofile.openapi.OASFilter;

@OpenApiFilter(documentNames = {OpenApiFilter.DEFAULT_DOCUMENT_NAME, "user"}) <1>
public class MyFilter implements OASFilter {
    // ...
}
```

1. This filter applies only to the default OpenAPI document, and to the document named `user`.

<a id="dev-mode"></a>
## Use Swagger UI for development

When building APIs, developers want to test them quickly. [Swagger UI](https://swagger.io/tools/swagger-ui/) is a great tool permitting to visualize and interact with your APIs. The UI is automatically generated from your OpenAPI specification.

The Quarkus `smallrye-openapi` extension comes with a `swagger-ui` extension embedding a properly configured Swagger UI page.

> [!NOTE]
> By default, Swagger UI is only available when Quarkus is started in dev or test mode.
>
> If you want to make it available in production too, you can include the following configuration in your `application.properties`:
>
> ```properties
> quarkus.swagger-ui.always-include=true
> ```
>
> This is a build time property, it cannot be changed at runtime after your application is built.

By default, Swagger UI is accessible at `/q/swagger-ui`.

You can update the `/swagger-ui` sub path by setting the `quarkus.swagger-ui.path` property in your `application.properties`:

```properties
quarkus.swagger-ui.path=my-custom-path
```

> [!WARNING]
> The value `/` is not allowed as it blocks the application from serving anything else. A value prefixed with '/' makes it absolute and not relative.

Now, we are ready to run our application:

```bash
./mvnw quarkus:dev
```

You can check the Swagger UI path in your application's log:

```
00:00:00,000 INFO  [io.qua.swa.run.SwaggerUiServletExtension] Swagger UI available at /q/swagger-ui
```

Once your application is started, you can go to <http://localhost:8080/q/swagger-ui> and play with your API.

You can visualize your API's operations and schemas. ![Visualize your API](/guides/images/openapi-swaggerui-guide-screenshot01.png)

You can also interact with your API in order to quickly test it. ![Interact with your API](/guides/images/openapi-swaggerui-guide-screenshot02.png)

Hit `CTRL+C` to stop the application.

### Styling

You can style the swagger ui by supplying your own logo and css.

#### Logo

To supply your own logo, you need to place a file called `logo.png` in `src/main/resources/META-INF/branding`.

This will set the logo for all UIs (not just swagger ui), so in this case also GraphQL-UI and Health-UI (if included).

If you only want to apply this logo to swagger-ui (and not globally to all UIs) call the file `smallrye-open-api-ui.png` rather than `logo.png`.

#### CSS

To supply your own css that override/enhance style in the html, you need to place a file called `style.css` in `src/main/resources/META-INF/branding`.

This will add that css to all UIs (not just swagger ui), so in this case also GraphQL-UI and Health-UI (if included).

If you only want to apply this style to swagger-ui (and not globally to all UIs) call the file `smallrye-open-api-ui.css` rather than `style.css`.

For more information on styling, read this blog entry: <https://quarkus.io/blog/stylish-api/>

### Cross Origin Resource Sharing

If you plan to consume this application from a Single Page Application running on a different domain, you will need to configure CORS (Cross-Origin Resource Sharing). Please read the [CORS filter](/guides/security-cors/index.md#cors-filter) section of the "Cross-origin resource sharing" guide for more details.

### Multiple OpenAPI Documents

Multiple OpenAPI documents will automatically show when they are generated by Quarkus as described in [Generating multiple OpenAPI documents](#generating-multiple-openapi-documents). By default, Swagger UI renders a dropdown with entries named after their document name. Assuming a default document, an "order" document, and a "user" document are defined, the following entries will be shown:

- \<default>
- order
- user

You may want to change this to more user-friendly names, based on your requirements. For example, changing the options in the dropdown to "Intro", "Order Service", "User Service" would look like this:

```properties
quarkus.swagger-ui.urls."Combined"=/q/openapi
quarkus.swagger-ui.urls."Order Service"=/q/openapi-order
quarkus.swagger-ui.urls."User Service"=/q/openapi-user
```

## Configuration Reference

### MicroProfile OpenAPI

MicroProfile OpenAPI Core configuration is defined in [MicroProfile OpenAPI Specification](https://download.eclipse.org/microprofile/microprofile-open-api-3.1.1/microprofile-openapi-spec-3.1.1.html#_core_configurations). More information about the MicroProfile OpenAPI annotations can be found in the [MicroProfile OpenAPI API Javadoc](https://download.eclipse.org/microprofile/microprofile-open-api-3.1.1/apidocs/org/eclipse/microprofile/openapi/annotations/package-summary.html).

### OpenAPI

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-management-enabled"></a> [`quarkus.smallrye-openapi.management.enabled`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-management-enabled) config_property_copy_button:+++quarkus.smallrye-openapi.management.enabled+++[]<br><br>If management interface is turned on the openapi schema document will be published under the management interface. This allows you to exclude OpenAPI from management by setting the value to false<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_MANAGEMENT_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-js-client-enabled"></a> [`quarkus.smallrye-openapi.js-client.enabled`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-js-client-enabled) config_property_copy_button:+++quarkus.smallrye-openapi.js-client.enabled+++[]<br><br>Generate a JavaScript client proxy for all REST operations discovered by OpenAPI. When enabled, a static client library and a typed proxy module are generated as web resources, along with an import map for bare module imports.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_JS_CLIENT_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-enabled"></a> [`quarkus.smallrye-openapi.enabled`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-enabled) config_property_copy_button:+++quarkus.smallrye-openapi.enabled+++[]<br><br>Enable the openapi endpoint. By default it's enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-path"></a> [`quarkus.smallrye-openapi.path`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-path) config_property_copy_button:+++quarkus.smallrye-openapi.path+++[]<br><br>`quarkus.smallrye-openapi."document-name".path` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".path+++[]<br><br>The path at which to register the OpenAPI Servlet.<br><br>For the default document, the default value is simply `openapi`. For any named OpenAPI documents, the `-<document-name>` part will be replaced by the document name.<br><br>If you set a non-default value, then your custom value is used without resolving `-<document-name>`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_PATH+++[] | string | `openapi-<document-name>` |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-store-schema-directory"></a> [`quarkus.smallrye-openapi.store-schema-directory`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-store-schema-directory) config_property_copy_button:+++quarkus.smallrye-openapi.store-schema-directory+++[]<br><br>`quarkus.smallrye-openapi."document-name".store-schema-directory` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".store-schema-directory+++[]<br><br>If set, the generated OpenAPI schema documents will be stored here on build. Both openapi.json and openapi.yaml will be stored here if this is set.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_STORE_SCHEMA_DIRECTORY+++[] | path |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-store-schema-file-name"></a> [`quarkus.smallrye-openapi.store-schema-file-name`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-store-schema-file-name) config_property_copy_button:+++quarkus.smallrye-openapi.store-schema-file-name+++[]<br><br>`quarkus.smallrye-openapi."document-name".store-schema-file-name` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".store-schema-file-name+++[]<br><br>The name of the file in case it is being stored.<br><br>For the default document, the default value is simply `openapi`. For any named OpenAPI documents, the `-<document-name>` part will be replaced by the document name.<br><br>If you set a non-default value, then your custom value is used without resolving `-<document-name>`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_STORE_SCHEMA_FILE_NAME+++[] | string | `openapi-<document-name>` |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-ignore-static-document"></a> [`quarkus.smallrye-openapi.ignore-static-document`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-ignore-static-document) config_property_copy_button:+++quarkus.smallrye-openapi.ignore-static-document+++[]<br><br>`quarkus.smallrye-openapi."document-name".ignore-static-document` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".ignore-static-document+++[]<br><br>Do not include the provided static openapi document (eg. META-INF/openapi.yaml)<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_IGNORE_STATIC_DOCUMENT+++[] | boolean | `false` |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-additional-docs-directory"></a> [`quarkus.smallrye-openapi.additional-docs-directory`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-additional-docs-directory) config_property_copy_button:+++quarkus.smallrye-openapi.additional-docs-directory+++[]<br><br>`quarkus.smallrye-openapi."document-name".additional-docs-directory` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".additional-docs-directory+++[]<br><br>A list of local directories that should be scanned for yaml and/or json files to be included in the static model. Example: `META-INF/openapi/`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_ADDITIONAL_DOCS_DIRECTORY+++[] | list of path |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-security-scheme"></a> [`quarkus.smallrye-openapi.security-scheme`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-security-scheme) config_property_copy_button:+++quarkus.smallrye-openapi.security-scheme+++[]<br><br>`quarkus.smallrye-openapi."document-name".security-scheme` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".security-scheme+++[]<br><br>Add a certain SecurityScheme with config<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_SECURITY_SCHEME+++[] | `api-key`, `basic`, `jwt`, `oauth2`, `oidc`, `oauth2-implicit` |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-security-scheme-name"></a> [`quarkus.smallrye-openapi.security-scheme-name`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-security-scheme-name) config_property_copy_button:+++quarkus.smallrye-openapi.security-scheme-name+++[]<br><br>`quarkus.smallrye-openapi."document-name".security-scheme-name` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".security-scheme-name+++[]<br><br>Add a Security Scheme name to the generated OpenAPI document<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_SECURITY_SCHEME_NAME+++[] | string | `SecurityScheme` |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-security-scheme-description"></a> [`quarkus.smallrye-openapi.security-scheme-description`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-security-scheme-description) config_property_copy_button:+++quarkus.smallrye-openapi.security-scheme-description+++[]<br><br>`quarkus.smallrye-openapi."document-name".security-scheme-description` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".security-scheme-description+++[]<br><br>Add a description to the Security Scheme<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_SECURITY_SCHEME_DESCRIPTION+++[] | string | `Authentication` |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-security-scheme-extensions-extension-name"></a> [`quarkus.smallrye-openapi.security-scheme-extensions."extension-name"`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-security-scheme-extensions-extension-name) config_property_copy_button:+++quarkus.smallrye-openapi.security-scheme-extensions."extension-name"+++[]<br><br>`quarkus.smallrye-openapi."document-name".security-scheme-extensions."extension-name"` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".security-scheme-extensions."extension-name"+++[]<br><br>Add one or more extensions to the security scheme<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_SECURITY_SCHEME_EXTENSIONS__EXTENSION_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-auto-add-security-requirement"></a> [`quarkus.smallrye-openapi.auto-add-security-requirement`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-auto-add-security-requirement) config_property_copy_button:+++quarkus.smallrye-openapi.auto-add-security-requirement+++[]<br><br>`quarkus.smallrye-openapi."document-name".auto-add-security-requirement` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".auto-add-security-requirement+++[]<br><br>This will automatically add the security requirement to all methods/classes that has a `RolesAllowed` annotation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_AUTO_ADD_SECURITY_REQUIREMENT+++[] | boolean | `true` |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-auto-add-tags"></a> [`quarkus.smallrye-openapi.auto-add-tags`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-auto-add-tags) config_property_copy_button:+++quarkus.smallrye-openapi.auto-add-tags+++[]<br><br>`quarkus.smallrye-openapi."document-name".auto-add-tags` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".auto-add-tags+++[]<br><br>This will automatically add tags to operations based on the Java class name.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_AUTO_ADD_TAGS+++[] | boolean | `true` |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-auto-add-bad-request-response"></a> [`quarkus.smallrye-openapi.auto-add-bad-request-response`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-auto-add-bad-request-response) config_property_copy_button:+++quarkus.smallrye-openapi.auto-add-bad-request-response+++[]<br><br>`quarkus.smallrye-openapi."document-name".auto-add-bad-request-response` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".auto-add-bad-request-response+++[]<br><br>This will automatically add Bad Request (400 HTTP response) API response to operations with an input.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_AUTO_ADD_BAD_REQUEST_RESPONSE+++[] | boolean | `true` |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-auto-add-operation-summary"></a> [`quarkus.smallrye-openapi.auto-add-operation-summary`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-auto-add-operation-summary) config_property_copy_button:+++quarkus.smallrye-openapi.auto-add-operation-summary+++[]<br><br>`quarkus.smallrye-openapi."document-name".auto-add-operation-summary` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".auto-add-operation-summary+++[]<br><br>This will automatically add a summary to operations based on the Java method name.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_AUTO_ADD_OPERATION_SUMMARY+++[] | boolean | `true` |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-auto-add-server"></a> [`quarkus.smallrye-openapi.auto-add-server`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-auto-add-server) config_property_copy_button:+++quarkus.smallrye-openapi.auto-add-server+++[]<br><br>`quarkus.smallrye-openapi."document-name".auto-add-server` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".auto-add-server+++[]<br><br>Setting it to `true` will automatically add a default server to the schema if none is provided, using the current running server host and port.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_AUTO_ADD_SERVER+++[] | boolean |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-auto-add-security"></a> [`quarkus.smallrye-openapi.auto-add-security`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-auto-add-security) config_property_copy_button:+++quarkus.smallrye-openapi.auto-add-security+++[]<br><br>`quarkus.smallrye-openapi."document-name".auto-add-security` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".auto-add-security+++[]<br><br>This will automatically add security based on the security extension included (if any).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_AUTO_ADD_SECURITY+++[] | boolean | `true` |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-auto-add-open-api-endpoint"></a> [`quarkus.smallrye-openapi.auto-add-open-api-endpoint`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-auto-add-open-api-endpoint) config_property_copy_button:+++quarkus.smallrye-openapi.auto-add-open-api-endpoint+++[]<br><br>`quarkus.smallrye-openapi."document-name".auto-add-open-api-endpoint` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".auto-add-open-api-endpoint+++[]<br><br>This will automatically add the OpenAPI specification document endpoint to the schema. It also adds "openapi" to the list of tags and specify an "operationId"<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_AUTO_ADD_OPEN_API_ENDPOINT+++[] | boolean | `false` |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-api-key-parameter-in"></a> [`quarkus.smallrye-openapi.api-key-parameter-in`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-api-key-parameter-in) config_property_copy_button:+++quarkus.smallrye-openapi.api-key-parameter-in+++[]<br><br>`quarkus.smallrye-openapi."document-name".api-key-parameter-in` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".api-key-parameter-in+++[]<br><br>Required when using `apiKey` security. The location of the API key. Valid values are "query", "header" or "cookie".<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_API_KEY_PARAMETER_IN+++[] | string |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-api-key-parameter-name"></a> [`quarkus.smallrye-openapi.api-key-parameter-name`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-api-key-parameter-name) config_property_copy_button:+++quarkus.smallrye-openapi.api-key-parameter-name+++[]<br><br>`quarkus.smallrye-openapi."document-name".api-key-parameter-name` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".api-key-parameter-name+++[]<br><br>Required when using `apiKey` security. The name of the header, query or cookie parameter to be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_API_KEY_PARAMETER_NAME+++[] | string |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-basic-security-scheme-value"></a> [`quarkus.smallrye-openapi.basic-security-scheme-value`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-basic-security-scheme-value) config_property_copy_button:+++quarkus.smallrye-openapi.basic-security-scheme-value+++[]<br><br>`quarkus.smallrye-openapi."document-name".basic-security-scheme-value` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".basic-security-scheme-value+++[]<br><br>Add a scheme value to the Basic HTTP Security Scheme<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_BASIC_SECURITY_SCHEME_VALUE+++[] | string | `basic` |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-jwt-security-scheme-value"></a> [`quarkus.smallrye-openapi.jwt-security-scheme-value`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-jwt-security-scheme-value) config_property_copy_button:+++quarkus.smallrye-openapi.jwt-security-scheme-value+++[]<br><br>`quarkus.smallrye-openapi."document-name".jwt-security-scheme-value` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".jwt-security-scheme-value+++[]<br><br>Add a scheme value to the JWT Security Scheme<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_JWT_SECURITY_SCHEME_VALUE+++[] | string | `bearer` |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-jwt-bearer-format"></a> [`quarkus.smallrye-openapi.jwt-bearer-format`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-jwt-bearer-format) config_property_copy_button:+++quarkus.smallrye-openapi.jwt-bearer-format+++[]<br><br>`quarkus.smallrye-openapi."document-name".jwt-bearer-format` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".jwt-bearer-format+++[]<br><br>Add a bearer format the JWT Security Scheme<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_JWT_BEARER_FORMAT+++[] | string | `JWT` |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-oauth2-security-scheme-value"></a> [`quarkus.smallrye-openapi.oauth2-security-scheme-value`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-oauth2-security-scheme-value) config_property_copy_button:+++quarkus.smallrye-openapi.oauth2-security-scheme-value+++[]<br><br>`quarkus.smallrye-openapi."document-name".oauth2-security-scheme-value` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".oauth2-security-scheme-value+++[]<br><br>Add a scheme value to the OAuth2 opaque token Security Scheme<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_OAUTH2_SECURITY_SCHEME_VALUE+++[] | string | `bearer` |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-oauth2-bearer-format"></a> [`quarkus.smallrye-openapi.oauth2-bearer-format`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-oauth2-bearer-format) config_property_copy_button:+++quarkus.smallrye-openapi.oauth2-bearer-format+++[]<br><br>`quarkus.smallrye-openapi."document-name".oauth2-bearer-format` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".oauth2-bearer-format+++[]<br><br>Add a scheme value to OAuth2 opaque token Security Scheme<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_OAUTH2_BEARER_FORMAT+++[] | string | `Opaque` |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-oidc-open-id-connect-url"></a> [`quarkus.smallrye-openapi.oidc-open-id-connect-url`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-oidc-open-id-connect-url) config_property_copy_button:+++quarkus.smallrye-openapi.oidc-open-id-connect-url+++[]<br><br>`quarkus.smallrye-openapi."document-name".oidc-open-id-connect-url` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".oidc-open-id-connect-url+++[]<br><br>Add a openIdConnectUrl value to the OIDC Security Scheme<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_OIDC_OPEN_ID_CONNECT_URL+++[] | string |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-oauth2-implicit-refresh-url"></a> [`quarkus.smallrye-openapi.oauth2-implicit-refresh-url`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-oauth2-implicit-refresh-url) config_property_copy_button:+++quarkus.smallrye-openapi.oauth2-implicit-refresh-url+++[]<br><br>`quarkus.smallrye-openapi."document-name".oauth2-implicit-refresh-url` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".oauth2-implicit-refresh-url+++[]<br><br>Add a implicit flow refreshUrl value to the OAuth2 Security Scheme<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_OAUTH2_IMPLICIT_REFRESH_URL+++[] | string |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-oauth2-implicit-authorization-url"></a> [`quarkus.smallrye-openapi.oauth2-implicit-authorization-url`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-oauth2-implicit-authorization-url) config_property_copy_button:+++quarkus.smallrye-openapi.oauth2-implicit-authorization-url+++[]<br><br>`quarkus.smallrye-openapi."document-name".oauth2-implicit-authorization-url` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".oauth2-implicit-authorization-url+++[]<br><br>Add an implicit flow authorizationUrl value to the OAuth2 Security Scheme<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_OAUTH2_IMPLICIT_AUTHORIZATION_URL+++[] | string |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-oauth2-implicit-token-url"></a> [`quarkus.smallrye-openapi.oauth2-implicit-token-url`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-oauth2-implicit-token-url) config_property_copy_button:+++quarkus.smallrye-openapi.oauth2-implicit-token-url+++[]<br><br>`quarkus.smallrye-openapi."document-name".oauth2-implicit-token-url` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".oauth2-implicit-token-url+++[]<br><br>Add an implicit flow tokenUrl value to the OAuth2 Security Scheme<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_OAUTH2_IMPLICIT_TOKEN_URL+++[] | string |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-open-api-version"></a> [`quarkus.smallrye-openapi.open-api-version`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-open-api-version) config_property_copy_button:+++quarkus.smallrye-openapi.open-api-version+++[]<br><br>`quarkus.smallrye-openapi."document-name".open-api-version` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".open-api-version+++[]<br><br>Override the openapi version in the Schema document<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_OPEN_API_VERSION+++[] | string |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-info-title"></a> [`quarkus.smallrye-openapi.info-title`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-info-title) config_property_copy_button:+++quarkus.smallrye-openapi.info-title+++[]<br><br>`quarkus.smallrye-openapi."document-name".info-title` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".info-title+++[]<br><br>Set the title in Info tag in the Schema document<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_INFO_TITLE+++[] | string |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-info-version"></a> [`quarkus.smallrye-openapi.info-version`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-info-version) config_property_copy_button:+++quarkus.smallrye-openapi.info-version+++[]<br><br>`quarkus.smallrye-openapi."document-name".info-version` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".info-version+++[]<br><br>Set the version in Info tag in the Schema document<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_INFO_VERSION+++[] | string |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-info-description"></a> [`quarkus.smallrye-openapi.info-description`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-info-description) config_property_copy_button:+++quarkus.smallrye-openapi.info-description+++[]<br><br>`quarkus.smallrye-openapi."document-name".info-description` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".info-description+++[]<br><br>Set the description in Info tag in the Schema document<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_INFO_DESCRIPTION+++[] | string |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-info-terms-of-service"></a> [`quarkus.smallrye-openapi.info-terms-of-service`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-info-terms-of-service) config_property_copy_button:+++quarkus.smallrye-openapi.info-terms-of-service+++[]<br><br>`quarkus.smallrye-openapi."document-name".info-terms-of-service` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".info-terms-of-service+++[]<br><br>Set the terms of the service in Info tag in the Schema document<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_INFO_TERMS_OF_SERVICE+++[] | string |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-info-contact-email"></a> [`quarkus.smallrye-openapi.info-contact-email`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-info-contact-email) config_property_copy_button:+++quarkus.smallrye-openapi.info-contact-email+++[]<br><br>`quarkus.smallrye-openapi."document-name".info-contact-email` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".info-contact-email+++[]<br><br>Set the contact email in Info tag in the Schema document<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_INFO_CONTACT_EMAIL+++[] | string |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-info-contact-name"></a> [`quarkus.smallrye-openapi.info-contact-name`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-info-contact-name) config_property_copy_button:+++quarkus.smallrye-openapi.info-contact-name+++[]<br><br>`quarkus.smallrye-openapi."document-name".info-contact-name` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".info-contact-name+++[]<br><br>Set the contact name in Info tag in the Schema document<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_INFO_CONTACT_NAME+++[] | string |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-info-contact-url"></a> [`quarkus.smallrye-openapi.info-contact-url`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-info-contact-url) config_property_copy_button:+++quarkus.smallrye-openapi.info-contact-url+++[]<br><br>`quarkus.smallrye-openapi."document-name".info-contact-url` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".info-contact-url+++[]<br><br>Set the contact url in Info tag in the Schema document<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_INFO_CONTACT_URL+++[] | string |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-info-license-name"></a> [`quarkus.smallrye-openapi.info-license-name`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-info-license-name) config_property_copy_button:+++quarkus.smallrye-openapi.info-license-name+++[]<br><br>`quarkus.smallrye-openapi."document-name".info-license-name` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".info-license-name+++[]<br><br>Set the license name in Info tag in the Schema document<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_INFO_LICENSE_NAME+++[] | string |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-info-license-url"></a> [`quarkus.smallrye-openapi.info-license-url`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-info-license-url) config_property_copy_button:+++quarkus.smallrye-openapi.info-license-url+++[]<br><br>`quarkus.smallrye-openapi."document-name".info-license-url` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".info-license-url+++[]<br><br>Set the license url in Info tag in the Schema document<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_INFO_LICENSE_URL+++[] | string |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-operation-id-strategy"></a> [`quarkus.smallrye-openapi.operation-id-strategy`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-operation-id-strategy) config_property_copy_button:+++quarkus.smallrye-openapi.operation-id-strategy+++[]<br><br>`quarkus.smallrye-openapi."document-name".operation-id-strategy` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".operation-id-strategy+++[]<br><br>Set the strategy to automatically create an operation Id. The strategy may be one of the predefined values or the name of a fully-qualified class that implements the `io.smallrye.openapi.api.OperationIdGenerator` interface.<br><br>Predefined strategies:<br><br>\* `method`: generate an operationId with the resource method's name \* `class-method`: generate an operationId with the resource class's simple name and the resource method's name \* `package-class-method`: generate an operationId with the resource class's fully-qualified name and the resource method's name<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_OPERATION_ID_STRATEGY+++[] | string |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-merge-schema-examples"></a> [`quarkus.smallrye-openapi.merge-schema-examples`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-merge-schema-examples) config_property_copy_button:+++quarkus.smallrye-openapi.merge-schema-examples+++[]<br><br>`quarkus.smallrye-openapi."document-name".merge-schema-examples` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".merge-schema-examples+++[]<br><br>Set this boolean value to enable or disable the merging of the deprecated `@Schema` `example` property into the `examples` array introduced in OAS 3.1.0. If set to `false`, the deprecated `example` will be kept as a separate annotation on the schema in the OpenAPI model.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_MERGE_SCHEMA_EXAMPLES+++[] | boolean | `true` |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-scan-profiles"></a> [`quarkus.smallrye-openapi.scan-profiles`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-scan-profiles) config_property_copy_button:+++quarkus.smallrye-openapi.scan-profiles+++[]<br><br>`quarkus.smallrye-openapi."document-name".scan-profiles` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".scan-profiles+++[]<br><br>Profiles which explicitly include operations. If set, any operation without a matching profile is excluded.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_SCAN_PROFILES+++[] | list of string |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-scan-exclude-profiles"></a> [`quarkus.smallrye-openapi.scan-exclude-profiles`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-scan-exclude-profiles) config_property_copy_button:+++quarkus.smallrye-openapi.scan-exclude-profiles+++[]<br><br>`quarkus.smallrye-openapi."document-name".scan-exclude-profiles` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".scan-exclude-profiles+++[]<br><br>Profiles which explicitly exclude operations. If set, any operation without a matching profile is included. Excludes are considered before inclusions.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_SCAN_EXCLUDE_PROFILES+++[] | list of string |  |
| <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-servers"></a> [`quarkus.smallrye-openapi.servers`](#quarkus-smallrye-openapi_quarkus-smallrye-openapi-servers) config_property_copy_button:+++quarkus.smallrye-openapi.servers+++[]<br><br>`quarkus.smallrye-openapi."documents".servers` config_property_copy_button:+++quarkus.smallrye-openapi."documents".servers+++[]<br><br>Specify the list of servers that provide connectivity information<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_SERVERS+++[] | list of string |  |
| <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-enable"></a> <mark>link:#quarkus-smallrye-openapi_quarkus-smallrye-openapi-enable\[`quarkus.smallrye-openapi.enable`\]</mark> config_property_copy_button:+++quarkus.smallrye-openapi.enable+++[]<br><br>*This property is deprecated since `3.26`: use `quarkus.smallrye-openapi.enabled` instead.*<br><br>Enable the openapi endpoint. By default it's enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_ENABLE+++[] | boolean |  |
| [lock] <a id="quarkus-smallrye-openapi_quarkus-smallrye-openapi-always-run-filter"></a> <mark>link:#quarkus-smallrye-openapi_quarkus-smallrye-openapi-always-run-filter\[`quarkus.smallrye-openapi.always-run-filter`\]</mark> config_property_copy_button:+++quarkus.smallrye-openapi.always-run-filter+++[]<br><br>`quarkus.smallrye-openapi."document-name".always-run-filter` config_property_copy_button:+++quarkus.smallrye-openapi."document-name".always-run-filter+++[]<br><br>*This property is deprecated since `3.34`: Use `@OpenApiFilter(stages = RunStage.RUNTIME_PER_REQUEST)` instead to mark individual filters as per-request. This makes the OpenAPI document dynamic automatically without requiring this configuration property.*<br><br>Do not run the filter only at startup, but every time the document is requested (dynamic).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_OPENAPI_ALWAYS_RUN_FILTER+++[] | boolean | `false` |

### Swagger UI

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-path"></a> [`quarkus.swagger-ui.path`](#quarkus-swagger-ui_quarkus-swagger-ui-path) config_property_copy_button:+++quarkus.swagger-ui.path+++[]<br><br>The path where Swagger UI is available.<br><br>The value `/` is not allowed as it blocks the application from serving anything else. By default, this value will be resolved as a path relative to `${quarkus.http.non-application-root-path}`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_PATH+++[] | string | `swagger-ui` |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-always-include"></a> [`quarkus.swagger-ui.always-include`](#quarkus-swagger-ui_quarkus-swagger-ui-always-include) config_property_copy_button:+++quarkus.swagger-ui.always-include+++[]<br><br>If this should be included every time. By default, this is only included when the application is running in dev mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_ALWAYS_INCLUDE+++[] | boolean | `false` |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-urls-name"></a> [`quarkus.swagger-ui.urls."name"`](#quarkus-swagger-ui_quarkus-swagger-ui-urls-name) config_property_copy_button:+++quarkus.swagger-ui.urls."name"+++[]<br><br>The urls that will be included as options. By default, the OpenAPI path will be used. Here you can override that and supply multiple urls that will appear in the TopBar plugin.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_URLS__NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-urls-primary-name"></a> [`quarkus.swagger-ui.urls-primary-name`](#quarkus-swagger-ui_quarkus-swagger-ui-urls-primary-name) config_property_copy_button:+++quarkus.swagger-ui.urls-primary-name+++[]<br><br>If urls option is used, this will be the name of the default selection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_URLS_PRIMARY_NAME+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-title"></a> [`quarkus.swagger-ui.title`](#quarkus-swagger-ui_quarkus-swagger-ui-title) config_property_copy_button:+++quarkus.swagger-ui.title+++[]<br><br>The html title for the page.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_TITLE+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-theme"></a> [`quarkus.swagger-ui.theme`](#quarkus-swagger-ui_quarkus-swagger-ui-theme) config_property_copy_button:+++quarkus.swagger-ui.theme+++[]<br><br>Swagger UI theme to be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_THEME+++[] | `original`, `feeling-blue`, `flattop`, `material`, `monokai`, `muted`, `newspaper`, `outline` |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-footer"></a> [`quarkus.swagger-ui.footer`](#quarkus-swagger-ui_quarkus-swagger-ui-footer) config_property_copy_button:+++quarkus.swagger-ui.footer+++[]<br><br>A footer for the html page. Nothing by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_FOOTER+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-deep-linking"></a> [`quarkus.swagger-ui.deep-linking`](#quarkus-swagger-ui_quarkus-swagger-ui-deep-linking) config_property_copy_button:+++quarkus.swagger-ui.deep-linking+++[]<br><br>If set to true, enables deep linking for tags and operations.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_DEEP_LINKING+++[] | boolean |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-display-operation-id"></a> [`quarkus.swagger-ui.display-operation-id`](#quarkus-swagger-ui_quarkus-swagger-ui-display-operation-id) config_property_copy_button:+++quarkus.swagger-ui.display-operation-id+++[]<br><br>Controls the display of operationId in operations list. The default is false.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_DISPLAY_OPERATION_ID+++[] | boolean |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-default-models-expand-depth"></a> [`quarkus.swagger-ui.default-models-expand-depth`](#quarkus-swagger-ui_quarkus-swagger-ui-default-models-expand-depth) config_property_copy_button:+++quarkus.swagger-ui.default-models-expand-depth+++[]<br><br>The default expansion depth for models (set to -1 completely hide the models).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_DEFAULT_MODELS_EXPAND_DEPTH+++[] | int |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-default-model-expand-depth"></a> [`quarkus.swagger-ui.default-model-expand-depth`](#quarkus-swagger-ui_quarkus-swagger-ui-default-model-expand-depth) config_property_copy_button:+++quarkus.swagger-ui.default-model-expand-depth+++[]<br><br>The default expansion depth for the model on the model-example section.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_DEFAULT_MODEL_EXPAND_DEPTH+++[] | int |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-default-model-rendering"></a> [`quarkus.swagger-ui.default-model-rendering`](#quarkus-swagger-ui_quarkus-swagger-ui-default-model-rendering) config_property_copy_button:+++quarkus.swagger-ui.default-model-rendering+++[]<br><br>Controls how the model is shown when the API is first rendered.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_DEFAULT_MODEL_RENDERING+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-display-request-duration"></a> [`quarkus.swagger-ui.display-request-duration`](#quarkus-swagger-ui_quarkus-swagger-ui-display-request-duration) config_property_copy_button:+++quarkus.swagger-ui.display-request-duration+++[]<br><br>Controls the display of the request duration (in milliseconds) for "Try it out" requests.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_DISPLAY_REQUEST_DURATION+++[] | boolean |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-doc-expansion"></a> [`quarkus.swagger-ui.doc-expansion`](#quarkus-swagger-ui_quarkus-swagger-ui-doc-expansion) config_property_copy_button:+++quarkus.swagger-ui.doc-expansion+++[]<br><br>Controls the default expansion setting for the operations and tags.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_DOC_EXPANSION+++[] | `list`, `full`, `none` |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-filter"></a> [`quarkus.swagger-ui.filter`](#quarkus-swagger-ui_quarkus-swagger-ui-filter) config_property_copy_button:+++quarkus.swagger-ui.filter+++[]<br><br>If set, enables filtering. The top bar will show an edit box that you can use to filter the tagged operations that are shown. Can be Boolean to enable or disable, or a string, in which case filtering will be enabled using that string as the filter expression. Filtering is case-sensitive matching the filter expression anywhere inside the tag.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_FILTER+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-max-displayed-tags"></a> [`quarkus.swagger-ui.max-displayed-tags`](#quarkus-swagger-ui_quarkus-swagger-ui-max-displayed-tags) config_property_copy_button:+++quarkus.swagger-ui.max-displayed-tags+++[]<br><br>If set, limits the number of tagged operations displayed to at most this many. The default is to show all operations.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_MAX_DISPLAYED_TAGS+++[] | int |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-operations-sorter"></a> [`quarkus.swagger-ui.operations-sorter`](#quarkus-swagger-ui_quarkus-swagger-ui-operations-sorter) config_property_copy_button:+++quarkus.swagger-ui.operations-sorter+++[]<br><br>Apply a sort to the operation list of each API. It can be 'alpha' (sort by paths alphanumerically), 'method' (sort by HTTP method) or a function (see Array.prototype.sort() to know how sort function works). Default is the order returned by the server unchanged.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_OPERATIONS_SORTER+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-show-extensions"></a> [`quarkus.swagger-ui.show-extensions`](#quarkus-swagger-ui_quarkus-swagger-ui-show-extensions) config_property_copy_button:+++quarkus.swagger-ui.show-extensions+++[]<br><br>Controls the display of vendor extension (x-) fields and values for Operations, Parameters, and Schema.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_SHOW_EXTENSIONS+++[] | boolean |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-show-common-extensions"></a> [`quarkus.swagger-ui.show-common-extensions`](#quarkus-swagger-ui_quarkus-swagger-ui-show-common-extensions) config_property_copy_button:+++quarkus.swagger-ui.show-common-extensions+++[]<br><br>Controls the display of extensions (pattern, maxLength, minLength, maximum, minimum) fields and values for Parameters.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_SHOW_COMMON_EXTENSIONS+++[] | boolean |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-tags-sorter"></a> [`quarkus.swagger-ui.tags-sorter`](#quarkus-swagger-ui_quarkus-swagger-ui-tags-sorter) config_property_copy_button:+++quarkus.swagger-ui.tags-sorter+++[]<br><br>Apply a sort to the tag list of each API. It can be 'alpha' (sort by paths alphanumerically) or a function (see Array.prototype.sort() to learn how to write a sort function). Two tag name strings are passed to the sorter for each pass. Default is the order determined by Swagger UI.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_TAGS_SORTER+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-on-complete"></a> [`quarkus.swagger-ui.on-complete`](#quarkus-swagger-ui_quarkus-swagger-ui-on-complete) config_property_copy_button:+++quarkus.swagger-ui.on-complete+++[]<br><br>Provides a mechanism to be notified when Swagger UI has finished rendering a newly provided definition.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_ON_COMPLETE+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-syntax-highlight"></a> [`quarkus.swagger-ui.syntax-highlight`](#quarkus-swagger-ui_quarkus-swagger-ui-syntax-highlight) config_property_copy_button:+++quarkus.swagger-ui.syntax-highlight+++[]<br><br>Set to `false` to deactivate syntax highlighting of payloads and cURL command. Can be otherwise an object with the `activate` and `theme` properties.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_SYNTAX_HIGHLIGHT+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-oauth2-redirect-url"></a> [`quarkus.swagger-ui.oauth2-redirect-url`](#quarkus-swagger-ui_quarkus-swagger-ui-oauth2-redirect-url) config_property_copy_button:+++quarkus.swagger-ui.oauth2-redirect-url+++[]<br><br>OAuth redirect URL.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_OAUTH2_REDIRECT_URL+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-request-interceptor"></a> [`quarkus.swagger-ui.request-interceptor`](#quarkus-swagger-ui_quarkus-swagger-ui-request-interceptor) config_property_copy_button:+++quarkus.swagger-ui.request-interceptor+++[]<br><br>MUST be a function. Function to intercept remote definition, "Try it out", and OAuth 2.0 requests. Accepts one argument requestInterceptor(request) and must return the modified request, or a Promise that resolves to the modified request.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_REQUEST_INTERCEPTOR+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-request-curl-options"></a> [`quarkus.swagger-ui.request-curl-options`](#quarkus-swagger-ui_quarkus-swagger-ui-request-curl-options) config_property_copy_button:+++quarkus.swagger-ui.request-curl-options+++[]<br><br>If set, MUST be an array of command line options available to the curl command. This can be set on the mutated request in the requestInterceptor function.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_REQUEST_CURL_OPTIONS+++[] | list of string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-response-interceptor"></a> [`quarkus.swagger-ui.response-interceptor`](#quarkus-swagger-ui_quarkus-swagger-ui-response-interceptor) config_property_copy_button:+++quarkus.swagger-ui.response-interceptor+++[]<br><br>MUST be a function. Function to intercept remote definition, "Try it out", and OAuth 2.0 responses. Accepts one argument responseInterceptor(response) and must return the modified response, or a Promise that resolves to the modified response.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_RESPONSE_INTERCEPTOR+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-show-mutated-request"></a> [`quarkus.swagger-ui.show-mutated-request`](#quarkus-swagger-ui_quarkus-swagger-ui-show-mutated-request) config_property_copy_button:+++quarkus.swagger-ui.show-mutated-request+++[]<br><br>If set to true, uses the mutated request returned from a requestInterceptor to produce the curl command in the UI, otherwise the request before the requestInterceptor was applied is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_SHOW_MUTATED_REQUEST+++[] | boolean |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-supported-submit-methods"></a> [`quarkus.swagger-ui.supported-submit-methods`](#quarkus-swagger-ui_quarkus-swagger-ui-supported-submit-methods) config_property_copy_button:+++quarkus.swagger-ui.supported-submit-methods+++[]<br><br>List of HTTP methods that have the "Try it out" feature enabled. An empty array disables "Try it out" for all operations. This does not filter the operations from the display.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_SUPPORTED_SUBMIT_METHODS+++[] | list of `get`, `put`, `post`, `delete`, `options`, `head`, `patch`, `trace` |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-validator-url"></a> [`quarkus.swagger-ui.validator-url`](#quarkus-swagger-ui_quarkus-swagger-ui-validator-url) config_property_copy_button:+++quarkus.swagger-ui.validator-url+++[]<br><br>By default, Swagger UI attempts to validate specs against swagger.io's online validator. You can use this parameter to set a different validator URL, for example for locally deployed validators (Validator Badge). Setting it to either none, 127.0.0.1 or localhost will disable validation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_VALIDATOR_URL+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-with-credentials"></a> [`quarkus.swagger-ui.with-credentials`](#quarkus-swagger-ui_quarkus-swagger-ui-with-credentials) config_property_copy_button:+++quarkus.swagger-ui.with-credentials+++[]<br><br>If set to true, enables passing credentials, as defined in the Fetch standard, in CORS requests that are sent by the browser.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_WITH_CREDENTIALS+++[] | boolean |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-model-property-macro"></a> [`quarkus.swagger-ui.model-property-macro`](#quarkus-swagger-ui_quarkus-swagger-ui-model-property-macro) config_property_copy_button:+++quarkus.swagger-ui.model-property-macro+++[]<br><br>Function to set default values to each property in model. Accepts one argument modelPropertyMacro(property), property is immutable<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_MODEL_PROPERTY_MACRO+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-parameter-macro"></a> [`quarkus.swagger-ui.parameter-macro`](#quarkus-swagger-ui_quarkus-swagger-ui-parameter-macro) config_property_copy_button:+++quarkus.swagger-ui.parameter-macro+++[]<br><br>Function to set default value to parameters. Accepts two arguments parameterMacro(operation, parameter). Operation and parameter are objects passed for context, both remain immutable<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_PARAMETER_MACRO+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-persist-authorization"></a> [`quarkus.swagger-ui.persist-authorization`](#quarkus-swagger-ui_quarkus-swagger-ui-persist-authorization) config_property_copy_button:+++quarkus.swagger-ui.persist-authorization+++[]<br><br>If set to true, it persists authorization data and it would not be lost on browser close/refresh<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_PERSIST_AUTHORIZATION+++[] | boolean |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-layout"></a> [`quarkus.swagger-ui.layout`](#quarkus-swagger-ui_quarkus-swagger-ui-layout) config_property_copy_button:+++quarkus.swagger-ui.layout+++[]<br><br>The name of a component available via the plugin system to use as the top-level layout for Swagger UI.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_LAYOUT+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-plugins"></a> [`quarkus.swagger-ui.plugins`](#quarkus-swagger-ui_quarkus-swagger-ui-plugins) config_property_copy_button:+++quarkus.swagger-ui.plugins+++[]<br><br>A list of plugin functions to use in Swagger UI.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_PLUGINS+++[] | list of string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-scripts"></a> [`quarkus.swagger-ui.scripts`](#quarkus-swagger-ui_quarkus-swagger-ui-scripts) config_property_copy_button:+++quarkus.swagger-ui.scripts+++[]<br><br>A list of external scripts (usually plugins) to use in Swagger UI.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_SCRIPTS+++[] | list of string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-presets"></a> [`quarkus.swagger-ui.presets`](#quarkus-swagger-ui_quarkus-swagger-ui-presets) config_property_copy_button:+++quarkus.swagger-ui.presets+++[]<br><br>A list of presets to use in Swagger UI.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_PRESETS+++[] | list of string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-oauth-client-id"></a> [`quarkus.swagger-ui.oauth-client-id`](#quarkus-swagger-ui_quarkus-swagger-ui-oauth-client-id) config_property_copy_button:+++quarkus.swagger-ui.oauth-client-id+++[]<br><br>OAuth default clientId - Used in the initOAuth method.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_OAUTH_CLIENT_ID+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-oauth-client-secret"></a> [`quarkus.swagger-ui.oauth-client-secret`](#quarkus-swagger-ui_quarkus-swagger-ui-oauth-client-secret) config_property_copy_button:+++quarkus.swagger-ui.oauth-client-secret+++[]<br><br>OAuth default clientSecret - Used in the initOAuth method.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_OAUTH_CLIENT_SECRET+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-oauth-realm"></a> [`quarkus.swagger-ui.oauth-realm`](#quarkus-swagger-ui_quarkus-swagger-ui-oauth-realm) config_property_copy_button:+++quarkus.swagger-ui.oauth-realm+++[]<br><br>OAuth1 Realm query parameter added to authorizationUrl and tokenUrl - Used in the initOAuth method.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_OAUTH_REALM+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-oauth-app-name"></a> [`quarkus.swagger-ui.oauth-app-name`](#quarkus-swagger-ui_quarkus-swagger-ui-oauth-app-name) config_property_copy_button:+++quarkus.swagger-ui.oauth-app-name+++[]<br><br>OAuth application name, displayed in authorization popup - Used in the initOAuth method.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_OAUTH_APP_NAME+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-oauth-scope-separator"></a> [`quarkus.swagger-ui.oauth-scope-separator`](#quarkus-swagger-ui_quarkus-swagger-ui-oauth-scope-separator) config_property_copy_button:+++quarkus.swagger-ui.oauth-scope-separator+++[]<br><br>OAuth scope separator for passing scopes - Used in the initOAuth method.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_OAUTH_SCOPE_SEPARATOR+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-oauth-scopes"></a> [`quarkus.swagger-ui.oauth-scopes`](#quarkus-swagger-ui_quarkus-swagger-ui-oauth-scopes) config_property_copy_button:+++quarkus.swagger-ui.oauth-scopes+++[]<br><br>OAuth Scopes, separated using the oauthScopeSeparator - Used in the initOAuth method.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_OAUTH_SCOPES+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-oauth-additional-query-string-params"></a> [`quarkus.swagger-ui.oauth-additional-query-string-params`](#quarkus-swagger-ui_quarkus-swagger-ui-oauth-additional-query-string-params) config_property_copy_button:+++quarkus.swagger-ui.oauth-additional-query-string-params+++[]<br><br>OAuth additional query parameters added to authorizationUrl and tokenUrl - Used in the initOAuth method.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_OAUTH_ADDITIONAL_QUERY_STRING_PARAMS+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-oauth-use-basic-authentication-with-access-code-grant"></a> [`quarkus.swagger-ui.oauth-use-basic-authentication-with-access-code-grant`](#quarkus-swagger-ui_quarkus-swagger-ui-oauth-use-basic-authentication-with-access-code-grant) config_property_copy_button:+++quarkus.swagger-ui.oauth-use-basic-authentication-with-access-code-grant+++[]<br><br>OAuth only activated for the accessCode flow. During the authorization_code request to the tokenUrl, pass the Client Password using the HTTP Basic Authentication scheme - Used in the initOAuth method.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_OAUTH_USE_BASIC_AUTHENTICATION_WITH_ACCESS_CODE_GRANT+++[] | boolean |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-oauth-use-pkce-with-authorization-code-grant"></a> [`quarkus.swagger-ui.oauth-use-pkce-with-authorization-code-grant`](#quarkus-swagger-ui_quarkus-swagger-ui-oauth-use-pkce-with-authorization-code-grant) config_property_copy_button:+++quarkus.swagger-ui.oauth-use-pkce-with-authorization-code-grant+++[]<br><br>OAuth only applies to authorization code flows. Proof Key for Code Exchange brings enhanced security for OAuth public clients - Used in the initOAuth method.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_OAUTH_USE_PKCE_WITH_AUTHORIZATION_CODE_GRANT+++[] | boolean |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-preauthorize-basic-auth-definition-key"></a> [`quarkus.swagger-ui.preauthorize-basic-auth-definition-key`](#quarkus-swagger-ui_quarkus-swagger-ui-preauthorize-basic-auth-definition-key) config_property_copy_button:+++quarkus.swagger-ui.preauthorize-basic-auth-definition-key+++[]<br><br>Pre-authorize Basic Auth, programmatically set DefinitionKey for a Basic authorization scheme - Used in the preauthorizeBasic method.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_PREAUTHORIZE_BASIC_AUTH_DEFINITION_KEY+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-preauthorize-basic-username"></a> [`quarkus.swagger-ui.preauthorize-basic-username`](#quarkus-swagger-ui_quarkus-swagger-ui-preauthorize-basic-username) config_property_copy_button:+++quarkus.swagger-ui.preauthorize-basic-username+++[]<br><br>Pre-authorize Basic Auth, programmatically set Username for a Basic authorization scheme - Used in the preauthorizeBasic method.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_PREAUTHORIZE_BASIC_USERNAME+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-preauthorize-basic-password"></a> [`quarkus.swagger-ui.preauthorize-basic-password`](#quarkus-swagger-ui_quarkus-swagger-ui-preauthorize-basic-password) config_property_copy_button:+++quarkus.swagger-ui.preauthorize-basic-password+++[]<br><br>Pre-authorize Basic Auth, programmatically set Password for a Basic authorization scheme - Used in the preauthorizeBasic method.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_PREAUTHORIZE_BASIC_PASSWORD+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-preauthorize-api-key-auth-definition-key"></a> [`quarkus.swagger-ui.preauthorize-api-key-auth-definition-key`](#quarkus-swagger-ui_quarkus-swagger-ui-preauthorize-api-key-auth-definition-key) config_property_copy_button:+++quarkus.swagger-ui.preauthorize-api-key-auth-definition-key+++[]<br><br>Pre-authorize ApiKey Auth, programmatically set DefinitionKey for an API key or Bearer authorization scheme - Used in the preauthorizeApiKey method.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_PREAUTHORIZE_API_KEY_AUTH_DEFINITION_KEY+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-preauthorize-api-key-api-key-value"></a> [`quarkus.swagger-ui.preauthorize-api-key-api-key-value`](#quarkus-swagger-ui_quarkus-swagger-ui-preauthorize-api-key-api-key-value) config_property_copy_button:+++quarkus.swagger-ui.preauthorize-api-key-api-key-value+++[]<br><br>Pre-authorize ApiKey Auth, programmatically set ApiKeyValue for an API key or Bearer authorization scheme - Used in the preauthorizeApiKey method.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_PREAUTHORIZE_API_KEY_API_KEY_VALUE+++[] | string |  |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-query-config-enabled"></a> [`quarkus.swagger-ui.query-config-enabled`](#quarkus-swagger-ui_quarkus-swagger-ui-query-config-enabled) config_property_copy_button:+++quarkus.swagger-ui.query-config-enabled+++[]<br><br>If set to true, this allows the user to modify and test different query parameters in the API request<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_QUERY_CONFIG_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-swagger-ui_quarkus-swagger-ui-try-it-out-enabled"></a> [`quarkus.swagger-ui.try-it-out-enabled`](#quarkus-swagger-ui_quarkus-swagger-ui-try-it-out-enabled) config_property_copy_button:+++quarkus.swagger-ui.try-it-out-enabled+++[]<br><br>If try it out should be enabled by default<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_TRY_IT_OUT_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-swagger-ui_quarkus-swagger-ui-enabled"></a> [`quarkus.swagger-ui.enabled`](#quarkus-swagger-ui_quarkus-swagger-ui-enabled) config_property_copy_button:+++quarkus.swagger-ui.enabled+++[]<br><br>If Swagger UI is included, it should be enabled/disabled. By default, Swagger UI is enabled if it is included (see `always-include`).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-swagger-ui_quarkus-swagger-ui-enable"></a> <mark>link:#quarkus-swagger-ui_quarkus-swagger-ui-enable\[`quarkus.swagger-ui.enable`\]</mark> config_property_copy_button:+++quarkus.swagger-ui.enable+++[]<br><br>*This property is deprecated since `3.26`: use `quarkus.swagger-ui.enabled` instead.*<br><br>If Swagger UI is included, it should be enabled/disabled. By default, Swagger UI is enabled if it is included (see `always-include`).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SWAGGER_UI_ENABLE+++[] | boolean |  |
