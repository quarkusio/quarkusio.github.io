# Using gRPC CLI

This page explains how to use gRPC CLI -- a grpcurl-like tool.

See basic Quarkus CLI usage first:

- [CLI tooling](/guides/cli-tooling/index.md)

## Examples

Display gRPC CLI help:

```shell
quarkus grpc -h
Usage: grpc [-h] [COMMAND]
  -h, --help   Display this help message.
Commands:
  list      grpc list
  describe  grpc describe
  invoke    grpc invoke
```

List all available gRPC services:

```shell
quarkus grpc list localhost:8080

helloworld.Greeter
grpc.health.v1.Health
```

Describe a service:

```shell
quarkus grpc describe localhost:8080 helloworld.Greeter

{
  "name": "helloworld.proto",
  "package": "helloworld",
  "dependency": ["google/protobuf/empty.proto"],
  "messageType": [{
    "name": "HelloRequest",
    "field": [{
      "name": "name",
      "number": 1,
      "label": "LABEL_OPTIONAL",
      "type": "TYPE_STRING"
    }]
  }, {
    "name": "HelloReply",
    "field": [{
      "name": "message",
      "number": 1,
      "label": "LABEL_OPTIONAL",
      "type": "TYPE_STRING"
    }]
  }],
  "service": [{
    "name": "Greeter",
    "method": [{
      "name": "SayHello",
      "inputType": ".helloworld.HelloRequest",
      "outputType": ".helloworld.HelloReply",
      "options": {
      }
    }, {
      "name": "SayJo",
      "inputType": ".google.protobuf.Empty",
      "outputType": ".helloworld.HelloReply",
      "options": {
      }
    }, {
      "name": "ThreadName",
      "inputType": ".helloworld.HelloRequest",
      "outputType": ".helloworld.HelloReply",
      "options": {
      }
    }]
  }],
  "options": {
    "javaPackage": "examples",
    "javaOuterClassname": "HelloWorldProto",
    "javaMultipleFiles": true,
    "objcClassPrefix": "HLW"
  },
  "syntax": "proto3"
}
{
  "name": "google/protobuf/empty.proto",
  "package": "google.protobuf",
  "messageType": [{
    "name": "Empty"
  }],
  "options": {
    "javaPackage": "com.google.protobuf",
    "javaOuterClassname": "EmptyProto",
    "javaMultipleFiles": true,
    "goPackage": "google.golang.org/protobuf/types/known/emptypb",
    "ccEnableArenas": true,
    "objcClassPrefix": "GPB",
    "csharpNamespace": "Google.Protobuf.WellKnownTypes"
  },
  "syntax": "proto3"
}
```

Invoke a service method:

```shell
quarkus grpc invoke localhost:8080 helloworld.Greeter/SayHello -d '{"name" : "gRPC"}'

{
  "message": "Hello gRPC"
}
```
