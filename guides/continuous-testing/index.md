# Continuous testing

Learn how to use continuous testing in your Quarkus Application.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

- The completed greeter application from the [Getting Started Guide](/guides/getting-started/index.md)

## Introduction

Quarkus supports continuous testing, where tests run immediately after code changes have been saved. This allows you to get instant feedback on your code changes. Quarkus detects which tests cover which code, and uses this information to only run the relevant tests when code is changed.

## Solution

Start the [Getting Started](/guides/getting-started/index.md) application (or any other application) using:

**CLI**

```bash
quarkus dev
```

**Maven**

```bash
./mvnw quarkus:dev
```

**Gradle**

```bash
./gradlew --console=plain quarkusDev
```

Quarkus will start in development mode as normal, but down the bottom of the screen you should see the following:

```
--
Tests paused, press [r] to resume, [h] for more options>
```

Press `r` and the tests will start running. You should see the status change down the bottom of the screen as they are running, and it should finish with:

```
--
Tests all passed, 2 tests were run, 0 were skipped. Tests took 1470ms.
Press [r] to re-run, [v] to view full results, [p] to pause, [h] for more options>
```

> [!NOTE]
> If you want continuous testing to start automatically you can set `quarkus.test.continuous-testing=enabled` in `application.properties`. If you don't want it at all you can change this to `disabled`.

Now you can start making changes to your application. Go into the `GreetingResource` and change the hello endpoint to return `"hello world"`, and save the file. Quarkus should immediately re-run the test, and you should get output similar to the following:

```
2021-05-11 14:21:34,338 ERROR [io.qua.test] (Test runner thread) Test GreetingResourceTest#testHelloEndpoint() failed
: java.lang.AssertionError: 1 expectation failed.
Response body doesn't match expectation.
Expected: is "hello"
  Actual: hello world

	at io.restassured.internal.ValidatableResponseImpl.body(ValidatableResponseImpl.groovy)
	at org.acme.getting.started.GreetingResourceTest.testHelloEndpoint(GreetingResourceTest.java:21)


--
Test run failed, 2 tests were run, 1 failed, 0 were skipped. Tests took 295ms
Press [r] to re-run, [v] to view full results, [p] to pause, [h] for more options>
```

Change it back and the tests will run again.

## Controlling Continuous Testing

There are various hotkeys you can use to control continuous testing. Pressing `h` will display the following list of commands:

```
The following commands are available:
[r] - Re-run all tests
[f] - Re-run failed tests
[b] - Toggle 'broken only' mode, where only failing tests are run (disabled)
[v] - Print failures from the last test run
[p] - Pause tests
[o] - Toggle test output (disabled)
[i] - Toggle instrumentation based reload (disabled)
[l] - Toggle live reload (enabled)
[s] - Force restart
[h] - Display this help
[q] - Quit
```

These are explained below:

**\[r\] - Re-run all tests**\
This will re-run every test

**\[f\] - Re-run failed tests**\
This will re-run every failing test

**\[b\] - Toggle 'broken only' mode, where only failing tests are run**\
Broken only mode will only run tests that have previously failed, even if other tests would normally be affected by a code change. This can be useful if you are modifying code that is used by lots of tests, but you only want to focus on debugging the failing one.

**\[v\] - Print failures from the last test run**\
Prints the failures to the console again, this can be useful if there has been lots of console output since the last run.

**\[p\] - Pause tests**\
Temporarily stops running tests. This can be useful if you are making lots of changes, and don't want feedback until they are all done.

**\[o\] - Toggle test output**\
By default test output is filtered and not displayed on the console, so that test output and dev mode output is not interleaved. Enabling test output will print output to the console when tests are run. Even when output is disabled the filtered output is saved and can be viewed in the Dev UI.

**\[i\] - Toggle instrumentation based reload**\
This is not directly related to testing, but allows you to toggle instrumentation based reload. This will allow live reload to avoid a restart if a change does not affect the structure of a class, which gives a faster reload and allows you to keep state.

**\[l\] - Toggle live reload**\
This is not directly related to testing, but allows you to turn live reload on and off.

**\[s\] - Force restart**\
This will force a scan for changed files, and will perform a live reload with and changes. Note that even if there are no changes the application will still restart. This will still work even if live reload is disabled.

## Continuous Testing Without Dev Mode

It is possible to run continuous testing without starting dev mode. This can be useful if dev mode will interfere with your tests (e.g. running wiremock on the same port), or if you only want to develop using tests. To start continuous testing mode, run `mvn quarkus:test` or `gradle quarkusTest`.

