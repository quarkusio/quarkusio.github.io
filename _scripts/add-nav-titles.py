#!/usr/bin/env python3
"""Add JTBD-compliant nav-title fields to domains.yaml.

Rules applied (from /jtbdize skill):
  - Tutorials:  imperative verbs — Build, Create, Protect, Get started
  - How-tos:    imperative verbs — Configure, Enable, Use, Set up, Deploy
  - Guides:     imperative verbs — same as how-to
  - Concepts:   Understand, Learn, Explore
  - References: descriptive, no leading verb; strip "Reference Guide" suffix

General:
  - No gerunds (-ing forms as title start)
  - Strip redundant "Quarkus" prefix
  - Keep titles 3-7 words where possible
  - Focus on user outcomes, not feature names
"""

import yaml
import sys
from pathlib import Path

# ── Explicit nav-title overrides ──────────────────────────────────────────
# key = original title (exact match), value = JTBD nav-title.
# Titles not listed here get mechanical gerund-stripping only.
OVERRIDES = {
    # ── get-started ──
    "Creating Your First Application": "Create your first application",
    "Your second Quarkus application": "Create your second application",
    "Getting Started With Reactive": "Get started with Reactive",
    "Using our Tooling": "Explore the tooling",
    "Quarkus Tools in your favorite IDE": "Set up IDE tools",
    "Building Quarkus apps with Quarkus Command Line Interface (CLI)": "Use the Quarkus CLI",
    "Configuring Your Application": "Configure your application",
    "Configuration Reference Guide": "Configuration reference",
    "Mapping configuration to objects": "Map configuration to objects",
    "YAML configuration": "Use YAML configuration",
    "Testing Your Application": "Test your application",
    "Continuous Testing": "Use continuous testing",
    "Using Kotlin": "Use Kotlin",

    # ── build-backend-apis ──
    "Writing REST Services with Quarkus REST (formerly RESTEasy Reactive)": "Write REST services",
    "Writing JSON REST Services": "Write JSON REST services",
    "Using the REST Client": "Use the REST client",
    "Using OpenAPI and Swagger UI": "Use OpenAPI and Swagger UI",
    "Using Reactive Routes": "Use reactive routes",
    "Validation with Hibernate Validator": "Validate with Hibernate Validator",
    "SmallRye Fault Tolerance": "Use SmallRye Fault Tolerance",
    "Cross-Site Request Forgery Prevention": "Prevent CSRF",
    "Generating Jakarta REST resources with Panache": "Generate REST resources with Panache",
    "Use virtual threads in REST applications": "Use virtual threads in REST",
    "Migrating to Quarkus REST (formerly RESTEasy Reactive)": "Migrate to Quarkus REST",
    "SmallRye GraphQL": "Use SmallRye GraphQL",
    "SmallRye GraphQL Client": "Use the GraphQL client",
    "gRPC": "Use gRPC",
    "Getting Started with gRPC": "Get started with gRPC",
    "Implementing a gRPC Service": "Implement a gRPC service",
    "Consuming a gRPC Service": "Consume a gRPC service",
    "Deploying your gRPC Service in Kubernetes": "Deploy gRPC to Kubernetes",
    "Quarkus Virtual Thread support for gRPC services": "Use virtual threads with gRPC",
    "Using gRPC CLI": "Use the gRPC CLI",
    "Using xDS gRPC": "Use xDS gRPC",
    "HTTP Reference": "HTTP reference",
    "TLS registry reference": "TLS registry reference",
    "Cross-Origin Resource Sharing (CORS)": "CORS reference",
    "Load Shedding reference guide": "Load shedding reference",
    "gRPC reference guide": "gRPC reference",
    "gRPC code generation reference guide": "gRPC code generation reference",
    "RESTEasy Classic": "Use RESTEasy Classic",
    "Using the legacy REST Client": "Use the legacy REST client",
    "Using the legacy REST Client with Multipart": "Use the legacy REST client with multipart",
    "Web dependency locator": "Use the web dependency locator",

    # ── build-web-uis ──
    "Quarkus for the Web": "Build for the web",
    "Qute Templating Engine": "Use the Qute templating engine",
    "Qute Reference Guide": "Qute reference",
    "Getting started with WebSockets Next": "Get started with WebSockets",
    "WebSockets Next reference guide": "WebSockets reference",
    "Using WebSockets with Undertow": "Use WebSockets with Undertow",

    # ── access-and-manage-data ──
    "Configure data sources in Quarkus": "Configure data sources",
    "Using Hibernate ORM and Jakarta Persistence": "Use Hibernate ORM",
    "Simplified Hibernate ORM with Panache": "Use Hibernate ORM with Panache",
    "Simplified Hibernate ORM with Panache and Kotlin": "Use Hibernate ORM Panache with Kotlin",
    "Simplified Hibernate Reactive with Panache": "Use Hibernate Reactive with Panache",
    "Simplified Hibernate with Panache Next": "Use Hibernate Panache Next",
    "Simplified MongoDB with Panache": "Use MongoDB with Panache",
    "Simplified MongoDB with Panache and Kotlin": "Use MongoDB Panache with Kotlin",
    "Using the MongoDB Client": "Use the MongoDB client",
    "Using Hibernate Reactive": "Use Hibernate Reactive",
    "Reactive SQL Clients": "Use reactive SQL clients",
    "Using Flyway": "Use Flyway",
    "Using Liquibase": "Use Liquibase",
    "Using Liquibase MongoDB": "Use Liquibase MongoDB",
    "Using transactions in Quarkus": "Transaction reference",
    "Application Data Caching": "Cache application data",
    "Infinispan Cache": "Infinispan cache reference",
    "Redis Cache": "Redis cache reference",
    "Using the Redis Client": "Use the Redis client",
    "Redis Extension Reference Guide": "Redis reference",
    "Using the Infinispan Client": "Use the Infinispan client",
    "Infinispan Client Extension Reference Guide": "Infinispan client reference",
    "Connecting to an Elasticsearch cluster": "Connect to Elasticsearch",
    "Use Hibernate Search with Hibernate ORM and Elasticsearch/OpenSearch": "Use Hibernate Search with ORM",
    "Use Hibernate Search in Standalone mode with Elasticsearch/OpenSearch": "Use Hibernate Search standalone",
    "Using the Cassandra Client": "Use the Cassandra client",
    "Using Blaze-Persistence": "Use Blaze-Persistence",
    "Narayana LRA Participant Support": "Use Narayana LRA",
    "Using Software Transactional Memory in Quarkus": "Use software transactional memory",
    "Dev Services for Databases": "Dev Services for databases",

    # ── secure-your-application ──
    "Quarkus Security overview": "Understand Quarkus security",
    "Quarkus Security architecture": "Understand security architecture",
    "Authentication mechanisms in Quarkus": "Understand authentication mechanisms",
    "Identity providers": "Understand identity providers",
    "Basic authentication": "Understand basic authentication",
    "Proactive authentication": "Understand proactive authentication",
    "Security vulnerability detection and reporting in Quarkus": "Understand vulnerability detection",
    "Getting started with security by using Basic authentication and Jakarta Persistence": "Get started with Basic auth and JPA",
    "Protect a service application by using OpenID Connect (OIDC) Bearer token authentication": "Protect a service with OIDC bearer tokens",
    "Protect a web application by using OpenID Connect (OIDC) authorization code flow": "Protect a web app with OIDC code flow",
    "Protect a Quarkus web application by using an Auth0 OpenID Connect provider": "Protect a web app with Auth0",
    "OpenID Connect client and token propagation quickstart": "Get started with OIDC client",
    "Migrate from Vert.x OIDC to Quarkus OIDC": "Migrate from Vert.x OIDC",
    "OpenID Connect (OIDC) Bearer token authentication": "Understand OIDC bearer authentication",
    "OpenID Connect authorization code flow mechanism for protecting web applications": "Understand OIDC code flow",
    "Configuring Well-Known OpenID Connect Providers": "Understand OIDC providers",
    "Quarkus OpenId Connect (OIDC) Expanded Configuration Reference": "OIDC expanded configuration",
    "Enable Basic authentication": "Enable Basic authentication",
    "Quarkus Security with Jakarta Persistence": "Set up security with JPA",
    "Using OpenID Connect (OIDC) and Keycloak to centralize authorization": "Centralize authorization with Keycloak",
    "Using OpenID Connect (OIDC) multitenancy": "Configure OIDC multitenancy",
    "Dev Services and Dev UI for OpenID Connect (OIDC)": "Use OIDC Dev Services",
    "Authorization of web endpoints": "Web endpoint authorization reference",
    "OpenID Connect (OIDC) configuration properties": "OIDC configuration properties",
    "OpenID Connect (OIDC) and OAuth2 client and filters": "OIDC and OAuth2 client reference",
    "OpenID Connect (OIDC) and OAuth2 dynamic client registration": "OIDC dynamic client registration",
    "Using JWT RBAC": "Use JWT RBAC",
    "Build, sign, and encrypt JSON Web Tokens": "Build, sign, and encrypt JWTs",
    "Using OAuth2 RBAC": "Use OAuth2 RBAC",
    "Using Security with JDBC": "Use security with JDBC",
    "Using Security with an LDAP Realm": "Use security with LDAP",
    "Using Security with WebAuthn": "Use security with WebAuthn",
    "Using Security with .properties File": "Use security with .properties",
    "Using a Credentials Provider": "Use a credentials provider",
    "Using Keycloak Admin Client": "Use the Keycloak admin client",
    "Security Tips and Tricks": "Security tips and tricks",
    "Security Testing": "Test security",
    "Secrets in Configuration": "Manage configuration secrets",
    "Using SSL With Native Executables": "Use SSL with native executables",
    "Using Proxy Registry": "Use the proxy registry",

    # ── send-and-receive-messages ──
    "Quarkus Messaging Extensions": "Use messaging extensions",
    "Getting Started to Quarkus Messaging with Apache Kafka": "Get started with Kafka",
    "Getting Started to Quarkus Messaging with AMQP 1.0": "Get started with AMQP",
    "Getting Started to Quarkus Messaging with Apache Pulsar": "Get started with Pulsar",
    "Getting Started to Quarkus Messaging with RabbitMQ": "Get started with RabbitMQ",
    "Apache Kafka Reference Guide": "Kafka reference",
    "Apache Pulsar Reference Guide": "Pulsar reference",
    "Reactive Messaging AMQP 1.0 Connector Reference Documentation": "AMQP connector reference",
    "Reactive Messaging RabbitMQ Connector Reference Documentation": "RabbitMQ connector reference",
    "Using Apache Kafka Streams": "Use Kafka Streams",
    "Using Apache Kafka with Schema Registry and Avro": "Use Kafka with Schema Registry and Avro",
    "Using Apache Kafka with Schema Registry and JSON Schema": "Use Kafka with Schema Registry and JSON Schema",
    "Kafka Dev UI": "Use the Kafka Dev UI",
    "Quarkus Virtual Thread support with Reactive Messaging": "Use virtual threads with messaging",
    "Using the event bus": "Use the event bus",
    "Using JMS": "Use JMS",

    # ── deploy-to-the-cloud ──
    "Building a Native Executable": "Build a native executable",
    "Container Images": "Build container images",
    "Kubernetes extension": "Deploy to Kubernetes",
    "Deploying Quarkus applications to OpenShift": "Deploy to OpenShift",
    "Deploying Quarkus applications to OpenShift in a single step": "Deploy to OpenShift in one step",
    "Deploying Quarkus Java applications to OpenShift by using a Docker build strategy": "Deploy to OpenShift with Docker",
    "Deploying Quarkus applications compiled to native executables": "Deploy native executables to OpenShift",
    "Using S2I to deploy Quarkus applications to OpenShift": "Deploy to OpenShift with S2I",
    "Deploying to Google Cloud Platform (GCP)": "Deploy to Google Cloud",
    "Deploying to Microsoft Azure Cloud": "Deploy to Azure",
    "Deploying to Heroku": "Deploy to Heroku",
    "AWS Lambda": "Deploy to AWS Lambda",
    "AWS Lambda SnapStart Configuration": "Configure AWS Lambda SnapStart",
    "AWS Lambda with Quarkus REST, Undertow, or Reactive Routes": "Use AWS Lambda with HTTP",
    "Azure Functions": "Deploy to Azure Functions",
    "Azure Functions with Quarkus REST, Undertow, or Reactive Routes": "Use Azure Functions with HTTP",
    "Google Cloud Functions (Serverless)": "Deploy to Google Cloud Functions",
    "Google Cloud Functions (Serverless) with Quarkus REST, Undertow, or Reactive Routes": "Use Google Cloud Functions with HTTP",
    "Funqy": "Use Funqy",
    "Funqy AWS Lambda Binding": "Use Funqy with AWS Lambda",
    "Funqy Google Cloud Functions": "Use Funqy with Google Cloud Functions",
    "Funqy HTTP Binding (Standalone)": "Use Funqy HTTP standalone",
    "Funqy HTTP Binding with AWS Lambda": "Use Funqy HTTP with AWS Lambda",
    "Funqy HTTP Binding with Azure Functions": "Use Funqy HTTP with Azure Functions",
    "Funqy HTTP Binding with Google Cloud Functions": "Use Funqy HTTP with Google Cloud Functions",
    "Funqy Knative Events Binding": "Use Funqy with Knative Events",
    "Kubernetes Client": "Use the Kubernetes client",
    "Kubernetes Config": "Use Kubernetes config",
    "Getting Started with SmallRye Stork": "Get started with SmallRye Stork",
    "Stork Reference Guide": "Stork reference",
    "Using Stork with Kubernetes": "Use Stork with Kubernetes",
    "Initialization tasks": "Configure initialization tasks",
    "Automate Quarkus deployment with Ansible": "Automate deployment with Ansible",
    "AppCDS": "Use AppCDS",
    "Native Reference Guide": "Native reference",
    "Tips for writing native applications": "Write native applications",
    "Compressing native executables using UPX": "Compress native executables with UPX",

    # ── observe-your-application ──
    "Observability in Quarkus": "Observability reference",
    "Collect metrics using Micrometer": "Collect metrics with Micrometer",
    "Migrate from OpenTracing to OpenTelemetry tracing": "Migrate from OpenTracing to OpenTelemetry",
    "SmallRye Health": "Use SmallRye Health",
    "Using OpenTelemetry Tracing": "Use OpenTelemetry tracing",
    "Using OpenTelemetry Metrics": "Use OpenTelemetry metrics",
    "Using OpenTelemetry Logging": "Use OpenTelemetry logging",
    "Using OpenTelemetry": "OpenTelemetry reference",
    "Micrometer Metrics": "Micrometer metrics reference",
    "Micrometer and OpenTelemetry extension": "Micrometer and OpenTelemetry reference",
    "Logging configuration": "Logging configuration reference",
    "Centralized log management (Graylog, Logstash, Fluentd)": "Centralize log management",
    "Using JDK Flight Recorder": "Use JDK Flight Recorder",
    "Management interface reference": "Management interface reference",
    "Observability Dev Services": "Use observability Dev Services",
    "Observability Dev Services with Grafana OTel LGTM": "Use observability Dev Services with Grafana LGTM",

    # ── understand-the-runtime ──
    "Introduction to Contexts and Dependency Injection (CDI)": "Understand CDI",
    "Contexts and Dependency Injection": "CDI reference",
    "Application Initialization and Termination": "Manage application lifecycle",
    "Quarkus Reactive Architecture": "Understand the reactive architecture",
    "Mutiny - Async for mere mortals": "Use Mutiny for async",
    "Context Propagation in Quarkus": "Propagate context",
    "Duplicated context, context locals, asynchronous processing and propagation": "Understand duplicated context",
    "Virtual Thread support reference": "Virtual threads reference",
    "Extending Configuration Support": "Extend configuration support",
    "Dev Services Overview": "Understand Dev Services",
    "Compose Dev Services": "Use Compose Dev Services",
    "How dev mode differs from a production application": "Understand dev mode differences",
    "Using Eclipse Vert.x API from a Quarkus Application": "Use the Vert.x API",
    "Vert.x Reference Guide": "Vert.x reference",
    "Class Loading Reference": "Class loading reference",
    "Command Mode Applications": "Command mode reference",
    "Platform": "Understand the platform",
    "Quarkus Extension Registry": "Use the extension registry",
    "Quarkus Base Runtime Image": "Use the base runtime image",
    "Testing components": "Test components",

    # ── automate-and-integrate ──
    "Scheduling Periodic Tasks": "Schedule periodic tasks",
    "Scheduling Periodic Tasks with Quartz": "Schedule tasks with Quartz",
    "Scheduler Reference Guide": "Scheduler reference",
    "Sending emails using SMTP": "Send email with SMTP",
    "Mailer Reference Guide": "Mailer reference",
    "Defining and executing business rules with Drools": "Define business rules with Drools",
    "Apache Camel on Quarkus": "Use Apache Camel",
    "Command Mode with Picocli": "Use Picocli for command mode",
    "Scripting with Quarkus": "Write scripts",

    # ── use-build-tools ──
    "Quarkus and Maven": "Use Maven",
    "Quarkus and Gradle": "Use Gradle",
    "Quarkus Maven Plugin": "Maven plugin reference",
    "Generating CycloneDX BOMs": "Generate CycloneDX BOMs",
    "Packaging And Releasing With JReleaser": "Package and release with JReleaser",
    "Using Podman with Quarkus": "Use Podman",
    "Re-augment a Quarkus Application": "Re-augment an application",
    "Measuring Performance": "Measure performance",
    "Build analytics": "Use build analytics",
    "All configuration options": "All configuration options",
    "Update projects to the latest Quarkus version": "Update to the latest version",
    "Measuring the coverage of your tests": "Measure test coverage",

    # ── use-spring-apis ──
    "Quarkus Extension for Spring DI API": "Use Spring DI",
    "Quarkus Extension for Spring Web API": "Use Spring Web",
    "Extension for Spring Data API": "Use Spring Data JPA",
    "Extension for Spring Data REST": "Use Spring Data REST",
    "Quarkus Extension for Spring Security API": "Use Spring Security",
    "Quarkus Extension for Spring Cache API": "Use Spring Cache",
    "Quarkus Extension for Spring Scheduling API": "Use Spring Scheduling",
    "Accessing application properties with Spring Boot properties API": "Use Spring Boot properties",
    "Reading properties from Spring Cloud Config Server": "Use Spring Cloud Config",

    # ── write-extensions ──
    "Writing Your Own Extension": "Write your own extension",
    "Building my first extension": "Build your first extension",
    "A maturity matrix for Quarkus extensions": "Understand extension maturity",
    "Frequently asked questions about writing extensions": "Extension FAQ",
    "Extension Capabilities": "Define extension capabilities",
    "Extension codestart": "Create an extension codestart",
    "Quarkus Extension Metadata": "Define extension metadata",
    "Conditional Extension Dependencies": "Configure conditional dependencies",
    "CDI Integration Guide": "Integrate with CDI",
    "Writing a Dev Service": "Write a Dev Service",
    "Dev UI": "Use the Dev UI",
    "Dev Assistant": "Use the Dev Assistant",
    "Dev MCP": "Use Dev MCP",
    "Build Items": "Build items reference",

    # ── contribute-to-quarkus-docs ──
    "Quarkus documentation content types": "Understand content types",
    "Quarkus style and content guidelines": "Style and content guidelines",
    "Contribute to Quarkus documentation": "Contribute to Quarkus documentation",
    "Creating a tutorial": "Create a tutorial",
}


def add_nav_titles(domains_path: Path) -> None:
    with open(domains_path) as f:
        data = yaml.safe_load(f)

    changed = 0
    unchanged = 0
    missing = []

    for domain in data["domains"]:
        for guide in domain.get("guides", []):
            title = guide["title"].strip()
            if title in OVERRIDES:
                nav = OVERRIDES[title]
                if nav != title:
                    guide["nav-title"] = nav
                    changed += 1
                else:
                    unchanged += 1
            else:
                missing.append(title)
                unchanged += 1

    if missing:
        print(f"WARNING: {len(missing)} titles had no override (kept original):")
        for t in missing:
            print(f"  - {t}")

    print(f"\nNav-titles added: {changed}")
    print(f"Titles unchanged: {unchanged}")

    # Write back with comment header
    with open(domains_path, "w") as f:
        f.write("# Generated by _scripts/generate-domains-yaml.py — do not edit manually\n---\n")
        yaml.dump(data, f, default_flow_style=False, allow_unicode=True, sort_keys=False, width=120)

    print(f"\nWrote {domains_path}")


if __name__ == "__main__":
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("_data/domains.yaml")
    add_nav_titles(path)
