# Connecting to an Elasticsearch cluster

Elasticsearch is a well known full text search engine and NoSQL datastore.

In this guide, we will see how you can get your REST services to interact with an Elasticsearch cluster.

Quarkus provides two ways of accessing Elasticsearch:

- The lower level REST Client
- The Elasticsearch Java client

> [!WARNING]
> A third Quarkus extension for the "high level REST Client" used to exist, but was removed as this client has been deprecated by Elastic and has some licensing issues.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

- Elasticsearch installed or Docker installed

## Architecture

The application built in this guide is quite simple: the user can add elements in a list using a form and the list is updated.

All the information between the browser and the server is formatted as JSON.

The elements are stored in Elasticsearch.

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:elasticsearch-quickstart \
    --extension='rest-jackson,elasticsearch-rest-client' \
    --no-code
cd elasticsearch-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=elasticsearch-quickstart \
    -Dextensions='rest-jackson,elasticsearch-rest-client' \
    -DnoCode
cd elasticsearch-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=elasticsearch-quickstart"`

This command generates a Maven structure importing the Quarkus REST (formerly RESTEasy Reactive), Jackson, and Elasticsearch low level REST client extensions.

The Elasticsearch low level REST client comes with the `quarkus-elasticsearch-rest-client` extension that has been added to your build file.

If you want to use the Elasticsearch Java client instead, replace the `quarkus-elasticsearch-rest-client` extension by the `quarkus-elasticsearch-java-client` extension.

> [!NOTE]
> We use the `rest-jackson` extension here and not the JSON-B variant because we will use the Vert.x `JsonObject` helper to serialize/deserialize our objects to/from Elasticsearch and it uses Jackson under the hood.

To add the extensions to an existing project, follow the instructions below.

For the Elasticsearch low level REST client, add the following dependency to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-elasticsearch-rest-client</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-elasticsearch-rest-client")
```

For the Elasticsearch Java client, add the following dependency to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-elasticsearch-java-client</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-elasticsearch-java-client")
```

## Creating your first JSON REST service

In this example, we will create an application to manage a list of fruits.

First, let's create the `Fruit` bean as follows:

```java
package org.acme.elasticsearch;

public class Fruit {
    public String id;
    public String name;
    public String color;
}
```

Nothing fancy. One important thing to note is that having a default constructor is required by the JSON serialization layer.

