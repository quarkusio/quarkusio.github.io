# Using S2I to deploy Quarkus applications to OpenShift

You can deploy your Quarkus applications to OpenShift by using the Source-to-Image (S2I) method. With S2I, you must provide the source code to the build container through a Git repository or by uploading the source code at build time.

You can deploy Quarkus applications to OpenShift with Java 17, 21, or 25 versions.

## Prerequisites

- You have a Quarkus application built with Java 17, 21, or 25.
- Optional: You have a Quarkus project that includes the `quarkus-openshift` extension.
- You are working in the correct OpenShift project namespace.
- Your project is hosted in a Git repository.

## Procedure

1. Open the `pom.xml` file, and set the Java version, where `${java.version}` is 17, 21, or 25.

   ```xml
   <maven.compiler.release>${java.version}</maven.compiler.release>
   ```


1. Package your application, by entering the following command:

   ```shell
   ./mvnw clean package
   ```

2. Create a directory called `.s2i` at the same level as the `pom.xml` file.
3. Create a file called `environment` in the `.s2i` directory and add the following content:

   ```text
   MAVEN_S2I_ARTIFACT_DIRS=target/quarkus-app
   S2I_SOURCE_DEPLOYMENTS_FILTER=app lib quarkus quarkus-run.jar
   JAVA_OPTIONS=-Dquarkus.http.host=0.0.0.0
   AB_JOLOKIA_OFF=true
   JAVA_APP_JAR=/deployments/quarkus-run.jar
   ```

4. Commit and push your changes to the remote Git repository.

1. Import the supported OpenShift image.
   - Java 17:

   ```shell
   oc import-image ubi9/openjdk-17 --from=registry.access.redhat.com/ubi9/openjdk-17 --confirm
   ```

   - Java 21:

   ```shell
   oc import-image ubi9/openjdk-21 --from=registry.access.redhat.com/ubi9/openjdk-21 --confirm
   ```

   - Java 25:

   ```shell
   oc import-image ubi9/openjdk-25 --from=registry.access.redhat.com/ubi9/openjdk-25 --confirm
   ```

   > [!NOTE]
   > - If you are using the OpenShift image registry and pulling from image streams in the same project, your pod service account must already have the correct permissions.
   >
   > - If you are pulling images across other OpenShift projects or from secured registries, additional configuration steps might be required.
   >
   > For more information, see the [Red Hat OpenShift Container Platform](https://docs.openshift.com/container-platform/) documentation.
   >
   > - If you are deploying on IBM Z infrastructure, enter `oc import-image ubi9/openjdk-21 --from=registry.redhat.io/ubi9/openjdk-21 --confirm` instead. For information about this image, see [OpenJDK 21 runtime image on UBI9](https://catalog.redhat.com/en/software/containers/ubi9/openjdk-21-runtime/6501ce769a0d86945c422d5f).


1. Build the project, create the application, and deploy the OpenShift service.
   - Java 17:

   ```shell
   oc new-app registry.access.redhat.com/ubi9/openjdk-17~<git_path> --name=<project_name>
   ```

   - Java 21:

   ```shell
   oc new-app registry.access.redhat.com/ubi9/openjdk-21~<git_path> --name=<project_name>
   ```

   - Java 25:

   ```shell
   oc new-app registry.access.redhat.com/ubi9/openjdk-25~<git_path> --name=<project_name>
   ```

   1. Replace `<git_path>` with the path of the Git repository that hosts your Quarkus project.

      For example, for Java 21:

      ```shell
      oc new-app registry.access.redhat.com/ubi9/openjdk-21~https://github.com/johndoe/code-with-quarkus.git --name=code-with-quarkus
      ```

      {nbsp}

      If you do not have SSH keys configured for the Git repository, when specifying the Git path, use the HTTPS URL instead of the SSH URL.

.. Replace `<project_name>` with the name of your application. +

> [!NOTE]
> If you are deploying on IBM Z infrastructure, enter `oc new-app ubi9/openjdk-21~<git_path> --name=<project_name>` instead.

1. To deploy an updated version of the project, push changes to the Git repository, and then run:

   ```shell
   oc start-build <project_name>
   ```

2. To expose a route to the application, run the following command:

   ```shell
   oc expose svc <project_name>
   ```


## Verification

1. List the pods associated with your current OpenShift project:

   ```shell
   oc get pods
   ```

2. To get the log output for your application's pod, run the following command, replacing `<pod_name>` with the name of the latest pod prefixed by your application name:

   ```shell
   oc logs -f <pod_name>
   ```


## References

- [Deploying Quarkus applications to OpenShift](/guides/deploying-to-openshift/index.md)
