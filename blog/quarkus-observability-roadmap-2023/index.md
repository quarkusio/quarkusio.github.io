# Quarkus Observability Roadmap 2023

[Quarkus](https://quarkus.io) has come a long way in the *observability* space. Initial support for Tracing and Metrics came from MicroProfile and their respective implementations, [SmallRye OpenTracing](https://github.com/smallrye/smallrye-opentracing/) and [SmallRye Metrics](https://github.com/smallrye/smallrye-metrics/). Because the observability space evolved, these implementations expanded beyond the original specifications, and the Quarkus extensions providing Tracing and Metrics have been heavily used.

Our goal with Quarkus is always to offer the best technologies and embrace the state of the practice to get the job done. It was true for both [SmallRye OpenTracing](https://github.com/smallrye/smallrye-opentracing/) and [SmallRye Metrics](https://github.com/smallrye/smallrye-metrics/), but we now believe there are better alternatives to pursue in the upcoming Quarkus versions.

## OpenTracing Archived

At the beginning of 2022, the [CNCF](https://www.cncf.io) announced that there were [archiving the OpenTracing project](https://www.cncf.io/blog/2022/01/31/cncf-archives-the-opentracing-project/), meaning that the project is no longer maintained. An expected decision since the merger between [OpenTracing](https://opentracing.io) and [OpenCensus](http://opencensus.io/) to kick off the [OpenTelemetry](http://opentelemetry.io) project.

Quarkus released the first version of the OpenTelemetry Extension with Quarkus 1.13.0.Final. The initial support was only for HTTP Tracing (Vert.x and REST) but expanded to cover other components, including CDI, gRPC, Reactive Messaging, and many more, while also adopting and adjusting to all the new APIs released by OpenTelemetry Tracing. Our goal is to continue this work, expand OpenTelemetry Tracing to the remaining components and close the integration gaps between Quarkus and Tracing.

\*Moving forward, [SmallRye OpenTracing](https://github.com/smallrye/smallrye-opentracing/) and all the [OpenTracing](https://opentracing.io) work in Quarkus are now deprecated!\*

While things will likely continue to work, we encourage all our users to move to [OpenTelemetry](http://opentelemetry.io), for all the Tracing capabilities. In the following upcoming versions, we may start to remove [OpenTracing](https://opentracing.io) support from some extensions and continue that work until we completely remove [OpenTracing](https://opentracing.io) from [Quarkus](https://quarkus.io).

For [MicroProfile](http://microprofile.io) users out there that rely on [MicroProfile OpenTracing](https://github.com/eclipse/microprofile-opentracing), MicroProfile 6.0, which will be supported by Quarkus 3.x, ships [MicroProfile Telemetry Tracing](https://github.com/eclipse/microprofile-telemetry). This new specification defines the behaviors that allow MicroProfile applications to participate in an environment where distributed tracing is enabled via OpenTelemetry Tracing.

## Metrics and Micrometer

On the Metrics front, it became evident that while [MicroProfile Metrics](https://github.com/eclipse/microprofile-metrics) and [SmallRye Metrics](https://github.com/smallrye/smallrye-metrics/) were acceptable for simple use cases, they had some issues with more advanced ones. It also didn't help that users from different technologies had to learn new APIs and constructs to extract metrics information from their applications. We made a move to adopt [Micrometer](https://micrometer.io) to resolve some of these issues while at the same time offering a less intrusive way to monitor Quarkus applications.

We know that a new version of [MicroProfile Metrics](https://github.com/eclipse/microprofile-metrics) is in the works, and an implementation provided by [SmallRye Metrics](https://github.com/smallrye/smallrye-metrics/) will be available. Unfortunately, at this time, we feel that the current [MicroProfile Metrics](https://github.com/eclipse/microprofile-metrics) path does not address many of our concerns.

We intend to keep the [Micrometer](https://micrometer.io) approach and expand it to the remaining areas. With OpenTelemetry Metrics reaching a stable milestone and Micrometer collaborating with [OpenTelemetry](http://opentelemetry.io), we expect to tie together both tracing and metrics in the Observability capabilities of Quarkus.

Quarkus provides MicroProfile implementations when the proposed specification sufficiently addresses the problem space. If needs are not sufficiently addressed and an industry initiative is open to collaboration, Quarkus proposes working within those initiatives to address MicroProfile needs and not having MicroProfile appear to compete with them by creating another specification. The current MicroProfile Metrics proposal is for consumers to use specific MicroProfile Metrics annotations and APIs, and it is up to the implementations to provide the support for them. We believe that a better approach is to follow OpenTelemetry Metrics and Micrometer initiatives, which are already well established and cover a comprehensive set of system components, including cloud providers, applications, and tooling.

\*We don’t intend to support the new [MicroProfile Metrics](https://github.com/eclipse/microprofile-metrics) specification, and the current SmallRye Metrics Quarkus extension will be deprecated.\*

The [SmallRye Metrics](https://github.com/smallrye/smallrye-metrics/) component will still be under active development by IBM and Tomitribe. We will keep monitoring the situation and may revise our decision based on users feedback.

## Future

There is still a lot of work to be done in Tracing and Metrics, but we also want to start looking into Logging, which is part of the [OpenTelemetry](http://opentelemetry.io) specification. Our primary focus for 2023 will be to clean up and provide the missing capabilities on the existing extensions and give some new things coming from [OpenTelemetry](http://opentelemetry.io). As always, we are looking forward to your feedback and contributions. Feel free to look at our [Observability roadmap](https://github.com/quarkusio/quarkus/projects/20) and help us move forward!
