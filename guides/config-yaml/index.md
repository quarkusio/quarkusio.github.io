# YAML configuration

You can use a YAML file,`application.yaml`, to configure your Quarkus application instead of the standard Java properties file, `application.properties`.

[YAML](https://en.wikipedia.org/wiki/YAML) is widely used for defining resource descriptors, especially in Kubernetes.

## Enable YAML configuration

To enable YAML configuration, add the `quarkus-config-yaml` extension:

**CLI**

```bash
quarkus extension add quarkus-config-yaml
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='quarkus-config-yaml'
```

**Gradle**

```bash
./gradlew addExtension --extensions='quarkus-config-yaml'
```

Alternatively, add the following dependency to your project:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-config-yaml</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-config-yaml")
```

After adding the extension or dependency, to avoid confusion, remove the `src/main/resources/application.properties` file and create a `src/main/resources/application.yaml` file.

> [!NOTE]
> If both files are present, Quarkus gives precedence to properties in the YAML file.

> [!TIP]
> Quarkus recognizes both `.yaml` and `.yml` file extensions. If both files are available, Quarkus gives precedence to the `.yaml` file and then the `.yml` file.

### Example YAML configurations

The following snippets give examples of YAML configurations:

```yaml
# YAML supports comments
quarkus:
  datasource:
    db-kind: postgresql
    jdbc:
      url: jdbc:postgresql://localhost:5432/some-database

# REST Client configuration property
quarkus:
  rest-client:
    org.acme.rest.client.ExtensionsService:
      url: https://stage.code.quarkus.io/api
```

```yaml
# For configuration property names that use quotes, do not split the string inside the quotes
quarkus:
  log:
    category:
      "io.quarkus.category":
        level: INFO
```

```yaml
quarkus:
  datasource:
    jdbc:
      url: jdbc:postgresql://localhost:5432/quarkus_test

  hibernate-orm:
    schema-management:
      strategy: create

  oidc:
    enabled: true
    auth-server-url: http://localhost:8180/auth/realms/quarkus
    client-id: app


app:
  frontend:
    oidc-realm: quarkus
    oidc-app: app
    oidc-server: http://localhost:8180/auth

# With profiles
"%test":
   quarkus:
     oidc:
       enabled: false
     security:
        users:
            file:
              enabled: true
              realm-name: quarkus
              plain-text: true
```

> [!TIP]
> You can also use JSON in the YAML file to describe the configuration. Use a single format per file. The file extension is either `yaml` or `yml`.

## Profiles

As you can see in the previous snippet, you can use [profiles](/guides/config-reference/index.md#profiles) in YAML.

In YAML, keys that begin with `%` are not allowed. However, profile keys must start with this symbol. To resolve this, enclose the profile keys in double quotes, as demonstrated by the example, `"%test"`.

All configurations under the `"%test"` key activate only when the `test` profile is enabled. For instance, the previous snippet shows that OpenID Connect (OIDC) (`quarkus.oidc.enabled: false`) is disabled when the `test` profile is active. Without the `test` profile, OIDC is enabled by default.

You can also define custom profiles, such as `%staging` in the following example:

```yaml
quarkus:
  http:
    port: 8081

"%staging":
    quarkus:
        http:
          port: 8082
```

If you enable the `staging` profile, the HTTP port is set to `8082` instead of `8081`.

The YAML configuration also supports profile-aware files. In this case, properties for a specific profile can reside in an `application-{profile}.yaml` named file. The previous example can be expressed as:

```yaml
quarkus:
  http:
    port: 8081
```

**application-staging.yaml**

```yaml
quarkus:
  http:
    port: 8082
```

> [!IMPORTANT]
> An `application.yaml` file must exist (even if empty) in the exact location of the profile-aware (`application-{profile}.yaml`) file to be included in the configuration to ensure a consistent order when loading the files. All profile files must match the same extension of the main file.

## Expressions

The YAML format also supports [property expressions](/guides/config-reference/index.md#property-expressions), by using the same format as Java properties:

```yaml
mach: 3
x:
  factor: 2.23694

display:
  mach: ${mach}
  unit:
    name: "mph"
    factor: ${x.factor}
```

You can reference nested properties by using the `.` (dot) separator, as in `${x.factor}`.

## External application.yaml file

The `application.yaml` file can also be placed in `config/application.yaml` to specialize the runtime configuration. The file must be present in the root of the working directory relative to the Quarkus application runner:

```text
.
├── config
│    └── application.yaml
├── my-app-runner
```

The values from this file override any values from the regular `application.yaml` file if it exists.

## Configuration property conflicts

The MicroProfile Config specification defines configuration properties as an arbitrary `.`-delimited string. However, structured formats such as YAML only support a subset of the possible configuration namespace. For example, consider the two configuration properties `one.two` and `one.two.three`. One property is the prefix of another, so it might not be immediately evident how to specify both keys in your YAML configuration.

This is solved by using `~` as a `null` key to represent any YAML property that is a prefix of another one:

```yaml
one:
  two:
    ~: 12
    three: 123
```

YAML `null` keys are not included in the assembly of the configuration property name, allowing them to be used at any level for disambiguating configuration properties.
