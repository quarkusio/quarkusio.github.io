# Qute - Why (Not Just) Yet Another Templating Engine

> [!NOTE]
> Qute is an experimental feature.
>
> There is no guarantee of stability nor long term presence in the platform until the solution matures.
>
> An [introduction guide](https://quarkus.io/guides/qute) and a more comprehensive [reference guide](https://quarkus.io/guides/qute-reference) are available.

Let's start with a very good question: *"Why yet another templating engine?"*. There are plenty of templating libraries in Java. And Quarkus is known to build on top of "Best of Breed Libraries and Standards". That's true. On the other hand, the Quarkus community is also a powerful innovation catalyst. And so we decided to start Qute (QUarkus TEmplates) - a templating engine designed specifically to meet the Quarkus needs. We believe that we can bring new ideas even to such an explored area as the templating is.

## Basic Ideas

Our main goal is to provide an opinionated innovative templating engine. But we don't want to reinvent the wheel. Instead, we got inspired by existing technologies. Just a few examples:

- The [syntax](https://quarkus.io/guides/qute-reference#syntax-and-building-blocks) is mainly inspired by [Handlebars](https://handlebarsjs.com/) and [Dust.js](https://www.dustjs.com/).
- The [template inheritance](https://quarkus.io/guides/qute-reference#include_helper) is inspired by [Facelets](https://en.wikipedia.org/wiki/Facelets) and [Django](https://docs.djangoproject.com/en/3.0/ref/templates/language/).
- Qute supports the [elvis operator](https://en.wikipedia.org/wiki/Elvis_operator) you might be familiar with from [Groovy](https://groovy-lang.org/) and [Kotlin](https://kotlinlang.org/).
- [Extension methods](https://quarkus.io/guides/qute-reference#template_extension_methods)  that can be used to extend the data classes with new functionality are also inspired by modern languages.
- If you come from the world of JSP/JSF/Facelets you'll appreciate that `@Named` CDI beans can be referenced directly in any template through the `inject` namespace, e.g. `{inject:foo.price}`. See [Injecting Beans Directly In Templates](https://quarkus.io/guides/qute-reference#injecting-beans-directly-in-templates) for more information.

But that's not all. We introduce new features based on Quarkus principles...

## Asynchronous Data Resolution - On The Way To Reactive

When we started to design Qute we had one important aspect in mind - the data resolution API should be asynchronous. This allows for better resource utilization and fits the Quarkus reactive model. Another consequence of this design decision is that it's possible to leverage non-blocking clients directly from a template, i.e. to asynchronously fetch data from various sources.

**Non-blocking Client Data Fetching Example**

```html
{@org.acme.Client client} <1>
<html>
<body>
    <h1>Quarkus Open Pull Requests</h1>
    {#for pull in client.pullRequests} <2>
        <p>{pull.title} - {pull.user.login}</p>
    {/for} 
</body>
</html>
```

1. Parameter declaration - maps `client` to `org.acme.Client`. See the [next section](#type-safe-templates) for more information.
2. `org.acme.Client#getPullRequests()` is using a non-blocking Vert.x client to fetch the data directly from the GitHub API. Since the data resolution is asynchronous the thread is not blocked and can continue performing some other tasks:

   ```java
   CompletionStage<JsonArray> getPullRequests() {
      return webClient
               .get(80, "api.github.com", "/repos/quarkusio/quarkus/pulls?state=open&per_page=10")
               .as(BodyCodec.jsonArray())
               .send()
               .thenCompose(r -> {
                  if (r.statusCode() == 200) {
                     return CompletableFuture.completedFuture(r.body());
                  } else {
                     // Log errors etc.
                  }
               });
   }
   ```


<a id="type-safe-templates"></a>
## Type-safe Templates

Most of the templating engines are not type-safe, ie. do not prevent type errors. It's quite natural because dynamicity in templates is very often practical. On the other hand, a user is not protected from tedious errors caused by typos and various refactoring consequences. Qute templates can be *optionally* type-safe. What does it actually mean? A template may contain one or more **parameter declarations**. A parameter declaration binds a concrete type information to a given identifier in the current context. And what are the advantages of having a type-safe template?

- Quarkus validates *all expressions* that reference a parameter declaration. If an invalid/incorrect expression is found the build fails.

> [!TIP]
> In development mode, all files located in the `src/main/resources/templates` directory are watched for changes and modifications are immediately visible. That also implies that your application fails fast whenever a type error occurs.

- A *value resolver* is generated for all types used in parameter declarations so that it’s possible to access its properties without reflection. This is very useful when targeting GraalVM native images.
- We have few more ideas in our TODO list, such as performance optimizations for type-safe expressions, etc.

**Type-safe Template Example**

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
  <p>{foo.message}</p> <3>
  {#for foo in baz.foos}
    <p>Hello {foo.message}!</p> <4>
  {/for}
</body>
</html>
```

1. Parameter declaration - maps `foo` to `org.acme.Foo`.
2. `{title}` is not validated - not matching a param declaration.
3. `{foo.message}` is validated. `org.acme.Foo` must have a property `message` or a matching template extension method must exist.
4. `{foo.message}` is not validated because `foo` is overridden in the loop section and there is no type information available.

> [!NOTE]
> Only properties are currently validated in expressions; "virtual methods" such as `foo.getBar(baz.name)` are currently ignored.

## First-class Quarkus Citizen

> [!TIP]
> Despite the fact that Qute is highly optimized for Quarkus the core engine is developed as an independent library that could be integrated in any environment.

In Quarkus, all templates located in the `src/main/resources/templates` directory are validated and can be easily injected.

**Template Injection Example**

```java
package org.acme.qute;

import io.quarkus.qute.Template;

class MyBean {

    @Inject 
    Template items; <1>
    
    @Inject
    Service service;

    String renderItems() {
       return items.data("items", service.getItems()).render(); <2>
    }
}
```

1. The field name is used to locate the template. In this particular case, the container will attempt to locate a template with path `src/main/resources/templates/items.html`. If there is no such template available the build fails.
2. See the [Hello World Example](https://quarkus.io/guides/qute-reference#hello-world-example) to explore the basic workflow.

Moreover, a preconfigured `Engine` instance is provided and available for injection. The `Engine` is a central point for template management and provides some low-level API.

## RESTEasy Integration

If used together with RESTEasy a resource method may return a `TemplateInstance` and the integration code takes care of all the necessary steps and renders the output to the response. See [RESTEasy Integration](https://quarkus.io/guides/qute-reference#resteasy-integration) for more information.

**JAX-RS Resource Example**

```java
package org.acme.qute;
...
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Template;

@Path("hello")
public class HelloResource {

    @Inject
    Template hello; <1>

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public TemplateInstance get(@QueryParam("name") String name) {
        // the template looks like: Hello {name}!
        return hello.data("name", name); <2><3>
    }
}
```

1. The field name is used to locate the template. In this particular case, we're injecting a template with path `templates/hello.txt`.
2. `Template.data()` returns a new template instance that can be customized before the actual rendering is triggered. In this case, we put the name value under the key `name`. The data map is accessible during rendering.
3. Note that we don't trigger the rendering - this is done automatically by a special `ContainerResponseFilter` implementation.

## Mailer Integration

Templates may come in handy when creating e-mail messages. The Mailer extension integrates with Qute to provide a convenient way of sending e-mails. In particular, the message body is automatically created using `*.html` and `*.txt` templates from the `src/main/resources/templates` directory. See the [Sending Emails](https://quarkus.io/guides/mailer#message-body-based-on-qute-templates) guide for more details.

## Conclusion

Qute first landed in Quarkus 1.1.0.Final. Since then we fixed many bugs and implemented some feature requests. Feel free to join our community to stabilize the API, harden the implementation and explore the new possibilities!
