# Using OAuth2 RBAC

This guide explains how your Quarkus application can utilize OAuth2 tokens to provide secured access to the Jakarta REST (formerly known as JAX-RS) endpoints.

OAuth2 is an authorization framework that enables applications to obtain access to an HTTP resource on behalf of a user. It can be used to implement an application authentication mechanism based on tokens by delegating to an external server (the authentication server) the user authentication and providing a token for the authentication context.

This extension provides a light-weight support for using the opaque Bearer Tokens and validating them by calling an introspection endpoint.

If the OAuth2 Authentication server provides JWT Bearer Tokens, consider using either [OIDC Bearer token authentication](/guides/security-oidc-bearer-token-authentication/index.md) or [SmallRye JWT](/guides/security-jwt/index.md) extensions instead. OpenID Connect extension has to be used if the Quarkus application needs to authenticate the users using OIDC Authorization Code Flow. For more information, see the [OIDC code flow mechanism for protecting web applications](/guides/security-oidc-code-flow-authentication/index.md) guide.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: git clone <https://github.com/quarkusio/quarkus-quickstarts.git>, or download an archive.

The solution is located in the `security-oauth2-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/security-oauth2-quickstart). It contains a very simple UI to use the Jakarta REST resources created here, too.

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:security-oauth2-quickstart \
    --extension='rest-jackson,security-oauth2' \
    --no-code
cd security-oauth2-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=security-oauth2-quickstart \
    -Dextensions='rest-jackson,security-oauth2' \
    -DnoCode
cd security-oauth2-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=security-oauth2-quickstart"`

This command generates a project and imports the `elytron-security-oauth2` extension, which includes the OAuth2 opaque token support.

If you don't want to use the Maven plugin, you can just include the dependency in your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-elytron-security-oauth2</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-elytron-security-oauth2")
```

### Examine the Jakarta REST resource

Create the `src/main/java/org/acme/security/oauth2/TokenSecuredResource.java` file with the following content:

```java
package org.acme.security.oauth2;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/secured")
public class TokenSecuredResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }
}
```

This is a basic REST endpoint that does not have any of the Elytron Security OAuth2 specific features, so let's add some.

We will use the JSR 250 common security annotations, they are described in the [Using Security](/guides/security-overview/index.md) guide.

```java
package org.acme.security.oauth2;

import java.security.Principal;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path("/secured")
@ApplicationScoped
public class TokenSecuredResource {


    @GET()
    @Path("permit-all")
    @PermitAll // <1>
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@Context SecurityContext ctx) { // <2>
        Principal caller =  ctx.getUserPrincipal(); <3>
        String name = caller == null ? "anonymous" : caller.getName();
        String helloReply = String.format("hello + %s, isSecure: %s, authScheme: %s", name, ctx.isSecure(), ctx.getAuthenticationScheme());
        return helloReply; // <4>
    }
}
```

1. `@PermitAll` indicates that the given endpoint is accessible by any caller, authenticated or not.
2. Here we inject the Jakarta REST `SecurityContext` to inspect the security state of the call.
3. Here we obtain the current request user/caller `Principal`. For an unsecured call this will be null, so we build the username by checking `caller` against null.
4. The reply we build up makes use of the caller name, the `isSecure()` and `getAuthenticationScheme()` states of the request `SecurityContext`.

### Setting up application.properties

You need to configure your application with the following minimal properties:

```properties
quarkus.oauth2.client-id=client_id
quarkus.oauth2.client-secret=secret
quarkus.oauth2.introspection-url=http://oauth-server/introspect
```

You need to specify the introspection URL of your authentication server and the `client-id` / `client-secret` that your application will use to authenticate itself to the authentication server.\
The extension will then use this information to validate the token and recover the information associated with it.

For all configuration properties, see the [Configuration Reference](#config-reference) section at the end of this guide.

## Run the application

Now we are ready to run our application. Use:

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

Now that the REST endpoint is running, we can access it using a command line tool like curl:

```shell
$ curl http://127.0.0.1:8080/secured/permit-all; echo
hello + anonymous, isSecure: false, authScheme: null
```

We have not provided any token in our request, so we would not expect that there is any security state seen by the endpoint, and the response is consistent with that:

- username is anonymous
- `isSecure` is false as https is not used
- `authScheme` is null

### Securing the endpoint

So now let's actually secure something. Take a look at the new endpoint method `helloRolesAllowed` in the following:

```java
package org.acme.security.oauth2;

import java.security.Principal;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

@Path("/secured")
@ApplicationScoped
public class TokenSecuredResource {

