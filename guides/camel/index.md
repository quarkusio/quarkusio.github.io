# Apache Camel on Quarkus

[Apache Camel](https://camel.apache.org/) is the Swiss knife of integrating heterogeneous systems with more than a decade of history and a lively community of users and developers. It offers 350+ connectors covering various protocols, file formats, messaging systems, databases, APIs and cloud services, so you can integrate almost anything without writing custom adapter code.

Camel Quarkus turns Camel components into Quarkus extensions, so routes are wired at build time and live-reloaded in dev mode.

The [Apache Camel Quarkus documentation](https://camel.apache.org/camel-quarkus/) covers getting started, the list of supported extensions, configuration, observability, testing and native mode.

## Defining a route

Generate a project with the `camel-quarkus-kafka`, `camel-quarkus-jackson` and `camel-quarkus-direct` extensions from [code.quarkus.io](https://code.quarkus.io/?e=org.apache.camel.quarkus:camel-quarkus-kafka&e=org.apache.camel.quarkus:camel-quarkus-jackson&e=org.apache.camel.quarkus:camel-quarkus-direct), then declare the route as a CDI bean:

```java
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class OrderEventRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("direct:order-placed")
                .routeId("order-event")
                .marshal().json()
                .to("kafka:order-events")
                .log("Order event sent to Kafka");
    }
}
```

A route is a processing pipeline: it consumes messages from an endpoint (`direct:order-placed`), transforms them, and sends them to one or more endpoints (`kafka:order-events`). A `ProducerTemplate` is available as a CDI bean, so existing code, such as a JAX-RS resource, can feed the route with `producerTemplate.sendBody("direct:order-placed", order)`.

Run `./mvnw quarkus:dev`: Dev Services starts a Kafka broker and the route publishes each order as JSON to the `order-events` topic. Camel routes are hot-reloaded, so changes take effect on save.

## Start using Camel Quarkus

- [First steps](https://camel.apache.org/camel-quarkus/latest/user-guide/first-steps.html)
- [Examples](https://camel.apache.org/camel-quarkus/latest/user-guide/examples.html)
- [List of Camel connectors](https://camel.apache.org/camel-quarkus/latest/reference/index.html)
