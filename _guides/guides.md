---
layout: guides-index
title: Quarkus - Guides
permalink: /guides/
---

<div class="grid__item width-4-12 hide-mobile toc" markdown="1">
## Table of Contents
### Guides & Documentation

- [Configuring Your Application]({{site.baseurl}}/guides/application-configuration-guide)
- [Application Initialization and Termination]({{site.baseurl}}/guides/application-lifecycle-events-guide)
- [Validation with Hibernate Validator]({{site.baseurl}}/guides/validation-guide)
- [Scheduling Periodic Tasks]({{site.baseurl}}/guides/scheduled-guide)
- [Using WebSockets]({{site.baseurl}}/guides/websocket-guide)
- [Using Hibernate ORM and JPA]({{site.baseurl}}/guides/hibernate-orm-guide)
- [Simplified Hibernate ORM with Panache]({{site.baseurl}}/guides/hibernate-orm-panache-guide)
- [Using OpenTracing]({{site.baseurl}}/guides/opentracing-guide)


### Advanced Guides

- [Writing Your Own Extension]({{site.baseurl}}/guides/extension-authors-guide)
- [Measuring Performance]({{site.baseurl}}/guides/performance-measure)
- [Contexts and Dependency Injection]({{site.baseurl}}/guides/cdi-reference)

## Additional Resources
### Getting Started

- [Creating Your First Application]({{site.baseurl}}/guides/getting-started-guide)
- [Building a Native Image]({{site.baseurl}}/guides/building-native-image-guide)
- [Deploying on Kubernetes and OpenShift]({{site.baseurl}}/guides/kubernetes-guide)
- [Project Scaffolding and IDE]({{site.baseurl}}/guides/ide-configuration)

</div>
<div class="grid__item width-8-12 width-12-12-m guides-content">
<div class="guide-item" markdown="1">
## Configuring Your Application

Hardcoded values in your code is a no go (even if we all did it at some point ;-)). In this guide, we learn how to configure your application.

<a href="{{site.baseurl}}/guides/application-configuration-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Application Initialization and Termination

You often need to execute custom actions when the application starts and clean up everything when the application stops. This guide explains how to be notified when an application stops or starts.

<a href="{{site.baseurl}}/guides/application-lifecycle-events-guid" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Validation with Hibernate Validator

JSON is now the lingua franca between microservices. In this guide, we see how you can consume and produce JSON payloads. The guide also covers how to validate JSON payloads.

<a href="{{site.baseurl}}/guides/validation-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Scheduling Periodic Tasks

Modern applications often need to run specific tasks periodically. In this guide, you learn how to schedule periodic tasks.

<a href="{{site.baseurl}}/guides/scheduled-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Using WebSockets

This guide explains how your Shamrock application can utilize web sockets to create interactive web applications. Because it’s the canonical web socket application, we are going to create a simple chat application.

<a href="{{site.baseurl}}/guides/websocket-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Using Hibernate ORM and JPA

Hibernate ORM is the de facto JPA implementation and offers you the full breath of an Object Relational Mapper. It works beautifully in Quarkus.

<a href="{{site.baseurl}}/guides/hibernate-orm-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Using OpenTracing

This guide explains how your Quarkus application can utilize opentracing to provide distributed tracing for interactive web applications.

<a href="{{site.baseurl}}/guides/opentracing-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Using Infinispan Client

Infinispan is an in memory data grid that allows running in a server outside of application processes. This extension provides functionality to allow the client that can connect to said server when running in Protean.

<a href="{{site.baseurl}}/guides/infinispan-client-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Using our Spring Dependency Injection compatibility layer

While you are encouraged to use CDI annotations for injection, Protean provides a compatibility layer for Spring dependency injection in the form of the spring-di extension.

<a href="{{site.baseurl}}/guides/spring-di-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
</div>
<div class="grid__item width-12-12"><hr></div>
<div class="grid__item width-4-12 width-12-12-m toc" markdown="1">
## Advanced Guides
</div>
<div class="grid__item width-8-12 width-12-12-m guides-content">
<div class="guide-item" markdown="1">
## Write Your Own Extension

Quarkus extensions add a new developer focused behavior to the core offering, and consist of two distinct parts, buildtime augmentation and runtime container. The augmentation part is responsible for all metadata processing, such as reading annotations, XML descriptors etc. The output of this augmentation phase is recorded bytecode which is responsible for directly instantiating the relevant runtime services.

This means that metadata is only processed once at build time, which both saves on startup time, and also on memory usage as the classes etc that are used for processing are not loaded (or even present) in the runtime JVM.

<a href="{{site.baseurl}}/guides/extension-authors-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Measuring Performance

When measuring the footprint of a Protean application, we measure Resident Set Size (RSS) and not the JVM heap size which is only a small part of the overall problem. The JVM not only allocates native memory for heap (-Xms, -Xmx) but also structures required by the jvm to run your application.

<a href="{{site.baseurl}}/guides/performance-measure" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Contexts and Dependency Injection

Protean DI solution is based on the [Contexts and Dependency Injection for Java 2.0](https://docs.jboss.org/cdi/spec/2.0/cdi-spec) specification. However, it is not a full CDI implementation verified by the TCK. Only a subset of the CDI features is implemented - see also the list of supported features and the list of limitations.

<a href="{{site.baseurl}}/guides/cdi-reference" class="button-cta secondary">READ THE GUIDE</a>
</div>
</div>
