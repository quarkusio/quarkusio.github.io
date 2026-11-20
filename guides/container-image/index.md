# Container images

Quarkus provides extensions for building (and pushing) container images. Currently, it supports:

- [Jib](#jib)
- [Docker](#docker)
- [Podman](#podman)
- [OpenShift](#openshift)
- [Buildpack](#buildpack)

## Container Image extensions

<a id="jib"></a>
### Jib

The extension `quarkus-container-image-jib` is powered by [Jib](https://github.com/GoogleContainerTools/jib) for performing container image builds. The major benefit of using Jib with Quarkus is that all the dependencies (everything found under `target/lib`) are cached in a different layer than the actual application making rebuilds really fast and small (when it comes to pushing). Another important benefit of using this extension is that it provides the ability to create a container image without having to have any dedicated client side tooling (like Docker) or running daemon processes (like the Docker daemon) when all that is needed is the ability to push to a container image registry.

To use this feature, add the following extension to your project:

**CLI**

```bash
quarkus extension add quarkus-container-image-jib
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='quarkus-container-image-jib'
```

**Gradle**

```bash
./gradlew addExtension --extensions='quarkus-container-image-jib'
```

> [!WARNING]
> In situations where all that is needed to build a container image and no push to a registry is necessary (essentially by having set `quarkus.container-image.build=true` and left `quarkus.container-image.push` unset - it defaults to `false`), then this extension creates a container image and registers it with the Docker daemon. This means that although Docker isn't used to build the image, it is nevertheless necessary. Also note that using this mode, the built container image **will** show up when executing `docker images`.

#### Including extra files

There are cases when additional files (other than ones produced by the Quarkus build) need to be added to a container image. To support these cases, Quarkus copies any file under `src/main/jib` into the built container image (which is essentially the same idea that the Jib Maven and Gradle plugins support). For example, the presence of `src/main/jib/foo/bar` would result in  `/foo/bar` being added into the container filesystem.

#### JVM Debugging

There are cases where the built container image may need to have Java debugging conditionally enabled at runtime.

When the base image has not been changed (and therefore `ubi9/openjdk-17-runtime`, or `ubi9/openjdk-21-runtime` is used), then the `quarkus.jib.jvm-additional-arguments` configuration property can be used in order to make the JVM listen on the debug port at startup.

The exact configuration is:

```properties
quarkus.jib.jvm-additional-arguments=-agentlib:jdwp=transport=dt_socket\\,server=y\\,suspend=n\\,address=*:5005
```

Other base images might provide launch scripts that enable debugging when an environment variable is set, in which case you would set that environment variable when launching the container.

#### Custom Entrypoint

The `quarkus.jib.jvm-entrypoint` configuration property can be used to completely override the container entry point and can thus be used to either hard code the JVM debug configuration or point to a script that handles the details.

For example, if the base images `ubi9/openjdk-17-runtime` or  `ubi9/openjdk-21-runtime` are used to build the container, the entry point can be hard-coded on the application properties file.

**Example application.properties**

```properties
quarkus.jib.jvm-entrypoint=java,-Dcustom.param=custom_value,-jar,quarkus-run.jar
```

Or a custom start-up script can be created and referenced on the properties file. This approach works better if there's a need to set application params using environment variables:

**Example application.properties**

```properties
quarkus.jib.jvm-entrypoint=/bin/sh,run-java.sh
```

**Example src/main/jib/home/jboss/run-java.sh**

```shell
java \
  -Djavax.net.ssl.trustStore=/deployments/truststore \
  -Djavax.net.ssl.trustStorePassword="$TRUST_STORE_PASSWORD" \
  -jar quarkus-run.jar
```

> [!NOTE]
> `/home/jboss` is the WORKDIR for all quarkus binaries in the base images `ubi9/openjdk-17-runtime` and `ubi9/openjdk-21-runtime` ([Dockerfile for ubi9/openjdk-17-runtime](https://catalog.redhat.com/software/containers/ubi9/openjdk-21-runtime/6501ce769a0d86945c422d5f?container-tabs=dockerfile))

#### Multi-module projects and layering

When building a multi-module project containing a Quarkus application as one module and various supporting project dependencies as other modules, Quarkus supports placing these supporting modules in a separate container image layer from the rest of the application dependencies, with the expectation that these supporting modules will change more frequently than the regular application dependencies - thus making a rebuild faster if the application dependencies have not changed.

To enable this feature, the property `quarkus.bootstrap.workspace-discovery` needs to be set to `true` either as a system property when invoking the build tool, either as a build tool property. Setting this property in `application.properties` will **not** work because this property needs to be known very early on in the build process.

#### AppCDS

Quarkus supports generating and including AOT caches (including [Application Class Data Sharing](https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html#application-class-data-sharing) archives) when generating container images. See the [AOT Caching documentation](/guides/aot/index.md) for more details.

<a id="docker"></a>
### Docker

The extension `quarkus-container-image-docker` is using the Docker binary and the generated Dockerfiles under `src/main/docker` in order to perform Docker builds.

To use this feature, add the following extension to your project.

**CLI**

```bash
quarkus extension add quarkus-container-image-docker
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='quarkus-container-image-docker'
```

**Gradle**

```bash
./gradlew addExtension --extensions='quarkus-container-image-docker'
```

The `quarkus-container-image-docker` extension is capable of [creating multi-platform (or multi-arch)](https://docs.docker.com/buildx/working-with-buildx/#build-multi-platform-images/) images using [`docker buildx build`](https://docs.docker.com/engine/reference/commandline/buildx_build/). See the `quarkus.docker.buildx.*` configuration items in the [Docker Options](#DockerOptions) section below.

> [!NOTE]
> `docker buildx build` ONLY supports [loading the result of a build](https://docs.docker.com/engine/reference/commandline/buildx_build/#load) to `docker images` when building for a single platform. Therefore, if you specify more than one argument in the `quarkus.docker.buildx.platform` property, the resulting images will not be loaded into `docker images`. If `quarkus.docker.buildx.platform` is omitted or if only a single platform is specified, it will then be loaded into `docker images`.
>
> This means that if you want to build images for more than one platform at a time (i.e. `quarkus.docker.buildx.platform=linux/amd64,linux/arm64`), you need to push the images (`quarkus.container-image.push=true`) directly as part of the build process. Docker buildx does not support loading into the local registry when building multi-platform images.

<a id="podman"></a>
### Podman

The extension `quarkus-container-image-podman` uses [Podman](https://podman.io/) and the generated `Dockerfiles` under `src/main/docker` in order to perform container builds.

To use this feature, add the following extension to your project.

**CLI**

```bash
quarkus extension add quarkus-container-image-podman
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='quarkus-container-image-podman'
```

**Gradle**

```bash
./gradlew addExtension --extensions='quarkus-container-image-podman'
```

> [!TIP]
> **When to use Docker vs Podman extension**
>
> The [Docker extension](#docker) is and has always been backwards-compatible with Podman because Podman exposes a [Docker-compatible API](https://podman.io/docs/installation). You can build container images with Podman using the Docker extension (see the [Using Podman with Quarkus guide](https://quarkus.io/guides/podman)).
>
> Use either the `quarkus-container-image-docker` or `quarkus-container-image-podman` extension when doing things specific to either Docker or Podman, respectively.
>
> For example, building multi-platform images is implemented differently for Docker and Podman. Docker uses [the buildx plugin](https://docs.docker.com/engine/reference/commandline/buildx_build/) whereas Podman can build multi-platform images [natively](https://docs.podman.io/en/latest/markdown/podman-build.1.html#platform-os-arch-variant). Because of this, you would need to use the specific extension to perform that function.

<a id="openshift"></a>
### <a id="s2i"></a>OpenShift

The extension `quarkus-container-image-openshift` is using OpenShift binary builds in order to perform container builds inside the OpenShift cluster. The idea behind the binary build is that you just upload the artifact and its dependencies to the cluster and during the build they will be merged to a builder image (defaults to `ubi9/openjdk-17` or `ubi9/openjdk-21`).

The benefit of this approach, is that it can be combined with OpenShift's `DeploymentConfig` that makes it easy to roll out changes to the cluster.

To use this feature, add the following extension to your project.

**CLI**

```bash
quarkus extension add quarkus-container-image-openshift
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='quarkus-container-image-openshift'
```

**Gradle**

```bash
./gradlew addExtension --extensions='quarkus-container-image-openshift'
```

OpenShift builds require creating a `BuildConfig` and two `ImageStream` resources, one for the builder image and one for the output image. The creation of such objects is being taken care of by the Quarkus Kubernetes extension.

<a id="buildpack"></a>
### Buildpack

The extension `quarkus-container-image-buildpack` is using buildpacks in order to perform container image builds. Under the hood buildpacks will use a Docker daemon for the actual build. While buildpacks support alternatives to Docker, this extension will only work with Docker.

Additionally, the user will have to configure which build image to use (no default image is provided). For example:

```properties
quarkus.buildpack.jvm-builder-image=<jvm builder image>
```

or for native:

```properties
quarkus.buildpack.native-builder-image=<native builder image>
```

To use this feature, add the following extension to your project.

**CLI**

```bash
quarkus extension add quarkus-container-image-buildpack
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='quarkus-container-image-buildpack'
```

**Gradle**

```bash
./gradlew addExtension --extensions='quarkus-container-image-buildpack'
```

> [!NOTE]
> When using the buildpack container image extension it is strongly advised to avoid adding `quarkus.container-image.build=true` in your properties configuration as it might trigger nesting builds within builds. It's preferable to pass it as an option to the build command instead.

## Building

To build a container image for your project, `quarkus.container-image.build=true` needs to be set using any of the ways that Quarkus supports.

**CLI**

```bash
quarkus build
quarkus deploy openshift
```

**Maven**

```bash
./mvnw install -Dquarkus.container-image.build=true
```

**Gradle**

```bash
./gradlew build -Dquarkus.container-image.build=true
```

> [!NOTE]
> If you ever want to build a native container image and already have an existing native image you can set `-Dquarkus.native.reuse-existing=true` and the native image build will not be re-run.

## Using @QuarkusIntegrationTest

To run tests on the resulting image, `quarkus.container-image.build=true` needs to be set using any of the ways that Quarkus supports.

**Maven**

```bash
./mvnw verify -Dquarkus.container-image.build=true
```

**Gradle**

```bash
./gradlew quarkusIntTest -Dquarkus.container-image.build=true
```

## Pushing

To push a container image for your project, `quarkus.container-image.push=true` needs to be set using any of the ways that Quarkus supports.

**CLI**

```bash
quarkus build
quarkus deploy openshift
```

**Maven**

```bash
./mvnw install -Dquarkus.container-image.push=true
```

**Gradle**

```bash
./gradlew build -Dquarkus.container-image.push=true
```

> [!NOTE]
> If no registry is set (using `quarkus.container-image.registry`) then `docker.io` will be used as the default.

## Selecting among multiple extensions

It does not make sense to use multiple extension as part of the same build. When multiple container image extensions are present, an error will be raised to inform the user. The user can either remove the unneeded extensions or select one using `application.properties`.

For example, if both `container-image-docker` and `container-image-podman` are present and the user needs to use `container-image-docker`:

```properties
quarkus.container-image.builder=docker
```

## Integrating with `systemd-notify`

If you are building a container image in order to deploy your Quarkus application as a Linux service with Podman and Systemd, you might want to consider including the [Quarkus Systemd Notify Extension](https://docs.quarkiverse.io/quarkus-systemd-notify/dev/index.html) as part of your application, with:

**CLI**

```bash
quarkus extension add io.quarkiverse.systemd.notify:quarkus-systemd-notify
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='io.quarkiverse.systemd.notify:quarkus-systemd-notify'
```

**Gradle**

```bash
./gradlew addExtension --extensions='io.quarkiverse.systemd.notify:quarkus-systemd-notify'
```

## Customizing

The following properties can be used to customize the container image build process.

### Container Image Options

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-container-image_quarkus-container-image-group"></a> [`quarkus.container-image.group`](#quarkus-container-image_quarkus-container-image-group) config_property_copy_button:+++quarkus.container-image.group+++[]<br><br>The group the container image will be part of<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CONTAINER_IMAGE_GROUP+++[] | string |  |
| [lock] <a id="quarkus-container-image_quarkus-container-image-name"></a> [`quarkus.container-image.name`](#quarkus-container-image_quarkus-container-image-name) config_property_copy_button:+++quarkus.container-image.name+++[]<br><br>The name of the container image. If not set defaults to the application name<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CONTAINER_IMAGE_NAME+++[] | string | `${quarkus.application.name:unset}` |
| [lock] <a id="quarkus-container-image_quarkus-container-image-tag"></a> [`quarkus.container-image.tag`](#quarkus-container-image_quarkus-container-image-tag) config_property_copy_button:+++quarkus.container-image.tag+++[]<br><br>The tag of the container image. If not set defaults to the application version<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CONTAINER_IMAGE_TAG+++[] | string |  |
| [lock] <a id="quarkus-container-image_quarkus-container-image-additional-tags"></a> [`quarkus.container-image.additional-tags`](#quarkus-container-image_quarkus-container-image-additional-tags) config_property_copy_button:+++quarkus.container-image.additional-tags+++[]<br><br>Additional tags of the container image.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CONTAINER_IMAGE_ADDITIONAL_TAGS+++[] | list of string |  |
| [lock] <a id="quarkus-container-image_quarkus-container-image-labels-label-name"></a> [`quarkus.container-image.labels."label-name"`](#quarkus-container-image_quarkus-container-image-labels-label-name) config_property_copy_button:+++quarkus.container-image.labels."label-name"+++[]<br><br>Custom labels to add to the generated image.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CONTAINER_IMAGE_LABELS__LABEL_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-container-image_quarkus-container-image-registry"></a> [`quarkus.container-image.registry`](#quarkus-container-image_quarkus-container-image-registry) config_property_copy_button:+++quarkus.container-image.registry+++[]<br><br>The container registry to use<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CONTAINER_IMAGE_REGISTRY+++[] | string |  |
| [lock] <a id="quarkus-container-image_quarkus-container-image-image"></a> [`quarkus.container-image.image`](#quarkus-container-image_quarkus-container-image-image) config_property_copy_button:+++quarkus.container-image.image+++[]<br><br>Represents the entire image string. If set, then `group`, `name`, `registry`, `tags`, `additionalTags` are ignored<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CONTAINER_IMAGE_IMAGE+++[] | string |  |
| [lock] <a id="quarkus-container-image_quarkus-container-image-username"></a> [`quarkus.container-image.username`](#quarkus-container-image_quarkus-container-image-username) config_property_copy_button:+++quarkus.container-image.username+++[]<br><br>The username to use to authenticate with the registry where the built image will be pushed<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CONTAINER_IMAGE_USERNAME+++[] | string |  |
| [lock] <a id="quarkus-container-image_quarkus-container-image-password"></a> [`quarkus.container-image.password`](#quarkus-container-image_quarkus-container-image-password) config_property_copy_button:+++quarkus.container-image.password+++[]<br><br>The password to use to authenticate with the registry where the built image will be pushed<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CONTAINER_IMAGE_PASSWORD+++[] | string |  |
| [lock] <a id="quarkus-container-image_quarkus-container-image-insecure"></a> [`quarkus.container-image.insecure`](#quarkus-container-image_quarkus-container-image-insecure) config_property_copy_button:+++quarkus.container-image.insecure+++[]<br><br>Whether or not insecure registries are allowed<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CONTAINER_IMAGE_INSECURE+++[] | boolean | `false` |
| [lock] <a id="quarkus-container-image_quarkus-container-image-build"></a> [`quarkus.container-image.build`](#quarkus-container-image_quarkus-container-image-build) config_property_copy_button:+++quarkus.container-image.build+++[]<br><br>Whether or not a image build will be performed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CONTAINER_IMAGE_BUILD+++[] | boolean |  |
| [lock] <a id="quarkus-container-image_quarkus-container-image-push"></a> [`quarkus.container-image.push`](#quarkus-container-image_quarkus-container-image-push) config_property_copy_button:+++quarkus.container-image.push+++[]<br><br>Whether or not an image push will be performed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CONTAINER_IMAGE_PUSH+++[] | boolean |  |
| [lock] <a id="quarkus-container-image_quarkus-container-image-builder"></a> [`quarkus.container-image.builder`](#quarkus-container-image_quarkus-container-image-builder) config_property_copy_button:+++quarkus.container-image.builder+++[]<br><br>The name of the container image extension to use (e.g. docker, podman, jib, s2i). The option will be used in case multiple extensions are present.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CONTAINER_IMAGE_BUILDER+++[] | string |  |
| [lock] <a id="quarkus-container-image_quarkus-container-image-aot-image-suffix"></a> [`quarkus.container-image.aot-image-suffix`](#quarkus-container-image_quarkus-container-image-aot-image-suffix) config_property_copy_button:+++quarkus.container-image.aot-image-suffix+++[]<br><br>The suffix to be used to create a new container image when an AOT file has been created. When this value is empty, Quarkus will interpret this as a request to not create a separate container image, but instead will just re-create the original one with the AOT file included<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CONTAINER_IMAGE_AOT_IMAGE_SUFFIX+++[] | string | `-aot` |

#### Using CI Environments

Various CI environments provide a ready to use container-image registry which can be combined with the container-image Quarkus extensions in order to effortlessly create and push a Quarkus application to said registry.

For example, [GitLab](https://gitlab.com/) provides such a registry and in the provided CI environment, makes available the `CI_REGISTRY_IMAGE` environment variable (see GitLab's [documentation](https://docs.gitlab.com/ee/ci/variables/)) for more information), which can be used in Quarkus like so:

```properties
quarkus.container-image.image=${CI_REGISTRY_IMAGE}
```

> [!NOTE]
> See [this](/guides/config-reference/index.md#with-environment-variables) for more information on how to combine properties with environment variables.

### Jib Options

In addition to the generic container image options, the `container-image-jib` also provides the following options:

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-base-jvm-image"></a> [`quarkus.jib.base-jvm-image`](#quarkus-container-image-jib_quarkus-jib-base-jvm-image) config_property_copy_button:+++quarkus.jib.base-jvm-image+++[]<br><br>The base image to be used when a container image is being produced for the jar build. When the application is built against Java 25 or higher, `registry.access.redhat.com/ubi9/openjdk-25-runtime:1.24` is used as the default. Otherwise, when the application is built against Java 21 to Java 24, `registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24` is used as the default. Otherwise `registry.access.redhat.com/ubi9/openjdk-17-runtime:1.24` is used as the default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_BASE_JVM_IMAGE+++[] | string |  |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-base-native-image"></a> [`quarkus.jib.base-native-image`](#quarkus-container-image-jib_quarkus-jib-base-native-image) config_property_copy_button:+++quarkus.jib.base-native-image+++[]<br><br>The base image to be used when a container image is being produced for the native binary build. The default is "quay.io/quarkus/ubi9-quarkus-micro-image:2.0". You can also use "registry.access.redhat.com/ubi9/ubi-minimal" which is a bigger base image, but provide more built-in utilities such as the microdnf package manager.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_BASE_NATIVE_IMAGE+++[] | string | `quay.io/quarkus/ubi9-quarkus-micro-image:2.0` |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-jvm-arguments"></a> [`quarkus.jib.jvm-arguments`](#quarkus-container-image-jib_quarkus-jib-jvm-arguments) config_property_copy_button:+++quarkus.jib.jvm-arguments+++[]<br><br>The JVM arguments to pass to the JVM when starting the application<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_JVM_ARGUMENTS+++[] | list of string | `-Djava.util.logging.manager=org.jboss.logmanager.LogManager` |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-jvm-additional-arguments"></a> [`quarkus.jib.jvm-additional-arguments`](#quarkus-container-image-jib_quarkus-jib-jvm-additional-arguments) config_property_copy_button:+++quarkus.jib.jvm-additional-arguments+++[]<br><br>Additional JVM arguments to pass to the JVM when starting the application<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_JVM_ADDITIONAL_ARGUMENTS+++[] | list of string |  |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-native-arguments"></a> [`quarkus.jib.native-arguments`](#quarkus-container-image-jib_quarkus-jib-native-arguments) config_property_copy_button:+++quarkus.jib.native-arguments+++[]<br><br>Additional arguments to pass when starting the native application<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_NATIVE_ARGUMENTS+++[] | list of string |  |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-jvm-entrypoint"></a> [`quarkus.jib.jvm-entrypoint`](#quarkus-container-image-jib_quarkus-jib-jvm-entrypoint) config_property_copy_button:+++quarkus.jib.jvm-entrypoint+++[]<br><br>If this is set, then it will be used as the entry point of the container image. There are a few things to be aware of when creating an entry point<br><br>\* Entrypoint "INHERIT" means to inherit entrypoint from base image, `jvmArguments` field is used for arguments \* A valid entrypoint is jar package specific (see `quarkus.package.jar.type`) \* A valid entrypoint depends on the location of both the launching scripts and the application jar file. To that end it's helpful to remember that when `fast-jar` packaging is used (the default), all necessary application jars are added to the `/work` directory and that the same directory is also used as the working directory. When `legacy-jar` or `uber-jar` are used, the application jars are unpacked under the `/app` directory and that directory is used as the working directory. \* Even if the `jvmArguments` field is set, it is ignored completely unless entrypoint is "INHERIT"<br><br>When this is not set, a proper default entrypoint will be constructed. As a final note, a very useful tool for inspecting container image layers that can greatly aid when debugging problems with endpoints is [dive](https://github.com/wagoodman/dive)<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_JVM_ENTRYPOINT+++[] | list of string |  |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-native-entrypoint"></a> [`quarkus.jib.native-entrypoint`](#quarkus-container-image-jib_quarkus-jib-native-entrypoint) config_property_copy_button:+++quarkus.jib.native-entrypoint+++[]<br><br>If this is set, then it will be used as the entry point of the container image. There are a few things to be aware of when creating an entry point<br><br>\* Entrypoint "INHERIT" means to inherit entrypoint from base image, `nativeArguments` field is used for arguments \* A valid entrypoint depends on the location of both the launching scripts and the native binary file. To that end it's helpful to remember that the native application is added to the `/work` directory and that and the same directory is also used as the working directory \* Even if the `nativeArguments` field is set, it is ignored completely unless entrypoint is "INHERIT"<br><br>When this is not set, a proper default entrypoint will be constructed. As a final note, a very useful tool for inspecting container image layers that can greatly aid when debugging problems with endpoints is [dive](https://github.com/wagoodman/dive)<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_NATIVE_ENTRYPOINT+++[] | list of string |  |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-environment-variables-environment-variable-name"></a> [`quarkus.jib.environment-variables."environment-variable-name"`](#quarkus-container-image-jib_quarkus-jib-environment-variables-environment-variable-name) config_property_copy_button:+++quarkus.jib.environment-variables."environment-variable-name"+++[]<br><br>Environment variables to add to the container image<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_ENVIRONMENT_VARIABLES__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-base-registry-username"></a> [`quarkus.jib.base-registry-username`](#quarkus-container-image-jib_quarkus-jib-base-registry-username) config_property_copy_button:+++quarkus.jib.base-registry-username+++[]<br><br>The username to use to authenticate with the registry used to pull the base JVM image<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_BASE_REGISTRY_USERNAME+++[] | string |  |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-base-registry-password"></a> [`quarkus.jib.base-registry-password`](#quarkus-container-image-jib_quarkus-jib-base-registry-password) config_property_copy_button:+++quarkus.jib.base-registry-password+++[]<br><br>The password to use to authenticate with the registry used to pull the base JVM image<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_BASE_REGISTRY_PASSWORD+++[] | string |  |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-ports"></a> [`quarkus.jib.ports`](#quarkus-container-image-jib_quarkus-jib-ports) config_property_copy_button:+++quarkus.jib.ports+++[]<br><br>The ports to expose<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_PORTS+++[] | list of int | `${quarkus.http.port:8080}` |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-user"></a> [`quarkus.jib.user`](#quarkus-container-image-jib_quarkus-jib-user) config_property_copy_button:+++quarkus.jib.user+++[]<br><br>The user to use in generated image<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_USER+++[] | string |  |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-working-directory"></a> [`quarkus.jib.working-directory`](#quarkus-container-image-jib_quarkus-jib-working-directory) config_property_copy_button:+++quarkus.jib.working-directory+++[]<br><br>The working directory to use in the generated image. The default value is chosen to work in accordance with the default base image.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_WORKING_DIRECTORY+++[] | string | `/home/jboss` |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-always-cache-base-image"></a> [`quarkus.jib.always-cache-base-image`](#quarkus-container-image-jib_quarkus-jib-always-cache-base-image) config_property_copy_button:+++quarkus.jib.always-cache-base-image+++[]<br><br>Controls the optimization which skips downloading base image layers that exist in a target registry. If the user does not set this property, then read as false. If `true`, base image layers are always pulled and cached. If `false`, base image layers will not be pulled/cached if they already exist on the target registry.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_ALWAYS_CACHE_BASE_IMAGE+++[] | boolean | `false` |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-platforms"></a> [`quarkus.jib.platforms`](#quarkus-container-image-jib_quarkus-jib-platforms) config_property_copy_button:+++quarkus.jib.platforms+++[]<br><br>List of target platforms. Each platform is defined using the pattern:<br><br>```<br><os>\|<arch>[/variant]\|<os>/<arch>[/variant]<br>```<br><br>for example:<br><br>```<br>linux/amd64,linux/arm64/v8<br>```<br><br>If not specified, OS default is linux and architecture default is `amd64`. If more than one platform is configured, it is important to note that the base image has to be a Docker manifest or an OCI image index containing a version of each chosen platform. The feature does not work with native images, as cross-compilation is not supported. This configuration is based on an incubating feature of Jib. See [Jib FAQ](https://github.com/GoogleContainerTools/jib/blob/master/docs/faq.md#how-do-i-specify-a-platform-in-the-manifest-list-or-oci-index-of-a-base-image) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_PLATFORMS+++[] | list of string |  |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-image-digest-file"></a> [`quarkus.jib.image-digest-file`](#quarkus-container-image-jib_quarkus-jib-image-digest-file) config_property_copy_button:+++quarkus.jib.image-digest-file+++[]<br><br>The path of a file in which the digest of the generated image will be written. If the path is relative, the base path is the output directory of the build tool.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_IMAGE_DIGEST_FILE+++[] | string | `jib-image.digest` |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-image-id-file"></a> [`quarkus.jib.image-id-file`](#quarkus-container-image-jib_quarkus-jib-image-id-file) config_property_copy_button:+++quarkus.jib.image-id-file+++[]<br><br>The path of a file in which the id of the generated image will be written. If the path is relative, the base path is the output directory of the build tool.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_IMAGE_ID_FILE+++[] | string | `jib-image.id` |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-offline-mode"></a> [`quarkus.jib.offline-mode`](#quarkus-container-image-jib_quarkus-jib-offline-mode) config_property_copy_button:+++quarkus.jib.offline-mode+++[]<br><br>Whether, or not to operate offline.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_OFFLINE_MODE+++[] | boolean | `false` |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-docker-executable-name"></a> [`quarkus.jib.docker-executable-name`](#quarkus-container-image-jib_quarkus-jib-docker-executable-name) config_property_copy_button:+++quarkus.jib.docker-executable-name+++[]<br><br>Name of binary used to execute the docker commands. This is only used by Jib when the container image is being built locally.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_DOCKER_EXECUTABLE_NAME+++[] | string |  |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-docker-environment-environment-variable-name"></a> [`quarkus.jib.docker-environment."environment-variable-name"`](#quarkus-container-image-jib_quarkus-jib-docker-environment-environment-variable-name) config_property_copy_button:+++quarkus.jib.docker-environment."environment-variable-name"+++[]<br><br>Sets environment variables used by the Docker executable. This is only used by Jib when the container image is being built locally.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_DOCKER_ENVIRONMENT__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-use-current-timestamp"></a> [`quarkus.jib.use-current-timestamp`](#quarkus-container-image-jib_quarkus-jib-use-current-timestamp) config_property_copy_button:+++quarkus.jib.use-current-timestamp+++[]<br><br>Whether to set the creation time to the actual build time. Otherwise, the creation time will be set to the Unix epoch (00:00:00, January 1st, 1970 in UTC). See [Jib FAQ](https://github.com/GoogleContainerTools/jib/blob/master/docs/faq.md#why-is-my-image-created-48-years-ago) for more information<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_USE_CURRENT_TIMESTAMP+++[] | boolean | `true` |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-use-current-timestamp-file-modification"></a> [`quarkus.jib.use-current-timestamp-file-modification`](#quarkus-container-image-jib_quarkus-jib-use-current-timestamp-file-modification) config_property_copy_button:+++quarkus.jib.use-current-timestamp-file-modification+++[]<br><br>Whether to set the modification time (last modified time) of the files put by Jib in the image to the actual build time. Otherwise, the modification time will be set to the Unix epoch (00:00:00, January 1st, 1970 in UTC). If the modification time is constant (flag is set to false so Unix epoch is used) across two consecutive builds, the docker layer sha256 digest will be different only if the actual files added by Jib to the docker layer were changed. More exactly, having 2 consecutive builds will generate different docker layers only if the actual content of the files within the docker layer was changed. If the current timestamp is used the sha256 digest of the docker layer will always be different even if the content of the files didn't change.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_USE_CURRENT_TIMESTAMP_FILE_MODIFICATION+++[] | boolean | `true` |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-base-image-layers-cache"></a> [`quarkus.jib.base-image-layers-cache`](#quarkus-container-image-jib_quarkus-jib-base-image-layers-cache) config_property_copy_button:+++quarkus.jib.base-image-layers-cache+++[]<br><br>The directory to use for caching base image layers. If not specified, the Jib default directory is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_BASE_IMAGE_LAYERS_CACHE+++[] | string |  |
| [lock] <a id="quarkus-container-image-jib_quarkus-jib-application-layers-cache"></a> [`quarkus.jib.application-layers-cache`](#quarkus-container-image-jib_quarkus-jib-application-layers-cache) config_property_copy_button:+++quarkus.jib.application-layers-cache+++[]<br><br>The directory to use for caching application layers. If not specified, the Jib default directory is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JIB_APPLICATION_LAYERS_CACHE+++[] | string |  |

<a id="DockerOptions"></a>
### Docker Options

In addition to the generic container image options, the `container-image-docker` also provides the following options:

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-container-image-docker_quarkus-docker-dockerfile-jvm-path"></a> [`quarkus.docker.dockerfile-jvm-path`](#quarkus-container-image-docker_quarkus-docker-dockerfile-jvm-path) config_property_copy_button:+++quarkus.docker.dockerfile-jvm-path+++[]<br><br>Path to the JVM Dockerfile. If set to an absolute path then the absolute path will be used, otherwise the path will be considered relative to the project root. If not set src/main/docker/Dockerfile.jvm will be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DOCKER_DOCKERFILE_JVM_PATH+++[] | string | `src/main/docker/Dockerfile.jvm` |
| [lock] <a id="quarkus-container-image-docker_quarkus-docker-dockerfile-native-path"></a> [`quarkus.docker.dockerfile-native-path`](#quarkus-container-image-docker_quarkus-docker-dockerfile-native-path) config_property_copy_button:+++quarkus.docker.dockerfile-native-path+++[]<br><br>Path to the native Dockerfile. If set to an absolute path then the absolute path will be used, otherwise the path will be considered relative to the project root. If not set src/main/docker/Dockerfile.native will be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DOCKER_DOCKERFILE_NATIVE_PATH+++[] | string | `src/main/docker/Dockerfile.native` |
| [lock] <a id="quarkus-container-image-docker_quarkus-docker-build-args-arg-name"></a> [`quarkus.docker.build-args."arg-name"`](#quarkus-container-image-docker_quarkus-docker-build-args-arg-name) config_property_copy_button:+++quarkus.docker.build-args."arg-name"+++[]<br><br>Build args passed to docker via `--build-arg`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DOCKER_BUILD_ARGS__ARG_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-container-image-docker_quarkus-docker-cache-from"></a> [`quarkus.docker.cache-from`](#quarkus-container-image-docker_quarkus-docker-cache-from) config_property_copy_button:+++quarkus.docker.cache-from+++[]<br><br>Images to consider as cache sources. Values are passed to `docker build`/`podman build` via the `cache-from` option<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DOCKER_CACHE_FROM+++[] | list of string |  |
| [lock] <a id="quarkus-container-image-docker_quarkus-docker-network"></a> [`quarkus.docker.network`](#quarkus-container-image-docker_quarkus-docker-network) config_property_copy_button:+++quarkus.docker.network+++[]<br><br>The networking mode for the RUN instructions during build<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DOCKER_NETWORK+++[] | string |  |
| [lock] <a id="quarkus-container-image-docker_quarkus-docker-executable-name"></a> [`quarkus.docker.executable-name`](#quarkus-container-image-docker_quarkus-docker-executable-name) config_property_copy_button:+++quarkus.docker.executable-name+++[]<br><br>Name of binary used to execute the docker/podman commands. This setting can override the global container runtime detection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DOCKER_EXECUTABLE_NAME+++[] | string |  |
| [lock] <a id="quarkus-container-image-docker_quarkus-docker-additional-args"></a> [`quarkus.docker.additional-args`](#quarkus-container-image-docker_quarkus-docker-additional-args) config_property_copy_button:+++quarkus.docker.additional-args+++[]<br><br>Additional arbitrary arguments passed to the executable when building the container image.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DOCKER_ADDITIONAL_ARGS+++[] | list of string |  |
| <a id="quarkus-container-image-docker_section_quarkus-docker-buildx"></a> [Configuration for Docker Buildx options](#quarkus-container-image-docker_section_quarkus-docker-buildx) | Type | Default |
| [lock] <a id="quarkus-container-image-docker_quarkus-docker-buildx-platform"></a> [`quarkus.docker.buildx.platform`](#quarkus-container-image-docker_quarkus-docker-buildx-platform) config_property_copy_button:+++quarkus.docker.buildx.platform+++[]<br><br>Which platform(s) to target during the build. See <https://docs.docker.com/engine/reference/commandline/buildx_build/++#++platform><br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DOCKER_BUILDX_PLATFORM+++[] | list of string |  |
| [lock] <a id="quarkus-container-image-docker_quarkus-docker-buildx-output"></a> [`quarkus.docker.buildx.output`](#quarkus-container-image-docker_quarkus-docker-buildx-output) config_property_copy_button:+++quarkus.docker.buildx.output+++[]<br><br>Sets the export action for the build result. See <https://docs.docker.com/engine/reference/commandline/buildx_build/++#++output>. Note that any filesystem paths need to be absolute paths, not relative from where the command is executed from.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DOCKER_BUILDX_OUTPUT+++[] | string |  |
| [lock] <a id="quarkus-container-image-docker_quarkus-docker-buildx-progress"></a> [`quarkus.docker.buildx.progress`](#quarkus-container-image-docker_quarkus-docker-buildx-progress) config_property_copy_button:+++quarkus.docker.buildx.progress+++[]<br><br>Set type of progress output (`auto`, `plain`, `tty`). Use `plain` to show container output (default “`auto`”). See <https://docs.docker.com/engine/reference/commandline/buildx_build/++#++progress><br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DOCKER_BUILDX_PROGRESS+++[] | string |  |

<a id="PodmanOptions"></a>
### Podman Options

In addition to the generic container image options, the `container-image-podman` also provides the following options:

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-container-image-podman_quarkus-podman-dockerfile-jvm-path"></a> [`quarkus.podman.dockerfile-jvm-path`](#quarkus-container-image-podman_quarkus-podman-dockerfile-jvm-path) config_property_copy_button:+++quarkus.podman.dockerfile-jvm-path+++[]<br><br>Path to the JVM Dockerfile. If set to an absolute path then the absolute path will be used, otherwise the path will be considered relative to the project root. If not set src/main/docker/Dockerfile.jvm will be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PODMAN_DOCKERFILE_JVM_PATH+++[] | string | `src/main/docker/Dockerfile.jvm` |
| [lock] <a id="quarkus-container-image-podman_quarkus-podman-dockerfile-native-path"></a> [`quarkus.podman.dockerfile-native-path`](#quarkus-container-image-podman_quarkus-podman-dockerfile-native-path) config_property_copy_button:+++quarkus.podman.dockerfile-native-path+++[]<br><br>Path to the native Dockerfile. If set to an absolute path then the absolute path will be used, otherwise the path will be considered relative to the project root. If not set src/main/docker/Dockerfile.native will be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PODMAN_DOCKERFILE_NATIVE_PATH+++[] | string | `src/main/docker/Dockerfile.native` |
| [lock] <a id="quarkus-container-image-podman_quarkus-podman-build-args-arg-name"></a> [`quarkus.podman.build-args."arg-name"`](#quarkus-container-image-podman_quarkus-podman-build-args-arg-name) config_property_copy_button:+++quarkus.podman.build-args."arg-name"+++[]<br><br>Build args passed to docker via `--build-arg`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PODMAN_BUILD_ARGS__ARG_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-container-image-podman_quarkus-podman-cache-from"></a> [`quarkus.podman.cache-from`](#quarkus-container-image-podman_quarkus-podman-cache-from) config_property_copy_button:+++quarkus.podman.cache-from+++[]<br><br>Images to consider as cache sources. Values are passed to `docker build`/`podman build` via the `cache-from` option<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PODMAN_CACHE_FROM+++[] | list of string |  |
| [lock] <a id="quarkus-container-image-podman_quarkus-podman-network"></a> [`quarkus.podman.network`](#quarkus-container-image-podman_quarkus-podman-network) config_property_copy_button:+++quarkus.podman.network+++[]<br><br>The networking mode for the RUN instructions during build<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PODMAN_NETWORK+++[] | string |  |
| [lock] <a id="quarkus-container-image-podman_quarkus-podman-executable-name"></a> [`quarkus.podman.executable-name`](#quarkus-container-image-podman_quarkus-podman-executable-name) config_property_copy_button:+++quarkus.podman.executable-name+++[]<br><br>Name of binary used to execute the docker/podman commands. This setting can override the global container runtime detection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PODMAN_EXECUTABLE_NAME+++[] | string |  |
| [lock] <a id="quarkus-container-image-podman_quarkus-podman-additional-args"></a> [`quarkus.podman.additional-args`](#quarkus-container-image-podman_quarkus-podman-additional-args) config_property_copy_button:+++quarkus.podman.additional-args+++[]<br><br>Additional arbitrary arguments passed to the executable when building the container image.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PODMAN_ADDITIONAL_ARGS+++[] | list of string |  |
| [lock] <a id="quarkus-container-image-podman_quarkus-podman-platform"></a> [`quarkus.podman.platform`](#quarkus-container-image-podman_quarkus-podman-platform) config_property_copy_button:+++quarkus.podman.platform+++[]<br><br>Which platform(s) to target during the build. See <https://docs.podman.io/en/latest/markdown/podman-build.1.html++#++platform-os-arch-variant><br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PODMAN_PLATFORM+++[] | list of string |  |
| [lock] <a id="quarkus-container-image-podman_quarkus-podman-tls-verify"></a> [`quarkus.podman.tls-verify`](#quarkus-container-image-podman_quarkus-podman-tls-verify) config_property_copy_button:+++quarkus.podman.tls-verify+++[]<br><br>Require HTTPS and verify certificates when contacting registries<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PODMAN_TLS_VERIFY+++[] | boolean | `true` |

### OpenShift  Options

In addition to the generic container image options, the `container-image-openshift` also provides the following options:

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-container-image-openshift_quarkus-openshift-build-strategy"></a> [`quarkus.openshift.build-strategy`](#quarkus-container-image-openshift_quarkus-openshift-build-strategy) config_property_copy_button:+++quarkus.openshift.build-strategy+++[]<br><br>The build config strategy to use.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OPENSHIFT_BUILD_STRATEGY+++[] | `binary`, `docker` | `binary` |
| [lock] <a id="quarkus-container-image-openshift_quarkus-openshift-base-jvm-image"></a> [`quarkus.openshift.base-jvm-image`](#quarkus-container-image-openshift_quarkus-openshift-base-jvm-image) config_property_copy_button:+++quarkus.openshift.base-jvm-image+++[]<br><br>The base image to be used when a container image is being produced for the jar build. The value of this property is used to create an ImageStream for the builder image used in the OpenShift build. When it references images already available in the internal OpenShift registry, the corresponding streams are used instead. When the application is built against Java 25 or higher, `registry.access.redhat.com/ubi9/openjdk-25:1.24` is used as the default. Otherwise, when the application is built against Java 21 to Java 24, `registry.access.redhat.com/ubi9/openjdk-21:1.24` is used as the default. Otherwise `registry.access.redhat.com/ubi9/openjdk-17:1.24` is used as the default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OPENSHIFT_BASE_JVM_IMAGE+++[] | string |  |
| [lock] <a id="quarkus-container-image-openshift_quarkus-openshift-base-native-image"></a> [`quarkus.openshift.base-native-image`](#quarkus-container-image-openshift_quarkus-openshift-base-native-image) config_property_copy_button:+++quarkus.openshift.base-native-image+++[]<br><br>The base image to be used when a container image is being produced for the native binary build. The value of this property is used to create an ImageStream for the builder image used in the OpenShift build. When it references images already available in the internal OpenShift registry, the corresponding streams are used instead.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OPENSHIFT_BASE_NATIVE_IMAGE+++[] | string | `quay.io/quarkus/ubi9-quarkus-native-binary-s2i:2.0` |
| [lock] <a id="quarkus-container-image-openshift_quarkus-openshift-jvm-dockerfile"></a> [`quarkus.openshift.jvm-dockerfile`](#quarkus-container-image-openshift_quarkus-openshift-jvm-dockerfile) config_property_copy_button:+++quarkus.openshift.jvm-dockerfile+++[]<br><br>The default Dockerfile to use for jvm builds<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OPENSHIFT_JVM_DOCKERFILE+++[] | string | `src/main/docker/Dockerfile.jvm` |
| [lock] <a id="quarkus-container-image-openshift_quarkus-openshift-native-dockerfile"></a> [`quarkus.openshift.native-dockerfile`](#quarkus-container-image-openshift_quarkus-openshift-native-dockerfile) config_property_copy_button:+++quarkus.openshift.native-dockerfile+++[]<br><br>The default Dockerfile to use for native builds<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OPENSHIFT_NATIVE_DOCKERFILE+++[] | string | `src/main/docker/Dockerfile.native` |
| [lock] <a id="quarkus-container-image-openshift_quarkus-openshift-jvm-arguments"></a> [`quarkus.openshift.jvm-arguments`](#quarkus-container-image-openshift_quarkus-openshift-jvm-arguments) config_property_copy_button:+++quarkus.openshift.jvm-arguments+++[]<br><br>The JVM arguments to pass to the JVM when starting the application<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OPENSHIFT_JVM_ARGUMENTS+++[] | list of string |  |
| [lock] <a id="quarkus-container-image-openshift_quarkus-openshift-native-arguments"></a> [`quarkus.openshift.native-arguments`](#quarkus-container-image-openshift_quarkus-openshift-native-arguments) config_property_copy_button:+++quarkus.openshift.native-arguments+++[]<br><br>Additional arguments to pass when starting the native application<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OPENSHIFT_NATIVE_ARGUMENTS+++[] | list of string |  |
| [lock] <a id="quarkus-container-image-openshift_quarkus-openshift-jar-directory"></a> [`quarkus.openshift.jar-directory`](#quarkus-container-image-openshift_quarkus-openshift-jar-directory) config_property_copy_button:+++quarkus.openshift.jar-directory+++[]<br><br>The directory where the jar is added during the assemble phase. This is dependent on the S2I image and should be supplied if a non default image is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OPENSHIFT_JAR_DIRECTORY+++[] | string |  |
| [lock] <a id="quarkus-container-image-openshift_quarkus-openshift-jar-file-name"></a> [`quarkus.openshift.jar-file-name`](#quarkus-container-image-openshift_quarkus-openshift-jar-file-name) config_property_copy_button:+++quarkus.openshift.jar-file-name+++[]<br><br>The resulting filename of the jar in the S2I image. This option may be used if the selected S2I image uses a fixed name for the jar.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OPENSHIFT_JAR_FILE_NAME+++[] | string |  |
| [lock] <a id="quarkus-container-image-openshift_quarkus-openshift-native-binary-directory"></a> [`quarkus.openshift.native-binary-directory`](#quarkus-container-image-openshift_quarkus-openshift-native-binary-directory) config_property_copy_button:+++quarkus.openshift.native-binary-directory+++[]<br><br>The directory where the native binary is added during the assemble phase. This is dependent on the S2I image and should be supplied if a non-default image is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OPENSHIFT_NATIVE_BINARY_DIRECTORY+++[] | string |  |
| [lock] <a id="quarkus-container-image-openshift_quarkus-openshift-native-binary-file-name"></a> [`quarkus.openshift.native-binary-file-name`](#quarkus-container-image-openshift_quarkus-openshift-native-binary-file-name) config_property_copy_button:+++quarkus.openshift.native-binary-file-name+++[]<br><br>The resulting filename of the native binary in the S2I image. This option may be used if the selected S2I image uses a fixed name for the native binary.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OPENSHIFT_NATIVE_BINARY_FILE_NAME+++[] | string |  |
| [lock] <a id="quarkus-container-image-openshift_quarkus-openshift-build-timeout"></a> [`quarkus.openshift.build-timeout`](#quarkus-container-image-openshift_quarkus-openshift-build-timeout) config_property_copy_button:+++quarkus.openshift.build-timeout+++[]<br><br>The build timeout.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OPENSHIFT_BUILD_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-container-image-openshift_quarkus-openshift) | `5M` |
| [lock] <a id="quarkus-container-image-openshift_quarkus-openshift-build-log-level"></a> [`quarkus.openshift.build-log-level`](#quarkus-container-image-openshift_quarkus-openshift-build-log-level) config_property_copy_button:+++quarkus.openshift.build-log-level+++[]<br><br>The log level of OpenShift build log.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OPENSHIFT_BUILD_LOG_LEVEL+++[] | `fatal`, `error`, `warn`, `info`, `debug`, `trace` | `info` |
| [lock] <a id="quarkus-container-image-openshift_quarkus-openshift-image-push-secret"></a> [`quarkus.openshift.image-push-secret`](#quarkus-container-image-openshift_quarkus-openshift-image-push-secret) config_property_copy_button:+++quarkus.openshift.image-push-secret+++[]<br><br>The image push secret to use for pushing to external registries. (see: <https://cloud.redhat.com/blog/pushing-application-images-to-an-external-registry>)<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OPENSHIFT_IMAGE_PUSH_SECRET+++[] | string |  |

<a id="duration-note-anchor-quarkus-container-image-openshift_quarkus-openshift"></a>

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

<a id="BuildpackOptions"></a>
### Buildpack Options

In addition to the generic container image options, the `container-image-buildpack` also provides the following options:

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-container-image-buildpack_quarkus-buildpack-jvm-builder-image"></a> [`quarkus.buildpack.jvm-builder-image`](#quarkus-container-image-buildpack_quarkus-buildpack-jvm-builder-image) config_property_copy_button:+++quarkus.buildpack.jvm-builder-image+++[]<br><br>The buildpacks builder image to use when building the project in jvm mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_BUILDPACK_JVM_BUILDER_IMAGE+++[] | string | `docker.io/paketobuildpacks/ubi-9-builder` |
| [lock] <a id="quarkus-container-image-buildpack_quarkus-buildpack-native-builder-image"></a> [`quarkus.buildpack.native-builder-image`](#quarkus-container-image-buildpack_quarkus-buildpack-native-builder-image) config_property_copy_button:+++quarkus.buildpack.native-builder-image+++[]<br><br>The buildpacks builder image to use when building the project in native mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_BUILDPACK_NATIVE_BUILDER_IMAGE+++[] | string |  |
| [lock] <a id="quarkus-container-image-buildpack_quarkus-buildpack-lifecycle-image"></a> [`quarkus.buildpack.lifecycle-image`](#quarkus-container-image-buildpack_quarkus-buildpack-lifecycle-image) config_property_copy_button:+++quarkus.buildpack.lifecycle-image+++[]<br><br>The lifecycle image to use when building the project This is optional, but can be used to override the lifecycle present within a builder image.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_BUILDPACK_LIFECYCLE_IMAGE+++[] | string |  |
| [lock] <a id="quarkus-container-image-buildpack_quarkus-buildpack-platform-level"></a> [`quarkus.buildpack.platform-level`](#quarkus-container-image-buildpack_quarkus-buildpack-platform-level) config_property_copy_button:+++quarkus.buildpack.platform-level+++[]<br><br>The platform level to force for the build. Normally the platform level is determined from the intersection of the builder image supported levels, and the platform implementation supported levels. Sometimes it can be beneficial to force the platform to a particular version to force behavior during the build.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_BUILDPACK_PLATFORM_LEVEL+++[] | string |  |
| [lock] <a id="quarkus-container-image-buildpack_quarkus-buildpack-trust-builder-image"></a> [`quarkus.buildpack.trust-builder-image`](#quarkus-container-image-buildpack_quarkus-buildpack-trust-builder-image) config_property_copy_button:+++quarkus.buildpack.trust-builder-image+++[]<br><br>Should the builder image be 'trusted' ? Trusted builders are allowed to attempt to use the `creator` lifecycle, which runs all the build phases within a single container. This is only possible for builders that do not use extensions. Running all phases in one container exposes some phases to information they may not see normally with a container-per-phase.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_BUILDPACK_TRUST_BUILDER_IMAGE+++[] | boolean |  |
| [lock] <a id="quarkus-container-image-buildpack_quarkus-buildpack-builder-env-environment-variable-name"></a> [`quarkus.buildpack.builder-env."environment-variable-name"`](#quarkus-container-image-buildpack_quarkus-buildpack-builder-env-environment-variable-name) config_property_copy_button:+++quarkus.buildpack.builder-env."environment-variable-name"+++[]<br><br>Environment key/values to pass to buildpacks.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_BUILDPACK_BUILDER_ENV__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-container-image-buildpack_quarkus-buildpack-registry-user-registry-host"></a> [`quarkus.buildpack.registry-user."registry-host"`](#quarkus-container-image-buildpack_quarkus-buildpack-registry-user-registry-host) config_property_copy_button:+++quarkus.buildpack.registry-user."registry-host"+++[]<br><br>Usernames to use with registry hosts<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_BUILDPACK_REGISTRY_USER__REGISTRY_HOST_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-container-image-buildpack_quarkus-buildpack-registry-password-registry-host"></a> [`quarkus.buildpack.registry-password."registry-host"`](#quarkus-container-image-buildpack_quarkus-buildpack-registry-password-registry-host) config_property_copy_button:+++quarkus.buildpack.registry-password."registry-host"+++[]<br><br>Passwords to use with registry hosts<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_BUILDPACK_REGISTRY_PASSWORD__REGISTRY_HOST_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-container-image-buildpack_quarkus-buildpack-registry-token-registry-host"></a> [`quarkus.buildpack.registry-token."registry-host"`](#quarkus-container-image-buildpack_quarkus-buildpack-registry-token-registry-host) config_property_copy_button:+++quarkus.buildpack.registry-token."registry-host"+++[]<br><br>Tokens to use with registry hosts<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_BUILDPACK_REGISTRY_TOKEN__REGISTRY_HOST_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-container-image-buildpack_quarkus-buildpack-run-image"></a> [`quarkus.buildpack.run-image`](#quarkus-container-image-buildpack_quarkus-buildpack-run-image) config_property_copy_button:+++quarkus.buildpack.run-image+++[]<br><br>The buildpacks run image to use when building the project When not supplied, the run image is determined by the builder image. If extensions are used by the builder image, they may override the run image.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_BUILDPACK_RUN_IMAGE+++[] | string |  |
| [lock] <a id="quarkus-container-image-buildpack_quarkus-buildpack-pull-timeout-seconds"></a> [`quarkus.buildpack.pull-timeout-seconds`](#quarkus-container-image-buildpack_quarkus-buildpack-pull-timeout-seconds) config_property_copy_button:+++quarkus.buildpack.pull-timeout-seconds+++[]<br><br>Initial pull timeout for builder/run images, in seconds<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_BUILDPACK_PULL_TIMEOUT_SECONDS+++[] | int | `300` |
| [lock] <a id="quarkus-container-image-buildpack_quarkus-buildpack-pull-timeout-increase-seconds"></a> [`quarkus.buildpack.pull-timeout-increase-seconds`](#quarkus-container-image-buildpack_quarkus-buildpack-pull-timeout-increase-seconds) config_property_copy_button:+++quarkus.buildpack.pull-timeout-increase-seconds+++[]<br><br>Increase pull timeout for builder/run images after failure, in seconds<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_BUILDPACK_PULL_TIMEOUT_INCREASE_SECONDS+++[] | int | `15` |
| [lock] <a id="quarkus-container-image-buildpack_quarkus-buildpack-pull-retry-count"></a> [`quarkus.buildpack.pull-retry-count`](#quarkus-container-image-buildpack_quarkus-buildpack-pull-retry-count) config_property_copy_button:+++quarkus.buildpack.pull-retry-count+++[]<br><br>How many times to retry an image pull after a failure<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_BUILDPACK_PULL_RETRY_COUNT+++[] | int | `3` |
| [lock] <a id="quarkus-container-image-buildpack_quarkus-buildpack-docker-host"></a> [`quarkus.buildpack.docker-host`](#quarkus-container-image-buildpack_quarkus-buildpack-docker-host) config_property_copy_button:+++quarkus.buildpack.docker-host+++[]<br><br>DOCKER_HOST value to use. This value is normally auto-determined, and is available for override if needed. If not set, the env var DOCKER_HOST is used, if that is not set the platform will test if `podman` is available on the path, if so, it will use podman to configure the appropriate values. If `podman` is not on the path, docker is assumed, and per-platform defaults for docker are used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_BUILDPACK_DOCKER_HOST+++[] | string |  |
| [lock] <a id="quarkus-container-image-buildpack_quarkus-buildpack-docker-socket"></a> [`quarkus.buildpack.docker-socket`](#quarkus-container-image-buildpack_quarkus-buildpack-docker-socket) config_property_copy_button:+++quarkus.buildpack.docker-socket+++[]<br><br>Path to the Docker socket to use. This value is normally auto-determined, and is available for override if needed. The path to the socket can vary, especially when the docker/podman daemon is running inside a VM, if useDaemon mode is true, then this path must refer to the path that can be used to mount the socket inside a container, so may refer to the path to the socket in the VM rather than the host.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_BUILDPACK_DOCKER_SOCKET+++[] | string |  |
| [lock] <a id="quarkus-container-image-buildpack_quarkus-buildpack-use-daemon"></a> [`quarkus.buildpack.use-daemon`](#quarkus-container-image-buildpack_quarkus-buildpack-use-daemon) config_property_copy_button:+++quarkus.buildpack.use-daemon+++[]<br><br>use Daemon mode? Should the buildpack build have the docker socket mounted into the build container(s). If this is false, then the image will be built directly as layers in a remote registry, this will probably require registry credentials to be passed. Defaults to 'true'<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_BUILDPACK_USE_DAEMON+++[] | boolean | `true` |
| [lock] <a id="quarkus-container-image-buildpack_quarkus-buildpack-docker-network"></a> [`quarkus.buildpack.docker-network`](#quarkus-container-image-buildpack_quarkus-buildpack-docker-network) config_property_copy_button:+++quarkus.buildpack.docker-network+++[]<br><br>Use specified docker network during build This can be handy when building against a locally hosted docker registry, where you will require the build containers to be part of the 'host' network to enable them to access the local registry.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_BUILDPACK_DOCKER_NETWORK+++[] | string |  |
| [lock] <a id="quarkus-container-image-buildpack_quarkus-buildpack-log-level"></a> [`quarkus.buildpack.log-level`](#quarkus-container-image-buildpack_quarkus-buildpack-log-level) config_property_copy_button:+++quarkus.buildpack.log-level+++[]<br><br>Log level to use. The log level to use when executing the build phases in containers. Defaults to 'info', supported values are 'info', 'warn' and 'debug'<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_BUILDPACK_LOG_LEVEL+++[] | string | `info` |
| [lock] <a id="quarkus-container-image-buildpack_quarkus-buildpack-get-use-timestamps"></a> [`quarkus.buildpack.get-use-timestamps`](#quarkus-container-image-buildpack_quarkus-buildpack-get-use-timestamps) config_property_copy_button:+++quarkus.buildpack.get-use-timestamps+++[]<br><br>Should the container log information include timestamps?<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_BUILDPACK_GET_USE_TIMESTAMPS+++[] | boolean | `true` |
