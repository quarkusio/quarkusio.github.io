# Azure Functions

The `quarkus-azure-functions` extension is a simple integration point between Azure Functions and Quarkus.  It interacts with Azure Functions runtime to bootstrap Quarkus and turns any Azure Functions class you write into a CDI/ArC bean.

This allows you to inject any service or component initialized by Quarkus directly into your function classes. You can also change the lifecycle of your function class from request scoped (the default) to application scoped too if you want your function class to be a singleton.

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered preview.
>
> In *preview*, backward compatibility and presence in the ecosystem is not guaranteed. Specific improvements might require changing configuration or APIs, and plans to become *stable* are under way. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

```java
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import jakarta.inject.Inject;
import java.util.Optional;

public class Function {
    @Inject
    GreetingService service;

    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        // Parse query parameter
        final String query = request.getQueryParameters().get("name");
        final String name = request.getBody().orElse(query);

        if (name == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a name on the query string or in the request body").build();
        } else {
            return request.createResponseBuilder(HttpStatus.OK).body(service.greeting(name)).build();
        }
    }
}
```

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

- [An Azure Account](https://azure.microsoft.com).  Free accounts work.
- [Azure Functions Core Tools](https://learn.microsoft.com/en-us/azure/azure-functions/functions-run-local#v2) version 4.x
- [Azure CLI Installed](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)

## Solution

This guide walks you through running a Maven project that can deploy an HTTP Trigger Azure Function class. This function class injects a CDI bean service that generates a greeting message that is passed back to the client.

## Creating the Maven/Gradle Project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:azure-functions-quickstart \
    --extension='quarkus-azure-functions'
cd azure-functions-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=azure-functions-quickstart \
    -Dextensions='quarkus-azure-functions'
cd azure-functions-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=azure-functions-quickstart"`

This command generates a project structure importing the Quarkus Azure Functions HTTP extension. It includes an example.

## Examining the project

If you open the `pom.xml` or `build.gradle` build file of the generated project, you'll see that the project is similar to any other Quarkus project. The `quarkus-azure-functions` extension is the integration point between Quarkus and Azure Functions. It registers callbacks with the Azure Functions runtime to bootstrap Quarkus and to set up Quarkus/ArC as the function factory for your function classes.

The current implementation of the `quarkus-azure-functions` extension no longer requires the `azure-functions-maven-plugin` or Gradle equivalent. Local development and Azure Functions packaging and deployment is now all done by Quarkus.

Build configuration is now all within `application.properties`. The only required configuration switch is `quarkus.azure-functions.app-name`.

## Azure Deployment Descriptors

The Azure Functions `host.json` deployment descriptor is automatically generated, but if you need to override it, declare it in the root directory of the project and rerun the build when you are ready.

## Quarkus dev mode

Quarkus dev mode does not work currently with Azure Functions.

## Run locally in Azure Functions local environment

If you want to try your app with a local Azure Functions environment, you can use this command:

**CLI**

```bash
quarkus run
```

**Maven**

```bash
./mvnw quarkus:run
```

**Gradle**

```bash
./gradlew --console=plain --info --no-daemon quarkusRun
```

> [!TIP]
> Gradle is a bit quirky with process management, so you need the `--no-daemon` switch, or control-c will not destroy the process cleanly, and you'll have open ports.

> [!NOTE]
> You must have the [Azure Functions Core Tools](https://learn.microsoft.com/en-us/azure/azure-functions/functions-run-local#v2) installed for this to work.

The URL to access the example would be:

<https://localhost:8081/HttpExample?name=Bill>

## Quarkus Integration Testing

You can implement integration tests using `@QuarkusIntegrationTest` functionality.  When these integration tests run, the local Azure Functions environment will be spun up for the duration of integration testing.

**Maven**

```bash
./mvnw verify
```

**Gradle**

```bash
./gradlew --info quarkusIntTest
```

> [!TIP]
> Make sure any integration tests you execute with Maven use the `*IT.java` file pattern so that regular builds do not execute the test.

> [!TIP]
> Make sure any integration tests you execute with Gradle are located within `src/integrationTest/java`. Integration tests that exist in `src/test` will run with normal build and fail.

## Login to Azure

To be able to deploy your application to Azure, log in to Azure:

```bash
az login
```

## Deploy to Azure

The `quarkus-azure-functions` extension handles all the work to deploy to Azure. By default, Quarkus will use the Azure CLI in the background to authenticate and deploy to Azure. If you have multiple subscriptions associated with your account, you must set the `quarkus.azure-functions.subscription-id` property in your `application.properties` file to the subscription you want to use. For other authentication mechanisms and deployment options, see our config properties [Azure Functions Configuration Reference](#configuration-reference).

To deploy your application, after you built your project, execute:

**Maven**

```bash
./mvnw quarkus:deploy
```

**Gradle**

```bash
./gradlew --info deploy
```

> [!TIP]
> For Gradle, you must use the `--info` switch to see this output.

If deployment is a success, Quarkus will output the endpoint URL of the example function to the console:

```
[INFO] HTTP Trigger Urls:
[INFO] 	 HttpExample : https://{appName}.azurewebsites.net/api/httpexample
```

The URL to access the service would be: `https://{appName}.azurewebsites.net/api/HttpExample`

<a id="configuration-reference"></a>
## Azure Functions Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-azure-functions_quarkus-azure-functions-app-name"></a> [`quarkus.azure-functions.app-name`](#quarkus-azure-functions_quarkus-azure-functions-app-name) config_property_copy_button:+++quarkus.azure-functions.app-name+++[]<br><br>App name for azure function project. This is required setting. Defaults to the base artifact name<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AZURE_FUNCTIONS_APP_NAME+++[] | string |  |
| [lock] <a id="quarkus-azure-functions_quarkus-azure-functions-resource-group"></a> [`quarkus.azure-functions.resource-group`](#quarkus-azure-functions_quarkus-azure-functions-resource-group) config_property_copy_button:+++quarkus.azure-functions.resource-group+++[]<br><br>Azure Resource Group for your Azure Functions<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AZURE_FUNCTIONS_RESOURCE_GROUP+++[] | string | `quarkus` |
| [lock] <a id="quarkus-azure-functions_quarkus-azure-functions-region"></a> [`quarkus.azure-functions.region`](#quarkus-azure-functions_quarkus-azure-functions-region) config_property_copy_button:+++quarkus.azure-functions.region+++[]<br><br>Specifies the region where your Azure Functions will be hosted; default value is westus. [Valid values](https://github.com/microsoft/azure-maven-plugins/wiki/Azure-Functions:-Configuration-Details#supported-regions)<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AZURE_FUNCTIONS_REGION+++[] | string | `westus` |
| [lock] <a id="quarkus-azure-functions_quarkus-azure-functions-disable-app-insights"></a> [`quarkus.azure-functions.disable-app-insights`](#quarkus-azure-functions_quarkus-azure-functions-disable-app-insights) config_property_copy_button:+++quarkus.azure-functions.disable-app-insights+++[]<br><br>Specifies whether to disable application insights for your function app<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AZURE_FUNCTIONS_DISABLE_APP_INSIGHTS+++[] | boolean | `false` |
| [lock] <a id="quarkus-azure-functions_quarkus-azure-functions-app-insights-key"></a> [`quarkus.azure-functions.app-insights-key`](#quarkus-azure-functions_quarkus-azure-functions-app-insights-key) config_property_copy_button:+++quarkus.azure-functions.app-insights-key+++[]<br><br>Specifies the instrumentation key of application insights which will bind to your function app<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AZURE_FUNCTIONS_APP_INSIGHTS_KEY+++[] | string |  |
| [lock] <a id="quarkus-azure-functions_quarkus-azure-functions-runtime-os"></a> [`quarkus.azure-functions.runtime.os`](#quarkus-azure-functions_quarkus-azure-functions-runtime-os) config_property_copy_button:+++quarkus.azure-functions.runtime.os+++[]<br><br>Valid values are linux, windows, and docker<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AZURE_FUNCTIONS_RUNTIME_OS+++[] | string | `linux` |
| [lock] <a id="quarkus-azure-functions_quarkus-azure-functions-runtime-java-version"></a> [`quarkus.azure-functions.runtime.java-version`](#quarkus-azure-functions_quarkus-azure-functions-runtime-java-version) config_property_copy_button:+++quarkus.azure-functions.runtime.java-version+++[]<br><br>Should be set to at least the minimum Quarkus compatible version<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AZURE_FUNCTIONS_RUNTIME_JAVA_VERSION+++[] | string | `17` |
| [lock] <a id="quarkus-azure-functions_quarkus-azure-functions-runtime-image"></a> [`quarkus.azure-functions.runtime.image`](#quarkus-azure-functions_quarkus-azure-functions-runtime-image) config_property_copy_button:+++quarkus.azure-functions.runtime.image+++[]<br><br>URL of docker image if deploying via docker<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AZURE_FUNCTIONS_RUNTIME_IMAGE+++[] | string |  |
| [lock] <a id="quarkus-azure-functions_quarkus-azure-functions-runtime-registry-url"></a> [`quarkus.azure-functions.runtime.registry-url`](#quarkus-azure-functions_quarkus-azure-functions-runtime-registry-url) config_property_copy_button:+++quarkus.azure-functions.runtime.registry-url+++[]<br><br>If using docker, url of registry<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AZURE_FUNCTIONS_RUNTIME_REGISTRY_URL+++[] | string |  |
| [lock] <a id="quarkus-azure-functions_quarkus-azure-functions-auth-type"></a> [`quarkus.azure-functions.auth.type`](#quarkus-azure-functions_quarkus-azure-functions-auth-type) config_property_copy_button:+++quarkus.azure-functions.auth.type+++[]<br><br>Description of each type can be found [here](https://github.com/microsoft/azure-maven-plugins/wiki/Authentication) Valid values are<br><br>\* **azure_cli** Delegates to Azure CLI for login \* **managed_identity** Requires client to be set \* **oauth2** Requires tenant to be set \* **device_code** Requires tenant to be set \* **file** Filesystem path to a property file that defines authentication. Properties supported are \*\* **type** Supports same type values as well as *service_principal* \*\* **client** \*\* **tenant** \*\* **key** Password for *service_principal* if using password authentication \*\* **certificate** Path to PEM file if using *service_principal* \*\* **certificate-password** Password for PEM file if it is password protected and if using *service_principal* \*\* **environment** if using *service_principal*<br><br>Defaults to "azure_cli" for authentication<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AZURE_FUNCTIONS_AUTH_TYPE+++[] | string | `azure_cli` |
| [lock] <a id="quarkus-azure-functions_quarkus-azure-functions-auth-path"></a> [`quarkus.azure-functions.auth.path`](#quarkus-azure-functions_quarkus-azure-functions-auth-path) config_property_copy_button:+++quarkus.azure-functions.auth.path+++[]<br><br>Filesystem path to properties file if using *file* type<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AZURE_FUNCTIONS_AUTH_PATH+++[] | string |  |
| [lock] <a id="quarkus-azure-functions_quarkus-azure-functions-auth-client"></a> [`quarkus.azure-functions.auth.client`](#quarkus-azure-functions_quarkus-azure-functions-auth-client) config_property_copy_button:+++quarkus.azure-functions.auth.client+++[]<br><br>Client or App Id required if using *managed_identity* type<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AZURE_FUNCTIONS_AUTH_CLIENT+++[] | string |  |
| [lock] <a id="quarkus-azure-functions_quarkus-azure-functions-auth-tenant"></a> [`quarkus.azure-functions.auth.tenant`](#quarkus-azure-functions_quarkus-azure-functions-auth-tenant) config_property_copy_button:+++quarkus.azure-functions.auth.tenant+++[]<br><br>Tenant ID required if using *oauth2* or *device_code* type<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AZURE_FUNCTIONS_AUTH_TENANT+++[] | string |  |
| [lock] <a id="quarkus-azure-functions_quarkus-azure-functions-app-service-plan-name"></a> [`quarkus.azure-functions.app-service-plan-name`](#quarkus-azure-functions_quarkus-azure-functions-app-service-plan-name) config_property_copy_button:+++quarkus.azure-functions.app-service-plan-name+++[]<br><br>Specifies the name of the existing App Service Plan when you do not want to create a new one.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AZURE_FUNCTIONS_APP_SERVICE_PLAN_NAME+++[] | string | `java-functions-app-service-plan` |
| [lock] <a id="quarkus-azure-functions_quarkus-azure-functions-app-service-plan-resource-group"></a> [`quarkus.azure-functions.app-service-plan-resource-group`](#quarkus-azure-functions_quarkus-azure-functions-app-service-plan-resource-group) config_property_copy_button:+++quarkus.azure-functions.app-service-plan-resource-group+++[]<br><br>The app service plan resource group.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AZURE_FUNCTIONS_APP_SERVICE_PLAN_RESOURCE_GROUP+++[] | string |  |
| [lock] <a id="quarkus-azure-functions_quarkus-azure-functions-subscription-id"></a> [`quarkus.azure-functions.subscription-id`](#quarkus-azure-functions_quarkus-azure-functions-subscription-id) config_property_copy_button:+++quarkus.azure-functions.subscription-id+++[]<br><br>Azure subscription id. Required only if there are more than one subscription in your account<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AZURE_FUNCTIONS_SUBSCRIPTION_ID+++[] | string |  |
| [lock] <a id="quarkus-azure-functions_quarkus-azure-functions-pricing-tier"></a> [`quarkus.azure-functions.pricing-tier`](#quarkus-azure-functions_quarkus-azure-functions-pricing-tier) config_property_copy_button:+++quarkus.azure-functions.pricing-tier+++[]<br><br>The pricing tier.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AZURE_FUNCTIONS_PRICING_TIER+++[] | string |  |
| [lock] <a id="quarkus-azure-functions_quarkus-azure-functions-func-port"></a> [`quarkus.azure-functions.func-port`](#quarkus-azure-functions_quarkus-azure-functions-func-port) config_property_copy_button:+++quarkus.azure-functions.func-port+++[]<br><br>Port to run azure function in local runtime. Will default to quarkus.http.test-port or 8081<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AZURE_FUNCTIONS_FUNC_PORT+++[] | int |  |
| [lock] <a id="quarkus-azure-functions_quarkus-azure-functions-local-debug-config"></a> [`quarkus.azure-functions.local-debug-config`](#quarkus-azure-functions_quarkus-azure-functions-local-debug-config) config_property_copy_button:+++quarkus.azure-functions.local-debug-config+++[]<br><br>Config String for local debug<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AZURE_FUNCTIONS_LOCAL_DEBUG_CONFIG+++[] | string | `transport=dt_socket,server=y,suspend=n,address=5005` |
