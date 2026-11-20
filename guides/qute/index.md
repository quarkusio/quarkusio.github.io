# Qute templating engine

Qute is a templating engine developed specifically for Quarkus. Reflection usage is minimized to reduce the size of native images. The API combines both the imperative and the non-blocking reactive style of coding. In development mode, all files located in `src/main/resources/templates` are monitored for changes, and modifications become visible immediately. Furthermore, we aim to detect most template issues at build time. In this guide, you will learn how to easily render templates in your application.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `qute-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/qute-quickstart).

<a id="serving-templates"></a>
## Serving Qute templates via HTTP

If you want to serve your templates via HTTP:

1. The Qute Web extension allows you to directly serve via HTTP templates located in `src/main/resources/templates/pub/`. In that case you don't need any Java code to "plug" the template, for example, the template `src/main/resources/templates/pub/foo.html` will be served from the paths `/foo` and `/foo.html` by default.
2. For finer control, you can combine it with Quarkus REST to control how your template will be served. All files located in the `src/main/resources/templates` directory and its subdirectories are registered as templates and can be injected in a REST resource.

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkiverse.qute.web</groupId>
    <artifactId>quarkus-qute-web</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkiverse.qute.web:quarkus-qute-web")
```

> [!NOTE]
> The Qute Web extension, while hosted in the Quarkiverse, is part of the Quarkus Platform and its version is defined in the Quarkus Platform BOM.

<a id="hello-qute-web"></a>
### Serving Hello World with Qute

Let's start with a Hello World template:

**src/main/resources/templates/pub/hello.html**

```
<h1>Hello {http:param('name', 'Quarkus')}!</h1> <1>
```

1. `{http:param('name', 'Quarkus')}` is an expression that is evaluated when the template is rendered (Quarkus is the default value).

> [!NOTE]
> Templates located in the `pub` directory are served via HTTP. This behavior is built-in, no controllers are needed. For example, the template `src/main/resources/templates/pub/foo.html` will be served from the paths `/foo` and `/foo.html` by default.

Once your application is running, you can open your browser and navigate to: <http://localhost:8080/hello?name=Martin>

