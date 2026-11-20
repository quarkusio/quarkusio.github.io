# A2A Java SDK 1.0.0.Beta1 Released

We are pleased to announce the release of A2A Java SDK 1.0.0.Beta1, our first release fully aligned with the [final A2A Specification 1.0.0](https://a2a-protocol.org/v1.0.0/specification/). The A2A Protocol has reached stability as an open standard, and this release marks the Java SDK tracking that maturity: protocol conformance is the central focus, and the SDK coordinates now reflect the project's identity under the [A2A Project organization](https://github.com/a2aproject).

As a sign of this maturity, two structural breaking changes accompany this release:

- Java packages have been renamed from `io.a2a.*` to `org.a2aproject.sdk.*`
- The Maven `groupId` has changed from `io.github.a2asdk` to `org.a2aproject.sdk` (first introduced in 1.0.0.Alpha4)

## What's A2A?

The Agent2Agent (A2A) Protocol is an open standard that enables AI agents to communicate and collaborate with one another, regardless of each agent's underlying framework, language, or vendor. The A2A Java SDK makes it easy to build A2A-compliant agents in Java, with reference implementations based on Quarkus.

## Breaking Changes

### New Maven Coordinates

> [!IMPORTANT]
> The Maven `groupId` has changed from `io.github.a2asdk` to `org.a2aproject.sdk`. If you are upgrading from any previous release, you must update your build files accordingly.

The root artifact coordinates were renamed to reflect the project's new home under the [A2A Project organization](https://github.com/a2aproject) and its availability on Maven Central.

Update your `pom.xml` dependencies accordingly:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.a2aproject.sdk</groupId>
            <artifactId>a2a-java-sdk-bom</artifactId>
            <version>1.0.0.Beta1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- Client -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-client</artifactId>
</dependency>

<!-- For JSON-RPC transport -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-reference-jsonrpc</artifactId>
</dependency>

<!-- For gRPC transport -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-reference-grpc</artifactId>
</dependency>

<!-- For HTTP+JSON/REST transport -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-reference-rest</artifactId>
</dependency>
```

### New Java Package Names

> [!IMPORTANT]
> All Java packages have been renamed from `io.a2a.*` to `org.a2aproject.sdk.*`. You must update all import statements in your source code when upgrading from any previous release.

For example:

```java
// Before
import io.a2a.client.A2AClient;
import io.a2a.model.Task;

// After
import org.a2aproject.sdk.client.A2AClient;
import org.a2aproject.sdk.model.Task;
```

Most IDEs can automate this migration via a project-wide find-and-replace of `io.a2a` → `org.a2aproject.sdk` in import statements, or by using the "Optimize Imports" / "Migrate" refactoring tools.

## Key Highlights in 1.0.0.Beta1

### Equal Transport Support

All three transports — JSON-RPC, gRPC, and HTTP+JSON/REST — are now fully supported and considered equal. Simply add the dependency for the transport you need (`a2a-java-sdk-reference-jsonrpc`, `a2a-java-sdk-reference-grpc`, or `a2a-java-sdk-reference-rest`) and the SDK will work seamlessly regardless of your choice.

### Structured Error Codes and Details

A2A error types now carry structured error codes and details, making error handling more precise and easier to integrate with observability tooling. Error responses also now use the correct `data` field name (previously `details`) in JSON/HTTP responses, in alignment with the specification.

### HTTP Caching Headers for Agent Card

The Agent Card endpoint now returns proper HTTP caching headers, allowing clients and proxies to cache agent metadata efficiently and reduce unnecessary round trips.

### `DataPart.fromJson()` Factory Method

A new `DataPart.fromJson()` factory method makes it straightforward to construct a `DataPart` from a raw JSON string, without requiring prior deserialization.

### Flexible HTTP Client Support

The SDK defaults to a `JdkA2AHttpClient` backed by the standard JDK `HttpClient`, suitable for any Java environment. The `HttpClient` builder also supports constructing a `JdkA2AHttpClient` from a prebuilt `HttpClient` instance, enabling advanced configuration scenarios such as custom SSL contexts or connection pool tuning.

When running on Quarkus, you can instead use the `VertxA2AHttpClient`, which is backed by the Vert.x HTTP client for better integration with Quarkus's reactive engine, connection management, and observability features.

## Bug Fixes and Stability

- Terminal-state tasks now correctly reject further operations with `UnsupportedOperationError`
- Race conditions in `EventConsumer` and the SSE transport have been resolved
- `PushNotificationNotSupportedError` now returns HTTP 501 as required by the specification
- POST requests without a `Content-Type` header are now accepted when the body is empty
- Tenant and `protocolVersion` are now correctly preserved in the `ClientBuilder`

## What's Next: Road to 1.0.0.GA

Before the 1.0.0.GA release, we plan to introduce the following:

- **Cross-version protocol support**: the SDK will be able to handle both version 1.0.0 and 0.3.0 of the A2A Protocol simultaneously, making it easier to interoperate with agents built against earlier versions of the specification.
- **Specification micro-update tracking**: as the A2A Specification evolves with patch-level updates, the SDK will follow those changes promptly to remain fully conformant.

## Contributors

This release was made possible by the work of seven contributors. A special thank you to our four first-time contributors:

- [@brucearctor](https://github.com/brucearctor)
- [@maff](https://github.com/maff)
- [@Lirons01](https://github.com/Lirons01)
- [@yyy9942](https://github.com/yyy9942)

And to the returning contributors:

- [@jmesnil](https://github.com/jmesnil)
- [@kabir](https://github.com/kabir)
- [@ehsavoie](https://github.com/ehsavoie)

Thank you all for your code, reviews, and feedback!

## Resources

- [Release Notes on GitHub](https://github.com/a2aproject/a2a-java/releases/tag/1.0.0.Beta1)
- [Maven Central](https://central.sonatype.com/artifact/org.a2aproject.sdk/a2a-java-sdk-parent/1.0.0.Beta1)
- [JavaDoc](https://javadoc.io/doc/org.a2aproject.sdk/)
- [A2A Specification](https://a2a-protocol.org/v1.0.0/specification/)

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are an A2A Java SDK user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/a2aproject/a2a-java/issues); \* craft some code and [push a PR](https://github.com/a2aproject/a2a-java/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on [Discord](https://discord.com/channels/1362108044737253548/1384238827090219138);