    @GET()
    @Path("permit-all")
    @PermitAll
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@Context SecurityContext ctx) {
        Principal caller =  ctx.getUserPrincipal();
        String name = caller == null ? "anonymous" : caller.getName();
        String helloReply = String.format("hello + %s, isSecure: %s, authScheme: %s", name, ctx.isSecure(), ctx.getAuthenticationScheme());
        return helloReply;
    }

    @GET()
    @Path("roles-allowed") // <1>
    @RolesAllowed({"Echoer", "Subscriber"}) // <2>
    @Produces(MediaType.TEXT_PLAIN)
    public String helloRolesAllowed(@Context SecurityContext ctx) {
        Principal caller =  ctx.getUserPrincipal();
        String name = caller == null ? "anonymous" : caller.getName();
        String helloReply = String.format("hello + %s, isSecure: %s, authScheme: %s", name, ctx.isSecure(), ctx.getAuthenticationScheme());
        return helloReply;
    }
}
```

1. This new endpoint will be located at `/secured/roles-allowed`
2. `@RolesAllowed` indicates that the given endpoint is accessible by a caller if they have either an "Echoer" or a "Subscriber" role assigned.

After you make this addition to your `TokenSecuredResource`, try `curl -v http://127.0.0.1:8080/secured/roles-allowed; echo` to attempt to access the new endpoint. Your output should be:

```shell
$ curl -v http://127.0.0.1:8080/secured/roles-allowed; echo
*   Trying 127.0.0.1...
* TCP_NODELAY set
* Connected to 127.0.0.1 (127.0.0.1) port 8080 (#0)
> GET /secured/roles-allowed HTTP/1.1
> Host: 127.0.0.1:8080
> User-Agent: curl/7.54.0
> Accept: */*
>
< HTTP/1.1 401 Unauthorized
< Connection: keep-alive
< Content-Type: text/html;charset=UTF-8
< Content-Length: 14
< Date: Sun, 03 Mar 2019 16:32:34 GMT
<
* Connection #0 to host 127.0.0.1 left intact
Not authorized
```

Excellent, we have not provided any OAuth2 token in the request, so we should not be able to access the endpoint, and we were not. Instead, we received an HTTP 401 Unauthorized error. We need to obtain and pass in a valid OAuth2 token to access that endpoint. There are two steps to this, 1) configuring our Elytron Security OAuth2 extension with information on how to validate the token, and 2) generating a matching token with the appropriate claims.

### Generating a token

