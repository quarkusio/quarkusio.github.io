# Deploying Quarkus applications to OpenShift in a single step

As an application developer, you build and deploy your Quarkus applications to OpenShift in a single step. Complete one of the following commands:

**CLI**

```bash
quarkus build
quarkus deploy openshift
```

**Maven**

```bash
./mvnw install -Dquarkus.openshift.deploy=true
```

**Gradle**

```bash
./gradlew build -Dquarkus.openshift.deploy=true
```

With these commands, you build your application locally, trigger a container image build, and apply the generated OpenShift resources automatically.

The generated resources use a Kubernetes `Deployment`, but still make use of OpenShift-specific resources, such as `Route`, `BuildConfig`, and so on.

## Prerequisites

- You have OpenJDK 21 or later installed.
- You have set the `JAVA_HOME` environment variable to the location of the Java SDK.
- You have access to an OpenShift cluster and the latest compatible version of the `oc` CLI tool installed.
- You are working in the correct OpenShift project namespace.

> [!NOTE]
> To run this deployment, it is not necessary to have the `quarkus-openshift` extension included in your Quarkus project.

## Procedure

1. Trigger a build and deployment in a single step:

**CLI**

```bash
quarkus build
quarkus deploy openshift
```

**Maven**

```bash
./mvnw install -Dquarkus.openshift.deploy=true
```

**Gradle**

```bash
./gradlew build -Dquarkus.openshift.deploy=true
```

> [!TIP]
> If you want to test your application immediately, set the `quarkus.openshift.route.expose` configuration property to `true` to expose the service automatically.
>
> For example, add `-Dquarkus.openshift.route.expose=true` to the above command. For more information, see [Exposing routes](/guides/deploying-to-openshift/index.md#exposing_routes).

> [!NOTE]
> As of OpenShift 4.14, the `DeploymentConfig` object is deprecated. `Deployment` is now the default and preferred deployment kind for the `quarkus-openshift` extension.
>
> Because of this change, consider the following:
>
> - If you redeploy applications that you previously deployed by using `DeploymentConfig`, by default, those applications use `Deployment` but do not remove the previous `DeploymentConfig`. This leads to a deployment of both new and old applications, so, you must remove the old `DeploymentConfig` manually. If you want to continue to use `DeploymentConfig`, it is still possible to do so by explicitly setting `quarkus.openshift.deployment-kind` to `DeploymentConfig`.
> - `Deployment` is a Kubernetes resource and not OpenShift specific, so it cannot leverage `ImageStream` resources, as is the case with `DeploymentConfig`. Therefore, the image references must include the container image registry that hosts the image.
>
> For more information about the deprecation, how to set up and use automatic rollbacks, triggers, lifecycle hooks, and custom strategies, see the Red Hat Knowledgebase article [DeploymentConfig API is being deprecated in Red Hat OpenShift Container Platform 4.14](https://access.redhat.com/articles/7041372).

## Verification

1. Verify that an image stream and a service resource are created and the application is deployed by using the OpenShift web console.

   ```properties
   quarkus.container-image.group=<project/namespace name>
   ```

   Alternatively, run the following OpenShift command-line interface (CLI) commands:

   ```bash
   oc get is <1>
   oc get pods <2>
   oc get svc <3>
   ```

   1. List the image streams created.
   2. List the pods associated with your current OpenShift project.
   3. List the Kubernetes services.


1. To get the log output for your application's pod, enter the following command:

   ```shell
   oc logs -f <pod_name>
   ```

   By default, the service is not exposed to the outside world. Therefore, if you did not expose the created service automatically by setting the `quarkus.openshift.route.expose=true` property before building the application, you can expose the service manually.

   ```bash
   oc expose svc/openshift-quickstart <1>
   oc get routes <2>
   curl http://<route>/hello <3>
   ```

   1. Expose the service.
   2. Get the list of exposed routes.
   3. Access your application.


## References

- [Deploying Quarkus applications to OpenShift](/guides/deploying-to-openshift/index.md)
