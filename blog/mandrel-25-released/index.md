# Mandrel 25 is Here!

We are thrilled to announce the official release of Mandrel 25! The beginning of the next LTS version of the Quarkus native builder.

Mandrel 25 is a downstream distribution of the [GraalVM 25](https://www.graalvm.org/release-notes/JDK_25) Community Edition. Mandrel's main goal is to provide a native-image release specifically tailored to support [Quarkus](https://quarkus.io).  The aim is to align the native-image capabilities from GraalVM with OpenJDK and Red Hat Enterprise Linux libraries to improve maintainability for native Quarkus applications. As such, Mandrel 25 is the same as GraalVM 25 native-image with some small differences. It's built from the same code-base!

<a id="what-is-new-25"></a>
## What's New in GraalVM 25?

While there are too many new features added in GraalVM 25 to name them all it's worth highlighting a few. GraalVM 25 includes enhanced support for Foreign Function and Memory API in native-image. It enables FFM API support for MacOSX Aarch64 and Linux Aarch64 (in addition to Linux x86_64). GraalVM 25 also enhances metadata support in native-image. It now throws subclasses of LinkageError when missing metadata is being detected at native image runtime. This way it enables users to handle missing metadata registrations in a more efficient way.

<a id="based-on-openjdk-25"></a>
### Mandrel 25 is Based on OpenJDK 25

Mandrel 25 is based on the new vanilla Eclipse Temurin 25 LTS release. Temurin is an OpenJDK distribution produced at Eclipse. Based on the next LTS release of OpenJDK, Mandrel 25 will be the next LTS version supporting Quarkus' native compilation capabilities for the Quarkus releases to come.

<a id="new-monitoring-features"></a>
### New Monitoring Features

GraalVM 25 also introduces several new features in the monitoring area of native-image that have been contributed by Red Hat/IBM. Some highlights in JDK Flight Recorder support ([JFR](https://www.graalvm.org/jdk25/reference-manual/native-image/debugging-and-diagnostics/JFR/)), and [native memory tracking](https://www.graalvm.org/jdk25/reference-manual/native-image/debugging-and-diagnostics/NMT/) (NMT) are:

- **JCMD support:** GraalVM 25 adds support for the [Java Diagnostic Command](https://www.graalvm.org/jdk25/reference-manual/native-image/debugging-and-diagnostics/jcmd/)  (JCMD) to native-image. This is useful to asynchronously request specific actions on the running native image. For example a thread dump or a JFR recording can be requested after the application has been started.
- **Added support for more JFR events:** Additional JFR events have been added, specifically tracking native memory when monitoring option NMT is enabled as well. Object allocation sampling with the jdk.ObjectAllocationSample event is now also supported. For a list of all supported JFR events see the [https://www.graalvm.org/jdk25/reference-manual/native-image/debugging-and-diagnostics/JFR/#built-in-events\[upstream](https://www.graalvm.org/jdk25/reference-manual/native-image/debugging-and-diagnostics/JFR/#built-in-events[upstream) documentation\].
- **Native Memory Tracking:** A new monitoring option for [https://www.graalvm.org/jdk25/reference-manual/native-image/debugging-and-diagnostics/NMT/\[tracking](https://www.graalvm.org/jdk25/reference-manual/native-image/debugging-and-diagnostics/NMT/[tracking) native memory\], NMT, has been added to native-image. This is useful to diagnose memory leaks.

<a id="getting-started-25"></a>
## Get Started with Mandrel 25 Today!

Ready to experience the power of Mandrel 25?

- **Download Mandrel 25:** You can download the latest version from the [https://github.com/graalvm/mandrel/releases/tag/mandrel-25.0.0.1-Final\[Mandrel](https://github.com/graalvm/mandrel/releases/tag/mandrel-25.0.0.1-Final[Mandrel)  release page\] on Github or install it using [sdkman](https://sdkman.io/) by running:

  ```bash
  sdk install java 25.0.0.1.r25-mandrel
  ```

- **Builder Image with Quarkus:** Alternatively start using the Mandrel 25 builder image for your quarkus application by specifying `-Dquarkus.native.builder-image=quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-25` to your maven build.

<a id="quarkus-compatibility-mandrel-25"></a>
## Quarkus Compatibility

It's recommended to use at least Quarkus release 3.27.0 when using the Mandrel 25 native builder. For older versions of Quarkus use the Mandrel 23.1 native builder.

<a id="graalvm-community-gratitude"></a>
## Shout Out to GraalVM Community

We are very grateful for the immense support from the upstream GraalVM community, especially the Oracle GraalVM team for stewarding and sponsoring the development of GraalVM as well as for accepting Mandrel under the GraalVM organization. Onward to more collaboration with the community as the next version of GraalVM unfolds!

We are incredibly excited about Mandrel 25 and will be making it the default native image generator in an upcoming Quarkus release. In the meantime, do let us know your experience of using Mandrel 25 with Quarkus!

You can share your feedback with us in Quarkus' [Zulip chat workspace](https://quarkusio.zulipchat.com/) or on [GitHub](https://github.com/quarkusio/quarkus/).
