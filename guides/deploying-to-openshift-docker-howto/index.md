# Deploying Quarkus Java applications to OpenShift by using a Docker build strategy

As an application developer, you can deploy your applications to OpenShift by using the Docker build strategy as a deployment option.

This strategy builds the artifacts outside the OpenShift cluster, either locally or in a CI environment, and provides them to the OpenShift build system together with a Dockerfile. The artifacts include JAR files or a native executable. The OpenShift cluster builds the container image and provides it as an image stream.

This functionality is provided by the `quarkus-openshift` extension. If you want to use a custom Dockerfile, add the file to the `src/main/docker` directory or another location inside the module. Set the path to your Dockerfile with the `quarkus.openshift.jvm-dockerfile` property.

## Prerequisites

- You have OpenJDK 17, 21, or 25 installed.
- You have set the `JAVA_HOME` environment variable to the location of the Java SDK.
- You have Apache Maven 3.9.16 installed.
- You have a Quarkus project that includes the `quarkus-openshift` extension.
- You have access to a OpenShift cluster and the latest compatible version of the `oc` CLI tool installed.
- You are working in the correct OpenShift project namespace.

## Procedure

1. Set the Docker build strategy in your `application.properties` file:

   ```properties
   quarkus.openshift.build-strategy=docker
   ```

2. Optional: Complete one or more of the following configuration steps in the `application.properties` file:
   - If you are using an untrusted certificate, enable certificate trust for the `KubernetesClient`:

   ```properties
   quarkus.kubernetes-client.trust-certs=true
   ```

   - To expose the service and create an OpenShift route, set the following property:

   ```properties
   quarkus.openshift.route.expose=true
   ```

   - To use a custom Dockerfile instead of the pre-generated Dockerfiles, set the path to your Dockerfile:

   ```properties
   quarkus.openshift.jvm-dockerfile=<path_to_your_dockerfile>
   ```

   For example, to specify a custom Dockerfile named `Dockerfile.custom-jvm`, set the following property:

   ```properties
   quarkus.openshift.jvm-dockerfile=src/main/resources/Dockerfile.custom-jvm
   ```

3. Package and deploy your application to the current OpenShift project:

   ```shell
   ./mvnw clean package -Dquarkus.openshift.deploy=true
   ```


## Verification

The following verification steps use the `openshift-helloworld` example application.

1. Display the list of pods associated with your current OpenShift project:

   ```shell
   oc get pods
   ```

   ```text
   NAME                            READY   STATUS      RESTARTS   AGE
   openshift-helloworld-1-build    0/1     Completed   0          11m
   openshift-helloworld-1-deploy   0/1     Completed   0          10m
   openshift-helloworld-1-gzzrx    1/1     Running     0          10m
   ```

2. To get the log output from your application's pod, run the `oc logs -f` command with the pod name. The following example uses the `openshift-helloworld-1-gzzrx` pod name, which corresponds to the latest pod prefixed with the application name:

   ```shell
   oc logs -f openshift-helloworld-1-gzzrx
   ```

   ```text
   Starting the Java application using /opt/jboss/container/java/run/run-java.sh ...
   INFO exec -a "java" java -Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager -XX:MaxRAMPercentage=50.0 -XX:+UseParallelGC -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:+ExitOnOutOfMemoryError -cp "." -jar /deployments/quarkus-run.jar
   __  ____  __  _____   ___  __ ____  ______
   --/ __ \/ / / / _ | / _ \/ //_/ / / / __/
   -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \
   --\___\_\____/_/ |_/_/|_/_/|_|\____/___/
   2024-09-17 10:23:25,254 INFO  [io.quarkus] (main) getting-started 1.0.0-SNAPSHOT on JVM (powered by Quarkus {QuarkusCore}) started in 0.653s. Listening on: http://0.0.0.0:8080
   2024-09-17 10:23:25,281 INFO  [io.quarkus] (main) Profile prod activated.
   2024-09-17 10:23:25,281 INFO  [io.quarkus] (main) Installed features: [cdi, kubernetes, rest, smallrye-context-propagation, vertx]
   ```

3. Display the list of services:

   ```shell
   oc get svc
   ```

   ```text
   NAME                   TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)                               AGE
   openshift-helloworld   ClusterIP   172.30.64.57     <none>        80/TCP                                14m
   ```

4. Get a URL to test your application. To do so, ensure that you exposed an OpenShift route by setting the `quarkus.openshift.route.expose=true` property in the `application.properties` file before building the application:

   ```shell
   oc get routes
   ```

   ```text
   NAME                   HOST/PORT                                                                   PATH   SERVICES               PORT   TERMINATION   WILDCARD
   openshift-helloworld   openshift-helloworld-username-dev.apps.sandbox-m2.ll9k.p1.openshiftapps.com          openshift-helloworld   http                 None
   ```

   > [!NOTE]
   > Be aware that the route listens on port 80 and no longer listens on port 8080.

   You can test the application in this example with a web browser or from a terminal. Use the complete URL from `oc get routes`, for example `http://openshift-helloworld-username-dev.apps.sandbox-m2.ll9k.p1.openshiftapps.com`.

   For example:

   ```shell
   curl http://openshift-helloworld-username-dev.apps.sandbox-m2.ll9k.p1.openshiftapps.com
   ```


## References

- [Deploying Quarkus applications to OpenShift](/guides/deploying-to-openshift/index.md)