For more information about Qute Web options, see the [Qute Web guide](https://docs.quarkiverse.io/quarkus-qute-web/dev/index.html).

<a id="hello-qute-rest"></a>
### Hello Qute and REST

For finer control, you can combine Qute Web with Quarkus REST (formerly RESTEasy Reactive) or the legacy RESTEasy Classic-based extension to control how your template will be served

Using the `quarkus-rest-qute` extension:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-qute</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-rest-qute")
```

A very simple text template:

**hello.txt**

```
Hello {name}! <1>
```

1. `{name}` is a value expression that is evaluated when the template is rendered.

Now let's inject the "compiled" template in the resource class.

**HelloResource.java**

```java
package org.acme.quarkus.sample;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Template;

@Path("hello")
public class HelloResource {

    @Inject
    Template hello; <1>

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public TemplateInstance get(@QueryParam("name") String name) {
        return hello.data("name", name); <2><3>
    }
}
```

1. If there is no `@Location` qualifier provided, the field name is used to locate the template. In this particular case, we're injecting a template with path `templates/hello.txt`.
2. `Template.data()` returns a new template instance that can be customized before the actual rendering is triggered. In this case, we put the name value under the key `name`. The data map is accessible during rendering.
3. Note that we don't trigger the rendering - this is done automatically by a special `ContainerResponseFilter` implementation provided by `quarkus-rest-qute`.

If your application is running, you can request the endpoint:

```shell
$ curl -w "\n" http://localhost:8080/hello?name=Martin
Hello Martin!
```

## Type-safe templates

There's an alternate way to declare your templates in your Java code, which relies on the following convention:

- Organize your template files in the `/src/main/resources/templates` directory, by grouping them into one directory per resource class. So, if your `FruitResource` class references two templates `apples` and `oranges`, place them at `/src/main/resources/templates/FruitResource/apples.txt`  and `/src/main/resources/templates/FruitResource/oranges.txt`. Grouping templates per resource class makes it easier to navigate to them.
- In each of your resource class, declare a `@CheckedTemplate static class Template {}` class within your resource class.
- Declare one `public static native TemplateInstance method();` per template file for your resource.
- Use those static methods to build your template instances.

Here's the previous example, rewritten using this style:

We'll start with a very simple template:

**HelloResource/hello.txt**

```
Hello {name}! <1>
```

1. `{name}` is a value expression that is evaluated when the template is rendered.

Now let's declare and use this template in the resource class.

**HelloResource.java**

```java
package org.acme.quarkus.sample;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.CheckedTemplate;

@Path("hello")
public class HelloResource {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance hello(String name); <1>
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public TemplateInstance get(@QueryParam("name") String name) {
        return Templates.hello(name); <2>
    }
}
```

1. This declares a template with path `templates/HelloResource/hello`.
2. `Templates.hello()` returns a new template instance that is returned from the resource method. Note that we don't trigger the rendering - this is done automatically by a special `ContainerResponseFilter` implementation provided by `quarkus-rest-qute`.

> [!NOTE]
> Once you have declared a `@CheckedTemplate` class, we will check that all its methods point to existing templates, so if you try to use a template from your Java code and you forgot to add it, we will let you know at build time :)

Keep in mind this style of declaration allows you to reference templates declared in other resources too:

**GreetingResource.java**

```java
package org.acme.quarkus.sample;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.TemplateInstance;

@Path("greeting")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public TemplateInstance get(@QueryParam("name") String name) {
        return HelloResource.Templates.hello(name);
    }
}
```

### Top-level type-safe templates

Naturally, if you want to declare templates at the top-level, directly in `/src/main/resources/templates/hello.txt`, for example, you can declare them in a top-level (non-nested) `Templates` class:

**HelloResource.java**

```java
package org.acme.quarkus.sample;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Template;
import io.quarkus.qute.CheckedTemplate;

@CheckedTemplate
public class Templates {
    public static native TemplateInstance hello(String name); <1>
}
```

1. This declares a template with path `templates/hello`.

## Template Parameter Declarations

If you declare a **parameter declaration** in a template then Qute attempts to validate all expressions that reference this parameter and if an incorrect expression is found the build fails.

Let's suppose we have a simple class like this:

**Item.java**

```java
public class Item {
    public String name;
    public BigDecimal price;
}
```

And we'd like to render a simple HTML page that contains the item name and price.

Let's start again with the template:

**ItemResource/item.html**

```html
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>{item.name}</title> <1>
</head>
<body>
    <h1>{item.name}</h1>
    <div>Price: {item.price}</div> <2>
</body>
</html>
```

1. This expression is validated. Try to change the expression to `{item.nonSense}` and the build should fail.
2. This is also validated.

Finally, let's create a resource class with type-safe templates:

**ItemResource.java**

```java
package org.acme.quarkus.sample;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Template;
import io.quarkus.qute.CheckedTemplate;

@Path("item")
public class ItemResource {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance item(Item item); <1>
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get(@PathParam("id") Integer id) {
        return Templates.item(service.findItem(id)); <2>
    }
}
```

1. Declare a method that gives us a `TemplateInstance` for `templates/ItemResource/item.html` and declare its `Item item` parameter so we can validate the template.
2. Make the `Item` object accessible in the template.

> [!NOTE]
> When the `--parameters` compiler argument is enabled, Quarkus REST may infer the parameter names from the method argument names, making the `@PathParam("id")` annotation optional in this case.

### Template parameter declaration inside the template itself

Alternatively, you can declare your template parameters in the template file itself.

Let's start again with the template:

**item.html**

```html
{@org.acme.Item item} <1>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>{item.name}</title> <2>
</head>
<body>
    <h1>{item.name}</h1>
    <div>Price: {item.price}</div>
