---
layout: faq
title: FAQ
permalink: /faq/
---

## What is your license?

Quarkus is an Open Source project licensed under the [Apache License version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

## Where can I get it?

Quarkus is published in Maven Central, check out [which extensions](/extensions) you need and just import them in your `pom.xml` to get Quarkus.
We recommend you start your Quarkus experience via our [Getting Started guides)(/get-started).

## Quarkus is beta?

Yes, we consider Quarkus beta.
However 95% of the features Quarkus apps use are provided by the ecosystem like Hibernate ORM, Eclipse Vert.x, Netty, RESTEasy, etc.
These libraries are rock solid :)

## What is a Quarkus extension?

One of the main goals of Quarkus is ease of extensibility and to build a vibrant ecosystem.

Think of Quarkus extensions as your project dependencies. Extensions configure, boot and integrate a framework or technology into your Quarkus application. They also do all of the heavy lifting of providing the right information to GraalVM for your application to compile natively.
This will allow 3rd party projects to easily take advantage of the work we have done to make it easier to target GraalVM.

We are also aiming to provide a support for a full [extension ecosystem](/extensions), to make it easy to discover and consume
3rd party extensions, as well as providing easier version management.

## What is GraalVM?

[GraalVM](https://www.graalvm.org) is a universal virtual machine for running applications written in
various different languages, as well as providing the ability to compile JVM bytecode to a native executable (this
native executable runs a special virtual machine called SubstrateVM). These native executable's start much faster
and can use a lot less memory that a traditional JVM, however not every JVM feature is supported, and some are more
limited than normal.

For example by default reflection in GraalVM will not work, unless a class/member has been explicitly registered for
reflection. This is normally achieved by listing every class, method, field and constructor in a JSON file, and passing
this as a parameter into the native image build. This obviously gets quite cumbersome for all but the most trivial projects.
Quarkus provides a framework that makes it easy to work around these annotations, and programmatically determine what should
be registered.

## How do you unify imperative and reactive programming?

[Learn more](/vision/continuum).

## What does Container First mean?

[Learn more](/vision/container-first).

## What is your view on standards?

[Learn more](/vision/standards).

## What are you doing to improve developer joy?

[Learn more](/vision/developer-joy).
