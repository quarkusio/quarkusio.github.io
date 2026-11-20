# Funqy HTTP Binding with AWS Lambda 

If you want to allow HTTP clients to invoke on your Funqy functions on AWS Lambda, Quarkus allows you to expose multiple Funqy functions through HTTP deployed as one AWS Lambda.  This approach does add overhead over the regular Funqy AWS Lambda integration and also requires you to use AWS API Gateway.

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered preview.
>
> In *preview*, backward compatibility and presence in the ecosystem is not guaranteed. Specific improvements might require changing configuration or APIs, and plans to become *stable* are under way. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

Follow the [AWS Lambda Http Guide](/guides/aws-lambda-http/index.md).  It walks through using a variety of HTTP frameworks on AWS Lambda, including Funqy.

> [!WARNING]
> The Funqy HTTP + AWS Lambda binding is not a replacement for REST over HTTP.  Because Funqy needs to be portable across a lot of different protocols and function providers its HTTP binding is very minimalistic and you will lose REST features like linking and the ability to leverage HTTP features like cache-control and conditional GETs.  You may want to consider using Quarkus's Jakarta REST, Spring MVC, or Vert.x Web Reactive Route [support](/guides/aws-lambda-http/index.md) instead.  They also work with Quarkus and AWS Lambda.

## An additional Quickstart

Beyond generating an AWS project that is covered in the [AWS Lambda Http Guide](/guides/aws-lambda-http/index.md), there's also a quickstart for running Funqy HTTP on AWS Lambda.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `funqy-amazon-lambda-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/funqy-quickstarts/funqy-amazon-lambda-http-quickstart).

## The Code

There is nothing special about the code and more importantly nothing AWS specific.  Funqy functions can be deployed to many environments and AWS Lambda is one of them.  The Java code is actually the same exact code as the [funqy-http-quickstart](https://github.com/quarkusio/quarkus-quickstarts/tree/main/funqy-quickstarts/funqy-http-quickstart).

## Getting Started

The steps to get this quickstart running are exactly the same as defined in the [AWS Lambda HTTP Guide](/guides/aws-lambda-http/index.md). These differences are that you are running from a quickstart and the maven dependencies are slightly different.

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-funqy-http</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-amazon-lambda-http</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-funqy-http")
implementation("io.quarkus:quarkus-amazon-lambda-http")
```