</body>
</html>
```

1. Optional parameter declaration. Qute attempts to validate all expressions that reference the parameter `item`.
2. This expression is validated. Try to change the expression to `{item.nonSense}` and the build should fail.

Finally, let's create a resource class.

**ItemResource.java**

```java
package org.acme.quarkus.sample;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Template;

@Path("item")
public class ItemResource {

    @Inject
    ItemService service;

    @Inject
    Template item; <1>

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get(Integer id) {
        return item.data("item", service.findItem(id)); <2>
    }
}
```

1. Inject the template with path `templates/item.html`.
2. Make the `Item` object accessible in the template.

## Template Extension Methods

**Template extension methods** are used to extend the set of accessible properties of data objects.

Sometimes, you're not in control of the classes that you want to use in your template, and you cannot add methods to them. Template extension methods allow you to declare new methods for those classes that will be available from your templates just as if they belonged to the target class.

Let's keep extending on our simple HTML page that contains the item name, price and add a discounted price. The discounted price is sometimes called a "computed property". We will implement a template extension method to render this property easily. Let's update our template:

**HelloResource/item.html**

```html
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>{item.name}</title>
</head>
<body>
    <h1>{item.name}</h1>
    <div>Price: {item.price}</div>
    {#if item.price > 100} <1>
    <div>Discounted Price: {item.discountedPrice}</div> <2>
    {/if}
</body>
</html>
```

1. `if` is a basic control flow section.
2. This expression is also validated against the `Item` class and obviously there is no such property declared. However, there is a template extension method declared on the `TemplateExtensions` class - see below.

Finally, let's create a class where we put all our extension methods:

**TemplateExtensions.java**

```java
package org.acme.quarkus.sample;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class TemplateExtensions {

    public static BigDecimal discountedPrice(Item item) { <1>
        return item.price.multiply(new BigDecimal("0.9"));
    }
}
```

1. A static template extension method can be used to add "computed properties" to a data class. The class of the first parameter is used to match the base object and the method name is used to match the property name.

> [!NOTE]
> you can place template extension methods in every class if you annotate them with `@TemplateExtension` but we advise to keep them either grouped by target type, or in a single `TemplateExtensions` class by convention.

## Rendering Periodic Reports

The templating engine can also be very useful for rendering periodic reports. You'll need to add the `quarkus-scheduler` and `quarkus-qute` extensions first. In your `pom.xml` file, add:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-qute</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-scheduler</artifactId>
</dependency>
```

Let's suppose we have a `SampleService` bean whose `get()` method returns a list of samples.

**Sample.java**

```java
public class Sample {
    public boolean valid;
    public String name;
    public String data;
}
```

The template is simple:

**report.html**

```html
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Report {now}</title>
</head>
<body>
    <h1>Report {now}</h1>
    {#for sample in samples} <1>
      <h2>{sample.name ?: 'Unknown'}</h2> <2>
      <p>
      {#if sample.valid}
        {sample.data}
      {#else}
        <strong>Invalid sample found</strong>.
      {/if}
      </p>
    {/for}
</body>
</html>
```

1. The loop section makes it possible to iterate over iterables, maps and streams.
2. This value expression is using the [elvis operator](https://en.wikipedia.org/wiki/Elvis_operator) - if the name is null the default value is used.

**ReportGenerator.java**

```java
package org.acme.quarkus.sample;

import jakarta.inject.Inject;

import io.quarkus.qute.Template;
import io.quarkus.qute.Location;
import io.quarkus.scheduler.Scheduled;

public class ReportGenerator {

    @Inject
    SampleService service;

    @Location("reports/v1/report_01") <1>
    Template report;

    @Scheduled(cron="0 30 * * * ?") <2>
    void generate() {
        String result = report
            .data("samples", service.get())
            .data("now", java.time.LocalDateTime.now())
            .render(); <3>
        // Write the result somewhere...
    }
}
```

1. In this case, we use the `@Location` qualifier to specify the template path: `templates/reports/v1/report_01.html`.
2. Use the `@Scheduled` annotation to instruct Quarkus to execute this method on the half hour. For more information see the [Scheduler](/guides/scheduler/index.md) guide.
3. The `TemplateInstance.render()` method triggers rendering. Note that this method blocks the current thread.

## Qute Reference Guide

To learn more about Qute, please refer to the [Qute reference guide](/guides/qute-reference/index.md).

<a id="qute-configuration-reference"></a>
## Qute Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-qute_quarkus-qute-suffixes"></a> [`quarkus.qute.suffixes`](#quarkus-qute_quarkus-qute-suffixes) config_property_copy_button:+++quarkus.qute.suffixes+++[]<br><br>The list of suffixes used when attempting to locate a template file.<br><br>By default, `engine.getTemplate("foo")` would result in several lookups: `foo`, `foo.html`, `foo.txt`, etc.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUTE_SUFFIXES+++[] | list of string | `qute.html`, `qute.txt`, `html`, `txt` |
| [lock] <a id="quarkus-qute_quarkus-qute-content-types-file-suffix"></a> [`quarkus.qute.content-types."file-suffix"`](#quarkus-qute_quarkus-qute-content-types-file-suffix) config_property_copy_button:+++quarkus.qute.content-types."file-suffix"+++[]<br><br>The additional map of suffixes to content types. This map is used when working with template variants. By default, the `java.net.URLConnection#getFileNameMap()` is used to determine the content type of a template file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUTE_CONTENT_TYPES__FILE_SUFFIX_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-qute_quarkus-qute-type-check-excludes"></a> [`quarkus.qute.type-check-excludes`](#quarkus-qute_quarkus-qute-type-check-excludes) config_property_copy_button:+++quarkus.qute.type-check-excludes+++[]<br><br>The list of exclude rules used to intentionally ignore some parts of an expression when performing type-safe validation.<br><br>An element value must have at least two parts separated by dot. The last part is used to match the property/method name. The prepended parts are used to match the class name. The value `*` can be used to match any name.<br><br>Examples:<br><br>\* `org.acme.Foo.name` - exclude the property/method `name` on the `org.acme.Foo` class \* `org.acme.Foo.*` - exclude any property/method on the `org.acme.Foo` class \* `*.age` - exclude the property/method `age` on any class<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUTE_TYPE_CHECK_EXCLUDES+++[] | list of string |  |
| [lock] <a id="quarkus-qute_quarkus-qute-template-path-exclude"></a> [`quarkus.qute.template-path-exclude`](#quarkus-qute_quarkus-qute-template-path-exclude) config_property_copy_button:+++quarkus.qute.template-path-exclude+++[]<br><br>This regular expression is used to exclude template files found in template roots. Excluded templates are neither parsed nor validated during build and are not available at runtime.<br><br>The matched input is the file path relative from the root directory and the `/` is used as a path separator.<br><br>By default, the hidden files are excluded. The name of a hidden file starts with a dot.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUTE_TEMPLATE_PATH_EXCLUDE+++[] | [Pattern](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html) | `^\..*\|.*\/\..*$` |
| [lock] <a id="quarkus-qute_quarkus-qute-iteration-metadata-prefix"></a> [`quarkus.qute.iteration-metadata-prefix`](#quarkus-qute_quarkus-qute-iteration-metadata-prefix) config_property_copy_button:+++quarkus.qute.iteration-metadata-prefix+++[]<br><br>The prefix is used to access the iteration metadata inside a loop section.<br><br>A valid prefix consists of alphanumeric characters and underscores. Three special constants can be used:<br><br>\* `<alias_>` - the alias of an iterated element suffixed with an underscore is used, e.g. `item_hasNext` and `it_count` \* `<alias?>` - the alias of an iterated element suffixed with a question mark is used, e.g. `item?hasNext` and `it?count` \* `<none>` - no prefix is used, e.g. `hasNext` and `count`<br><br>By default, the `<alias_>` constant is set.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUTE_ITERATION_METADATA_PREFIX+++[] | string | `<alias_>` |
| [lock] <a id="quarkus-qute_quarkus-qute-escape-content-types"></a> [`quarkus.qute.escape-content-types`](#quarkus-qute_quarkus-qute-escape-content-types) config_property_copy_button:+++quarkus.qute.escape-content-types+++[]<br><br>The list of content types for which the `'`, `"`, `<`, `>` and `&` characters are escaped if a template variant is set.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUTE_ESCAPE_CONTENT_TYPES+++[] | list of string | `text/html`, `text/xml`, `application/xml`, `application/xhtml+xml` |
| [lock] <a id="quarkus-qute_quarkus-qute-default-charset"></a> [`quarkus.qute.default-charset`](#quarkus-qute_quarkus-qute-default-charset) config_property_copy_button:+++quarkus.qute.default-charset+++[]<br><br>The default charset of the templates files.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUTE_DEFAULT_CHARSET+++[] | [Charset](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/charset/Charset.html) | `UTF-8` |
| [lock] <a id="quarkus-qute_quarkus-qute-duplicit-templates-strategy"></a> [`quarkus.qute.duplicit-templates-strategy`](#quarkus-qute_quarkus-qute-duplicit-templates-strategy) config_property_copy_button:+++quarkus.qute.duplicit-templates-strategy+++[]<br><br>The strategy used when multiple templates with the same path are found in the application.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUTE_DUPLICIT_TEMPLATES_STRATEGY+++[] | tooltip:prioritize[If multiple templates with the same path are found then determine the highest priority value and eliminate all templates with lowest priority. If there is exactly one template remaining then use this template. Otherwise,opts=fail the build.  Templates from the root application archive have the priority `30`. Templates from other application archives have the priority `10`. Templates from build items can define any priority.], tooltip:fail[Fail the build if multiple templates with the same path are found.] | tooltip:prioritize[If multiple templates with the same path are found then determine the highest priority value and eliminate all templates with lowest priority. If there is exactly one template remaining then use this template. Otherwise,opts=fail the build.  Templates from the root application archive have the priority {@code 30}. Templates from other application archives have the priority {@code 10}. Templates from build items can define any priority.] |
| [lock] <a id="quarkus-qute_quarkus-qute-alt-expr-syntax"></a> [`quarkus.qute.alt-expr-syntax`](#quarkus-qute_quarkus-qute-alt-expr-syntax) config_property_copy_button:+++quarkus.qute.alt-expr-syntax+++[]<br><br>Use the equals character as a command to identify an output expression in all templates from the application template root, i.e. `src/main/resources/templates`.<br><br>By default, no special command is used, i.e. the syntax for an output expression looks like `{foo.name}`. If set to `true` then the equals command is used instead. The alternative syntax looks like `{=foo.name}`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUTE_ALT_EXPR_SYNTAX+++[] | boolean | `false` |
| [lock] <a id="quarkus-qute_quarkus-qute-dev-mode-no-restart-templates"></a> [`quarkus.qute.dev-mode.no-restart-templates`](#quarkus-qute_quarkus-qute-dev-mode-no-restart-templates) config_property_copy_button:+++quarkus.qute.dev-mode.no-restart-templates+++[]<br><br>By default, a template modification results in an application restart that triggers build-time validations.<br><br>This regular expression can be used to specify the templates for which the application is not restarted. I.e. the templates are reloaded and only runtime validations are performed.<br><br>The matched input is the template path that starts with a template root, and the `/` is used as a path separator. For example, `templates/foo.html`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUTE_DEV_MODE_NO_RESTART_TEMPLATES+++[] | [Pattern](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html) |  |
| [lock] <a id="quarkus-qute_quarkus-qute-test-mode-record-rendered-results"></a> [`quarkus.qute.test-mode.record-rendered-results`](#quarkus-qute_quarkus-qute-test-mode-record-rendered-results) config_property_copy_button:+++quarkus.qute.test-mode.record-rendered-results+++[]<br><br>By default, the rendering results of injected and type-safe templates are recorded in the managed `RenderedResults` which is registered as a CDI bean.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUTE_TEST_MODE_RECORD_RENDERED_RESULTS+++[] | boolean | `true` |
| [lock] <a id="quarkus-qute_quarkus-qute-debug-enabled"></a> [`quarkus.qute.debug.enabled`](#quarkus-qute_quarkus-qute-debug-enabled) config_property_copy_button:+++quarkus.qute.debug.enabled+++[]<br><br>Enables or disables the Qute debug mode. This feature is experimental.<br><br>When enabled, Qute templates can be debugged directly at runtime. This includes the ability to:<br><br>\* Set breakpoints inside templates \* Inspect the stack trace of visited template nodes during rendering \* Evaluate expressions in the current Qute context<br><br>This mode is intended for development and troubleshooting purposes.<br><br>**Default value:** `true`<br><br>Example configuration:<br><br>```<br>quarkus.qute.debug.enabled = false<br>```<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUTE_DEBUG_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-qute_quarkus-qute-property-not-found-strategy"></a> [`quarkus.qute.property-not-found-strategy`](#quarkus-qute_quarkus-qute-property-not-found-strategy) config_property_copy_button:+++quarkus.qute.property-not-found-strategy+++[]<br><br>The strategy used when a standalone expression evaluates to a "not found" value at runtime and the `quarkus.qute.strict-rendering` config property is set to `false`<br><br>This strategy is never used when evaluating section parameters, e.g. `{#if foo.name}`. In such case, it's the responsibility of the section to handle this situation appropriately.<br><br>By default, the `NOT_FOUND` constant is written to the output. However, in the development mode the `PropertyNotFoundStrategy#THROW_EXCEPTION` is used by default, i.e. when the strategy is not specified.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUTE_PROPERTY_NOT_FOUND_STRATEGY+++[] | tooltip:default[Output the `NOT_FOUND` constant.], tooltip:noop[No operation - no output.], tooltip:throw-exception[Throw a `TemplateException`.], tooltip:output-original[Output the original expression string,opts=e.g. `++{++foo.name++}++`.] |  |
| <a id="quarkus-qute_quarkus-qute-remove-standalone-lines"></a> [`quarkus.qute.remove-standalone-lines`](#quarkus-qute_quarkus-qute-remove-standalone-lines) config_property_copy_button:+++quarkus.qute.remove-standalone-lines+++[]<br><br>Specify whether the parser should remove standalone lines from the output. A standalone line is a line that contains at least one section tag, parameter declaration, or comment but no expression and no non-whitespace character.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUTE_REMOVE_STANDALONE_LINES+++[] | boolean | `true` |
| <a id="quarkus-qute_quarkus-qute-strict-rendering"></a> [`quarkus.qute.strict-rendering`](#quarkus-qute_quarkus-qute-strict-rendering) config_property_copy_button:+++quarkus.qute.strict-rendering+++[]<br><br>If set to `true` then any expression that is evaluated to a `Results.NotFound` value will always result in a `TemplateException` and the rendering is aborted.<br><br>Note that the `quarkus.qute.property-not-found-strategy` config property is completely ignored if strict rendering is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUTE_STRICT_RENDERING+++[] | boolean | `true` |
| <a id="quarkus-qute_quarkus-qute-timeout"></a> [`quarkus.qute.timeout`](#quarkus-qute_quarkus-qute-timeout) config_property_copy_button:+++quarkus.qute.timeout+++[]<br><br>The global rendering timeout in milliseconds. It is used if no `timeout` template instance attribute is set.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUTE_TIMEOUT+++[] | long | `10000` |
| <a id="quarkus-qute_quarkus-qute-use-async-timeout"></a> [`quarkus.qute.use-async-timeout`](#quarkus-qute_quarkus-qute-use-async-timeout) config_property_copy_button:+++quarkus.qute.use-async-timeout+++[]<br><br>If set to `true` then the timeout should also be used for asynchronous rendering methods, such as `TemplateInstance#createUni()` and `TemplateInstance#renderAsync()`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUTE_USE_ASYNC_TIMEOUT+++[] | boolean | `true` |
