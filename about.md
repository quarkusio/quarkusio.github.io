---
layout: base-fullwidthcontent
title: What is Quarkus?
subtitle: Learn what we mean by "Supersonic Subatomic Java".
permalink: /about/
---


Quarkus is a full-stack, Kubernetes-native Java framework made for Java virtual machines (JVMs) and native compilation, optimizing Java specifically for containers and enabling it to become an effective platform for serverless, cloud, and Kubernetes environments.

Quarkus is designed to work with popular Java standards, frameworks, and libraries like Eclipse MicroProfile and Spring (demonstrated together as part of a session in this Red Hat Summit 2020 track), as well as Apache Kafka, RESTEasy (JAX-RS), Hibernate ORM (JPA), Spring, Infinispan, Camel, and many more..

Quarkus’ dependency injection solution is based on CDI (contexts and dependency injection) and includes an extension framework to expand functionality and to configure, boot, and integrate a framework into your application. Adding an extension is as easy as adding a dependency, or you can use Quarkus tooling.

It also provides the correct information to GraalVM (a universal virtual machine for running apps written in a number of languages, including Java and JavaScript) for native compilation of your application.

### Designed for developers

Quarkus was designed to be easy to use right from the start, with features that work well with little to no configuration..

Developers can choose the Java frameworks they want for their applications, which can be run in JVM mode or compiled and run in native mode..

Built with an eye to enjoyability for developers, Quarkus also includes the following capabilities:

*   Live coding so that developers can immediately check the effect of code changes and quickly troubleshoot them
*   Unified imperative and reactive programming with an embedded managed event bus
*   Unified configuration
*   Easy native executable generation

### Container-first

Whether an application is hosted on a public cloud or in an internally hosted Kubernetes cluster, characteristics like fast startup and low memory consumption are important to keeping overall host costs down.

Quarkus was built around a container-first philosophy, meaning it’s optimized for lower memory usage and faster startup times in the following ways:

*   First-class support for Graal/SubstrateVM
*   Build-time metadata processing
*   Reduction in reflection usage
*   Native image preboot

So Quarkus builds applications to consume 1/10th the memory when compared to traditional Java, and has a faster startup time (as much as 300 times faster), both of which greatly reduce the cost of cloud resources.

### Imperative and reactive code

Quarkus is designed to seamlessly combine the familiar imperative style code and the non-blocking, reactive style when developing applications..

This is helpful for both Java developers who are used to working with the imperative model and don’t want to switch things up, and those working with a cloud-native/reactive approach..

The Quarkus development model can adapt itself to whatever app you’re developing..

Quarkus is an effective solution for running Java in this new world of serverless architecture, microservices, containers, Kubernetes, function-as-a-service (FaaS), and cloud because it was created with all these things in mind.