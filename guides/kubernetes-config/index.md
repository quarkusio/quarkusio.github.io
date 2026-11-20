# Kubernetes Config

Quarkus includes the `kubernetes-config` extension which allows developers to use Kubernetes [ConfigMaps](https://cloud.google.com/kubernetes-engine/docs/concepts/configmap) and [Secrets](https://cloud.google.com/kubernetes-engine/docs/concepts/secret) as a configuration source, without having to mount them into the [Pod](https://kubernetes.io/docs/concepts/workloads/pods/pod/) running the Quarkus application or make any other modifications to their Kubernetes `Deployment` (or OpenShift `DeploymentConfig`).

## Configuration

Once you have your Quarkus project configured you can add the `kubernetes-config` extension by running the following command in your project base directory.

**CLI**

```bash
quarkus extension add kubernetes-config
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='kubernetes-config'
```

**Gradle**

```bash
./gradlew addExtension --extensions='kubernetes-config'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-kubernetes-config</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-kubernetes-config")
```

## Usage

The extension works by reading ConfigMaps and Secrets directly from the Kubernetes API server using the [Kubernetes Client](/guides/kubernetes-client/index.md).

The extension understands the following types of ConfigMaps and Secrets as input sources:

- ConfigMaps and Secrets that contain literal data (see [this](https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/#create-configmaps-from-literal-values) for an example on how to create one)
- ConfigMaps and Secrets created from files named `application.properties`, `application.yaml` or `application.yml` (see [this](https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/#create-configmaps-from-files) for an example on how to create one).

The extension is disabled by default in order to prevent the application for making API calls when it is not running in a Kubernetes environment. To enable it, set `quarkus.kubernetes-config.enabled=true` (for example using a specific [profile](/guides/config-reference/index.md#profiles)).

The values of `quarkus.kubernetes-config.config-maps` and `quarkus.kubernetes-config.secrets` determine which ConfigMaps and/or Secrets will be used as configuration sources. Keep in mind that these ConfigMaps and Secrets must be in the same Kubernetes `Namespace` as the running application. If they are to be found in a different namespace, then `quarkus.kubernetes-config.namespace` must be set to the proper value.

### Priority of obtained properties

The properties obtained from the ConfigMaps and Secrets have a higher priority than (i.e. they override) any properties of the same name that are found in `application.properties` (or the YAML equivalents), but they have lower priority than properties set via Environment Variables or Java System Properties.

Furthermore, when multiple ConfigMaps (or Secrets) are used, ConfigMaps (or Secrets) defined later in the list have a higher priority than ConfigMaps (or Secrets) defined earlier in the list.

Finally, when both ConfigMaps and Secrets are used, the latter always has a higher priority than the former.

### Kubernetes Permissions

Since reading ConfigMaps involves interacting with the Kubernetes API Server, when [RBAC](https://kubernetes.io/docs/reference/access-authn-authz/rbac/) is enabled on the cluster, the [ServiceAccount](https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/) that is used to run the application needs to have the proper permissions for such access.

Thankfully, when using the `kubernetes-config` extension along with the [Kubernetes](/guides/deploying-to-kubernetes/index.md) extension, all the necessary Kubernetes resources to make that happen are automatically generated.

#### Secrets

By default, the [Kubernetes](/guides/deploying-to-kubernetes/index.md) extension doesn't generate the necessary resources to allow accessing secrets. Set `quarkus.kubernetes-config.secrets.enabled=true` to generate the necessary role and corresponding role binding.

## Example configuration

A very common use case is to deploy a Quarkus application that needs to access a relational database which has itself already been deployed on Kubernetes. Using the `quarkus-kubernetes-config` extension makes this use case very simple to handle.

Let's assume that our Quarkus application needs to talk to PostgreSQL and that when PostgreSQL was deployed on our Kubernetes cluster, a `Secret` named `postgresql` was created as part of that deployment and contains the following entries:

- `database-name`
- `database-user`
- `database-password`

One possible way to make Quarkus use these entries to connect the database is to use the following configuration:

```properties
%prod.quarkus.kubernetes-config.secrets.enabled=true <1>
quarkus.kubernetes-config.secrets=postgresql <2>

%prod.quarkus.datasource.jdbc.url=postgresql://somehost:5432/${database-name} <3>
%prod.quarkus.datasource.username=${database-user} <4>
%prod.quarkus.datasource.password=${database-password} <5>
```

1. Enable reading of secrets. Note the use of `%prod` profile as we only want this setting applied when the application is running in production.
2. Configure the name of the secret that will be used. This doesn't need to be prefixed with the `%prod` profile as it won't have any effect if secret reading is disabled.
3. Quarkus will substitute `${database-name}` with the value obtained from the entry with name `database-name` of the `postgres` Secret. `somehost` is the name of the Kubernetes `Service` that was created when PostgreSQL was deployed to Kubernetes.
4. Quarkus will substitute `${database-user}` with the value obtained from the entry with name `database-user` of the `postgres` Secret.
5. Quarkus will substitute `${database-password}` with the value obtained from the entry with name `database-password` of the `postgres` Secret.

The values above allow the application to be completely agnostic of the actual database configuration used in production while also not inhibiting the usability of the application at development time.

### Alternatives

The use of the `quarkus-kubernetes-config` extensions is completely optional as there are other ways an application can be configured to use ConfigMaps or Secrets.

One common alternative is to map each entry of the ConfigMap and / Secret to an environment variable on the Kubernetes `Deployment` - see [this](https://kubernetes.io/docs/concepts/configuration/secret/#use-case-as-container-environment-variables) for more details. To achieve that in Quarkus, we could use the `quarkus-kubernetes` extension (which is responsible for creating Kubernetes manifests and include the following configuration) and configure it as so:

```properties
quarkus.kubernetes.env.secrets=postgresql
quarkus.kubernetes.env.mapping.database-name.from-secret=postgresql
quarkus.kubernetes.env.mapping.database-name.with-key=database-name
quarkus.kubernetes.env.mapping.database-user.from-secret=postgresql
quarkus.kubernetes.env.mapping.database-user.with-key=database-user
quarkus.kubernetes.env.mapping.database-password.from-secret=postgresql
quarkus.kubernetes.env.mapping.database-password.with-key=database-password

%prod.quarkus.datasource.jdbc.url=postgresql://somehost:5432/${database-name}
%prod.quarkus.datasource.username=${database-user}
%prod.quarkus.datasource.password=${database-password}
```

The end result of the above configuration would be the following `env` part being applied the generated `Deployment`:

```yaml
          env:
            - name: DATABASE_NAME
              valueFrom:
                secretKeyRef:
                  key: database-name
                  name: postgresql
            - name: DATABASE_USER
              valueFrom:
                secretKeyRef:
                  key: database-user
                  name: postgresql
            - name: DATABASE_PASSWORD
              valueFrom:
                secretKeyRef:
                  key: database-password
                  name: postgresql
```

See [this](/guides/deploying-to-kubernetes/index.md#secret-mapping) for more details.

## Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-kubernetes-config_quarkus-kubernetes-config-secrets-enabled"></a> [`quarkus.kubernetes-config.secrets.enabled`](#quarkus-kubernetes-config_quarkus-kubernetes-config-secrets-enabled) config_property_copy_button:+++quarkus.kubernetes-config.secrets.enabled+++[]<br><br>Whether configuration can be read from secrets. If set to `true`, Kubernetes resources allowing access to secrets (role and role binding) will be generated.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CONFIG_SECRETS_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-kubernetes-config_quarkus-kubernetes-config-secrets-role-config-name"></a> [`quarkus.kubernetes-config.secrets-role-config.name`](#quarkus-kubernetes-config_quarkus-kubernetes-config-secrets-role-config-name) config_property_copy_button:+++quarkus.kubernetes-config.secrets-role-config.name+++[]<br><br>The name of the role.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CONFIG_SECRETS_ROLE_CONFIG_NAME+++[] | string | `view-secrets` |
| [lock] <a id="quarkus-kubernetes-config_quarkus-kubernetes-config-secrets-role-config-namespace"></a> [`quarkus.kubernetes-config.secrets-role-config.namespace`](#quarkus-kubernetes-config_quarkus-kubernetes-config-secrets-role-config-namespace) config_property_copy_button:+++quarkus.kubernetes-config.secrets-role-config.namespace+++[]<br><br>The namespace of the role.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CONFIG_SECRETS_ROLE_CONFIG_NAMESPACE+++[] | string |  |
| [lock] <a id="quarkus-kubernetes-config_quarkus-kubernetes-config-secrets-role-config-cluster-wide"></a> [`quarkus.kubernetes-config.secrets-role-config.cluster-wide`](#quarkus-kubernetes-config_quarkus-kubernetes-config-secrets-role-config-cluster-wide) config_property_copy_button:+++quarkus.kubernetes-config.secrets-role-config.cluster-wide+++[]<br><br>Whether the role is cluster wide or not. By default, it's not a cluster wide role.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CONFIG_SECRETS_ROLE_CONFIG_CLUSTER_WIDE+++[] | boolean | `false` |
| [lock] <a id="quarkus-kubernetes-config_quarkus-kubernetes-config-secrets-role-config-generate"></a> [`quarkus.kubernetes-config.secrets-role-config.generate`](#quarkus-kubernetes-config_quarkus-kubernetes-config-secrets-role-config-generate) config_property_copy_button:+++quarkus.kubernetes-config.secrets-role-config.generate+++[]<br><br>If the current role is meant to be generated or not. If not, it will only be used to generate the role binding resource.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CONFIG_SECRETS_ROLE_CONFIG_GENERATE+++[] | boolean | `true` |
| <a id="quarkus-kubernetes-config_quarkus-kubernetes-config-enabled"></a> [`quarkus.kubernetes-config.enabled`](#quarkus-kubernetes-config_quarkus-kubernetes-config-enabled) config_property_copy_button:+++quarkus.kubernetes-config.enabled+++[]<br><br>If set to true, the application will attempt to look up the configuration from the API server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CONFIG_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-kubernetes-config_quarkus-kubernetes-config-fail-on-missing-config"></a> [`quarkus.kubernetes-config.fail-on-missing-config`](#quarkus-kubernetes-config_quarkus-kubernetes-config-fail-on-missing-config) config_property_copy_button:+++quarkus.kubernetes-config.fail-on-missing-config+++[]<br><br>If set to true, the application will not start if any of the configured config sources cannot be located<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CONFIG_FAIL_ON_MISSING_CONFIG+++[] | boolean | `true` |
| <a id="quarkus-kubernetes-config_quarkus-kubernetes-config-config-maps"></a> [`quarkus.kubernetes-config.config-maps`](#quarkus-kubernetes-config_quarkus-kubernetes-config-config-maps) config_property_copy_button:+++quarkus.kubernetes-config.config-maps+++[]<br><br>ConfigMaps to look for in the namespace that the Kubernetes Client has been configured for. ConfigMaps defined later in this list have a higher priority that ConfigMaps defined earlier in this list. Furthermore, any Secrets defined in `secrets`, will have higher priorities than all ConfigMaps.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CONFIG_CONFIG_MAPS+++[] | list of string |  |
| <a id="quarkus-kubernetes-config_quarkus-kubernetes-config-secrets"></a> [`quarkus.kubernetes-config.secrets`](#quarkus-kubernetes-config_quarkus-kubernetes-config-secrets) config_property_copy_button:+++quarkus.kubernetes-config.secrets+++[]<br><br>Secrets to look for in the namespace that the Kubernetes Client has been configured for. If you use this, you probably want to enable `quarkus.kubernetes-config.secrets.enabled`. Secrets defined later in this list have a higher priority that ConfigMaps defined earlier in this list. Furthermore, these Secrets have a higher priorities than all ConfigMaps defined in `configMaps`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CONFIG_SECRETS+++[] | list of string |  |
| <a id="quarkus-kubernetes-config_quarkus-kubernetes-config-namespace"></a> [`quarkus.kubernetes-config.namespace`](#quarkus-kubernetes-config_quarkus-kubernetes-config-namespace) config_property_copy_button:+++quarkus.kubernetes-config.namespace+++[]<br><br>Namespace to look for config maps and secrets. If this is not specified, then the namespace configured in the kubectl config context is used. If the value is specified and the namespace doesn't exist, the application will fail to start.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CONFIG_NAMESPACE+++[] | string |  |