You need to obtain the token from a standard OAuth2 authentication server ([Keycloak](https://www.keycloak.org/) for example) using the token endpoint.

You can find below a curl example of such call for a `client_credential` flow:

```bash
curl -X POST "http://oauth-server/token?grant_type=client_credentials" \
-H  "Accept: application/json" -H  "Authorization: Basic Y2xpZW50X2lkOmNsaWVudF9zZWNyZXQ="
```

It should respond something like that...

```json
{"access_token":"60acf56d-9daf-49ba-b3be-7a423d9c7288","token_type":"bearer","expires_in":1799,"scope":"READER"}
```

### Finally, make a secured request to /secured/roles-allowed

Now let's use this to make a secured request to the `/secured/roles-allowed` endpoint

```shell
$ curl -H "Authorization: Bearer 60acf56d-9daf-49ba-b3be-7a423d9c7288" http://127.0.0.1:8080/secured/roles-allowed; echo
hello + client_id isSecure: false, authScheme: OAuth2
```

Success! We now have:

- a non-anonymous caller name of client_id
- an authentication scheme of OAuth2

## Roles mapping

Roles are mapped from one of the claims of the introspection endpoint response. By default, it's the `scope` claim. Roles are obtained by splitting the claim with a space separator. If the claim is an array, no splitting is done, the roles are obtained from the array.

You can customize the name of the claim to use for the roles with the `quarkus.oauth2.role-claim` property.

## Package and run the application

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

And executed using `java -jar target/quarkus-app/quarkus-run.jar`:

```shell
[INFO] Scanning for projects...
...
$ java -jar target/quarkus-app/quarkus-run.jar
2019-03-28 14:27:48,839 INFO  [io.quarkus] (main) Quarkus 3.40.1.2 started in 0.796s. Listening on: http://[::]:8080
2019-03-28 14:27:48,841 INFO  [io.quarkus] (main) Installed features: [cdi, rest, rest-jackson, security, security-oauth2]
```

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

```shell
[INFO] Scanning for projects...
...
[security-oauth2-quickstart-runner:25602]     universe:     493.17 ms
[security-oauth2-quickstart-runner:25602]      (parse):     660.41 ms
[security-oauth2-quickstart-runner:25602]     (inline):   1,431.10 ms
[security-oauth2-quickstart-runner:25602]    (compile):   7,301.78 ms
[security-oauth2-quickstart-runner:25602]      compile:  10,542.16 ms
[security-oauth2-quickstart-runner:25602]        image:   2,797.62 ms
[security-oauth2-quickstart-runner:25602]        write:     988.24 ms
[security-oauth2-quickstart-runner:25602]      [total]:  43,778.16 ms
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  51.500 s
[INFO] Finished at: 2019-06-28T14:30:56-07:00
[INFO] ------------------------------------------------------------------------

$ ./target/security-oauth2-quickstart-runner
2019-03-28 14:31:37,315 INFO  [io.quarkus] (main) Quarkus 0.20.0 started in 0.006s. Listening on: http://[::]:8080
2019-03-28 14:31:37,316 INFO  [io.quarkus] (main) Installed features: [cdi, rest, rest-jackson, security, security-oauth2]
```

<a id="integration-testing"></a>
## Integration testing

If you don't want to use a real OAuth2 authorization server for your integration tests, you can use the [Properties based security](/guides/security-properties/index.md) extension for your test, or mock an authorization server using Wiremock.

First, Wiremock needs to be added as a test dependency. For a Maven project that would happen like so:

**pom.xml**

```xml
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock</artifactId>
    <scope>test</scope>
    <version>${wiremock.version}</version> // <1>
</dependency>
```

1. Use a proper Wiremock version. All available versions can be found [here](https://search.maven.org/artifact/org.wiremock/wiremock).

**build.gradle**

```gradle
testImplementation("org.wiremock:wiremock:${wiremock.version}") <1>
```

1. Use a proper Wiremock version. All available versions can be found [here](https://search.maven.org/artifact/org.wiremock/wiremock).

In Quarkus tests when some service needs to be started before the Quarkus tests are ran, we utilize the `@io.quarkus.test.common.QuarkusTestResource` annotation to specify a `io.quarkus.test.common.QuarkusTestResourceLifecycleManager` which can start the service and supply configuration values that Quarkus will use.

> [!NOTE]
> For more details about `@QuarkusTestResource` refer to [this part of the documentation](/guides/getting-started-testing/index.md#quarkus-test-resource).

Let's create an implementation of `QuarkusTestResourceLifecycleManager` called `MockAuthorizationServerTestResource` like so:

```java
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Collections;
import java.util.Map;

public class MockAuthorizationServerTestResource implements QuarkusTestResourceLifecycleManager {  // <1>

    private WireMockServer wireMockServer;

    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer();
        wireMockServer.start(); // <2>

        // define the mock for the introspect endpoint
        WireMock.stubFor(WireMock.post("/introspect").willReturn(WireMock.aResponse() // <3>
                .withBody(
                        "{\"active\":true,\"scope\":\"Echoer\",\"username\":null,\"iat\":1562315654,\"exp\":1562317454,\"expires_in\":1458,\"client_id\":\"my_client_id\"}")));


        return Collections.singletonMap("quarkus.oauth2.introspection-url", wireMockServer.baseUrl() + "/introspect"); // <4>
    }

    @Override
    public void stop() {
        if (null != wireMockServer) {
            wireMockServer.stop();  // <5>
        }
    }
}
```

1. The `start` method is invoked by Quarkus before any test is run and returns a `Map` of configuration properties that apply during the test execution.
2. Launch Wiremock.
3. Configure Wiremock to stub the calls to `/introspect` by returning an OAuth2 introspect response. You need to customize this line to return what's needed for your application (at least the scope property as roles are derived from the scope).
4. As the `start` method returns configuration that applies for tests, we set the `quarkus.oauth2.introspection-url` property that controls the URL of the introspect endpoint used by the OAuth2 extension.
5. When all tests have finished, shutdown Wiremock.

Your test class needs to be annotated like with `@QuarkusTestResource(MockAuthorizationServerTestResource.class)` to use this `QuarkusTestResourceLifecycleManager`.

Below is an example of a test that uses the `MockAuthorizationServerTestResource`.

```java
@QuarkusTest
@QuarkusTestResource(MockAuthorizationServerTestResource.class) // <1>
class TokenSecuredResourceTest {
    // use whatever token you want as the mock OAuth server will accept all tokens
    private static final String BEARER_TOKEN = "337aab0f-b547-489b-9dbd-a54dc7bdf20d"; // <2>

    @Test
    void testPermitAll() {
        RestAssured.given()
                .when()
                .header("Authorization", "Bearer " + BEARER_TOKEN) // <3>
                .get("/secured/permit-all")
                .then()
                .statusCode(200)
                .body(containsString("hello"));
    }

    @Test
    void testRolesAllowed() {
        RestAssured.given()
                .when()
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .get("/secured/roles-allowed")
                .then()
                .statusCode(200)
                .body(containsString("hello"));
    }
}
```

1. Use the previously created `MockAuthorizationServerTestResource` as a Quarkus test resource.
2. Define whatever token you want, it will not be validated by the OAuth2 mock authorization server.
3. Use this token inside the `Authorization` header to trigger OAuth2 authentication.

> [!WARNING]
> `@QuarkusTestResource` applies to all tests, not just `TokenSecuredResourceTest`.

## References

- [OAuth2](https://tools.ietf.org/html/rfc6749)
- [Quarkus Security overview](/guides/security-overview/index.md)

<a id="config-reference"></a>
## Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-elytron-security-oauth2_quarkus-oauth2-enabled"></a> [`quarkus.oauth2.enabled`](#quarkus-elytron-security-oauth2_quarkus-oauth2-enabled) config_property_copy_button:+++quarkus.oauth2.enabled+++[]<br><br>Determine if the OAuth2 extension is enabled. Enabled by default if you include the `elytron-security-oauth2` dependency, so this would be used to disable it.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OAUTH2_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-elytron-security-oauth2_quarkus-oauth2-role-claim"></a> [`quarkus.oauth2.role-claim`](#quarkus-elytron-security-oauth2_quarkus-oauth2-role-claim) config_property_copy_button:+++quarkus.oauth2.role-claim+++[]<br><br>The claim that is used in the introspection endpoint response to load the roles.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OAUTH2_ROLE_CLAIM+++[] | string | `scope` |
| <a id="quarkus-elytron-security-oauth2_quarkus-oauth2-client-id"></a> [`quarkus.oauth2.client-id`](#quarkus-elytron-security-oauth2_quarkus-oauth2-client-id) config_property_copy_button:+++quarkus.oauth2.client-id+++[]<br><br>The OAuth2 client id used to validate the token. Mandatory if the extension is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OAUTH2_CLIENT_ID+++[] | string |  |
| <a id="quarkus-elytron-security-oauth2_quarkus-oauth2-client-secret"></a> [`quarkus.oauth2.client-secret`](#quarkus-elytron-security-oauth2_quarkus-oauth2-client-secret) config_property_copy_button:+++quarkus.oauth2.client-secret+++[]<br><br>The OAuth2 client secret used to validate the token. Mandatory if the extension is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OAUTH2_CLIENT_SECRET+++[] | string |  |
| <a id="quarkus-elytron-security-oauth2_quarkus-oauth2-introspection-url"></a> [`quarkus.oauth2.introspection-url`](#quarkus-elytron-security-oauth2_quarkus-oauth2-introspection-url) config_property_copy_button:+++quarkus.oauth2.introspection-url+++[]<br><br>The OAuth2 introspection endpoint URL used to validate the token and gather the authentication claims. Mandatory if the extension is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OAUTH2_INTROSPECTION_URL+++[] | string |  |
| <a id="quarkus-elytron-security-oauth2_quarkus-oauth2-ca-cert-file"></a> [`quarkus.oauth2.ca-cert-file`](#quarkus-elytron-security-oauth2_quarkus-oauth2-ca-cert-file) config_property_copy_button:+++quarkus.oauth2.ca-cert-file+++[]<br><br>The OAuth2 server certificate file. *Warning*: this is not supported in native mode where the certificate must be included in the truststore used during the native image generation, see [Using SSL With Native Executables](/guides/native-and-ssl).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OAUTH2_CA_CERT_FILE+++[] | string |  |
| <a id="quarkus-elytron-security-oauth2_quarkus-oauth2-connection-timeout"></a> [`quarkus.oauth2.connection-timeout`](#quarkus-elytron-security-oauth2_quarkus-oauth2-connection-timeout) config_property_copy_button:+++quarkus.oauth2.connection-timeout+++[]<br><br>Client connection timeout for token introspection. Infinite if not set.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OAUTH2_CONNECTION_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-elytron-security-oauth2_quarkus-oauth2) |  |
| <a id="quarkus-elytron-security-oauth2_quarkus-oauth2-read-timeout"></a> [`quarkus.oauth2.read-timeout`](#quarkus-elytron-security-oauth2_quarkus-oauth2-read-timeout) config_property_copy_button:+++quarkus.oauth2.read-timeout+++[]<br><br>Client read timeout for token introspection. Infinite if not set.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OAUTH2_READ_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-elytron-security-oauth2_quarkus-oauth2) |  |

<a id="duration-note-anchor-quarkus-elytron-security-oauth2_quarkus-oauth2"></a>

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
