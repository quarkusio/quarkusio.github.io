# Dev Services for Kubernetes

Dev Services for Kubernetes automatically starts a Kubernetes API server (plus the required etcd) in dev mode and when running tests. So you don't have to start it manually. The application is configured automatically.

The following [testcontainers](https://github.com/dajudge/kindcontainer?tab=readme-ov-file#container-flavors) are supported: [kind](https://kind.sigs.k8s.io/), [k3s](https://k3s.io/) or api only(default)

## Enabling / Disabling Dev Services for Kubernetes

To use the Kubernetes Dev Service, you simply need to add the `quarkus-kubernetes-client` extension to your project:

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

A Kubernetes API server is automatically started in dev or test modes whenever the `quarkus-kubernetes-client` extension is configured for a project. However, the dev service is disabled in some cases to prevent potential confusing situations:

- if the dev service is explicitly disabled by setting `quarkus.kubernetes-client.devservices.enabled` to `false`
- if the client is explicitly configured to access a given API server via `quarkus.kubernetes-client.api-server-url`
- if a valid Kube config file is found, in which case that configuration will be used by the client. It is, however, possible to force the dev service to start anyway by setting `quarkus.kubernetes-client.devservices.override-kubeconfig` to `true` to disregard the existing configuration
- if you include the `quarkus-test-kubernetes-client` dependency as, presumably, in that case, you have tests that rely on the Fabric8 mock server and don't need to start a cluster

> [!NOTE]
> Dev Services for Kubernetes relies on a container engine (Docker or Podman) to start the server. If your environment does not support such a container engine, you will have to start a Kubernetes cluster running in a VM, in the cloud, etc. In this case, you can configure the Kubernetes cluster access using either a Kube config file or the various properties available as specified in the [Kubernetes client configuration guide](/guides/kubernetes-client/index.md).

## Shared cluster

Applications often need to share access to the same cluster. For that purpose, Dev Services for Kubernetes implements a *service discovery* mechanism for multiple Quarkus applications running in dev mode to share a single cluster.

> [!NOTE]
> Dev Services for Kubernetes starts the api server container with the `quarkus-dev-service-kubernetes` label which is used to identify it.

If you need multiple (shared) clusters, you can provide a value for the `quarkus.kubernetes-client.devservices.service-name` configuration property to specify the name of the cluster that will be shared among the selected applications. The dev service support will look for an already existing container with that specified name or starts a new one if none can be found.The default service name is `kubernetes`.

Sharing is enabled by default in dev mode, but disabled in test mode. You can disable the sharing with `quarkus.kubernetes-client.devservices.shared=false`.

## Configuring the cluster

Dev Services for Kubernetes provides three different flavors of Kubernetes cluster. Each flavor supports different Kubernetes API versions. You can configure the flavor and version using the `quarkus.kubernetes-client.devservices.flavor` and `quarkus.kubernetes-client.devservices.api-version` properties:

```properties
quarkus.kubernetes-client.devservices.flavor=api-only # k3s or kind
quarkus.kubernetes-client.devservices.api-version=1.22
```

You can also configure a custom image compatible with standard images (kind, k3s & api-server) using the `quarkus.kubernetes-client.devservices.image-name` property. However, it must be consistent with the flavor and api-version properties:

```properties
quarkus.kubernetes-client.devservices.flavor=api-only # k3s or kind
quarkus.kubernetes-client.devservices.api-version=1.24.1
quarkus.kubernetes-client.devservices.image-name=registry.k8s.io/kube-apiserver:v1.24.1
```

`api-only` only starts a Kubernetes API Server (plus the required etcd). If you need a fully-featured Kubernetes cluster that can spin up Pods, you can use `k3s` or `kind`. `k3s` requires to start the container with `privileged mode`. The `kind` test container now also supports using podman's rootless mode.

If `api-version` is not set, the latest version for the given flavor will be used. Otherwise, the version must match a [version supported by the given flavor](https://github.com/dajudge/kindcontainer/blob/master/k8s-versions.json).

Once the cluster is configured, you can access it easily as you normally would, for example, by injecting a client instance in your test.

## Applying manifests to the cluster

Within Quarkus it is possible to apply Kubernetes manifests to the cluster dev service on startup. This is especially useful if your application expects certain resources to be present in the cluster, such as for example a specific namespace or certain Custom Resource Definitions that the application interacts with.

You can configure the manifests to be applied using the `quarkus.kubernetes-client.devservices.manifests` property. This property accepts a list of files within the application's `resources` directory.

As an example, to ensure the `example-namespace` namespace is present in the dev service, create the file `/src/main/resources/kubernetes/namespace.yml`: ```yaml apiVersion: v1 kind: Namespace metadata: name: example-namespace ```

And then add the following property to your `application.properties`: ``` quarkus.kubernetes-client.devservices.manifests=kubernetes/namespace.yaml ```

When applying multiple properties, you can apply them as: ``` quarkus.kubernetes-client.devservices.manifests=kubernetes/first_manifest.yaml,kubernetes/second_manifest.yaml ```

It is also possible to apply manifests from a URL. So the following syntax would also be possible: ``` quarkus.kubernetes-client.devservices.manifests=https://example.com/kubernetes/namespace.yaml ```

> [!NOTE]
> Quarkus will apply the manifests in the order they are specified in the list.

> [!NOTE]
> Quarkus will await resources such as deployments to be ready before moving on to the next manifest. All applied resources are guaranteed to be ready by the time your application finishes startup.

### Deploying helm charts

`quarkus.kubernetes-client.devservices.manifests` only supports manifests. If you want to deploy a helm chart, you can use the `k3s` flavor and deploy a `HelmChart` manifest. See <https://docs.k3s.io/helm> for more information on how to use helm charts with k3s.

## Configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-devservices_quarkus-kubernetes-client-devservices-enabled"></a> [`quarkus.kubernetes-client.devservices.enabled`](#quarkus-kubernetes-client_quarkus-kubernetes-client-devservices_quarkus-kubernetes-client-devservices-enabled) config_property_copy_button:+++quarkus.kubernetes-client.devservices.enabled+++[]<br><br>If Dev Services for Kubernetes should be used. (default to true)<br><br>If this is true and kubernetes client is not configured then a kubernetes cluster will be started and will be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_DEVSERVICES_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-devservices_quarkus-kubernetes-client-devservices-api-version"></a> [`quarkus.kubernetes-client.devservices.api-version`](#quarkus-kubernetes-client_quarkus-kubernetes-client-devservices_quarkus-kubernetes-client-devservices-api-version) config_property_copy_button:+++quarkus.kubernetes-client.devservices.api-version+++[]<br><br>The kubernetes api server version to use.<br><br>If not set, Dev Services for Kubernetes will use the [latest supported version](https://github.com/dajudge/kindcontainer/blob/master/k8s-versions.json) of the given flavor.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_DEVSERVICES_API_VERSION+++[] | string |  |
| [lock] <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-devservices_quarkus-kubernetes-client-devservices-image-name"></a> [`quarkus.kubernetes-client.devservices.image-name`](#quarkus-kubernetes-client_quarkus-kubernetes-client-devservices_quarkus-kubernetes-client-devservices-image-name) config_property_copy_button:+++quarkus.kubernetes-client.devservices.image-name+++[]<br><br>The kubernetes image to use.<br><br>If not set, Dev Services for Kubernetes will use default image for the specified `api-version()` for the given `flavor()`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_DEVSERVICES_IMAGE_NAME+++[] | string |  |
| [lock] <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-devservices_quarkus-kubernetes-client-devservices-flavor"></a> [`quarkus.kubernetes-client.devservices.flavor`](#quarkus-kubernetes-client_quarkus-kubernetes-client-devservices_quarkus-kubernetes-client-devservices-flavor) config_property_copy_button:+++quarkus.kubernetes-client.devservices.flavor+++[]<br><br>The flavor to use (kind, k3s or api-only).<br><br>If not set, Dev Services for Kubernetes will set it to: api-only.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_DEVSERVICES_FLAVOR+++[] | tooltip:kind[kind (needs privileged docker)], tooltip:k3s[k3s (needs privileged docker)], tooltip:api-only[api only] |  |
| [lock] <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-devservices_quarkus-kubernetes-client-devservices-override-kubeconfig"></a> [`quarkus.kubernetes-client.devservices.override-kubeconfig`](#quarkus-kubernetes-client_quarkus-kubernetes-client-devservices_quarkus-kubernetes-client-devservices-override-kubeconfig) config_property_copy_button:+++quarkus.kubernetes-client.devservices.override-kubeconfig+++[]<br><br>By default, if a kubeconfig is found, Dev Services for Kubernetes will not start. Set this to true to override the kubeconfig config.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_DEVSERVICES_OVERRIDE_KUBECONFIG+++[] | boolean | `false` |
| [lock] <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-devservices_quarkus-kubernetes-client-devservices-manifests"></a> [`quarkus.kubernetes-client.devservices.manifests`](#quarkus-kubernetes-client_quarkus-kubernetes-client-devservices_quarkus-kubernetes-client-devservices-manifests) config_property_copy_button:+++quarkus.kubernetes-client.devservices.manifests+++[]<br><br>A list of manifest file paths that should be applied to the Kubernetes cluster Dev Service on startup. If not set, no manifests are applied.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_DEVSERVICES_MANIFESTS+++[] | list of string |  |
| [lock] <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-devservices_quarkus-kubernetes-client-devservices-shared"></a> [`quarkus.kubernetes-client.devservices.shared`](#quarkus-kubernetes-client_quarkus-kubernetes-client-devservices_quarkus-kubernetes-client-devservices-shared) config_property_copy_button:+++quarkus.kubernetes-client.devservices.shared+++[]<br><br>Indicates if the Kubernetes cluster managed by Quarkus Dev Services is shared. When shared, Quarkus looks for running containers using label-based service discovery. If a matching container is found, it is used, and so a second one is not started. Otherwise, Dev Services for Kubernetes starts a new container.<br><br>The discovery uses the `quarkus-dev-service-kubernetes` label. The value is configured using the `service-name` property.<br><br>Container sharing is only used in dev mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_DEVSERVICES_SHARED+++[] | boolean | `true` |
| [lock] <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-devservices_quarkus-kubernetes-client-devservices-service-name"></a> [`quarkus.kubernetes-client.devservices.service-name`](#quarkus-kubernetes-client_quarkus-kubernetes-client-devservices_quarkus-kubernetes-client-devservices-service-name) config_property_copy_button:+++quarkus.kubernetes-client.devservices.service-name+++[]<br><br>The value of the `quarkus-dev-service-kubernetes` label attached to the started container. This property is used when `shared` is set to `true`. In this case, before starting a container, Dev Services for Kubernetes looks for a container with the `quarkus-dev-service-kubernetes` label set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it starts a new container with the `quarkus-dev-service-kubernetes` label set to the specified value.<br><br>This property is used when you need multiple shared Kubernetes clusters.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_DEVSERVICES_SERVICE_NAME+++[] | string | `kubernetes` |
| [lock] <a id="quarkus-kubernetes-client_quarkus-kubernetes-client-devservices_quarkus-kubernetes-client-devservices-container-env-environment-variable-name"></a> [`quarkus.kubernetes-client.devservices.container-env."environment-variable-name"`](#quarkus-kubernetes-client_quarkus-kubernetes-client-devservices_quarkus-kubernetes-client-devservices-container-env-environment-variable-name) config_property_copy_button:+++quarkus.kubernetes-client.devservices.container-env."environment-variable-name"+++[]<br><br>Environment variables that are passed to the container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KUBERNETES_CLIENT_DEVSERVICES_CONTAINER_ENV__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |
