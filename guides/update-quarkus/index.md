# Update projects to the latest Quarkus version

You can update or upgrade your Quarkus projects to the latest version of Quarkus by using an update command.

The update command primarily employs OpenRewrite recipes to automate updates for most project dependencies, source code, and documentation.

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered experimental.
>
> In *experimental* mode, early feedback is requested to mature the idea. There is no guarantee of stability nor long term presence in the platform until the solution matures. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

> [!IMPORTANT]
> The update command automates only a subset of the migration. Always review the [Migration Guides](https://github.com/quarkusio/quarkus/wiki/Migration-Guides) for your target version to identify any additional steps that require manual changes.

Post-update, if expected updates are missing, consider the following reasons:

- The recipe might not include a specific item in your project.
- Your project might use an extension that is incompatible with the latest Quarkus version.

## Prerequisites

To complete this guide, you need:

- Roughly 30 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

- A project based on Quarkus version 2.13 or later.

## Procedure

1. Create a working branch for your project by using your version control system.

1. To use the Quarkus CLI in the next step, [install the latest version of the Quarkus CLI](/guides/cli-tooling/index.md#installing-the-cli). Confirm the version number using `quarkus -v`.

1. Go to the project directory and update the project to the latest stream:

   .Using Quarkus CLI

   ```bash
   quarkus update
   ```

   Optional: To specify a particular stream, use the `--stream` option; for example: `--stream=3.2`

   .Using Maven

   ```bash
   ./mvnw io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:update -N
   ```

   Optional: To specify a particular stream, use the `-Dstream` option; for example: `-Dstream=3.2`

   1. Analyze the update command output for potential instructions and perform the suggested tasks if necessary.
   1. Use a diff tool to inspect all changes.
   1. Review the [Migration Guides](https://github.com/quarkusio/quarkus/wiki/Migration-Guides) for your target version and apply any additional steps not covered by the update command.
   1. Ensure the project builds without errors, all tests pass, and the application functions as required before deploying to production.

   ## Update recipes

   The OpenRewrite recipes used by the update command are maintained in the [Quarkus Updates](https://github.com/quarkusio/quarkus-updates) repository. If you find that a recipe is missing or incorrect, you can contribute improvements there. Refer to the [README.md](https://github.com/quarkusio/quarkus-updates/blob/main/README.md) and the [CONTRIBUTING.md](https://github.com/quarkusio/quarkus-updates/blob/main/CONTRIBUTING.md) guide for detailed instructions on how to write and test recipes.
