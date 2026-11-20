# Google Cloud Functions (Serverless) with Quarkus REST, Undertow, or Reactive Routes

The `quarkus-google-cloud-functions-http` extension allows you to write microservices with Quarkus REST (Jakarta REST), Undertow (Servlet), Reactive Routes, or [Funqy HTTP](/guides/funqy-http/index.md), and make these microservices deployable to the Google Cloud Functions runtime.

One Google Cloud Functions deployment can represent any number of Jakarta REST, Servlet, Reactive Routes, or [Funqy HTTP](/guides/funqy-http/index.md) endpoints.

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

- [A Google Cloud Account](https://cloud.google.com/). Free accounts work.
- [Cloud SDK CLI Installed](https://cloud.google.com/sdk)

## Solution

This guide walks you through generating a sample project followed by creating three HTTP endpoints written with Jakarta REST APIs, Servlet APIs, Reactive Routes, or [Funqy HTTP](/guides/funqy-http/index.md) APIs. Once built, you will be able to deploy the project to Google Cloud.

If you don't want to follow all these steps, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `google-cloud-functions-http-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/google-cloud-functions-http-quickstart).

## Creating the Maven Deployment Project

Create an application with the `quarkus-google-cloud-functions-http` extension. You can use the following Maven command to create it:

**CLI**

```bash
quarkus create app org.acme:google-cloud-functions-http \
    --extension='google-cloud-functions-http,rest-jackson,undertow,reactive-routes,funqy-http' \
    --no-code
cd google-cloud-functions-http
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=google-cloud-functions-http \
    -Dextensions='google-cloud-functions-http,rest-jackson,undertow,reactive-routes,funqy-http' \
    -DnoCode
cd google-cloud-functions-http
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=google-cloud-functions-http"`

## Login to Google Cloud

Login to Google Cloud is necessary for deploying the application. It can be done as follows:

```bash
gcloud auth login
```

## Creating the endpoints

For this example project, we will create four endpoints, one for Quarkus REST (Jakarta REST), one for Undertow (Servlet), one for Reactive routes and one for [Funqy HTTP](/guides/funqy-http/index.md).

> [!NOTE]
> These various endpoints are for demonstration purposes. For real life applications, you should choose one of these technologies and stick to it.

If you don't need endpoints of each type, you can remove the corresponding extensions from your `pom.xml`.

### The Jakarta REST endpoint

```java
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }
}
```

### The Servlet endpoint

```java
import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "ServletGreeting", urlPatterns = "/servlet/hello")
public class GreetingServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(200);
        resp.addHeader("Content-Type", "text/plain");
        resp.getWriter().write("hello");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = req.getReader().readLine();
        resp.setStatus(200);
        resp.addHeader("Content-Type", "text/plain");
        resp.getWriter().write("hello " + name);
    }
}
```

### The Reactive Routes endpoint

```java
import static io.quarkus.vertx.web.Route.HttpMethod.GET;

import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;

public class GreetingRoutes {
    @Route(path = "/vertx/hello", methods = GET)
    void hello(RoutingContext context) {
        context.response().headers().set("Content-Type", "text/plain");
        context.response().setStatusCode(200).end("hello");
    }
}
```

### The Funqy HTTP endpoint

```java
import io.quarkus.funqy.Funq;

public class GreetingFunqy {
    @Funq
    public String funqy() {
        return "Make it funqy";
    }
}
```

## Build and Deploy to Google Cloud

> [!NOTE]
> Quarkus forces a packaging of type `uber-jar` for your function as Google Cloud Function deployment requires a single JAR.

Package your application using the standard `mvn clean package` command. The result of the previous command is a single JAR file inside the `target/deployment` directory that contains the classes and the dependencies of the project.

Then you will be able to use `gcloud` to deploy your function to Google Cloud.

> [!NOTE]
> We will use the Java 21 runtime, but you can switch to the Java 17 runtime by using `--runtime=java17` instead of `--runtime=java21` on the deploy commands.

```bash
gcloud functions deploy quarkus-example-http \
  --entry-point=io.quarkus.gcp.functions.http.QuarkusHttpFunction \
  --runtime=java21 --trigger-http --allow-unauthenticated --source=target/deployment
```

> [!IMPORTANT]
> The entry point must always be set to `io.quarkus.gcp.functions.http.QuarkusHttpFunction` as this is the class that integrates Cloud Functions with Quarkus.

> [!WARNING]
> The first time you launch this command, you can have the following error message:
>
> ```
> ERROR: (gcloud.functions.deploy) OperationError: code=7, message=Build Failed: Cloud Build has not been used in project <project_name> before or it is disabled. Enable it by visiting https://console.developers.google.com/apis/api/cloudbuild.googleapis.com/overview?project=<my-project> then retry.
> ```
>
> This means that Cloud Build is not activated yet. To overcome this error, open the URL shown in the error, follow the instructions and then wait a few minutes before retrying the command.

This command will give you as output a `url` that points to your function.

You can then call your endpoints via:

- For Jakarta REST: `{url}/hello`
- For servlet: `{url}/servlet/hello`
- For Reactive Routes: `{url}/vertx/hello`
- For Funqy: `{url}/funqy`

## Testing locally

The easiest way to locally test your function is using the Cloud Function invoker JAR.

You can download it via Maven using the following command:

```bash
mvn dependency:copy \
  -Dartifact='com.google.cloud.functions.invoker:java-function-invoker:2.0.0' \
  -DoutputDirectory=.
```

Before using the invoker, you first need to build your function via `mvn package`.

Then you can use it to launch your function locally.

```bash
java -jar java-function-invoker-2.0.0.jar \
  --classpath target/deployment/google-cloud-functions-http-1.0.0-SNAPSHOT-runner.jar \
  --target io.quarkus.gcp.functions.http.QuarkusHttpFunction
```

> [!IMPORTANT]
> The `--classpath` parameter needs to be set to the previously packaged JAR that contains your function class and all Quarkus related classes.

Your endpoints will be available on <http://localhost:8080>.

## What's next?

You can use our [Google Cloud Functions Funqy binding](/guides/funqy-gcp-functions/index.md) to use Funqy, a provider-agnostic function as a service framework, that allow to deploy HTTP function or Background function to Google Cloud.
