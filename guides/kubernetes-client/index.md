# Kubernetes Client

Quarkus includes the `kubernetes-client` extension which enables the use of the [Fabric8 Kubernetes Client](https://github.com/fabric8io/kubernetes-client) in native mode while also making it easier to work with.

Having a Kubernetes Client extension in Quarkus is very useful in order to unlock the power of Kubernetes Operators. Kubernetes Operators are quickly emerging as a new class of Cloud Native applications. These applications essentially watch the Kubernetes API and react to changes on various resources and can be used to manage the lifecycle of all kinds of complex systems like databases, messaging systems and much more. Being able to write such operators in Java with the very low footprint that native images provide is a great match.

## Configuration

Once you have your Quarkus project configured you can add the `kubernetes-client` extension to your project by running the following command in your project base directory.

**CLI**

```bash
quarkus extension add kubernetes-client
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='kubernetes-client'
```

**Gradle**

```bash
./gradlew addExtension --extensions='kubernetes-client'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-kubernetes-client</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-kubernetes-client")
```

## Usage

Quarkus configures a Bean of type `KubernetesClient` which can be injected into application code using the well known CDI methods. This client can be configured using various properties as can be seen in the following example:

```properties
quarkus.kubernetes-client.trust-certs=false
quarkus.kubernetes-client.namespace=default
```

Note that the full list of properties is available in the [Dev Services section of the configuration reference](#quarkus-kubernetes-client_section_quarkus-kubernetes-client-devservices).

In dev mode and when running tests, [Dev Services for Kubernetes](/guides/kubernetes-dev-services/index.md) automatically starts a Kubernetes API server.

### Customizing and overriding

Quarkus provides multiple integration points for influencing the Kubernetes Client provided as a CDI bean.

#### Kubernetes Client Config customization

The first integration point is the use of the `io.quarkus.kubernetes.client.KubernetesConfigCustomizer` interface. When such a bean exists, it allows for arbitrary customizations of the `io.fabric8.kubernetes.client.Config` created by Quarkus (which takes into account the `quarkus.kubernetes-client.*` properties).

Alternatively, application code can override the  `io.fabric8.kubernetes.client.Config` or even the `io.fabric8.kubernetes.client.KubernetesClient` bean (which are normally provided by the extension) by simply declaring custom versions of those beans.

An example of this can be seen in the following snippet:

```java
@Singleton
public class KubernetesClientProducer {

    @Produces
    public KubernetesClient kubernetesClient() {
        // here you would create a custom client
        return new DefaultKubernetesClient();
    }
}
```

#### Kubernetes Client ObjectMapper customization

The Fabric8 Kubernetes Client uses its own `ObjectMapper` instance for serialization and deserialization of Kubernetes resources. This mapper is provided to the client through a `KubernetesSerialization` instance that's injected into the `KubernetesClient` bean.

If for some reason you must customize the default `ObjectMapper` bean provided by this extension and used by the Kubernetes Client, you can do so by declaring a bean that implements the `KubernetesClientObjectMapperCustomizer` interface.

The following code snippet contains an example of a `KubernetesClientObjectMapperCustomizer` to set the `ObjectMapper` locale:

```java
@Singleton
public static class Customizer implements KubernetesClientObjectMapperCustomizer {
    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.setLocale(Locale.ROOT);
    }
}
```

Furthermore, if you must replace the default `ObjectMapper` bean used by the Kubernetes Client that the extension creates automatically, you can do so by declaring a bean of type `@KubernetesClientObjectMapper`. The following code snippet shows how you can declare this bean:

```java
@Singleton
public class KubernetesObjectMapperProducer {
    @KubernetesClientObjectMapper
    @Singleton
    @Produces
    public ObjectMapper kubernetesClientObjectMapper() {
        return new ObjectMapper();
    }
}
```

> [!WARNING]
> The static `io.fabric8.kubernetes.client.utils.Serialization` utils class is deprecated and should not be used. Access to `Serialization.jsonMapper()` should be replaced by the usage of `@KubernetesClientObjectMapperCustomizer` declared beans.

## Testing

To make testing against a mock Kubernetes API extremely simple, Quarkus provides the `WithKubernetesTestServer` annotation which automatically launches a mock of the Kubernetes API server and sets the proper environment variables needed so that the Kubernetes Client configures itself to use said mock. Tests can inject the mock server and set it up in any way necessary for the particular testing using the `@KubernetesTestServer` annotation.

Let's assume we have a REST endpoint defined like so:

```java
@Path("/pod")
public class Pods {

    private final KubernetesClient kubernetesClient;

    public Pods(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @GET
    @Path("/{namespace}")
    public List<Pod> pods(String namespace) {
        return kubernetesClient.pods().inNamespace(namespace).list().getItems();
    }
}
```

We could write a test for this endpoint very easily like so:

```java
// you can even configure aspects like crud, https and port on this annotation
@WithKubernetesTestServer
@QuarkusTest
public class KubernetesClientTest {

    @KubernetesTestServer
    KubernetesServer mockServer;
    @Inject
    KubernetesClient client;

    @BeforeEach
    public void before() {
        final Pod pod1 = new PodBuilder().withNewMetadata().withName("pod1").withNamespace("test").and().build();
        final Pod pod2 = new PodBuilder().withNewMetadata().withName("pod2").withNamespace("test").and().build();

        // Set up Kubernetes so that our "pretend" pods are created
        client.pods().resource(pod1).create();
        client.pods().resource(pod2).create();
    }

    @Test
    public void testInteractionWithAPIServer() {
        RestAssured.when().get("/pod/test").then()
                .body("size()", is(2));
    }

}
```

Note that to take advantage of these features, the `quarkus-test-kubernetes-client` dependency needs to be added, for example like so:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-test-kubernetes-client</artifactId>
    <scope>test</scope>
</dependency>
```

**build.gradle**

```gradle
testImplementation("io.quarkus:quarkus-test-kubernetes-client")
```

By default, the mock server will be in CRUD mode, so you have to use the client to build your state before your application can retrieve it, but you can also set it up in non-CRUD mode and mock all HTTP requests made to Kubernetes:

```java
// you can even configure aspects like crud, https and port on this annotation
@WithKubernetesTestServer(crud = false)
@QuarkusTest
public class KubernetesClientTest {

    @KubernetesTestServer
    KubernetesServer mockServer;

    @BeforeEach
    public void before() {
        final Pod pod1 = new PodBuilder().withNewMetadata().withName("pod1").withNamespace("test").and().build();
        final Pod pod2 = new PodBuilder().withNewMetadata().withName("pod2").withNamespace("test").and().build();

        // Mock any HTTP request to Kubernetes pods so that our pods are returned
        mockServer.expect().get().withPath("/api/v1/namespaces/test/pods")
                .andReturn(200,
                        new PodListBuilder().withNewMetadata().withResourceVersion("1").endMetadata().withItems(pod1, pod2)
                                .build())
                .always();
    }

    @Test
    public void testInteractionWithAPIServer() {
        RestAssured.when().get("/pod/test").then()
                .body("size()", is(2));
    }

}
```

You can also use the `setup` attribute on the `@WithKubernetesTestServer` annotation to provide a class that will configure the `KubernetesServer` instance:

```java
@WithKubernetesTestServer(setup = MyTest.Setup.class)
@QuarkusTest
public class MyTest {

    public static class Setup implements Consumer<KubernetesServer> {

        @Override
        public void accept(KubernetesServer server) {
          server.expect().get().withPath("/api/v1/namespaces/test/pods")
            .andReturn(200, new PodList()).always();
        }
    }

    // tests
}
```

Alternately, you can create an extension of the `KubernetesServerTestResource` class to ensure all your `@QuarkusTest` enabled test classes share the same mock server setup via the `QuarkusTestResource` annotation:

```java
public class CustomKubernetesMockServerTestResource extends KubernetesServerTestResource {

    @Override
    protected void configureServer() {
        super.configureServer();
        server.expect().get().withPath("/api/v1/namespaces/test/pods")
          .andReturn(200, new PodList()).always();
    }
}
```

and use this in your other test classes as follows:

```java
@QuarkusTestResource(CustomKubernetesMockServerTestResource.class)
@QuarkusTest
public class KubernetesClientTest {

    //tests will now use the configured server...
}
```

<a id="note-on-generic-types"></a>
## Note on implementing or extending generic types

Due to the restrictions imposed by GraalVM, extra care needs to be taken when implementing or extending generic types provided by the client if the application is intended to work in native mode. Essentially every implementation or extension of generic classes such as `Watcher`, `ResourceHandler` or `CustomResource` needs to specify their associated Kubernetes model class (or, in the case of `CustomResource`, regular Java types) at class definition time. To better understand this, suppose we want to watch for changes to Kubernetes `Pod` resources. There are a couple ways to write such a `Watcher` that are guaranteed to work in native:

```java
client.pods().watch(new Watcher<Pod>() {
    @Override
    public void eventReceived(Action action, Pod pod) {
        // do something
    }

    @Override
    public void onClose(KubernetesClientException e) {
        // do something
    }
});
```

or

```java
public class PodResourceWatcher implements Watcher<Pod> {
    @Override
    public void eventReceived(Action action, Pod pod) {
        // do something
    }

    @Override
    public void onClose(KubernetesClientException e) {
        // do something
    }
}

...


client.pods().watch(new PodResourceWatcher());
```

Note that defining the generic type via a class hierarchy similar to the following example will also work correctly:

```java
public abstract class MyWatcher<S> implements Watcher<S> {
}

...


client.pods().watch(new MyWatcher<Pod>() {
    @Override
    public void eventReceived(Action action, Pod pod) {
        // do something
    }
});
```

> [!WARNING]
> The following example will **not** work in native mode because the generic type of watcher cannot be determined by looking at the class and method definitions thus making Quarkus unable to properly determine the Kubernetes model class for which reflection registration is needed:

```java
public class ResourceWatcher<T extends HasMetadata> implements Watcher<T> {
    @Override
    public void eventReceived(Action action, T resource) {
        // do something
    }

    @Override
    public void onClose(KubernetesClientException e) {
        // do something
    }
}

client.pods().watch(new ResourceWatcher<Pod>());
```

<a id="note-on-ec-keys"></a>
## Note on using Elliptic Curve keys

Please note that if you would like to use Elliptic Curve keys with Kubernetes Client then adding a BouncyCastle PKIX dependency is required:

**pom.xml**

```xml
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcpkix-jdk18on</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("org.bouncycastle:bcpkix-jdk18on")
```

Note that internally an `org.bouncycastle.jce.provider.BouncyCastleProvider` provider will be registered if it has not already been registered.

You can have this provider registered as described in the [BouncyCastle](/guides/security-customization/index.md#bouncy-castle) or [BouncyCastle FIPS](/guides/security-customization/index.md#bouncy-castle-fips) sections.

## Access to the Kubernetes API

In many cases in order to access the Kubernetes API server a `ServiceAccount`, `Role` and `RoleBinding` will be necessary. An example that allows listing all pods could look something like this:

```yaml
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: <applicationName>
  namespace: <namespace>
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: <applicationName>
  namespace: <namespace>
rules:
  - apiGroups: [""]
    resources: ["pods"]
    verbs: ["list"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: <applicationName>
  namespace: <namespace>
roleRef:
  kind: Role
  name: <applicationName>
  apiGroup: rbac.authorization.k8s.io
subjects:
  - kind: ServiceAccount
    name: <applicationName>
    namespace: <namespace>
```

Replace `<applicationName>` and `<namespace>` with your values. Have a look at [Configure Service Accounts for Pods](https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/) to get further information.

<a id="openshift-client"></a>
## OpenShift Client

If the targeted Kubernetes cluster is an OpenShift cluster, it is possible to access it through the `openshift-client` extension, in a similar way. This leverages the dedicated fabric8 openshift client, and provides access to `OpenShift` proprietary objects (e.g. `Route`, `ProjectRequest`, `BuildConfig` ...)

Note that the configuration properties are shared with the `kubernetes-client` extension. In particular, they have the same `quarkus.kubernetes-client` prefix.

Add the extension with:

**CLI**

```bash
quarkus extension add openshift-client
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='openshift-client'
```

**Gradle**

```bash
./gradlew addExtension --extensions='openshift-client'
```

Note that `openshift-client` extension has a dependency on the `kubernetes-client` extension.

To use the client, inject an `OpenShiftClient` instead of the `KubernetesClient`:

```java
@Inject
private OpenShiftClient openshiftClient;
```

If you need to override the default `OpenShiftClient`, provide a producer such as:

```java
@Singleton
public class OpenShiftClientProducer {

    @Produces
    public OpenShiftClient openshiftClient() {
        // here you would create a custom client
        return new DefaultOpenShiftClient();
    }
}
```

Mock support is also provided in a similar fashion by using the `@WithKubernetesTestServer` explained in the previous section:

```java
@WithKubernetesTestServer
@QuarkusTest
public class OpenShiftClientTest {

    @KubernetesTestServer
    KubernetesServer mockServer;
    @Inject
    OpenShiftClient client;

    @Test
    public void testInteractionWithAPIServer() {
        RestAssured.when().get("/route/test").then()
                .body("size()", is(2));
    }
}
```

To use this feature, you have to add a dependency on `quarkus-test-kubernetes-client`:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-test-kubernetes-client</artifactId>
    <scope>test</scope>
</dependency>
```

**build.gradle**

```gradle
testImplementation("io.quarkus:quarkus-test-kubernetes-client")
```

<a id="optimizing-native-image"></a>
## Optimizing the Native Image

The Kubernetes and OpenShift client extensions aim to provide a great **developer experience** while enabling the client to work in **native mode**. When building a native image, the Kubernetes Client extension will register all the accessible Kubernetes model classes for reflection. Unfortunately, this can lead to large native image sizes and longer build times.

Once you've completed your application implementation, if you want to distribute and deploy your application as a **native image**, you should consider reducing its size by following these guidelines.

### Use the Kubernetes Client extension

The [OpenShift Client](#openshift-client) provides domain-specific language (DSL) accessors to common OpenShift resources. In addition, the extension supplies the necessary project configuration to bring in the OpenShift model type modules.

In JVM mode, this works great because, as a developer, you don't need to worry about the configuration. However, in native mode, by depending on the OpenShift extension you're bringing in many resources that your application might not need unnecessarily increasing its size.

In this context, it's better to depend only on what you need, by adding a dependency to the Kubernetes Client extension and only the minimum OpenShift model dependencies:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-kubernetes-client</artifactId>
</dependency>
<dependency>
    <groupId>io.fabric8</groupId>
    <artifactId>openshift-model</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-kubernetes-client")
implementation("io.fabric8:openshift-model")
```

The OpenShift-specific DSL accessors won't be available since you'll now have a Bean of type `KubernetesClient` instead of `OpenShiftClient`. However, the Fabric8 Kubernetes Client provides generic entry-points to perform operations on any resource:

```java
// List OpenShift Routes in any namespace
kubernetesClient
    .resources(io.fabric8.openshift.api.model.Route.class)
    .inAnyNamespace().list();
// Delete an OpenShift Route
kubernetesClient
    .resources(io.fabric8.openshift.api.model.Route.class)
    .inNamespace("default").withName("the-route").delete();
// Create or replace a new OpenShift Route
kubernetesClient
    .resource(new RouteBuilder()/* ... */.build())
    .inNamespace("default").createOr(NonDeletingOperation::update);
```

### Depend only on the modules you need

The Kubernetes Client extension has transitive dependencies to all of the vanilla Kubernetes API model types. This is very convenient in JVM mode, since you don't need to worry about configuring the project.

However, in native mode, this implies registering for reflection model types that you very likely won't use in your application. You can mitigate this by providing a more granular project configuration and depending only on those models you are sure the application uses.

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-kubernetes-client</artifactId>
</dependency>
<dependency>
    <groupId>io.fabric8</groupId>
    <artifactId>kubernetes-client-api</artifactId>
    <!-- Exclude all transitive dependencies -->
    <exclusions>
        <exclusion>
            <groupId>io.fabric8</groupId>
            <artifactId>*</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<!-- Include only those that make sense for your application -->
<dependency>
    <groupId>io.fabric8</groupId>
    <artifactId>kubernetes-client</artifactId>
</dependency>
<dependency>
    <groupId>io.fabric8</groupId>
    <artifactId>kubernetes-model-core</artifactId>
</dependency>
<dependency>
    <groupId>io.fabric8</groupId>
    <artifactId>kubernetes-model-admissionregistration</artifactId>
</dependency>
<dependency>
    <groupId>io.fabric8</groupId>
    <artifactId>kubernetes-model-apps</artifactId>
</dependency>
<!-- ... -->
```

**build.gradle**

```gradle
implementation("quarkus-kubernetes-client")
implementation("io.fabric8:kubernetes-client-api") {
    // Exclude all transitive dependencies
    exclude group: "io.fabric8"
}
// Include only those that make sense for your application
implementation("io.fabric8:kubernetes-client")
implementation("io.fabric8:kubernetes-model-core")
implementation("io.fabric8:kubernetes-model-admissionregistration")
implementation("io.fabric8:kubernetes-model-apps")
// ...
```

## Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-generate-rbac"></a> [`quarkus.kubernetes-client.generate-rbac`](#quarkus-kubernetes-client_quarkus-kubernetes-client-generate-rbac) config_property_copy_button:+++quarkus.kubernetes-client.generate-rbac+++[]<br><br>Enable the generation of the RBAC manifests. If enabled and no other role binding are provided using the properties `quarkus.kubernetes.rbac.`, it will generate a default role binding using the role "view" and the application service account.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_GENERATE_RBAC+++[] | boolean | `true` |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-trust-certs"></a> [`quarkus.kubernetes-client.trust-certs`](#quarkus-kubernetes-client_quarkus-kubernetes-client-trust-certs) config_property_copy_button:+++quarkus.kubernetes-client.trust-certs+++[]<br><br>Whether the client should trust a self-signed certificate if so presented by the API server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_TRUST_CERTS+++[] | boolean |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-api-server-url"></a> [`quarkus.kubernetes-client.api-server-url`](#quarkus-kubernetes-client_quarkus-kubernetes-client-api-server-url) config_property_copy_button:+++quarkus.kubernetes-client.api-server-url+++[]<br><br>URL of the Kubernetes API server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_API_SERVER_URL+++[] | string |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-namespace"></a> [`quarkus.kubernetes-client.namespace`](#quarkus-kubernetes-client_quarkus-kubernetes-client-namespace) config_property_copy_button:+++quarkus.kubernetes-client.namespace+++[]<br><br>Default namespace to use<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_NAMESPACE+++[] | string |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-ca-cert-file"></a> [`quarkus.kubernetes-client.ca-cert-file`](#quarkus-kubernetes-client_quarkus-kubernetes-client-ca-cert-file) config_property_copy_button:+++quarkus.kubernetes-client.ca-cert-file+++[]<br><br>CA certificate file<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_CA_CERT_FILE+++[] | string |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-ca-cert-data"></a> [`quarkus.kubernetes-client.ca-cert-data`](#quarkus-kubernetes-client_quarkus-kubernetes-client-ca-cert-data) config_property_copy_button:+++quarkus.kubernetes-client.ca-cert-data+++[]<br><br>CA certificate data<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_CA_CERT_DATA+++[] | string |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-kubeconfig-file"></a> [`quarkus.kubernetes-client.kubeconfig-file`](#quarkus-kubernetes-client_quarkus-kubernetes-client-kubeconfig-file) config_property_copy_button:+++quarkus.kubernetes-client.kubeconfig-file+++[]<br><br>Path to a kubeconfig file to use for configuring the client. When set, the file will be read and used as the base configuration.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_KUBECONFIG_FILE+++[] | string |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-client-cert-file"></a> [`quarkus.kubernetes-client.client-cert-file`](#quarkus-kubernetes-client_quarkus-kubernetes-client-client-cert-file) config_property_copy_button:+++quarkus.kubernetes-client.client-cert-file+++[]<br><br>Client certificate file<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_CLIENT_CERT_FILE+++[] | string |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-client-cert-data"></a> [`quarkus.kubernetes-client.client-cert-data`](#quarkus-kubernetes-client_quarkus-kubernetes-client-client-cert-data) config_property_copy_button:+++quarkus.kubernetes-client.client-cert-data+++[]<br><br>Client certificate data<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_CLIENT_CERT_DATA+++[] | string |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-client-key-file"></a> [`quarkus.kubernetes-client.client-key-file`](#quarkus-kubernetes-client_quarkus-kubernetes-client-client-key-file) config_property_copy_button:+++quarkus.kubernetes-client.client-key-file+++[]<br><br>Client key file<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_CLIENT_KEY_FILE+++[] | string |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-client-key-data"></a> [`quarkus.kubernetes-client.client-key-data`](#quarkus-kubernetes-client_quarkus-kubernetes-client-client-key-data) config_property_copy_button:+++quarkus.kubernetes-client.client-key-data+++[]<br><br>Client key data<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_CLIENT_KEY_DATA+++[] | string |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-client-key-algo"></a> [`quarkus.kubernetes-client.client-key-algo`](#quarkus-kubernetes-client_quarkus-kubernetes-client-client-key-algo) config_property_copy_button:+++quarkus.kubernetes-client.client-key-algo+++[]<br><br>Client key algorithm<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_CLIENT_KEY_ALGO+++[] | string |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-client-key-passphrase"></a> [`quarkus.kubernetes-client.client-key-passphrase`](#quarkus-kubernetes-client_quarkus-kubernetes-client-client-key-passphrase) config_property_copy_button:+++quarkus.kubernetes-client.client-key-passphrase+++[]<br><br>Client key passphrase<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_CLIENT_KEY_PASSPHRASE+++[] | string |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-username"></a> [`quarkus.kubernetes-client.username`](#quarkus-kubernetes-client_quarkus-kubernetes-client-username) config_property_copy_button:+++quarkus.kubernetes-client.username+++[]<br><br>Kubernetes auth username<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_USERNAME+++[] | string |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-password"></a> [`quarkus.kubernetes-client.password`](#quarkus-kubernetes-client_quarkus-kubernetes-client-password) config_property_copy_button:+++quarkus.kubernetes-client.password+++[]<br><br>Kubernetes auth password<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_PASSWORD+++[] | string |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-token"></a> [`quarkus.kubernetes-client.token`](#quarkus-kubernetes-client_quarkus-kubernetes-client-token) config_property_copy_button:+++quarkus.kubernetes-client.token+++[]<br><br>Kubernetes oauth token<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_TOKEN+++[] | string |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-watch-reconnect-interval"></a> [`quarkus.kubernetes-client.watch-reconnect-interval`](#quarkus-kubernetes-client_quarkus-kubernetes-client-watch-reconnect-interval) config_property_copy_button:+++quarkus.kubernetes-client.watch-reconnect-interval+++[]<br><br>Watch reconnect interval<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_WATCH_RECONNECT_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-kubernetes-client_quarkus-kubernetes-client) |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-watch-reconnect-limit"></a> [`quarkus.kubernetes-client.watch-reconnect-limit`](#quarkus-kubernetes-client_quarkus-kubernetes-client-watch-reconnect-limit) config_property_copy_button:+++quarkus.kubernetes-client.watch-reconnect-limit+++[]<br><br>Maximum reconnect attempts in case of watch failure By default there is no limit to the number of reconnect attempts<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_WATCH_RECONNECT_LIMIT+++[] | int |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-connection-timeout"></a> [`quarkus.kubernetes-client.connection-timeout`](#quarkus-kubernetes-client_quarkus-kubernetes-client-connection-timeout) config_property_copy_button:+++quarkus.kubernetes-client.connection-timeout+++[]<br><br>Maximum amount of time to wait for a connection with the API server to be established<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_CONNECTION_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-kubernetes-client_quarkus-kubernetes-client) |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-request-timeout"></a> [`quarkus.kubernetes-client.request-timeout`](#quarkus-kubernetes-client_quarkus-kubernetes-client-request-timeout) config_property_copy_button:+++quarkus.kubernetes-client.request-timeout+++[]<br><br>Maximum amount of time to wait for a request to the API server to be completed<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_REQUEST_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-kubernetes-client_quarkus-kubernetes-client) |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-request-retry-backoff-limit"></a> [`quarkus.kubernetes-client.request-retry-backoff-limit`](#quarkus-kubernetes-client_quarkus-kubernetes-client-request-retry-backoff-limit) config_property_copy_button:+++quarkus.kubernetes-client.request-retry-backoff-limit+++[]<br><br>Maximum number of retry attempts for API requests that fail with an HTTP code of >= 500<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_REQUEST_RETRY_BACKOFF_LIMIT+++[] | int |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-request-retry-backoff-interval"></a> [`quarkus.kubernetes-client.request-retry-backoff-interval`](#quarkus-kubernetes-client_quarkus-kubernetes-client-request-retry-backoff-interval) config_property_copy_button:+++quarkus.kubernetes-client.request-retry-backoff-interval+++[]<br><br>Time interval between retry attempts for API requests that fail with an HTTP code of >= 500<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_REQUEST_RETRY_BACKOFF_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-kubernetes-client_quarkus-kubernetes-client) |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-http-proxy"></a> [`quarkus.kubernetes-client.http-proxy`](#quarkus-kubernetes-client_quarkus-kubernetes-client-http-proxy) config_property_copy_button:+++quarkus.kubernetes-client.http-proxy+++[]<br><br>HTTP proxy used to access the Kubernetes API server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_HTTP_PROXY+++[] | string |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-https-proxy"></a> [`quarkus.kubernetes-client.https-proxy`](#quarkus-kubernetes-client_quarkus-kubernetes-client-https-proxy) config_property_copy_button:+++quarkus.kubernetes-client.https-proxy+++[]<br><br>HTTPS proxy used to access the Kubernetes API server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_HTTPS_PROXY+++[] | string |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-proxy-username"></a> [`quarkus.kubernetes-client.proxy-username`](#quarkus-kubernetes-client_quarkus-kubernetes-client-proxy-username) config_property_copy_button:+++quarkus.kubernetes-client.proxy-username+++[]<br><br>Proxy username<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_PROXY_USERNAME+++[] | string |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-proxy-password"></a> [`quarkus.kubernetes-client.proxy-password`](#quarkus-kubernetes-client_quarkus-kubernetes-client-proxy-password) config_property_copy_button:+++quarkus.kubernetes-client.proxy-password+++[]<br><br>Proxy password<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_PROXY_PASSWORD+++[] | string |  |
| <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-no-proxy"></a> [`quarkus.kubernetes-client.no-proxy`](#quarkus-kubernetes-client_quarkus-kubernetes-client-no-proxy) config_property_copy_button:+++quarkus.kubernetes-client.no-proxy+++[]<br><br>IP addresses or hosts to exclude from proxying<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_NO_PROXY+++[] | list of string |  |
| <a id="quarkus-kubernetes-client_section_quarkus-kubernetes-client-devservices"></a> [Dev Services](#quarkus-kubernetes-client_section_quarkus-kubernetes-client-devservices) | Type | Default |
| [lock] <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-devservices-enabled"></a> [`quarkus.kubernetes-client.devservices.enabled`](#quarkus-kubernetes-client_quarkus-kubernetes-client-devservices-enabled) config_property_copy_button:+++quarkus.kubernetes-client.devservices.enabled+++[]<br><br>If Dev Services for Kubernetes should be used. (default to true)<br><br>If this is true and kubernetes client is not configured then a kubernetes cluster will be started and will be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_DEVSERVICES_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-devservices-api-version"></a> [`quarkus.kubernetes-client.devservices.api-version`](#quarkus-kubernetes-client_quarkus-kubernetes-client-devservices-api-version) config_property_copy_button:+++quarkus.kubernetes-client.devservices.api-version+++[]<br><br>The kubernetes api server version to use.<br><br>If not set, Dev Services for Kubernetes will use the [latest supported version](https://github.com/dajudge/kindcontainer/blob/master/k8s-versions.json) of the given flavor.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_DEVSERVICES_API_VERSION+++[] | string |  |
| [lock] <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-devservices-image-name"></a> [`quarkus.kubernetes-client.devservices.image-name`](#quarkus-kubernetes-client_quarkus-kubernetes-client-devservices-image-name) config_property_copy_button:+++quarkus.kubernetes-client.devservices.image-name+++[]<br><br>The kubernetes image to use.<br><br>If not set, Dev Services for Kubernetes will use default image for the specified `api-version()` for the given `flavor()`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_DEVSERVICES_IMAGE_NAME+++[] | string |  |
| [lock] <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-devservices-flavor"></a> [`quarkus.kubernetes-client.devservices.flavor`](#quarkus-kubernetes-client_quarkus-kubernetes-client-devservices-flavor) config_property_copy_button:+++quarkus.kubernetes-client.devservices.flavor+++[]<br><br>The flavor to use (kind, k3s or api-only).<br><br>If not set, Dev Services for Kubernetes will set it to: api-only.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_DEVSERVICES_FLAVOR+++[] | tooltip:kind[kind (needs privileged docker)], tooltip:k3s[k3s (needs privileged docker)], tooltip:api-only[api only] |  |
| [lock] <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-devservices-override-kubeconfig"></a> [`quarkus.kubernetes-client.devservices.override-kubeconfig`](#quarkus-kubernetes-client_quarkus-kubernetes-client-devservices-override-kubeconfig) config_property_copy_button:+++quarkus.kubernetes-client.devservices.override-kubeconfig+++[]<br><br>By default, if a kubeconfig is found, Dev Services for Kubernetes will not start. Set this to true to override the kubeconfig config.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_DEVSERVICES_OVERRIDE_KUBECONFIG+++[] | boolean | `false` |
| [lock] <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-devservices-manifests"></a> [`quarkus.kubernetes-client.devservices.manifests`](#quarkus-kubernetes-client_quarkus-kubernetes-client-devservices-manifests) config_property_copy_button:+++quarkus.kubernetes-client.devservices.manifests+++[]<br><br>A list of manifest file paths that should be applied to the Kubernetes cluster Dev Service on startup. If not set, no manifests are applied.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_DEVSERVICES_MANIFESTS+++[] | list of string |  |
| [lock] <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-devservices-shared"></a> [`quarkus.kubernetes-client.devservices.shared`](#quarkus-kubernetes-client_quarkus-kubernetes-client-devservices-shared) config_property_copy_button:+++quarkus.kubernetes-client.devservices.shared+++[]<br><br>Indicates if the Kubernetes cluster managed by Quarkus Dev Services is shared. When shared, Quarkus looks for running containers using label-based service discovery. If a matching container is found, it is used, and so a second one is not started. Otherwise, Dev Services for Kubernetes starts a new container.<br><br>The discovery uses the `quarkus-dev-service-kubernetes` label. The value is configured using the `service-name` property.<br><br>Container sharing is only used in dev mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_DEVSERVICES_SHARED+++[] | boolean | `true` |
| [lock] <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-devservices-service-name"></a> [`quarkus.kubernetes-client.devservices.service-name`](#quarkus-kubernetes-client_quarkus-kubernetes-client-devservices-service-name) config_property_copy_button:+++quarkus.kubernetes-client.devservices.service-name+++[]<br><br>The value of the `quarkus-dev-service-kubernetes` label attached to the started container. This property is used when `shared` is set to `true`. In this case, before starting a container, Dev Services for Kubernetes looks for a container with the `quarkus-dev-service-kubernetes` label set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it starts a new container with the `quarkus-dev-service-kubernetes` label set to the specified value.<br><br>This property is used when you need multiple shared Kubernetes clusters.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_DEVSERVICES_SERVICE_NAME+++[] | string | `kubernetes` |
| [lock] <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-devservices-container-env-environment-variable-name"></a> [`quarkus.kubernetes-client.devservices.container-env."environment-variable-name"`](#quarkus-kubernetes-client_quarkus-kubernetes-client-devservices-container-env-environment-variable-name) config_property_copy_button:+++quarkus.kubernetes-client.devservices.container-env."environment-variable-name"+++[]<br><br>Environment variables that are passed to the container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_DEVSERVICES_CONTAINER_ENV__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |

<a id="duration-note-anchor-quarkus-kubernetes-client_quarkus-kubernetes-client"></a>

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
