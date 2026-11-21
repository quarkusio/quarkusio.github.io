# Quarkus Insights #268: Secure AI agents with Quarkus LangChain4j

This summary was generated using AI, reviewed by humans - [watch the video](https://youtube.com/live/eJBlbuuW5II) for the full story.

## Quarkus Insights #268: Secure AI agents with Quarkus LangChain4j

As generative AI transitions from simple chat interfaces to autonomous, multi-step agentic workflows, the security boundaries of enterprise applications face unprecedented challenges. Traditional role-based access control and API gateways were designed for deterministic clients, but AI agents introduce non-deterministic execution, dynamic tool selection, and complex delegation chains across backend microservices. In episode 268, regular host Eric Deandrea is joined by guests Willem Jan Glerum, software engineer at [Lunatech](https://lunatech.com/), and Sergey Beryozkin, security lead for Quarkus, to demonstrate how to build end-to-end secure AI agent architectures using [LangChain4j](https://github.com/langchain4j/langchain4j), the [Model Context Protocol (MCP)](https://modelcontextprotocol.io/), [Keycloak](https://www.keycloak.org/), and [SPIFFE](https://spiffe.io/) workload identity federation.

<iframe width="560" height="315" src="https://www.youtube.com/embed/eJBlbuuW5II" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>

## Quarkus News and Community Updates

Eric kicked off the episode with key milestones and upcoming events across the Quarkus ecosystem:

- **Quarkus 4 Candidate Release 1 (CR1):** The first release candidate for Quarkus 4 shipped mid-week, signaling that the general availability release is rapidly approaching.
- **Quarkiverse extension readiness:** Ecosystem extensions, including Langfuse, Docling, and Quarkus Wire, are actively upgrading to Quarkus 4 with minimal code changes and seamless compatibility.
- **Quarkus:Dev CFP is open:** The call for presentations is officially open for Quarkus:Dev, the upcoming virtual community conference scheduled for early February. Community members are encouraged to submit talk proposals, architectural case studies, and user stories.

## The AI Security Landscape: The OWASP Top 10 for LLMs

Willem framed the conversation around the [OWASP Top 10 for LLM Applications](https://owasp.org/www-project-top-10-for-large-language-model-applications/), which highlights the unique vulnerabilities encountered when integrating large language models into enterprise systems:

- **Prompt injection:** Attackers craft adversarial inputs or indirect payloads (such as malicious text embedded in user reviews or conference feedback) to hijack model execution and manipulate tool invocations.
- **Sensitive information disclosure:** Models inadvertently revealing personal data, credentials, or proprietary information without proper authorization checks.
- **Excessive agency:** Agents granted broad permissions or access to all backend tools, allowing an untrusted or confused model to execute destructive operations.
- **Hidden context exposure (System prompt leakage):** Attackers prompting the agent to reveal internal instructions, secret tokens, or business rules embedded in system messages.
- **Unbounded consumption:** Denial of wallet attacks that exhaust API quotas and compute budgets through infinite loops or oversized prompt payloads.
- **Supply chain vulnerabilities:** Vulnerabilities in third-party libraries, container images, or external model endpoints.

Addressing these risks requires moving away from static API keys and embracing layered defense in depth, spanning prompt hardening, deterministic guardrails, delegated identity propagation, and zero-trust workload federation.

## Architecture of the Demonstration Stack

To demonstrate these security patterns in action, Willem constructed a distributed, multi-tier conference management system consisting of three distinct Quarkus services coordinated by Keycloak:

1. **Conference REST API:** A backend service built with Quarkus REST, [Hibernate ORM](https://hibernate.org/orm/) with Panache, Hibernate Validator, [PostgreSQL](https://www.postgresql.org/), and [OpenAPI](https://www.openapis.org/) / [Swagger UI](https://swagger.io/tools/swagger-ui/). The API enforces role-based access control (RBAC) across attendees, speakers, and organizers, and records immutable audit events.
2. **Conference MCP Server:** A middleware service built using the Quarkus MCP server extension and [OpenID Connect (OIDC)](https://openid.net/developers/how-connect-works/). Rather than exposing raw technical endpoints, the MCP server wraps backend capabilities into semantic tools with natural language names and descriptions, while managing token delegation and step-up authentication.
3. **Conference Chat Application:** A browser-based AI assistant built with Quarkus LangChain4j using the `@RegisterAiService` interface, connecting to Anthropic Claude 3.5 Sonnet. The chat app facilitates conversational discovery, talk bookmarking, and session ratings through interactive MCP tool invocations.

Each service operates as an independent OAuth client in Keycloak, leveraging Quarkus Dev Services for seamless local container provisioning.

## Guardrails: Defense in Depth for LLM Inputs, Outputs, and Tools

While modern LLMs such as Claude 3.5 Sonnet demonstrate strong intrinsic resistance against naive indirect prompt injections (such as ignoring malicious commands injected into user feedback strings), production applications cannot rely solely on probabilistic model behavior.

Willem and Eric demonstrated how Quarkus LangChain4j provides deterministic guardrails at multiple levels of the AI lifecycle:

### Input Guardrails

Input guardrails evaluate incoming user prompts before they reach the model. In the demo application, an input guardrail inspects email-sending requests, extracting the authenticated user identity from the security context and asserting that the destination email matches the caller's own address. If an unauthorized recipient is detected, the guardrail rejects the request immediately, avoiding unnecessary token consumption.

### Output Guardrails and Canary Tokens

Output guardrails validate model completions before they are returned to the user. To prevent hidden context exposure (system prompt leakage), Willem demonstrated a canary token strategy:

- A unique marker or canary string is embedded directly into the agent's system message.
- The output guardrail inspects the LLM response. If the canary string appears in the output, the guardrail detects that the system prompt was compromised, terminates the response with a fatal error, and suppresses the leaked content.
- Eric noted that an upstream LangChain4j pull request introduces dynamic per-invocation canary tokens, ensuring that canary markers vary across parallel requests and singleton services rather than relying on static strings.

### Tool Input and Output Guardrails

Guardrails can also intercept tool calls directly. Eric shared that tool-level input and output guardrails, initially developed in Quarkus LangChain4j, are being ported upstream to LangChain4j so they apply consistently across standard tools and MCP tools. This prevents agents from calling sensitive tools with unvalidated parameters or surfacing raw tool outputs containing unvetted data.

## Securing Model Context Protocol with OAuth 2.1 and Token Exchange

As tool calling standardizes on the Model Context Protocol, securing communication between clients, MCP servers, and downstream APIs becomes critical.

Early iterations of the MCP specification relied on static API keys or stateful handshake sessions where authentication occurred once during connection startup. However, stateful sessions hinder horizontal scalability across server instances. The latest MCP specifications adopt stateless, streamable HTTP connections where every individual JSON-RPC request is authenticated and authorized using [OAuth 2.1](https://oauth.net/2.1/) with [Proof Key for Code Exchange (PKCE)](https://datatracker.ietf.org/doc/html/rfc7636).

### The Peril of Token Passthrough

A common architectural misstep is token passthrough, where the chat client simply forwards its own bearer token through the MCP server to downstream APIs. Willem and Sergey explained why this violates security standards:

- **Audience mismatch:** The initial user token is minted specifically for the MCP server audience (`aud` claim). Passing it to backend APIs breaks audience validation and allows compromised downstream services to impersonate clients.
- **Loss of intent and delegation context:** Downstream systems cannot distinguish whether an operation was performed directly by the user or initiated by an intermediate agent acting on their behalf.

### RFC 8693 Token Exchange in Practice

To adhere to the MCP specification, the MCP server must exchange the incoming user token for a new token targeting the downstream API audience:

1. When the user logs in, Keycloak presents an explicit consent screen requesting permission for the conference chat application to access their profile and delegate actions to the conference MCP server.
2. When the chat application invokes an MCP tool, the MCP server receives the user's access token.
3. Using [RFC 8693 (OAuth 2.0 Token Exchange)](https://datatracker.ietf.org/doc/html/rfc8693), the MCP server presents its own client credentials and the user's token to Keycloak.
4. Keycloak mints a new access token scoped specifically for the Conference REST API audience, embedding an `act` (actor) claim that records the MCP server as the intermediate delegate.
5. The Conference REST API validates the new token, enforces user-specific role boundaries, and logs the complete audit trail: the action was performed by the user, via the chat client, through the MCP server.

To bridge current Identity Provider draft specifications, Willem demonstrated using trusted `X-MCP` headers to propagate client context and authentication class references until nested JWT delegation claims are fully standardized across upstream identity providers.

## Step-Up Authentication and Dynamic Authorization

Certain high-risk operations (such as exporting GDPR-sensitive attendee lists or canceling scheduled presentations) should not execute under standard login credentials.

Willem demonstrated dynamic step-up authentication integrated into the MCP workflow:

1. An attendee requests a sensitive action through the chat assistant.
2. The MCP server evaluates the request against Keycloak security policies and rejects the invocation with an `insufficient_user_authentication` protocol error.
3. The chat application intercepts the challenge and prompts the user to perform two-factor authentication (Time-Based One-Time Password / TOTP).
4. Once verified, Keycloak elevates the user's Authentication Context Class Reference (ACR) from Level 1 (basic password) to Level 2 (multi-factor authentication).
5. The application enforces a strict time-to-live policy (such as a maximum validity of five minutes for elevated credentials), after which subsequent sensitive operations require re-authentication.

## Zero Trust for Non-Human Workloads: SPIFFE Workload Identity Federation

In the second half of the episode, Sergey Beryozkin addressed security for non-human agentic workloads, where automated agents interact with LLM providers and enterprise services without direct user logins.

### The Failure of Static API Keys

In enterprise zero-trust architectures, static API keys present significant operational liabilities:

- They are hard to rotate and difficult to revoke without breaking running services.
- They lack caller identity, role scoping, and fine-grained audit metadata.
- If leaked, static keys grant unrestricted access across an entire organization.

### Dynamic Token Federation with SPIFFE and SPIRE

[Zero Trust Architecture](https://www.nist.gov/publications/zero-trust-architecture) requires evaluating every machine interaction dynamically. Sergey demonstrated how Quarkus integrates with [SPIRE](https://github.com/spiffe/spire) on [Red Hat OpenShift](https://www.redhat.com/en/technologies/cloud-computing/openshift) to eliminate API keys entirely:

1. A non-human AI agent workload running on OpenShift or [Kubernetes](https://kubernetes.io/) is issued a cryptographically verifiable SPIFFE identity document (SVID) by the local SPIRE agent.
2. The Quarkus application uses the upcoming SPIFFE client extension in Quarkus 4 to retrieve the short-lived workload token.
3. The agent exchanges the SPIFFE token with an LLM provider federation layer (such as Anthropic or Google Cloud identity federation) to obtain a federated, short-lived access token.
4. The agent invokes the LLM using the federated token over [Eclipse Vert.x](https://vertx.io/) HTTP clients, completely bypassing hardcoded secrets.
5. Audit logs capture rich provenance for every request, including workload identity, token issuer, audience, timestamp, and HTTP client runtime.

## Key Takeaways

1. **AI agents break traditional perimeter assumptions:** Dynamic tool invocation, non-deterministic responses, and multi-tier delegation require explicit zero-trust security boundaries.
2. **Layered guardrails provide defense in depth:** Input guardrails sanitize prompts before model invocation, while output guardrails validate completions before returning them to users.
3. **Canary tokens stop system prompt leakage:** Embedding dynamic canary strings in system messages allows output guardrails to detect and abort prompt exfiltration attempts.
4. **Tool-level guardrail standardization:** Tool input and output guardrails developed in Quarkus LangChain4j are moving upstream to LangChain4j for universal tool protection.
5. **Modern MCP embraces stateless OAuth 2.1:** The latest Model Context Protocol specifications replace stateful handshakes with stateless HTTP requests protected by OAuth 2.1 and PKCE.
6. **Token passthrough is a security hazard:** Directly forwarding user tokens breaks audience isolation and exposes backend systems to cross-service impersonation.
7. **RFC 8693 token exchange preserves identity:** Token exchange allows MCP servers to acquire correctly scoped audience tokens while preserving user provenance via actor claims.
8. **Step-up authentication gates sensitive tools:** High-impact operations trigger dynamic re-authentication challenges, elevating Keycloak ACR levels with configurable time-to-live policies.
9. **Static API keys fail zero-trust requirements:** Hardcoded API keys lack audit context, resist rotation, and present catastrophic leak liabilities in agentic architectures.
10. **SPIFFE workload identity federation eliminates keys:** Machine-to-machine agents running in Kubernetes and OpenShift can exchange short-lived SPIFFE tokens with LLM providers for keyless execution.

## Conclusion

Securing AI agents requires shifting from static credentials and blind trust to deterministic guardrails, delegated token exchange, and cryptographically verified workload identities. By combining Quarkus LangChain4j, Quarkus MCP Server, Keycloak, and SPIFFE, developers can build powerful agentic applications that enforce strict access control, prevent data leakage, and maintain complete auditability. Next week, Quarkus Insights returns with Charles Moulliard and Aurea Muñoz Hernandez to discuss AI-assisted migration tools for Quarkus.

###### Watch the full episode on the [Quarkus YouTube channel](https://www.youtube.com/quarkusio).
