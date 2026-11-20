# Quarkus MCP Server: The First Java Server SDK to Support Streamable HTTP!

The Model Context Protocol (MCP) is taking the developer world by storm, and now, with its latest spec update: Streamable HTTP support has arrived!

We're thrilled to announce that Quarkus MCP Server is the very first Java-based MCP server SDK to embrace this innovation, making it easier than ever for you to build, experiment, and deploy MCP-powered solutions—wherever you need them.

[Quarkus MCP Server 1.2](https://github.com/quarkiverse/quarkus-mcp-server/releases/tag/1.2.0) now supports Streamable HTTP alongside `stdio` and `SSE` transports. This enables new possibilities for connecting your MCP servers to mobile apps and cloud services. While the implementation is fully functional, some advanced features like [Resumability and Redelivery](https://modelcontextprotocol.io/specification/2025-03-26/basic/transports#resumability-and-redelivery) are planned for future releases.

## Why Streamable HTTP Matters

Streamable HTTP is the approach MCP spec have taken for real-time, efficient, and scalable communication between clients and servers. It opens the door to new integrations and user experiences, especially for platforms and devices where traditional transports like SSE or stdio aren't ideal.

And now, thanks to Quarkus MCP Server, Java developers are at the forefront of this evolution. Whether you're building AI assistants, developer tools, or next-gen chatbots, Streamable HTTP gives you the flexibility to reach more users, faster.

## Easy upgrade

Ready to try it out? Just update your Maven dependency to the latest Quarkus MCP Server SSE transport:

```xml <dependency> <groupId>io.quarkiverse.mcp</groupId> <artifactId>quarkus-mcp-server-sse</artifactId> <version>1.2.0</version> </dependency> ```

That's it! You're now equipped to serve Streamable HTTP from your Java MCP server.

Want to see how to write your own MCP server? Check out our previous post: [Introducing MCP Servers](https://quarkus.io/blog/mcp-server/).

## Quarkus MCP Servers: Power and Simplicity

The [Quarkus MCP Servers project](https://github.com/quarkiverse/quarkus-mcp-servers) brings a suite of ready-to-use MCP servers, all built on Quarkus. With version 1.0.0.CR4, streamable HTTP support is baked in—no extra configuration required. We just updated the dependency, and it was ready to go!

To enable Streamable HTTP, simply launch any server in Quarkus MCP Servers with the `--sse` flag:

```
jbang jvminsight@mcp-java --sse
```

## Connecting Clients

The default URL for Streamable HTTP is:

[http://\<your-ip>:8080/mcp/](<http://%3Cyour-ip%3E:8080/mcp/>)

(For SSE, use `http://<your-ip>:8080/mcp/sse` as before.)

While Streamable HTTP is still new, some pioneering clients already support it. Notably, the open source iOS app [ChatMCP](https://github.com/daodao97/chatmcp") (available on [TestFlight](https://testflight.apple.com/join/dCXksFJV)) and a non-open source version on the [iOS App Store](https://apps.apple.com/dk/app/chatmcp/id6745196560) both work seamlessly with MCP and support or even require Streamable HTTP.

Here's a quick demo of ChatMCP in action with the jvminsight server:

[6GomKEMucYs](6GomKEMucYs)

## Kotlin: Lightweight and Fun

Quarkus supports both Java and Kotlin, giving you flexibility in how you build your MCP servers. Want to experiment? Here's a playful example of a Kotlin MCP server you can run instantly with JBang. It fetches a random image from <https://picsum.photos/> and returns it as a base64-encoded image, as the MCP spec requires.

``\`kotlin ///usr/bin/env jbang "$0" "$@" ; exit $?

import io.quarkiverse.mcp.server.\* import java.net.URL import java.util.Base64.getEncoder import kotlin.io.readBytes

class demo {

@Tool(description = "Get a random picture") fun randomimage(@ToolArg(description = "seed for randomness") seed: String, @ToolArg(description = "width", defaultValue = "300") width: Int, @ToolArg(description = "height", defaultValue = "300") height : Int): ImageContent {

val image = URL("<https://picsum.photos/seed/>$seed/$width/$height").readBytes()

return ImageContent( getEncoder().encodeToString(image), "image/jpeg" ) } } ``\`

Save this as `demo.kt` and run it with:

```
jbang demo.kt --sse
```

You can now use the `randomimage` tool in ChatMCP or any other MCP client that supports Streamable HTTP. It's that easy—and a great way to start experimenting!

## Conclusion

Streamable HTTP is an important step for the MCP ecosystem, and Quarkus MCP Server is putting Java developers in the driver's seat. Whether you're building tools, bots, or entirely new experiences, now's the perfect time to dive in and see what you can create.

We can't wait to see what you build. Try it out, share your feedback, and help shape the future of MCP — powered by Quarkus!

Have fun!
