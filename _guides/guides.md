---
layout: guides-index
title: Quarkus - Guides
permalink: /guides/
---

<div class="grid__item width-4-12 hide-mobile toc" markdown="1">
## Table of Contents

### Core

- [Configuring Your Application]({{site.baseurl}}/guides/application-configuration-guide)
- [Application Initialization and Termination]({{site.baseurl}}/guides/application-lifecycle-events-guide)
- [Contexts and Dependency Injection]({{site.baseurl}}/guides/cdi-reference)
- [Testing Your Application]({{site.baseurl}}/guides/getting-started-testing)
- [Configuring Logging]({{site.baseurl}}/guides/logging-guide)
- [Using SSL With Native Images]({{site.baseurl}}/guides/native-and-ssl-guide)

### Data

- [Using Hibernate ORM and JPA]({{site.baseurl}}/guides/hibernate-orm-guide)
- [Simplified Hibernate ORM with Panache]({{site.baseurl}}/guides/hibernate-orm-panache-guide)
- [Using Infinispan Client]({{site.baseurl}}/guides/infinispan-client-guide)
- [Using Transactions]({{site.baseurl}}/guides/transaction-guide)
- [Validation with Hibernate Validator]({{site.baseurl}}/guides/validation-guide)

### Web

- [Validation with Hibernate Validator]({{site.baseurl}}/guides/validation-guide)
- [Using the REST Client]({{site.baseurl}}/guides/rest-client-guide)
- [Writing REST JSON Services]({{site.baseurl}}/guides/rest-json-guide)
- [Using JWT RBAC]({{site.baseurl}}/guides/jwt-guide)
- [Using WebSockets]({{site.baseurl}}/guides/websocket-guide)

### Security

- [Using Security]({{site.baseurl}}/guides/security-guide)
- [Using JWT RBAC]({{site.baseurl}}/guides/jwt-guide)

### Cloud

- [Deploying Knative Application to Kubernetes or OpenShift]({{site.baseurl}}/guides/getting-started-knative-guide)
- [Deploying Applications on Kubernetes]({{site.baseurl}}/guides/kubernetes-guide)

### Observability

- [Using OpenTracing]({{site.baseurl}}/guides/opentracing-guide)

### Serialization

- [Writing REST JSON Services]({{site.baseurl}}/guides/rest-json-guide)

### Tooling

- [Building Applications with Maven]({{site.baseurl}}/guides/maven-tooling)
- [Building Applications with Gradle]({{site.baseurl}}/guides/gradle-tooling)

### Migration

- [Using our Spring DI compatibility layer]({{site.baseurl}}/guides/spring-di-guide)

### Writing Extensions

- [Writing Your Own Extension]({{site.baseurl}}/guides/extension-authors-guide)

### Alternative Languages

- [Using Kotlin]({{site.baseurl}}/guides/kotlin)

### Miscellaneous

- [Measuring Performance]({{site.baseurl}}/guides/performance-measure)
- [Scheduling Periodic Tasks]({{site.baseurl}}/guides/scheduled-guide)
</div>
<div class="grid__item width-8-12 width-12-12-m guides-content">
<div markdown="1">
You have gone through the [Getting Started]({{site.baseurl}}/get-started) guides.
You are now ready to dive into specific topic guides.
</div>
<div class="guide-item" markdown="1">
## Configuring Your Application

Hardcoded values in your code is a no go (even if we all did it at some point ;-)). In this guide, we learn how to configure your application.

<a href="{{site.baseurl}}/guides/application-configuration-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Application Initialization and Termination

You often need to execute custom actions when the application starts and clean up everything when the application stops. This guide explains how to be notified when an application stops or starts.

<a href="{{site.baseurl}}/guides/application-lifecycle-events-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Contexts and Dependency Injection

