---
date: 2021-10-27
---
## MicroProfile Config

Injection points targeting beans annotated with `org.eclipse.microprofile.config.inject.ConfigProperties`, require to be annotated with the `org.eclipse.microprofile.config.inject.ConfigProperties` qualifier as stated in the MicroProfile specification. Previous Quarkus versions were only requiring a single `@Inject`. 

## Hibernate Reactive API changes

This version of Quarkus updated to Hibernate Reactive `1.0.0.CR10`, in which a frequently used API changed signature;

to open a new `Mutiny`.`Session` from a `Mutiny`.`SessionFactory` you now have:

* `Uni<Session> openSession();`
* `Uni<Session> openSession(String tenantId);`
* `Uni<StatelessSession> openStatelessSession();`
* `Uni<StatelessSession> openStatelessSession(String tenantId);`

N.B. the difference: they no longer return a `Mutiny.Session` directly but a Uni of such object.

We considered giving these a new name, and deprecate the existing name, but since it's an experimental component and we really like this name we decided breaking the API was the better choice, for the long term benefits.

A similar change was applied to the `Stage`.`SessionFactory`: all methods opening a `Stage`.`Session` are now returning a `CompletionStage<Session>`.

## gRPC Decoupled from HTTP

In previous versions of Quarkus adding the gRPC extension would also add the `quarkus-vertx-http` extension that would open a HTTP socket on port 8080 (as well as gRPC opening a port on 9000). This is no longer the case so if you wish to also serve content on port 8080 then you will need to install a HTTP extension such as `quarkus-vertx-http` or `quarkus-resteasy-reactive`.