> [!NOTE]
> The Dev UI is not available when running in continuous testing mode, as this is provided by dev mode.

## Selecting Tests to Run

The configuration properties `quarkus.test.include-pattern` and `quarkus.test.exclude-pattern` can be used to select which tests to run. They are regular expressions matched against the fully qualified class name of the test class. If `include-patterns` is configured, `exclude-patterns` is ignored.

Alternatively, an approach native to the build system may be used. In Maven, that is the `-Dtest=\...` system property, while in Gradle, it is the `--tests \...` command line option. These options, when used with `maven quarkus:[dev|test]` or `gradle quarkus[Dev|Test]`, work just like they work with `mvn test` or `gradle test`, except that they filter the set of tests executed during continuous testing. When these options are used, the `quarkus.test.[include|exclude]-pattern` configuration is ignored.

### Maven

The `-Dtest=\...` system property selects given test(s) for continuous testing. The format of this configuration property is the same as the Maven Surefire `-Dtest=\...` [format](https://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html#test). Specifically: it is a comma (`,`) separated list of globs of class file paths and/or method names. Each glob can potentially be prefixed with an exclamation mark (`!`), which makes it an exclusion filter instead of an inclusion filter. Exclusions have higher priority than inclusions. The class file path glob is separated from the method name glob by the hash sign (`#`) and multiple method name globs may be present, separated by the plus sign (`+`).

For example:

- `Basic*`: all classes starting with `Basic`
- `???Test`: all classes named with 3 arbitrary characters followed by `Test`
- `!Unstable*`: all classes except classes starting with `Unstable`
- `pkg/**/Ci*leTest`: all classes in the package `pkg` and subpackages, starting with `Ci` and ending with `leTest`
- `*Test#test*One+testTwo?????`: all classes ending with `Test`, and in them, only methods starting with `test` and ending with `One`, or starting with `testTwo` and followed by 5 arbitrary characters
- `#fast*+slowTest`: all classes, and in them, only methods starting with `fast` or methods named `slowTest`

Note that the syntax `%regex[\...]` and `%ant[\...]` is *NOT* supported.

### Gradle

The `--tests \...` command line option selects given test(s) for continuous testing. The format is the same as the `gradle test --tests \...` [format](https://docs.gradle.org/current/userguide/java_testing.html#test_filtering). Specifically: the option can be passed multiple times, and each item is a simple pattern for the test class name and optionally a method name. When the pattern starts with an upper case letter, it matches a simple name of the class; otherwise, it matches a fully qualified name of the class. After the class name, separated by a period (`.`), a method name pattern may be included. The only wildcard character supported is `\*`, which matches an arbitrary number of characters. Note that `*` is based purely on text, it doesn't pay extra attention to package delimiters.

For example:

- `com.example.Basic*`: all classes in package `com.example` starting with `Basic`
- `MyTest*`: all classes whose simple name starts with `MyTest`
- `\*.pkg.Test*`: all classes in package `pkg` (regardless of the parent packages) starting with `Test`
- `MyTest.test*`: all classes whose simple name is `MyTest`, and in them, only methods starting with `test`
- `com.example.IntegTest.fast*`: the class `com.example.IntegTest`, and in it, only methods starting with `fast`

## Multi-module Projects

Note that continuous testing supports multi-module projects, so tests in modules other than the application can still be run when files are changed. The modules that are run can be controlled using config as listed below.

This is enabled by default, and can be disabled via `quarkus.test.only-test-application-module=true`.

## Configuring Continuous Testing

Continuous testing supports multiple configuration options that can be used to limit the tests that are run, and to control the output. The configuration properties are shown below:

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-core_quarkus-test-continuous-testing"></a> [`quarkus.test.continuous-testing`](#quarkus-core_quarkus-test-continuous-testing) config_property_copy_button:+++quarkus.test.continuous-testing+++[]<br><br>If continuous testing is enabled.<br><br>The default value is 'paused', which will allow you to start testing from the console or the Dev UI, but will not run tests on startup.<br><br>If this is set to 'enabled' then testing will start as soon as the application has started.<br><br>If this is 'disabled' then continuous testing is not enabled, and can't be enabled without restarting the application.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_CONTINUOUS_TESTING+++[] | `paused`, `enabled`, `disabled` | `paused` |
| [lock] <a id="quarkus-core_quarkus-test-display-test-output"></a> [`quarkus.test.display-test-output`](#quarkus-core_quarkus-test-display-test-output) config_property_copy_button:+++quarkus.test.display-test-output+++[]<br><br>If output from the running tests should be displayed in the console.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_DISPLAY_TEST_OUTPUT+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-test-class-orderer"></a> [`quarkus.test.class-orderer`](#quarkus-core_quarkus-test-class-orderer) config_property_copy_button:+++quarkus.test.class-orderer+++[]<br><br>The FQCN of the JUnit `ClassOrderer` to use. If the class cannot be found, it fallbacks to JUnit default behaviour which does not set a `ClassOrderer` at all.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_CLASS_ORDERER+++[] | string |  |
| [lock] <a id="quarkus-core_quarkus-test-include-tags"></a> [`quarkus.test.include-tags`](#quarkus-core_quarkus-test-include-tags) config_property_copy_button:+++quarkus.test.include-tags+++[]<br><br>Tags that should be included for continuous testing. This supports JUnit Tag Expressions.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_INCLUDE_TAGS+++[] | list of string |  |
| [lock] <a id="quarkus-core_quarkus-test-exclude-tags"></a> [`quarkus.test.exclude-tags`](#quarkus-core_quarkus-test-exclude-tags) config_property_copy_button:+++quarkus.test.exclude-tags+++[]<br><br>Tags that should be excluded by default with continuous testing.<br><br>This is ignored if include-tags has been set.<br><br>Defaults to 'slow'.<br><br>This supports JUnit Tag Expressions.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_EXCLUDE_TAGS+++[] | list of string | `slow` |
| [lock] <a id="quarkus-core_quarkus-test-include-pattern"></a> [`quarkus.test.include-pattern`](#quarkus-core_quarkus-test-include-pattern) config_property_copy_button:+++quarkus.test.include-pattern+++[]<br><br>Tests that should be included for continuous testing. This is a regular expression and is matched against the test class name (not the file name).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_INCLUDE_PATTERN+++[] | string |  |
| [lock] <a id="quarkus-core_quarkus-test-exclude-pattern"></a> [`quarkus.test.exclude-pattern`](#quarkus-core_quarkus-test-exclude-pattern) config_property_copy_button:+++quarkus.test.exclude-pattern+++[]<br><br>Tests that should be excluded with continuous testing. This is a regular expression and is matched against the test class name (not the file name).<br><br>This is ignored if include-pattern has been set.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_EXCLUDE_PATTERN+++[] | string | `.*\.IT[^.]+\|.*IT\|.*ITCase` |
| [lock] <a id="quarkus-core_quarkus-test-include-engines"></a> [`quarkus.test.include-engines`](#quarkus-core_quarkus-test-include-engines) config_property_copy_button:+++quarkus.test.include-engines+++[]<br><br>Test engine ids that should be included for continuous testing.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_INCLUDE_ENGINES+++[] | list of string |  |
| [lock] <a id="quarkus-core_quarkus-test-exclude-engines"></a> [`quarkus.test.exclude-engines`](#quarkus-core_quarkus-test-exclude-engines) config_property_copy_button:+++quarkus.test.exclude-engines+++[]<br><br>Test engine ids that should be excluded by default with continuous testing.<br><br>This is ignored if include-engines has been set.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_EXCLUDE_ENGINES+++[] | list of string |  |
| [lock] <a id="quarkus-core_quarkus-test-flat-class-path"></a> [`quarkus.test.flat-class-path`](#quarkus-core_quarkus-test-flat-class-path) config_property_copy_button:+++quarkus.test.flat-class-path+++[]<br><br>Changes tests to use the 'flat' ClassPath used in Quarkus 1.x versions.<br><br>This means all Quarkus and test classes are loaded in the same ClassLoader, however it means you cannot use continuous testing.<br><br>Note that if you find this necessary for your application then you may also have problems running in development mode, which cannot use a flat class path.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_FLAT_CLASS_PATH+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-test-integration-test-profile"></a> [`quarkus.test.integration-test-profile`](#quarkus-core_quarkus-test-integration-test-profile) config_property_copy_button:+++quarkus.test.integration-test-profile+++[]<br><br>The profile to use when testing using `@QuarkusIntegrationTest`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_INTEGRATION_TEST_PROFILE+++[] | string | `prod` |
| [lock] <a id="quarkus-core_quarkus-test-profile"></a> [`quarkus.test.profile`](#quarkus-core_quarkus-test-profile) config_property_copy_button:+++quarkus.test.profile+++[]<br><br>A comma separated list of profiles (dev, test, prod or custom profiles) to use when testing using @QuarkusTest<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_PROFILE+++[] | list of string | `test` |
| [lock] <a id="quarkus-core_quarkus-test-profile-tags"></a> [`quarkus.test.profile.tags`](#quarkus-core_quarkus-test-profile-tags) config_property_copy_button:+++quarkus.test.profile.tags+++[]<br><br>The tags this profile is associated with. When the `quarkus.test.profile.tags` System property is set (its value is a comma separated list of strings) then Quarkus will only execute tests that are annotated with a `@TestProfile` that has at least one of the supplied (via the aforementioned system property) tags.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_PROFILE_TAGS+++[] | list of string |  |
| [lock] <a id="quarkus-core_quarkus-test-container-network"></a> [`quarkus.test.container.network`](#quarkus-core_quarkus-test-container-network) config_property_copy_button:+++quarkus.test.container.network+++[]<br><br>Controls the container network to be used when @QuarkusIntegration needs to launch the application in a container. This setting only applies if Quarkus does not need to use a shared network - which is the case if DevServices are used when running the test.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_CONTAINER_NETWORK+++[] | string |  |
| [lock] <a id="quarkus-core_quarkus-test-container-additional-exposed-ports-host-port"></a> [`quarkus.test.container.additional-exposed-ports."host-port"`](#quarkus-core_quarkus-test-container-additional-exposed-ports-host-port) config_property_copy_button:+++quarkus.test.container.additional-exposed-ports."host-port"+++[]<br><br>Set additional ports to be exposed when @QuarkusIntegration needs to launch the application in a container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_CONTAINER_ADDITIONAL_EXPOSED_PORTS__HOST_PORT_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-core_quarkus-test-container-labels-label-name"></a> [`quarkus.test.container.labels."label-name"`](#quarkus-core_quarkus-test-container-labels-label-name) config_property_copy_button:+++quarkus.test.container.labels."label-name"+++[]<br><br>A set of labels to add to the launched container<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_CONTAINER_LABELS__LABEL_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-core_quarkus-test-container-volume-mounts-host-path"></a> [`quarkus.test.container.volume-mounts."host-path"`](#quarkus-core_quarkus-test-container-volume-mounts-host-path) config_property_copy_button:+++quarkus.test.container.volume-mounts."host-path"+++[]<br><br>A set of volume mounts to add to the launched container<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_CONTAINER_VOLUME_MOUNTS__HOST_PATH_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-core_quarkus-test-rest-assured-enable-logging-on-failure"></a> [`quarkus.test.rest-assured.enable-logging-on-failure`](#quarkus-core_quarkus-test-rest-assured-enable-logging-on-failure) config_property_copy_button:+++quarkus.test.rest-assured.enable-logging-on-failure+++[]<br><br>Enable logging of both the request and the response if REST Assureds test validation fails<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_REST_ASSURED_ENABLE_LOGGING_ON_FAILURE+++[] | boolean | `true` |
| [lock] <a id="quarkus-core_quarkus-test-arg-line"></a> [`quarkus.test.arg-line`](#quarkus-core_quarkus-test-arg-line) config_property_copy_button:+++quarkus.test.arg-line+++[]<br><br>Additional launch parameters to be used when Quarkus launches the produced artifact for `@QuarkusIntegrationTest` When the artifact is a `jar`, this string is passed right after the `java` command. When the artifact is a `container`, this string is passed right after the `docker run` command. When the artifact is a `native binary`, this string is passed right after the native binary name.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_ARG_LINE+++[] | string |  |
| [lock] <a id="quarkus-core_quarkus-test-env-environment-variable-name"></a> [`quarkus.test.env."environment-variable-name"`](#quarkus-core_quarkus-test-env-environment-variable-name) config_property_copy_button:+++quarkus.test.env."environment-variable-name"+++[]<br><br>Additional environment variables to be set in the process that `@QuarkusIntegrationTest` launches.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_ENV__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-core_quarkus-test-wait-time"></a> [`quarkus.test.wait-time`](#quarkus-core_quarkus-test-wait-time) config_property_copy_button:+++quarkus.test.wait-time+++[]<br><br>Used in `@QuarkusIntegrationTest` to determine how long the test will wait for the application to launch<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_WAIT_TIME+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-core_quarkus-test) | `1M` |
| [lock] <a id="quarkus-core_quarkus-test-hang-detection-timeout"></a> [`quarkus.test.hang-detection-timeout`](#quarkus-core_quarkus-test-hang-detection-timeout) config_property_copy_button:+++quarkus.test.hang-detection-timeout+++[]<br><br>Configures the hang detection in @QuarkusTest. If no activity happens (i.e. no test callbacks are called) over this period then QuarkusTest will dump all threads stack traces, to help diagnose a potential hang.<br><br>Note that the initial timeout (before Quarkus has started) will only apply if provided by a system property, as it is not possible to read all config sources until Quarkus has booted.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_HANG_DETECTION_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-core_quarkus-test) | `10M` |
| [lock] <a id="quarkus-core_quarkus-test-type"></a> [`quarkus.test.type`](#quarkus-core_quarkus-test-type) config_property_copy_button:+++quarkus.test.type+++[]<br><br>The type of test to run, this can be either:<br><br>quarkus-test: Only runs `@QuarkusTest` annotated test classes unit: Only runs classes that are not annotated with `@QuarkusTest` all: Runs both, running the unit tests first<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_TYPE+++[] | `unit`, `quarkus-test`, `all` | `all` |
| [lock] <a id="quarkus-core_quarkus-test-only-test-application-module"></a> [`quarkus.test.only-test-application-module`](#quarkus-core_quarkus-test-only-test-application-module) config_property_copy_button:+++quarkus.test.only-test-application-module+++[]<br><br>If this is true then only the tests from the main application module will be run (i.e. the module that is currently running mvn quarkus:dev).<br><br>If this is false then tests from all dependency modules will be run as well.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_ONLY_TEST_APPLICATION_MODULE+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-test-include-module-pattern"></a> [`quarkus.test.include-module-pattern`](#quarkus-core_quarkus-test-include-module-pattern) config_property_copy_button:+++quarkus.test.include-module-pattern+++[]<br><br>Modules that should be included for continuous testing. This is a regular expression and is matched against the module groupId:artifactId.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_INCLUDE_MODULE_PATTERN+++[] | string |  |
| [lock] <a id="quarkus-core_quarkus-test-exclude-module-pattern"></a> [`quarkus.test.exclude-module-pattern`](#quarkus-core_quarkus-test-exclude-module-pattern) config_property_copy_button:+++quarkus.test.exclude-module-pattern+++[]<br><br>Modules that should be excluded for continuous testing. This is a regular expression and is matched against the module groupId:artifactId.<br><br>This is ignored if include-module-pattern has been set.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_EXCLUDE_MODULE_PATTERN+++[] | string |  |
| [lock] <a id="quarkus-core_quarkus-test-enable-callbacks-for-integration-tests"></a> [`quarkus.test.enable-callbacks-for-integration-tests`](#quarkus-core_quarkus-test-enable-callbacks-for-integration-tests) config_property_copy_button:+++quarkus.test.enable-callbacks-for-integration-tests+++[]<br><br>If the test callbacks should be invoked for the integration tests (tests annotated with `@QuarkusIntegrationTest`).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_ENABLE_CALLBACKS_FOR_INTEGRATION_TESTS+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-test-integration-test-artifact-type"></a> [`quarkus.test.integration-test-artifact-type`](#quarkus-core_quarkus-test-integration-test-artifact-type) config_property_copy_button:+++quarkus.test.integration-test-artifact-type+++[]<br><br>Used to override the artifact type against which a `@QuarkusIntegrationTest` or `@QuarkusMainIntegrationTest` run. For example, if the application's artifact is a container build from a jar, this property could be used to test the jar instead of the container.<br><br>Allowed values are: jar, native<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TEST_INTEGRATION_TEST_ARTIFACT_TYPE+++[] | string |  |

<a id="duration-note-anchor-quarkus-core_quarkus-test"></a>

> [!NOTE]
> **About the Duration format**
>
> To write duration values, use the standard `java.time.Duration` format. See the [Duration#parse() Java API documentation](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html#parse(java.lang.CharSequence)) for more information.
>
> You can also use a simplified format, starting with a number:
>
> - If the value is only a number, it represents time in seconds.
> - If the value is a number followed by `ms`, it represents time in milliseconds.
>
> In other cases, the simplified format is translated to the `java.time.Duration` format for parsing:
>
> - If the value is a number followed by `h`, `m`, or `s`, it is prefixed with `PT`.
> - If the value is a number followed by `d`, it is prefixed with `P`.
