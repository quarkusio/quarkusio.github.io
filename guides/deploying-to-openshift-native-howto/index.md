# Deploying Quarkus applications compiled to native executables

You can deploy your Quarkus applications compiled to native executables to OpenShift by using the Docker build strategy.

You must create a native executable for your application that targets a supported operating system and matches the architecture. This means, if you are building on Windows, you create a native Linux executable by using a container runtime, for example, Docker or Podman.

Your Quarkus project includes pregenerated Dockerfiles with instructions. If you want to use a custom Dockerfile, add the file to the `src/main/docker` directory or any location inside the module.

Additionally, if you want to have multiple Dockerfiles and switch between them, set the path to your preferred Dockerfile by using the `quarkus.openshift.native-dockerfile` property.

> [!NOTE]
> This guide describes this strategy by using a Quarkus project with Maven as the example project.

## Prerequisites

- A supported operating system or an Open Container Initiative (OCI) compatible container runtime, such as Podman or Docker.
- You have a Quarkus Maven project that includes the `quarkus-openshift` extension.
- You have access to an OpenShift cluster and the latest compatible version of the `oc` CLI tool installed.
- You are working in the correct OpenShift project namespace.

## Procedure

1. Set the Docker build strategy in your `application.properties` configuration file:

   ```properties
   quarkus.openshift.build-strategy=docker
   ```

2. Enable container-based native builds:

   ```properties
   quarkus.native.container-build=true
   ```

3. Optional: Set the following properties in the `application.properties` file based on your environment:

   \*\* If you are using an untrusted certificate, enable certificate trust for the `KubernetesClient`:

   ```properties
   quarkus.kubernetes-client.trust-certs=true
   ```

   \*\* To expose the service and create an OpenShift route, set the following property:

   ```properties
   quarkus.openshift.route.expose=true
   ```

   \*\* To use a custom Dockerfile instead of the pregenerated Dockerfiles, set the path to your custom Dockerfile:

   ```properties
   quarkus.openshift.native-dockerfile=<path_to_your_dockerfile>
   ```

   For example, to specify a custom Dockerfile named `Dockerfile.custom-native`:

   ```properties
   quarkus.openshift.native-dockerfile=src/main/docker/Dockerfile.custom-native
   ```


\*\* Specify the container engine: \*\*\* To build a native executable with Podman: +

```properties
quarkus.native.container-runtime=podman
```

\*\*\* To build a native executable with Docker: +

```properties
quarkus.native.container-runtime=docker
```

1. Finally, build the native executable, package, and deploy your application to OpenShift:

   ```shell
   ./mvnw clean package -Pnative -Dquarkus.openshift.deploy=true
   ```


## Verification

1. Verify that an image stream and a service resource are created, and that the application is deployed. Use the OpenShift web console or the following OpenShift command-line interface (CLI) commands:

   ```shell
   oc get is <1>
   oc get pods <2>
   oc get svc <3>
   ```

   1. List the image streams created.
   2. List the pods associated with your current OpenShift project.
   3. List the Kubernetes services.


2. To get the log output for your application's pod, run the following command where `__<pod_name>__` is the name of the latest pod prefixed with the name of your application:

   ```shell
   oc logs -f __<pod_name>__
   ```


## References

- [Deploying Quarkus applications to OpenShift](/guides/deploying-to-openshift/index.md)
