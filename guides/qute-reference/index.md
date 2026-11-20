# Qute reference guide

Qute is a templating engine designed specifically to meet the Quarkus needs. The usage of reflection is minimized to reduce the size of native images. The API combines both the imperative and the non-blocking reactive style of coding. In the development mode, all files located in the `src/main/resources/templates` folder are watched for changes and modifications are immediately visible in your application. Furthermore, Qute attempts to detect most of the template problems at build time and fail fast.

In this guide, you will find an [introductory example](#hello_world_example), the description of the [core features](#core_features) and [Quarkus integration](#quarkus_integration) details.

> [!NOTE]
> Qute is primarily designed as a Quarkus extension. It is possible to use it as a "standalone" library too. However, in such case some features are not available. In general, any feature mentioned under the [Quarkus Integration](#quarkus_integration) section is missing. Find more information about the limitations and possibilities in the [Qute Used as a Standalone Library](#standalone) section.

<a id="the_simplest_example"></a>
## The Simplest Example

The easiest way to try Qute is to use the convenient `io.quarkus.qute.Qute` class and call one of its `fmt()` static methods that can be used to format simple messages:

```java
import io.quarkus.qute.Qute;

Qute.fmt("Hello {}!", "Lucy"); <1>
// => Hello Lucy!

Qute.fmt("Hello {name} {surname ?: 'Default'}!", Map.of("name", "Andy")); <2>
// => Hello Andy Default!

Qute.fmt("<html>{header}</html>").contentType("text/html").data("header", "<h1>My header</h1>").render(); <3>
// <html>&lt;h1&gt;Header&lt;/h1&gt;</html> <4>

Qute.fmt("I am {#if ok}happy{#else}sad{/if}!", Map.of("ok", true)); <5>
// => I am happy!
```

1. The empty expression `{}` is a placeholder that is replaced with an index-based array accessor, i.e. `{data[0]}`.
2. You can provide a data map instead.
3. A builder-like API is available for more complex formatting requirements.
4. Note that for a "text/html" template the special chars are replaced with html entities by default.
5. You can use any [building block](#basic-building-blocks) in the template. In this case, the [If Section](#if_section) is used to render the appropriate part of the message based on the input data.

> [!TIP]
> In [Quarkus](#quarkus_integration), the engine used to format the messages is the same as the one injected by `@Inject Engine`. Therefore, you can make use of any Quarkus-specific integration feature such as [Template Extension Methods](#template_extension_methods), [Injecting Beans Directly In Templates](#injecting-beans-directly-in-templates) or even [Type-safe Message Bundles](#type-safe-message-bundles).

The format object returned by the `Qute.fmt(String)` method can be evaluated lazily and used e.g. as a log message:

```java
LOG.info(Qute.fmt("Hello {name}!").data("name", "Foo"));
// => Hello Foo! and the message template is only evaluated if the log level INFO is used for the specific logger
```

> [!NOTE]
> Please read the javadoc of the `io.quarkus.qute.Qute` class for more details.

<a id="hello_world_example"></a>
## Hello World Example

In this example, we would like to demonstrate the *basic workflow* when working with Qute templates. Let's start with a simple "hello world" example. We will always need some **template contents**:

**hello.html**

```html
<html>
  <p>Hello {name}! <1>
</html>
```

1. `{name}` is a value expression that is evaluated when the template is rendered.

Then, we will need to parse the contents into a **template definition** Java object. A template definition is an instance of `io.quarkus.qute.Template`.

If using Qute "standalone" you'll need to create an instance of `io.quarkus.qute.Engine` first. The `Engine` represents a central point for template management with dedicated configuration. Let's use the convenient builder:

```java
Engine engine = Engine.builder().addDefaults().build();
```

> [!TIP]
> In Quarkus, there is a preconfigured `Engine` available for injection - see [Quarkus Integration](#quarkus_integration).

Once we have an `Engine` instance we could parse the template contents:

```java
Template hello = engine.parse(helloHtmlContent);
```

> [!TIP]
> In Quarkus, you can simply inject the template definition. The template is automatically parsed and cached - see [Quarkus Integration](#quarkus_integration).

Finally, create a **template instance**, set the data and render the output:

```java
// Renders <html><p>Hello Jim!</p></html>
hello.data("name", "Jim").render(); <1><2>
```

1. `Template.data(String, Object)` is a convenient method that creates a template instance and sets the data in one step.
2. `TemplateInstance.render()` triggers a synchronous rendering, i.e. the current thread is blocked until the rendering is finished. However, there are also asynchronous ways to trigger the rendering and consume the results. For example, there is the `TemplateInstance.renderAsync()` method that returns `CompletionStage<String>` or `TemplateInstance.createMulti()` that returns Mutiny's `Multi<String>`.

So the workflow is simple:

1. Create the template contents (`hello.html`),
2. Parse the template definition (`io.quarkus.qute.Template`),
3. Create a template instance (`io.quarkus.qute.TemplateInstance`),
4. Render the output.

> [!TIP]
> The `Engine` is able to cache the template definitions so that it's not necessary to parse the contents again and again. In Quarkus, the caching is done automatically.

<a id="core_features"></a>
## Core Features

<a id="basic-building-blocks"></a>
### Basic Building Blocks

The dynamic parts of a template include comments, expressions, sections and unparsed character data.

**Comments**\
A comment starts with the sequence `{!` and ends with the sequence `!}`, e.g. `{! This is a comment !}`. Can be multiline and may contain expressions and sections: `{! {#if true} !}`. The content of a comment is completely ignored when rendering the output.

**Expressions**\
An [expression](#expressions) outputs an evaluated value. It consists of one or more parts. A part may represent simple properties: `{foo}`, `{item.name}`, and virtual methods: `{item.get(name)}`, `{name ?: 'John'}`. An expression may also start with a namespace: `{inject:colors}`.

**Sections**\
A [section](#sections) may contain static text, expressions and nested sections: `{#if foo.active}{foo.name}{/if}`. The name in the closing tag is optional: `{#if active}ACTIVE!{/}`. A section can be empty: `{#myTag image=true /}`. Some sections support optional end tags, i.e. if the end tag is missing then the section ends where the parent section ends. A section may also declare nested section blocks: `{#if item.valid} Valid. {#else} Invalid. {/if}` and decide which block to render.

**Unparsed Character Data**

  It is used to mark the content that should be rendered but *not parsed*. It starts with the sequence `\{|`  and ends with the sequence `|\}`: `\{| <script>if(true){alert('Qute is cute!')};</script> |\}`, and could be multi-line. + The number of `|` (pipe) characters in the opening delimiter can be increased to avoid conflicts with content that itself contains `|\}`. The closing delimiter must use the same number of pipes as the opening delimiter. For example, `\{||||` starts an unparsed block that only ends with `||||\}`, making it safe to include `|\}` in the content: `\{|||| content with |\} inside ||||\}`. +

  > [!NOTE]
  > Consecutive `|` characters immediately after `\{|` are now counted as part of the opening delimiter. For example, `\{||foo|\}` was previously interpreted as single-pipe unparsed character data with content `|foo`, but is now interpreted as a double-pipe opening that requires `||\}` to close.

  \+

  > [!WARNING]
  > Previously, unparsed character data could start with `{[` and end with `]}`. This syntax is now removed due to common collisions with constructs from other languages.

<a id="identifiers"></a>
### Identifiers and Tags

Identifiers are used in expressions and section tags. A valid identifier is a sequence of non-whitespace characters. However, users are encouraged to only use valid Java identifiers in expressions.

> [!TIP]
> You can use bracket notation if you need to specify an identifier that contains a dot, e.g. `{map['my.key']}`.

When parsing a template document the parser identifies all *tags*. A tag starts and ends with a curly bracket, e.g. `{foo}`. The content of a tag must start with:

- a digit, or
- an alphabet character, or
- underscore, or
- a built-in command: `#`, `!`, `@`, `/`.

If it does not start with any of the above it is ignored by the parser.

**Tag Examples**

```html
<html>
   <body>
   {_foo.bar} <1>
   {! comment !} <2>
   {  foo} <3>
   {{foo}} <4>
   {"foo":true} <5>
   </body>
</html>
```

1. Parsed: an expression that starts with underscore.
2. Parsed: a comment
3. Ignored: starts with whitespace.
4. Ignored: starts with `{`.
5. Ignored: starts with `"`.

> [!TIP]
> It is also possible to use escape sequences `\{` and `\}` to insert delimiters in the text. In fact, an escape sequence is usually only needed for the start delimiter, i.e. `\{foo}` will be rendered as `{foo}` (no parsing/evaluation will happen).

### Removing Standalone Lines From the Template

By default, the parser removes standalone lines from the template output. A **standalone line** is a line that contains at least one section tag (e.g. `{#each}` and `{/each}`), parameter declaration (e.g. `{@org.acme.Foo foo}`) or comment but no expression and no non-whitespace character. In other words, a line that contains no section tag or a parameter declaration is **not** a standalone line. Likewise, a line that contains an *expression* or a *non-whitespace character* is **not** a standalone line.

**Template Example**

```html
<html>
  <body>
     <ul>
     {#for item in items} <1>
       <li>{item.name} {#if item.active}{item.price}{/if}</li> <2>
 <3>
     {/for} <4>
     </ul>
   </body>
</html>
```

1. This is a standalone line and will be removed.
2. Not a standalone line - contains an expression and non-whitespace characters
3. Not a standalone line - contains no section tag/parameter declaration
4. This is a standalone line.

**Default Output**

```html
<html>
  <body>
     <ul>
       <li>Foo 100</li>

     </ul>
   </body>
</html>
```

> [!TIP]
> In Quarkus, the default behavior can be disabled by setting the property `quarkus.qute.remove-standalone-lines` to `false`. In this case, all whitespace characters from a standalone line will be printed to the output.

**Output with `quarkus.qute.remove-standalone-lines=false`**

```html
<html>
  <body>
     <ul>

       <li>Foo 100</li>


     </ul>
   </body>
</html>
```

<a id="expressions"></a>
### Expressions

An expression is evaluated and outputs the value. It has one or more parts, where each part represents either a property accessor (aka Field Access Expression) or a virtual method invocation (aka Method Invocation Expression).

When accessing the properties you can either use the dot notation or bracket notation. In the `object.property` (dot notation) syntax, the `property` must be a [valid identifier](#identifiers). In the `object[property_name]` (bracket notation) syntax, the `property_name` has to be a non-null [literal](#literals) value.

An expression can start with an optional namespace followed by a colon (`:`). A valid namespace consists of alphanumeric characters and underscores. Namespace expressions are resolved differently - see also [Resolution](#expression_resolution).

**Property Accessor Examples**

```
{name} <1>
{item.name} <2>
{item['name']} <3>
{global:colors} <4>
```

1. no namespace, one part: `name`
2. no namespace, two parts: `item`, `name`
3. equivalent to `{item.name}` but using the bracket notation
4. namespace `global`, one part: `colors`

A part of an expression can be a *virtual method* in which case the name can be followed by a list of comma-separated parameters in parentheses. A parameter of a virtual method can be either a nested expression or a [literal](#literals) value. We call these methods *"virtual"* because they do not have to be backed by a real Java method. You can learn more about virtual methods in the [following section](#virtual_methods).

**Virtual Method Example**

```
{item.getLabels(1)} <1>
{name or 'John'} <2>
```

1. no namespace, two parts - `item`, `getLabels(1)`, the second part is a virtual method with name `getLabels` and params `1`
2. infix notation that can be used for virtual methods with single parameter, translated to `name.or('John')`; no namespace, two parts - `name`, `or('John')`

An expression can also use a [literal](#literals) value as the base, followed by property accessors or virtual method invocations.

**Literal Base Expression Examples**

```
{='foo'.toUpperCase} <1>
{=1.intValue} <2>
{#let name=('foo'.toUpperCase)}{name}{/let} <3>
{name.replace('foo'.toUpperCase)} <4>
```

1. string literal as the base followed by a property accessor; outputs `FOO`
2. integer literal as the base followed by a property accessor; outputs `1`
3. in a [let](#let_section) section parameter, a literal base can be used directly inside parentheses
4. a literal base can also be used in nested expressions passed as parameters of virtual methods

> [!NOTE]
> In a top-level output expression, a literal base requires a special syntax prefix (such as `=`) configured via `ParserConfig`.

<a id="literals"></a>
#### Supported Literals

| Literal | Examples |
| --- | --- |
| boolean | `true`, `false` |
| null | `null` |
| string | `'value'`, `"string"` |
| integer | `1`, `-5` |
| long | `1l`, `-5L` |
| double | `1D`, `-5d` |
| float | `1f`, `-5F` |

<a id="expression_resolution"></a>
#### Resolution

When evaluating expressions a list of registered [value resolvers](#value-resolvers) is used. The first part of the expression is always resolved against the [current context object](#current_context_object). If no result is found for the first part, it's resolved against the parent context object (if available). For an expression that starts with a namespace the current context object is found using all the available ``NamespaceResolver``s. For an expression that does not start with a namespace the current context object is **derived from the position** of the tag. All other parts of an expression are resolved using all ``ValueResolver``s against the result of the previous resolution.

For example, expression `{name}` has no namespace and single part - `name`. The "name" will be resolved using all available value resolvers against the current context object. However, the expression `{global:colors}` has the namespace `global` and single part - `colors`. First, all available ``NamespaceResolver``s will be used to find the current context object. And afterwards value resolvers will be used to resolve "colors" against the context object found.

> [!TIP]
> Data passed to the template instance are always accessible using the `data` namespace. This could be useful to access data for which the key is overridden:
>
> ```html
> <html>
> {item.name} <1>
> <ul>
> {#for item in item.derivedItems} <2>
>   <li>
>   {item.name} <3>
>   is derived from
>   {data:item.name} <4>
>   </li>
> {/for}
> </ul>
> </html>
> ```
>
> 1. `item` is passed to the template instance as a data object.
> 2. Iterate over the list of derived items.
> 3. `item` is an alias for the iterated element.
> 4. Use the `data` namespace to access the `item` data object.

<a id="current_context_object"></a>
#### Current Context

If an expression does not specify a namespace, the *current context object* is derived from the position of the tag. By default, the current context object represents the data passed to the template instance. However, sections may change the current context object. A typical example is the [`let`](#let_section) section that can be used to define named local variables:

```html
{#let myParent=order.item.parent myPrice=order.price} <1>
  <h1>{myParent.name}</h1>
  <p>Price: {myPrice}</p>
{/let}
```

1. The current context object inside the section is the map of resolved parameters.

> [!NOTE]
> The current context can be accessed via the implicit binding `this`.

#### Built-in Resolvers

| Name | Description | Examples |
| --- | --- | --- |
| Elvis Operator: `?:` | Outputs the default value if the previous part cannot be resolved or resolves to `null`. | `{person.name ?: 'John'}`, `{person.name or 'John'}`, `{person.name.or('John')}` |
| `orEmpty` | Outputs an empty list if the previous part cannot be resolved or resolves to `null`. | `{pets.orEmpty.size}` outputs `0` if `pets` is not resolvable or `null` |
| Ternary Operator: `condition ? ifTrue : ifFalse` | Shorthand for if-then-else statement. Unlike in [If Section](#if_section) nested operators are not supported. | `{item.isActive ? item.name : 'Inactive item'}` outputs the value of `item.name` if `item.isActive` resolves to `true`. |
| Logical AND Operator: `&&` | Outputs `true` if both parts are not `falsy` as described in the [If Section](#if_section). The parameter is only evaluated if needed. | `{person.isActive && person.hasStyle}` |
| Logical OR Operator: `\|\|` | Outputs `true` if any of the parts is not `falsy` as described in the [If Section](#if_section). The parameter is only evaluated if needed. | `{person.isActive \|\| person.hasStyle}` |
| Equals Operator: `==`/`eq`/`is` | Outputs `true` if the base object is equal to the argument. | `{obj1 is obj2 ? 'Equal' : 'Inequal'}`, `{obj1 == obj2 ? 'Equal' : 'Inequal'}`, `{obj1.eq(obj2) ? 'Equal' : 'Inequal'}` |

> [!TIP]
> The condition in a ternary operator evaluates to `true` if the value is not considered `falsy` as described in [If Section](#if_section).

> [!NOTE]
> In fact, the operators are implemented as "virtual methods" that consume one parameter and can be used with infix notation. For example `{person.name or 'John'}` is translated to `{person.name.or('John')}` and `{item.isActive ? item.name : 'Inactive item'}` is translated to `{item.isActive.ifTruthy(item.name).or('Inactive item')}`

#### Arrays

You can iterate over elements of an array with [Loop Section](#loop_section). Moreover, it's also possible to get the length of the specified array and access the elements directly via an index value. Additionally, you can access the first/last `n` elements via the `take(n)/takeLast(n)` methods.

**Array Examples**

```html
<h1>Array of length: {myArray.length}</h1> <1>
<ul>
  <li>First: {myArray.0}</li> <2>
  <li>Second: {myArray[1]}</li> <3>
  <li>Third: {myArray.get(2)}</li> <4>
</ul>
<ol>
 {#for element in myArray}
 <li>{element}</li>
 {/for}
</ol>
First two elements: {#each myArray.take(2)}{it}{/each} <5>
```

1. Outputs the length of the array.
2. Outputs the first element of the array.
3. Outputs the second element of the array using the bracket notation.
4. Outputs the third element of the array via the virtual method `get()`.
5. Outputs the first two elements of the array.

#### Character Escapes

For HTML and XML templates the `'`, `"`, `<`, `>`, `&` characters are escaped by default if a corresponding template variant is set. For JSON templates the `"`, `\` and the control characters (`U+0000` through `U+001F`) are escaped by default if a corresponding template variant is set.

> [!NOTE]
> In Quarkus, a variant is set automatically for templates located in the `src/main/resources/templates`. By default, the `java.net.URLConnection#getFileNameMap()` is used to determine the content-type of a template file. The additional map of suffixes to content types can be set via `quarkus.qute.content-types`.

If you need to render the unescaped value:

1. Either use the `raw` or `safe` properties implemented as extension methods of the `java.lang.Object`,
2. Or wrap the `String` value in a `io.quarkus.qute.RawString`.

**HTML Example**

```html
<html>
<h1>{title}</h1> <1>
{paragraph.raw} <2>
</html>
```

1. `title` that resolves to `Expressions & Escapes` will be rendered as `Expressions &amp;amp; Escapes`
2. `paragraph` that resolves to `<p>My text!</p>` will be rendered as `<p>My text!</p>`

> [!TIP]
> By default, a template with one of the following content types is escaped: `text/html`, `text/xml`, `application/xml` and `application/xhtml+xml`. However, it's possible to extend this list via the `quarkus.qute.escape-content-types` configuration property.

**JSON Example**

```json
{
  "id": "{valueId.raw}", <1>
  "name": "{valueName}" <2>
}
```

1. `valueId` that resolves to `\nA12345` will be rendered as `\nA12345` that will result in an invalid JSON Object because of the new line inserted inside the string value for the attribute `id`.
2. `valueName` that resolves to `\tExpressions \n Escapes` will be rendered as `\\tExpressions \\n Escapes`.

<a id="virtual_methods"></a>
#### Virtual Methods

A virtual method is a **part of an expression** that looks like a regular Java method invocation. It's called "virtual" because it does not have to match the actual method of a Java class. In fact, like normal properties a virtual method is also handled by a value resolver. The only difference is that for virtual methods a value resolver consumes parameters that are also expressions.

**Virtual Method Example**

```html
<html>
<h1>{item.buildName(item.name,5)}</h1> <1>
</html>
```

1. `buildName(item.name,5)` represents a virtual method with name `buildName` and two parameters: `item.name` and `5` . The virtual method could be evaluated by a value resolver generated for the following Java class:

   ```java
   class Item {
      String buildName(String name, int age) {
         return name + ":" + age;
      }
   }
   ```


> [!NOTE]
> Virtual methods are usually evaluated by value resolvers generated for [@TemplateExtension methods](#template_extension_methods), [@TemplateData](#template_data) or classes used in [parameter declarations](#typesafe_expressions). However, a custom value resolver that is not backed by any Java class/method can be registered as well.

A virtual method with single parameter can be called using the infix notation:

**Infix Notation Example**

```html
<html>
<p>{item.price or 5}</p> <1>
</html>
```

1. `item.price or 5` is translated to `item.price.or(5)`.

Virtual method parameters can be "nested" virtual method invocations.

**Nested Virtual Method Example**

```html
<html>
<p>{item.subtractPrice(item.calculateDiscount(10))}</p> <1>
</html>
```

1. `item.calculateDiscount(10)` is evaluated first and then passed as an argument to `item.subtractPrice()`.

#### Evaluation of `CompletionStage` and `Uni` Objects

Objects that implement `java.util.concurrent.CompletionStage` and `io.smallrye.mutiny.Uni` are evaluated in a special way. If a part of an expression resolves to a `CompletionStage`, the resolution continues once this stage is completed and the next part of the expression (if any) is evaluated against the result of the completed stage. For example, if there is an expression `{foo.size}` and `foo` resolves to `CompletionStage<List<String>>` then `size` is resolved against the completed result, i.e. `List<String>`. If a part of an expression resolves to a `Uni`, a `CompletionStage` is first created from `Uni` using `Uni#subscribeAsCompletionStage()` and then evaluated as described above.

> [!IMPORTANT]
> Note that each `Uni#subscribeAsCompletionStage()` results in a new subscription. You might need to configure memoization of the `Uni` item or failure before it's used as template data, i.e. `myUni.memoize().indefinitely()`.

It can happen that a `CompletionStage` never completes or a `Uni` emits no item/failure. In this case, the rendering methods (such as `TemplateInstance#render()` and `TemplateInstance#createUni()`) fail after a specific timeout. The timeout can be specified as a template instance `timeout` attribute. If no `timeout` attribute is set the global rendering timeout is used.

> [!TIP]
> In Quarkus, the default timeout can be set via the `io.quarkus.qute.timeout` configuration property. If using Qute standalone then the `EngineBuilder#timeout()` method can be used.

> [!NOTE]
> In previous versions, only the `TemplateInstance#render()` method honored the timeout attribute. You can use the `io.quarkus.qute.useAsyncTimeout=false` config property to preserve the old behavior and take care of the timeout yourself, for example `templateInstance.createUtni().ifNoItem().after(Duration.ofMillis(500)).fail()`.

##### How to Identify a Problematic Part of the Template

It's not easy to find the problematic part of a template when a timeout occurs. You can set the `TRACE` level for the logger `io.quarkus.qute.nodeResolve` and try to analyze the log output afterwards.

**`application.properties` Example**

```properties
quarkus.log.category."io.quarkus.qute.nodeResolve".min-level=TRACE
quarkus.log.category."io.quarkus.qute.nodeResolve".level=TRACE
```

You should see the following pair of log messages for every expression and section used in a template:

```
TRACE [io.qua.qut.nodeResolve] Resolve {name} started: Template hello.html at line 8
TRACE [io.qua.qut.nodeResolve] Resolve {name} completed: Template hello.html at line 8
```

If a `completed` log message is missing then you have a good candidate to explore.

#### Missing Properties

It can happen that an expression may not be evaluated at runtime. For example, if there is an expression `{person.age}` and there is no property `age` declared on the `Person` class. The behavior differs based on whether the [Strict Rendering](#strict_rendering) is enabled or not.

If enabled then a missing property will always result in a `TemplateException` and the rendering is aborted. You can use *default values* and *safe expressions* in order to suppress the error.

If disabled then the special constant `NOT_FOUND` is written to the output by default.

> [!TIP]
> In Quarkus, it's possible to change the default strategy via the `quarkus.qute.property-not-found-strategy` as described in the [Configuration Reference](#configuration-reference).

> [!NOTE]
> Similar errors are detected at build time if [Type-safe Expressions](#typesafe_expressions) and [Type-safe Templates](#typesafe_templates) are used.

<a id="sections"></a>
### Sections

A section has a start tag that starts with `#`, followed by the name of the section such as `{#if}` and `{#each}`. It may be empty, i.e. the start tag ends with `/`: `{#myEmptySection /}`. Sections usually contain nested expressions and other sections. The end tag starts with `/` and contains the name of the section (optional): `{#if foo}Foo!{/if}` or `{#if foo}Foo!{/}`. Some sections support optional end tags, i.e. if the end tag is missing then the section ends where the parent section ends.

**`#let` Optional End Tag Example**

```html
{#if item.isActive}
  {#let price = item.price} <1>
  {price}
  // synthetic {/let} added here automatically
{/if}
// {price} cannot be used here!
```

1. Defines the local variable that can be used inside the parent `{#if}` section.

| Built-in section | Supports Optional End Tag |
| --- | --- |
| `{#for}` | ❌ |
| `{#if}` | ❌ |
| `{#when}` | ❌ |
| `{#let}` | ✅ |
| `{#with}` | ❌ |
| `{#include}` | ✅ |
| User-defined Tags | ❌ |
| `{#fragment}` | ❌ |
| `{#cached}` | ❌ |

<a id="sections_params"></a>
#### Parameters

A start tag can define parameters with optional names, e.g. `{#if item.isActive}` and `{#let foo=1 bar=false}`. Parameters are separated by one or more whitespaces. Names are separated from the values by the equals sign. Names and values can be prefixed and suffixed with any number of spaces, e.g. `{#let id='Foo'}` and `{#let id  = 'Foo'}` are equivalents where the name of the parameter is `id` and the value is `Foo`. Values can be grouped using parentheses, e.g. `{#let id=(item.id ?: 42)}` where the name is `id` and the value is `item.id ?: 42`. Sections can interpret parameter values in any way, e.g. take the value as is. However, in most cases, the parameter value is registered as an [expression](#expressions) and evaluated before use.

A section may contain several content **blocks**. The "main" block is always present. Additional/nested blocks also start with `#` and can have parameters too - `{#else if item.isActive}`. A section helper that defines the logic of a section can "execute" any of the blocks and evaluate the parameters.

**`#if` Section Example**

```
{#if item.name is 'sword'}
  It's a sword! <1>
{#else if item.name is 'shield'}
  It's a shield! <2>
{#else}
  Item is neither a sword nor a shield. <3>
{/if}
```

1. This is the main block.
2. Additional block.
3. Additional block.

<a id="loop_section"></a>
#### Loop Section

The loop section makes it possible to iterate over an instance of `Iterable`, `Iterator`, array, `Map` (element is a `Map.Entry`), `Stream`, `Integer`, `Long`, `int` and `long` (primitive value). A `null` parameter value results in a no-op.

This section has two flavors. The first one is using the name `each` and `it` is an implicit alias for the iteration element.

```
{#each items}
  {it.name} <1>
{/each}
```

1. `name` is resolved against the current iteration element.

The other form is using the name `for` and specifies the alias used to reference the iteration element:

```
{#for item in items} <1>
  {item.name}
{/for}
```

1. `item` is the alias used for the iteration element.

It's also possible to access the iteration metadata inside the loop via the following keys:

- `count` - 1-based index
- `index` - zero-based index
- `hasNext` - `true` if the iteration has more elements
- `isLast` - `true` if `hasNext == false`
- `isFirst` - `true` if `count == 1`
- `odd` - `true` if the element's count is odd
- `even` - `true` if the element's count is even
- `indexParity` - outputs `odd` or `even` based on the count value

However, the keys cannot be used directly. Instead, a prefix is used to avoid possible collisions with variables from the outer scope. By default, the alias of an iterated element suffixed with an underscore is used as a prefix. For example, the `hasNext` key must be prefixed with `it_` inside an `{#each}` section: `{it_hasNext}`.

**`each` Iteration Metadata Example**

```
{#each items}
  {it_count}. {it.name} <1>
  {#if it_hasNext}<br>{/if} <2>
{/each}
```

1. `it_count` represents one-based index.
2. `<br>` is only rendered if the iteration has more elements.

And must be used in a form of `{item_hasNext}` inside a `{#for}` section with the `item` element alias.

**`for` Iteration Metadata Example**

```
{#for item in items}
  {item_count}. {item.name} <1>
  {#if item_hasNext}<br>{/if} <2>
{/for}
```

1. `item_count` represents one-based index.
2. `<br>` is only rendered if the iteration has more elements.

> [!TIP]
> The iteration metadata prefix is configurable either via `EngineBuilder.iterationMetadataPrefix()` for standalone Qute or via the `quarkus.qute.iteration-metadata-prefix` configuration property in a Quarkus application. Three special constants can be used:
>
> 1. `<alias_>` - the alias of an iterated element suffixed with an underscore is used (default)
> 2. `<alias?>` - the alias of an iterated element suffixed with a question mark is used
> 3. `<none>` - no prefix is used

The `for` statement also works with integers, starting from 1. In the example below, considering that `total = 3`:

```
{#for i in total}
  {i}: ({i_count} {i_indexParity} {i_even})<br>
{/for}
```

And the output will be:

```
1: (1 odd false)
2: (2 even true)
3: (3 odd false)
```

A loop section may also define the `{#else}` block that is executed when there are no items to iterate:

```
{#for item in items}
  {item.name}
{#else}
  No items.
{/for}
```

<a id="if_section"></a>
#### If Section

The `{#if}` section represents a basic control flow section. The simplest possible version accepts a single parameter and renders the content if the condition is evaluated to `true`. A condition without an operator evaluates to `true` if the value is not considered `falsy`, i.e. if the value is not `null`, `false`, an empty collection, an empty map, an empty array, an empty string/char sequence, an empty `java.util.Optional`/`java.util.OptionalInt`/`java.util.OptionalLong`/`java.util.OptionalDouble` or a number equal to zero.

```html
{#if item.active}
  This item is active.
{/if}
```

You can also use the following operators in a condition:

| Operator | Aliases | Precedence | Example | Description |
| --- | --- | --- | --- | --- |
| logical complement | `!` | 4 | `{#if !item.active}{/if}` | Inverts the evaluated value. |
| greater than | `gt`, `>` | 3 | `{#if item.age > 43}This item is very old.{/if}` | Evaluates to `true` if `value1` is greater than `value2`. |
| greater than or equal to | `ge`, `>=` | 3 | `{#if item.price >= 100}This item is expensive.{/if}` | Evaluates to `true` if `value1` is greater than or equal to `value2`. |
| less than | `lt`, `<` | 3 | `{#if item.price < 100}This item is cheap.{/if}` | Evaluates to `true` if `value1` is less than `value2`. |
| less than or equal to | `le`, `\<=` | 3 | `{#if item.age <= 43}This item is young.{/if}` | Evaluates to `true` if `value1` is less than or equal to `value2`. |
| equals | `eq`, `==`, `is` | 2 | `{#if item.name eq 'Foo'}Foo item!{/if}` | Evaluates to `true` if `value1` is equal to `value2`. |
| not equals | `ne`, `!=` | 2 | `{#if item.name != 'Bar'}Not a Bar item!{/if}` | Evaluates to `true` if `value1` is not equal to `value2`. |
| logical AND (short-circuiting) | `&&`, `and` | 1 | `{#if item.price > 100 && item.isActive}Expensive and active item.{/if}` | Evaluates to `true` if both operands evaluate to `true`. |
| logical OR (short-circuiting) | `\|\|`, `or` | 1 | `{#if item.price > 100 \|\| item.isActive}Expensive or active item.{/if}` | Evaluates to `true` if one of the operands evaluates to `true`. |

For `>`, `>=`, `<`, and `\<=` the following rules are applied:

- Neither of the operands may be `null`.
- If both operands are of the same type that implements the `java.lang.Comparable` then the `Comparable#compareTo(T)` method is used to perform comparison.
- Otherwise, both operands are coerced to `java.math.BigDecimal` first and then the `BigDecimal#compareTo(BigDecimal)` method is used to perform comparison.

> [!NOTE]
> Types that support coercion include `BigInteger`, `Integer`, `Long`, `Double`, `Float` and `String`.

For `==` and `!=` the following rules are applied:

- Operands are first tested using the `java.util.Objects#equals(Object, Object)` method. If it returns `true` the operands are considered equal.
- Otherwise, if both operands are not `null` and at least one of them is an instance of `java.lang.Number`, then operands are coerced to `java.math.BigDecimal` and the `BigDecimal#compareTo(BigDecimal)` method is used to perform comparison.

Multiple conditions are also supported.

**Multiple conditions example**

```html
{#if item.age > 10 && item.price > 500}
  This item is very old and expensive.
{/if}
```

The default precedence rules (higher precedence wins) can be overridden by parentheses.

**Parentheses example**

```html
{#if (item.age > 10 || item.price > 500) && user.loggedIn}
  User must be logged in and item age must be > 10 or price must be > 500.
{/if}
```

You can also add any number of `else` blocks:

```html
{#if item.age > 10}
  This item is very old.
{#else if item.age > 5}
  This item is quite old.
{#else if item.age > 2}
  This item is old.
{#else}
  This item is not old at all!
{/if}
```

<a id="when_section"></a>
#### When Section

This section is similar to Java's `switch` or Kotlin's `when` constructs. It matches a *tested value* against all blocks sequentially until a condition is satisfied. The first matching block is executed. All other blocks are ignored (this behavior differs to the Java `switch` where a `break` statement is necessary).

**Example using the `when`/`is` name aliases**

```
{#when items.size}
  {#is 1} <1>
    There is exactly one item!
  {#is > 10} <2>
    There are more than 10 items!
  {#else} <3>
    There are 2 -10 items!
{/when}
```

1. If there is exactly one parameter it's tested for equality.
2. It is possible to use [an operator](#when_operators) to specify the matching logic. Unlike in the [If Section](#if_section) nested operators are not supported.
3. `else` is block is executed if no other block matches the value.

**Example using the `switch`/`case` name aliases**

```
{#switch person.name}
  {#case 'John'} <1>
    Hey John!
  {#case 'Mary'}
    Hey Mary!
{/switch}
```

1. `case` is an alias for `is`.

A tested value that resolves to an enum is handled specifically. The parameters of an `is`/`case` block are not evaluated as expressions but compared with the result of `toString()` invocation upon the tested value.

```
{#when machine.status}
  {#is ON}
    It's running. <1>
  {#is in OFF BROKEN}
    It's broken or OFF. <2>
{/when}
```

1. This block is executed if `machine.status.toString().equals("ON")`.
2. This block is executed if  `machine.status.toString().equals("OFF")` or `machine.status.toString().equals("BROKEN")`.

> [!NOTE]
> An enum constant is validated if the tested value has a type information available and resolves to an enum type.

The following operators are supported in `is`/`case` block conditions:

<a id="when_operators"></a>

| Operator | Aliases | Example |
| --- | --- | --- |
| not equal | `!=`, `not`, `ne` | `{#is not 10}`,`{#case != 10}` |
| greater than | `gt`, `>` | `{#case le 10}` |
| greater than or equal to | `ge`, `>=` | `{#is >= 10}` |
| less than | `lt`, `<` | `{#is < 10}` |
| less than or equal to | `le`, `\<=` | `{#case le 10}` |
| in | `in` | `{#is in 'foo' 'bar' 'baz'}` |
| not in | `ni`,`!in` | `{#is !in 1 2 3}` |

<a id="let_section"></a>
#### Let Section

This section allows you to define named local variables.

**Let**

```html
{#let myParent=order.item.parent isActive=false age=10 price=(order.price + 10)} <1><2>
  <h1>{myParent.name}</h1>
  Is active: {isActive}
  Age: {age}
  Price: {price}
{/let} <3>
```

1. The local variable is initialized with an expression that can also represent a [literal](#literals), i.e. `isActive=false` and `age=10`.
2. The infix notation is only supported if parentheses are used for grouping, e.g. `price=(order.price + 10)` is equivalent to `price=order.price.plus(10)`. A literal value can also be used as the first operand, e.g. `price=('$' + item.price)`.
3. Variables are not available outside the `let` section.

The variables are not available outside the defining `let` section. However, the end tag is optional, if missing then the section ends where the parent section ends.

**Let with optional end tag**

```html
<ul>
{#for item in items}
{#let price=item.price} <1>
   <li>{price}</li>
{! a synthetic {/let} is added here automatically !}
{/for}
</ul>
{price} --> BOOM! <2>
```

1. The local variable `price` is initialized with expression `item.price`.
2. Variable `price` is not available outside the `let` section.

If a key of a section parameter, such as the name of the local variable, ends with a `?`, then the local variable is only set if the key without the `?` suffix resolves to `null` or *"not found"*:

```html
{#let enabled?=true} <1><2>
  {#if enabled}ON{/if}
{/let}
```

1. `true` is effectively a *default value* that is only used if the parent scope does not define `enabled` already.
2. `enabled?=true` is a short version of `enabled=enabled.or(true)`.

This section tag is also registered under the `set` alias:

```html
{#set myParent=item.parent price=item.price}
  <h1>{myParent.name}</h1>
  <p>Price: {price}
{/set}
```

<a id="with_section"></a>
#### With Section

This section can be used to set the current context object. This could be useful to simplify the template structure:

```html
{#with item.parent}
  <h1>{name}</h1> <1>
  <p>{description}</p> <2>
{/with}
```

1. The `name` will be resolved against the `item.parent`.
2. The `description` will be also resolved against the `item.parent`.

> [!IMPORTANT]
> Note that the `with` section should not be used in [Type-safe Templates](#typesafe_templates) or templates that define [Type-safe Expressions](#typesafe_expressions). The reason is that it prevents Qute from validating the nested expressions. If possible, replace it with the `{#let}` section which declares an explicit binding:
>
> ```html
> {#let it=item.parent}
>   <h1>{it.name}</h1>
>   <p>{it.description}</p>
> {/let}
> ```

This section might also come in handy when we'd like to avoid multiple expensive invocations:

```html
{#with item.callExpensiveLogicToGetTheValue(1,'foo',bazinga)}
  {#if this is "fun"} <1>
    <h1>Yay!</h1>
  {#else}
    <h1>{this} is not fun at all!</h1>
  {/if}
{/with}
```

1. `this` is the result of `item.callExpensiveLogicToGetTheValue(1,'foo',bazinga)`. The method is only invoked once even though the result may be used in multiple expressions.

<a id="include_helper"></a>
#### Include Section

This section can be used to include another template and possibly override some parts of the template (see the *template inheritance* below).

**Simple Example**

```html
<html>
<head>
<meta charset="UTF-8">
<title>Simple Include</title>
</head>
<body>
  {#include foo limit=10 /} <1><2>
</body>
</html>
```

1. Include a template with id `foo`. The included template can reference data from the current context.
2. It's also possible to define optional parameters that can be used in the included template.

By default, the first unnamed parameter represents the id of a template that should be included. And it is taken as is. For example, `{#include bar/foo /}` includes a template with id `bar/foo`; i.e. `src/main/resources/templates/bar/foo.html` could be matched. However, it is also possible to supply the template id dynamically. Just add the `_id` parameter to the tag. In this case, the argument value of the `_id` parameter represents an expression that is resolved and the result represents the template id. For example, `{#include _id=bar.foo /}` means that `bar.foo` is first resolved and then the resulting template id is used.

*Template inheritance* makes it possible to reuse template layouts.

**Template "base"**

```html
<html>
<head>
<meta charset="UTF-8">
<title>{#insert title}Default Title{/}</title> <1>
</head>
<body>
  {#insert}No body!{/} <2>
</body>
</html>
```

1. `insert` sections are used to specify parts that could be overridden by a template that includes the given template.
2. An `insert` section may define the default content that is rendered if not overridden. If there is no name supplied then the main block of the relevant `{#include}` section is used.

**Template "detail"**

```html
{#include base} <1>
  {#title}My Title{/title} <2>
  <div> <3>
    My body.
  </div>
{/include}
```

1. `include` section is used to specify the extended template.
2. Nested blocks are used to specify the parts that should be overridden.
3. The content of the main block is used for an `{#insert}` section with no name parameter specified.

> [!NOTE]
> Section blocks can also define an optional end tag - `{/title}`.

<a id="user_tags"></a>
#### User-defined Tags

User-defined tags can be used to include a *tag template*, optionally pass some arguments and possibly override some parts of the template. Let's suppose we have a tag template called `itemDetail.html`:

```
{#if showImage} <1>
  {it.image} <2>
  {nested-content} <3>
{/if}
```

1. `showImage` is a named parameter.
2. `it` is a special key that is replaced with the first unnamed parameter of the tag.
3. (optional) `nested-content` is a special key that will be replaced by the content of the tag.

In Quarkus, all files from the `src/main/resources/templates/tags` are registered and monitored automatically. For Qute standalone, you need to put the parsed template under the name `itemDetail.html` and register a relevant `UserTagSectionHelper` to the engine:

```java
Engine engine = Engine.builder()
                   .addSectionHelper(new UserTagSectionHelper.Factory("itemDetail","itemDetail.html"))
                   .build();
engine.putTemplate("itemDetail.html", engine.parse("..."));
```

Then, we can call the tag like this:

```html
<ul>
{#for item in items}
  <li>
  {#itemDetail item showImage=true} <1>
    = <b>{item.name}</b> <2>
  {/itemDetail}
  </li>
{/for}
</ul>
```

1. `item` is resolved to an iteration element and can be referenced using the `it` key in the tag template.
2. Tag content injected using the `nested-content` key in the tag template.

By default, a tag template cannot reference the data from the parent context. Qute executes the tag as an *isolated* template, i.e. without access to the context of the template that calls the tag. However, sometimes it might be useful to change the default behavior and disable the isolation. In this case, just add `_isolated=false` or `_unisolated` argument to the call site, for example `{#itemDetail item showImage=true _isolated=false /}` or `{#itemDetail item showImage=true _unisolated /}`.

##### Arguments

Named arguments can be accessed directly in the tag template. However, the first argument does not need to define a name, and it can be accessed using the `it` alias. Furthermore, if an argument does not have a name defined and the value is a single identifier, such as `foo`, then the name is defaulted to the value identifier, e.g. `{#myTag foo /}` becomes `{#myTag foo=foo /}`. In other words, the argument value `foo` is resolved and can be accessed using `{foo}` in the tag template.

> [!NOTE]
> If an argument does not have a name and the value is a single word string literal , such as `"foo"`, then the name is defaulted and quotation marks are removed, e.g. `{#myTag "foo" /}` becomes `{#myTag foo="foo" /}`.

`io.quarkus.qute.UserTagSectionHelper.Arguments` metadata are accessible in a tag using the `_args` alias.

- `_args.size` - returns the actual number of arguments passed to a tag
- `_args.empty`/`_args.isEmpty` - returns `true` if no arguments are passed
- `_args.get(String name)` - returns the argument value of the given name or `null`
- `_args.filter(String...)` - returns the arguments matching the given names
- `_args.filterIdenticalKeyValue` - returns the arguments with the name equal to the value; typically `foo` from `{#test foo="foo" bar=true}` or `{#test "foo" bar=true /}`
- `_args.skip(String...)` - returns only the arguments that do not match the given names
- `_args.skipIdenticalKeyValue` - returns only the arguments with the name not equal to the value; typically `bar` from `{#test foo="foo" bar=true /}`
- `_args.skipIt` - returns all arguments except for the first unnamed argument; typically `bar` from `{#test foo bar=true /}`
- `_args.asHtmlAttributes` - renders the arguments as HTML attributes; e.g. `foo="true" readonly="readonly"`; the arguments are sorted by name in alphabetical order and the `'`, `"`, `<`, `>`, `&` characters are escaped

`_args` is also iterable of `java.util.Map.Entry`: `{#each _args}{it.key}={it.value}{/each}`.

For example, we can call the user tag defined below with `{#test 'Martin' readonly=true /}`.

**`tags/test.html`**

```
{it} <1>
{readonly} <2>
{_args.filter('readonly').asHtmlAttributes} <3>
```

1. `it` is replaced with the first unnamed parameter of the tag.
2. `readonly` is a named parameter.
3. `_args` represents arguments metadata.

The result would be:

```
Martin
true
readonly="true"
```

##### Inheritance

User tags can also make use of the template inheritance in the same way as regular `{#include}` sections do.

**Tag `myTag`**

```
This is {#insert title}my title{/title}! <1>
```

1. `insert` sections are used to specify parts that could be overridden by a template that includes the given template.

**Tag Call Site**

```html
<p>
  {#myTag}
    {#title}my custom title{/title} <1>
  {/myTag}
</p>
```

1. The result would be something like `<p>This is my custom title!</p>`.

<a id="fragments"></a>
#### Fragments

A fragment represents a part of a template that can be treated as a separate template, i.e. rendered separately. One of the main motivations to introduce this feature was the support of use cases like [htmx fragments](https://htmx.org/essays/template-fragments/).

Fragments can be defined with the `{#fragment}` section. Each fragment has an identifier that can only consist of alphanumeric characters and underscores.

> [!NOTE]
> Note that a fragment identifier must be unique in a template.

**Fragment Definition in `item.html`**

```html
{@org.acme.Item item}
{@java.util.List<String> aliases}

<h1>Item - {item.name}</h1>

<p>This document contains a detailed info about an item.</p>

{#fragment id=item_aliases} <1>
<h2>Aliases</h2>
<ol>
    {#for alias in aliases}
    <li>{alias}</li>
    {/for}
</ol>
{/fragment}
```

1. Defines a fragment with identifier `item_aliases`. Note that only alphanumeric characters and underscores can be used in the identifier. The name of the first parameter can be omitted - `{#fragment item_aliases}` is fine too.

You can obtain a fragment programmatically via the `io.quarkus.qute.Template.getFragment(String)` method.

**Obtaining a Fragment**

```java
@Inject
Template item;

String useTheFragment() {
   return item.getFragment("item_aliases") <1>
            .data("aliases", List.of("Foo","Bar")) <2>
            .render();
}
```

1. Obtains the template fragment with identifier `item_aliases`.
2. Make sure the data are set correctly.

The snippet above should render something like:

```html
<h2>Aliases</h2>
<ol>
    <li>Foo</li>
    <li>Bar</li>
</ol>
```

> [!TIP]
> In Quarkus, it is also possible to define a [type-safe fragment](#type_safe_fragments).

You can also include a fragment with an `{#include}` section inside another template or the template that defines the fragment. A fragment can be also used in expressions with the `frg:`/`fragment:` namespaces.

**Including a Fragment in `user.html`**

```html
<h1>User - {user.name}</h1>

<p>
{#fragment fullname}
{name} <strong>{surname}</strong>
{/fragment}
</p>

<p>This document contains a detailed info about a user.</p>

{#include item$item_aliases aliases=user.aliases /} <1><2>

{frg:fullname} is a happy user! <3>
```

1. A template identifier that contains a dollar sign `$` denotes a fragment. The `item$item_aliases` value is translated as: *Use the fragment `item_aliases` from the template `item`.*
2. The `aliases` parameter is used to pass the relevant data. We need to make sure that the data are set correctly. In this particular case the fragment will use the expression `user.aliases` as the value of `aliases` in the `{#for alias in aliases}` section.
3. The `{frg:username}` expression outputs the fragment content. `frg:` can be replaced with `fragment:`.

> [!TIP]
> If you want to reference a fragment from the same template, skip the part before `$`, i.e. something like `{#include $item_aliases /}`.

> [!NOTE]
> You can specify `{#include item$item_aliases _ignoreFragments=true /}` in order to disable this feature, i.e. a dollar sign `$` in the template identifier does not result in a fragment lookup.

##### Hidden Fragments (Capture)

By default, a fragment is normally rendered as a part of the original template. However, sometimes it might be useful to mark a fragment as *hidden*. The regular fragment section has the `capture` alias that implies a hidden fragment. Alternatively, you can "hide" a fragment either with `rendered=false` or `_hidden` parameters. An interesting use case could be a fragment that can be used multiple-times inside the template that defines it.

**Hidden Fragment Definition in `item.html`**

```html
{#capture strong} <1>
<strong>{val}</strong>
{/capture}

<h1>My page</h1>
<p>This document
{#include $strong val='contains' /} <2>
a lot of
{capture:strong(param:val = 'information')} <3><4>
!</p>
```

1. Defines a hidden fragment with identifier `strong`. `{#capture strong}` can be replaced with `{#fragment strong rendered=false}` or `{#fragment strong _hidden}`. The `rendered` parameter can use any expression, e.g. `{#fragment strong rendered=config.isRendered}`.
2. Include the fragment `strong` and pass the value. Note the syntax `$strong` which is translated to include the fragment `strong` from the current template.
3. A namespace resolver can be used to access a hidden fragment too. `capture:` can be replaced with `cap:`.
4. `param:val = 'information'` is used to pass a named parameter to the fragment.

The snippet above renders something like:

```html
<h1>My page</h1>
<p>This document
<strong>contains</strong>
a lot of
<strong>information</strong>
!</p>
```

> [!TIP]
> In Quarkus, the namespace resolvers are automatically registered for namespaces `frg`, `fragment`, `cap` and `capture`.

#### Eval Section

This section can be used to parse and evaluate a template dynamically. The behavior is very similar to [Include Section](#include_helper) but:

1. The template content is passed directly, i.e. not obtained via an `io.quarkus.qute.TemplateLocator`,
2. It's not possible to override parts of the evaluated template.

```html
{#eval myData.template name='Mia' /} <1><2><3>
```

1. The result of `myData.template` will be used as the template. The template is executed with the [Current Context](#current_context_object), i.e. can reference data from the template it's included into.
2. It's also possible to define optional parameters that can be used in the evaluated template.
3. The content of the section is always ignored.

> [!NOTE]
> The evaluated template is parsed and evaluated every time the section is executed. In other words, it is not possible to cache the parsed value to conserve resources and optimize performance.

> [!IMPORTANT]
> The `{#eval}` section helper is not registered automatically. It can be registered manually using `engine.addSectionHelper(new EvalSectionHelper.Factory())`. See [Engine Customization](#engine-customization) for more details.

#### Cached Section

Sometimes it's practical to cache parts of the template that rarely change. In order to use the caching capability, register and configure the built-in `io.quarkus.qute.CacheSectionHelper.Factory`:

```java
// A simple map-based cache
ConcurrentMap<String, CompletionStage<ResultNode>> map = new ConcurrentHashMap<>();
engineBuilder
    .addSectionHelper(new CacheSectionHelper.Factory(new Cache() {
        @Override
        public CompletionStage<ResultNode> getValue(String key,
           Function<String, CompletionStage<ResultNode>> loader) {
              return map.computeIfAbsent(key, k -> loader.apply(k));
           }
     })).build();
```

> [!TIP]
> If the `quarkus-cache` extension is present in a Quarkus application then the `CacheSectionHelper` is registered and configured *automatically*. The name of the cache is `qute-cache`. It can be configured [in a standard way](/guides/cache/index.md#configuring-the-underlying-caching-provider) and even managed programmatically via `@Inject @CacheName("qute-cache") Cache`.

Then, the `{#cached}` section can be used in a template:

```html
{#cached} <1>
 Result: {service.findResult} <2>
{/cached}
```

1. If the `key` param is not used then all clients of the template share the same cached value.
2. This part of the template will be cached and the `{service.findResult}` expression is only evaluated when a cache entry is missing/invalidated.

```html
{#cached key=currentUser.username} <1>
 User-specific result: {service.findResult(currentUser)}
{/cached}
```

1. The `key` param is set and so a different cached value is used for each result of the `{currentUser.username}` expression.

> [!TIP]
> When using cache it's very often important to have the option to invalidate a cache entry by the specific key. In Qute the key of a cache entry is a `String` that consist of the template name, line and column of the starting `{#cached}` tag and the optional `key` parameter: `{TEMPLATE}:{LINE}:{COLUMN}_{KEY}`. For example, `foo.html:10:1_alpha` is a key for the cached section in a template `foo.html`, the `{#cached}` tag is placed on the line 10, column 1. And the optional `key` parameter resolves to `alpha`.

### Rendering Output

`TemplateInstance` provides several ways to trigger the rendering and consume the result. The most straightforward approach is represented by `TemplateInstance.render()`. This method triggers a synchronous rendering, i.e. the current thread is blocked until the rendering is finished, and returns the output. By contrast, `TemplateInstance.renderAsync()` returns a `CompletionStage<String>` which is completed when the rendering is finished.

**`TemplateInstance.renderAsync()` Example**

```java
template.data(foo).renderAsync().whenComplete((result, failure) -> { <1>
   if (failure == null) {
      // consume the output...
   } else {
      // process failure...
   }
};
```

1. Register a callback that is executed once the rendering is finished.

There are also two methods that return [Mutiny](https://smallrye.io/smallrye-mutiny/) types. `TemplateInstance.createUni()` returns a new `Uni<String>` object. If you call `createUni()` the template is not rendered right away. Instead, every time `Uni.subscribe()` is called a new rendering of the template is triggered.

**`TemplateInstance.createUni()` Example**

```java
template.data(foo).createUni().subscribe().with(System.out::println);
```

`TemplateInstance.createMulti()` returns a new `Multi<String>` object. Each item represents a part/chunk of the rendered template. Again, `createMulti()` does not trigger rendering. Instead, every time a computation is triggered by a subscriber, the template is rendered again.

**`TemplateInstance.createMulti()` Example**

```java
template.data(foo).createMulti().subscribe().with(buffer:append,buffer::flush);
```

> [!NOTE]
> The template rendering is divided in two phases. During the first phase, which is asynchronous, all expressions in the template are resolved and a *result tree* is built. In the second phase, which is synchronous, the result tree is *materialized*, i.e. one by one the result nodes emit chunks that are consumed/buffered by the specific consumer.

### Engine Configuration

<a id="value-resolvers"></a>
#### Value Resolvers

Value resolvers are used when evaluating expressions. First the resolvers that apply to the given `EvalContext` are filtered. Then the resolver with *highest priority* is used to resolve the data. If a `io.quarkus.qute.Results.NotFound` object is returned then the next available resolver is used instead. However, `null` return value is considered a valid result.

A custom `io.quarkus.qute.ValueResolver` can be registered programmatically via `EngineBuilder.addValueResolver()`.

**`ValueResolver` Builder Example**

```java
engineBuilder.addValueResolver(ValueResolver.builder()
    .appliesTo(ctx -> ctx.getBase() instanceof Long && ctx.getName().equals("tenTimes"))
    .resolveSync(ctx -> (Long) ctx.getBase() * 10)
    .build());
```

> [!TIP]
> In Quarkus, the [`@EngineConfiguration`](#engine-customization) annotation can be used to register a `ValueResolver` implemented as a CDI bean.

> [!NOTE]
> Keep in mind that the reflection-based value resolver has priority `-1` and the max priority value for resolvers generated from [`@TemplateData`](#template_data) and [type-safe expressions](#typesafe_expressions) is `10`.

<a id="template-locator"></a>
#### Template Locator

A template can be either registered manually or automatically via a template locator. The locators are used whenever the `Engine.getTemplate()` method is called, and the engine has no template for a given id stored in the cache. The locator is responsible for using the correct character encoding when reading the contents of a template.

> [!NOTE]
> In Quarkus, all templates from the `src/main/resources/templates` are located automatically and the encoding set via `quarkus.qute.default-charset` (UTF-8 by default) is used. Custom locators can be [registered](#template-locator-registration) by using the `@Locate` annotation.

#### Content Filters

Content filters can be used to modify the template contents before parsing.

**Content Filter Example**

```java
engineBuilder.addParserHook(new ParserHook() {
    @Override
    public void beforeParsing(ParserHelper parserHelper) {
        parserHelper.addContentFilter(contents -> contents.replace("${", "$\\{")); <1>
    }
});
```

1. Escape all occurrences of `${`.

<a id="strict_rendering"></a>
#### Strict Rendering

The strict rendering enables the developers to catch insidious errors caused by typos and invalid expressions. If enabled then any expression that cannot be resolved, i.e. is evaluated to an instance of `io.quarkus.qute.Results.NotFound`, will always result in a `TemplateException` and the rendering is aborted. A `NotFound` value is considered an error because it basically means that no value resolver was able to resolve the expression correctly.

> [!NOTE]
> `null` is a valid value though. It is considered `falsy` as described in [If Section](#if_section) and does not produce any output.

Strict rendering is enabled by default. However, you can disable this functionality via `io.quarkus.qute.EngineBuilder.strictRendering(boolean)`.

> [!TIP]
> In Quarkus, a dedicated config property can be used instead: `quarkus.qute.strict-rendering`.

If you really need to use an expression which can potentially lead to a "not found" error, you can use *default values* and *safe expressions* in order to suppress the error. A default value is used if the previous part of an expression cannot be resolved or resolves to `null`. You can use the elvis operator to output the default value: `{foo.bar ?: 'baz'}`, which is effectively the same as the following virtual method: `{foo.bar.or('baz')}`. A safe expression ends with the `??` suffix and results in `null` if the expression cannot be resolved. It can be very useful e.g. in `{#if}` sections: `{#if valueNotFound??}Only rendered if valueNotFound is truthy!{/if}`. In fact, `??` is just a shorthand notation for `.or(null)`, i.e. `{#if valueNotFound??}` becomes `{#if valueNotFound.or(null)}`.

<a id="quarkus_integration"></a>
## Quarkus Integration

If you want to use Qute in your Quarkus application, add the following dependency to your project:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-qute</artifactId>
</dependency>
```

In Quarkus, a preconfigured engine instance is provided and available for injection - a bean with scope `@ApplicationScoped`, bean type `io.quarkus.qute.Engine` and qualifier `@Default` is registered automatically. Moreover, all templates located in the `src/main/resources/templates` directory are validated and can be easily injected.

> [!NOTE]
> A valid template file name is a sequence of non-whitespace characters. For example, a template file named `foo and bar.html` will be ignored.

```java
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.qute.Location;

class MyBean {

    @Inject
    Template items; <1>

    @Location("detail/items2_v1.html") <2>
    Template items2;

    @Inject
    Engine engine; <3>
}
```

1. If there is no `Location` qualifier provided, the field name is used to locate the template. In this particular case, the container will attempt to locate a template with path `src/main/resources/templates/items.html`.
2. The `Location` qualifier instructs the container to inject a template from a path relative from `src/main/resources/templates`. In this case, the full path is `src/main/resources/templates/detail/items2_v1.html`.
3. Inject the configured `Engine` instance.

<a id="engine-customization"></a>
### Engine Customization

Additional components can be registered manually via `EngineBuilder` methods in a CDI observer method at runtime:

```java
import io.quarkus.qute.EngineBuilder;

class MyBean {

    void configureEngine(@Observes EngineBuilder builder) {
       // Add a custom section helper
       builder.addSectionHelper(new CustomSectionFactory());
       // Add a custom value resolver
       builder.addValueResolver(ValueResolver.builder()
                .appliesTo(ctx -> ctx.getBase() instanceof Long && ctx.getName().equals("tenTimes"))
                .resolveSync(ctx -> (Long) ec.getBase() * 10)
                .build());
    }
}
```

However, in this particular case the section helper factory is ignored during validation at build time. If you want to register a section that participates in validation of templates at build time then use the convenient `@EngineConfiguration` annotation:

```java
import io.quarkus.qute.EngineConfiguration;
import io.quarkus.qute.SectionHelper;
import io.quarkus.qute.SectionHelperFactory;

@EngineConfiguration <1>
public class CustomSectionFactory implements SectionHelperFactory<CustomSectionFactory.CustomSectionHelper> {

    @Inject
    Service service; <2>

    @Override
    public List<String> getDefaultAliases() {
        return List.of("custom");
    }

    @Override
    public ParametersInfo getParameters() {
        // Param "foo" is required
        return ParametersInfo.builder().addParameter("foo").build(); <3>
    }

    @Override
    public Scope initializeBlock(Scope outerScope, BlockInfo block) {
        block.addExpression("foo", block.getParameter("foo"));
        return outerScope;
    }


    @Override
    public CustomSectionHelper initialize(SectionInitContext context) {
        return new CustomSectionHelper();
    }

    class CustomSectionHelper implements SectionHelper {

        private final Expression foo;

        public CustomSectionHelper(Expression foo) {
            this.foo = foo;
        }

        @Override
        public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
            return context.evaluate(foo).thenApply(fooVal -> new SingleResultNode(service.getValueForFoo(fooVal))); <4>
        }
    }
}
```

1. A `SectionHelperFactory` annotated with `@EngineConfiguration` is used during validation of templates at build time and automatically registered at runtime (a) as a section factory and (b) as a CDI bean.
2. A CDI bean instance is used at runtime - this means that the factory can define injection points
3. Validate that `foo` parameter is always present; e.g. `{#custom foo='bar' /}` is ok but `{#custom /}` results in a build failure.
4. Use the injected `Service` during rendering.

> [!TIP]
> The `@EngineConfiguration` annotation can be also used to register `ValueResolver`, `NamespaceResolver` and `ParserHook` components.

<a id="template-locator-registration"></a>
#### Template Locator Registration

The easiest way to register [template locators](#template-locator) is to make them CDI beans. As the custom locator is not available during the build time when a template validation is done, you need to disable the validation via the `@Locate` annotation.

**Custom Locator Example**

```java
@Locate("bar.html") <1>
@Locate("foo.*") <2>
public class CustomLocator implements TemplateLocator {

    @Inject <3>
    MyLocationService myLocationService;

    @Override
    public Optional<TemplateLocation> locate(String templateId) {

        return myLocationService.getTemplateLocation(templateId);
    }

}
```

1. A template named `bar.html` is located by the custom locator at runtime.
2. A regular expression `foo.*` disables validation for templates whose name is starting with `foo`.
3. Injection fields are resolved as template locators annotated with `@Locate` are registered as singleton session beans.

### Template Variants

Sometimes it's useful to render a specific variant of the template based on the content negotiation. This can be done by setting a special attribute via `TemplateInstance.setVariant()`:

```java
class MyService {

    @Inject
    Template items;

    @Inject
    ItemManager manager;

    String renderItems() {
       return items.data("items", manager.findItems())
                   .setVariant(new Variant(Locale.getDefault(), "text/html", "UTF-8"))
                   .render();
    }
}
```

> [!NOTE]
> When using `quarkus-rest-qute` or `quarkus-resteasy-qute` the content negotiation is performed automatically. For more information, see the [REST Integration](#rest_integration) section.

<a id="injecting-beans-directly-in-templates"></a>
### Injecting Beans Directly In Templates

A CDI bean annotated with `@Named` can be referenced in any template through `cdi` and/or `inject` namespaces:

```html
{cdi:personService.findPerson(10).name} <1>
{inject:foo.price} <2>
```

1. First, a bean with name `personService` is found and then used as the base object.
2. First, a bean with name `foo` is found and then used as the base object.

> [!NOTE]
> `@Named @Dependent` beans are shared across all expressions in a template for a single rendering operation, and destroyed after the rendering finished.

All expressions with `cdi` and `inject` namespaces are validated during build. For the expression `cdi:personService.findPerson(10).name`, the implementation class of the injected bean must either declare the `findPerson` method or a matching [template extension method](#template_extension_methods) must exist. For the expression `inject:foo.price`, the implementation class of the injected bean must either have the `price` property (e.g. a `getPrice()` method) or a matching [template extension method](#template_extension_methods) must exist.

> [!NOTE]
> A `ValueResolver` is also generated for all beans annotated with `@Named` so that it's possible to access its properties without reflection.

> [!TIP]
> If your application serves [HTTP requests](/guides/http-reference/index.md) you can also inject the current `io.vertx.core.http.HttpServerRequest` via the `inject` namespace, e.g. `{inject:vertxRequest.getParam('foo')}`.

Sometimes it may be necessary to access public methods and properties of a CDI bean that is not annotated with `@Named`. However, if you don't control the source of the bean it is not possible to add the `@Named` annotation. Nevertheless, it is possible to create an intermediate CDI bean annotated with `@Named`. This intermediate bean can inject the bean in question and make it accessible. A Java record is a very convenient way to define such an intermediate CDI bean.

```java
@Named <1><2>
public record UserData(UserInfo info, @LoggedIn String username) { <3>
}
```

1. If no name is explicitly specified by the `value` member the [default name is assigned](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1#default_name) - the simple name of the bean class, after converting the first character to lower case. In this particular case, the default name is `userData`.
2. The `@Singleton` scope is added automatically.
3. All parameters of the canonical constructor are injection points. The accessor methods can be used to obtain the injected bean.

And then in a template you can simply use `{cdi:userData.info}` or `{cdi:userData.username}`.

<a id="typesafe_expressions"></a>
### Type-safe Expressions

Template expressions can be optionally type-safe. Which means that an expression is validated against the existing Java types and template extension methods. If an invalid/incorrect expression is found then the build fails.

For example, if there is an expression `item.name` where `item` maps to `org.acme.Item` then `Item` must have a property `name` or a matching template extension method must exist.

An optional *parameter declaration* is used to bind a Java type to expressions whose first part matches the parameter name. Parameter declarations are specified directly in a template.

A Java type should be always identified with a *fully qualified name* unless it's a JDK type from the `java.lang` package - in this case, the package name is optional. Parameterized types are supported, however wildcards are always ignored - only the upper/lower bound is taken into account. For example, the parameter declaration `{@java.util.List<? extends org.acme.Foo> list}` is recognized as `{@java.util.List<org.acme.Foo> list}`. Type variables are not handled in a special way and should never be used.

**Parameter Declaration Example**

```html
{@org.acme.Foo foo} <1>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Qute Hello</title>
</head>
<body>
  <h1>{title}</h1> <2>
  Hello {foo.message.toLowerCase}! <3><4>
</body>
</html>
```

1. Parameter declaration - maps `foo` to `org.acme.Foo`.
2. Not validated - not matching a param declaration.
3. This expression is validated. `org.acme.Foo` must have a property `message` or a matching template extension method must exist.
4. Likewise, the Java type of the object resolved from `foo.message` must have a property `toLowerCase` or a matching template extension method must exist.

> [!IMPORTANT]
> A value resolver is automatically generated for all types used in parameter declarations so that it's possible to access its properties without reflection.

> [!TIP]
> Method parameters of [type-safe templates](#typesafe_templates) are automatically turned into parameter declarations.

Note that sections can override names that would otherwise match a parameter declaration:

```html
{@org.acme.Foo foo}
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Qute Hello</title>
</head>
<body>
  <h1>{foo.message}</h1> <1>
  {#for foo in baz.foos}
    <p>Hello {foo.message}!</p> <2>
  {/for}
</body>
</html>
```

1. Validated against `org.acme.Foo`.
2. Not validated - `foo` is overridden in the loop section.

A parameter declaration may specify the *default value* after the key. The key and the default value are separated by an equals sign: `{@int age=10}`. The default value is used in the template if the parameter key resolves to `null` or is not found.

For example, if there's a parameter declaration `{@String foo="Ping"}` and `foo` is not found then you can use `{foo}` and the output will be `Ping`. On the other hand, if the value is set (e.g. via `TemplateInstance.data("foo", "Pong")`) then the output of `{foo}` will be `Pong`.

The type of a default value must be assignable to the type of the parameter declaration. For example, see the incorrect parameter declaration that results in a build failure: `{@org.acme.Foo foo=1}`.

> [!TIP]
> The default value is actually an [expression](#expressions). So the default value does not have to be a literal (such as `42` or `true`). For example, you can leverage the `@TemplateEnum` and specify an enum constant as a default value of a parameter declaration: `{@org.acme.MyEnum myEnum=MyEnum:FOO}`. However, the infix notation is not supported in default values unless the parentheses are used for grouping, e.g. `{@org.acme.Foo foo=(foo1 ?: foo2)}`.

> [!IMPORTANT]
> The type of a default value is not validated in [Qute standalone](#standalone).

**More Parameter Declarations Examples**

```
{@int pages} <1>
{@java.util.List<String> strings} <2>
{@java.util.Map<String,? extends Number> numbers} <3>
{@java.util.Optional<?> param} <4>
{@String name="Quarkus"} <5>
```

1. A primitive type.
2. `String` is replaced with `java.lang.String`: `{@java.util.List<java.lang.String> strings}`
3. The wildcard is ignored and the upper bound is used instead: `{@java.util.Map<String,Number>}`
4. The wildcard is ignored and the `java.lang.Object` is used instead: `{@java.util.Optional<java.lang.Object>}`
5. The type is `java.lang.String`, the key is `name` and the default value is `Quarkus`.

<a id="typesafe_templates"></a>
### Type-safe Templates

You can define type-safe templates in your Java code. Parameters of type-safe templates are automatically turned into *parameter declarations* that are used to bind [Type-safe Expressions](#typesafe_expressions). The type-safe expressions are then validated at build time.

There are two ways to define a type-safe template:

1. Annotate a class with `@io.quarkus.qute.CheckedTemplate` and all its `static native` methods will be used to define type-safe templates and the list of parameters they require.
2. Use a Java record that implements `io.quarkus.qute.TemplateInstance`; the record components represent the template parameters and `@io.quarkus.qute.CheckedTemplate` can be optionally used to configure the template.

#### Nested Type-safe Templates

If using [templates in Jakarta REST resources](#rest_integration), you can rely on the following convention:

- Organise your template files in the `/src/main/resources/templates` directory, by grouping them into one directory per resource class. So, if your `ItemResource` class references two templates `hello` and `goodbye`, place them at `/src/main/resources/templates/ItemResource/hello.txt`  and `/src/main/resources/templates/ItemResource/goodbye.txt`. Grouping templates per resource class makes it easier to navigate to them.
- In each of your resource class, declare a `@CheckedTemplate static class Template {}` class within your resource class.
- Declare one `public static native TemplateInstance method();` per template file for your resource.
- Use those static methods to build your template instances.

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
        public static native TemplateInstance item(Item item); <1><2>
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get(Integer id) {
        return Templates.item(service.findItem(id)); <3>
    }
}
```

1. Declare a method that gives us a `TemplateInstance` for `templates/ItemResource/item.html` and declare its `Item item` parameter so we can validate the template.
2. The `item` parameter is automatically turned into a [parameter declaration](#typesafe_expressions) and so all expressions that reference this name will be validated.
3. Make the `Item` object accessible in the template.

> [!TIP]
> By default, the templates defined in a class annotated with `@CheckedTemplate` can only contain type-safe expressions, i.e. expressions that can be validated at build time. You can use `@CheckedTemplate(requireTypeSafeExpressions = false)` to relax this requirement.

#### Top-level Type-safe Templates

You can also declare a top-level Java class annotated with `@CheckedTemplate`:

**Top-level checked templates**

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

1. This declares a template with path `templates/hello.txt`. The `name` parameter is automatically turned into a  [parameter declaration](#typesafe_expressions), so that all expressions referencing this name will be validated.

Then declare one `public static native TemplateInstance method();` per template file. Use those static methods to build your template instances:

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

@Path("hello")
public class HelloResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public TemplateInstance get(@QueryParam("name") String name) {
        return Templates.hello(name);
    }
}
```

#### Template Records

A Java record that implements `io.quarkus.qute.TemplateInstance` denotes a type-safe template. The record components represent the template parameters and `@io.quarkus.qute.CheckedTemplate` can be optionally used to configure the template.

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

@Path("hello")
public class HelloResource {

    record Hello(String name) implements TemplateInstance {} <1>

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public TemplateInstance get(@QueryParam("name") String name) {
        return new Hello(name); <2>
    }
}
```

1. Declares a type-safe template with the Java record. The template is located at `/src/main/resources/templates/HelloResource/Hello.html`.
2. Instantiate the record and use it as an ordinary `TemplateInstance`.

> [!TIP]
> **Kotlin users** can use a data class annotated with `@JvmRecord` to achieve the same result:
>
> ```kotlin
> @CheckedTemplate(basePath = "HelloResource")
> @JvmRecord
> data class Hello(val name: String) : TemplateInstance
> ```
>
> The `@JvmRecord` annotation instructs the Kotlin compiler to generate a Java record in the bytecode, which Quarkus then recognizes as a type-safe template. Note that `@JvmRecord` requires Kotlin 1.5+ and JVM target 16+.

#### Customized Template Path

The path of a type-safe template (`@CheckedTemplate` method or record) consists of a *base path* and a *defaulted name*. The *base path* is supplied by the `@CheckedTemplate#basePath()`. By default, the simple name of the enclosing class for a nested static class or an empty string for a top level class is used. The *defaulted name* is derived by the strategy specified in `@CheckedTemplate#defaultName()`. By default, the name of the `@CheckedTemplate` method/record is used as is.

> [!NOTE]
> A template record that is not annotated with `@CheckedTemplate` is treated as if it was annotated with `@CheckedTemplate` with default values.

**Customized Template Path Example**

```java
package org.acme.quarkus.sample;

import jakarta.ws.rs.Path;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.CheckedTemplate;

@Path("item")
public class ItemResource {

    @CheckedTemplate(basePath = "items", defaultName = CheckedTemplate.HYPHENATED_ELEMENT_NAME)
    static class Templates {
        static native TemplateInstance itemAndOrder(Item item); <1>
    }
}
```

1. The template path for this method will be `items/item-and-order`.

<a id="type_safe_fragments"></a>
#### Type-safe Fragments

You can also define a type-safe [fragment](#fragments) of a type-safe template in your Java code. There are two ways to define a type-safe fragment:

1. A *native static* method annotated with `@CheckedTemplate`, with a name that contains a dollar sign `$`.
2. A Java record that implements `io.quarkus.qute.TemplateInstance` and its name contains a dollar sign `$`.

The name of the fragment is derived from the annotated member name. The part before the last occurrence of a dollar sign `$` is the method name of the related type-safe template. The part after the last occurrence of a dollar sign is the fragment identifier. The strategy defined by the relevant `CheckedTemplate#defaultName()` is honored when constructing the defaulted names.

**Type-safe Fragment Example**

```java
import io.quarkus.qute.CheckedTemplate;
import org.acme.Item;

@CheckedTemplate
class Templates {

  // defines a type-safe template
  static native TemplateInstance items(List<Item> items);

  // defines a fragment of Templates#items() with identifier "item"
  static native TemplateInstance items$item(Item item); <1>
  
  // type-safe fragment as a Java record - functionally equivalent to the items$item() method above
  record items$item(Item item) implements TemplateInstance {}
}
```

1. Quarkus validates at build time that each template that corresponds to the `Templates#items()` contains a fragment with identifier `item`. Moreover, the parameters of the fragment method are validated too. In general, all type-safe expressions that are found in the fragment and that reference some data from the original/outer template require a specific parameter to be present.

**Fragment Definition in `items.html`**

```html
<h1>Items</h1>
<ol>
    {#for item in items}
    {#fragment id=item} <1>
    <li>{item.name}</li> <2>
    {/fragment}
    {/for}
</ol>
```

1. Defines a fragment with identifier `item`.
2. The `{item.name}` expression implies that the `Templates#items$item()` method must declare a parameter of name `item` and type `org.acme.Item`.

**Type-safe Fragment Call Site Example**

```java
class ItemService {

  String renderItem(Item item) {
     // this would return something like "<li>Foo</li>"
     return Templates.items$item(item).render();
  }
}
```

> [!NOTE]
> You can specify `@CheckedTemplate#ignoreFragments=true` in order to disable this feature, i.e. a dollar sign `$` in the method name will not result in a checked fragment method.

<a id="template_contents"></a>
#### Template Contents

It is also possible to specify the contents for a type-safe template directly in your Java code. A `static native` method of a class annotated with `@CheckedTemplate` or a Java record that implements `TemplateInstance` may be annotated with `@io.quarkus.qute.TemplateContents`. The annotation value is used as the template contents. The template id/path is derived from the type-safe template.

**Template Contents Example**

```java
package org.acme.quarkus.sample;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.TemplateContents;
import io.quarkus.qute.TemplateInstance;

@Path("hello")
public class HelloResource {

    @TemplateContents("Hello {name}!") <1>
    record Hello(String name) implements TemplateInstance {} 

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public TemplateInstance get(@QueryParam("name") String name) {
        return new Hello(name); 
    }
}
```

1. Defines the contents for the type-safe template represented by the `Hello` record. The derived template id is `HelloResource/Hello`.

<a id="template_extension_methods"></a>
### Template Extension Methods

Extension methods can be used to extend the data classes with new functionality (to extend the set of accessible properties and methods) or to resolve expressions for a specific [namespace](#namespace_extension_methods). For example, it is possible to add *computed properties* and *virtual methods*.

A value resolver is automatically generated for a method annotated with `@TemplateExtension`. If a class is annotated with `@TemplateExtension` then a value resolver is generated for every *non-private static method* declared on the class. Method-level annotations override the behavior defined on the class. Methods that do not meet the following requirements are ignored.

A template extension method:

- must not be `private`
- must be static,
- must not return `void`.

If there is no namespace defined the class of the first parameter that is not annotated with `@TemplateAttribute` is used to match the base object. Otherwise, the namespace is used to match an expression.

#### Matching by Name

The method name is used to match the property name by default.

**Extension Method Example**

```java
package org.acme;

class Item {

    public final BigDecimal price;

    public Item(BigDecimal price) {
        this.price = price;
    }
}

@TemplateExtension
class MyExtensions {

    static BigDecimal discountedPrice(Item item) { <1>
        return item.getPrice().multiply(new BigDecimal("0.9"));
    }
}
```

1. This method matches an expression with base object of the type `Item.class` and the `discountedPrice` property name.

This template extension method makes it possible to render the following template:

```html
{item.discountedPrice} <1>
```

1. `item` is resolved to an instance of `org.acme.Item`.

However, it is possible to specify the matching name with `matchName()`.

**`TemplateExtension#matchName()` Example**

```java
@TemplateExtension(matchName = "discounted")
static BigDecimal discountedPrice(Item item) {
   // this method matches {item.discounted} if "item" resolves to an object assignable to "Item"
   return item.getPrice().multiply(new BigDecimal("0.9"));
}
```

A special constant - `TemplateExtension#ANY` - can be used to specify that the extension method matches any name.

**`TemplateExtension#ANY` Example**

```java
@TemplateExtension(matchName = TemplateExtension.ANY)
static String itemProperty(Item item, String name) { <1>
   // this method matches {item.foo} if "item" resolves to an object assignable to "Item"
   // the value of the "name" argument is "foo"
}
```

1. An additional string method parameter is used to pass the actual property name.

It's also possible to match the name against a regular expression specified in `matchRegex()`.

**`TemplateExtension#matchRegex()` Example**

```java
@TemplateExtension(matchRegex = "foo|bar")
static String itemProperty(Item item, String name) { <1>
   // this method matches {item.foo} and {item.bar} if "item" resolves to an object assignable to "Item"
   // the value of the "name" argument is "foo" or "bar"
}
```

1. An additional string method parameter is used to pass the actual property name.

Finally, `matchNames()` can be used to specify a collection of matching names. An additional string method parameter is mandatory as well.

**`TemplateExtension#matchNames()` Example**

```java
@TemplateExtension(matchNames = {"foo", "bar"})
static String itemProperty(Item item, String name) {
   // this method matches {item.foo} and {item.bar} if "item" resolves to an object assignable to "Item"
   // the value of the "name" argument is "foo" or "bar"
}
```

> [!NOTE]
> Superfluous matching conditions are ignored. The conditions sorted by priority in descending order are: `matchRegex()`, `matchNames()` and `matchName()`.

#### Method Parameters

An extension method may declare parameters. If no namespace is specified then the first parameter that is not annotated with `@TemplateAttribute` is used to pass the base object, i.e. `org.acme.Item` in the first example. If matching any name or using a regular expression, then a string method parameter (not not annotated with `@TemplateAttribute`) needs to be used to pass the property name. Parameters annotated with `@TemplateAttribute` are obtained via `TemplateInstance#getAttribute()`. All other parameters are treated as virtual method parameters and resolved when rendering the template and passed to the extension method.

**Multiple Parameters Example**

```java
@TemplateExtension
class BigDecimalExtensions {

    @TemplateExtension(matchNames = {"scale", "setScale"})
    static BigDecimal scale(BigDecimal val, String ignoredName, int scale, RoundingMode mode) { <1>
        return val.setScale(scale, mode);
    }
}
```

1. This method matches an expression with base object of the type `BigDecimal.class`, with the `scale()`/`setScale()` virtual method name and two virtual method parameters - `scale` and `mode`.

```html
{item.discountedPrice.scale(2,mode)} <1>
```

1. `item.discountedPrice` is resolved to an instance of `BigDecimal`.

<a id="namespace_extension_methods"></a>
#### Namespace Extension Methods

If `TemplateExtension#namespace()` is specified then the extension method is used to resolve expressions with the given [namespace](#expressions). Template extension methods that share the same namespace are grouped in one resolver ordered by `TemplateExtension#priority()`. The first matching extension method is used to resolve an expression.

**Namespace Extension Method Example**

```java
@TemplateExtension(namespace = "str")
public class StringExtensions {

   static String format(String fmt, Object... args) {
      return String.format(fmt, args);
   }

   static String reverse(String val) {
      return new StringBuilder(val).reverse().toString();
   }
}
```

These extension methods can be used as follows.

```html
{str:format('%s %s!','Hello', 'world')} <1>
{str:reverse('hello')} <2>
```

1. The output is `Hello world!`
2. The output is `olleh`

<a id="built-in-template-extension"></a>
#### Built-in Template Extensions

Quarkus provides a set of built-in extension methods.

##### Maps

- `keys` or `keySet`: Returns a Set view of the keys contained in a map
  - `{#for key in map.keySet}`

- `values`: Returns a Collection view of the values contained in a map
  - `{#for value in map.values}`

- `size`: Returns the number of key-value mappings in a map
  - `{map.size}`

- `isEmpty`: Returns true if a map contains no key-value mappings
  - `{#if map.isEmpty}`

- `get(key)`: Returns the value to which the specified key is mapped
  - `{map.get('foo')}`

> [!TIP]
> A map value can be also accessed directly: `{map.myKey}`. Use the bracket notation for keys that are not legal identifiers: `{map['my key']}`.

##### Lists

- `get(index)`: Returns the element at the specified position in a list
  - `{list.get(0)}`

- `reversed`: Returns a reversed iterator over a list
  - `{#for r in recordsList.reversed}`

- `take`: Returns the first `n` elements from the given list; throws an `IndexOutOfBoundsException` if `n` is out of range
  - `{#for r in recordsList.take(3)}`

- `takeLast`: Returns the last `n` elements from the given list; throws an `IndexOutOfBoundsException` if `n` is out of range
  - `{#for r in recordsList.takeLast(3)}`

- `first`: Returns the first element of the given list; throws an `NoSuchElementException` if the list is empty
  - `{recordsList.first}`

- `last`: Returns the last element of the given list; throws an `NoSuchElementException` if the list is empty
  - `{recordsList.last}`

> [!TIP]
> A list element can be accessed directly via an index: `{list.10}` or even `{list[10]}`.

##### Integer Numbers

- `mod`: Modulo operation
  - `{#if counter.mod(5) == 0}`
- `plus` or `+`: Addition
  - `{counter + 1}`
  - `{age plus 10}`
  - `{age.plus(10)}`
- `minus` or `-`: Subtraction
  - `{counter - 1}`
  - `{age minus 10}`
  - `{age.minus(10)}`

##### Strings

- `fmt` or `format`: Formats the string instance via `java.lang.String.format()`
  - `{myStr.fmt("arg1","arg2")}`
  - `{myStr.format(locale,arg1)}`

- `+`: Infix notation for concatenation, works with `String` and `StringBuilder` base objects
  - `{item.name + '_' + mySuffix}`
  - `{name + 10}`

- `str:['<value>']`: Returns the string value, e.g. to easily concatenate another string value
  - `{str:['/path/to/'] + fileName}`

- `str:fmt` or `str:format`: Formats the supplied string value via `java.lang.String.format()`
  - `{str:format("Hello %s!",name)}`
  - `{str:fmt(locale,'%tA',now)}`
  - `{str:fmt('/path/to/%s', fileName)}`

- `str:concat`: Concatenates the string representations of the specified arguments.
  - `{str:concat("Hello ",name,"!")}` yields `Hello Foo!` if `name` resolves to `Foo`
  - `{str:concat('/path/to/', fileName)}`

- `str:join`: Joins the string representations of the specified arguments together with a delimiter.
  - `{str:join('_','Qute','is','cool')}` yields `Qute_is_cool`

- `str:builder`: Returns a new string builder.
  - `{str:builder('Qute').append("is").append("cool!")}` yields `Qute is cool!`
  - `{str:builder('Qute') + "is" + whatisqute + "!"}` yields `Qute is cool!` if `whatisqute` resolves to `cool`

- `str:eval`: Evaluates the string representation of the first argument as a template in the [current context](#current_context_object).
  - `{str:eval('Hello {name}!')` yields `Hello lovely!` if `name` resolves to `lovely`
  - `{str:eval(myTemplate)}` yields `Hello lovely!` if `myTemplate` resolves to `Hello {name}!` and `name` resolves to `lovely`
  - `{str:eval('/path/to/{fileName}')}` yields `/path/to/file.txt` if `fileName` resolves to `file.txt`
  - 

    > [!IMPORTANT]
    > For security reasons, the `str:eval` namespace resolver is *not* registered automatically. Evaluating an untrusted string as a template may result in a server-side template injection. If you need this functionality then you have to register the `io.quarkus.qute.StrEvalNamespaceResolver` manually, e.g. via [`@Observes EngineBuilder`](#engine-customization):

    ```java
    import io.quarkus.qute.EngineBuilder;
    import io.quarkus.qute.StrEvalNamespaceResolver;

    class MyBean {

        void configureEngine(@Observes EngineBuilder builder) {
            builder.addNamespaceResolver(new StrEvalNamespaceResolver());
        }
    }
    ```

##### Config

- `config:<name>` or `config:[<name>]`: Returns the config value for the given property name
  - `{config:foo}` or `{config:['property.with.dot.in.name']}`

- `config:property(name)`: Returns the config value for the given property name; the name can be obtained dynamically by an expression
  - `{config:property('quarkus.foo')}`
  - `{config:property(foo.getPropertyName())}`

- `config:boolean(name)`: Returns the config value for the given property name as a boolean; the name can be obtained dynamically by an expression
  - `{config:boolean('quarkus.foo.boolean') ?: 'Not Found'}`
  - `{config:boolean(foo.getPropertyName()) ?: 'property is false'}`

- `config:integer(name)`: Returns the config value for the given property name as an integer; the name can be obtained dynamically by an expression
  - `{config:integer('quarkus.foo')}`
  - `{config:integer(foo.getPropertyName())}`

##### Time

- `format(pattern)`: Formats temporal objects from the `java.time` package
  - `{dateTime.format('d MMM uuuu')}`

- `format(pattern,locale)`: Formats temporal objects from the `java.time` package
  - `{dateTime.format('d MMM uuuu',myLocale)}`

- `format(pattern,locale,timeZone)`: Formats temporal objects from the `java.time` package
  - `{dateTime.format('d MMM uuuu',myLocale,myTimeZoneId)}`

- `time:format(dateTime,pattern)`: Formats temporal objects from the `java.time` package, `java.util.Date`, `java.util.Calendar` and `java.lang.Number`
  - `{time:format(myDate,'d MMM uuuu')}`

- `time:format(dateTime,pattern,locale)`: Formats temporal objects from the `java.time` package, `java.util.Date`, `java.util.Calendar` and `java.lang.Number`
  - `{time:format(myDate,'d MMM uuuu', myLocale)}`

- `time:format(dateTime,pattern,locale,timeZone)`: Formats temporal objects from the `java.time` package, `java.util.Date`, `java.util.Calendar` and `java.lang.Number`
  - `{time:format(myDate,'d MMM uuuu',myLocale,myTimeZoneId)}`

<a id="template_data"></a>
### @TemplateData

A value resolver is automatically generated for a type annotated with `@TemplateData`. This allows Quarkus to avoid using reflection to access the data at runtime.

> [!NOTE]
> Non-public members, constructors, static initializers, static, synthetic and void methods are always ignored.

```java
package org.acme;

@TemplateData
class Item {

    public final BigDecimal price;

    public Item(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getDiscountedPrice() {
        return price.multiply(new BigDecimal("0.9"));
    }
}
```

Any instance of `Item` can be used directly in the template:

```html
{#each items} <1>
  {it.price} / {it.discountedPrice}
{/each}
```

1. `items` is resolved to a list of `org.acme.Item` instances.

Furthermore, `@TemplateData.properties()` and `@TemplateData.ignore()` can be used to fine-tune the generated resolver. Finally, it is also possible to specify the "target" of the annotation - this could be useful for third-party classes not controlled by the application:

```java
@TemplateData(target = BigDecimal.class)
@TemplateData
class Item {

    public final BigDecimal price;

    public Item(BigDecimal price) {
        this.price = price;
    }
}
```

```html
{#each items}
  {it.price.setScale(2, rounding)} <1>
{/each}
```

1. The generated value resolver knows how to invoke the `BigDecimal.setScale()` method.

#### Accessing Static Fields and Methods

If `@TemplateData#namespace()` is set to a non-empty value then a namespace resolver is automatically generated to access the public static fields and methods of the target class. By default, the namespace is the FQCN of the target class where dots and dollar signs are replaced by underscores. For example, the namespace for a class with name `org.acme.Foo` is `org_acme_Foo`. The static field `Foo.AGE` can be accessed via `{org_acme_Foo:AGE}`. The static method `Foo.computeValue(int number)` can be accessed via `{org_acme_Foo:computeValue(10)}`.

> [!NOTE]
> A namespace can only consist of alphanumeric characters and underscores.

**Class Annotated With `@TemplateData`**

```java
package model;

@TemplateData <1>
public class Statuses {
    public static final String ON = "on";
    public static final String OFF = "off";
}
```

1. A name resolver with the namespace `model_Statuses` is generated automatically.

**Template Accessing Class Constants**

```html
{#if machine.status == model_Statuses:ON}
  The machine is ON!
{/if}
```

<a id="convenient-annotation-for-enums"></a>
#### Convenient Annotation For Enums

There's also a convenient annotation to access enum constants: `@io.quarkus.qute.TemplateEnum`. This annotation is functionally equivalent to `@TemplateData(namespace = TemplateData.SIMPLENAME)`, i.e. a namespace resolver is automatically generated for the target enum and the simple name of the target enum is used as the namespace.

**Enum Annotated With `@TemplateEnum`**

```java
package model;

@TemplateEnum <1>
public enum Status {
    ON,
    OFF
}
```

1. A name resolver with the namespace `Status` is generated automatically.

> [!NOTE]
> `@TemplateEnum` declared on a non-enum class is ignored. Also, if an enum also declares the `@TemplateData` annotation, then the `@TemplateEnum` annotation is ignored.

**Template Accessing Enum Constants**

```html
{#if machine.status == Status:ON}
  The machine is ON!
{/if}
```

> [!TIP]
> Quarkus detects possible namespace collisions and fails the build if a specific namespace is defined by multiple `@TemplateData` and/or `@TemplateEnum` annotations.

<a id="global_variables"></a>
### Global Variables

The `io.quarkus.qute.TemplateGlobal` annotation can be used to denote static fields and methods that supply *global variables* which are accessible in any template.

Global variables are:

- added as *computed data* of any `TemplateInstance` during initialization,
- accessible with the `global:` namespace.

> [!NOTE]
> When using `TemplateInstance#computedData(String, Function<String, Object>)` a mapping function is associated with a specific key and this function is used each time a value for the given key is requested. In case of global variables, a static method is called or a static field is read in the mapping function.

**Global Variables Definition**

```java
enum Color { RED, GREEN, BLUE }

@TemplateGlobal <1>
public class Globals {

    static int age = 40;

    static Color[] myColors() {
      return new Color[] { Color.RED, Color.BLUE };
    }

    @TemplateGlobal(name = "currentUser") <2>
    static String user() {
       return "Mia";
    }
}
```

1. If a class is annotated with `@TemplateGlobal` then every non-void non-private static method that declares no parameters and every non-private static field is considered a global variable. The name is defaulted, i.e. the name of the field/method is used.
2. Method-level annotations override the class-level annotation. In this particular case, the name is not defaulted but selected explicitly.

**A Template Accessing Global Variables**

```html
User: {currentUser} <1>
Age:  {global:age} <2>
Colors: {#each myColors}{it}{#if it_hasNext}, {/if}{/each} <3>
```

1. `currentUser` resolves to `Globals#user()`.
2. The `global:` namespace is used; `age` resolves to `Globals#age`.
3. `myColors` resolves to `Globals#myColors()`.

> [!NOTE]
> Note that global variables implicitly add [parameter declarations](#typesafe_expressions) to all templates and so any expression that references a global variable is validated during build.

**The Output**

```html
User: Mia
Age:  40
Colors: RED, BLUE
```

#### Resolving Conflicts

If not accessed via the `global:` namespace the global variables may conflict with regular data objects. [Type-safe templates](#typesafe_templates) override the global variables automatically. For example, the following definition overrides the global variable supplied by the `Globals#user()` method:

**Type-safe Template Definition**

```java
import org.acme.User;

@CheckedTemplate
public class Templates {
    static native TemplateInstance hello(User currentUser); <1>
}
```

1. `currentUser` conflicts with the global variable supplied by `Globals#user()`.

So the corresponding template does not result in a validation error even though the `Globals#user()` method returns `java.lang.String` which does not have the `name` property:

**`templates/hello.txt`**

```html
User name: {currentUser.name} <1>
```

1. `org.acme.User` has the `name` property.

For other templates an explicit parameter declaration is needed:

```html
{@org.acme.User currentUser} <1>

User name: {currentUser.name}
```

1. This parameter declaration overrides the declaration added by the global variable supplied by the `Globals#user()` method.

<a id="native_executables"></a>
### Native Executables

In the JVM mode a reflection-based value resolver may be used to access properties and call methods of the model classes. But this does not work for [a native executable](/guides/building-native-image/index.md) out of the box. As a result, you may encounter template exceptions like `Property "name" not found on the base object "org.acme.Foo" in expression {foo.name} in template hello.html` even if the `Foo` class declares a relevant getter method.

There are several ways to solve this problem:

- Make use of [type-safe templates](#typesafe_templates) or [type-safe expressions](#typesafe_expressions)
  - In this case, an optimized value resolver is generated automatically and used at runtime
  - This is the preferred solution
- Annotate the model class with [`@TemplateData`](#template_data) - a specialized value resolver is generated and used at runtime
- Annotate the model class with `@io.quarkus.runtime.annotations.RegisterForReflection` to make the reflection-based value resolver work. More details about the `@RegisterForReflection` annotation can be found on the [native application tips](/guides/writing-native-applications-tips/index.md#registerForReflection) page.

<a id="rest_integration"></a>
### <a id="resteasy_integration"></a> REST Integration

If you want to use Qute in your Jakarta REST application, then depending on which Jakarta REST stack you are using, you'll need to register the proper extension first.

If you are using Quarkus REST (formerly RESTEasy Reactive) via the `quarkus-rest` extension, then in your `pom.xml` file, add:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-qute</artifactId>
</dependency>
```

If instead you are using the legacy RESTEasy Classic-based `quarkus-resteasy` extension, then in your `pom.xml` file, add:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-resteasy-qute</artifactId>
</dependency>
```

Both of these extensions register a special response filter which enables resource methods to return a `TemplateInstance`, thus freeing users of having to take care of all necessary internal steps.

> [!NOTE]
> If using Quarkus REST, a resource method that returns `TemplateInstance` is considered non-blocking. You need to annotate the method with `io.smallrye.common.annotation.Blocking` in order to mark the method as blocking. For example if it's also annotated with `@RunOnVirtualThread`.

The end result is that a using Qute within a Jakarta REST resource may look as simple as:

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
3. Note that we don't trigger the rendering - this is done automatically by a special `ContainerResponseFilter` implementation.

> [!TIP]
> Users are encouraged to use [Type-safe templates](#typesafe_templates) that help to organize the templates for a specific Jakarta REST resource and enable [type-safe expressions](#typesafe_expressions) automatically.

The content negotiation is performed automatically. The resulting output depends on the `Accept` header received from the client.

```java
@Path("/detail")
class DetailResource {

    @Inject
    Template item; <1>

    @GET
    @Produces({ MediaType.TEXT_HTML, MediaType.TEXT_PLAIN })
    public TemplateInstance item() {
        return item.data("myItem", new Item("Alpha", 1000)); <2>
    }
}
```

1. Inject a variant template with base path derived from the injected field - `src/main/resources/templates/item`.
2. For `text/plain` the `src/main/resources/templates/item.txt` template is used. For `text/html` the `META-INF/resources/templates/item.html` template is used.

The `RestTemplate` util class can be used to obtain a template instance from a body of a Jakarta REST resource method:

**RestTemplate Example**

```java
@Path("/detail")
class DetailResource {

    @GET
    @Produces({ MediaType.TEXT_HTML, MediaType.TEXT_PLAIN })
    public TemplateInstance item() {
        return RestTemplate.data("myItem", new Item("Alpha", 1000)); <1>
    }
}
```

1. The name of the template is derived from the resource class and method name; `DetailResource/item` in this particular case.

> [!WARNING]
> Unlike with `@Inject` the templates obtained via `RestTemplate` are not validated, i.e. the build does not fail if a template does not exist.

<a id="vertx_integration"></a>
### Vert.x Integration

If you want to use `io.vertx.core.json.JsonObject` as data in your templates, then you will need to add the `quarkus-vertx` extension to your build file if not already part of your dependencies (most applications use this extension by default).

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-vertx</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-vertx")
```

With this dependency included, we have a special value resolver for `io.vertx.core.json.JsonObject` which makes it possible to access the properties of a JSON object in a template:

**src/main/resources/templates/foo.txt**

```text
{tool.name}
{tool.fieldNames}
{tool.fields}
{tool.size}
{tool.empty}
{tool.isEmpty}
{tool.get('name')}
{tool.containsKey('name')}
```

**QuteVertxIntegration.java**

```java
import java.util.HashMap;
import jakarta.inject.Inject;
import io.vertx.core.json.JsonObject;
import io.quarkus.qute.Template;

public class QuteVertxIntegration {

    @Inject
    Template foo;

    public String render() {
         HashMap<String, Object> toolMap = new Map<String, Object>();
         toolMap.put("name", "Roq");
         JsonObject jsonObject = new JsonObject(toolMap);
         return foo.data("tool", jsonObject).render();
    }
}
```

The `QuteVertxIntegration#render()` output should look like:

```text
Roq
[name]
[name]
1
false
false
Roq
true
```

### Development Mode

In the development mode, all files located in `src/main/resources/templates` are watched for changes. By default, a template modification results in an application restart that also triggers build-time validations.

However, it's possible to use the `quarkus.qute.dev-mode.no-restart-templates` configuration property to specify the templates for which the application is not restarted. The configuration value is a regular expression that matches the template path relative from the `templates` directory and `/` is used as a path separator. For example, `quarkus.qute.dev-mode.no-restart-templates=templates/foo.html` matches the template `src/main/resources/templates/foo.html`. The matching templates are reloaded and only runtime validations are performed.

### Testing

In the test mode, the rendering results of injected and type-safe templates are recorded in the managed `io.quarkus.qute.RenderedResults` which is registered as a CDI bean. You can inject `RenderedResults` in a test or any other CDI bean and assert the results. However, it's possible to set the `quarkus.qute.test-mode.record-rendered-results` configuration property to `false` to disable this feature.

<a id="type-safe-message-bundles"></a>
### Type-safe Message Bundles

#### Basic Concepts

The basic idea is that every message is potentially a very simple template. In order to prevent type errors, a message is defined as an annotated method of a **message bundle interface**. Quarkus generates the **message bundle implementation** at build time.

**Message Bundle Interface Example**

```java
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

@MessageBundle <1>
public interface AppMessages {

    @Message("Hello {name}!") <2>
    String hello_name(String name); <3>
}
```

1. Denotes a message bundle interface. The bundle name is defaulted to `msg` and is used as a namespace in templates expressions, e.g. `{msg:hello_name}`.
2. Each method must be annotated with `@Message`. The value is a qute template. If no value is provided, then a corresponding value from a localized file is taken. If no such file exists, an exception is thrown and the build fails.
3. The method parameters can be used in the template.

The message bundles can be used at runtime:

1. Directly in your code via `io.quarkus.qute.i18n.MessageBundles#get()`; e.g. `MessageBundles.get(AppMessages.class).hello_name("Lucie")`
2. Injected in your beans via `@Inject`; e.g. `@Inject AppMessages`
3. Referenced in the templates via the message bundle namespace:

   ```html
    {msg:hello_name('Lucie')} <1><2><3>
    {msg:message(myKey,'Lu')} <4>
   ```

   1. `msg` is the default namespace.
   2. `hello_name` is the message key.
   3. `Lucie` is the parameter of the message bundle interface method.
   4. It is also possible to obtain a localized message for a key resolved at runtime using a reserved key `message`. The validation is skipped in this case though.


#### Default Bundle Name

The bundle name is defaulted unless it's specified with `@MessageBundle#value()`. For a top-level class the `msg` value is used by default. For a nested class the name consists of the simple names of all enclosing classes in the hierarchy (top-level class goes first), followed by the simple name of the message bundle interface. Names are separated by underscores.

For example, the name of the following message bundle will be defaulted to `Controller_index`:

```java
class Controller {

    @MessageBundle
    interface index {

        @Message("Hello {name}!")
        String hello(String name); <1>
   }
}
```

1. This message could be used in a template via `{Controller_index:hello(name)}`.

> [!NOTE]
> The bundle name is also used as a part of the name of a localized file, e.g. `Controller_index` in the `Controller_index_de.properties`.

#### Bundle Name and Message Keys

Message keys are used directly in templates. The bundle name is used as a namespace in template expressions. The `@MessageBundle` can be used to define the default strategy used to generate message keys from method names. However, the `@Message` can override this strategy and even define a custom key. By default, the annotated element's name is used as-is. Other possibilities are:

1. De-camel-cased and hyphenated; e.g. `helloName()` -> `hello-name`
2. De-camel-cased and parts separated by underscores; e.g. `helloName()` -> `hello_name`.

#### Validation

- All message bundle templates are validated:
  - All expressions without a namespace must map to a parameter; e.g. `Hello {foo}` -> the method must have a param of name `foo`
  - All expressions are validated against the types of the parameters; e.g. `Hello {foo.bar}` where the parameter `foo` is of type `org.acme.Foo` -> `org.acme.Foo` must have a property of name `bar`

    > [!NOTE]
    > A warning message is logged for each *unused* parameter.
- Expressions that reference a message bundle method, such as `{msg:hello(item.name)}`, are validated too.

#### Localization

The default locale specified via the `quarkus.default-locale` config property is used for the `@MessageBundle` interface by default. However, the `io.quarkus.qute.i18n.MessageBundle#locale()` can be used to specify a custom locale. Additionally, there are two ways to define a localized bundle:

1. Create an interface that extends the default interface that is annotated with `@Localized`
2. Create a UTF-8 encoded file located in the `src/main/resources/messages` directory of an application archive; e.g. `msg_de.properties`.

> [!TIP]
> While a localized interface enables easy refactoring, an external file might be more convenient in many situations.

**Localized Interface Example**

```java
import io.quarkus.qute.i18n.Localized;
import io.quarkus.qute.i18n.Message;

@Localized("de") <1>
public interface GermanAppMessages extends AppMessages {

    @Override
    @Message("Hallo {name}!") <2>
    String hello_name(String name);
}
```

1. The value is the locale tag string (IETF).
2. The value is the localized template.

Message bundle files must be encoded in *UTF-8*. The file name consists of the relevant bundle name (e.g. `msg`) and underscore followed by a language tag (IETF; e.g. `en-US`). The language tag may be omitted, in which case the language tag of the default bundle locale is used. For example, if bundle `msg` has default locale `en`, then `msg.properties` is going to be treated as `msg_en.properties`.

If there are multiple files for a specific locale then Qute attempts to resolve the ambiguity. Localized files from the application root have higher priority and take precedence over localized files from dependencies. If multiple files of the same priority exist, then the build fails. For example, if the default bundle locale is `en` and the files `msg.properties` and `msg_en.properties` are found in the application root, then an exception is thrown and the build fails. Or another example - if there are two dependencies and both contain the `msg_en.properties` file, then the build fails again. On the other hand, if there is the `msg_en.properties` file in the application root and also the `msg_en.properties` file in a dependency, then messages from the application root take precedence and override the values from the dependency.

The file format is very simple: each line represents either a key/value pair with the equals sign used as a separator or a comment (line starts with `#`). Blank lines are ignored. Keys are *mapped to method names* from the corresponding message bundle interface. Values represent the templates normally defined by `io.quarkus.qute.i18n.Message#value()`. A value may be spread out across several adjacent normal lines. In such case, the line terminator must be escaped with a backslash character `\`. The behavior is very similar to the behavior of the `java.util.Properties.load(Reader)` method.

**Localized File Example - `msg_de.properties`**

```properties
# This comment is ignored
hello_name=Hallo {name}! <1><2>
```

1. Each line in a localized file represents a key/value pair. The key must correspond to a method declared on the message bundle interface. The value is the message template.
2. Keys and values are separated by the equals sign.

> [!NOTE]
> We use the `.properties` suffix in our example because most IDEs and text editors support syntax highlighting of `.properties` files. But in fact, the suffix could be anything - it is just ignored.

> [!TIP]
> An example properties file is generated into the target directory for each message bundle interface automatically. For example, by default if no name is specified for `@MessageBundle` the file `target/qute-i18n-examples/msg.properties` is generated when the application is build via `mvn clean package`. You can use this file as a base for a specific locale. Just rename the file - e.g. `msg_fr.properties`, change the message templates and move it in the `src/main/resources/messages` directory.

**Value Spread Out Across Several Adjacent Lines**

```properties
hello=Hello \
   {name} and \
   good morning!
```

Note that the line terminator is escaped with a backslash character `\` and white space at the start of the following line is ignored. I.e. `{msg:hello('Edgar')}` would be rendered as `Hello Edgar and good morning!`.

Once we have the localized bundles defined, we need a way to *select* the correct bundle for a specific template instance, i.e. to specify the locale for all message bundle expressions in the template. By default, the locale specified via the `quarkus.default-locale` configuration property is used to select the bundle. Alternatively, you can specify the `locale` attribute of a template instance.

**`locale` Attribute Example**

```java
@Singleton
public class MyBean {

    @Inject
    Template hello;

    String render() {
       return hello.instance().setLocale("cs").render(); <1>
    }
}
```

1. You can set a `Locale` instance or a locale tag string (IETF).

> [!NOTE]
> When using [`quarkus-rest-qute`](#rest_integration) (or `quarkus-resteasy-qute`) the `locale` attribute is derived from the `Accept-Language` header if not set by a user.

The `@Localized` qualifier can be used to inject a localized message bundle interface.

**Injected Localized Message Bundle Example**

```java
@Singleton
public class MyBean {

    @Localized("cs") <1>
    AppMessages msg;

    String render() {
       return msg.hello_name("Jachym");
    }
}
```

1. The annotation value is a locale tag string (IETF).

##### Enums

There is a convenient way to localize enums. If there is a message bundle method that accepts a single parameter of an enum type and has no message template defined:

```java
@Message <1>
String methodName(MyEnum enum);
```

1. The value is intentionally not provided. There's also no key/value pair for this method in a localized file.

Then it receives a generated template like:

```html
{#when enumParamName}
  {#is CONSTANT1}{msg:methodName_CONSTANT1}
  {#is CONSTANT2}{msg:methodName_CONSTANT2}
{/when}
```

Furthermore, a special message method is generated for each enum constant. Finally, each localized file must contain keys and values for all enum constants:

```poperties
methodName_CONSTANT1=Value 1
methodName_CONSTANT2=Value 2
```

By default, the message key consists of the method name followed by the `\_` separator and the constant name. If any constant name of a particular enum contains the `_` or the `$` character then the `\_$` separator must be used for all message keys for this enum instead. For example, `methodName_$CONSTANT_1=Value 1` or `methodName_$CONSTANT$1=Value 1`. A constant of a localized enum may not contain the `_$` separator.

In a template, the localized message for an enum constant can be obtained with a message bundle method like `{msg:methodName(enumConstant)}`.

> [!TIP]
> There is also [`@TemplateEnum`](#convenient-annotation-for-enums) - a convenient annotation to access enum constants in a template.

#### Message Templates

Every method of a message bundle interface must define a message template. The value is normally defined by `io.quarkus.qute.i18n.Message#value()`, but for convenience, there is also an option to define the value in a localized file. Message templates are validated during the build. If a missing message template is detected, an exception is thrown and the build fails.

**Example of the Message Bundle Interface without the value**

```java
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

@MessageBundle
public interface AppMessages {

    @Message <1>
    String hello_name(String name);

    @Message("Goodbye {name}!") <2>
    String goodbye(String name);
}
```

1. The annotation value is not defined. In such a case, the value from supplementary localized file is taken.
2. The annotation value is defined and preferred to the value defined in the localized file.

**Supplementary localized file**

```properties
hello_name=Hello \
   {name} and \
   good morning!
goodbye=Best regards, {name} <1>
```

1. The value is ignored as `io.quarkus.qute.i18n.Message#value()` is always prioritized.

It is also possible to define a *default message template*. The default template is only used if the `Message#value()` is not specified and no relevant message template is defined in a localized file.

**Example of the Message Bundle Interface with a default value**

```java
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

@MessageBundle
public interface AppMessages {

    @Message(defaultValue = "Goodbye {name}!") <1>
    String goodbye(String name);
}
```

1. The annotation value is only used if no message template is defined in a localized file.

<a id="configuration-reference"></a>
### Configuration Reference

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

<a id="standalone"></a>
## Qute Used as a Standalone Library

Qute is primarily designed as a Quarkus extension. However. it is possible to use it as a "standalone" library. In this case, some features are not available and some additional configuration is needed.

**Engine**

  - First, no managed `Engine` instance is available out of the box. You'll need to configure a new instance via `Engine.builder()`.

**Template locators**

  - By default, no [template locators](#template-locator) are registered, i.e. `Engine.getTemplate(String)` will not work.
  - You can register a custom template locator using `EngineBuilder.addLocator()` or parse a template manually and put the result in the cache via `Engine.putTemplate(String, Template)`.

**Template initializers**

  - No `TemplateInstance.Initializer` is registered by default, therefore [`@TemplateGlobal`](#global_variables) annotations are ignored.
  - A custom `TemplateInstance.Initializer` can be registered with `EngineBuilder#addTemplateInstanceInitializer()` and initialize a template instance with any data and attributes.

**Sections**

  - No section helpers are registered by default.
  - The default set of value resolvers can be registered via the convenient `EngineBuilder.addDefaultSectionHelpers()` method and the `EngineBuilder.addDefaults()` method respectively.

**Value resolvers**

  - No [``ValueResolver``s](#value-resolvers) are generated automatically.
    - [`@TemplateExtension` methods](#template_extension_methods) will not work.
    - [`@TemplateData`](#template_data) and [`@TemplateEnum`](#convenient-annotation-for-enums) annotations are ignored.
  - The default set of value resolvers can be registered via the convenient `EngineBuilder.addDefaultValueResolvers()` method and the `EngineBuilder.addDefaults()` method respectively.

    > [!NOTE]
    > Not all functionality provided by the built-in extension methods is covered by the default value resolvers. However, a custom value resolver can be easily built via the `ValueResolver.builder()`.

  - It's recommended to register a `ReflectionValueResolver` instance via `Engine.addValueResolver(new ReflectionValueResolver())` so that Qute can access object properties and call public methods.

    > [!NOTE]
    > Keep in mind that reflection may not work correctly in some restricted environments or may require additional configuration, e.g. registration in case of a GraalVM native image.

**User-defined Tags**

  - No user-defined tags are registered automatically.
  - A tag can be registered manually via `Engine.builder().addSectionHelper(new UserTagSectionHelper.Factory("tagName","tagTemplate.html")).build()`

**Type-safety**

  - [Type-safe Expressions](#typesafe_expressions) are not validated.
  - [Type-safe message bundles](#type-safe-message-bundles) are not supported.

**Injection**\
It is not possible to inject a `Template` instance and vice versa - a template cannot inject a `@Named` CDI bean via the `inject:` and `cdi:` namespace.
