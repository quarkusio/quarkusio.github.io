# DataCater uses Quarkus to make Data Streaming more accessible

This article gives a brief overview of the data streaming platform DataCater, discusses how we moved from Scala Play! and Kafka Streams to Quarkus, and presents why we think that Quarkus is an exceptional framework for developing cloud-native Java applications.

## What is DataCater?

[DataCater^](https://datacater.io) is a real-time, cloud-native data pipeline platform based on Apache Kafka and Kubernetes. It allows users to build streaming data pipelines that stream records between different Apache Kafka topics and can apply filters or transforms to the records on the way. Given its focus on data scientists and data engineers as target users, DataCater enables users to develop transforms in Python. It provides an [interactive, UI-based preview of streaming data pipelines^](https://www.loom.com/share/15947f13e71540948e3f9d57eac64976) and uses Kubernetes as the runtime for pipeline deployments.

## Why did DataCater choose Quarkus?

DataCater was created in 2020 and initially built its control plane on top of the Scala framework [Play^](https://www.playframework.com/) and implemented pipelines with [Kafka Streams^](https://kafka.apache.org/documentation/streams/). Over time, we experienced the following limitations and issues with the chosen technologies:

1. **Inefficient resource usage:** Kafka Streams applications consume a considerable amount of main memory, making it quite expensive to operate at scale.
2. **Long startup times:** Starting a Kafka Streams application can take up to a few minutes, which has a negative impact on the availability of streaming data pipelines.
3. **Restriction to intra-cluster streaming:** By default, Kafka Streams can only stream data between topics of the same Apache Kafka cluster. When facing use cases that stream data between different Kafka clusters, for instance, between a production and test cluster, we had to employ additional tooling, e.g., MirrorMaker 2.
4. **No support for Java 17:** The current Play! version 2.8 does not support running on top of Java 17.

Especially the first two issues, inefficient resource usage and long startup times, hurt a lot when operating in the cloud at scale.

In 2022, we rewrote DataCater to implement lots of learnings that we made when working with early users. Using this unique opportunity, we also switched from Play! and Kafka Streams to Quarkus, thus using Quarkus for implementing both our control plane and the data pipelines.

> With Quarkus, we are able to bring the best of cloud native and messaging applications together. Streaming messages, especially in the context of Apache Kafka, is still a Java-dominated environment, while the traditional Java stack lacks the characteristics of cloud-native applications, like small footprints, fast startups, and self-containment. **-Hakan Lofcali, CTO, DataCater**

In the following, we list the main reasons why we chose Quarkus over other Java frameworks:

1. **Versatility:** We cannot only implement the API of our control plane with the Quarkus RESTeasy extension but can also employ Quarkus as the base for implementing streaming data pipelines using its [SmallRye Reactive Messaging^](https://smallrye.io/smallrye-reactive-messaging) extension.
2. **Dev services:** Quarkus' dev services help us to spin up dependencies, like PostgreSQL or Apache Kafka, very fast and provide an outstanding developer experience. Our developers can focus on their job instead of messing with the configuration of tooling.
3. **Support for native executables:** Quarkus allows us to easily build native executables, which are very beneficial when operating in a cloud context. They require much fewer resources and achieve faster startup times.
4. **Minimal footprint:** Quarkus’ build time optimizations allow for smaller footprints of JVM- and GraalVM-based containers.

## References

- [DataCater GitHub repository^](https://github.com/DataCater/datacater)
- [DataCater documentation^](https://docs.datacater.io)
