# gRPC

[gRPC](https://grpc.io/) is a high-performance RPC framework. It can efficiently connect services implemented using various languages and frameworks. It is also applicable in the last mile of distributed computing to connect devices, mobile applications, and browsers to backend services.

In general, gRPC uses HTTP/2, TLS, and [Protobuf (Protocol Buffers)](https://developers.google.com/protocol-buffers). In a microservice architecture, gRPC is an efficient, type-safe alternative to HTTP.

The Quarkus gRPC extension integrates gRPC in Quarkus applications. It:

- supports implementing gRPC services
- supports consuming gRPC services
- integrates with the reactive engine from Quarkus as well as the reactive development model
- allows plain-text communication as well as TLS, and TLS with mutual authentication
- supports [xDS gRPC](https://grpc.github.io/grpc/core/md_doc_grpc_xds_features.html) integration
- supports [InProcess](https://grpc.github.io/grpc-java/javadoc/io/grpc/inprocess/InProcessServerBuilder.html) gRPC development

Quarkus gRPC is based on [Vert.x gRPC](https://vertx.io/docs/vertx-grpc/java/).

- [Getting Started](/guides/grpc-getting-started/index.md)
- [Implementing a gRPC Service](/guides/grpc-service-implementation/index.md)
- [Consuming a gRPC Service](/guides/grpc-service-consumption/index.md)
- [Deploying your gRPC Service in Kubernetes](/guides/grpc-kubernetes/index.md)
- [Enabling xDS gRPC support](/guides/grpc-xds/index.md)
- [gRPC code generation reference guide](/guides/grpc-generation-reference/index.md)
- [gRPC reference guide](/guides/grpc-reference/index.md)
- [gRPC CLI support](/guides/grpc-cli/index.md)