Quarkus DI solution is based on the [Contexts and Dependency Injection for Java 2.0](https://docs.jboss.org/cdi/spec/2.0/cdi-spec) specification. However, it is not a full CDI implementation verified by the TCK. Only a subset of the CDI features is implemented - see also the list of supported features and the list of limitations.

<a href="{{site.baseurl}}/guides/cdi-reference" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Testing Your Application

This guide covers:

* Testing in JVM mode
* Testing in native mode
* Injection of resources into tests

<a href="{{site.baseurl}}/guides/getting-started-testing" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Configuring Logging

This guide explains logging and how to configure it.

<a href="{{site.baseurl}}/guides/logging-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Using SSL With Native Images

In this guide, we will discuss how you can get your native images to support SSL,
as native images don't support it out of the box.

<a href="{{site.baseurl}}/guides/native-and-ssl-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
</div>
<div class="grid__item width-12-12"><hr></div>
<div class="grid__item width-4-12 width-12-12-m toc" markdown="1">
## Data
</div>
<div class="grid__item width-8-12 width-12-12-m guides-content">
<div class="guide-item" markdown="1">
## Using Hibernate ORM and JPA

Hibernate ORM is the de facto JPA implementation and offers you the full breath of an Object Relational Mapper. It works beautifully in Quarkus.

<a href="{{site.baseurl}}/guides/hibernate-orm-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Hibernate ORM and JPA with Panache

Hibernate ORM is the de facto JPA implementation and offers you the full breadth of an Object Relational Mapper.
It makes complex mappings possible, but it does not make simple and common mappings trivial.
Panache focuses on making your entities trivial and fun to write.

<a href="{{site.baseurl}}/guides/hibernate-orm-panache-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Using Infinispan Client

Infinispan is an in memory data grid that allows running in a server outside of application processes. This extension provides functionality to allow the client that can connect to said server when running in Quarkus.

<a href="{{site.baseurl}}/guides/infinispan-client-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Using Transactions

Quarkus comes with a Transaction Manager and uses it to coordinate and expose transactions to your applications.
Each extension dealing with persistence will integrate with it for you.
And you will explicitly interact with transactions via CDI.
This guide will walk you through all that.

<a href="{{site.baseurl}}/guides/transaction-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Validation with Hibernate Validator

This guide covers how to use Hibernate Validator/Bean Validation in your REST services.

<a href="{{site.baseurl}}/guides/validation-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
</div>
<div class="grid__item width-12-12"><hr></div>
<div class="grid__item width-4-12 width-12-12-m toc" markdown="1">
## Web
</div>
<div class="grid__item width-8-12 width-12-12-m guides-content">
<div class="guide-item" markdown="1">
## Validation with Hibernate Validator

This guide covers how to use Hibernate Validator/Bean Validation in your REST services.

<a href="{{site.baseurl}}/guides/validation-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Using the REST Client

This guide explains how to use the MicroProfile REST Client in order to interact with REST APIs
with very little effort.

<a href="{{site.baseurl}}/guides/rest-client-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Writing JSON REST Services

JSON is now the lingua franca between microservices. In this guide, we see how you can get your REST services to consume and produce JSON payloads.

<a href="{{site.baseurl}}/guides/rest-json-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Using JWT RBAC

This guide explains how your application can utilize {mp-jwt} to provide
secured access to the JAX-RS endpoints.

<a href="{{site.baseurl}}/guides/jwt-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Using WebSockets

This guide explains how your Quarkus application can utilize web sockets to create interactive web applications. Because it’s the canonical web socket application, we are going to create a simple chat application.

<a href="{{site.baseurl}}/guides/websocket-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
</div>
<div class="grid__item width-12-12"><hr></div>
<div class="grid__item width-4-12 width-12-12-m toc" markdown="1">
## Security
</div>
<div class="grid__item width-8-12 width-12-12-m guides-content">
<div class="guide-item" markdown="1">
## Using Security

This quide demonstrates integration with the Elytron security subsystem to allow for RBAC based on the
common security annotations `@RolesAllowed`, `@DenyAll`, `@PermitAll` on REST endpoints.

<a href="{{site.baseurl}}/guides/security-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Using JWT RBAC

This guide explains how your application can utilize {mp-jwt} to provide
secured access to the JAX-RS endpoints.

<a href="{{site.baseurl}}/guides/jwt-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
</div>
<div class="grid__item width-12-12"><hr></div>
<div class="grid__item width-4-12 width-12-12-m toc" markdown="1">
## Cloud
</div>
<div class="grid__item width-8-12 width-12-12-m guides-content">
<div class="guide-item" markdown="1">
## Deploying Knative Application to Kubernetes or OpenShift

This guide covers:

* The deployment of the application to Kubernetes

<a href="{{site.baseurl}}/guides/getting-started-knative-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Deploying Applications on Kubernetes

This guide covers:

* The deployment of the application to Kubernetes
* The deployment of the application to OpenShift

<a href="{{site.baseurl}}/guides/kubernetes-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
</div>
<div class="grid__item width-12-12"><hr></div>
<div class="grid__item width-4-12 width-12-12-m toc" markdown="1">
## Observability
</div>
<div class="grid__item width-8-12 width-12-12-m guides-content">
<div class="guide-item" markdown="1">
## Using OpenTracing

This guide explains how your Quarkus application can utilize OpenTracing to provide distributed tracing for interactive web applications.

<a href="{{site.baseurl}}/guides/opentracing-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
</div>
<div class="grid__item width-12-12"><hr></div>
<div class="grid__item width-4-12 width-12-12-m toc" markdown="1">
## Serialization
</div>
<div class="grid__item width-8-12 width-12-12-m guides-content">
<div class="guide-item" markdown="1">
## Writing JSON REST Services

JSON is now the lingua franca between microservices. In this guide, we see how you can get your REST services to consume and produce JSON payloads.

<a href="{{site.baseurl}}/guides/rest-json-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
</div>
<div class="grid__item width-12-12"><hr></div>
<div class="grid__item width-4-12 width-12-12-m toc" markdown="1">
## Tooling
</div>
<div class="grid__item width-8-12 width-12-12-m guides-content">
<div class="guide-item" markdown="1">
## Building Applications with Maven

This guide covers:

* Maven configuration
* Creating a new project
* Dealing with extensions
* Development mode
* Debugging
* Import in your IDE
* Building a native image
* Build a container friendly executable

<a href="{{site.baseurl}}/guides/maven-tooling" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Building Applications with Gradle

This guide covers:

* Maven configuration
* Creating a new project
* Dealing with extensions
* Development mode
* Debugging
* Import in your IDE
* Building a native image
* Build a container friendly executable

<a href="{{site.baseurl}}/guides/gradle-tooling" class="button-cta secondary">READ THE GUIDE</a>
</div>
</div>
<div class="grid__item width-12-12"><hr></div>
<div class="grid__item width-4-12 width-12-12-m toc" markdown="1">
## Migration
</div>
<div class="grid__item width-8-12 width-12-12-m guides-content">
<div class="guide-item" markdown="1">
## Using our Spring Dependency Injection compatibility layer

While you are encouraged to use CDI annotations for injection, Quarkus provides a compatibility layer for Spring dependency injection in the form of the spring-di extension.

<a href="{{site.baseurl}}/guides/spring-di-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
</div>
<div class="grid__item width-12-12"><hr></div>
<div class="grid__item width-4-12 width-12-12-m toc" markdown="1">
## Writing Extensions
</div>
<div class="grid__item width-8-12 width-12-12-m guides-content">
<div class="guide-item" markdown="1">
## Write Your Own Extension

Quarkus extensions add a new developer focused behavior to the core offering, and consist of two distinct parts, buildtime augmentation and runtime container. The augmentation part is responsible for all metadata processing, such as reading annotations, XML descriptors etc. The output of this augmentation phase is recorded bytecode which is responsible for directly instantiating the relevant runtime services.

This means that metadata is only processed once at build time, which both saves on startup time, and also on memory usage as the classes etc that are used for processing are not loaded (or even present) in the runtime JVM.

<a href="{{site.baseurl}}/guides/extension-authors-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
</div>
<div class="grid__item width-12-12"><hr></div>
<div class="grid__item width-4-12 width-12-12-m toc" markdown="1">
## Alternative Languages
</div>
<div class="grid__item width-8-12 width-12-12-m guides-content">
<div class="guide-item" markdown="1">
## Using Kotlin

This guide explains how to use Kotlin.

<a href="{{site.baseurl}}/guides/kotlin" class="button-cta secondary">READ THE GUIDE</a>
</div>
</div>
<div class="grid__item width-12-12"><hr></div>
<div class="grid__item width-4-12 width-12-12-m toc" markdown="1">
## Miscellaneous
</div>
<div class="grid__item width-8-12 width-12-12-m guides-content">
<div class="guide-item" markdown="1">
## Measuring Performance

When measuring the footprint of a Quarkus application, we measure Resident Set Size (RSS) and not the JVM heap size which is only a small part of the overall problem. The JVM not only allocates native memory for heap (-Xms, -Xmx) but also structures required by the jvm to run your application.

<a href="{{site.baseurl}}/guides/performance-measure" class="button-cta secondary">READ THE GUIDE</a>
</div>
<div class="guide-item" markdown="1">
## Scheduling Periodic Tasks

Modern applications often need to run specific tasks periodically. In this guide, you learn how to schedule periodic tasks.

<a href="{{site.baseurl}}/guides/scheduled-guide" class="button-cta secondary">READ THE GUIDE</a>
</div>
</div>

