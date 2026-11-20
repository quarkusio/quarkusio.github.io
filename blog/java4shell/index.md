# Java4Shell (CVE-2026-114938): Status and mitigation for Quarkus users

> [!IMPORTANT]
> This post is updated as the situation evolves.
>
> **Last updated:** 20 November 2026, 08:00UTC

## TL;DR / Current status

- Quarkus is **not** affected. The JDK is.
- Native executables built with Mandrel are **not** affected.
- IBM Semeru (OpenJ9) is **not** affected.
- Builder images (Mandrel and GraalVM CE): **patched** (workaround applied). Re-pull floating tags. Update pinned tags. See [Builder images (Mandrel and GraalVM CE)](#builder-images).
- UBI Java images (UBI 8, UBI 9 and UBI 10): **fixed** on 19 November 2026. Rebuild your container images on the fixed base image, or apply the `-XX:-UseBASE64Intrinsics` workaround. See [JVM in a container, UBI Java runtime image](#ubi-images).
- The UBI 9 `1.23` images (used by default by Quarkus 3.27) are **not** fixed and will not be. Switch to `1.24`.
- JVM on bare metal: if your JDK is patched, you are fine. Otherwise, add `-XX:-UseBASE64Intrinsics` as workaround.

| Deployment model | Status | Action |
| --- | --- | --- |
| JVM on bare metal | Depends on your JDK | Upgrade the JDK, or add `-XX:-UseBASE64Intrinsics` |
| JVM in a container, UBI Java runtime image | ✅ Fixed, rebuild required | Repull the base image, rebuild your container image, and redeploy, or add `-XX:-UseBASE64Intrinsics` to `JAVA_OPTS_APPEND`\` |
| Native executable (Mandrel) | ✅ Not affected | None |
| Builder images (Mandrel, GraalVM CE) | ✅ Patched | Re-pull floating tags, update pinned tags |

## The vulnerability

CVE-2026-114938, aka "Java4Shell", is a memory corruption bug in the OpenJDK Base64 decoder. The JVM uses a CPU-optimized implementation (an *intrinsic*) to decode Base64. A crafted Base64 input makes it write past its buffer. Researchers demonstrated memory disclosure and remote code execution.

Exploitation requires **all** of the following:

- An unpatched OpenJDK 17.0.6+, 21, 25 or 27 build (all vendors: Temurin, Red Hat, Corretto, Zulu, etc.). IBM Semeru is not affected: it uses the OpenJ9 JVM, not HotSpot, and does not include the vulnerable intrinsic.
- An **x86-64** CPU exposing **AVX-512 VBMI** (on Linux: `avx512f`, `avx512bw`, `avx512vbmi` and `bmi2` in `/proc/cpuinfo`). ARM64 is not affected.
- Base64 intrinsics enabled (the default).
- The application decodes attacker-controlled Base64 data. Almost every HTTP application does decode Base64: Basic auth headers, JWTs, request payloads.

Oracle rates it CVSS 3.1 8.1. No exploitation in the wild is known at the time of writing.

References:

- [Tenable FAQ](https://www.tenable.com/blog/java4shell-cve-2026-114938-frequently-asked-questions-about-openjdk-rce-vulnerability)
- [Keycloak advisory](https://www.keycloak.org/2026/11/java4shell-vulnerability-cve-2026-114938)

## Is Quarkus affected?

No. The bug lives in the JDK, not in Quarkus. Upgrading Quarkus does not fix it. Fixing or mitigating the JDK does.

If you are deploying a Quarkus application (in JVM mode) in a container, you are affected if your base image contains a vulnerable JDK. So, make sure you update your base image, and, if it's pointing to a floating tag, repull the image before rebuilding the container.

## Mitigation per deployment model

### The workaround: `-XX:-UseBASE64Intrinsics`

The `-XX:-UseBASE64Intrinsics` flag disables the vulnerable intrinsic. The JVM falls back to the standard Java implementation of the `Base64` decoder, which is memory-safe. The decoded output is the same. Only the speed changes: decoding is slower, which rarely matters outside Base64-heavy workloads.

### JVM on bare metal

You run your Quarkus application with a JDK installed on the host.

- If your JDK vendor shipped a fixed build and you installed it: you are fine.
- Otherwise: add `-XX:-UseBASE64Intrinsics` to the JVM options.

```shell
java -XX:-UseBASE64Intrinsics -jar target/quarkus-app/quarkus-run.jar
```

Or, if you use `JAVA_OPTS` in your start scripts:

```shell
export JAVA_OPTS="$JAVA_OPTS -XX:-UseBASE64Intrinsics"
```

Remove the flag once a fixed JDK is deployed.

<a id="ubi-images"></a>
### JVM in a container, UBI Java runtime image

This is the Quarkus recommended setup (`Dockerfile.jvm`, `registry.access.redhat.com/ubi9/openjdk-<version>-runtime` or `registry.access.redhat.com/ubi10/openjdk-<version>-runtime`).

The UBI Java images were **fixed** on 19 November 2026. The floating tags (`1.23` for UBI 8, `1.24` for UBI 9 and UBI 10) now point to builds shipping a fixed JDK:

| Image | Floating tag | Fixed build | Release date (UTC) | JDK |
| --- | --- | --- | --- | --- |
| `registry.access.redhat.com/ubi8/openjdk-17-runtime` | `1.23` | `1.23-7` | 2026-11-19 10:44 | 17.0.21.1 |
| `registry.access.redhat.com/ubi8/openjdk-21-runtime` | `1.23` | `1.23-7` | 2026-11-19 10:48 | 21.0.13.1 |
| `registry.access.redhat.com/ubi9/openjdk-17-runtime` | `1.24` | `1.24-6` | 2026-11-19 10:54 | 17.0.21.1 |
| `registry.access.redhat.com/ubi9/openjdk-21-runtime` | `1.24` | `1.24-5` | 2026-11-19 10:51 | 21.0.13.1 |
| `registry.access.redhat.com/ubi9/openjdk-25-runtime` | `1.24` | `1.24-5` | 2026-11-19 10:55 | 25.0.5.1 |
| `registry.access.redhat.com/ubi10/openjdk-21-runtime` | `1.24` | `1.24-21` | 2026-11-19 12:51 | 21.0.13.1 |
| `registry.access.redhat.com/ubi10/openjdk-25-runtime` | `1.24` | `1.24-21` | 2026-11-19 09:48 | 25.0.5.1 |

#### Rebuild your container images

Your existing container images still contain the vulnerable JDK. Fixed base images do not change images you already built, nor containers already running. You have to **rebuild** your container images on top of the fixed base image, then **redeploy** them.

Docker and Podman reuse the base image cached locally. Make sure the build uses the fixed one:

- With a `Dockerfile`: build with `docker build --pull` (or `podman build --pull=newer`), or run `docker pull <base image>` before building, including when the build is run by the Quarkus Docker or Podman extensions.
- With Jib: the base image tag is resolved against the registry at each build, so the next build uses the fixed image.
- If you pinned the base image to a build tag (e.g. `1.24-3`) or a digest (`@sha256:...`), update it to the fixed build listed above.

To check that an image ships a fixed JDK:

```shell
docker run --rm --entrypoint java <your image> -version
```

The JDK version must be at least 17.0.21.1, 21.0.13.1 or 25.0.5.1. Vulnerable images show 17.0.21, 21.0.13 or 25.0.5 (without the trailing `.1`), or older.

#### Quarkus 3.27: switch to the `1.24` images

Quarkus 3.27 uses the UBI 9 `1.23` images by default (`registry.access.redhat.com/ubi9/openjdk-<version>-runtime:1.23`, in the generated `Dockerfile.jvm` and as the default base image of the container image extensions). This stream is no longer maintained: it was **not** fixed and will not be.

If you are using Quarkus 3.27, switch to the `1.24` images:

- In your `Dockerfile.jvm`: `FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24` (or `openjdk-17-runtime:1.24`).
- With Jib: `quarkus.jib.base-jvm-image=registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24` (or `openjdk-17-runtime:1.24`).
- With OpenShift S2I builds: `quarkus.openshift.base-jvm-image=registry.access.redhat.com/ubi9/openjdk-21:1.24` (or `openjdk-17:1.24`).

#### Workaround, if you cannot rebuild yet

If you cannot rebuild and redeploy your images right away, add `-XX:-UseBASE64Intrinsics` to `JAVA_OPTS_APPEND`. It can be set at deployment time, without rebuilding the image. Use `JAVA_OPTS_APPEND`, not `JAVA_OPTS`: overriding `JAVA_OPTS` drops the defaults computed by the image.

In your `Dockerfile.jvm`:

```dockerfile
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager -XX:-UseBASE64Intrinsics"
```

Or at deployment time, without rebuilding the image (Kubernetes / OpenShift):

```yaml
env:
  - name: JAVA_OPTS_APPEND
    value: "-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager -XX:-UseBASE64Intrinsics"
```

If you use the Quarkus Kubernetes extension:

```properties
quarkus.kubernetes.env.vars.java-opts-append=-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager -XX:-UseBASE64Intrinsics
```

### Native executable compiled with Mandrel

Not affected. A native executable does not run on the HotSpot JVM and does not use the vulnerable intrinsic. No action needed.

<a id="builder-images"></a>
### Builder images (Mandrel and GraalVM CE)

These images let you build a native executable without installing Mandrel or GraalVM (`quarkus.native.container-build=true`).

The resulting native executables are **not affected**, whatever image version built them.

#### Why patch the builder images?

The `native-image` tool is a Java application. It runs on the JDK shipped in the builder image. That JDK is affected. The build only processes your own code and dependencies, so the risk is low. Still, we do not want to ship images with an unprotected vulnerable JDK. So we applied the workaround to the builder images.

#### Patched images

- [`ubi10-quarkus-mandrel-builder-image`](https://quay.io/repository/quarkus/ubi10-quarkus-mandrel-builder-image?tab=tags) (used by default by Quarkus)
- [`ubi9-quarkus-mandrel-builder-image`](https://quay.io/repository/quarkus/ubi9-quarkus-mandrel-builder-image?tab=tags)
- [`ubi-quarkus-mandrel-builder-image`](https://quay.io/repository/quarkus/ubi-quarkus-mandrel-builder-image?tab=tags) (deprecated)
- [`ubi10-quarkus-graalvmce-builder-image`](https://quay.io/repository/quarkus/ubi10-quarkus-graalvmce-builder-image?tab=tags)
- [`ubi9-quarkus-graalvmce-builder-image`](https://quay.io/repository/quarkus/ubi9-quarkus-graalvmce-builder-image?tab=tags)
- [`ubi-quarkus-graalvmce-builder-image`](https://quay.io/repository/quarkus/ubi-quarkus-graalvmce-builder-image?tab=tags)

#### What changed

The images have been patched with the workaround ([quarkus-images#344](https://github.com/quarkusio/quarkus-images/pull/344)), followed by a second patch ([quarkus-images#345](https://github.com/quarkusio/quarkus-images/pull/345)) to fix the `native-image` tool invocation:

- On amd64 images bundling an affected JDK (21 before 21.0.13.1, 25 before 25.0.5.1), the image sets `JAVA_TOOL_OPTIONS=-XX:-UseBASE64Intrinsics`.
- arm64 images are not affected and are unchanged.
- Once the images bundle a fixed JDK, the workaround is dropped automatically.

Your build logs now show `Picked up JAVA_TOOL_OPTIONS: -XX:-UseBASE64Intrinsics`. This is expected.

If you override `JAVA_TOOL_OPTIONS` when running the builder image, you drop the workaround. Keep `-XX:-UseBASE64Intrinsics` in your value.

To get the patched image:

- Floating tags (`jdk-21`, `jdk-25`): re-pull the image. Docker and Podman do not re-pull automatically.

  ```shell
  docker pull quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21
  docker pull quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-25
  ```

  Use `podman pull` with the same arguments if you use Podman. Or set `quarkus.native.container-runtime-options=--pull=always`.

- Pinned tags: update to a patched tag:
  - JDK 21: `23.1.13.0-Final-java21-2026-11-18`
  - JDK 25: `25.0.5.0-Final-java25-2026-11-18`

To check that your local image is patched (amd64):

```shell
docker run --rm --entrypoint printenv quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21 JAVA_TOOL_OPTIONS
docker run --rm --entrypoint printenv quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-25 JAVA_TOOL_OPTIONS
```

Expected output: `-XX:-UseBASE64Intrinsics`.

## Verify the mitigation

Do not trust the environment variable alone. Check the running JVM:

```shell
jcmd <pid> VM.flags -all | grep UseBASE64Intrinsics
```

Expected output:

```
bool UseBASE64Intrinsics = false {product} {command line}
```

The origin at the end depends on how the flag is set: `{command line}` for `JAVA_OPTS_APPEND` or the `java` command, `{environment}` for `JAVA_TOOL_OPTIONS`. What matters is `UseBASE64Intrinsics = false`.

In a container, run it with `kubectl exec` or `docker exec`. The Quarkus process is usually PID 1.

## Acknowledgements

Thanks to [@zakkak](https://github.com/zakkak), [@Karm](https://github.com/Karm) and [@gsmet](https://github.com/gsmet) for their help assessing the impact and patching the images.

## Updates

- **20 November 2026, 08:00 UTC**: Highlight the need to re-pull the base image before rebuilding the container image.
- **19 November 2026, 21:45 UTC**: The UBI 8, UBI 9 and UBI 10 Java images are fixed. Rebuild your container images on the fixed base images. Quarkus 3.27 users have to switch to the UBI 9 `1.24` images.
- **19 November 2026, 09:00 UTC**: Explain the impact on native executables and builder images.
- **18 November 2026, 19:00 UTC**: Mandrel builder images (`jdk-21`, `jdk-25`) required a second patch at 17:30 UTC.
- **18 November 2026, 18:00 UTC**: Initial publication. Mandrel builder images (`jdk-21`, `jdk-25`) patched at 13:15 UTC.
