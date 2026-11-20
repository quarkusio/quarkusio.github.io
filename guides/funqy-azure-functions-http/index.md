# Funqy HTTP binding with Azure Functions

You can use [Funqy HTTP](/guides/funqy-http/index.md) on Azure Functions.  This allows you to invoke on multiple Funqy functions using HTTP deployed as one Azure Function.

> [!WARNING]
> The Funqy HTTP + Azure Functions binding is not a replacement for REST over HTTP.  Because Funqy needs to be portable cross a lot of different protocols and function providers its HTTP binding is very minimalistic and you will lose REST features like linking and the ability to leverage HTTP features like cache-control and conditional GETs.  You may want to consider using Quarkus's Jakarta REST, Spring MVC, or Vert.x Web Reactive Route [support](/guides/azure-functions-http/index.md) instead.  They also work with Quarkus and Azure Functions.

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered preview.
>
> In *preview*, backward compatibility and presence in the ecosystem is not guaranteed. Specific improvements might require changing configuration or APIs, and plans to become *stable* are under way. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

Follow the [Azure Functions HTTP Guide](/guides/azure-functions-http/index.md).  It walks through using a variety of HTTP frameworks on Azure Functions.  Including Funqy.
