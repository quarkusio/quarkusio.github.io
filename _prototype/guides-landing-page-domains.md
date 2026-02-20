# Proposed Domain Categories with JTBD Job Statements

This document is a companion to [guides-landing-page-plan.md](guides-landing-page-plan.md). It contains the proposed 15-domain taxonomy with full guide mapping tables, JTBD job statements, learning paths, and scope notes.

The 15 domains below are a proposed starting point, not a final taxonomy. The guide assignments and topic cluster ordering within each domain are initial recommendations based on content analysis. Step 1 of the implementation roadmap includes reviewing and refining each domain with subject matter experts — extension maintainers, documentation maintainers, and stakeholders — before the taxonomy is committed to `domains.yaml`.

For a compact overview of all 15 domains, see the [Guide Distribution Summary](guides-landing-page-plan.md#guide-distribution-summary) in the main plan.

## Cross-reference policy

Three guides appear in two domains. Each has one **primary domain** (where it lives in the sidebar and counts toward the guide total) and one **secondary domain** (where it appears as a "See also" link):

| Guide | Primary Domain | Secondary Domain |
|---|---|---|
| Use the Quarkus CLI | Get Started | Use Build Tools |
| Protect secrets in configuration | Secure Your Application | Understand the Runtime |
| Write native-compatible code | Deploy to the Cloud | Write Extensions |

In `domains.yaml`, secondary entries use an `also-in` flag so the sidebar and landing page can render them as cross-references rather than full entries.

## Learning path policy

Domains with more than 25 guides include a recommended learning path on the domain page. The learning path is a curated sequence of 4-6 guides that gives a newcomer a clear reading order through the domain's most important concepts. Domains below this threshold have few enough guides that the topic-clustered ordering is sufficient.

## 1. Get Started

> "When I'm evaluating Quarkus or starting a new project, I want to create a working application quickly, so I can understand the developer experience and decide if Quarkus fits my needs."

| Current Title | JTBD Title | Type |
|---|---|---|
| Creating Your First Application | Create your first application | tutorial |
| Your second Quarkus application | Build your second application with Dev Services | tutorial |
| Getting Started With Reactive | Get started with reactive | tutorial |
| Using our Tooling | Explore the developer toolchain | tutorial |
| Quarkus Tools in your favorite IDE | Set up IDE tooling | tutorial |
| Building Quarkus apps with Quarkus Command Line Interface (CLI) | Use the Quarkus CLI | howto |
| Configuring Your Application | Configure your application | howto |
| Configuration Reference Guide | Configuration properties | reference |
| Mapping configuration to objects | Map configuration to objects | howto |
| YAML configuration | Use YAML configuration | howto |
| Testing Your Application | Test your application | howto |
| Continuous Testing | Use continuous testing | howto |
| Using Kotlin | Use Kotlin | howto |

**Guide count**: 13

---

## 2. Build Backend APIs

> "When I need to expose HTTP endpoints, consume external APIs, or build service-to-service communication, I want to create and configure API services efficiently, so I can build reliable backends and integrations."

> **Learning path**: Write REST services with Quarkus REST → Use the REST Client → Validate input with Hibernate Validator → Generate OpenAPI docs and use Swagger UI → Build resilient services with fault tolerance. Start with building and consuming REST APIs, then add validation and documentation, then fault tolerance for production readiness.

*Scope note*: This domain covers all backend API protocols — REST, gRPC, and GraphQL — because users think "build an API," not "do gRPC." The domain name uses "Backend APIs" rather than "REST APIs" to reflect this broader boundary and to distinguish it clearly from Build Web UIs (browser-facing).

| Current Title | JTBD Title | Type |
|---|---|---|
| Writing REST Services with Quarkus REST | Write REST services with Quarkus REST | howto |
| Writing JSON REST Services | Write JSON REST services | howto |
| Using the REST Client | Use the REST Client | howto |
| Using OpenAPI and Swagger UI | Generate OpenAPI docs and use Swagger UI | howto |
| Using Reactive Routes | Use reactive routes | howto |
| Validation with Hibernate Validator | Validate input with Hibernate Validator | howto |
| SmallRye Fault Tolerance | Build resilient services with fault tolerance | howto |
| Cross-Site Request Forgery Prevention | Prevent Cross-Site Request Forgery (CSRF) | howto |
| Generating Jakarta REST resources with Panache | Generate REST resources with Panache | howto |
| Use virtual threads in REST applications | Use virtual threads in REST applications | howto |
| Migrating to Quarkus REST | Migrate to Quarkus REST | howto |
| SmallRye GraphQL | Build GraphQL services | howto |
| SmallRye GraphQL Client | Use the GraphQL client | howto |
| gRPC | Understand gRPC in Quarkus | concept |
| Getting Started with gRPC | Get started with gRPC | tutorial |
| Implementing a gRPC Service | Implement a gRPC service | howto |
| Consuming a gRPC Service | Consume a gRPC service | howto |
| Deploying your gRPC Service in Kubernetes | Deploy a gRPC service to Kubernetes | howto |
| Quarkus Virtual Thread support for gRPC services | Use virtual threads with gRPC | howto |
| Using gRPC CLI | Use the gRPC CLI | howto |
| Using xDS gRPC | Use xDS with gRPC | howto |
| HTTP Reference | HTTP configuration | reference |
| TLS registry reference | TLS registry configuration | reference |
| Cross-Origin Resource Sharing (CORS) | CORS configuration | reference |
| Load Shedding reference guide | Load shedding configuration | reference |
| gRPC reference guide | gRPC server and client configuration | reference |
| gRPC code generation reference guide | gRPC code generation configuration | reference |
| RESTEasy Classic | RESTEasy Classic compatibility | reference |
| Using the legacy REST Client | Use the legacy REST Client | howto |
| Using the legacy REST Client with Multipart | Use the legacy REST Client with multipart | howto |
| Web dependency locator | Web dependency locator | reference |

**Guide count**: 31

---

## 3. Build Web UIs

> "When I need to build a full-stack web application with server-side rendering, templating, or real-time WebSocket communication, I want to use Quarkus web UI frameworks, so I can deliver interactive web experiences with Java on the backend."

*Scope note*: This domain is limited to browser-facing UI concerns (Qute templates, WebSockets, web bundler). All HTTP API work lives in Build Backend APIs.

| Current Title | JTBD Title | Type |
|---|---|---|
| Quarkus for the Web | Explore Quarkus web frameworks | concept |
| Qute Templating Engine | Use the Qute templating engine | howto |
| Qute Reference Guide | Qute template language | reference |
| Getting started with WebSockets Next | Build a WebSocket application | tutorial |
| WebSockets Next reference guide | WebSockets Next configuration | reference |
| Using WebSockets with Undertow | Use legacy WebSockets with Undertow | howto |

**Guide count**: 6

> **Note**: This domain will grow as Quarkiverse web frameworks (Renarde, Quinoa, Web Bundler) mature and their guides enter the main documentation set.

---

## 4. Access and Manage Data

> "When I need to store, retrieve, or manage persistent data, I want to connect to databases, use ORM frameworks, and manage schemas, so I can build data-driven applications reliably."

> **Learning path**: Configure data sources → Use Hibernate ORM and Jakarta Persistence → Simplify data access with Panache → Manage schema migrations with Flyway → Cache application data. Start with datasource configuration, then move to ORM, then to the simplified Panache API, then schema management, then caching.

| Current Title | JTBD Title | Type |
|---|---|---|
| Configure data sources in Quarkus | Configure data sources | reference |
| Using Hibernate ORM and Jakarta Persistence | Use Hibernate ORM and Jakarta Persistence | howto |
| Simplified Hibernate ORM with Panache | Simplify data access with Panache | howto |
| Simplified Hibernate ORM with Panache and Kotlin | Use Panache with Kotlin | howto |
| Simplified Hibernate Reactive with Panache | Use Hibernate Reactive with Panache | howto |
| Simplified Hibernate with Panache Next | Use Panache Next | howto |
| Simplified MongoDB with Panache | Use MongoDB with Panache | howto |
| Simplified MongoDB with Panache and Kotlin | Use MongoDB with Panache and Kotlin | howto |
| Using the MongoDB Client | Use the MongoDB client | howto |
| Using Hibernate Reactive | Use Hibernate Reactive | howto |
| Reactive SQL Clients | Use reactive SQL clients | howto |
| Using Flyway | Manage schema migrations with Flyway | howto |
| Using Liquibase | Manage schema migrations with Liquibase | howto |
| Using Liquibase MongoDB | Manage MongoDB migrations with Liquibase | howto |
| Using transactions in Quarkus | Use transactions | reference |
| Application Data Caching | Cache application data | howto |
| Infinispan Cache | Infinispan cache configuration | reference |
| Redis Cache | Redis cache configuration | reference |
| Using the Redis Client | Use the Redis client | howto |
| Redis Extension Reference Guide | Redis client configuration | reference |
| Using the Infinispan Client | Use the Infinispan client | howto |
| Infinispan Client Extension Reference Guide | Infinispan client configuration | reference |
| Connecting to an Elasticsearch cluster | Connect to Elasticsearch | howto |
| Use Hibernate Search with Hibernate ORM and Elasticsearch/OpenSearch | Use Hibernate Search with Hibernate ORM | howto |
| Use Hibernate Search in Standalone mode with Elasticsearch/OpenSearch | Use Hibernate Search in standalone mode | howto |
| Using the Cassandra Client | Use the Cassandra client | howto |
| Using Blaze-Persistence | Use Blaze-Persistence | howto |
| Narayana LRA Participant Support | Coordinate activities with LRA | howto |
| Using Software Transactional Memory in Quarkus | Use Software Transactional Memory | howto |
| Dev Services for Databases | Dev Services for databases | reference |
| Dev Services for Elasticsearch | Dev Services for Elasticsearch | reference |
| Dev Services for Infinispan | Dev Services for Infinispan | reference |
| Dev Services for LRA | Dev Services for LRA | reference |
| Dev Services for MongoDB | Dev Services for MongoDB | reference |
| Dev Services for Redis | Dev Services for Redis | reference |

**Guide count**: 35

---

## 5. Secure Your Application

> "When I need to protect my application from unauthorized access, I want to configure authentication and authorization mechanisms, so I can enforce security policies and protect sensitive resources."

> **Learning path**: This domain is large (40 guides) and requires a curated reading order within the domain page. The recommended path is: Security overview → Security architecture → Authentication mechanisms → (choose a specific mechanism: OIDC, Basic, JWT, etc.) → Authorization → Testing. The domain page should present this path prominently before the full guide listing.

| Current Title | JTBD Title | Type |
|---|---|---|
| Quarkus Security overview | Understand Quarkus Security | concept |
| Quarkus Security architecture | Understand the security architecture | concept |
| Authentication mechanisms in Quarkus | Understand authentication mechanisms | concept |
| Identity providers | Understand identity providers | concept |
| Basic authentication | Understand Basic authentication | concept |
| Proactive authentication | Understand proactive authentication | concept |
| Security vulnerability detection and reporting in Quarkus | Understand security vulnerability detection | concept |
| Getting started with security by using Basic authentication and Jakarta Persistence | Build a secure application with Basic authentication | tutorial |
| Protect a service application by using OpenID Connect (OIDC) Bearer token authentication | Protect a service with OIDC Bearer tokens | tutorial |
| Protect a web application by using OpenID Connect (OIDC) authorization code flow | Protect a web application with OIDC authorization code flow | tutorial |
| Protect a Quarkus web application by using an Auth0 OpenID Connect provider | Protect a web application with Auth0 | tutorial |
| OpenID Connect client and token propagation quickstart | Propagate OIDC tokens between services | tutorial |
| Migrate from Vert.x OIDC to Quarkus OIDC | Migrate from Vert.x OIDC to Quarkus OIDC | tutorial |
| OpenID Connect (OIDC) Bearer token authentication | Understand OIDC Bearer token authentication | concept |
| OpenID Connect authorization code flow mechanism for protecting web applications | Understand OIDC authorization code flow | concept |
| Configuring Well-Known OpenID Connect Providers | Configure well-known OIDC providers | howto |
| Quarkus OpenId Connect (OIDC) Expanded Configuration Reference | OIDC expanded configuration | concept |
| Enable Basic authentication | Enable Basic authentication | howto |
| Quarkus Security with Jakarta Persistence | Configure security with Jakarta Persistence | howto |
| Using OpenID Connect (OIDC) and Keycloak to centralize authorization | Centralize authorization with Keycloak | howto |
| Using OpenID Connect (OIDC) multitenancy | Configure OIDC multitenancy | howto |
| Dev Services and Dev UI for OpenID Connect (OIDC) | Use Dev Services for OIDC | howto |
| Authorization of web endpoints | Web endpoint authorization | reference |
| OpenID Connect (OIDC) configuration properties | OIDC configuration properties | reference |
| OpenID Connect (OIDC) and OAuth2 client and filters | OIDC and OAuth2 client configuration | reference |
| OpenID Connect (OIDC) and OAuth2 dynamic client registration | OIDC dynamic client registration | reference |
| Using JWT RBAC | Use JWT role-based access control | howto |
| Build, sign, and encrypt JSON Web Tokens | Build, sign, and encrypt JWTs | howto |
| Using OAuth2 RBAC | Use OAuth2 role-based access control | howto |
| Using Security with JDBC | Configure security with JDBC | howto |
| Using Security with an LDAP Realm | Configure security with LDAP | howto |
| Using Security with WebAuthn | Configure security with WebAuthn | howto |
| Using Security with .properties File | Configure security with a properties file | howto |
| Using a Credentials Provider | Use a credentials provider | howto |
| Using Keycloak Admin Client | Use the Keycloak Admin Client | howto |
| Security Tips and Tricks | Security tips and tricks | howto |
| Security Testing | Test security | howto |
| Secrets in Configuration | Protect secrets in configuration | howto |
| Using SSL With Native Executables | Use SSL with native executables | howto |
| Using Proxy Registry | Use the proxy registry | howto |

**Guide count**: 40

---

## 6. Send and Receive Messages

> "When I need to build event-driven or asynchronous communication between services, I want to produce and consume messages with a broker, so I can decouple my services and handle workloads asynchronously."

| Current Title | JTBD Title | Type |
|---|---|---|
| Quarkus Messaging Extensions | Understand Quarkus Messaging | concept |
| Getting Started to Quarkus Messaging with Apache Kafka | Get started with Kafka messaging | tutorial |
| Getting Started to Quarkus Messaging with AMQP 1.0 | Get started with AMQP 1.0 messaging | tutorial |
| Getting Started to Quarkus Messaging with Apache Pulsar | Get started with Pulsar messaging | tutorial |
| Getting Started to Quarkus Messaging with RabbitMQ | Get started with RabbitMQ messaging | tutorial |
| Apache Kafka Reference Guide | Kafka configuration | reference |
| Apache Pulsar Reference Guide | Pulsar configuration | reference |
| Reactive Messaging AMQP 1.0 Connector Reference Documentation | AMQP 1.0 connector configuration | reference |
| Reactive Messaging RabbitMQ Connector Reference Documentation | RabbitMQ connector configuration | reference |
| Using Apache Kafka Streams | Use Kafka Streams | howto |
| Using Apache Kafka with Schema Registry and Avro | Use Kafka with Schema Registry and Avro | howto |
| Using Apache Kafka with Schema Registry and JSON Schema | Use Kafka with Schema Registry and JSON Schema | howto |
| Kafka Dev UI | Use the Kafka Dev UI | howto |
| Quarkus Virtual Thread support with Reactive Messaging | Use virtual threads with Reactive Messaging | howto |
| Using the event bus | Use the Vert.x event bus | howto |
| Using JMS | Use JMS messaging | howto |
| Dev Services for Kafka | Dev Services for Kafka | reference |
| Dev Services for AMQP | Dev Services for AMQP | reference |
| Dev Services for Pulsar | Dev Services for Pulsar | reference |
| Dev Services for RabbitMQ | Dev Services for RabbitMQ | reference |
| Dev Services for Apicurio Registry | Dev Services for Apicurio Registry | reference |

**Guide count**: 21

---

## 7. Deploy to the Cloud

> "When I need to run my application in production, I want to build optimized artifacts, create container images, and deploy to cloud platforms, so I can ship my application with confidence."

> **Learning path**: Build container images → Deploy to Kubernetes → Build a native executable → Write native-compatible code → Deploy to OpenShift. Start with containerization, then Kubernetes deployment, then native compilation for production optimization.

*Scope note*: Native compilation guides live here because native is a deployment optimization, not a standalone user journey. A developer building a native executable is doing so to deploy it.

| Current Title | JTBD Title | Type |
|---|---|---|
| Building a Native Executable | Build a native executable | tutorial |
| Container Images | Build container images | howto |
| Kubernetes extension | Deploy to Kubernetes | howto |
| Deploying Quarkus applications to OpenShift | Deploy to OpenShift | howto |
| Deploying Quarkus applications to OpenShift in a single step | Deploy to OpenShift in a single step | howto |
| Deploying Quarkus Java applications to OpenShift by using a Docker build strategy | Deploy to OpenShift with Docker | howto |
| Deploying Quarkus applications compiled to native executables | Deploy native executables to OpenShift | howto |
| Using S2I to deploy Quarkus applications to OpenShift | Deploy to OpenShift with S2I | howto |
| Deploying to Google Cloud Platform (GCP) | Deploy to Google Cloud | howto |
| Deploying to Microsoft Azure Cloud | Deploy to Azure | howto |
| Deploying to Heroku | Deploy to Heroku | howto |
| AWS Lambda | Deploy to AWS Lambda | howto |
| AWS Lambda SnapStart Configuration | Configure AWS Lambda SnapStart | howto |
| AWS Lambda with Quarkus REST, Undertow, or Reactive Routes | Deploy REST services to AWS Lambda | howto |
| Azure Functions | Deploy Azure Functions | howto |
| Azure Functions with Quarkus REST, Undertow, or Reactive Routes | Deploy REST services to Azure Functions | howto |
| Google Cloud Functions (Serverless) | Deploy Google Cloud Functions | howto |
| Google Cloud Functions (Serverless) with Quarkus REST, Undertow, or Reactive Routes | Deploy REST services to Google Cloud Functions | howto |
| Funqy | Understand the Funqy serverless framework | concept |
| Funqy AWS Lambda Binding | Use Funqy with AWS Lambda | howto |
| Funqy Google Cloud Functions | Use Funqy with Google Cloud Functions | howto |
| Funqy HTTP Binding (Standalone) | Use Funqy HTTP binding | howto |
| Funqy HTTP Binding with AWS Lambda | Use Funqy HTTP with AWS Lambda | howto |
| Funqy HTTP Binding with Azure Functions | Use Funqy HTTP with Azure Functions | howto |
| Funqy HTTP Binding with Google Cloud Functions | Use Funqy HTTP with Google Cloud Functions | howto |
| Funqy Knative Events Binding | Use Funqy with Knative Events | howto |
| Kubernetes Client | Use the Kubernetes Client | howto |
| Kubernetes Config | Read configuration from Kubernetes ConfigMaps | howto |
| Getting Started with SmallRye Stork | Discover services with SmallRye Stork | howto |
| Stork Reference Guide | Stork configuration | reference |
| Using Stork with Kubernetes | Use Stork with Kubernetes | howto |
| Dev Services for Kubernetes | Dev Services for Kubernetes | reference |
| Initialization tasks | Configure initialization tasks | howto |
| Automate Quarkus deployment with Ansible | Automate deployment with Ansible | howto |
| AppCDS | Optimize startup with AppCDS | howto |
| Native Reference Guide | Native compilation configuration | reference |
| Tips for writing native applications | Write native-compatible code | howto |
| Compressing native executables using UPX | Compress native executables with UPX | howto |

**Guide count**: 38

---

## 8. Observe Your Application

> "When I need to monitor, troubleshoot, or understand the runtime behavior of my application, I want to collect and export metrics, traces, and logs, so I can detect issues early and maintain service reliability."

| Current Title | JTBD Title | Type |
|---|---|---|
| Observability in Quarkus | Understand observability in Quarkus | reference |
| Collect metrics using Micrometer | Collect metrics with Micrometer | tutorial |
| Migrate from OpenTracing to OpenTelemetry tracing | Migrate from OpenTracing to OpenTelemetry | tutorial |
| SmallRye Health | Check application health | howto |
| Using OpenTelemetry Tracing | Use OpenTelemetry tracing | howto |
| Using OpenTelemetry Metrics | Use OpenTelemetry metrics | howto |
| Using OpenTelemetry Logging | Use OpenTelemetry logging | howto |
| Using OpenTelemetry | OpenTelemetry configuration | reference |
| Micrometer Metrics | Micrometer metrics configuration | reference |
| Micrometer and OpenTelemetry extension | Micrometer-to-OpenTelemetry bridge configuration | reference |
| Logging configuration | Logging configuration | reference |
| Centralized log management (Graylog, Logstash, Fluentd) | Centralize logs with Graylog, Logstash, or Fluentd | howto |
| Using JDK Flight Recorder | Use JDK Flight Recorder | howto |
| Management interface reference | Management interface configuration | reference |
| Observability Dev Services | Dev Services for observability | reference |
| Observability Dev Services with Grafana OTel LGTM | Use Grafana OTel LGTM Dev Services | howto |

**Guide count**: 16

---

## 9. Understand the Runtime

> "When I need to understand how Quarkus works under the hood -- dependency injection, the reactive engine, virtual threads, or the application lifecycle -- I want clear explanations and configuration guidance, so I can make informed architectural decisions."

*Scope note*: This domain and Automate and Integrate were split from a single "Core Framework" domain that had ~31 guides. The split separates "how Quarkus works" (CDI, reactive engine, lifecycle) from "do things beyond HTTP" (scheduling, mailer, integrations).

| Current Title | JTBD Title | Type |
|---|---|---|
| Introduction to Contexts and Dependency Injection (CDI) | Understand CDI and dependency injection | concept |
| Contexts and Dependency Injection | CDI configuration | reference |
| Application Initialization and Termination | Manage application lifecycle events | howto |
| Quarkus Reactive Architecture | Understand the reactive architecture | concept |
| Mutiny - Async for mere mortals | Use Mutiny for reactive programming | howto |
| Context Propagation in Quarkus | Propagate context in async code | howto |
| Duplicated context, context locals, asynchronous processing and propagation | Understand duplicated context and propagation | concept |
| Virtual Thread support reference | Virtual threads configuration | reference |
| Extending Configuration Support | Extend configuration support | howto |
| Secrets in Configuration | Protect secrets in configuration | howto |
| Dev Services Overview | Understand Dev Services | concept |
| Compose Dev Services | Configure custom Dev Services with Compose | howto |
| How dev mode differs from a production application | Understand dev mode differences | concept |
| Using Eclipse Vert.x API from a Quarkus Application | Use the Vert.x API | howto |
| Vert.x Reference Guide | Vert.x configuration | reference |
| Class Loading Reference | Class loading | reference |
| Command Mode Applications | Command mode applications | reference |
| Platform | Understand Quarkus platforms | concept |
| Quarkus Extension Registry | Understand the extension registry | concept |
| Quarkus Base Runtime Image | Quarkus base runtime image | reference |
| Config Reference Guide | Configuration reference | reference |
| Testing components | Test individual components | howto |

**Guide count**: 22

---

## 10. Automate and Integrate

> "When I need to schedule recurring tasks, send email, apply business rules, or integrate with external systems beyond HTTP and messaging, I want purpose-built extensions, so I can add these capabilities without writing integration code from scratch."

| Current Title | JTBD Title | Type |
|---|---|---|
| Scheduling Periodic Tasks | Schedule periodic tasks | howto |
| Scheduling Periodic Tasks with Quartz | Schedule tasks with Quartz | howto |
| Scheduler Reference Guide | Scheduler configuration | reference |
| Sending emails using SMTP | Send email with SMTP | howto |
| Mailer Reference Guide | Mailer configuration | reference |
| Defining and executing business rules with Drools | Define business rules with Drools | howto |
| Apache Camel on Quarkus | Integrate with Apache Camel | howto |
| Command Mode with Picocli | Build CLI applications with Picocli | howto |
| Scripting with Quarkus | Script with JBang | howto |

**Guide count**: 9

---

## 11. Use Build Tools

> "When I need to build, package, test, or manage my project, I want to use standard build tools with Quarkus-specific optimizations, so I can integrate Quarkus into my existing development workflow."

| Current Title | JTBD Title | Type |
|---|---|---|
| Quarkus and Maven | Build with Maven | howto |
| Quarkus and Gradle | Build with Gradle | howto |
| Quarkus Maven Plugin | Quarkus Maven Plugin | reference |
| Building Quarkus apps with Quarkus Command Line Interface (CLI) | Use the Quarkus CLI | howto |
| Generating CycloneDX BOMs | Generate CycloneDX SBOMs | howto |
| Packaging And Releasing With JReleaser | Package and release with JReleaser | howto |
| Using Podman with Quarkus | Use Podman with Quarkus | howto |
| Re-augment a Quarkus Application | Re-augment an application | howto |
| Measuring Performance | Measure performance | howto |
| Build analytics | Understand build analytics | concept |
| All configuration options | All configuration properties | reference |
| Update projects to the latest Quarkus version | Update to the latest Quarkus version | howto |
| Measuring the coverage of your tests | Measure test coverage | howto |

**Guide count**: 13

---

## 12. Use Spring APIs

> "When I'm moving an existing Spring Boot application to Quarkus or prefer Spring-style annotations, I want to use familiar Spring APIs as a bridge, so I can migrate incrementally without rewriting everything at once."

*Scope note*: Named "Use Spring APIs" rather than "Migrate from Spring" to cover both migration scenarios and teams that simply prefer Spring-style annotations.

| Current Title | JTBD Title | Type |
|---|---|---|
| Quarkus Extension for Spring DI API | Use Spring DI annotations | howto |
| Quarkus Extension for Spring Web API | Use Spring Web annotations | howto |
| Extension for Spring Data API | Use Spring Data JPA | howto |
| Extension for Spring Data REST | Use Spring Data REST | howto |
| Quarkus Extension for Spring Security API | Use Spring Security annotations | howto |
| Quarkus Extension for Spring Cache API | Use Spring Cache annotations | howto |
| Quarkus Extension for Spring Scheduling API | Use Spring Scheduling annotations | howto |
| Accessing application properties with Spring Boot properties API | Use Spring Boot @ConfigurationProperties | howto |
| Reading properties from Spring Cloud Config Server | Read from Spring Cloud Config Server | howto |

**Guide count**: 9

---

## 13. Write Extensions

> "When I need to extend Quarkus with custom build-time optimizations or integrate a new library, I want to build and package a Quarkus extension, so I can contribute reusable functionality to the ecosystem."

| Current Title | JTBD Title | Type |
|---|---|---|
| Writing Your Own Extension | Write an extension | howto |
| Building my first extension | Build your first extension | tutorial |
| A maturity matrix for Quarkus extensions | Understand extension maturity levels | concept |
| Frequently asked questions about writing extensions | Extension development FAQ | reference |
| Extension Capabilities | Understand extension capabilities | concept |
| Extension codestart | Create an extension codestart | howto |
| Quarkus Extension Metadata | Extension metadata format | reference |
| Conditional Extension Dependencies | Configure conditional extension dependencies | howto |
| CDI Integration Guide | Integrate with the CDI container | howto |
| Writing a Dev Service | Write a Dev Service | howto |
| Dev UI | Extend the Dev UI | howto |
| Dev Assistant | Use the Dev Assistant | howto |
| Dev MCP | Use Dev MCP | howto |
| Build Items | Build items catalog | reference |
| Tips for writing native applications | Write native-compatible code | howto |

**Guide count**: 15

---

## 14. Contribute to Quarkus Docs

> "When I want to contribute documentation to the Quarkus project, I want to understand the content standards and workflow, so I can write content that meets the project's quality bar."

| Current Title | JTBD Title | Type |
|---|---|---|
| Quarkus documentation content types | Understand documentation content types | concept |
| Quarkus style and content guidelines | Documentation style guide | reference |
| Contribute to Quarkus documentation | Contribute documentation | howto |
| Creating a tutorial | Create a tutorial | tutorial |

**Guide count**: 4

---

## 15. Build AI Applications

> "When I need to integrate large language models, RAG pipelines, or AI services into my application, I want to use Quarkus AI extensions, so I can build intelligent applications with Java."

*No guides exist in the main repository yet.* This domain is a placeholder that reflects the growing `quarkus-langchain4j` extension ecosystem and the prominent AI section already present in the quarkus.io navigation (AI Overview, Java for AI, Quarkus for AI, AI Blueprints).

**Expected future guides**:

| Expected Title | Type |
|---|---|
| Get started with LangChain4j | tutorial |
| Use chat models and prompt templates | howto |
| Build a RAG pipeline | tutorial |
| Configure embedding stores | howto |
| Use AI services with CDI | howto |
| LangChain4j configuration | reference |

**Guide count**: 0 (placeholder)
