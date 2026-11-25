# SPIFFE Client

[SPIFFE](https://spiffe.io/) (Secure Production Identity Framework for Everyone) provides cryptographic workload identities in zero-trust environments. The SPIFFE client extension uses the [Workload API](https://github.com/spiffe/spiffe/blob/main/standards/SPIFFE_Workload_API.md) to retrieve [JWT-SVIDs](https://github.com/spiffe/spiffe/blob/main/standards/JWT-SVID.md) and [X.509-SVIDs](https://github.com/spiffe/spiffe/blob/main/standards/X509-SVID.md) for Quarkus workloads directly from the local [SPIRE Agent](https://spiffe.io/docs/latest/spire-about/spire-concepts/). It simplifies deployment by eliminating the need for a SPIFFE client sidecar that stores SVIDs on a mounted file path for Quarkus to read.

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered experimental.
>
> In *experimental* mode, early feedback is requested to mature the idea. There is no guarantee of stability nor long term presence in the platform until the solution matures. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

## Adding the extension

Add the `quarkus-spiffe-client` extension to your project:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-spiffe-client</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-spiffe-client")
```

## Configuration

The extension requires the URI of the SPIRE Agent's Workload Endpoint socket:

```properties
quarkus.spiffe-client.endpoint-socket=unix:///run/spire/sockets/agent.sock
```

If `quarkus.spiffe-client.endpoint-socket` is not set, the extension falls back to the standard `SPIFFE_ENDPOINT_SOCKET` environment variable as defined by the [SPIFFE Workload Endpoint specification](https://spiffe.io/docs/latest/spiffe-specs/spiffe_workload_endpoint/#4-locating-the-endpoint). This environment variable is typically set by the SPIRE Agent for workloads it manages.

The URI must use one of the following schemes, as defined by the [SPIFFE Workload Endpoint](https://github.com/spiffe/spiffe/blob/main/standards/SPIFFE_Workload_Endpoint.md) specification:

- `unix://` for Unix Domain Sockets (production default), for example `unix:///run/spire/sockets/agent.sock`
- `tcp://` for TCP connections (development and testing), for example `tcp://127.0.0.1:8080`

For the `tcp` scheme, the host must be an IP address and a port is required. Hostnames are not accepted.

On Windows, only the `tcp://` scheme is currently supported.

## Retrieve JWT-SVID

Inject the `JwtSvidClient` CDI bean to retrieve JWT-SVIDs from the SPIRE Agent. The following example retrieves a single JWT-SVID for one audience:

```java
import io.quarkus.spiffe.svid.jwt.JwtSvidClient;
import io.quarkus.spiffe.svid.jwt.WorkloadJsonWebToken;

@ApplicationScoped
public class MyService {

    @Inject
    JwtSvidClient jwtSvidClient;

    public Uni<String> getToken() {
        return jwtSvidClient.getWorkloadJsonWebToken("https://my-audience")
                .map(WorkloadJsonWebToken::token);
    }
}
```

To specify multiple audiences:

```java
import java.util.Set;

import io.quarkus.spiffe.svid.jwt.JwtSvidClient;
import io.quarkus.spiffe.svid.jwt.WorkloadJsonWebToken;
import io.smallrye.mutiny.Uni;

public Uni<String> getWorkloadIdentity() {
    return jwtSvidClient.getWorkloadJsonWebToken(Set.of("https://audience-a", "https://audience-b"))
            .map(WorkloadJsonWebToken::subject); <1>
}
```

1. The `subject()` method returns the JWT `sub` claim, which is a valid SPIFFE ID, for example `spiffe://example.org/myservice`.

Calls are not retried automatically; implement your own retry logic if your use case requires it.

### Default audiences

Pre-configure default audiences with `quarkus.spiffe-client.audiences` so callers do not need to specify them on every request:

```properties
quarkus.spiffe-client.audiences=https://keycloak.example.com,https://mcp-server.example.com
```

```java
return jwtSvidClient.getWorkloadJsonWebToken().map(WorkloadJsonWebToken::token);
```

When explicit audiences are provided, they take precedence over the configured defaults:

```java
return jwtSvidClient.getWorkloadJsonWebToken("https://other-service.example.com").map(WorkloadJsonWebToken::token);
```

## OIDC SPIFFE Client Authentication

When this extension is combined with either `quarkus-oidc` or `quarkus-oidc-client` extensions, JWT-SVIDs can be supplied automatically and used as OIDC client authentication assertions. Please see the [OIDC provider client authentication](/guides/security-oidc-code-flow-authentication/index.md#oidc-provider-client-authentication) section of the OIDC code flow authentication guide and the [OIDC Client authentication](/guides/security-openid-connect-client-reference/index.md#oidc-client-authentication) section of the OIDC client guide for more information.

## Retrieve X.509-SVID

X.509-SVID is an X.509 workload certificate document which includes a client certificate chain and private key, as well as a server trust bundle.

To retrieve an X.509-SVID from the SPIFFE Agent, inject the `X509SvidClient` CDI bean:

### Quarkus REST Client

The following example shows how to configure a Quarkus REST client with a retrieved X.509-SVID:

```java
import java.net.URI;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.spiffe.svid.x509.WorkloadCertificateDocument;
import io.quarkus.spiffe.svid.x509.X509SvidClient;
import io.quarkus.tls.BaseTlsConfiguration;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.TrustOptions;

@RequestScoped
class MyServiceClientProducer {

    @Inject
    X509SvidClient x509SvidClient;

    @Produces
    @ApplicationScoped
    MyServiceClient createClient() { <1>
        WorkloadCertificateDocument cert = x509SvidClient.getWorkloadCertificate()
                .await().indefinitely();
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create("https://my-service:8443"))
                .tlsConfiguration(new SpiffeTlsConfiguration(cert))
                .build(MyServiceClient.class);
    }

    private static final class SpiffeTlsConfiguration extends BaseTlsConfiguration {

        private final KeyCertOptions keyCertOptions;
        private final TrustOptions trustOptions;

        private SpiffeTlsConfiguration(WorkloadCertificateDocument cert) {
            var keyCert = new PemKeyCertOptions(); <2>
            for (String pem : cert.certificateChain().chainPem()) {
                keyCert.addCertValue(Buffer.buffer(pem));
            }
            keyCert.addKeyValue(Buffer.buffer(cert.certificateChain().privateKeyPem()));
            this.keyCertOptions = keyCert;

            var trust = new PemTrustOptions(); <3>
            for (String pem : cert.trustBundle().certificatesPem()) {
                trust.addCertValue(Buffer.buffer(pem));
            }
            this.trustOptions = trust;
        }

        @Override
        public KeyCertOptions getKeyStoreOptions() {
            return keyCertOptions;
        }

        @Override
        public TrustOptions getTrustStoreOptions() {
            return trustOptions;
        }
    }
}
```

1. Creates a REST client per request, fetching a fresh X.509-SVID from the SPIFFE Workload API. In practice, clients should consider caching SVIDs until close to expiration.
2. Builds PEM key-cert options from the SPIFFE certificate chain and private key.
3. Builds PEM trust options from the trust bundle CA certificates. The server certificate must be issued by a CA in the SPIFFE trust bundle for the TLS handshake to succeed.

> [!NOTE]
> When the server also presents an X.509-SVID, standard hostname verification must be disabled because SPIFFE conveys workload identity in the URI SAN, not the DNS SAN. In that case, the [X.509-SVID specification](https://github.com/spiffe/spiffe/blob/main/standards/X509-SVID.md#5-constraints-and-usage) requires additional leaf certificate validation — verifying the URI SAN contains a valid SPIFFE ID, that `basicConstraints` has `cA=false`, and that `keyUsage` includes `digitalSignature` but not `keyCertSign`. SPIFFE-aware trust validation via TLS registry integration will be provided in a future release.

> [!TIP]
> The `io.quarkus.spiffe.client.SpiffeClient` class extends both `JwtSvidClient` and `X509SvidClient` classes. When your workload needs both JWT-SVIDs and X.509-SVIDs, consider injecting `SpiffeClient` instead of dealing with both `JwtSvidClient` and `X509SvidClient` clients.

## Testing

In dev and test modes, the extension provides a Dev Service that emulates the SPIFFE Workload API with a pre-configured identity and a locally generated key pair. While it does not support custom authorization policies or registration entries, it is sufficient for verifying that your application correctly retrieves and uses JWT-SVIDs without requiring external SPIRE infrastructure. You can choose the transport protocol with the `quarkus.spiffe-client.devservices.transport` property, which accepts `unix` (default) or `tcp`.

<a id="keycloak-dev-svc-integration"></a>
### Testing SPIFFE client authentication with Keycloak

To test OIDC SPIFFE client authentication against Keycloak Dev Services, you need to configure a Keycloak realm with a SPIFFE identity provider and a `federated-jwt` client. You can automate this setup by creating a custom test resource using `KeycloakTestClient`. This ensures the realm is ready before your tests execute. For example, you can create a test realm like this:

```java
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.keycloak.client.KeycloakTestClient;

public class SpiffeKeycloakTestResource implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {

    private final KeycloakTestClient client = new KeycloakTestClient();

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        client.setIntegrationTestContext(context);
    }

    @Override
    public Map<String, String> start() {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm("quarkus-spiffe");
        realm.setEnabled(true);
        realm.setUsers(new ArrayList<>());
        realm.setClients(new ArrayList<>());

        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setProviderId("spiffe");
        idp.setAlias("spiffe-idp");
        idp.setEnabled(true);
        idp.setConfig(Map.of(
                "trustDomain", "spiffe://test.quarkus.io",
                "bundleEndpoint", "http://host.testcontainers.internal:18443/bundle"));
        realm.addIdentityProvider(idp);

        ClientRepresentation spiffeClient = new ClientRepresentation();
        spiffeClient.setClientId("my-app");
        spiffeClient.setPublicClient(false);
        spiffeClient.setServiceAccountsEnabled(true);
        spiffeClient.setStandardFlowEnabled(true);
        spiffeClient.setClientAuthenticatorType("federated-jwt");
        spiffeClient.setRedirectUris(List.of("*"));
        spiffeClient.setEnabled(true);
        spiffeClient.setAttributes(Map.of(
                "jwt.credential.issuer", "spiffe-idp",
                "jwt.credential.sub", "spiffe://test.quarkus.io/test-workload"));
        realm.getClients().add(spiffeClient);

        client.createRealm(realm);
        return Map.of();
    }

    @Override
    public void stop() {
    }
}
```

Next, adjust your application configuration to enable the SPIFFE feature in Keycloak Dev Services. The SPIFFE Dev Service bundle endpoint listens on port `18443` by default, and Keycloak must be able to reach it:

```properties
quarkus.keycloak.devservices.host-accessible-ports=${quarkus.spiffe-client.devservices.http-port}
quarkus.keycloak.devservices.features=spiffe
quarkus.keycloak.devservices.realm-name=quarkus-spiffe
quarkus.keycloak.devservices.create-realm=false
```

Alternatively, if you already have an existing realm, export it to a JSON file and set the SPIFFE identity provider's `trustDomain` to `spiffe://test.quarkus.io` and `bundleEndpoint` to `\http://host.testcontainers.internal:18443/bundle`. In your realm JSON file, add or update the SPIFFE identity provider entry in the `identityProviders` array:

```json
"identityProviders": [
    {
        "alias": "spiffe-idp",
        "providerId": "spiffe",
        "enabled": true,
        "config": {
            "trustDomain": "spiffe://test.quarkus.io",
            "bundleEndpoint": "http://host.testcontainers.internal:18443/bundle"
        }
    }
]
```

Then load the realm file with:

```properties
quarkus.keycloak.devservices.host-accessible-ports=${quarkus.spiffe-client.devservices.http-port}
quarkus.keycloak.devservices.features=spiffe
quarkus.keycloak.devservices.realm-path=quarkus-spiffe.json
```

Finally, apply this setup to your test class using the `@QuarkusTestResource` annotation:

```java
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(SpiffeKeycloakTestResource.class)
class MySpiffeClientTest {

    @Test
    void testEndpointThatReliesOnSpiffeClientAuthentication() {
        // Test application endpoint that relies on Keycloak SPIFFE client authentication
    }
}
```

## Extension integration

Extensions integrating with `SpiffeClient` should depend on the API module:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-spiffe-client-api</artifactId>
</dependency>
```

## Configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-spiffe-client_quarkus-spiffe-client-enabled"></a> [`quarkus.spiffe-client.enabled`](#quarkus-spiffe-client_quarkus-spiffe-client-enabled) config_property_copy_button:+++quarkus.spiffe-client.enabled+++[]<br><br>If SPIFFE Workload API client is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPIFFE_CLIENT_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-spiffe-client_quarkus-spiffe-client-devservices-enabled"></a> [`quarkus.spiffe-client.devservices.enabled`](#quarkus-spiffe-client_quarkus-spiffe-client-devservices-enabled) config_property_copy_button:+++quarkus.spiffe-client.devservices.enabled+++[]<br><br>Flag to enable (default) or disable Dev Services.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPIFFE_CLIENT_DEVSERVICES_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-spiffe-client_quarkus-spiffe-client-devservices-transport"></a> [`quarkus.spiffe-client.devservices.transport`](#quarkus-spiffe-client_quarkus-spiffe-client-devservices-transport) config_property_copy_button:+++quarkus.spiffe-client.devservices.transport+++[]<br><br>Transport protocol for the SPIFFE Workload API server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPIFFE_CLIENT_DEVSERVICES_TRANSPORT+++[] | `tcp`, `unix` | `` Defaults to `tcp` on Windows and `unix` on all other platforms. `` |
| [lock] <a id="quarkus-spiffe-client_quarkus-spiffe-client-devservices-http-port"></a> [`quarkus.spiffe-client.devservices.http-port`](#quarkus-spiffe-client_quarkus-spiffe-client-devservices-http-port) config_property_copy_button:+++quarkus.spiffe-client.devservices.http-port+++[]<br><br>The port for the bundle HTTP server to listen on.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPIFFE_CLIENT_DEVSERVICES_HTTP_PORT+++[] | int | `18443` |
| <a id="quarkus-spiffe-client_quarkus-spiffe-client-endpoint-socket"></a> [`quarkus.spiffe-client.endpoint-socket`](#quarkus-spiffe-client_quarkus-spiffe-client-endpoint-socket) config_property_copy_button:+++quarkus.spiffe-client.endpoint-socket+++[]<br><br>SPIFFE Workload Endpoint socket URI. Supports `unix://` for Unix Domain Socket and `tcp://` for TCP transport as defined by the [SPIFFE Workload Endpoint](https://github.com/spiffe/spiffe/blob/main/standards/SPIFFE_Workload_Endpoint.md) specification.<br><br>If not set, the standard `SPIFFE_ENDPOINT_SOCKET` environment variable is used as a fallback, as defined by the [SPIFFE Workload Endpoint §4 – Locating the Endpoint](https://spiffe.io/docs/latest/spiffe-specs/spiffe_workload_endpoint/#4-locating-the-endpoint) specification.<br><br>Example values: `unix:///run/spire/sockets/agent.sock`, `tcp://127.0.0.1:8080`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPIFFE_CLIENT_ENDPOINT_SOCKET+++[] | [URI](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/URI.html) | `${SPIFFE_ENDPOINT_SOCKET:}` |
| <a id="quarkus-spiffe-client_quarkus-spiffe-client-audiences"></a> [`quarkus.spiffe-client.audiences`](#quarkus-spiffe-client_quarkus-spiffe-client-audiences) config_property_copy_button:+++quarkus.spiffe-client.audiences+++[]<br><br>Default audience values that a SPIFFE JSON Web Token (JWT-SVID) is required to contain in its audience `aud` claim. These audiences are supplied to the Workload API when no audiences are directly provided to `SpiffeClient` methods for retrieving workload tokens.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPIFFE_CLIENT_AUDIENCES+++[] | list of string |  |
| <a id="quarkus-spiffe-client_quarkus-spiffe-client-request-timeout"></a> [`quarkus.spiffe-client.request-timeout`](#quarkus-spiffe-client_quarkus-spiffe-client-request-timeout) config_property_copy_button:+++quarkus.spiffe-client.request-timeout+++[]<br><br>The duration after which a request to the SPIFFE Workload API times out.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPIFFE_CLIENT_REQUEST_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-spiffe-client_quarkus-spiffe-client) | `10S` |

<a id="duration-note-anchor-quarkus-spiffe-client_quarkus-spiffe-client"></a>

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
