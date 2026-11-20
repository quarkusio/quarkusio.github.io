# TLS registry reference

The TLS Registry is a Quarkus extension that centralizes TLS configuration, making it easier to manage and maintain secure connections across your application.

When defining TLS configurations in a single centralized location, you can use the TLS Registry to reference them from multiple components within the application, ensuring consistency and reducing the risk of configuration errors.

The TLS Registry consolidates settings and supports multiple named configurations. Therefore, you can tailor TLS settings for different application parts. This flexibility is particularly useful when different components require distinct security configurations.

The TLS Registry extension is automatically included in your project when you use compatible extensions, such as Quarkus REST, gRPC, SmallRye GraphQL Client , or Reactive Routes .

As a result, applications that use the TLS Registry can be ready to handle secure communications out of the box.

TLS Registry also provides automatic certificate reloading , integration with Let's Encrypt (ACME) , support for Kubernetes Cert-Manager, and compatibility with various keystore formats, such as PKCS12, PEM, and JKS.

<a id="using-the-tls-registry"></a>
## Using the TLS registry

To configure a TLS connection, including key and truststores, use the `quarkus.tls.*` properties. These properties are required for:

- Setting up the default TLS configuration, defined directly under `quarkus.tls.*`
- Creating separate, named configurations by using `quarkus.tls.<name>.*`. By specifying the `quarkus.tls.<name>.*` properties, you can adapt the TLS settings for a specific component.

> [!IMPORTANT]
> The default TLS configuration is not a fallback or global configuration. Each named TLS configuration, or "TLS bucket," must provide its own properties. For instance, `quarkus.tls.reload-period` applies only to the default TLS configuration.

