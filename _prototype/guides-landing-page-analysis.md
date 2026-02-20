# Quarkus Guides Landing Page: Analysis and Proposed Directions

## Phase 1: Research Synthesis

### Sources Analyzed

1. **GitHub Issue #418** ([quarkusio/quarkusio.github.io#418](https://github.com/quarkusio/quarkusio.github.io/issues/418#issuecomment-3892478686)) -- User feedback citing Laravel, VitePress, Docusaurus, and Vert.x docs as models
2. **Diataxis framework** ([diataxis.fr](https://diataxis.fr/)) -- Four-quadrant documentation taxonomy by Daniele Procida
3. **Anbox Cloud docs** ([documentation.ubuntu.com/anbox-cloud](https://documentation.ubuntu.com/anbox-cloud/)) -- Diataxis reference implementation by its creator
4. **Stripe docs** ([docs.stripe.com](https://docs.stripe.com/)) -- Widely lauded as best-in-class developer documentation (not Diataxis)
5. **Laravel docs** ([laravel.com/docs](https://laravel.com/docs/12.x)) -- Cited in the issue as an ideal navigation model for a full-stack framework

### Comparison Table

| Aspect | Diataxis | Anbox Cloud | Stripe | Issue #418 / Laravel |
|---|---|---|---|---|
| **Primary organizing principle** | Content type (4 quadrants) | Content type + role-based | Product/task ("what are you building?") | Logical topic progression (building blocks in learning order) |
| **Top-level taxonomy** | Tutorials, How-to, Reference, Explanation | Same 4 Diataxis types as sidebar sections | Product categories (Payments, Billing, Connect...) | Building blocks in learning order (routing -> middleware -> controllers -> views) |
| **Navigation model** | 2x2 grid / four co-equal entry points | Left sidebar with 3-level hierarchy + landing page cards | Contextual sidebar per product area + prominent search bar | Persistent left sidebar with collapsible sections (VitePress/Docusaurus model) |
| **Newcomer path** | Tutorials quadrant | "Start here" + 3 sequential tutorials | Curated "Get started" cards + quickstarts | "Getting Started" section at top of sidebar, linear progression |
| **Expert path** | Reference + How-to quadrants | Sidebar deep-links to APIs, CLIs, config schemas | Search bar + API Reference as standalone section | Direct sidebar links, no scrolling through unrelated content |
| **Progressive disclosure** | Implicit (tutorial -> how-to -> reference -> explanation) | Sidebar collapsibility, conditional "if evaluating... try appliance" | Tabbed code, expandable sections, "Next steps" links | Collapsible sidebar sections, "The Basics" vs "Digging Deeper" |
| **Task-oriented grouping** | How-to guides grouped by task | Operations grouped by domain (install, manage, troubleshoot) | Every page titled with a verb phrase ("Accept a payment") | Framework concepts in learning order |
| **Scale handling** | No guidance beyond 4-way split | ~50 pages; clean Diataxis mapping | ~200+ pages; product silos keep each section manageable | Laravel ~60 topics; manually curated order |
| **Key strength** | Clarity of purpose per page; author decision framework | Clean reference implementation; role-based entry points | Search-first; task-first verbs; persistent language selection; live API keys in examples | Persistent sidebar eliminates back-and-forth; sequential order serves both newcomers and experts |
| **Key weakness** | No guidance for 100+ extension scale; "explanation" is weakest category | Small docs set; doesn't demonstrate scaling | Product silos obscure cross-cutting concerns; concepts are buried | Requires manual curation of topic order |

### Cross-Cutting Patterns Worth Borrowing

1. **Persistent sidebar navigation** -- Every source except Diataxis (which is a taxonomy, not a navigation spec) uses a persistent left sidebar. This is the single most requested feature in the GitHub issue.
2. **Curated landing page as a funnel, not an index** -- Both Stripe and Anbox Cloud use their landing pages to route users, not to list all content. The landing page answers "What do you need?" not "Here is everything we have."
3. **Topic-area sub-grouping within each section** -- Whether by Diataxis type (Anbox Cloud) or by product (Stripe), content within each top-level section is further grouped by domain, not just sorted alphabetically.
4. **"Next steps" links at the bottom of every page** -- Stripe does this consistently. It creates implicit learning paths without formal "learning path" infrastructure.
5. **Type badges or labels on individual entries** -- Diataxis types (tutorial, how-to, reference, concept) work well as secondary labels even when they are not the primary organizing axis.

---

## Phase 2: Current Quarkus Guides Page -- Diagnosis

### Architecture Summary

- **Stack**: Jekyll static site with a `_guides` collection of AsciiDoc files
- **Data**: Generated `quarkus.yaml` index at `_data/versioned/latest/index/quarkus.yaml` with types, categories, topics, and extensions per guide
- **Landing page**: `documentation.html` layout renders guides via `_includes/index-docs.html`
- **Filtering**: Version dropdown + category dropdown + keyword search (remote `search.quarkus.io` service with JS fallback)
- **Guide metadata**: Each `.adoc` file has front matter with `:categories:`, `:topics:`, `:extensions:`, and `:diataxis-type:` (mapped to `type` field in the generated YAML)
- **Related content**: A `relations.yaml` file (730KB) maps guides to related guides by shared extensions and topics

### What's Working

1. **Diataxis type metadata exists** -- The infrastructure to classify guides into tutorial/howto/concepts/reference is already in the data model and the `.adoc` front matter.
2. **Rich metadata per guide** -- Categories (20 types), topics, extensions, summaries, and keywords are all available.
3. **Related content system** -- The `relations.yaml` maps guides to related guides by shared extensions/topics, rendered at the bottom of each guide page.
4. **Search exists** -- Remote search service with JS fallback and keyword filtering.
5. **Version switching** works across documented versions (main, latest, 3.27, 3.20, 3.15, etc.).
6. **Style guide with Diataxis awareness** -- The `doc-reference.adoc` contributor guidelines include explicit Diataxis type definitions, title conventions, and content type guidance.

### What's Not Working

| Problem | Evidence | Root Cause |
|---|---|---|
| **69% of guides are unclassified** | 185 of 268 guides have `type: guide` (the catch-all). Only 83 are properly typed: 16 tutorials, 15 howto, 14 concepts, 38 reference. | The `guide` catch-all type defeats the Diataxis classification. The landing page's "General Guides" section is by far the largest, making the properly-typed Diataxis sections feel incomplete. |
| **Flat alphabetical listing within type sections** | Within each type section (Tutorials, How-to Guides, Concepts, References, General Guides), guides are sorted alphabetically by title. | No sub-grouping by topic area, domain, or learning path. A user scanning "How-to Guides" sees an alphabetical wall of 15 entries, while "General Guides" has 185. |
| **No persistent sidebar navigation** | The `_layouts/guides.html` template renders only a "Back to Guides" link (line 24) and a right-side table-of-contents for the current page (line 68). | Users must bounce back to the listing page constantly. The issue commenter reported having 12+ browser tabs open simultaneously. |
| **Category filter is hidden behind a dropdown** | The 20 categories exist but require proactive interaction with a `<select>` dropdown. They don't structure the default view. | Categories are metadata, not navigation. Most users never discover them. |
| **No curated entry points** | The landing page treats all 268 guides as co-equal. No "Start here" hero, no "Most common tasks" section, no progressive journey. | The page is a comprehensive index, not a funnel. |
| **No cross-guide navigation** | Individual guide pages have no sidebar or "next/previous" links. The only navigation is "Back to Guides" and the related content section at the bottom. | Each guide is architecturally independent -- a spoke connected only to the hub page. |
| **"Miscellaneous" anti-pattern** | Important guides (Qute templating, Vert.x, scheduler, mailer) are categorized as "miscellaneous." | The category was used as a dump for anything that didn't fit elsewhere, telling users nothing about what they'll find. |
| **Inconsistent categorization** | gRPC is under "serialization" rather than "web" or "communication." Vert.x is "miscellaneous." | Category assignments were made per-guide without a global taxonomy design. |

### The Core Tension

The Quarkus docs were designed as a **collection of independent guides** -- each self-contained, tagged with flat metadata, organized by extension. But users experience it as a **documentation system** -- they need to navigate across guides, understand relationships, and follow learning paths. Every documentation site cited in the issue (Laravel, VitePress, Docusaurus, Vert.x) treats docs as a unified system with navigation as a first-class concern. Quarkus needs to make the same shift.

---

## Phase 3: Three Proposed Directions

### Direction 1: "Diataxis Done Right"

**Approach**: Fix the existing Diataxis implementation by properly classifying all 268 guides into the four types, eliminating the "guide" catch-all, and adding topic-area sub-grouping within each type section. Add a curated landing page with Diataxis quadrant cards (like Anbox Cloud) as the primary entry point.

**What changes**:

- Reclassify 185 "guide" type docs into tutorial/howto/reference/concepts (metadata-only change in `.adoc` front matter)
- Landing page becomes a curated hub with 4 quadrant cards, each showing 5-8 highlighted entries + "See all" link
- Within each type section, group by topic area (Security, Data, Web, Cloud, etc.) rather than alphabetically
- Add a persistent sidebar to individual guide pages showing the current type's table of contents

**Tradeoffs**:

| Dimension | Assessment |
|---|---|
| Effort | Medium -- reclassifying 185 guides is metadata-only (no content rewriting) + landing page redesign + sidebar component |
| Risk | Low -- builds on existing infrastructure; metadata fields already exist |
| Departure from current | Moderate -- same page, same data model, but the visual experience changes significantly |
| Draws from | Diataxis, Anbox Cloud |

**Strengths**: Preserves and completes the existing Diataxis investment. Lowest risk. Classification work benefits any future direction.

**Weaknesses**: Still organizes by documentation type first, which may not match how users think ("I need Hibernate docs" vs. "I need a tutorial"). Doesn't address the issue commenter's core request for building-block progression.

---

### Direction 2: "Task-Flow Hub"

**Approach**: Replace the type-first organization with a task/domain-first landing page, inspired by Stripe and Laravel. The landing page answers "What are you building?" or "What do you need to do?" rather than "What type of documentation are you looking for?" Guides are grouped by topic domain with type badges on individual entries.

**What changes**:

- Landing page becomes a card grid of ~12-15 curated topic domains (like Stripe's product grid):
  1. Getting Started
  2. Core Concepts (CDI, configuration, lifecycle, build system)
  3. Web (REST, reactive routes, WebSockets, gRPC, fault tolerance)
  4. Data (datasources, Hibernate ORM, Panache, caching, Flyway/Liquibase)
  5. Messaging (Kafka, AMQP, Pulsar, RabbitMQ)
  6. Security (overview, authentication, authorization, OIDC, JWT)
  7. Cloud & Deployment (Kubernetes, OpenShift, AWS Lambda, Azure, GCP, container images)
  8. Observability (health, metrics, tracing, logging)
  9. Advanced Topics (native compilation, virtual threads, context propagation, class loading)
  10. Tooling (Maven, Gradle, CLI, Dev Services, Dev UI)
  11. Spring Compatibility
  12. Writing Extensions
- Each domain card links to a domain-specific sub-page showing all relevant tutorials, how-to guides, references, and concepts for that domain
- A "Getting Started" hero section at the top with the 3-4 most important first-timer guides
- Diataxis types become secondary labels/badges rather than the primary grouping axis
- Persistent left sidebar on all guide pages showing the current domain's content tree (like Laravel)

**Tradeoffs**:

| Dimension | Assessment |
|---|---|
| Effort | High -- requires new domain landing pages, sidebar navigation component, domain-to-guide mapping, and rethinking the URL/template structure |
| Risk | Medium -- creating ~15 domain sub-pages requires curation and ongoing maintenance; domain boundaries need community consensus |
| Departure from current | Major -- fundamentally different navigation paradigm |
| Draws from | Stripe, Laravel, VitePress/Docusaurus (cited in issue #418) |

**Strengths**: Directly addresses the issue commenter's core complaints. Matches how users actually think ("I need database docs" not "I need a how-to guide"). Provides the persistent sidebar and logical progression that users are asking for.

**Weaknesses**: Highest effort. Requires agreeing on ~12-15 domain boundaries, which is a design challenge for a framework with 100+ extensions. Some guides span multiple domains (e.g., "Use OpenID Connect to protect a REST API" spans Security and Web).

---

### Direction 3: "Hybrid Navigator"

**Approach**: Keep the current single-page listing but add two major UX improvements: (1) a persistent, collapsible left sidebar that appears on all guide pages, and (2) a redesigned landing page header with curated "Start Here" paths. The listing below retains the Diataxis type sections but adds visible topic sub-groupings within each section.

**What changes**:

- Add a persistent left sidebar navigation component to all guide pages, grouped by topic domain with type indicators (e.g., an icon or badge for tutorial/howto/reference/concept)
- Redesign only the top portion of the landing page: add a persona-based "Start Here" section with 3-4 curated pathways:
  - "New to Quarkus? Start here" -> Getting Started tutorial
  - "Building a REST API" -> REST + CDI + Panache guide chain
  - "Deploying to production" -> Container images + Kubernetes + Observability
  - "Migrating from Spring" -> Spring compatibility guides
- Within the existing type sections, add topic-area subheadings (e.g., under "How-to Guides": Web, Data, Security, Cloud sub-groups)
- Make category filters visible as tabs or pills rather than a hidden dropdown
- Reclassify the 185 catch-all "guide" type docs (same as Direction 1)
- Add "Next/Previous" navigation links at the bottom of each guide page

**Tradeoffs**:

| Dimension | Assessment |
|---|---|
| Effort | Medium -- sidebar component is the main new engineering work; classification is metadata-only; landing page header redesign is incremental |
| Risk | Low -- additive changes; nothing breaks for existing users or bookmarks |
| Departure from current | Moderate -- the listing page evolves rather than being replaced |
| Draws from | All four sources (Diataxis type structure, Anbox Cloud curated landing, Stripe curated hero, Laravel/VitePress sidebar navigation) |

**Strengths**: Addresses the #1 user complaint (no sidebar) with manageable effort. The landing page gets curated entry points without a full redesign. Backward-compatible with existing bookmarks and URLs. Classification work (shared with Direction 1) benefits any future direction. Can be implemented incrementally.

**Weaknesses**: The listing page remains a single long page. Does not fully achieve the domain-first navigation of Direction 2. The sidebar requires a topic hierarchy that doesn't fully exist yet (the current 20 flat categories need refinement and nesting).

---

## Recommendation Matrix

| Criterion | Direction 1: Diataxis Done Right | Direction 2: Task-Flow Hub | Direction 3: Hybrid Navigator |
|---|---|---|---|
| Addresses issue #418 complaints | Partially (better classification, but no sidebar) | Fully (sidebar + domain-first + progression) | Mostly (sidebar + curated entry points) |
| Implementation effort | Medium | High | Medium |
| Risk to existing users | Low | Medium (URL changes, navigation paradigm shift) | Low (additive) |
| Maintenance burden | Low (metadata-driven) | High (curated domain pages) | Medium (sidebar hierarchy + metadata) |
| Prerequisite for others | Yes (classification work needed by all directions) | Yes (needs Direction 1's classification first) | Yes (needs Direction 1's classification) |
| Can be done incrementally | Yes | Partially | Yes |

**Note**: Direction 1's classification work (reclassifying 185 guides) is a prerequisite for both Direction 2 and Direction 3. Starting with Direction 1 keeps all options open. Directions are composable -- Direction 3's sidebar can evolve into Direction 2's domain-first navigation over time.

---

## Phase 4: Implementing Direction 2 with Jobs to Be Done (JTBD)

### Why JTBD for Documentation

The Jobs to Be Done framework reframes documentation from **what it is** (tutorial, reference, concept) to **what the user needs to accomplish**. Users don't arrive at docs thinking "I need a how-to guide." They arrive thinking "I need to connect my app to a database" or "I need to deploy to Kubernetes." JTBD aligns the documentation's organizing principle with these user motivations.

In the JTBD model, users **hire** documentation to get a job done. A job statement takes the form:

> "When [situation], I want to [motivation], so I can [expected outcome]."

This maps directly to Direction 2's "Task-Flow Hub" approach: the landing page answers "What do you need to do?" and routes users to domain-specific sections where all relevant content types (tutorials, how-to guides, references, concepts) live together under one roof.

### JTBD Principles Applied to Quarkus Guides

| Principle | Application |
|---|---|
| **Organize by user job, not content type** | Domain categories replace Diataxis type sections as the primary axis |
| **Title with imperative verbs** | Tutorials: "Build...", "Create..."; How-to: "Configure...", "Deploy..."; Concepts: "Understand...", "Learn..."; References: descriptive, no verb |
| **No gerunds** | "Configure logging" not "Configuring logging"; "Build a native executable" not "Building a Native Executable" |
| **Focus on user outcomes** | "Protect a web application with OIDC" not "OpenID Connect authorization code flow mechanism for protecting web applications" |
| **Diataxis types become secondary labels** | Each guide retains its type (tutorial/howto/reference/concept) as a badge, but type is not the grouping axis |

---

### Proposed Domain Categories with JTBD Job Statements

#### 1. Get Started

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

**Guide count**: 10

---

#### 2. Build REST APIs and Web Services

> "When I need to expose HTTP endpoints or consume external APIs, I want to create and configure REST services efficiently, so I can build reliable web services and integrations."

| Current Title | JTBD Title | Type |
|---|---|---|
| Writing REST Services with Quarkus REST | Write REST services with Quarkus REST | howto |
| Writing JSON REST Services | Write JSON REST services | howto |
| Using the REST Client | Use the REST Client | howto |
| Using OpenAPI and Swagger UI | Generate OpenAPI docs and use Swagger UI | howto |
| Using Reactive Routes | Use reactive routes | howto |
| Validation with Hibernate Validator | Validate input with Hibernate Validator | howto |
| SmallRye Fault Tolerance | Build resilient services with fault tolerance | howto |
| HTTP Reference | HTTP configuration | reference |
| TLS registry reference | TLS registry configuration | reference |
| Cross-Origin Resource Sharing (CORS) | CORS configuration | reference |
| Load Shedding reference guide | Load shedding configuration | reference |
| Migrating to Quarkus REST | Migrate to Quarkus REST | howto |
| RESTEasy Classic | RESTEasy Classic compatibility | reference |
| Using the legacy REST Client | Use the legacy REST Client | howto |
| Using the legacy REST Client with Multipart | Use the legacy REST Client with multipart | howto |
| Cross-Site Request Forgery Prevention | Prevent Cross-Site Request Forgery (CSRF) | howto |
| Generating Jakarta REST resources with Panache | Generate REST resources with Panache | howto |
| Use virtual threads in REST applications | Use virtual threads in REST applications | howto |
| Web dependency locator | Web dependency locator | reference |

**Guide count**: 19

---

#### 3. Build for the Web

> "When I need to build a full-stack web application with server-side rendering, templating, or WebSocket communication, I want to use Quarkus web frameworks, so I can deliver interactive web experiences."

| Current Title | JTBD Title | Type |
|---|---|---|
| Quarkus for the Web | Explore Quarkus web frameworks | concept |
| Qute Templating Engine | Use the Qute templating engine | howto |
| Qute Reference Guide | Qute template language | reference |
| Getting started with WebSockets Next | Build a WebSocket application | tutorial |
| WebSockets Next reference guide | WebSockets Next configuration | reference |
| Using WebSockets with Undertow | Use legacy WebSockets with Undertow | howto |
| SmallRye GraphQL | Build GraphQL services | howto |
| SmallRye GraphQL Client | Use the GraphQL client | howto |

**Guide count**: 8

---

#### 4. Access and Manage Data

> "When I need to store, retrieve, or manage persistent data, I want to connect to databases, use ORM frameworks, and manage schemas, so I can build data-driven applications reliably."

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

#### 5. Secure Your Application

> "When I need to protect my application from unauthorized access, I want to configure authentication and authorization mechanisms, so I can enforce security policies and protect sensitive resources."

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

#### 6. Send and Receive Messages

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

#### 7. Communicate with gRPC

> "When I need high-performance, strongly-typed service-to-service communication, I want to implement and consume gRPC services, so I can build efficient inter-service APIs with Protocol Buffers."

| Current Title | JTBD Title | Type |
|---|---|---|
| gRPC | Understand gRPC in Quarkus | concept |
| Getting Started with gRPC | Get started with gRPC | tutorial |
| Implementing a gRPC Service | Implement a gRPC service | howto |
| Consuming a gRPC Service | Consume a gRPC service | howto |
| Deploying your gRPC Service in Kubernetes | Deploy a gRPC service to Kubernetes | howto |
| Quarkus Virtual Thread support for gRPC services | Use virtual threads with gRPC | howto |
| Using gRPC CLI | Use the gRPC CLI | howto |
| Using xDS gRPC | Use xDS with gRPC | howto |
| gRPC reference guide | gRPC server and client configuration | reference |
| gRPC code generation reference guide | gRPC code generation configuration | reference |

**Guide count**: 10

---

#### 8. Deploy to the Cloud

> "When I need to run my application in production, I want to build container images and deploy to cloud platforms, so I can ship my application with confidence."

| Current Title | JTBD Title | Type |
|---|---|---|
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

**Guide count**: 34

---

#### 9. Observe Your Application

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

#### 10. Use Core Framework Features

> "When I need to understand Quarkus internals like dependency injection, the reactive engine, or the application lifecycle, I want clear explanations and configuration guidance, so I can make informed architectural decisions."

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
| Continuous Testing | Use continuous testing | howto |
| Using Eclipse Vert.x API from a Quarkus Application | Use the Vert.x API | howto |
| Vert.x Reference Guide | Vert.x configuration | reference |
| Scheduling Periodic Tasks | Schedule periodic tasks | howto |
| Scheduling Periodic Tasks with Quartz | Schedule tasks with Quartz | howto |
| Scheduler Reference Guide | Scheduler configuration | reference |
| Sending emails using SMTP | Send email with SMTP | howto |
| Mailer Reference Guide | Mailer configuration | reference |
| Class Loading Reference | Class loading | reference |
| Command Mode Applications | Command mode applications | reference |
| Command Mode with Picocli | Build CLI applications with Picocli | howto |
| Scripting with Quarkus | Script with JBang | howto |
| Using Eclipse Vert.x API from a Quarkus Application | Use the Vert.x API | howto |
| Defining and executing business rules with Drools | Define business rules with Drools | howto |
| Apache Camel on Quarkus | Integrate with Apache Camel | howto |
| Platform | Understand Quarkus platforms | concept |
| Quarkus Extension Registry | Understand the extension registry | concept |
| Quarkus Base Runtime Image | Quarkus base runtime image | reference |
| Config Reference Guide | Configuration reference | reference |

**Guide count**: ~31

---

#### 11. Compile to Native

> "When I need to optimize my application for fast startup and low memory footprint, I want to compile to a native executable, so I can run efficiently in containers and serverless environments."

| Current Title | JTBD Title | Type |
|---|---|---|
| Building a Native Executable | Build a native executable | tutorial |
| Native Reference Guide | Native compilation configuration | reference |
| Tips for writing native applications | Write native-compatible code | howto |
| Compressing native executables using UPX | Compress native executables with UPX | howto |
| Using SSL With Native Executables | Use SSL with native executables | howto |

**Guide count**: 5

---

#### 12. Test Your Application

> "When I need to verify that my application works correctly, I want to write and run tests efficiently, so I can catch regressions and ship with confidence."

| Current Title | JTBD Title | Type |
|---|---|---|
| Testing Your Application | Test your application | howto |
| Testing components | Test individual components | howto |
| Measuring the coverage of your tests | Measure test coverage | howto |
| Continuous Testing | Use continuous testing | howto |
| Security Testing | Test security | howto |

**Guide count**: 5

---

#### 13. Use Build Tools

> "When I need to build, package, or manage my project, I want to use standard build tools with Quarkus-specific optimizations, so I can integrate Quarkus into my existing development workflow."

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

**Guide count**: 12

---

#### 14. Migrate from Spring

> "When I'm moving an existing Spring Boot application to Quarkus, I want to use familiar Spring APIs as a bridge, so I can migrate incrementally without rewriting everything at once."

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
| Using Kotlin | Use Kotlin | howto |

**Guide count**: 10

---

#### 15. Write Extensions

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

#### 16. Contribute to Quarkus Docs

> "When I want to contribute documentation to the Quarkus project, I want to understand the content standards and workflow, so I can write content that meets the project's quality bar."

| Current Title | JTBD Title | Type |
|---|---|---|
| Quarkus documentation content types | Understand documentation content types | concept |
| Quarkus style and content guidelines | Documentation style guide | reference |
| Contribute to Quarkus documentation | Contribute documentation | howto |
| Creating a tutorial | Create a tutorial | tutorial |

**Guide count**: 4

---

### Guide Distribution Summary

| Domain | Guide Count | Primary User Job |
|---|---|---|
| Get Started | 10 | First project setup and orientation |
| Build REST APIs and Web Services | 19 | HTTP endpoints, REST clients, validation |
| Build for the Web | 8 | Templating, WebSockets, GraphQL |
| Access and Manage Data | 35 | Databases, ORM, caching, search, migrations |
| Secure Your Application | 40 | Authentication, authorization, identity |
| Send and Receive Messages | 21 | Kafka, AMQP, Pulsar, RabbitMQ, event bus |
| Communicate with gRPC | 10 | gRPC services, clients, code generation |
| Deploy to the Cloud | 34 | Kubernetes, OpenShift, serverless, containers |
| Observe Your Application | 16 | Metrics, tracing, logging, health |
| Use Core Framework Features | ~31 | CDI, config, lifecycle, reactive, scheduling |
| Compile to Native | 5 | GraalVM native executables |
| Test Your Application | 5 | Testing, coverage, continuous testing |
| Use Build Tools | 12 | Maven, Gradle, CLI, packaging |
| Migrate from Spring | 10 | Spring API compatibility |
| Write Extensions | 15 | Extension development, Dev UI, Dev Services |
| Contribute to Quarkus Docs | 4 | Documentation standards and workflow |
| **Total** | **~275** | *(some guides appear in 2 domains)* |

**Note**: The total exceeds 268 because a handful of guides (e.g., "Using SSL With Native Executables," "Continuous Testing," "Secrets in Configuration") serve two domains and appear in both. On the landing page, each guide would have a **primary** domain assignment, with cross-references from secondary domains.

---

### Content Gaps Identified

Jobs that users likely need but have no dedicated guide:

| Missing Job | Domain | Suggested Content Type |
|---|---|---|
| "Understand when to choose reactive vs. imperative" | Core | concept |
| "Migrate a monolith to Quarkus microservices" | Get Started | tutorial |
| "Debug a Quarkus application in dev mode" | Get Started / Tooling | howto |
| "Use structured logging with JSON" | Observability | howto |
| "Set up a CI/CD pipeline for a Quarkus application" | Deploy to the Cloud | howto |
| "Deploy to AWS ECS/Fargate" | Deploy to the Cloud | howto |
| "Use Redis for session storage" | Data | howto |
| "Configure rate limiting" | REST APIs | howto |
| "Handle file uploads with REST" | REST APIs | howto |
| "Monitor Quarkus with Prometheus and Grafana (end-to-end)" | Observability | tutorial |
| "Understand build-time vs. runtime in Quarkus" | Core | concept |
| "Choose between Hibernate ORM and Hibernate Reactive" | Data | concept |

---

### JTBD Title Rewrite Validation Checklist

- [x] All tutorial titles use imperative verbs: Build, Create, Get started, Protect, Collect, Migrate
- [x] All how-to titles use imperative verbs: Configure, Use, Deploy, Enable, Write, Set up, Manage
- [x] All concept titles use learning verbs: Understand, Explore, Learn
- [x] All reference titles are descriptive (no action verb): "...configuration," "...properties," "...catalog"
- [x] No gerunds (-ing forms) in any JTBD title
- [x] Titles focus on user outcomes, not features
- [x] No anthropomorphic language ("allows you to," "enables you to")
- [x] Categories ordered by user priority (Get Started first, Contributing last)

---

### Implementation Roadmap

#### Step 1: Reclassify Guides (prerequisite, ~2-3 days)

Assign correct Diataxis types to the 185 catch-all "guide" entries. This is metadata-only work in `.adoc` front matter (`:diataxis-type:` attribute). The domain mapping table above provides the suggested type for each guide.

**Deliverable**: Updated `quarkus.yaml` with zero `type: guide` entries.

#### Step 2: Define Domain Taxonomy (~1 day)

Formalize the 16 domain categories in a new `_data/domains.yaml` file:

```yaml
domains:
  - id: get-started
    title: "Get Started"
    job: "When I'm evaluating Quarkus or starting a new project..."
    icon: rocket
    order: 1
  - id: rest-apis
    title: "Build REST APIs and Web Services"
    job: "When I need to expose HTTP endpoints..."
    icon: globe
    order: 2
  # ... etc.
```

Add a `:domain:` attribute to each `.adoc` front matter to assign its primary domain.

**Deliverable**: `domains.yaml` + updated `.adoc` front matter.

#### Step 3: Redesign the Landing Page (~3-5 days)

Replace the type-section layout in `_includes/index-docs.html` with a domain-card grid:

1. **Hero section**: "Get Started" pathway with the 3 most important first-timer guides
2. **Domain card grid**: 16 cards (4x4 or responsive), each showing domain title, job statement summary, guide count, and a "View guides" link
3. **Search bar**: Retain and promote the existing search, placed above the card grid
4. **Category/type filters**: Move to a secondary filter bar below the cards

**Deliverable**: New `_includes/index-docs-v2.html` template.

#### Step 4: Create Domain Sub-Pages (~5-7 days)

For each domain, create a page that lists all guides in that domain, grouped by Diataxis type with type badges:

```
/guides/domain/data/
  ├── Concepts
  │   └── [concept guides]
  ├── Tutorials
  │   └── [tutorial guides]
  ├── How-to Guides
  │   └── [howto guides]
  └── Reference
      └── [reference guides]
```

Each guide entry shows its JTBD title, summary, type badge, and status (stable/preview/experimental).

**Deliverable**: Domain page template + 16 domain landing pages.

#### Step 5: Add Persistent Sidebar (~3-5 days)

Add a collapsible left sidebar to `_layouts/guides.html` that shows the current domain's guide tree. The sidebar:

- Highlights the current guide
- Groups by type within the domain
- Collapses to an icon on mobile
- Shows type badges (T, H, R, C) next to each entry

**Deliverable**: Sidebar component in `_includes/guide-sidebar.html`.

#### Step 6: Add Cross-Guide Navigation (~1-2 days)

At the bottom of each guide page, add:

1. **"Next steps"** links to 2-3 related guides within the same domain
2. **"Related in other domains"** links to cross-domain guides
3. **Previous/Next** links within the domain's guide ordering

**Deliverable**: Updated `_layouts/guides.html` with navigation footer.

#### Step 7: Update Guide Titles (~2-3 days)

Apply JTBD-compliant titles to guide `.adoc` front matter. This changes the `:title:` attribute in each file but does **not** change URLs (the filename-based URL is preserved).

**Deliverable**: Updated `.adoc` titles matching the JTBD title column above.

---

### Estimated Total Effort

| Step | Effort | Dependencies |
|---|---|---|
| 1. Reclassify guides | 2-3 days | None |
| 2. Define domain taxonomy | 1 day | Step 1 |
| 3. Redesign landing page | 3-5 days | Step 2 |
| 4. Create domain sub-pages | 5-7 days | Step 2 |
| 5. Add persistent sidebar | 3-5 days | Step 4 |
| 6. Add cross-guide navigation | 1-2 days | Step 4 |
| 7. Update guide titles | 2-3 days | Step 2 |
| **Total** | **~17-26 days** | Steps 3-7 can partially overlap |

Steps 3, 4, and 7 can run in parallel once Step 2 is complete. Steps 5 and 6 depend on Step 4. The critical path is: Step 1 -> Step 2 -> Step 4 -> Step 5.