Now create a `org.acme.elasticsearch.FruitService` that will be the business layer of our application and will store/load the fruits from the Elasticsearch instance. Here we use the low level REST client, if you want to use the Java API client instead, follow the instructions in the [Using the Elasticsearch Java Client](#using-the-elasticsearch-java-client) paragraph instead.

```java
package org.acme.elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.Response;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class FruitService {
    @Inject
    Rest5Client restClient; // <1>

    public void index(Fruit fruit) throws IOException {
        Request request = new Request(
                "PUT",
                "/fruits/_doc/" + fruit.id); // <2>
        request.setJsonEntity(JsonObject.mapFrom(fruit).toString()); // <3>
        restClient.performRequest(request); // <4>
    }

    public void index(List<Fruit> list) throws IOException {

        var entityList = new ArrayList<JsonObject>();

        for (var fruit : list) {

            entityList.add(new JsonObject().put("index", new JsonObject()// <5>
                    .put("_index", "fruits").put("_id", fruit.id)));
            entityList.add(JsonObject.mapFrom(fruit));
        }

        Request request = new Request(
                "POST", "fruits/_bulk?pretty");
        request.setEntity(new StringEntity(
                toNdJsonString(entityList),// <6>
                ContentType.create("application/x-ndjson")));// <7>
        restClient.performRequest(request);
    }

    public void delete(List<String> identityList) throws IOException {

        var entityList = new ArrayList<JsonObject>();

        for (var id : identityList) {
            entityList.add(new JsonObject().put("delete",
                    new JsonObject().put("_index", "fruits").put("_id", id)));// <8>
        }

        Request request = new Request(
                "POST", "fruits/_bulk?pretty");
        request.setEntity(new StringEntity(
                toNdJsonString(entityList),
                ContentType.create("application/x-ndjson")));
        restClient.performRequest(request);
    }

    public Fruit get(String id) throws IOException, ParseException {
        Request request = new Request(
                "GET",
                "/fruits/_doc/" + id);
        Response response = restClient.performRequest(request);
        String responseBody = EntityUtils.toString(response.getEntity());
        JsonObject json = new JsonObject(responseBody); // <9>
        return json.getJsonObject("_source").mapTo(Fruit.class);
    }

    public List<Fruit> searchByColor(String color) throws IOException, ParseException {
        return search("color", color);
    }

    public List<Fruit> searchByName(String name) throws IOException, ParseException {
        return search("name", name);
    }

    private List<Fruit> search(String term, String match) throws IOException, ParseException {
        Request request = new Request(
                "GET",
                "/fruits/_search");
        //construct a JSON query like {"query": {"match": {"<term>": "<match"}}
        JsonObject termJson = new JsonObject().put(term, match);
        JsonObject matchJson = new JsonObject().put("match", termJson);
        JsonObject queryJson = new JsonObject().put("query", matchJson);
        request.setJsonEntity(queryJson.encode());
        Response response = restClient.performRequest(request);
        String responseBody = EntityUtils.toString(response.getEntity());

        JsonObject json = new JsonObject(responseBody);
        JsonArray hits = json.getJsonObject("hits").getJsonArray("hits");
        List<Fruit> results = new ArrayList<>(hits.size());
        for (int i = 0; i < hits.size(); i++) {
            JsonObject hit = hits.getJsonObject(i);
            Fruit fruit = hit.getJsonObject("_source").mapTo(Fruit.class);
            results.add(fruit);
        }
        return results;
    }

    private static String toNdJsonString(List<JsonObject> objects) {
        return objects.stream()
                .map(JsonObject::encode)
                .collect(Collectors.joining("\n", "", "\n"));
    }    
}
```

1. We inject an Elasticsearch low level `Rest5Client` into our service.
2. We create an Elasticsearch request.
3. We use Vert.x `JsonObject` to serialize the object before sending it to Elasticsearch, you can use whatever you want to serialize your objects to JSON.
4. We send the request (indexing request here) to Elasticsearch.
5. As we `index` collection of objects we should use `index`, `create` or `update` action.
6. We use `toNdJsonString(entityList)` call to produce output like below

   ```json
   {"index", {"_index" : "fruits", "_id", "1"}}
   {"id": "1", "name": "apple", "color": "red"}
   ...           ...             ...        ...
   {"create", {"_index" : "fruits", "_id", "N"}}
   {"id": "N", "name": "dragonfruit", "color": "pink"}

   ```

7. Pass the content type that is expected by the search backend for bulk requests.
8. The bulk API's delete operation JSON already contains all the required information; hence, there is no request body following this operation in the Bulk API request body.

   ```json
   {"delete", {"_index" : "fruits", "_id", "1"}}
   {"delete", {"_index" : "fruits", "_id", "2"}}
   ...           ...             ...        ...
   {"delete", {"_index" : "fruits", "_id", "N"}}

   ```

9. In order to deserialize the object from Elasticsearch, we again use Vert.x JsonObject.

Now, create the `org.acme.elasticsearch.FruitResource` class as follows:

```java
package org.acme.elasticsearch;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.BadRequestException;

@Path("/fruits")
public class FruitResource {

    @Inject
    FruitService fruitService;

    @POST
    public Response index(Fruit fruit) throws IOException {
        if (fruit.id == null) {
            fruit.id = UUID.randomUUID().toString();
        }
        fruitService.index(fruit);
        return Response.created(URI.create("/fruits/" + fruit.id)).build();
    }

    @Path("bulk")
    @DELETE
    public Response delete(List<String> identityList) throws IOException {
        fruitService.delete(identityList);
        return Response.ok().build();
    }

    @Path("bulk")
    @POST
    public Response index(List<Fruit> list) throws IOException {
        fruitService.index(list);
        return Response.ok().build();
    }

    @GET
    @Path("/{id}")
    public Fruit get(String id) throws IOException {
        return fruitService.get(id);
    }

    @GET
    @Path("/search")
    public List<Fruit> search(@RestQuery String name, @RestQuery String color) throws IOException {
        if (name != null) {
            return fruitService.searchByName(name);
        } else if (color != null) {
            return fruitService.searchByColor(color);
        } else {
            throw new BadRequestException("Should provide name or color query parameter");
        }
    }
}
```

The implementation is pretty straightforward and you just need to define your endpoints using the Jakarta REST annotations and use the `FruitService` to list/add new fruits.

## Configuring Elasticsearch

The main property to configure is the URL to connect to the Elasticsearch cluster.

For a typical clustered Elasticsearch service, a sample configuration would look like the following:

```properties
# configure the Elasticsearch client for a cluster of two nodes
quarkus.elasticsearch.hosts = elasticsearch1:9200,elasticsearch2:9200
```

In our case, we are using a single instance running on localhost:

```properties
# configure the Elasticsearch client for a single instance on localhost
quarkus.elasticsearch.hosts = localhost:9200
```

If you need a more advanced configuration, you can find the comprehensive list of supported configuration properties at the end of this guide.

<a id="dev-services"></a>
### Dev Services

Quarkus supports a feature called Dev Services that allows you to start various containers without any config. In the case of Elasticsearch, this support extends to the default Elasticsearch connection. What that means practically is that, if you have not configured `quarkus.elasticsearch.hosts`, Quarkus will automatically start an Elasticsearch container when running tests or dev mode, and automatically configure the connection.

When running the production version of the application, the Elasticsearch connection needs to be configured as usual, so if you want to include a production database config in your `application.properties` and continue to use Dev Services we recommend that you use the `%prod.` profile to define your Elasticsearch settings.

For more information you can read the [Dev Services for Elasticsearch guide](/guides/elasticsearch-dev-services/index.md).

### Programmatically Configuring Elasticsearch

On top of the parametric configuration, you can also programmatically apply additional configuration to the client by implementing a `ElasticsearchClientConfigConfigurer` and annotating it with `ElasticsearchClientConfig`. You may provide multiple implementations and configuration provided by each implementation will be applied in a randomly ordered cascading manner.

For example, when accessing an Elasticsearch cluster that is set up for TLS on the HTTP layer, the client needs to trust the certificate that Elasticsearch is using. The following is an example of setting up the client to trust the CA that has signed the certificate that Elasticsearch is using, when that CA certificate is available in a PKCS#12 keystore.

```java
import io.quarkus.elasticsearch.restclient.lowlevel.ElasticsearchClientConfig;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;

import jakarta.enterprise.context.Dependent;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;

@ElasticsearchClientConfig
public class SSLContextConfigurator implements ElasticsearchClientConfigConfigurer {
    @Override
    public void accept(HttpAsyncClientBuilder httpClientBuilder) {
        try {
            String keyStorePass = "password-for-keystore";
            Path trustStorePath = Paths.get("/path/to/truststore.p12");
            KeyStore truststore = KeyStore.getInstance("pkcs12");
            try (InputStream is = Files.newInputStream(trustStorePath)) {
                truststore.load(is, keyStorePass.toCharArray());
            }
            SSLContextBuilder sslBuilder = SSLContexts.custom()
                    .loadTrustMaterial(truststore, null);
            SSLContext sslContext = sslBuilder.build();

            httpClientBuilder.setConnectionManager(
                    PoolingAsyncClientConnectionManagerBuilder
                            .create().setTlsStrategy(
                                    ClientTlsStrategyBuilder.create()
                                            .setSslContext(sslContext).buildAsync()
                            ).build()
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

See [Elasticsearch documentation](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/_encrypted_communication.html) for more details on this particular example.

> [!NOTE]
> Classes marked with `@ElasticsearchClientConfig` are made application scoped CDI beans by default. You can override the scope at the class level if you prefer a different scope.

## Running an Elasticsearch cluster

As by default, the Elasticsearch client is configured to access a local Elasticsearch cluster on port 9200 (the default Elasticsearch port), if you have a local running instance on this port, there is nothing more to do before being able to test it!

If you want to use Docker to run an Elasticsearch instance, you can use the following command to launch one:

```bash
docker run --name elasticsearch  -e "discovery.type=single-node" -e "ES_JAVA_OPTS=-Xms512m -Xmx512m"\
       -e "cluster.routing.allocation.disk.threshold_enabled=false" -e "xpack.security.enabled=false"\
       --rm -p 9200:9200 docker.io/elastic/elasticsearch:9.4.1
```

## Running the application

Let's start our application in dev mode:

**CLI**

```bash
quarkus dev
```

**Maven**

```bash
./mvnw quarkus:dev
```

**Gradle**

```bash
./gradlew --console=plain quarkusDev
```

You can add new fruits to the list via the following curl command:

```bash
curl localhost:8080/fruits -d '{"name": "bananas", "color": "yellow"}' -H "Content-Type: application/json"
```

And search for fruits by name or color via the following curl command:

```bash
curl localhost:8080/fruits/search?color=yellow
```

<a id="using-the-elasticsearch-java-client"></a>
## Using the Elasticsearch Java Client

Here is a version of the `FruitService` using the Elasticsearch Java Client instead of the low level one:

```java
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;

@ApplicationScoped
public class FruitService {
    @Inject
    ElasticsearchClient client; // <1>

    public void index(Fruit fruit) throws IOException {
        IndexRequest<Fruit> request = IndexRequest.of(  // <2>
            b -> b.index("fruits")
                .id(fruit.id)
                .document(fruit)); // <3>
        client.index(request);  // <4>
    }

    public void index(List<Fruit> list) throws IOException {

        BulkRequest.Builder br = new BulkRequest.Builder();

        for (var fruit : list) {
            br.operations(op -> op
                    .index(idx -> idx.index("fruits").id(fruit.id).document(fruit)));
        }

        BulkResponse result = client.bulk(br.build());

        if (result.errors()) {
            throw new RuntimeException("The indexing operation encountered errors.");
        }
    }

    public void delete(List<String> list) throws IOException {

        BulkRequest.Builder br = new BulkRequest.Builder();

        for (var id : list) {
            br.operations(op -> op.delete(idx -> idx.index("fruits").id(id)));
        }

        BulkResponse result = client.bulk(br.build());

        if (result.errors()) {
            throw new RuntimeException("The indexing operation encountered errors.");
        }
    }

    public Fruit get(String id) throws IOException {
        GetRequest getRequest = GetRequest.of(
            b -> b.index("fruits")
                .id(id));
        GetResponse<Fruit> getResponse = client.get(getRequest, Fruit.class);
        if (getResponse.found()) {
            return getResponse.source();
        }
        return null;
    }

    public List<Fruit> searchByColor(String color) throws IOException {
        return search("color", color);
    }

    public List<Fruit> searchByName(String name) throws IOException {
        return search("name", name);
    }

    private List<Fruit> search(String term, String match) throws IOException {
        SearchRequest searchRequest = SearchRequest.of(
            b -> b.index("fruits")
                .query(QueryBuilders.match().field(term).query(FieldValue.of(match)).build()._toQuery()));

        SearchResponse<Fruit> searchResponse = client.search(searchRequest, Fruit.class);
        HitsMetadata<Fruit> hits = searchResponse.hits();
        return hits.hits().stream().map(hit -> hit.source()).collect(Collectors.toList());
    }
}
```

1. We inject an `ElasticsearchClient` inside the service.
2. We create an Elasticsearch index request using a builder.
3. We directly pass the object to the request as the Java API client has a serialization layer.
4. We send the request to Elasticsearch.

## Hibernate Search Elasticsearch

Quarkus supports Hibernate Search with Elasticsearch via the `quarkus-hibernate-search-orm-elasticsearch` extension.

Hibernate Search Elasticsearch allows to synchronize your Jakarta Persistence entities to an Elasticsearch cluster and offers a way to query your Elasticsearch cluster using the Hibernate Search API.

If you are interested in it, please consult the [Hibernate Search with Elasticsearch guide](/guides/hibernate-search-orm-elasticsearch/index.md).

## Cluster Health Check

If you are using the `quarkus-smallrye-health` extension, both extensions will automatically add a readiness health check to validate the health of the cluster.

So when you access the `/q/health/ready` endpoint of your application, you will have information about the cluster status. It uses the cluster health endpoint, the check will be down if the status of the cluster is **red**, or the cluster is not available.

This behavior can be disabled by setting the `quarkus.elasticsearch.health.enabled` property to `false` in your `application.properties`.

## Building a native executable

You can use both clients in a native executable.

You can build a native executable with the usual command:

**CLI**

```bash
quarkus build --native
```

**Maven**

```bash
./mvnw install -Dnative
```

**Gradle**

```bash
./gradlew build -Dquarkus.native.enabled=true
```

Running it is as simple as executing `./target/elasticsearch-low-level-client-quickstart-1.0.0-SNAPSHOT-runner`.

You can then point your browser to `http://localhost:8080/fruits.html` and use your application.

## Conclusion

Accessing an Elasticsearch cluster from the low level REST client or the Elasticsearch Java client is easy with Quarkus as it provides easy configuration, CDI integration and native support for it.

## Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-health-enabled"></a> [`quarkus.elasticsearch.health.enabled`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-health-enabled) config_property_copy_button:+++quarkus.elasticsearch.health.enabled+++[]<br><br>Whether a health check is published in case the smallrye-health extension is present.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_HEALTH_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-hosts"></a> [`quarkus.elasticsearch.hosts`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-hosts) config_property_copy_button:+++quarkus.elasticsearch.hosts+++[]<br><br>The list of hosts of the Elasticsearch servers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_HOSTS+++[] | list of host:port | `localhost:9200` |
| <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-protocol"></a> [`quarkus.elasticsearch.protocol`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-protocol) config_property_copy_button:+++quarkus.elasticsearch.protocol+++[]<br><br>The protocol to use when contacting Elasticsearch servers. Set to "https" to enable SSL/TLS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_PROTOCOL+++[] | string | `http` |
| <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-username"></a> [`quarkus.elasticsearch.username`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-username) config_property_copy_button:+++quarkus.elasticsearch.username+++[]<br><br>The username for basic HTTP authentication.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_USERNAME+++[] | string |  |
| <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-password"></a> [`quarkus.elasticsearch.password`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-password) config_property_copy_button:+++quarkus.elasticsearch.password+++[]<br><br>The password for basic HTTP authentication.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_PASSWORD+++[] | string |  |
| <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-api-key"></a> [`quarkus.elasticsearch.api-key`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-api-key) config_property_copy_button:+++quarkus.elasticsearch.api-key+++[]<br><br>The API key for authentication.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_API_KEY+++[] | string |  |
| <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-connection-timeout"></a> [`quarkus.elasticsearch.connection-timeout`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-connection-timeout) config_property_copy_button:+++quarkus.elasticsearch.connection-timeout+++[]<br><br>The connection timeout.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_CONNECTION_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-elasticsearch-rest-client_quarkus-elasticsearch) | `1S` |
| <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-socket-timeout"></a> [`quarkus.elasticsearch.socket-timeout`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-socket-timeout) config_property_copy_button:+++quarkus.elasticsearch.socket-timeout+++[]<br><br>The socket timeout.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_SOCKET_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-elasticsearch-rest-client_quarkus-elasticsearch) | `30S` |
| <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-max-connections"></a> [`quarkus.elasticsearch.max-connections`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-max-connections) config_property_copy_button:+++quarkus.elasticsearch.max-connections+++[]<br><br>The maximum number of connections to all the Elasticsearch servers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_MAX_CONNECTIONS+++[] | int | `40` |
| <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-max-connections-per-route"></a> [`quarkus.elasticsearch.max-connections-per-route`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-max-connections-per-route) config_property_copy_button:+++quarkus.elasticsearch.max-connections-per-route+++[]<br><br>The maximum number of connections per Elasticsearch server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_MAX_CONNECTIONS_PER_ROUTE+++[] | int | `20` |
| <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-io-thread-counts"></a> [`quarkus.elasticsearch.io-thread-counts`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-io-thread-counts) config_property_copy_button:+++quarkus.elasticsearch.io-thread-counts+++[]<br><br>The number of IO thread. By default, this is the number of locally detected processors.<br><br>Thread counts higher than the number of processors should not be necessary because the I/O threads rely on non-blocking operations, but you may want to use a thread count lower than the number of processors.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_IO_THREAD_COUNTS+++[] | int |  |
| <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-discovery-enabled"></a> [`quarkus.elasticsearch.discovery.enabled`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-discovery-enabled) config_property_copy_button:+++quarkus.elasticsearch.discovery.enabled+++[]<br><br>Defines if automatic discovery is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DISCOVERY_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-discovery-refresh-interval"></a> [`quarkus.elasticsearch.discovery.refresh-interval`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-discovery-refresh-interval) config_property_copy_button:+++quarkus.elasticsearch.discovery.refresh-interval+++[]<br><br>Refresh interval of the node list.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DISCOVERY_REFRESH_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-elasticsearch-rest-client_quarkus-elasticsearch) | `5M` |
| <a id="quarkus-elasticsearch-rest-client_section_quarkus-elasticsearch-devservices"></a> [Dev Services](#quarkus-elasticsearch-rest-client_section_quarkus-elasticsearch-devservices) | Type | Default |
| [lock] <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices-enabled"></a> [`quarkus.elasticsearch.devservices.enabled`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices-enabled) config_property_copy_button:+++quarkus.elasticsearch.devservices.enabled+++[]<br><br>Whether this Dev Service should start with the application in dev mode or tests.<br><br>Dev Services are enabled by default unless connection configuration (e.g. `quarkus.elasticsearch.hosts`) is set explicitly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DEVSERVICES_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices-port"></a> [`quarkus.elasticsearch.devservices.port`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices-port) config_property_copy_button:+++quarkus.elasticsearch.devservices.port+++[]<br><br>Optional fixed port the dev service will listen to.<br><br>If not defined, the port will be chosen randomly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DEVSERVICES_PORT+++[] | int |  |
| [lock] <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices-distribution"></a> [`quarkus.elasticsearch.devservices.distribution`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices-distribution) config_property_copy_button:+++quarkus.elasticsearch.devservices.distribution+++[]<br><br>The Elasticsearch distribution to use.<br><br>Defaults to a distribution inferred from the explicitly configured `image-name` (if any), or by default to the distribution configured in depending extensions (e.g. Hibernate Search), or by default to `elastic`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DEVSERVICES_DISTRIBUTION+++[] | `elastic`, `opensearch` |  |
| [lock] <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices-image-name"></a> [`quarkus.elasticsearch.devservices.image-name`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices-image-name) config_property_copy_button:+++quarkus.elasticsearch.devservices.image-name+++[]<br><br>The Elasticsearch container image to use.<br><br>Defaults depend on the configured `distribution`:<br><br>- For the `elastic` distribution: `docker.io/elastic/elasticsearch:9.4.1`<br>- For the `opensearch` distribution: `docker.io/opensearchproject/opensearch:3.6.0`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DEVSERVICES_IMAGE_NAME+++[] | string |  |
| [lock] <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices-java-opts"></a> [`quarkus.elasticsearch.devservices.java-opts`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices-java-opts) config_property_copy_button:+++quarkus.elasticsearch.devservices.java-opts+++[]<br><br>The value for the ES_JAVA_OPTS env variable.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DEVSERVICES_JAVA_OPTS+++[] | string | `-Xms512m -Xmx1g` |
| [lock] <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices-shared"></a> [`quarkus.elasticsearch.devservices.shared`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices-shared) config_property_copy_button:+++quarkus.elasticsearch.devservices.shared+++[]<br><br>Whether the Elasticsearch server managed by Quarkus Dev Services is shared.<br><br>When shared, Quarkus looks for running containers using label-based service discovery. If a matching container is found, it is used, and so a second one is not started. Otherwise, Dev Services for Elasticsearch starts a new container.<br><br>The discovery uses the `quarkus-dev-service-elasticsearch` label. The value is configured using the `service-name` property.<br><br>Container sharing is only used in dev mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DEVSERVICES_SHARED+++[] | boolean | `true` |
| [lock] <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices-service-name"></a> [`quarkus.elasticsearch.devservices.service-name`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices-service-name) config_property_copy_button:+++quarkus.elasticsearch.devservices.service-name+++[]<br><br>The value of the `quarkus-dev-service-elasticsearch` label attached to the started container.<br><br>This property is used when `shared` is set to `true`. In this case, before starting a container, Dev Services for Elasticsearch looks for a container with the `quarkus-dev-service-elasticsearch` label set to the configured value. If found, it will use this container instead of starting a new one. Otherwise it starts a new container with the `quarkus-dev-service-elasticsearch` label set to the specified value.<br><br>This property is used when you need multiple shared Elasticsearch servers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DEVSERVICES_SERVICE_NAME+++[] | string | `elasticsearch` |
| [lock] <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices-container-env-environment-variable-name"></a> [`quarkus.elasticsearch.devservices.container-env."environment-variable-name"`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices-container-env-environment-variable-name) config_property_copy_button:+++quarkus.elasticsearch.devservices.container-env."environment-variable-name"+++[]<br><br>Environment variables that are passed to the container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DEVSERVICES_CONTAINER_ENV__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices-reuse"></a> [`quarkus.elasticsearch.devservices.reuse`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices-reuse) config_property_copy_button:+++quarkus.elasticsearch.devservices.reuse+++[]<br><br>Whether to keep Dev Service containers running **after a dev mode session or test suite execution** to reuse them in the next dev mode session or test suite execution.<br><br>Within a dev mode session or test suite execution, Quarkus will always reuse Dev Services as long as their configuration (username, password, environment, port bindings, ...) did not change. This feature is specifically about keeping containers running **when Quarkus is not running** to reuse them across runs.<br><br>> [!WARNING]<br>> This feature needs to be enabled explicitly in `testcontainers.properties`, may require changes to how you configure data initialization in dev mode and tests, and may leave containers running indefinitely, forcing you to stop and remove them manually. See [this section of the documentation](/guides/elasticsearch-dev-services/index.md#reuse) for more information.<br><br>This configuration property is set to `true` by default, so it is mostly useful to **disable** reuse, if you enabled it in `testcontainers.properties` but only want to use it for some of your Quarkus applications.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DEVSERVICES_REUSE+++[] | boolean | `true` |

<a id="duration-note-anchor-quarkus-elasticsearch-rest-client_quarkus-elasticsearch"></a>

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