> [!IMPORTANT]
> As described in detail [here](https://github.com/quarkusio/quarkus/blob/main/adr/0004-using-the-tls-registry-for-clients.adoc#configuring-clients-with-the-tls-registry), Quarkus client extensions (such as REST, GRPC, and so on) ignore properties defined in default (that is, unnamed) TLS configurations. The `quarkus.tls.trust-all` property is the only exception.

### Configuring HTTPS for an HTTP server

To ensure secure client-server communication, the client is often required to verify the server's authenticity.

- The server must use a keystore that contains its certificate and private key.
- The client needs to be configured with a truststore to validate the server's certificate.

During the TLS handshake, the server presents its certificate, which the client then validates. This prevents man-in-the-middle attacks and secures data transmission.

The following sections guide you through setting up HTTPS by using PEM or PKCS12 keystore types. In addition, they provide guidance on named configurations to specify and manage multiple TLS configurations simultaneously, allowing you to define distinct settings for each.

Use one of the following configuration examples based on your keystore type:

- **By using PEM files:**

  ```properties
  quarkus.tls.key-store.pem.0.cert=server.crt
  quarkus.tls.key-store.pem.0.key=server.key
  quarkus.http.insecure-requests=disabled # Reject HTTP requests
  ```


- **By using a `p12` (PKCS12) keystore:**

  ```properties
  quarkus.tls.key-store.p12.path=server-keystore.p12
  quarkus.tls.key-store.p12.password=secret
  quarkus.http.insecure-requests=disabled # Reject HTTP requests
  ```


- **Distinguishing multiple configurations with names:**

  ```properties
  quarkus.tls.https.key-store.p12.path=server-keystore.p12
  quarkus.tls.https.key-store.p12.password=secret
  quarkus.http.insecure-requests=disabled
  quarkus.http.tls-configuration-name=https
  ```


### Configuring HTTPS for a client

The following example configures a gRPC client named "hello" to use HTTPS with a truststore from the default TLS configuration:

```properties
quarkus.tls.trust-store.jks.path=grpc-client-truststore.jks
quarkus.tls.trust-store.jks.password=password

quarkus.grpc.clients.hello.plain-text=false
quarkus.grpc.clients.hello.use-quarkus-grpc-client=true
```

### Configuring mTLS

To set up mutual TLS (mTLS) in your Quarkus application, configure the server and the client by creating and managing both a keystore and a truststore for each:

- **Server keystore**: Contains the server's certificate and private key.
- **Client keystore**: Contains the client's certificate and private key.
- **Server truststore**: Stores the client's certificate for authenticating the client.
- **Client truststore**: Stores the server's certificate for authenticating the server.

  .An example configuration for specifying keystores and truststores:

  ```properties
  quarkus.tls.my-server.key-store.p12.path=target/certs/grpc-keystore.p12
  quarkus.tls.my-server.key-store.p12.password=password
  quarkus.tls.my-server.trust-store.p12.path=target/certs/grpc-server-truststore.p12
  quarkus.tls.my-server.trust-store.p12.password=password

  quarkus.tls.my-client.trust-store.p12.path=target/certs/grpc-client-truststore.p12
  quarkus.tls.my-client.trust-store.p12.password=password
  quarkus.tls.my-client.key-store.p12.path=target/certs/grpc-client-keystore.p12
  quarkus.tls.my-client.key-store.p12.password=password

  quarkus.grpc.clients.hello.plain-text=false
  quarkus.grpc.clients.hello.tls-configuration-name=my-client
  quarkus.grpc.clients.hello.use-quarkus-grpc-client=true

  quarkus.http.ssl.client-auth=REQUIRED # Enable mTLS
  quarkus.http.insecure-requests=disabled
  quarkus.http.tls-configuration-name=my-server
  quarkus.grpc.server.use-separate-server=false
  quarkus.grpc.server.plain-text=false
  ```

  This configuration enables mTLS by ensuring that both the server and the client validate each other's certificates, which provides an additional layer of security.


<a id="referencing-a-tls-configuration"></a>
## Referencing a TLS configuration

To reference an example *named* configuration that you created by using the `quarkus.tls.<name>.*` properties as explained in [Using the TLS registry](#using-the-tls-registry), use the `tls-configuration-name` property as shown in the following examples:

**Example configuration for the core HTTP server:**

```properties
# Reference the named configuration
quarkus.http.tls-configuration-name=MY_TLS_CONFIGURATION
```

**Example configuration for a gRPC client:**

```properties
quarkus.grpc.clients.hello.tls-configuration-name=MY_TLS_CONFIGURATION
```

**Example configuration for a SmallRye GraphQL client:**

```properties
quarkus.smallrye-graphql-client.my-client.tls-configuration-name=MY_TLS_CONFIGURATION
```

> [!NOTE]
> When you configure the Typesafe GraphQL client with certificate reloading, as described in the [Reloading certificates](#reloading-certificates) section, override the bean scope to `RequestScoped` or another scope shorter than the application scope. The Typesafe client is an application-scoped bean by default.
>
> A shorter scope ensures that instances created after a certificate reload use the latest certificate.
>
> Dynamic clients are `@Dependent` scoped. Inject dynamic clients into components with an appropriate scope.

### Referencing the default truststore of SunJSSE

JDK distributions typically contain a truststore in the `$JAVA_HOME/lib/security/cacerts` file. SunJSSE uses this truststore as the default truststore. SunJSSE is the default implementation of the Java Secure Socket Extension (JSSE).

Java runtime components, such as `javax.net.ssl.HttpsURLConnection`, rely on JSSE for SSL and TLS.

Although Quarkus extensions typically do not honor the default truststore of SunJSSE, it is still practical to use it in some situations. This applies when migrating from legacy technologies or running on a Linux distribution where the SunJSSE truststore is synchronized with the operating system (OS).

To simplify the use of the SunJSSE truststore, Quarkus TLS Registry provides a TLS configuration under the name `javax.net.ssl` that mimics the default behavior of SunJSSE:

- If the `javax.net.ssl.trustStore` system property is defined, its value is honored as a truststore.
- Otherwise, the paths `$JAVA_HOME/lib/security/jssecacerts` and `$JAVA_HOME/lib/security/cacerts` are checked, and the first existing file is used as a truststore.
- If neither condition is met, an `IllegalStateException` is thrown.

The password to open the truststore is obtained from the `javax.net.ssl.trustStorePassword` system property. If this property is not set, the default password `changeit` is used.

The `javax.net.ssl` configuration can be used as a value for various `*.tls-configuration-name` properties, as shown below:

**Example configuration for a gRPC client:**

```properties
quarkus.grpc.clients.hello.tls-configuration-name=javax.net.ssl
```

> [!WARNING]
> The `javax.net.ssl` TLS configuration can not be customized or overridden.

## Configuring TLS

TLS configuration primarily involves managing keystores and truststores. The specific setup depends on the format used, such as PEM, P12, or JKS.

The following sections outline the available TLS configuration properties.

### Key stores

Key stores store private keys and certificates. They are mainly used on the server side, but can also be used on the client side when mTLS is used.

#### PEM keystores

Privacy Enhanced Mail (PEM) keystores are composed of a list of file pairs:

- **The certificate file** - a `.crt` or `.pem` file.
- **The private key file** - often a `.key` file.

To configure a PEM keystore:

```properties
quarkus.tls.key-store.pem.a.cert=server.crt
quarkus.tls.key-store.pem.a.key=server.key
quarkus.tls.key-store.pem.b.cert=my-second-cert.crt
quarkus.tls.key-store.pem.b.key=my-second-key.key
```

In most cases, you only need a single pair consisting of a certificate and a private key. Even if the certificate is part of a certificate chain, it includes only one private key that corresponds to the end-entity certificate.

When multiple pairs are configured, the selection of one of the configured pairs of certificates and private keys is based on Server Name Indication (SNI). The client sends the name of the server to which the client is attempting to connect, and the server selects the appropriate pair of certificates and private keys. To use this feature, ensure that [SNI](/guides/tls-registry-reference/index.md#sni) is enabled on both the client and server.

> [!IMPORTANT]
> When configuring multiple key pairs or certificate pairs, the server executes the configured pairs in a lexicographical order of their names by default, as demonstrated with `store.pem.a` and `store.pem.b` in the previous example. The pair with the lowest lexicographical order is executed first. To change this, you can define the order by using the `quarkus.tls.key-store.pem.order` property. For example, `quarkus.tls.key-store.pem.order=b,c,a`.
>
> This setting is important when using SNI, because it uses the first specified pair as the default.

When using a PEM keystore, the following formats are supported:

- PKCS#8 private key (unencrypted)
- PKCS#1 RSA private key (unencrypted)
- Encrypted PKCS#8 private key (encrypted with AES-128-CBC)

For an encrypted PKCS#8 private key, set the `quarkus.tls.key-store.pem.password` or `quarkus.tls.key-store.pem.<name>.password` property to the password that decrypts the private key.

**An encrypted PEM keystore configuration example:**

```properties
quarkus.tls.http.key-store.pem.cert=certificate.crt
quarkus.tls.http.key-store.pem.key=key.key
quarkus.tls.http.key-store.pem.password=password
```

#### PKCS12 keystores

PKCS12 keystores are single files that contain the certificate and the private key.

To configure a PKCS12 keystore:

```properties
quarkus.tls.key-store.p12.path=server-keystore.p12
quarkus.tls.key-store.p12.password=secret
```

`.p12` files are password-protected, so you need to provide the password to open the keystore.

These files can contain multiple certificates and private keys. If this is the case, take either of the following actions:

- Provide and configure the alias of the certificate and the private key you want to use:

  ```properties
  quarkus.tls.key-store.p12.path=server-keystore.p12
  quarkus.tls.key-store.p12.password=secret
  quarkus.tls.key-store.p12.alias=my-alias
  quarkus.tls.key-store.p12.alias-password=my-alias-password
  ```


- Alternatively, use SNI to select the appropriate certificate and private key. Note that all keys must use the same password.

#### JKS keystores

JKS keystores are single files that contain the server or client certificate and its private key, used to authenticate and establish secure TLS/SSL connections.

> [!IMPORTANT]
> JKS is an old but still widely used Java-specific format. However, to work with this format, you must use specific, and nowadays also deprecated, Java tooling. Thus, its use with your Quarkus application is not recommended.
>
> Additionally, OpenShift cert-manager or Let's Encrypt does not typically provide JKS and remains PEM-only.

To configure a JKS keystore:

```properties
quarkus.tls.key-store.jks.path=server-keystore.jks
quarkus.tls.key-store.jks.password=secret
```

`.jks` files are password-protected, so you need to provide the password to open the keystore. Also, they can include multiple certificates and private keys. If this is the case:

- Provide and configure the alias of the certificate and the private key you want to use:

  ```properties
  quarkus.tls.key-store.jks.path=server-keystore.jks
  quarkus.tls.key-store.jks.password=secret
  quarkus.tls.key-store.jks.alias=my-alias
  quarkus.tls.key-store.jks.alias-password=my-alias-password
  ```

- Alternatively, use SNI to select the appropriate certificate and private key. Note that all keys must use the same password.

#### Provided keystores

If you need more control over the keystore used in a TLS configuration, you can provide a CDI bean implementing the `io.quarkus.tls.KeyStoreProvider` interface.

Quarkus calls `KeyStoreProvider::getKeyStore` when the TLS configuration is [initially created](#startup-checks) and any time the configuration is [reloaded](#reloading-certificates). The resulting keystore and options are then made available via `TlsConfiguration::getKeyStore` and `TlsConfiguration::getKeyStoreOptions`.

**Example KeyStoreProvider**

```java
import io.quarkus.tls.KeyStoreAndKeyCertOptions;
import io.quarkus.tls.KeyStoreProvider;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemKeyCertOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped <1>
@Identifier("<name>") <2>
public class ExampleKeyStoreProvider implements KeyStoreProvider {

    @Inject <3>
    SecurityDatabase securityDatabase;

    @Override
    public KeyStoreAndKeyCertOptions getKeyStore(Vertx vertx) {
        try {
            var options = new PemKeyCertOptions()
                    .addCertValue(Buffer.buffer(securityDatabase.getEntityCertificate()))
                    .addKeyValue(Buffer.buffer(securityDatabase.getEntityPrivateKey()));
            var keyStore = options
                    .loadKeyStore(vertx);
            return new KeyStoreAndKeyCertOptions(keyStore, options);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

1. The CDI bean implementing the `KeyStoreProvider` interface can be `@ApplicationScoped`, `@Singleton`, or `@Dependent`.
2. Use the `@Identifier` qualifier to indicate the named TLS configuration that provides the keystore options. Omit the qualifier to indicate the default TLS configuration. You can also use `@Default` explicitly to indicate the default TLS configuration. For more information, see [Referencing a TLS configuration](#referencing-a-tls-configuration).
3. You can inject other CDI beans to access keystore material at runtime.

#### Other keystore types

If you need to use a keystore type other than PEM, PKCS12, or JKS (for example, Bouncy Castle FIPS `BCFKS`), use the `other` configuration:

```properties
quarkus.tls.key-store.other.type=bcfks
quarkus.tls.key-store.other.provider=BCFIPS
quarkus.tls.key-store.other.path=server-keystore.bcfks
quarkus.tls.key-store.other.password=secret
quarkus.tls.key-store.other.alias=mykey
quarkus.tls.key-store.other.alias-password=secret
```

The `type` property is required and corresponds to the type passed to `java.security.KeyStore.getInstance(type)`. The optional `provider` property specifies the Java security provider name. All other properties (`path`, `password`, `alias`, `alias-password`) work identically to the JKS and PKCS12 configurations. The `other` option is mutually exclusive with `pem`, `p12`, `jks`, and `KeyStoreProvider` beans.

##### Custom keystore factory

For keystore types that require custom loading logic beyond `KeyStore.getInstance()`, implement a `KeyStoreFactory` CDI bean annotated with `@Identifier` matching the `type` value:

```java
import io.quarkus.tls.KeyStoreAndKeyCertOptions;
import io.quarkus.tls.KeyStoreFactory;
import io.quarkus.tls.OtherKeyStoreConfigurationuration;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Identifier("my-custom-type")
public class MyKeyStoreFactory implements KeyStoreFactory {

    @Override
    public KeyStoreAndKeyCertOptions createKeyStore(OtherKeyStoreConfiguration config, Vertx vertx, String name) {
        // Custom keystore loading logic
        // Access arbitrary params via config.params()
    }
}
```

The factory receives the full `OtherKeyStoreConfiguration`, including arbitrary parameters set via `quarkus.tls.key-store.other.params.<key>=<value>`. If no factory bean is found for the given type, the standard `KeyStore.getInstance(type, provider)` loading is used as a fallback.

##### PKCS#11 example

PKCS#11 keystores are provider-managed (backed by a hardware security module or software token) and cannot be loaded from a file. Use a `KeyStoreFactory` to integrate a PKCS#11 keystore with the TLS registry:

```properties
quarkus.tls.key-store.other.type=PKCS11
quarkus.tls.key-store.other.params.config-file=/etc/pkcs11/provider.cfg
```

```java
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;

import io.quarkus.tls.KeyStoreAndKeyCertOptions;
import io.quarkus.tls.KeyStoreFactory;
import io.quarkus.tls.OtherKeyStoreConfigurationuration;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;
import io.vertx.core.net.KeyStoreOptionsBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Identifier("PKCS11") // <1>
public class Pkcs11KeyStoreFactory implements KeyStoreFactory {

    @Override
    public KeyStoreAndKeyCertOptions createKeyStore(OtherKeyStoreConfiguration config, Vertx vertx, String name) {
        try {
            String configFile = config.params().get("config-file"); // <2>
            Provider provider = Security.getProvider("SunPKCS11").configure(configFile);
            Security.addProvider(provider);

            KeyStore ks = KeyStore.getInstance("PKCS11", provider);
            char[] pin = config.password().map(String::toCharArray).orElse(null);
            ks.load(null, pin); // <3>

            var options = new KeyStoreOptionsBase() {} // <4>
                    .setKeyStoreType("PKCS11")
                    .setProviderName(provider.getName())
                    .setPassword(config.password().orElse(null));

            return new KeyStoreAndKeyCertOptions(ks, options);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load PKCS#11 keystore for '" + name + "'", e);
        }
    }
}
```

1. The `@Identifier` value must match the `type` in the configuration.
2. Arbitrary parameters from `quarkus.tls.key-store.other.params.*` are available via `config.params()`.
3. PKCS#11 keystores are loaded with `null` — the provider manages the underlying storage.
4. Use `KeyStoreOptionsBase` to let Vert.x create a `KeyManagerFactory` from the provider-backed keystore.

<a id="sni"></a>
#### SNI

Server Name Indication (SNI) is a TLS extension that makes it possible for a client to specify the hostname to which it attempts to connect during the TLS handshake. SNI enables a server to present multiple TLS certificates for different domains on a single IP address, facilitating secure communication in virtual hosting scenarios.

To enable SNI:

```properties
quarkus.tls.key-store.sni=true # Disabled by default
```

With SNI enabled, the client indicates the server name during the TLS handshake, which allows the server to select the appropriate certificate:

- When configuring the keystore with PEM files, multiple certificate (CRT) and key files must be provided. CRT is a common file extension for X.509 certificate files, typically in PEM (Privacy-Enhanced Mail) format. These files contain the public certificate.

- When configuring the keystore with a JKS or P12 file, the server selects the appropriate certificate based on the SNI hostname provided by the client during the TLS handshake. The server matches the SNI hostname with the common name (CN) or subject alternative names (SAN) configured in the certificates stored in the keystore. All keystore and alias passwords must be identical.

#### Credential providers

You can use a credential provider instead of specifying the keystore and alias passwords in the configuration.

A credential provider offers a way to retrieve the keystore and alias password. Quarkus uses the credential provider only when the configuration does not set the keystore password or alias password.

To configure a credential provider:

```properties
# The name of the credential bucket in the credentials provider
quarkus.tls.key-store.credentials-provider.name=my-credentials

# The name of the bean providing the credential provider (optional, using the default credential provider if not set)
quarkus.tls.key-store.credentials-provider.bean-name=my-credentials-provider

# The key used to retrieve the keystore password, `password` by default
quarkus.tls.key-store.credentials-provider.password-key=password

# The key used to retrieve the alias password, `alias-password` by default
quarkus.tls.key-store.credentials-provider.alias-password-key=alias-password
```

> [!IMPORTANT]
> The credential provider can only be used with PKCS12 and JKS keystores.

### Trust stores

Trust stores are used to store the certificates of the trusted parties. In regular TLS, the client uses a truststore to authenticate the server. With mutual TLS (mTLS), both the server and the client use truststores to authenticate each other.

#### PEM truststores

PEM truststores are composed of a list of `.crt` or `.pem` files. Each of them contains a certificate.

To configure a PEM truststore:

```properties
quarkus.tls.trust-store.pem.certs=ca.crt,ca2.pem
```

<a id="pkcs12_truststores"></a>
#### PKCS12 truststores

PKCS12 truststores are single-file containers for certificates. You can use the alias to select the appropriate certificate when multiple certificates are included.

To configure a PKCS12 truststore:

```properties
quarkus.tls.trust-store.p12.path=client-truststore.p12
quarkus.tls.trust-store.p12.password=password
quarkus.tls.trust-store.p12.alias=my-alias
```

`.p12` files are password-protected, so you need to provide the password to open the truststore. However, unlike keystores, the alias does not require a password because it contains a public certificate, not a private key.

#### JKS truststores

JKS truststores are single files that contain multiple certificates. You can use the alias to select the appropriate certificate when multiple certificates are present. However, avoid using the JKS format, because it is less secure than PKCS12.

To configure a JKS truststore:

```properties
quarkus.tls.trust-store.jks.path=client-truststore.jks
quarkus.tls.trust-store.jks.password=password
quarkus.tls.trust-store.jks.alias=my-alias
```

`.jks` files are password-protected, so you need to provide the password to open the truststore. However, unlike keystores, the alias does not require a password because it contains a public certificate, not a private key.

#### Provided truststores

If you need more control over the truststore used in a TLS configuration, you can provide a CDI bean implementing the `io.quarkus.tls.TrustStoreProvider` interface. Quarkus calls `TrustStoreProvider::getTrustStore` when the TLS configuration is [initially created](#startup-checks) and any time the configuration is [reloaded](#reloading-certificates). The resulting truststore and options are then made available via `TlsConfiguration::getTrustStore` and `TlsConfiguration::getTrustStoreOptions`.

**Example TrustStoreProvider**

```java
import io.quarkus.tls.TrustStoreAndTrustOptions;
import io.quarkus.tls.TrustStoreProvider;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemTrustOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped <1>
@Identifier("<name>") <2>
public class ExampleTrustStoreProvider implements TrustStoreProvider {

    @Inject <3>
    SecurityDatabase securityDatabase;

    @Override
    public TrustStoreAndTrustOptions getTrustStore(Vertx vertx) {
        try {
            var options = new PemTrustOptions()
                    .addCertValue(Buffer.buffer(securityDatabase.getTrustedRootCertificate()));
            var trustStore = options
                    .loadKeyStore(vertx);
            return new TrustStoreAndTrustOptions(trustStore, options);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

1. The CDI bean that implements the `TrustStoreProvider` interface can be `@ApplicationScoped`, `@Singleton`, or `@Dependent`.
2. Use the `@Identifier` qualifier to indicate the named TLS configuration that provides the truststore options. Omit the qualifier to indicate the default TLS configuration. You can also use `@Default` explicitly to indicate the default TLS configuration. For more information, see [Referencing a TLS configuration](#referencing-a-tls-configuration).
3. You can inject other CDI beans to access truststore material at runtime.

#### Other truststore types

If you need to use a truststore type other than PEM, PKCS12, or JKS, use the `other` configuration:

```properties
quarkus.tls.trust-store.other.type=bcfks
quarkus.tls.trust-store.other.provider=BCFIPS
quarkus.tls.trust-store.other.path=truststore.bcfks
quarkus.tls.trust-store.other.password=secret
quarkus.tls.trust-store.other.alias=myca
```

This works the same as the [\[Other keystore types\]](<#Other keystore types>) configuration described above. For custom loading logic, implement a `TrustStoreFactory` CDI bean annotated with `@Identifier` matching the `type` value.

#### Credential providers

Instead of passing the truststore password in the configuration, you can use a credential provider. A credential provider allows you to retrieve passwords and other credentials. Note that the credential provider is used only if the password is not set in the configuration.

To configure a credential provider:

```properties
# The name of the credential bucket in the credentials provider
quarkus.tls.trust-store.credentials-provider.name=my-credentials

# The name of the bean providing the credential provider (optional, using the default credential provider if not set)
quarkus.tls.trust-store.credentials-provider.bean-name=my-credentials-provider

# The key used to retrieve the truststore password, `password` by default
quarkus.tls.trust-store.credentials-provider.password-key=password
```

> [!IMPORTANT]
> The credential provider can only be used with PKCS12 and JKS truststores.

### Other properties

While keystores and truststores are the most important properties, there are other properties you can use to configure TLS.

> [!NOTE]
> While the following examples use the *default* configuration, you can use the *named* configuration by prefixing the properties with the configuration's name.

#### Cipher suites

Cipher suites are a list of ciphers that you can use during the TLS handshake. You can configure an ordered list of enabled cipher suites. If not configured, a reasonable default is selected from the built-in ciphers. However, when specified, your configuration precedes the default suite defined by the SSL engine in use.

To configure the cipher suites:

```properties
quarkus.tls.cipher-suites=TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384
```

#### TLS protocol versions

The TLS protocol versions are the list of protocols that can be used during the TLS handshake. Enabled TLS protocol versions are specified as an ordered list separated by commas. The relevant configuration property is `quarkus.tls.protocols` or `quarkus.tls.<name>.protocols` for named TLS configurations. For improved security, it defaults to `TLSv1.3` unless configured otherwise.

The available options are `TLSv1`, `TLSv1.1`, `TLSv1.2`, and `TLSv1.3`.

For example, to enable both `TLSv1.3` and `TLSv1.2`:

```properties
quarkus.tls."services-requiring-v1.2".protocols=TLSv1.3,TLSv1.2
```

> [!TIP]
> If only certain services require `TLSv1.2`, it is recommended to configure a dedicated, named TLS configuration for those services and keep the default configuration for services that support `TLSv1.3`.

#### Handshake timeout

When a TLS connection is established, the handshake phase is the first step. During this phase, the client and server exchange information to establish the connection, which typically includes the cipher suite, the TLS protocol version, and the certification validation.

To configure the timeout for the handshake phase:

```properties
quarkus.tls.handshake-timeout=10S # Default.
```

#### ALPN

Application-Layer Protocol Negotiation (ALPN) is a TLS extension that allows the client and server to negotiate which protocol they will use for communication during the TLS handshake. ALPN enables more efficient communication by allowing the client to indicate its preferred application protocol to the server before establishing the TLS connection.

This helps in scenarios such as HTTP/2, where multiple protocols can be available, enabling faster protocol selection.

ALPN is enabled by default.

- To disable it:

  ```properties
  quarkus.tls.alpn=false
  ```

  > [!WARNING]
  > Disabling ALPN is not recommended for non-experts, as it can lead to performance degradation, protocol negotiation issues, and unexpected behavior, particularly with protocols like HTTP/2. However, disabling ALPN can be useful for diagnosing native inconsistencies or testing performance in specific edge cases where protocol negotiation causes conflicts.


#### Post-Quantum Cryptography (PQC)

Post-Quantum Cryptography (PQC) protects TLS connections against attacks from quantum computers. Quarkus exposes two properties to control post-quantum key exchange, both of which apply to the HTTP server, HTTP client, REST client, and mailer.

##### Key exchange groups

The key exchange groups (also called *supported groups*) are the elliptic-curve or post-quantum algorithms the TLS engine advertises during the handshake. When not set, the engine uses its built-in defaults.

To advertise the hybrid post-quantum group `X25519MLKEM768` (combining X25519 with ML-KEM-768):

```properties
quarkus.tls.key-exchange-groups=X25519MLKEM768
```

You can specify multiple groups as a comma-separated list. Groups are tried in the order listed; the first one the peer also supports is used.

##### PQC enforcement policy

The enforcement policy controls how strictly the server requires post-quantum key exchange from clients.

```properties
quarkus.tls.pqc-enforcement-policy=relaxed # Default
```

The available values are:

**`relaxed`**\
No enforcement. Standard TLS negotiation applies, and no post-quantum groups are required. This is the default.

**`client-negotiated`**\
The server advertises post-quantum groups and prefers them, but accepts connections from clients that do not support them, falling back to classical key exchange. Use this when you want post-quantum key exchange where available without breaking existing clients.

**`strict`**\
The server only accepts connections that use a post-quantum key exchange group. Clients that do not negotiate a post-quantum group are rejected. Use this when all clients are known to support post-quantum cryptography.

> [!IMPORTANT]
> `pqc-enforcement-policy` does more than enforce post-quantum key exchange: it controls which SSL engine Vert.x selects. Setting `pqc-enforcement-policy` to `client-negotiated` or `strict` activates the native OpenSSL engine (requires OpenSSL 3.5+), which negotiates post-quantum key exchange groups. With the default `relaxed` policy, Vert.x uses the JDK SSL engine, which ignores any post-quantum key-exchange groups before JDK 27 — meaning that configuring post-quantum groups without also setting the enforcement policy results in no post-quantum key exchange at all. On JDK 27+, the JDK natively supports `X25519MLKEM768` and post-quantum key exchange works without changing the enforcement policy.
>
> Quarkus logs a warning at startup if you configure a post-quantum group with `relaxed` policy to help catch this misconfiguration.

> [!NOTE]
> When using `client-negotiated` or `strict`, `quarkus.tls.key-exchange-groups` must include at least one post-quantum-capable group such as `X25519MLKEM768`. Vert.x validates this at startup and throws an exception if no compatible engine or group is configured.

> [!WARNING]
> `strict` enforcement will reject any client that does not support the configured post-quantum group, including standard browsers and TLS clients that have not yet adopted post-quantum extensions. Ensure all clients are upgraded before enabling this mode.

> [!NOTE]
> Policy enforcement (`strict` and `client-negotiated`) is not supported when using the JDK SSL engine. If you are running on JDK 27+, or on JDK 25+ once the post-quantum backport ships (expected October 2026), you can still enable post-quantum key exchange by setting `quarkus.tls.key-exchange-groups` — the negotiation will work correctly without policy enforcement. For full enforcement, OpenSSL 3.5+ via the native SSL engine is required.

> [!IMPORTANT]
> Post-quantum enforcement (`client-negotiated` and `strict`) relies on a native OpenSSL 3.5+ library. Quarkus bundles this library only for **Linux x86_64** and **Linux aarch64**. In addition, `libapr-1` must be installed on the host OS (see [netty-tcnative prerequisites](https://netty.io/wiki/forked-tomcat-native.html)). Other operating systems and architectures (macOS, Windows, Alpine Linux, Linux on RISC-V, etc.) are **not supported**: attempting to use `client-negotiated` or `strict` on an unsupported platform will fail at runtime because the native OpenSSL engine is unavailable. On those platforms, use `relaxed` with JDK 27+ for post-quantum key exchange.

##### SSL engine override

By default, Vert.x selects the SSL engine automatically based on the enforcement policy:

- `client-negotiated` or `strict` activates the native OpenSSL engine (requires OpenSSL 3.5+).
- `relaxed` uses the JDK SSL engine.

You can override this selection with the `ssl-engine` property:

```properties
quarkus.tls.ssl-engine=openssl
```

The supported values are `openssl` (requires netty-tcnative on the classpath) and `jdkssl` (the standard JDK SSL engine). This property is useful for testing or when you need explicit control over the engine — for example, forcing `jdkssl` on JDK 27+ where the JDK natively supports post-quantum groups.

##### PQC and native mode

In native mode, quantum-safe TLS is not available using OpenSSL. Thus, the only way to leverage PQC-compliant key exchange groups is to use a JDK SSL engine that supports them. This are not yet available in GraalVM/Mandrel, but should be available in the near future.

##### PQC in containerized environments

When running in a container with a UBI image as the base, `libapr-1` isn't installed by default. So you need to install it explicitly. Without it, netty-tcnative cannot load the native OpenSSL engine at runtime, and PQC enforcement will fail even if OpenSSL 3.5+ is present.

Install `apr` explicitly in your `Containerfile`:

```dockerfile
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime
# Explicitly add the `apr` package. Refer to your base image documentation to install the apr package
RUN microdnf install -y apr && microdnf clean all
```

Below is an example of a complete `Containerfile`:

```dockerfile
FROM registry.access.redhat.com/ubi10/openjdk-25-runtime:latest

ENV LANGUAGE='en_US:en'

USER root
RUN microdnf install -y apr && microdnf clean all
USER 185

# We make four distinct layers so if there are application changes the library layers can be re-used
COPY --chown=185 target/quarkus-app/lib/ /deployments/lib/
COPY --chown=185 target/quarkus-app/*.jar /deployments/
COPY --chown=185 target/quarkus-app/app/ /deployments/app/
COPY --chown=185 target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080 8443
USER 185
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
```

##### PQC availability summary

| Mode | Java Runtime | SSL Engine | PQC support | PQC enforcement available | Prerequisites |
| --- | --- | --- | --- | --- | --- |
| JVM | Any | OpenSSL (netty-tcnative) | Yes | Yes | OpenSSL 3.5+; `libapr-1` (`microdnf install apr` on UBI) |
| JVM | \< 27 (25\*) | JDK SSL | No | No | — |
| JVM | 27+ (25+\*) | JDK SSL | Yes | Yes | JDK 27+ (or 25+ after backport) |
| Native | Any | OpenSSL (netty-tcnative) | — | No | Engine unavailable in native mode |

*{asterisk} JDK 25 support pending backport (expected October 2026).*

#### Certificate Revocation List (CRL)

A Certificate Revocation List (CRL) is a list of certificates that the issuing Certificate Authority (CA) revoked before their scheduled expiration date. When a certificate is compromised, no longer needed, or deemed invalid, the CA adds it to the CRL to inform relying parties not to trust it anymore.

You can configure the CRL to include the list of certificate files you no longer trust in DER or PKCS#7 (P7B) format.

- For the DER format, pass DER-encoded CRLs.
- For the PKCS#7 format, pass the `SignedData` object, where the only significant field is `crls`.

To configure the CRL:

```properties
quarkus.tls.certificate-revocation-list=ca.crl, ca2.crl
```

#### Trusting all certificates and hostname verification

You can configure your TLS connection to trust all certificates and disable hostname verification. Note that these are two different processes:

- Trusting all certificates ignores certificate validation, so the client trusts all certificates. This method is useful for testing with self-signed certificates, but do not use it in production.

- Hostname verification is the process of verifying the server's identity.

It is useful to prevent man-in-the-middle attacks and often defaults to `HTTPS` or `LDAPS`.

> [!IMPORTANT]
> These two properties should not be used in production.

To trust all certificates:

```properties
quarkus.tls.trust-all=true
```

To disable hostname verification:

```properties
quarkus.tls.hostname-verification-algorithm=NONE
```

<a id="client-renegotiation"></a>
#### Preventing client renegotiation

Client-initiated renegotiation allows a client to request new session parameters, such as a different cipher suite, during an established TLS connection. While this feature can provide flexibility, it also introduces a potential security risk when using TLS 1.2.

When a client initiates a new TLS handshake, the server typically consumes significantly more CPU resources than the client. This resource asymmetry can be exploited to launch denial-of-service (DoS) attacks, overwhelming the server with renegotiation requests.

TLS 1.3 completely removes support for renegotiation, effectively closing this potential attack vector.

- To secure TLS 1.2 and earlier, set `jdk.tls.rejectClientInitiatedRenegotiation` to `true` to prevent client-initiated renegotiation.

  ```bash
  # JVM mode:
  java -Djdk.tls.rejectClientInitiatedRenegotiation=true -jar ...
  # Native mode
  ./application -Djdk.tls.rejectClientInitiatedRenegotiation=true
  ```

  If you are using the Quarkus-provided `Dockerfile` in JVM mode, you can disable renegotiation by adding the property to the `JAVA_OPTS_APPEND` environment variable:

  ```dockerfile
  ENV JAVA_OPTS_APPPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager -Djdk.tls.rejectClientInitiatedRenegotiation=true"
  ```


### Configuration reference

The following table lists the supported properties:

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-tls-registry_quarkus-tls-lets-encrypt-enabled"></a> [`quarkus.tls.lets-encrypt.enabled`](#quarkus-tls-registry_quarkus-tls-lets-encrypt-enabled) config_property_copy_button:+++quarkus.tls.lets-encrypt.enabled+++[]<br><br>Set to `true` to enable let's encrypt support.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_LETS_ENCRYPT_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-pem-order"></a> [`quarkus.tls.key-store.pem.order`](#quarkus-tls-registry_quarkus-tls-key-store-pem-order) config_property_copy_button:+++quarkus.tls.key-store.pem.order+++[]<br><br>The order of the key/cert files, based on the names in the `keyCerts` map.<br><br>By default, Quarkus sorts the key using a lexicographical order. This property allows you to specify the order of the key/cert files.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_PEM_ORDER+++[] | list of string |  |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-p12-path"></a> [`quarkus.tls.key-store.p12.path`](#quarkus-tls-registry_quarkus-tls-key-store-p12-path) config_property_copy_button:+++quarkus.tls.key-store.p12.path+++[]<br><br>Path to the key store file (P12 / PFX format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_P12_PATH+++[] | path | required [exclamation circle] |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-p12-password"></a> [`quarkus.tls.key-store.p12.password`](#quarkus-tls-registry_quarkus-tls-key-store-p12-password) config_property_copy_button:+++quarkus.tls.key-store.p12.password+++[]<br><br>Password of the key store. When not set, the password must be retrieved from the credential provider.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_P12_PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-p12-alias"></a> [`quarkus.tls.key-store.p12.alias`](#quarkus-tls-registry_quarkus-tls-key-store-p12-alias) config_property_copy_button:+++quarkus.tls.key-store.p12.alias+++[]<br><br>Alias of the private key and certificate in the key store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_P12_ALIAS+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-p12-alias-password"></a> [`quarkus.tls.key-store.p12.alias-password`](#quarkus-tls-registry_quarkus-tls-key-store-p12-alias-password) config_property_copy_button:+++quarkus.tls.key-store.p12.alias-password+++[]<br><br>Password of the alias in the key store. If not set, the password will be retrieved from the credential provider.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_P12_ALIAS_PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-p12-provider"></a> [`quarkus.tls.key-store.p12.provider`](#quarkus-tls-registry_quarkus-tls-key-store-p12-provider) config_property_copy_button:+++quarkus.tls.key-store.p12.provider+++[]<br><br>Provider of the key store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_P12_PROVIDER+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-jks-path"></a> [`quarkus.tls.key-store.jks.path`](#quarkus-tls-registry_quarkus-tls-key-store-jks-path) config_property_copy_button:+++quarkus.tls.key-store.jks.path+++[]<br><br>Path to the keystore file (JKS format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_JKS_PATH+++[] | path | required [exclamation circle] |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-jks-password"></a> [`quarkus.tls.key-store.jks.password`](#quarkus-tls-registry_quarkus-tls-key-store-jks-password) config_property_copy_button:+++quarkus.tls.key-store.jks.password+++[]<br><br>Password of the key store. When not set, the password must be retrieved from the credential provider.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_JKS_PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-jks-alias"></a> [`quarkus.tls.key-store.jks.alias`](#quarkus-tls-registry_quarkus-tls-key-store-jks-alias) config_property_copy_button:+++quarkus.tls.key-store.jks.alias+++[]<br><br>Alias of the private key and certificate in the key store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_JKS_ALIAS+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-jks-alias-password"></a> [`quarkus.tls.key-store.jks.alias-password`](#quarkus-tls-registry_quarkus-tls-key-store-jks-alias-password) config_property_copy_button:+++quarkus.tls.key-store.jks.alias-password+++[]<br><br>Password of the alias in the key store. When not set, the password may be retrieved from the credential provider.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_JKS_ALIAS_PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-jks-provider"></a> [`quarkus.tls.key-store.jks.provider`](#quarkus-tls-registry_quarkus-tls-key-store-jks-provider) config_property_copy_button:+++quarkus.tls.key-store.jks.provider+++[]<br><br>Provider of the key store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_JKS_PROVIDER+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-other-type"></a> [`quarkus.tls.key-store.other.type`](#quarkus-tls-registry_quarkus-tls-key-store-other-type) config_property_copy_button:+++quarkus.tls.key-store.other.type+++[]<br><br>The key store type, passed to `java.security.KeyStore.getInstance(type)`.<br><br>This value also serves as the lookup key for a `KeyStoreFactory` CDI bean via `@Identifier`. If a matching factory is found, it takes priority over the standard file-based loading.<br><br>Examples: `BCFKS`, `PKCS11`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_OTHER_TYPE+++[] | string | required [exclamation circle] |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-other-provider"></a> [`quarkus.tls.key-store.other.provider`](#quarkus-tls-registry_quarkus-tls-key-store-other-provider) config_property_copy_button:+++quarkus.tls.key-store.other.provider+++[]<br><br>The Java security provider name, passed to `java.security.KeyStore.getInstance(type, provider)`.<br><br>Used by the standard fallback loading. If not set, the default provider for the given type is used. A factory may also read this value but is not required to.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_OTHER_PROVIDER+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-other-path"></a> [`quarkus.tls.key-store.other.path`](#quarkus-tls-registry_quarkus-tls-key-store-other-path) config_property_copy_button:+++quarkus.tls.key-store.other.path+++[]<br><br>Path to the key store file.<br><br>Required when using the standard fallback (no factory). When a factory handles the loading, this value is available to the factory via this config but is not used by the TLS registry itself.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_OTHER_PATH+++[] | path |  |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-other-password"></a> [`quarkus.tls.key-store.other.password`](#quarkus-tls-registry_quarkus-tls-key-store-other-password) config_property_copy_button:+++quarkus.tls.key-store.other.password+++[]<br><br>Password of the key store.<br><br>Used by the standard fallback to load the store. A factory may also read this value. When not set, the password can be retrieved from the credential provider configured on the parent key store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_OTHER_PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-other-alias"></a> [`quarkus.tls.key-store.other.alias`](#quarkus-tls-registry_quarkus-tls-key-store-other-alias) config_property_copy_button:+++quarkus.tls.key-store.other.alias+++[]<br><br>Alias of the private key and certificate in the key store.<br><br>Used by the standard fallback to select and validate a specific entry. A factory may also read this value but is not required to.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_OTHER_ALIAS+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-other-alias-password"></a> [`quarkus.tls.key-store.other.alias-password`](#quarkus-tls-registry_quarkus-tls-key-store-other-alias-password) config_property_copy_button:+++quarkus.tls.key-store.other.alias-password+++[]<br><br>Password of the alias in the key store.<br><br>Used by the standard fallback to verify the private key is recoverable. When not set, the password can be retrieved from the credential provider configured on the parent key store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_OTHER_ALIAS_PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-other-params-params"></a> [`quarkus.tls.key-store.other.params."params"`](#quarkus-tls-registry_quarkus-tls-key-store-other-params-params) config_property_copy_button:+++quarkus.tls.key-store.other.params."params"+++[]<br><br>Arbitrary parameters available to a `KeyStoreFactory` CDI bean via `config.params()`.<br><br>Ignored by the standard fallback. Set via `quarkus.tls.key-store.other.params.<key>=<value>`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_OTHER_PARAMS__PARAMS_+++[] | Map\<String,String> |  |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-sni"></a> [`quarkus.tls.key-store.sni`](#quarkus-tls-registry_quarkus-tls-key-store-sni) config_property_copy_button:+++quarkus.tls.key-store.sni+++[]<br><br>Enables Server Name Indication (SNI).<br><br>Server Name Indication (SNI) is a TLS extension that allows a client to specify the hostname it is attempting to connect to during the TLS handshake. This enables a server to present different SSL certificates for multiple domains on a single IP address, facilitating secure communication for virtual hosting scenarios.<br><br>With this setting enabled, the client indicate the server name during the TLS handshake, allowing the server to select the right certificate.<br><br>When configuring the keystore with PEM files, multiple CRT/Key must be given. When configuring the keystore with a JKS or a P12 file, it selects one alias based on the SNI hostname. In this case, all the keystore password and alias password must be the same (configured with the `password` and `alias-password` properties. Do not set the `alias` property.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_SNI+++[] | boolean | `false` |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-credentials-provider-name"></a> [`quarkus.tls.key-store.credentials-provider.name`](#quarkus-tls-registry_quarkus-tls-key-store-credentials-provider-name) config_property_copy_button:+++quarkus.tls.key-store.credentials-provider.name+++[]<br><br>The name of the "credential" bucket (map key -> passwords) to retrieve from the `io.quarkus.credentials.CredentialsProvider`. If not set, the credential provider will not be used.<br><br>A credential provider offers a way to retrieve the key store password as well as alias password. Note that the credential provider is only used if the passwords are not set in the configuration.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_CREDENTIALS_PROVIDER_NAME+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-credentials-provider-bean-name"></a> [`quarkus.tls.key-store.credentials-provider.bean-name`](#quarkus-tls-registry_quarkus-tls-key-store-credentials-provider-bean-name) config_property_copy_button:+++quarkus.tls.key-store.credentials-provider.bean-name+++[]<br><br>The name of the bean providing the credential provider.<br><br>The name is used to select the credential provider to use. The credential provider must be exposed as a CDI bean and with the `@Named` annotation set to the configured name to be selected.<br><br>If not set, the default credential provider is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_CREDENTIALS_PROVIDER_BEAN_NAME+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-credentials-provider-password-key"></a> [`quarkus.tls.key-store.credentials-provider.password-key`](#quarkus-tls-registry_quarkus-tls-key-store-credentials-provider-password-key) config_property_copy_button:+++quarkus.tls.key-store.credentials-provider.password-key+++[]<br><br>The key used to retrieve the key store password.<br><br>If the selected credential provider does not support the key, the password is not retrieved. Otherwise, the retrieved value is used to open the key store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_CREDENTIALS_PROVIDER_PASSWORD_KEY+++[] | string | `password` |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-credentials-provider-alias-password-key"></a> [`quarkus.tls.key-store.credentials-provider.alias-password-key`](#quarkus-tls-registry_quarkus-tls-key-store-credentials-provider-alias-password-key) config_property_copy_button:+++quarkus.tls.key-store.credentials-provider.alias-password-key+++[]<br><br>The key used to retrieve the key store alias password.<br><br>If the selected credential provider does not contain the key, the alias password is not retrieved. Otherwise, the retrieved value is used to access the alias `private key` from the key store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_CREDENTIALS_PROVIDER_ALIAS_PASSWORD_KEY+++[] | string | `alias-password` |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-pem-certs"></a> [`quarkus.tls.trust-store.pem.certs`](#quarkus-tls-registry_quarkus-tls-trust-store-pem-certs) config_property_copy_button:+++quarkus.tls.trust-store.pem.certs+++[]<br><br>List of the trusted cert paths (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_PEM_CERTS+++[] | list of path |  |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-pem-cert-dirs"></a> [`quarkus.tls.trust-store.pem.cert-dirs`](#quarkus-tls-registry_quarkus-tls-trust-store-pem-cert-dirs) config_property_copy_button:+++quarkus.tls.trust-store.pem.cert-dirs+++[]<br><br>List of the directories with the trusted certificates (Pem format). The configured directory can be empty. Any file in the configured directories will be treated as a trusted certificate in the Pem format.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_PEM_CERT_DIRS+++[] | list of path |  |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-p12-path"></a> [`quarkus.tls.trust-store.p12.path`](#quarkus-tls-registry_quarkus-tls-trust-store-p12-path) config_property_copy_button:+++quarkus.tls.trust-store.p12.path+++[]<br><br>Path to the trust store file (P12 / PFX format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_P12_PATH+++[] | path | required [exclamation circle] |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-p12-password"></a> [`quarkus.tls.trust-store.p12.password`](#quarkus-tls-registry_quarkus-tls-trust-store-p12-password) config_property_copy_button:+++quarkus.tls.trust-store.p12.password+++[]<br><br>Password of the trust store. If not set, the password must be retrieved from the credential provider.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_P12_PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-p12-alias"></a> [`quarkus.tls.trust-store.p12.alias`](#quarkus-tls-registry_quarkus-tls-trust-store-p12-alias) config_property_copy_button:+++quarkus.tls.trust-store.p12.alias+++[]<br><br>Alias of the trust store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_P12_ALIAS+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-p12-provider"></a> [`quarkus.tls.trust-store.p12.provider`](#quarkus-tls-registry_quarkus-tls-trust-store-p12-provider) config_property_copy_button:+++quarkus.tls.trust-store.p12.provider+++[]<br><br>Provider of the trust store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_P12_PROVIDER+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-jks-path"></a> [`quarkus.tls.trust-store.jks.path`](#quarkus-tls-registry_quarkus-tls-trust-store-jks-path) config_property_copy_button:+++quarkus.tls.trust-store.jks.path+++[]<br><br>Path to the trust store file (JKS format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_JKS_PATH+++[] | path | required [exclamation circle] |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-jks-password"></a> [`quarkus.tls.trust-store.jks.password`](#quarkus-tls-registry_quarkus-tls-trust-store-jks-password) config_property_copy_button:+++quarkus.tls.trust-store.jks.password+++[]<br><br>Password of the trust store. If not set, the password must be retrieved from the credential provider.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_JKS_PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-jks-alias"></a> [`quarkus.tls.trust-store.jks.alias`](#quarkus-tls-registry_quarkus-tls-trust-store-jks-alias) config_property_copy_button:+++quarkus.tls.trust-store.jks.alias+++[]<br><br>Alias of the key in the trust store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_JKS_ALIAS+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-jks-provider"></a> [`quarkus.tls.trust-store.jks.provider`](#quarkus-tls-registry_quarkus-tls-trust-store-jks-provider) config_property_copy_button:+++quarkus.tls.trust-store.jks.provider+++[]<br><br>Provider of the trust store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_JKS_PROVIDER+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-other-type"></a> [`quarkus.tls.trust-store.other.type`](#quarkus-tls-registry_quarkus-tls-trust-store-other-type) config_property_copy_button:+++quarkus.tls.trust-store.other.type+++[]<br><br>The trust store type, passed to `java.security.KeyStore.getInstance(type)`.<br><br>This value also serves as the lookup key for a `TrustStoreFactory` CDI bean via `@Identifier`. If a matching factory is found, it takes priority over the standard file-based loading.<br><br>Examples: `BCFKS`, `PKCS11`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_OTHER_TYPE+++[] | string | required [exclamation circle] |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-other-provider"></a> [`quarkus.tls.trust-store.other.provider`](#quarkus-tls-registry_quarkus-tls-trust-store-other-provider) config_property_copy_button:+++quarkus.tls.trust-store.other.provider+++[]<br><br>The Java security provider name, passed to `java.security.KeyStore.getInstance(type, provider)`.<br><br>Used by the standard fallback loading. If not set, the default provider for the given type is used. A factory may also read this value but is not required to.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_OTHER_PROVIDER+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-other-path"></a> [`quarkus.tls.trust-store.other.path`](#quarkus-tls-registry_quarkus-tls-trust-store-other-path) config_property_copy_button:+++quarkus.tls.trust-store.other.path+++[]<br><br>Path to the trust store file.<br><br>Required when using the standard fallback (no factory). When a factory handles the loading, this value is available to the factory via this config but is not used by the TLS registry itself.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_OTHER_PATH+++[] | path |  |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-other-password"></a> [`quarkus.tls.trust-store.other.password`](#quarkus-tls-registry_quarkus-tls-trust-store-other-password) config_property_copy_button:+++quarkus.tls.trust-store.other.password+++[]<br><br>Password of the trust store.<br><br>Used by the standard fallback to load the store. A factory may also read this value. When not set, the password can be retrieved from the credential provider configured on the parent trust store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_OTHER_PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-other-alias"></a> [`quarkus.tls.trust-store.other.alias`](#quarkus-tls-registry_quarkus-tls-trust-store-other-alias) config_property_copy_button:+++quarkus.tls.trust-store.other.alias+++[]<br><br>Alias of the certificate in the trust store.<br><br>Used by the standard fallback to validate that a specific certificate exists. A factory may also read this value but is not required to.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_OTHER_ALIAS+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-other-params-params"></a> [`quarkus.tls.trust-store.other.params."params"`](#quarkus-tls-registry_quarkus-tls-trust-store-other-params-params) config_property_copy_button:+++quarkus.tls.trust-store.other.params."params"+++[]<br><br>Arbitrary parameters available to a `TrustStoreFactory` CDI bean via `config.params()`.<br><br>Ignored by the standard fallback. Set via `quarkus.tls.trust-store.other.params.<key>=<value>`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_OTHER_PARAMS__PARAMS_+++[] | Map\<String,String> |  |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-certificate-expiration-policy"></a> [`quarkus.tls.trust-store.certificate-expiration-policy`](#quarkus-tls-registry_quarkus-tls-trust-store-certificate-expiration-policy) config_property_copy_button:+++quarkus.tls.trust-store.certificate-expiration-policy+++[]<br><br>Enforce certificate expiration. When enabled, the certificate expiration date is verified and the certificate (or any certificate in the chain) is rejected if it is expired.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_CERTIFICATE_EXPIRATION_POLICY+++[] | tooltip:ignore[Ignore the expiration date.], tooltip:warn[Log a warning when the certificate is expired.], tooltip:reject[Reject the certificate if it is expired.] | tooltip:warn[Log a warning when the certificate is expired.] |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-credentials-provider-name"></a> [`quarkus.tls.trust-store.credentials-provider.name`](#quarkus-tls-registry_quarkus-tls-trust-store-credentials-provider-name) config_property_copy_button:+++quarkus.tls.trust-store.credentials-provider.name+++[]<br><br>The name of the "credential" bucket (map key -> passwords) to retrieve from the `io.quarkus.credentials.CredentialsProvider`. If not set, the credential provider will not be used.<br><br>A credential provider offers a way to retrieve the key store password as well as alias password. Note that the credential provider is only used if the passwords are not set in the configuration.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_CREDENTIALS_PROVIDER_NAME+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-credentials-provider-bean-name"></a> [`quarkus.tls.trust-store.credentials-provider.bean-name`](#quarkus-tls-registry_quarkus-tls-trust-store-credentials-provider-bean-name) config_property_copy_button:+++quarkus.tls.trust-store.credentials-provider.bean-name+++[]<br><br>The name of the bean providing the credential provider.<br><br>The name is used to select the credential provider to use. The credential provider must be exposed as a CDI bean and with the `@Named` annotation set to the configured name to be selected.<br><br>If not set, the default credential provider is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_CREDENTIALS_PROVIDER_BEAN_NAME+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-trust-store-credentials-provider-password-key"></a> [`quarkus.tls.trust-store.credentials-provider.password-key`](#quarkus-tls-registry_quarkus-tls-trust-store-credentials-provider-password-key) config_property_copy_button:+++quarkus.tls.trust-store.credentials-provider.password-key+++[]<br><br>The key used to retrieve the trust store password.<br><br>If the selected credential provider does not contain the configured key, the password is not retrieved. Otherwise, the retrieved value is used to open the trust store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_STORE_CREDENTIALS_PROVIDER_PASSWORD_KEY+++[] | string | `password` |
| <a id="quarkus-tls-registry_quarkus-tls-cipher-suites"></a> [`quarkus.tls.cipher-suites`](#quarkus-tls-registry_quarkus-tls-cipher-suites) config_property_copy_button:+++quarkus.tls.cipher-suites+++[]<br><br>Sets the ordered list of enabled cipher suites. If none is given, a reasonable default is selected from the built-in ciphers.<br><br>When suites are set, it takes precedence over the default suite defined by the `SSLEngineOptions` in use.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_CIPHER_SUITES+++[] | list of string |  |
| <a id="quarkus-tls-registry_quarkus-tls-protocols"></a> [`quarkus.tls.protocols`](#quarkus-tls-registry_quarkus-tls-protocols) config_property_copy_button:+++quarkus.tls.protocols+++[]<br><br>Sets the ordered list of enabled TLS protocols.<br><br>If not set, it defaults to `"TLSv1.3"`. The following list of protocols are supported: `TLSv1, TLSv1.1, TLSv1.2, TLSv1.3`. To enable `TLSv1.3` and `TLSv1.2`, set the value to `to "TLSv1.3, TLSv1.2"`.<br><br>Note that setting an empty list, and enabling TLS is invalid. You must at least have one protocol.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_PROTOCOLS+++[] | list of string | `TLSv1.3` |
| <a id="quarkus-tls-registry_quarkus-tls-handshake-timeout"></a> [`quarkus.tls.handshake-timeout`](#quarkus-tls-registry_quarkus-tls-handshake-timeout) config_property_copy_button:+++quarkus.tls.handshake-timeout+++[]<br><br>The timeout for the TLS handshake phase.<br><br>If not set, it defaults to 10 seconds.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_HANDSHAKE_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-tls-registry_quarkus-tls) | `10S` |
| <a id="quarkus-tls-registry_quarkus-tls-alpn"></a> [`quarkus.tls.alpn`](#quarkus-tls-registry_quarkus-tls-alpn) config_property_copy_button:+++quarkus.tls.alpn+++[]<br><br>Enables the Application-Layer Protocol Negotiation (ALPN).<br><br>Application-Layer Protocol Negotiation is a TLS extension that allows the client and server during the TLS handshake to negotiate which protocol they will use for communication. ALPN enables more efficient communication by allowing the client to indicate its preferred application protocol to the server before the TLS connection is established. This helps in scenarios such as HTTP/2 where multiple protocols may be available, allowing for faster protocol selection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_ALPN+++[] | boolean | `true` |
| <a id="quarkus-tls-registry_quarkus-tls-pqc-enforcement-policy"></a> [`quarkus.tls.pqc-enforcement-policy`](#quarkus-tls-registry_quarkus-tls-pqc-enforcement-policy) config_property_copy_button:+++quarkus.tls.pqc-enforcement-policy+++[]<br><br>Sets the PQC enforcement policy.<br><br>`STRICT`: a connection will be refused unless both the client and the server support a PQC-capable group (`X25519MLKEM768`, `SecP256r1MLKEM768`, `SecP384r1MLKEM1024`). The server requires PQC-capable clients.<br><br>`CLIENT_NEGOTIATED`: the server advertises PQC groups but will not refuse a connection if the client does not support them, allowing fallback to classical key exchange.<br><br>`RELAXED`: no PQC enforcement; standard TLS key exchange negotiation applies.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_PQC_ENFORCEMENT_POLICY+++[] | tooltip:strict[A connection is refused unless both the client and the server negotiate a PQC key exchange (`X25519MLKEM768`,opts=`SecP256r1MLKEM768`,`SecP384r1MLKEM1024`). Clients that do not advertise PQC support are rejected during the TLS handshake.], tooltip:client-negotiated[The server advertises PQC key exchange groups but does not require the client to support them. If the client does not support PQC,opts=the handshake falls back to classical key exchange.], tooltip:relaxed[No PQC enforcement; standard TLS key exchange negotiation applies. The server neither advertises nor requires PQC groups.] | tooltip:relaxed[No PQC enforcement; standard TLS key exchange negotiation applies. The server neither advertises nor requires PQC groups.] |
| <a id="quarkus-tls-registry_quarkus-tls-key-exchange-groups"></a> [`quarkus.tls.key-exchange-groups`](#quarkus-tls-registry_quarkus-tls-key-exchange-groups) config_property_copy_button:+++quarkus.tls.key-exchange-groups+++[]<br><br>Sets the ordered list of enabled TLS key exchange groups (supported groups). If not set, the default groups defined by the `SSLEngineOptions` in use are used.<br><br>Example values: `X25519MLKEM768`, `X25519`, `P-256`. When set, these groups take precedence over the engine defaults.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_EXCHANGE_GROUPS+++[] | list of string |  |
| <a id="quarkus-tls-registry_quarkus-tls-ssl-engine"></a> [`quarkus.tls.ssl-engine`](#quarkus-tls-registry_quarkus-tls-ssl-engine) config_property_copy_button:+++quarkus.tls.ssl-engine+++[]<br><br>Forces the SSL engine to use.<br><br>When not set, Vert.x selects the engine automatically: if `pqc-enforcement-policy` is `strict` or `client-negotiated`, it prefers the JDK engine (JDK 27{plus}) or falls back to OpenSSL. For `relaxed` policy it uses the JDK engine by default.<br><br>Use `OPENSSL` to force netty-tcnative (requires the native library on the classpath). Use `JDKSSL` to force the standard JDK SSL engine.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_SSL_ENGINE+++[] | tooltip:openssl[Use the OpenSsl Engine,opts=requires netty-tcnative.], tooltip:jdkssl[Use the default JdkSsl Engine.] |  |
| <a id="quarkus-tls-registry_quarkus-tls-certificate-revocation-list"></a> [`quarkus.tls.certificate-revocation-list`](#quarkus-tls-registry_quarkus-tls-certificate-revocation-list) config_property_copy_button:+++quarkus.tls.certificate-revocation-list+++[]<br><br>Sets the list of revoked certificates (paths to files).<br><br>A Certificate Revocation List (CRL) is a list of digital certificates that have been revoked by the issuing Certificate Authority (CA) before their scheduled expiration date. When a certificate is compromised, no longer needed, or deemed invalid for any reason, the CA adds it to the CRL to inform relying parties not to trust the certificate anymore.<br><br>Two formats are allowed: DER and PKCS++<mark>++7 (also known as P7B). When using the DER format, you must pass DER-encoded CRLs. When using the PKCS++</mark>++7 format, you must pass PKCS++#++7 `SignedData` object, with the only significant field being `crls`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_CERTIFICATE_REVOCATION_LIST+++[] | list of path |  |
| <a id="quarkus-tls-registry_quarkus-tls-trust-all"></a> [`quarkus.tls.trust-all`](#quarkus-tls-registry_quarkus-tls-trust-all) config_property_copy_button:+++quarkus.tls.trust-all+++[]<br><br>If set to `true`, the server trusts all certificates.<br><br>This is useful for testing, but should not be used in production.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_TRUST_ALL+++[] | boolean | `false` |
| <a id="quarkus-tls-registry_quarkus-tls-hostname-verification-algorithm"></a> [`quarkus.tls.hostname-verification-algorithm`](#quarkus-tls-registry_quarkus-tls-hostname-verification-algorithm) config_property_copy_button:+++quarkus.tls.hostname-verification-algorithm+++[]<br><br>The hostname verification algorithm to use in case the server's identity should be checked. Should be `HTTPS` (default), `LDAPS` or `NONE`.<br><br>If set to `NONE`, it does not verify the hostname.<br><br>If not set, the configured extension decides the default algorithm to use. For example, for HTTP, it will be "HTTPS". For TCP, it can depend on the protocol. Nevertheless, it is recommended to set it to "HTTPS" or "LDAPS".<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_HOSTNAME_VERIFICATION_ALGORITHM+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-reload-period"></a> [`quarkus.tls.reload-period`](#quarkus-tls-registry_quarkus-tls-reload-period) config_property_copy_button:+++quarkus.tls.reload-period+++[]<br><br>When configured, the server will reload the certificates (from the file system for example) and fires a `CertificateUpdatedEvent` if the reload is successful<br><br>This property configures the period to reload the certificates. IF not set, the certificates won't be reloaded automatically. However, the application can still trigger the reload manually using the `io.quarkus.tls.TlsConfiguration#reload()` method, and then fire the `CertificateUpdatedEvent` manually.<br><br>The fired event is used to notify the application that the certificates have been updated, and thus proceed with the actual switch of certificates.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_RELOAD_PERIOD+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-tls-registry_quarkus-tls) |  |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-pem-key-certs-key"></a> [`quarkus.tls.key-store.pem."key-certs".key`](#quarkus-tls-registry_quarkus-tls-key-store-pem-key-certs-key) config_property_copy_button:+++quarkus.tls.key-store.pem."key-certs".key+++[]<br><br>The path to the key file (in PEM format: PKCS++<mark>++8, PKCS++</mark>++1 or encrypted PKCS++#++8).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_PEM__KEY_CERTS__KEY+++[] | path | required [exclamation circle] |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-pem-key-certs-cert"></a> [`quarkus.tls.key-store.pem."key-certs".cert`](#quarkus-tls-registry_quarkus-tls-key-store-pem-key-certs-cert) config_property_copy_button:+++quarkus.tls.key-store.pem."key-certs".cert+++[]<br><br>The path to the certificate file (in PEM format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_PEM__KEY_CERTS__CERT+++[] | path | required [exclamation circle] |
| <a id="quarkus-tls-registry_quarkus-tls-key-store-pem-key-certs-password"></a> [`quarkus.tls.key-store.pem."key-certs".password`](#quarkus-tls-registry_quarkus-tls-key-store-pem-key-certs-password) config_property_copy_button:+++quarkus.tls.key-store.pem."key-certs".password+++[]<br><br>When the key is encrypted (encrypted PKCS++#++8), the password to decrypt it.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS_KEY_STORE_PEM__KEY_CERTS__PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-pem-key-certs-key"></a> [`quarkus.tls."tls-bucket-name".key-store.pem."key-certs".key`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-pem-key-certs-key) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.pem."key-certs".key+++[]<br><br>The path to the key file (in PEM format: PKCS++<mark>++8, PKCS++</mark>++1 or encrypted PKCS++#++8).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_PEM__KEY_CERTS__KEY+++[] | path | required [exclamation circle] |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-pem-key-certs-cert"></a> [`quarkus.tls."tls-bucket-name".key-store.pem."key-certs".cert`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-pem-key-certs-cert) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.pem."key-certs".cert+++[]<br><br>The path to the certificate file (in PEM format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_PEM__KEY_CERTS__CERT+++[] | path | required [exclamation circle] |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-pem-key-certs-password"></a> [`quarkus.tls."tls-bucket-name".key-store.pem."key-certs".password`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-pem-key-certs-password) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.pem."key-certs".password+++[]<br><br>When the key is encrypted (encrypted PKCS++#++8), the password to decrypt it.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_PEM__KEY_CERTS__PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-pem-order"></a> [`quarkus.tls."tls-bucket-name".key-store.pem.order`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-pem-order) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.pem.order+++[]<br><br>The order of the key/cert files, based on the names in the `keyCerts` map.<br><br>By default, Quarkus sorts the key using a lexicographical order. This property allows you to specify the order of the key/cert files.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_PEM_ORDER+++[] | list of string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-p12-path"></a> [`quarkus.tls."tls-bucket-name".key-store.p12.path`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-p12-path) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.p12.path+++[]<br><br>Path to the key store file (P12 / PFX format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_P12_PATH+++[] | path | required [exclamation circle] |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-p12-password"></a> [`quarkus.tls."tls-bucket-name".key-store.p12.password`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-p12-password) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.p12.password+++[]<br><br>Password of the key store. When not set, the password must be retrieved from the credential provider.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_P12_PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-p12-alias"></a> [`quarkus.tls."tls-bucket-name".key-store.p12.alias`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-p12-alias) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.p12.alias+++[]<br><br>Alias of the private key and certificate in the key store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_P12_ALIAS+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-p12-alias-password"></a> [`quarkus.tls."tls-bucket-name".key-store.p12.alias-password`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-p12-alias-password) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.p12.alias-password+++[]<br><br>Password of the alias in the key store. If not set, the password will be retrieved from the credential provider.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_P12_ALIAS_PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-p12-provider"></a> [`quarkus.tls."tls-bucket-name".key-store.p12.provider`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-p12-provider) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.p12.provider+++[]<br><br>Provider of the key store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_P12_PROVIDER+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-jks-path"></a> [`quarkus.tls."tls-bucket-name".key-store.jks.path`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-jks-path) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.jks.path+++[]<br><br>Path to the keystore file (JKS format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_JKS_PATH+++[] | path | required [exclamation circle] |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-jks-password"></a> [`quarkus.tls."tls-bucket-name".key-store.jks.password`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-jks-password) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.jks.password+++[]<br><br>Password of the key store. When not set, the password must be retrieved from the credential provider.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_JKS_PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-jks-alias"></a> [`quarkus.tls."tls-bucket-name".key-store.jks.alias`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-jks-alias) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.jks.alias+++[]<br><br>Alias of the private key and certificate in the key store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_JKS_ALIAS+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-jks-alias-password"></a> [`quarkus.tls."tls-bucket-name".key-store.jks.alias-password`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-jks-alias-password) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.jks.alias-password+++[]<br><br>Password of the alias in the key store. When not set, the password may be retrieved from the credential provider.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_JKS_ALIAS_PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-jks-provider"></a> [`quarkus.tls."tls-bucket-name".key-store.jks.provider`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-jks-provider) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.jks.provider+++[]<br><br>Provider of the key store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_JKS_PROVIDER+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-other-type"></a> [`quarkus.tls."tls-bucket-name".key-store.other.type`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-other-type) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.other.type+++[]<br><br>The key store type, passed to `java.security.KeyStore.getInstance(type)`.<br><br>This value also serves as the lookup key for a `KeyStoreFactory` CDI bean via `@Identifier`. If a matching factory is found, it takes priority over the standard file-based loading.<br><br>Examples: `BCFKS`, `PKCS11`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_OTHER_TYPE+++[] | string | required [exclamation circle] |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-other-provider"></a> [`quarkus.tls."tls-bucket-name".key-store.other.provider`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-other-provider) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.other.provider+++[]<br><br>The Java security provider name, passed to `java.security.KeyStore.getInstance(type, provider)`.<br><br>Used by the standard fallback loading. If not set, the default provider for the given type is used. A factory may also read this value but is not required to.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_OTHER_PROVIDER+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-other-path"></a> [`quarkus.tls."tls-bucket-name".key-store.other.path`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-other-path) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.other.path+++[]<br><br>Path to the key store file.<br><br>Required when using the standard fallback (no factory). When a factory handles the loading, this value is available to the factory via this config but is not used by the TLS registry itself.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_OTHER_PATH+++[] | path |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-other-password"></a> [`quarkus.tls."tls-bucket-name".key-store.other.password`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-other-password) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.other.password+++[]<br><br>Password of the key store.<br><br>Used by the standard fallback to load the store. A factory may also read this value. When not set, the password can be retrieved from the credential provider configured on the parent key store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_OTHER_PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-other-alias"></a> [`quarkus.tls."tls-bucket-name".key-store.other.alias`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-other-alias) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.other.alias+++[]<br><br>Alias of the private key and certificate in the key store.<br><br>Used by the standard fallback to select and validate a specific entry. A factory may also read this value but is not required to.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_OTHER_ALIAS+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-other-alias-password"></a> [`quarkus.tls."tls-bucket-name".key-store.other.alias-password`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-other-alias-password) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.other.alias-password+++[]<br><br>Password of the alias in the key store.<br><br>Used by the standard fallback to verify the private key is recoverable. When not set, the password can be retrieved from the credential provider configured on the parent key store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_OTHER_ALIAS_PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-other-params-params"></a> [`quarkus.tls."tls-bucket-name".key-store.other.params."params"`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-other-params-params) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.other.params."params"+++[]<br><br>Arbitrary parameters available to a `KeyStoreFactory` CDI bean via `config.params()`.<br><br>Ignored by the standard fallback. Set via `quarkus.tls.key-store.other.params.<key>=<value>`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_OTHER_PARAMS__PARAMS_+++[] | Map\<String,String> |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-sni"></a> [`quarkus.tls."tls-bucket-name".key-store.sni`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-sni) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.sni+++[]<br><br>Enables Server Name Indication (SNI).<br><br>Server Name Indication (SNI) is a TLS extension that allows a client to specify the hostname it is attempting to connect to during the TLS handshake. This enables a server to present different SSL certificates for multiple domains on a single IP address, facilitating secure communication for virtual hosting scenarios.<br><br>With this setting enabled, the client indicate the server name during the TLS handshake, allowing the server to select the right certificate.<br><br>When configuring the keystore with PEM files, multiple CRT/Key must be given. When configuring the keystore with a JKS or a P12 file, it selects one alias based on the SNI hostname. In this case, all the keystore password and alias password must be the same (configured with the `password` and `alias-password` properties. Do not set the `alias` property.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_SNI+++[] | boolean | `false` |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-credentials-provider-name"></a> [`quarkus.tls."tls-bucket-name".key-store.credentials-provider.name`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-credentials-provider-name) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.credentials-provider.name+++[]<br><br>The name of the "credential" bucket (map key -> passwords) to retrieve from the `io.quarkus.credentials.CredentialsProvider`. If not set, the credential provider will not be used.<br><br>A credential provider offers a way to retrieve the key store password as well as alias password. Note that the credential provider is only used if the passwords are not set in the configuration.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_CREDENTIALS_PROVIDER_NAME+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-credentials-provider-bean-name"></a> [`quarkus.tls."tls-bucket-name".key-store.credentials-provider.bean-name`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-credentials-provider-bean-name) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.credentials-provider.bean-name+++[]<br><br>The name of the bean providing the credential provider.<br><br>The name is used to select the credential provider to use. The credential provider must be exposed as a CDI bean and with the `@Named` annotation set to the configured name to be selected.<br><br>If not set, the default credential provider is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_CREDENTIALS_PROVIDER_BEAN_NAME+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-credentials-provider-password-key"></a> [`quarkus.tls."tls-bucket-name".key-store.credentials-provider.password-key`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-credentials-provider-password-key) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.credentials-provider.password-key+++[]<br><br>The key used to retrieve the key store password.<br><br>If the selected credential provider does not support the key, the password is not retrieved. Otherwise, the retrieved value is used to open the key store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_CREDENTIALS_PROVIDER_PASSWORD_KEY+++[] | string | `password` |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-credentials-provider-alias-password-key"></a> [`quarkus.tls."tls-bucket-name".key-store.credentials-provider.alias-password-key`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-store-credentials-provider-alias-password-key) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-store.credentials-provider.alias-password-key+++[]<br><br>The key used to retrieve the key store alias password.<br><br>If the selected credential provider does not contain the key, the alias password is not retrieved. Otherwise, the retrieved value is used to access the alias `private key` from the key store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_STORE_CREDENTIALS_PROVIDER_ALIAS_PASSWORD_KEY+++[] | string | `alias-password` |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-pem-certs"></a> [`quarkus.tls."tls-bucket-name".trust-store.pem.certs`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-pem-certs) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.pem.certs+++[]<br><br>List of the trusted cert paths (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_PEM_CERTS+++[] | list of path |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-pem-cert-dirs"></a> [`quarkus.tls."tls-bucket-name".trust-store.pem.cert-dirs`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-pem-cert-dirs) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.pem.cert-dirs+++[]<br><br>List of the directories with the trusted certificates (Pem format). The configured directory can be empty. Any file in the configured directories will be treated as a trusted certificate in the Pem format.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_PEM_CERT_DIRS+++[] | list of path |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-p12-path"></a> [`quarkus.tls."tls-bucket-name".trust-store.p12.path`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-p12-path) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.p12.path+++[]<br><br>Path to the trust store file (P12 / PFX format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_P12_PATH+++[] | path | required [exclamation circle] |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-p12-password"></a> [`quarkus.tls."tls-bucket-name".trust-store.p12.password`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-p12-password) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.p12.password+++[]<br><br>Password of the trust store. If not set, the password must be retrieved from the credential provider.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_P12_PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-p12-alias"></a> [`quarkus.tls."tls-bucket-name".trust-store.p12.alias`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-p12-alias) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.p12.alias+++[]<br><br>Alias of the trust store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_P12_ALIAS+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-p12-provider"></a> [`quarkus.tls."tls-bucket-name".trust-store.p12.provider`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-p12-provider) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.p12.provider+++[]<br><br>Provider of the trust store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_P12_PROVIDER+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-jks-path"></a> [`quarkus.tls."tls-bucket-name".trust-store.jks.path`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-jks-path) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.jks.path+++[]<br><br>Path to the trust store file (JKS format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_JKS_PATH+++[] | path | required [exclamation circle] |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-jks-password"></a> [`quarkus.tls."tls-bucket-name".trust-store.jks.password`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-jks-password) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.jks.password+++[]<br><br>Password of the trust store. If not set, the password must be retrieved from the credential provider.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_JKS_PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-jks-alias"></a> [`quarkus.tls."tls-bucket-name".trust-store.jks.alias`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-jks-alias) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.jks.alias+++[]<br><br>Alias of the key in the trust store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_JKS_ALIAS+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-jks-provider"></a> [`quarkus.tls."tls-bucket-name".trust-store.jks.provider`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-jks-provider) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.jks.provider+++[]<br><br>Provider of the trust store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_JKS_PROVIDER+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-other-type"></a> [`quarkus.tls."tls-bucket-name".trust-store.other.type`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-other-type) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.other.type+++[]<br><br>The trust store type, passed to `java.security.KeyStore.getInstance(type)`.<br><br>This value also serves as the lookup key for a `TrustStoreFactory` CDI bean via `@Identifier`. If a matching factory is found, it takes priority over the standard file-based loading.<br><br>Examples: `BCFKS`, `PKCS11`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_OTHER_TYPE+++[] | string | required [exclamation circle] |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-other-provider"></a> [`quarkus.tls."tls-bucket-name".trust-store.other.provider`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-other-provider) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.other.provider+++[]<br><br>The Java security provider name, passed to `java.security.KeyStore.getInstance(type, provider)`.<br><br>Used by the standard fallback loading. If not set, the default provider for the given type is used. A factory may also read this value but is not required to.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_OTHER_PROVIDER+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-other-path"></a> [`quarkus.tls."tls-bucket-name".trust-store.other.path`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-other-path) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.other.path+++[]<br><br>Path to the trust store file.<br><br>Required when using the standard fallback (no factory). When a factory handles the loading, this value is available to the factory via this config but is not used by the TLS registry itself.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_OTHER_PATH+++[] | path |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-other-password"></a> [`quarkus.tls."tls-bucket-name".trust-store.other.password`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-other-password) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.other.password+++[]<br><br>Password of the trust store.<br><br>Used by the standard fallback to load the store. A factory may also read this value. When not set, the password can be retrieved from the credential provider configured on the parent trust store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_OTHER_PASSWORD+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-other-alias"></a> [`quarkus.tls."tls-bucket-name".trust-store.other.alias`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-other-alias) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.other.alias+++[]<br><br>Alias of the certificate in the trust store.<br><br>Used by the standard fallback to validate that a specific certificate exists. A factory may also read this value but is not required to.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_OTHER_ALIAS+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-other-params-params"></a> [`quarkus.tls."tls-bucket-name".trust-store.other.params."params"`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-other-params-params) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.other.params."params"+++[]<br><br>Arbitrary parameters available to a `TrustStoreFactory` CDI bean via `config.params()`.<br><br>Ignored by the standard fallback. Set via `quarkus.tls.trust-store.other.params.<key>=<value>`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_OTHER_PARAMS__PARAMS_+++[] | Map\<String,String> |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-certificate-expiration-policy"></a> [`quarkus.tls."tls-bucket-name".trust-store.certificate-expiration-policy`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-certificate-expiration-policy) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.certificate-expiration-policy+++[]<br><br>Enforce certificate expiration. When enabled, the certificate expiration date is verified and the certificate (or any certificate in the chain) is rejected if it is expired.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_CERTIFICATE_EXPIRATION_POLICY+++[] | tooltip:ignore[Ignore the expiration date.], tooltip:warn[Log a warning when the certificate is expired.], tooltip:reject[Reject the certificate if it is expired.] | tooltip:warn[Log a warning when the certificate is expired.] |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-credentials-provider-name"></a> [`quarkus.tls."tls-bucket-name".trust-store.credentials-provider.name`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-credentials-provider-name) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.credentials-provider.name+++[]<br><br>The name of the "credential" bucket (map key -> passwords) to retrieve from the `io.quarkus.credentials.CredentialsProvider`. If not set, the credential provider will not be used.<br><br>A credential provider offers a way to retrieve the key store password as well as alias password. Note that the credential provider is only used if the passwords are not set in the configuration.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_CREDENTIALS_PROVIDER_NAME+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-credentials-provider-bean-name"></a> [`quarkus.tls."tls-bucket-name".trust-store.credentials-provider.bean-name`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-credentials-provider-bean-name) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.credentials-provider.bean-name+++[]<br><br>The name of the bean providing the credential provider.<br><br>The name is used to select the credential provider to use. The credential provider must be exposed as a CDI bean and with the `@Named` annotation set to the configured name to be selected.<br><br>If not set, the default credential provider is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_CREDENTIALS_PROVIDER_BEAN_NAME+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-credentials-provider-password-key"></a> [`quarkus.tls."tls-bucket-name".trust-store.credentials-provider.password-key`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-store-credentials-provider-password-key) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-store.credentials-provider.password-key+++[]<br><br>The key used to retrieve the trust store password.<br><br>If the selected credential provider does not contain the configured key, the password is not retrieved. Otherwise, the retrieved value is used to open the trust store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_STORE_CREDENTIALS_PROVIDER_PASSWORD_KEY+++[] | string | `password` |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-cipher-suites"></a> [`quarkus.tls."tls-bucket-name".cipher-suites`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-cipher-suites) config_property_copy_button:+++quarkus.tls."tls-bucket-name".cipher-suites+++[]<br><br>Sets the ordered list of enabled cipher suites. If none is given, a reasonable default is selected from the built-in ciphers.<br><br>When suites are set, it takes precedence over the default suite defined by the `SSLEngineOptions` in use.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__CIPHER_SUITES+++[] | list of string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-protocols"></a> [`quarkus.tls."tls-bucket-name".protocols`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-protocols) config_property_copy_button:+++quarkus.tls."tls-bucket-name".protocols+++[]<br><br>Sets the ordered list of enabled TLS protocols.<br><br>If not set, it defaults to `"TLSv1.3"`. The following list of protocols are supported: `TLSv1, TLSv1.1, TLSv1.2, TLSv1.3`. To enable `TLSv1.3` and `TLSv1.2`, set the value to `to "TLSv1.3, TLSv1.2"`.<br><br>Note that setting an empty list, and enabling TLS is invalid. You must at least have one protocol.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__PROTOCOLS+++[] | list of string | `TLSv1.3` |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-handshake-timeout"></a> [`quarkus.tls."tls-bucket-name".handshake-timeout`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-handshake-timeout) config_property_copy_button:+++quarkus.tls."tls-bucket-name".handshake-timeout+++[]<br><br>The timeout for the TLS handshake phase.<br><br>If not set, it defaults to 10 seconds.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__HANDSHAKE_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-tls-registry_quarkus-tls) | `10S` |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-alpn"></a> [`quarkus.tls."tls-bucket-name".alpn`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-alpn) config_property_copy_button:+++quarkus.tls."tls-bucket-name".alpn+++[]<br><br>Enables the Application-Layer Protocol Negotiation (ALPN).<br><br>Application-Layer Protocol Negotiation is a TLS extension that allows the client and server during the TLS handshake to negotiate which protocol they will use for communication. ALPN enables more efficient communication by allowing the client to indicate its preferred application protocol to the server before the TLS connection is established. This helps in scenarios such as HTTP/2 where multiple protocols may be available, allowing for faster protocol selection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__ALPN+++[] | boolean | `true` |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-pqc-enforcement-policy"></a> [`quarkus.tls."tls-bucket-name".pqc-enforcement-policy`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-pqc-enforcement-policy) config_property_copy_button:+++quarkus.tls."tls-bucket-name".pqc-enforcement-policy+++[]<br><br>Sets the PQC enforcement policy.<br><br>`STRICT`: a connection will be refused unless both the client and the server support a PQC-capable group (`X25519MLKEM768`, `SecP256r1MLKEM768`, `SecP384r1MLKEM1024`). The server requires PQC-capable clients.<br><br>`CLIENT_NEGOTIATED`: the server advertises PQC groups but will not refuse a connection if the client does not support them, allowing fallback to classical key exchange.<br><br>`RELAXED`: no PQC enforcement; standard TLS key exchange negotiation applies.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__PQC_ENFORCEMENT_POLICY+++[] | tooltip:strict[A connection is refused unless both the client and the server negotiate a PQC key exchange (`X25519MLKEM768`,opts=`SecP256r1MLKEM768`,`SecP384r1MLKEM1024`). Clients that do not advertise PQC support are rejected during the TLS handshake.], tooltip:client-negotiated[The server advertises PQC key exchange groups but does not require the client to support them. If the client does not support PQC,opts=the handshake falls back to classical key exchange.], tooltip:relaxed[No PQC enforcement; standard TLS key exchange negotiation applies. The server neither advertises nor requires PQC groups.] | tooltip:relaxed[No PQC enforcement; standard TLS key exchange negotiation applies. The server neither advertises nor requires PQC groups.] |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-exchange-groups"></a> [`quarkus.tls."tls-bucket-name".key-exchange-groups`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-key-exchange-groups) config_property_copy_button:+++quarkus.tls."tls-bucket-name".key-exchange-groups+++[]<br><br>Sets the ordered list of enabled TLS key exchange groups (supported groups). If not set, the default groups defined by the `SSLEngineOptions` in use are used.<br><br>Example values: `X25519MLKEM768`, `X25519`, `P-256`. When set, these groups take precedence over the engine defaults.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__KEY_EXCHANGE_GROUPS+++[] | list of string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-ssl-engine"></a> [`quarkus.tls."tls-bucket-name".ssl-engine`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-ssl-engine) config_property_copy_button:+++quarkus.tls."tls-bucket-name".ssl-engine+++[]<br><br>Forces the SSL engine to use.<br><br>When not set, Vert.x selects the engine automatically: if `pqc-enforcement-policy` is `strict` or `client-negotiated`, it prefers the JDK engine (JDK 27{plus}) or falls back to OpenSSL. For `relaxed` policy it uses the JDK engine by default.<br><br>Use `OPENSSL` to force netty-tcnative (requires the native library on the classpath). Use `JDKSSL` to force the standard JDK SSL engine.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__SSL_ENGINE+++[] | tooltip:openssl[Use the OpenSsl Engine,opts=requires netty-tcnative.], tooltip:jdkssl[Use the default JdkSsl Engine.] |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-certificate-revocation-list"></a> [`quarkus.tls."tls-bucket-name".certificate-revocation-list`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-certificate-revocation-list) config_property_copy_button:+++quarkus.tls."tls-bucket-name".certificate-revocation-list+++[]<br><br>Sets the list of revoked certificates (paths to files).<br><br>A Certificate Revocation List (CRL) is a list of digital certificates that have been revoked by the issuing Certificate Authority (CA) before their scheduled expiration date. When a certificate is compromised, no longer needed, or deemed invalid for any reason, the CA adds it to the CRL to inform relying parties not to trust the certificate anymore.<br><br>Two formats are allowed: DER and PKCS++<mark>++7 (also known as P7B). When using the DER format, you must pass DER-encoded CRLs. When using the PKCS++</mark>++7 format, you must pass PKCS++#++7 `SignedData` object, with the only significant field being `crls`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__CERTIFICATE_REVOCATION_LIST+++[] | list of path |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-all"></a> [`quarkus.tls."tls-bucket-name".trust-all`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-trust-all) config_property_copy_button:+++quarkus.tls."tls-bucket-name".trust-all+++[]<br><br>If set to `true`, the server trusts all certificates.<br><br>This is useful for testing, but should not be used in production.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__TRUST_ALL+++[] | boolean | `false` |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-hostname-verification-algorithm"></a> [`quarkus.tls."tls-bucket-name".hostname-verification-algorithm`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-hostname-verification-algorithm) config_property_copy_button:+++quarkus.tls."tls-bucket-name".hostname-verification-algorithm+++[]<br><br>The hostname verification algorithm to use in case the server's identity should be checked. Should be `HTTPS` (default), `LDAPS` or `NONE`.<br><br>If set to `NONE`, it does not verify the hostname.<br><br>If not set, the configured extension decides the default algorithm to use. For example, for HTTP, it will be "HTTPS". For TCP, it can depend on the protocol. Nevertheless, it is recommended to set it to "HTTPS" or "LDAPS".<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__HOSTNAME_VERIFICATION_ALGORITHM+++[] | string |  |
| <a id="quarkus-tls-registry_quarkus-tls-tls-bucket-name-reload-period"></a> [`quarkus.tls."tls-bucket-name".reload-period`](#quarkus-tls-registry_quarkus-tls-tls-bucket-name-reload-period) config_property_copy_button:+++quarkus.tls."tls-bucket-name".reload-period+++[]<br><br>When configured, the server will reload the certificates (from the file system for example) and fires a `CertificateUpdatedEvent` if the reload is successful<br><br>This property configures the period to reload the certificates. IF not set, the certificates won't be reloaded automatically. However, the application can still trigger the reload manually using the `io.quarkus.tls.TlsConfiguration#reload()` method, and then fire the `CertificateUpdatedEvent` manually.<br><br>The fired event is used to notify the application that the certificates have been updated, and thus proceed with the actual switch of certificates.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TLS__TLS_BUCKET_NAME__RELOAD_PERIOD+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-tls-registry_quarkus-tls) |  |

<a id="duration-note-anchor-quarkus-tls-registry_quarkus-tls"></a>

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

## The registry API

While extensions automatically use the TLS registry, you can also access the TLS configuration programmatically through the registry API.

To access the TLS configuration, inject the `TlsConfigurationRegistry` bean. You can retrieve a named TLS configuration by calling `get("<NAME>")` or the default configuration by calling `getDefault()`.

```java
 @Inject
 TlsConfigurationRegistry certificates;
// ...
TlsConfiguration def = certificates.getDefault().orElseThrow();
TlsConfiguration named = certificates.get("name").orElseThrow();
//...
```

The `TlsConfiguration` object contains the keystores, truststores, cipher suites, protocols, and other properties. It also provides a way to create an `SSLContext` from the configuration.

You can also use the `TlsConfiguration` object to configure the Vert.x client or server, such as `KeyCertOptions`, `TrustOptions`, and so on.

## Registering a certificate from an extension

The following section is only for extension developers. An extension can register a certificate in the TLS registry. This is useful when an extension provides a certificate to the application or provides the certificate in a different format.

To register a certificate in the TLS registry by using the extension, the *processor* extension must produce a `TlsCertificateBuildItem` composed of a name and a `CertificateSupplier`.

```java
TlsCertificateBuildItem item = new TlsCertificateBuildItem("named",
    new MyCertificateSupplier());
```

The certificate supplier is a runtime object generally retrieved by using a recorder method.

**An example of a certificate supplier:**

```java
public class MyCertificateSupplier implements Supplier<TlsConfiguration> {

        @Override
        public TlsConfiguration get() {
            try {
                KeyStore ks = KeyStore.getInstance("PKCS12");
                ks.load(getClass().getResourceAsStream("target/certs/test-registration-keystore.p12"),
                        "password".toCharArray());
                KeyStore ts = KeyStore.getInstance("PKCS12");
                ts.load(getClass().getResourceAsStream("target/certs/test-registration-truststore.p12"),
                        "password".toCharArray());
                return new BaseTlsConfiguration() {
                    @Override
                    public KeyStore getKeyStore() {
                        return ks;
                    }

                    @Override
                    public KeyStore getTrustStore() {
                        return ts;
                    }
                };
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
```

<a id="startup-checks"></a>
## Startup checks

When an application that uses the TLS extension starts, the TLS registry performs several checks to ensure the configuration is correct:

- Keystores and truststores are accessible.
- Aliases are available and accessible in the keystores and truststores.
- Certificates are valid.
- Cipher suites and protocols are valid.
- Certificate Revocation Lists (CRLs) are valid.

If any of these checks fail, the application will not start.

<a id="reloading-certificates"></a>
## Reloading certificates

The `TlsConfiguration` obtained from the `TLSConfigurationRegistry` includes a mechanism for reloading certificates. The `reload` method refreshes the keystores, truststores, and CRLs, typically by reloading them from the file system.

> [!NOTE]
> The reload operation is not automatic and must be triggered manually. Additionally, the `TlsConfiguration` implementation must support reloading, as is the case for the configured certificate.

The `reload` method returns a `boolean` indicating whether the reload was successful. A value of `true` means the reload operation was successful, not necessarily that there were updates to the certificates.

After a `TlsConfiguration` has been reloaded, servers and clients using this configuration may need to perform specific actions to apply the new certificates.

The preferred approach for informing clients and servers about the certificate reload is to fire a CDI event of type `io.quarkus.tls.CertificateUpdatedEvent`. To do so, inject a CDI event of this type and fire it when a reload occurs.

**An example of manually reloading a TLS configuration and notifying components by firing a `CertificateUpdatedEvent` and reacting to it:**

```java
// in the class that performs the reload
@Inject
Event<CertificateUpdatedEvent> event;
@Inject
TlsConfigurationRegistry registry;

public void reload() {
    TlsConfiguration config = registry.get("name").orElseThrow();
    if (config.reload()) {
        event.fire(new CertificateUpdatedEvent("name", config));
    }
}

// In the server (or client) code
private final io.vertx.core.http.HttpServer server;

public void onCertificateUpdate(@Observes CertificateUpdatedEvent reload) {
    if ("name".equals(event.getName())) {
        server.updateSSLOptions(reload.tlsConfiguration().getSSLOptions());
        // Or update the SSLContext.
    }

}
```

### Periodic reloading

The TLS registry includes a built-in mechanism that periodically checks the file system for changes and reloads certificates. The `reload-period` property specifies the interval for reloading certificates and emits a `CertificateUpdatedEvent` each time certificates are reloaded.

1. To configure periodic certificate reloading:

   ```properties
   quarkus.tls.reload-period=1h
   quarkus.tls.key-store.pem.0.cert=tls.crt
   quarkus.tls.key-store.pem.0.key=tls.key
   ```


1. For each named configuration, you can set a specific reload period:

   ```properties
   quarkus.tls.http.reload-period=30m
   quarkus.tls.http.key-store.pem.0.cert=tls.crt
   quarkus.tls.http.key-store.pem.0.key=tls.key
   ```


> [!IMPORTANT]
> Impacted servers and clients might need to listen to the `CertificateUpdatedEvent` to apply the new certificates. This is automatically handled for the Quarkus HTTP, REST, gRPC, and WebSocket servers, as well as the management interface if enabled. On the client side, Quarkus REST Client automatically handles certificate update events.

> [!NOTE]
> In Quarkus dev mode, when files are touched, the `CertificateUpdatedEvent` is triggered much more frequently.

## Using Kubernetes secrets or cert-manager

When running in Kubernetes, you can use Kubernetes secrets to store the keystores and truststores.

### Using Kubernetes secrets

1. By using the secret below as an example, create a secret with the keystores and truststores to use Kubernetes secrets:

   ```yaml
   apiVersion: v1
   data:
     tls.crt: ...
     tls.key: ...
   kind: Secret
   metadata:
     name: my-certs
   type: kubernetes.io/tls
   ```


1. Mount the secret as a volume in the pod, which is the easiest way to use these certificates:

   ```yaml
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     labels:
       app.kubernetes.io/name: demo
       app.kubernetes.io/version: 1.0.0-SNAPSHOT
       app.kubernetes.io/managed-by: quarkus
     name: demo
   spec:
     replicas: 1
     selector:
       matchLabels:
         app.kubernetes.io/name: demo
         app.kubernetes.io/version: 1.0.0-SNAPSHOT
     template:
       metadata:
         labels:
           app.kubernetes.io/managed-by: quarkus
           app.kubernetes.io/name: demo
           app.kubernetes.io/version: 1.0.0-SNAPSHOT
       spec:
         containers:
           - env:
               - name: KUBERNETES_NAMESPACE
                 valueFrom:
                   fieldRef:
                     fieldPath: metadata.namespace
             image: ...
             imagePullPolicy: IfNotPresent
             name: demo
             ports:
               - containerPort: 8443 # Configure the port to be HTTPS
                 name: http
                 protocol: TCP
             volumeMounts:
               - mountPath: /certs
                 name: my-volume
         volumes:
           - name: my-volume
             secret:
               defaultMode: 0666 # Set the permissions; otherwise, the pod may not be able to read the files
               optional: false
               secretName: my-certs # Reference the secret
   ```


1. Configure the TLS registry to use the certificates:

   ```properties
   # ...
   # TLS Registry configuration
   %prod.quarkus.tls.http.key-store.pem.0.cert=/certs/tls.crt
   %prod.quarkus.tls.http.key-store.pem.0.key=/certs/tls.key

   # HTTP server configuration:
   %prod.quarkus.http.tls-configuration-name=http
   %prod.quarkus.http.insecure-requests=disabled
   ```

   You can combine this with the periodic reloading to automatically reload the certificates when they change.


### Using cert-manager

When running in Kubernetes, you can use cert-manager to generate and renew certificates automatically. Cert-manager produces a secret containing the keystores and truststores. Configuring the TLS registry is the same as when using Kubernetes secrets. The generated secret includes the following files:

- `tls.crt` for the certificate
- `tls.key` for the private key
- `ca.crt` for the CA certificate (if needed)

1. To configure automatic certificate renewal, use the periodic reloading mechanism:

```properties
# ...
# TLS Registry configuration
%prod.quarkus.tls.http.key-store.pem.0.cert=/certs/tls.crt
%prod.quarkus.tls.http.key-store.pem.0.key=/certs/tls.key
%prod.quarkus.tls.http.reload-period=24h

# HTTP server configuration:
%prod.quarkus.http.tls-configuration-name=http
%prod.quarkus.http.insecure-requests=disabled
```

## Working with OpenShift serving certificates

When running your application in OpenShift, you can use the [OpenShift serving certificates](https://docs.openshift.com/container-platform/4.16/security/certificates/service-serving-certificate.html) to generate and renew TLS certificates automatically. The Quarkus TLS registry can use these certificates and Certificate Authority (CA) files to handle HTTPS traffic and securely validate certificates.

<a id="acquiring-a-certificate"></a>
### Acquiring a certificate

To have OpenShift generate a serving certificate, annotate an existing *Service* object. The generated certificate will be stored in a secret, which you can then mount in your pod.

The following snippet uses an example *Service* object with an annotation for generating a TLS certificate.

1. View the configuration of the *Service* object:

   ```yaml
   apiVersion: v1
   kind: Service
   metadata:
     labels:
       app.kubernetes.io/name: ...
       app.kubernetes.io/version: ...
       app.kubernetes.io/managed-by: quarkus
     name: hero-service
   spec:
     ports:
       - name: http
         port: 443
         protocol: TCP
         targetPort: 8443
     selector:
       app.kubernetes.io/name: ...
       app.kubernetes.io/version: ...
     type: ClusterIP
   ```


1. To generate a certificate, add this annotation to your existing OpenShift `service`:

   ```shell
   oc annotate service hero-service \
        service.beta.openshift.io/serving-cert-secret-name=my-tls-secret
   ```

   The annotation `service.beta.openshift.io/serving-cert-secret-name` instructs OpenShift to generate a certificate and store it in a secret named `my-tls-secret`.


1. Mount the secret as a volume in your pod by updating your *Deployment* configuration:

   ```yaml
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     labels:
       app.kubernetes.io/name: ...
       app.kubernetes.io/version: ...
     name: my-service
   spec:
     replicas: 1
     selector:
       matchLabels:
         app.kubernetes.io/name: ...
         app.kubernetes.io/version: ...
     template:
       metadata:
         labels:
           app.kubernetes.io/name: ...
           app.kubernetes.io/version: ...
       spec:
         volumes:
           - name: my-tls-secret  # <1>
             secret:
               secretName: my-tls-secret
         containers:
           - env:
               - name: KUBERNETES_NAMESPACE
                 valueFrom:
                   fieldRef:
                     fieldPath: metadata.namespace
               - name: QUARKUS_TLS_KEY_STORE_PEM_ACME_CERT # <2>
                 value: /etc/tls/tls.crt
               - name: QUARKUS_TLS_KEY_STORE_PEM_ACME_KEY
                 value: /etc/tls/tls.key
             image: ...
             imagePullPolicy: Always
             name: my-service
             volumeMounts:  # <3>
               - name: my-tls-secret
                 mountPath: /etc/tls
                 readOnly: true
             ports:
               - containerPort: 8443  # <4>
                 name: https
                 protocol: TCP
   ```

   1. Define a volume to mount the secret. Use the same name as the secret declared above.
   2. By using environment variables or configuration files, set up the keystore with the paths to the certificate and private key. This example uses environment variables. OpenShift serving certificates always create the `tls.crt` and `tls.key` files.
   3. Mount the secret in the container. Ensure that the path matches the one used in the configuration (here `/etc/tls`).
   4. Configure the port to serve HTTPS.

2. Deploy your application to use the certificate generated by OpenShift. This will make the service available over HTTPS.

> [!NOTE]
> By setting the `quarkus.tls.key-store.pem.acme.cert` and `quarkus.tls.key-store.pem.acme.key` variables or their environment variable variant, the TLS registry will use the certificate and private key from the secret.
>
> This configures the default keystore for the Quarkus HTTP server, which allows the server to use the certificate. For information about using this certificate in a named configuration, see [Referencing a TLS configuration](#referencing-a-tls-configuration).

### Trusting the Certificate Authority (CA)

**Prerequisites**

- [Acquiring a certificate](#acquiring-a-certificate)

Now that your service uses a certificate issued by OpenShift, configure your client applications to trust this certificate. To do so, create a ConfigMap that holds the CA certificate, and then configure the pod to mount it. The following steps use a Quarkus REST client as an example, but the same approach applies to any client.

1. Start by defining an *empty* ConfigMap, which will be populated with the CA certificate:

   ```yaml
   apiVersion: v1
   kind: ConfigMap
   metadata:
     name: client-tls-config
     annotations:
       service.beta.openshift.io/inject-cabundle: "true"
   ```

   The `service.beta.openshift.io/inject-cabundle` annotation is used to inject the CA certificate into the ConfigMap. Note that the ConfigMap initially has no data — it is empty. During its processing, OpenShift injects the CA certificate into the ConfigMap in the `service-ca.crt` file.


1. Mount the ConfigMap by adding a volume and mounting it in your *Deployment* configuration:

   ```yaml
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     name: my-service-client
     labels:
       app.kubernetes.io/name: ...
       app.kubernetes.io/version: ...
   spec:
     replicas: 1
     selector:
       matchLabels:
         app.kubernetes.io/name: ...
         app.kubernetes.io/version: ...
     template:
       metadata:
         labels:
           app.kubernetes.io/name: ...
           app.kubernetes.io/version: ...
       spec:
         containers:
           - name: my-service-client
             image: ...
             ports:
               - name: http
                 containerPort: 8080
                 protocol: TCP
             volumeMounts: # <1>
               - name: my-client-volume
                 mountPath: /deployments/tls
         volumes: # <2>
           - name: my-client-volume
             configMap:
               name: client-tls-config
   ```

   1. Mount the ConfigMap volume in the container. Ensure that the mount path matches the path referenced in the configuration. In this example, `/deployments/tls`.
   2. Define a volume to mount the ConfigMap and reference the ConfigMap that receives the CA certificate.


1. Configure the REST client to use this CA certificate.

   Consider the following REST client interface:

   ```java
   package org.acme;

   import jakarta.ws.rs.GET;
   import jakarta.ws.rs.Path;
   import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

   @RegisterRestClient(baseUri = "https://hero-service.cescoffi-dev.svc", configKey = "hero") // <1>
   public interface HeroClient {

       record Hero (Long id, String name, String otherName, int level, String picture, String powers) {
       }

       @GET
       @Path("/api/heroes/random")
       Hero getRandomHero();

   }
   ```

   1. Configure the base URI and the configuration key. The name must be in the format `<service-name>.<namespace>.svc`. Otherwise, the certificate will not be trusted. Ensure that the `configKey` is configured as well.


1. Configure the REST client to trust the CA certificate generated by OpenShift:

   ```properties
   quarkus.rest-client.hero.tls-configuration-name=my-service-tls # <1>
   quarkus.tls.my-service-tls.trust-store.pem.certs=/deployments/tls/service-ca.crt # <2>
   ```

   1. Configure the `hero` REST client with the TLS configuration named `my-service-tls`.
   2. Set up the `my-service-tls` TLS configuration, specifically the truststore with the CA certificate. Ensure the path matches the one used in the Kubernetes *Deployment* configuration. The file is always named `service-ca.crt`.


### Certificate renewal

OpenShift automatically renews the serving certificates it generates. When the certificate is renewed, the secret is updated with the new certificate and private key.

To ensure your application uses the new certificate, you can use the periodic reloading feature of the Quarkus TLS registry.

By setting the `reload-period` property, the TLS registry will periodically check the keystores and truststores for changes and reload them if needed:

```properties
quarkus.tls.reload-period=24h
```

1. Optionally, implement a custom mechanism to reload the certificates when the secret is updated. See [Reloading certificates](#reloading-certificates) for more information.

## Quarkus CLI commands and development Certificate Authority

The TLS registry provides Quarkus CLI commands to generate a development Certificate Authority (CA) and trusted certificates. This avoids having to use self-signed certificates locally.

The following snippet shows the description of the `quarkus tls` command, containing two sub-commands:

```shell
> quarkus tls
Install and Manage TLS development certificates
Usage: tls [COMMAND]
Commands:
  generate-quarkus-ca   Generate Quarkus Dev CA certificate and private key. <1>
  generate-certificate  Generate a TLS certificate with the Quarkus Dev CA if
                        available. <2>
```

1. This is useful for local development, as it allows Quarkus to act as its own certificate authority, which can be used to sign other certificates.
2. This is useful when creating a certificate for secure communication between your application and external services or clients during development.

In most cases, you generate the Quarkus Development CA once and then generate certificates signed by this CA. The Quarkus Development CA is a Certificate Authority that can be used to sign certificates locally. It is valid only for development purposes and is trusted only on the local machine.

The generated CA is located in `$HOME/.quarkus/quarkus-dev-root-ca.pem`, and installed in the system truststore.

### Understanding self-signed versus CA-signed certificates

When developing with TLS, you can use two types of certificates:

- **Self-signed certificate**: The owner signs the certificate with their own key. Clients do not trust this certificate by default. Use this certificate when a Certificate Authority (CA) is not available or when you need a simple setup. It is not suitable for production and is intended only for development.

- **CA-signed certificate**: CA, a trusted entity, signs the certificate. Clients trust this certificate by default, and it is the standard choice for production environments.

While you can use a self-signed certificate for local development, it has limitations. Browsers and tools like `curl`, `wget`, and `httpie` typically do not trust self-signed certificates, so you need to import the CA into your operating system manually.

To avoid this issue, use a development CA to sign certificates and install the CA in the system truststore. This ensures that the system trusts all certificates signed by the CA. Quarkus simplifies the generation of a development CA and the certificates it signs.

<a id="generate-a-development-ca"></a>
### Generate a development CA

The development CA is a Certificate Authority that can be used to sign certificates locally. Note that the generated CA is valid only for development purposes and can be trusted only on the local machine.

To generate a development CA:

```shell
quarkus tls generate-quarkus-ca --install \ <1>
     --renew \ <2>
     --truststore <3>
```

1. `--install` installs the CA in the system truststore. Windows, Mac, and Linux (Fedora and Ubuntu) are supported. However, depending on your browser, you might need to import the generated CA manually. Refer to your browser's documentation for more information. The generated CA is located in `$HOME/.quarkus/quarkus-dev-root-ca.pem`.
2. `--renew` renews the CA if it already exists. When this option is used, the private key is changed, so you need to regenerate the certificates signed by the CA. If the CA expires, it will automatically renew without requiring the `--renew` option.
3. `--truststore` also generates a PKCS12 truststore containing the CA certificate.

> [!WARNING]
> When installing the certificate, your system might prompt for your password to add it to the system truststore, or display a confirmation dialog on Windows.

> [!IMPORTANT]
> On Windows, run as administrator from an elevated terminal to install the CA in the system truststore.

### Generating a trusted (signed) certificate

**Prerequisites**

- [Generate a development CA](#generate-a-development-ca)

1. After installing the Quarkus Development CA, generate a trusted certificate. This certificate will be signed by the Quarkus Development CA and trusted by your system.

   ```shell
   quarkus tls generate-certificate --name my-cert
   ```

   This command generates a certificate signed by the Quarkus Development CA, which your system will trust if properly installed or imported.

   The certificate is stored in `./.certs/`. Two files are generated:

   - `$NAME-keystore.p12`: Contains the private key and the certificate. It is password-protected.
   - `$NAME-truststore.p12`: Contains the CA certificate, which you can use as a truststore, for example, for testing.

   Additional options are available:

   ```shell
   Usage: tls generate-certificate [-hrV] [-c=<cn>] [-d=<directory>]
          -n=<name> [-p=<password>]
   Generate a TLS certificate with the Quarkus Dev CA if available.
     -c, --cn=<cn>       The common name of the certificate. Default is 'localhost'
     -d, --directory=<directory>
                         The directory in which the certificates will be created.
                           Default is `.certs`
     -n, --name=<name>   Name of the certificate. It will be used as a file name and
                           alias in the keystore
     -p, --password=<password>
                         The password of the keystore. Default is 'password'
     -r, --renew         Whether existing certificates will need to be replaced
   ```

   The command also generates a `.env` file alongside the certificate so Quarkus dev mode can detect these certificates.


1. Run your application in dev mode to use these certificates:

   ```shell
   ./mvnw quarkus:dev
   ...
   INFO  [io.quarkus] (Quarkus Main Thread) demo 1.0.0-SNAPSHOT on JVM (powered by Quarkus 999-SNAPSHOT) started in 1.286s. Listening on: http://localhost:8080 and https://localhost:8443
   ```


1. Open the Dev UI by using HTTPS: `https://localhost:8443/q/dev` or by issuing a `curl` request:

   ```shell
   curl https://localhost:8443/hello
   Hello from Quarkus REST%
   ```

   > [!IMPORTANT]
   > Quarkus generates a self-signed certificate if the Quarkus Development CA is not installed.


### Generating a self-signed certificate

Even if the Quarkus Development CA is installed, you can generate a self-signed certificate:

```shell
quarkus tls generate-certificate --name my-cert --self-signed
```

This generates a self-signed certificate that the Quarkus Development CA does not sign.

### Uninstalling the Quarkus Development CA

Uninstalling the Quarkus Development CA depends on your OS.

#### Deleting the CA certificate on Windows

1. List the CA certificate on Windows by using the Powershell terminal with administrator rights:

   ```shell
   # First, we need to identify the serial number of the CA certificate
   > certutil -store -user Root
   root "Trusted Root Certification Authorities"
   ================ Certificate 0 ================
   Serial Number: 019036d564c8
   Issuer: O=Quarkus, CN=quarkus-dev-root-ca # <-That's the CA, copy the Serial Number (the line above)
   NotBefore: 6/19/2024 11:07 AM
   NotAfter: 6/20/2025 11:07 AM
   Subject: C=Cloud, S=world, L=home, OU=Quarkus Dev, O=Quarkus Dev, CN=quarkus-dev-root-ca
   Signature matches Public Key
   Non-root Certificate uses same Public Key as Issuer
   Cert Hash(sha1): 3679bc95b613a2112a3d3256fe8321b6eccce720
   No key provider information
   Cannot find the certificate and private key for decryption.
   CertUtil: -store command completed successfully.
   ```


1. Delete the stored CA certificate and replace `$Serial_Number` with the serial number of the CA certificate:

   ```shell
   > certutil -delstore -user -v Root $Serial_Number
   ```


#### Deleting the CA certificate on Linux

- On Fedora:

  ```shell
  sudo rm /etc/pki/ca-trust/source/anchors/quarkus-dev-root-ca.pem
  sudo update-ca-trust
  ```


- On Ubuntu:

  ```shell
  sudo rm /usr/local/share/ca-certificates/quarkus-dev-root-ca.pem
  sudo update-ca-certificates
  ```


#### Deleting the CA certificate on Mac

- On Mac:

  ```shell
  sudo security -v remove-trusted-cert -d /Users/clement/.quarkus/quarkus-dev-root-ca.pem
  ```


<a id="lets-encrypt"></a>
## Automatic certificate management with Let's Encrypt

[Let's Encrypt](https://letsencrypt.org) is a free, automated certificate authority provided by [Internet Security Research Group](https://www.abetterinternet.org/).

Let's Encrypt uses the [Automated Certificate Management Environment (ACME)](https://datatracker.ietf.org/doc/html/rfc8555) protocol to support automatic certificate issuance and renewal. To learn more about Let's Encrypt and ACME, see [Let's Encrypt documentation](https://letsencrypt.org/docs/).

The TLS registry extension allows a CLI ACME client to issue and renew Let's Encrypt certificates. Your application uses this TLS registry extension to resolve ACME protocol challenges.

Follow the steps below to prepare your Quarkus application and automatically update it with new and renewed Let's Encrypt certificates.

<a id="lets-encrypt-prerequisites"></a>
### Prerequisites

- Ensure that a fully resolvable DNS domain name is available that you can use to access your application. You can use this domain name to create a Let's Encrypt account and pass the Let's Encrypt ACME challenges to prove that you own this domain. You can use [ngrok](https://ngrok.com/) to start experimenting with the Quarkus Let's Encrypt ACME feature; for more information, see the [Testing with ngrok](#lets-encrypt-ngrok) section below.

- Your Quarkus HTTPS application must use a *build-time* property to enable a Let's Encrypt ACME challenge route:

  ```properties
  quarkus.tls.lets-encrypt.enabled=true
  ```

  The TLS registry can manage the challenge process from either the main HTTP interface or the management interface. Using a management interface is **strongly** recommended to let Quarkus deal with the ACME challenge configuration separately from the main application's deployment and security requirements:

  ```properties
  quarkus.tls.lets-encrypt.enabled=true
  quarkus.management.enabled=true
  ```


> [!IMPORTANT]
> The Let's Encrypt ACME challenge requires that the application is reachable on port `80`, essentially `http://your-dns-name`. Ensure the port `80` is accessible from the Internet. It might require an explicit security policy depending on your hosting provider.
>
> We also recommend setting `quarkus.http.insecure-requests` to `redirect` to redirect all HTTP requests to HTTPS. The ACME challenge accepts self-signed certificates and up to 10 redirections:
>
> ```properties
> quarkus.tls.lets-encrypt.enabled=true
> quarkus.management.enabled=true
> quarkus.http.insecure-requests=redirect
> ```

The primary HTTP interface serves the challenge and is accessible from your DNS domain name.

> [!IMPORTANT]
> Do not start your application yet.

<a id="lets-encrypt-prepare"></a>
### Application preparation

Before you request a Let's Encrypt certificate:

1. Move to the root directory of your application.
2. Run the TLS registry Let's Encrypt CLI `prepare` command:

   ```shell
   quarkus tls lets-encrypt prepare --domain=<domain-dns-name>
   ```

   The `prepare` command does the following:


- Creates a `.letsencrypt` folder in your application's root directory.
- Creates a self-signed domain certificate and private key for your application configured in the previous [Let's Encrypt prerequisites](#lets-encrypt-prerequisites) step to start and accept HTTPS requests.
- Creates a `.env` configuration file in the root directory of your application and configures it to use the self-signed domain certificate and private key until you obtain the Let's Encrypt certificate.

  The following snippet shows an example of the generated `.env` file:

  ```properties
  quarkus.tls.key-store.pem.acme.cert=.letsencrypt/lets-encrypt.crt
  quarkus.tls.key-store.pem.acme.key=.letsencrypt/lets-encrypt.key
  ```

  > [!NOTE]
  > The `.env` file does not contain the `quarkus.tls.lets-encrypt.enabled` and `quarkus.management.enabled` properties as they are build-time properties that require a rebuild of the application.


### Starting your application

1. Start your application:

   ```shell
   java -jar quarkus-run.jar
   ```


.. Access your application endpoint by using `https://your-domain-name:8443/`; for example, `https://your-domain-name:8443/hello`, and accept a self-signed certificate in the browser.

.. Keep the application running and request your first Let's Encrypt certificate.

<a id="lets-encrypt-issue-certificate"></a>
### Issue a certificate

1. From the application directory, run the `issue-certificate` command to acquire your first Let's Encrypt certificate:

   ```shell
   quarkus tls lets-encrypt issue-certificate \
     --domain=<domain-dns-name> \ <1>
     --email=<your contact email> \ <2>
     --management-url=https://localhost:9000 <3>
   ```

   1. Set your domain name.
   2. Provide your contact email address that Let's Encrypt can use to contact you in case of any issues with your Let's Encrypt account.
   3. Set your application management URL, which you can use to handle ACME challenges. Use `https://localhost:8443/` if you choose not to enable a management router in the [Let's Encrypt prerequisites](#lets-encrypt-prerequisites) step.

   Additional options are available:

   ```shell
     --acme-server-url=<url>         Custom ACME production server URL (default: Let's Encrypt production)
     --acme-staging-server-url=<url> Custom ACME staging server URL (default: Let's Encrypt staging)
     --staging                       Use staging environment (default: false)
     --insecure                      Disable SSL certificate validation for the management endpoint (INSECURE - development/testing only)
   ```

   These options allow you to:

   - Use alternative ACME providers such as [ZeroSSL](https://zerossl.com/) or custom ACME servers to issue certificates from different providers.
   - Test with staging environments to avoid rate limits during development.
   - Disable SSL validation for testing with self-signed management certificates (not recommended for production).

   > [!NOTE]
   > If `--staging` is set to `true`, the TLS registry CLI uses the staging server URL, which is either the default Let's Encrypt staging server or the custom staging server provided with `--acme-staging-server-url`.


1. During the processing of the `issue-certificate` command, the TLS registry CLI performs the following tasks:

- Checks if the application is prepared to serve the challenge.
- Creates and records Let's Encrypt account information.
- Issues a Let's Encrypt certificate request.
- Interacts with the Quarkus application to resolve ACME challenges.

  > [!NOTE]
  > After the `issue-certificate` command acquires the Let's Encrypt certificate chain and private key, the TLS registry CLI converts them to PEM format and copies them to the `.letsencrypt` folder. The TLS registry detects the new certificate and private key and reloads them automatically.

  1. Access your application's endpoint again at `https://your-domain-name:8443/`. Confirm that the browser shows Let's Encrypt as the certificate issuer.

  The `issue-certificate` command currently creates a Let's Encrypt account automatically to simplify initial ACME setup. The TLS registry CLI continues to add more support for Let's Encrypt account management.


### Renewing a certificate

Renewing certificates is similar to issuing the first certificate, but it requires an existing account created during the [Issue certificates with Let's Encrypt](#lets-encrypt-issue-certificate) step.

Run the following command to renew your Let's Encrypt certificate and set your domain DNS name:

```shell
quarkus tls lets-encrypt renew-certificate \
  --domain=<domain-dns-name> \
  --management-url=https://localhost:9000
```

During this command, the TLS registry CLI reads the Let's Encrypt account information recorded during the [Issue certificates with Let's Encrypt](#lets-encrypt-issue-certificate) step, issues a Let's Encrypt certificate request, and communicates with the Quarkus application to resolve ACME challenges.

After the `renew-certificate` command renews the Let's Encrypt certificate chain and private key, the TLS registry CLI converts them to PEM format and copies them to the `.letsencrypt` folder. The TLS registry detects the new certificate and private key and reloads them automatically.

Additional options are available to support alternative ACME providers:

```shell
  --acme-server-url=<url>         Custom ACME production server URL (default: Let's Encrypt production)
  --acme-staging-server-url=<url> Custom ACME staging server URL (default: Let's Encrypt staging)
  --insecure                      Disable SSL certificate validation for the management endpoint (INSECURE - development/testing only)
```

For example, to renew a certificate from ZeroSSL:

```shell
quarkus tls lets-encrypt renew-certificate \
  --domain=<domain-dns-name> \
  --management-url=https://localhost:9000 \
  --acme-server-url=https://acme.zerossl.com/v2/DV90
```

### Using alternative ACME providers

While Let's Encrypt is the default ACME provider, you can use other ACME-compatible certificate authorities by specifying custom server URLs. This is useful for:

- **Disaster recovery**: Failover to different providers if Let's Encrypt experiences outages
- **Regional requirements**: Use providers that better serve specific geographic regions
- **Organizational policies**: Comply with company requirements for specific CAs
- **Testing**: Use custom staging servers during development

#### Supported ACME providers

The following ACME providers are known to work with Quarkus:

| Provider | Production URL | Staging URL |
| --- | --- | --- |
| Let's Encrypt (default) | `https://acme-v02.api.letsencrypt.org/directory` | `https://acme-staging-v02.api.letsencrypt.org/directory` |
| ZeroSSL | `https://acme.zerossl.com/v2/DV90` | N/A (use Let's Encrypt staging for testing) |
| Google Trust Services | `https://dv.acme-v02.api.pki.goog/directory` | `https://dv.acme-v02.test-api.pki.goog/directory` |

#### Example: Using ZeroSSL

To issue a certificate from ZeroSSL instead of Let's Encrypt:

```shell
quarkus tls lets-encrypt issue-certificate \
  --domain=example.com \
  --email=admin@example.com \
  --management-url=https://localhost:9000 \
  --acme-server-url=https://acme.zerossl.com/v2/DV90
```

> [!IMPORTANT]
> Each ACME provider may have different rate limits, account requirements, and supported features. Consult the provider's documentation before using them in production.

<a id="lets-encrypt-ngrok"></a>
### Testing with ngrok

[ngrok](https://ngrok.com/) provides a secure HTTPS tunnel to an application running on localhost and helps you test HTTPS-based applications.

ngrok provides a simple way to get started with the Quarkus Let's Encrypt ACME feature.

1. Start testing by reserving a domain in ngrok:

   You can reserve the domain by using [Quarkiverse ngrok](https://github.com/quarkiverse/quarkus-ngrok) in dev mode or by reserving it directly in the ngrok dashboard. You cannot use the ngrok domain to test the Quarkus Let's Encrypt ACME feature immediately.

   ngrok uses Let's Encrypt and intercepts ACME challenges that the Quarkus application would otherwise handle.


1. Therefore, remove the ngrok Let's Encrypt certificate policy from your ngrok domain:

   ```shell
   ngrok api --api-key <YOUR-API-KEY> reserved-domains delete-certificate-management-policy <YOUR-RESERVED-DOMAIN-ID>
   ```

   `YOUR-RESERVED-DOMAIN-ID` is your reserved domain's ID, which starts from `rd_`; you can find it in the [ngrok dashboard domains section](https://dashboard.ngrok.com/cloud-edge/domains).


1. Because ngrok only forwards ACME challenges over HTTP, start ngrok with the following command:

   ```shell
   ngrok http --domain <YOUR-NGROK-DOMAIN> 8080 --scheme http <1>
   ```

   1. `8080` is the localhost HTTP port your application is listening on. Note that the application will be accessible from `http://YOUR-NGROK-DOMAIN` on port `80` but redirected to your local machine on port `8080`.


1. Test the Quarkus Let's Encrypt ACME feature from your local machine. // The reason for this ifndef condition is that this content is not supported in product docs. // Feel free to add more content to this chapter, but make sure this condition encloses it.
