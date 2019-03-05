---
layout: guides-index
title: guides
permalink: /guides/
---

<div class="grid__item width-4-12 hide-mobile toc" markdown="1">
## Table of Contents
### Guides & Documentation

- [Configuring Your Application](/guides/application-configuration-guide)
- [Application Initialization and Termination](/guides/application-lifecycle-events-guide)
- [Using JSON and Validation](/guides/json-and-validation-guide)
- [Scheduling Periodic Tasks](/guides/scheduled-guide)
- [Using Websockets](/guides/websocket-guide)
- [Using Hibernate ORM](/guides/hibernate-orm-guide)
- [Using OpenTracing](/guides/opentracing-guide)


### Advanced Guides

- [Writing Your Own Extension](/guides/extension-authors-guide)
- [Measuring Performance](/guides/performance-measure)
- [Contexts and Dependency Injection](/guides/cdi-reference)

## Additional Resources
### Getting Started

- [Getting Started](/guides/getting-started-guide)
- [Building Native Images](/guides/building-native-image-guide)
- [Deploying Applications on Kubernetes](/guides/kubernetes-guide)
- [Project Scaffolding and IDE](/guides/ide-configuration)

</div>
<div class="grid__item width-8-12 width-12-12-m guides-content">
<div class="guide-item" markdown="1">
## Configuring Your Application

Hardcoded values in your code is a no go (even if we all did it at some point ;-)). In this guide, we learn how to configure your application.

<a href="/guides/application-configuration-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Application Initialization and Termination

You often need to execute custom actions when the application starts and clean up everything when the application stops. This guide explains how to be notified when an application stops or starts.

<a href="/guides/application-lifecycle-events-guid" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Using JSON and Validation

JSON is now the lingua franca between microservices. In this guide, we see how you can consume and produce JSON payloads. The guide also covers how to validate JSON payloads.

<a href="/guides/json-and-validation-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Scheduling Periodic Tasks

Modern applications often need to run specific tasks periodically. In this guide, you learn how to schedule periodic tasks.

<a href="/guides/scheduled-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Using Websockets

This guide explains how your Shamrock application can utilize web sockets to create interactive web applications. Because it’s the canonical web socket application, we are going to create a simple chat application.

<a href="/guides/websocket-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Using Hibernate ORM

Hibernate ORM is the de facto JPA implementation and offers you the full breath of an Object Relational Mapper. It works beautifully in Quarkus.

<a href="/guides/hibernate-orm-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Using OpenTracing

This guide explains how your Quarkus application can utilize opentracing to provide distributed tracing for interactive web applications.

<a href="/guides/opentracing-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Using Infinispan Client

Infinispan is an in memory data grid that allows running in a server outside of application processes. This extension provides functionality to allow the client that can connect to said server when running in Protean.

<a href="/guides/infinispan-client-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Using our Spring Dependency Injection compatibility layer

While you are encouraged to use CDI annotations for injection, Protean provides a compatibility layer for Spring dependency injection in the form of the spring-di extension.

<a href="/guides/using-our-spring-dependency-injection-compatibility-layer" class="button-cta secondary">READ THE GUIDE</a>
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

<a href="/guides/extension-authors-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Measuring Performance

When measuring the footprint of a Protean application, we measure Resident Set Size (RSS) and not the JVM heap size which is only a small part of the overall problem. The JVM not only allocates native memory for heap (-Xms, -Xmx) but also structures required by the jvm to run your application.

<a href="/guides/performance-measure" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Contexts and Dependency Injection

Protean DI solution is based on the [Contexts and Dependency Injection for Java 2.0](https://docs.jboss.org/cdi/spec/2.0/cdi-spec) specification. However, it is not a full CDI implementation verified by the TCK. Only a subset of the CDI features is implemented - see also the list of supported features and the list of limitations.

<a href="/guides/cdi-reference" class="button-cta secondary">READ THE GUIDE</a>
</div>
</div>
