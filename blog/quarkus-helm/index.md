# Generate Helm charts manifests with Quarkus Helm

[Helm](https://helm.sh/) is the most popular package manager that finds, shares, and deploys software built for Kubernetes. To configure Java applications for distribution through Helm, you need to package them as Helm charts containing all the necessary metadata and resource definitions and this can be a very tedious task. The good news is that Quarkus has recently released the new [Quarkus Helm](https://github.com/quarkiverse/quarkus-helm) extension that will automatically generate all these Helm chart manifests and resources!

## Introduction to Helm charts

First of all, let's introduce how a Helm chart looks like. A Helm chart is a collection of files inside a directory. The following files are mandatory:

- `Chart.yaml`: Chart metadata, such as name, version, and developers.
- `values.yaml`: Default configuration values for the chart in YAML.
- `templates`: A directory containing the list of resources that, in combination with `values.yaml`, generate the application when the chart is installed. The resources are also specified in YAML.

Find more information about Helm charts in the [official Helm documentation](https://helm.sh/docs/topics/charts/).

## Getting started with the Quarkus Helm extension

Let's now see how we can generate the Helm chart of a Quarkus application.

In this example, we’ll create a Quarkus application with the Quarkus Helm and the Quarkus Kubernetes extensions by running the following command:

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:2.12.0.Final:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=helm-quickstart \
    -DclassName="org.acme.quickstart.GreetingResource" \
    -Dpath="/hello" \
    -Dextensions="resteasy-reactive,kubernetes,helm"
cd helm-quickstart
```

If you already have your Quarkus project configured, you can add the Helm extension to your project by running the following command in your project base directory:

```bash
./mvnw quarkus:add-extension -Dextensions="helm"
```

This command will add the following dependency to your `pom.xml` file:

```xml
<dependency>
    <groupId>io.quarkiverse.helm</groupId>
    <artifactId>quarkus-helm</artifactId>
</dependency>
```

And now you can generate the Helm resources using the Maven build command:

```bash
./mvnw clean package
```

Depending on the Quarkus Kubernetes extensions that you are using in your project, the Helm resources will include some resources or others. Since we have used the Quarkus Kubernetes extension, the Helm chart resources will be generated in the `target/helm/kubernetes/<chart name>/` directory.

> [!TIP]
> The `<chart name>` is set from either the property `quarkus.helm.name` or the Quarkus application name.

Moreover, the Helm resources include the following files in the `target/helm/kubernetes/<chart name>/templates` directory:

- `deployment.yaml`
- `ingress.yaml`
- `service.yaml`

Instead, if we were using the Quarkus OpenShift extension, the Helm chart sources would be generated in the `target/helm/openshift/<chart name>/` directory and it would include the files `buildconfig.yaml`, `deploymentconfig.yaml`, `service.yaml` and `imagestream.yaml`.

## Helm use

Let's see next how to use the previously generated Helm chart.

First, make sure you have installed the [Helm command-line interface](https://helm.sh/docs/intro/install/) (CLI) and logged into a Kubernetes cluster.

Then run the following Maven command to generate the Helm artifacts:

```bash
./mvnw clean package
```

And install the generated Helm chart into the cluster:

```bash
helm install helm-example ./target/helm/kubernetes/<chart name>
```

The `helm` command waits until the chart is fully installed and the application is up and running.

You can update your deployment in a couple of ways. The first is via the `upgrade` command of the Helm CLI. After having made changes to your project and regenerated the resources, you can upgrade your deployment using the following command:

```bash
helm upgrade helm-example ./target/helm/kubernetes/<chart name>
```

The second way to update a deployment is through the `--set` option of the helm upgrade command:

```bash
helm upgrade helm-example ./target/helm/kubernetes/<chart name> --set app.replicas=1
```

The `app.replicas` option is a parameterized property that is mapped by the `values.yaml` file. We'll explore this function more in the next section.

Finally, to delete a deployment, enter:

```bash
helm uninstall helm-example
```

## Mapping values

Helm allows *mapping* to set properties of your resources when installing your chart or after your chart has been installed. For example, suppose your deployment template in the `templates/deployment.yaml` file looks like this:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: example
spec:
  replicas: {{ .Values.app.replicas }}
```

You can set the number of replicas to use when installing the chart using the `helm` command, as shown in the previous section:

```bash
helm upgrade helm-example ./target/helm/kubernetes/<chart name> --set app.replicas=1
```

Alternatively, you can set the number of replicas to use in the generated `values.yaml` file, located in the chart's directory at `target/helm/kubernetes/<chart name>/values.yaml`:

```yaml
app:
  replicas: 1
```

So, do I need to manually update the `values.yaml` and the template resources to map these properties? No! The Quarkus Helm extension will automatically map some preconfigured properties, such as the Kubernetes replicas or the ingress host. The complete list of preconfigured properties is in the [official Quarkus Helm site](https://quarkiverse.github.io/quarkiverse-docs/quarkus-helm/dev/index.html#_mapping_values) documentation.

Let's try out an example of configuring the replicas using the Quarkus Kubernetes `quarkus.kubernetes.replicas` property. We'll see how the Quarkus Helm extension automatically maps it into the generated Helm chart.

First, set 3 replicas in your `application.properties` file:

```properties
quarkus.kubernetes.replicas=3
```

Now, the generated Helm values file at `target/helm/kubernetes/<chart name>/values.yaml` will contain the replicas value:

```yaml
app:
  replicas: 3
```

Also, the deployment template file at `target/helm/kubernetes/<chart name>/templates/deployment.yaml` will have a reference to this value.

But what if we want to map other properties that are not preconfigured? For example, given the following deployment template file, I want to map the value `example` that appears in the `metadata.name` property:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: example ## let's map this value!
# ...
```

You can configure the Quarkus Helm extension to map any properties that are present in your resources using [YAMLPath expressions](https://github.com/yaml-path/YamlPath). To continue with the preceding example, you only need to add the following configuration to your properties:

```properties
quarkus.helm.values[0].property=resource <1>
quarkus.helm.values[0].paths=metadata.name <2>
```

1. `property` is the property name to be set in the generated `values.yaml`
2. `paths` is a list of YAMLPath expressions that identify the properties you want to use

Using the properties just defined, the resulting Helm values file at `target/helm/kubernetes/<chart name>/values.yaml` will now include:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .Values.app.resource }} # `resource` comes from the property `quarkus.helm.values[0].property`
# ...
```

YAMLPath expressions also support more complex scenarios. You can use the expressions to replace properties for a certain resource type, or properties described as a key-value map. You can find all the supported features in [here](https://quarkiverse.github.io/quarkiverse-docs/quarkus-helm/dev/index.html#_mapping_user_properties_using_yamlpath_expressions).

## Helm profiles

By default, all properties are mapped to the same Helm `values.yaml` file. However, the Quarkus Helm extension also supports the generation of other Helm values files.

For example, let's say you have two environments: one for testing and another for production. Each environment has a different ingress host that exposes your Kubernetes application. You can configure your application as follows:

```properties
# Mapped to `values.yaml` by the preconfigured Ingress decorator
quarkus.kubernetes.ingress.host=my-host

# Overwrite the value of `quarkus.kubernetes.ingress.host` to `values-<profile-name>.yaml`:
quarkus.helm.values[0].property=host
quarkus.helm.values[0].paths=(kind == Ingress).spec.rules.host
quarkus.helm.values[0].value=my-test-host
## `test` is the profile name.
quarkus.helm.values[0].profile=test
```

The ingress host property is preconfigured by the Quarkus Helm extension. So you will find the `my-host` value in the `target/helm/kubernetes/<chart name>/values.yaml` file:

```yaml
app:
  host: my-host
```

But because you are also using a profile named `test` in one of your properties, the Quarkus Helm extension also generates a `target/helm/kubernetes/<chart name>/values-test.yaml` file with the content:

```yaml
app:
  host: my-test-host
```

When installing your chart in the test environment, you can make use of this values file as shown in the following command:

```bash
helm install -f ./target/helm/kubernetes/<chart name>/values-test.yaml helm-example ./target/helm/kubernetes/<chart name>
```

## Summary

In this article, we learned how to easily generate Helm charts using the Quarkus Helm extension, how to map properties to be set when installing or updating your charts, and how to use Helm profiles.
