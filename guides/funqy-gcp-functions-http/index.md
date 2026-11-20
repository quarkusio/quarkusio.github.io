# Funqy HTTP binding with Google Cloud Functions

If you want to allow HTTP clients to invoke your Funqy functions on Google Cloud Functions, Quarkus allows you to expose multiple Funqy functions through HTTP deployed as one Google Cloud Function.  This approach does add overhead over the regular Funqy Google Cloud Function integration.

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered preview.
>
> In *preview*, backward compatibility and presence in the ecosystem is not guaranteed. Specific improvements might require changing configuration or APIs, and plans to become *stable* are under way. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

Follow the [Google Cloud Functions Http Guide](/guides/gcp-functions-http/index.md).  It walks through using a variety of HTTP frameworks on Google Cloud Functions, including Funqy.

> [!WARNING]
> The Funqy HTTP + Google Cloud Functions binding is not a replacement for REST over HTTP.  Because Funqy needs to be portable across a lot of different protocols and function providers its HTTP binding is very minimalistic and you will lose REST features like linking and the ability to leverage HTTP features like cache-control and conditional GETs.  You may want to consider using Quarkus's Jakarta REST, Spring MVC, or Vert.x Web Reactive Route [support](/guides/gcp-functions-http/index.md) instead.  They also work with Quarkus and Google Cloud Functions.

## An additional Quickstart

Beyond generating a Google Cloud Functions project that is covered in the [Google Cloud Functions HTTP Guide](/guides/gcp-functions-http/index.md), there's also a quickstart for running Funqy HTTP on Google Cloud Functions.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `funqy-google-cloud-functions-http-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/funqy-quickstarts/funqy-google-cloud-functions-http-quickstart).

## The Code

There is nothing special about the code and more importantly nothing Google Cloud specific.  Funqy functions can be deployed to many environments and Google Cloud Functions is one of them.  The Java code is actually the same exact code as the [funqy-http-quickstart](https://github.com/quarkusio/quarkus-quickstarts/tree/main/funqy-quickstarts/funqy-http-quickstart).

## Getting Started

The steps to get this quickstart running are exactly the same as defined in the [Google Cloud Functions HTTP Guide](/guides/gcp-functions-http/index.md). These differences are that you are running from a quickstart and the Maven dependencies are slightly different.

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-funqy-http</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-google-cloud-functions-http</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-funqy-http")
implementation("io.quarkus:quarkus-google-cloud-functions-http")
```
