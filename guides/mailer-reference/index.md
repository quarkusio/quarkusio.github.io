# Mailer reference guide

This guide is the companion from the [Mailer Getting Started Guide](/guides/mailer/index.md). It explains in more details the configuration and usage of the Quarkus Mailer.

## Mailer extension

To use the mailer, you need to add the `quarkus-mailer` extension.

You can add the extension to your project using:

```bash
> ./mvnw quarkus:add-extensions -Dextensions="mailer"
```

Or just add the following dependency to your project:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-mailer</artifactId>
</dependency>
```

## Accessing the mailer

You can inject the mailer in your application using:

```java
@Inject
Mailer mailer;

@Inject
ReactiveMailer reactiveMailer;
```

There are 2 APIs:

- `io.quarkus.mailer.Mailer` provides the imperative (blocking and synchronous) API;
- `io.quarkus.mailer.reactive.ReactiveMailer` provides the reactive (non-blocking and asynchronous) API

> [!NOTE]
> The two APIs are equivalent feature-wise. Actually the `Mailer` implementation is built on top of the `ReactiveMailer` implementation.

> [!NOTE]
> **Deprecation**
>
> `io.quarkus.mailer.ReactiveMailer` is deprecated in favor of `io.quarkus.mailer.reactive.ReactiveMailer`.

To send a simple email, proceed as follows:

```java
// Imperative API:
mailer.send(Mail.withText("to@acme.org", "A simple email from quarkus", "This is my body.").setFrom("from@acme.org"));
// Reactive API:
Uni<Void> stage = reactiveMailer.send(Mail.withText("to@acme.org", "A reactive email from quarkus", "This is my body.").setFrom("from@acme.org"));
```

For example, you can use the `Mailer` in an HTTP endpoint as follows:

```java
@GET
@Path("/imperative")
public void sendASimpleEmail() {
    mailer.send(Mail.withText("to@acme.org", "A simple email from quarkus", "This is my body").setFrom("from@acme.org"));
}

@GET
@Path("/reactive")
public Uni<Void> sendASimpleEmailAsync() {
    return reactiveMailer.send(
            Mail.withText("to@acme.org", "A reactive email from quarkus", "This is my body").setFrom("from@acme.org"));
}
```

## Creating Mail objects

The mailer lets you send `io.quarkus.mailer.Mail` objects. You can create new `io.quarkus.mailer.Mail` instances from the constructor or from the `Mail.withText` and `Mail.withHtml` helper methods. The `Mail` instance lets you add recipients (to, cc, or bcc), set the subject, headers, sender (from) address...

Most of these properties are optional, but the sender address is required. It can either be set on an individual `Mail` instances using

```java
.setFrom("from@acme.org");
```

or a global default can be configured, using

```properties
quarkus.mailer.from=from@acme.org
```

You can also send several `Mail` objects in one call:

```java
mailer.send(mail1, mail2, mail3);
```

<a id="attachments"></a>
## Sending attachments

To send attachments, just use the `addAttachment` methods on the `io.quarkus.mailer.Mail` instance:

```java
@GET
@Path("/attachment")
public void sendEmailWithAttachment() {
        mailer.send(Mail.withText("clement.escoffier@gmail.com", "An email from quarkus with attachment",
                "This is my body")
                .addAttachment("my-file-1.txt",
                        "content of my file".getBytes(), "text/plain")
                .addAttachment("my-file-2.txt",
                        new File("my-file.txt"), "text/plain")
        );
}
```

Attachments can be created from raw bytes (as shown in the snippet) or files. Note that files are resolved from the working directory of the application.

<a id="html"></a>
## Sending HTML emails with inlined attachments

When sending HTML emails, you can add inlined attachments. For example, you can send an image with your email, and this image will be displayed in the mail content. If you put the image file into the `META-INF/resources` folder, you should specify the full path to the file, *e.g.* `META-INF/resources/quarkus-logo.png` otherwise Quarkus will look for the file in the root directory.

```java
@GET
@Path("/html")
public void sendingHTML() {
    String body = "<strong>Hello!</strong>" + "\n" +
        "<p>Here is an image for you: <img src=\"cid:my-image@quarkus.io\"/></p>" +
        "<p>Regards</p>";
    mailer.send(Mail.withHtml("to@acme.org", "An email in HTML", body)
        .addInlineAttachment("quarkus-logo.png",
            new File("quarkus-logo.png"),
            "image/png", "<my-image@quarkus.io>"));
}
```

Note the *content-id* format and reference. By spec, when you create the inline attachment, the content-id must be structured as follows: `<id@domain>`. If you don't wrap your content-id between `<>`, it is automatically wrapped for you. When you want to reference your attachment, for instance in the `src` attribute, use `cid:id@domain` (without the `<` and `>`).

<a id="templates"></a>
## Message Body Based on Qute Templates

It's also possible to send an e-mail where the message body is created automatically using [Qute templates](/guides/qute/index.md).

You can define a type-safe mail template in your Java code. Just annotate a class with `@io.quarkus.qute.CheckedTemplate` and all its `static native` methods that return `MailTemplate` will be used to define type-safe mail templates and the list of parameters they require:

```java
import io.quarkus.mailer.MailTemplate;
import io.quarkus.qute.CheckedTemplate;

@Path("")
public class MailingResource {

    @CheckedTemplate
    static class Templates {
        public static native MailTemplateInstance hello(String name); // <1>
    }

    @GET
    @Path("/mail")
    public Uni<Void> send() {
        // the template looks like: Hello {name}! // <2>
        return Templates.hello("John")
           .to("to@acme.org")  // <3>
           .subject("Hello from Qute template")
           .send(); // <4>
    }
}
```

1. By convention, the enclosing class name and method names are used to locate the template. In this particular case, we will use the `src/main/resources/templates/MailingResource/hello.html` and `src/main/resources/templates/MailingResource/hello.txt` templates to create the message body.
2. Set the data used in the template.
3. Create a mail template instance and set the recipient.
4. `MailTemplate.send()` triggers the rendering and, once finished, sends the e-mail via a `Mailer` instance.

> [!IMPORTANT]
> `MailTemplateInstance.send()` returns a `Uni<Void>` that **must be subscribed to** — otherwise no e-mail will be sent. If you return the `Uni` from a RESTEasy Reactive resource method, the framework subscribes automatically. In other contexts, make sure the `Uni` is part of a reactive pipeline that is eventually subscribed to, or use `sendAndAwait()` for a blocking alternative.

Alternatively, use a Java record that implements `io.quarkus.mailer.MailTemplate`. The record components represent the template parameters.

```java
import io.quarkus.mailer.MailTemplate;
import io.quarkus.qute.CheckedTemplate;

@Path("")
public class MailingResource {

    record hello(String name) implements MailTemplateInstance { // <1>
    }

    @GET
    @Path("/mail")
    public Uni<Void> send() {
        // the template looks like: Hello {name}! // <2>
        return new hello("John")
           .to("to@acme.org")  // <3>
           .subject("Hello from Qute template")
           .send(); // <4>
    }
}
```

1. By convention, the enclosing class name and the record name are used to locate the template. In this particular case, we will use the `src/main/resources/templates/MailingResource/hello.html` and `src/main/resources/templates/MailingResource/hello.txt` templates to create the message body.
2. Set the data used in the template.
3. Create a mail template instance and set the recipient.
4. `MailTemplate.send()` triggers the rendering and, once finished, sends the e-mail via a `Mailer` instance.

You can also do this without type-safe templates:

```java
import io.quarkus.mailer.MailTemplate;

@Inject
@Location("hello")
MailTemplate hello; // <1>

@GET
@Path("/mail")
public Uni<Void> send() {
    return hello.to("to@acme.org") // <2>
       .subject("Hello from Qute template")
       .data("name", "John") // <3>
       .send() // <4>
}
```

1. If there is no `@Location` qualifier provided, the field name is used to locate the template. Otherwise, search for the template as the specified location. In this particular case, we will use the `src/main/resources/templates/hello.html` and `src/main/resources/templates/hello.txt` templates to create the message body.
2. Create a mail template instance and set the recipient.
3. Set the data used in the template.
4. `MailTemplate.send()` triggers the rendering and, once finished, sends the e-mail via a `Mailer` instance.

> [!TIP]
> Injected mail templates are validated during build. The build fails if there is no matching template in `src/main/resources/templates`.

<a id="execution-model"></a>
## Execution model

The reactive mailer is non-blocking, and the results are provided on an I/O thread. See the [Quarkus Reactive Architecture documentation](/guides/quarkus-reactive-architecture/index.md) for further details on this topic.

The non-reactive mailer blocks until the messages are sent to the SMTP server. Note that does not mean that the message is delivered, just that it's been sent successfully to the SMTP server, which will be responsible for the delivery.

<a id="testing"></a>
## Testing email sending

Because it is very inconvenient to send emails during development and testing, you can set the `quarkus.mailer.mock` boolean configuration to `true` to prevent the actual sending of emails but instead print them on stdout and collect them in a `MockMailbox` bean instead. This is the default if you are running Quarkus in dev or test mode.

You can then write tests to verify that your emails were sent, for example, by a REST endpoint:

```java
@QuarkusTest
class MailTest {

    private static final String TO = "foo@quarkus.io";

    @Inject
    MockMailbox mailbox;

    @BeforeEach
    void init() {
        mailbox.clear();
    }

    @Test
    void testTextMail() throws MessagingException, IOException {
        // call a REST endpoint that sends email
        given()
        .when()
        .get("/send-email")
        .then()
           .statusCode(202)
           .body(is("OK"));

        // verify that it was sent
        List<Mail> sent = mailbox.getMessagesSentTo(TO);
        assertThat(sent).hasSize(1);
        Mail actual = sent.get(0);
        assertThat(actual.getText()).contains("Wake up!");
        assertThat(actual.getSubject()).isEqualTo("Alarm!");

        assertThat(mailbox.getTotalMessagesSent()).isEqualTo(6);
    }
}
```

Another option is to use the  [Quarkus Mailpit](https://github.com/quarkiverse/quarkus-mailpit) extension which provides Dev Services for [Mailpit](https://github.com/axllent/mailpit), a nice UI for testing and debugging email sending.

## Using the underlying Vert.x Mail Client

The Quarkus Mailer is implemented on top of the [Vert.x Mail Client](https://vertx.io/docs/vertx-mail-client/java/), providing an asynchronous and non-blocking way to send emails. If you need fine control on how the mail is sent, for instance if you need to retrieve the message ids, you can inject the underlying client, and use it directly:

```java
@Inject MailClient client;
```

Three API flavors are exposed:

- the Mutiny client (`io.vertx.mutiny.ext.mail.MailClient`)
- the bare client (`io.vertx.ext.mail.MailClient`)

Check the [Using Vert.x guide](/guides/vertx/index.md) for further details about these different APIs and how to select the most suitable for you.

The retrieved `MailClient` is configured using the configuration property presented above. You can also create your own instance, and pass your own configuration.

## Using SSL with native executables

Note that if you enable SSL for the mailer and you want to build a native executable, you will need to enable the SSL support. Please refer to the [Using SSL With Native Executables](/guides/native-and-ssl/index.md) guide for more information.

## Configuring the SMTP credentials

It is recommended to encrypt any sensitive data, such as the `quarkus.mailer.password`. One approach is to save the value into a secure store like HashiCorp Vault, and refer to it from the configuration. Assuming for instance that Vault contains key `mail-password` at path `myapps/myapp/myconfig`, then the mailer extension can be simply configured as:

```properties
...
# path within the kv secret engine where is located the application sensitive configuration
# This uses the https://github.com/quarkiverse/quarkus-vault extension.
quarkus.vault.secret-config-kv-path=myapps/myapp/myconfig

...
quarkus.mailer.password=${mail-password}
```

Please note that the password value is evaluated only once, at startup time. If `mail-password` was changed in Vault, the only way to get the new value would be to restart the application.

> [!NOTE]
> Do use Vault, you need the [Quarkus Vault](https://github.com/quarkiverse/quarkus-vault) extension. More details about this extension and its configuration can be found in the [extension documentation](https://docs.quarkiverse.io/quarkus-vault/dev/index.html).

> [!TIP]
> For more information about the Mailer configuration please refer to the [Configuration Reference](#configuration-reference).

## Configuring TLS

SMTP provides various way to use TLS:

- StartTLS: The client connects to the server using a plain connection and then upgrades to a secure connection.
- SSL/TLS: The client connects to the server using a secure connection from the start.

### Configuring STARTTLS

To use `STARTTLS`, you need to configure the `start-tls` property to `REQUIRED` or `OPTIONAL` and set `tls` to `false`:

```properties
quarkus.mailer.tls=false
quarkus.mailer.start-tls=REQUIRED
```

Setting `tls` to `false` ensure we connect using a plain connection and then upgrade to a secure connection using `STARTTLS`.

To configure the trust store, you can use a *named* TLS configuration stored in the [TLS registry](/guides/tls-registry-reference/index.md):

```properties
quarkus.mailer.tls=false
quarkus.mailer.start-tls=REQUIRED
quarkus.mailer.tls-configuration-name=my-mailer # Reference the named configuration

quarkus.tls.my-mailer.trust-store.pem.certs=server-cert.pem # Configure the trust store
```

While not recommended, you can trust all certificates by setting `quarkus.tls.trust-all` to `true`:

```properties
quarkus.mailer.tls=false
quarkus.mailer.start-tls=REQUIRED
quarkus.mailer.tls-configuration-name=my-mailer # Reference the named configuration
quarkus.tls.my-mailer.trust-all=true
```

Alternatively, you can use the *deprecated* `quarkus.mailer.trust-store.paths`  and `quarkus.mailer.trust-all` properties:

```properties
quarkus.mailer.tls=false
quarkus.mailer.start-tls=REQUIRED
quarkus.mailer.truststore.paths=target/certs/mailpit-ca.crt
```

```properties
quarkus.mailer.tls=false
quarkus.mailer.start-tls=REQUIRED
quarkus.mailer.trust-all=true
```

> [!IMPORTANT]
> To use `START_TLS`, make sure you set `tls` to `false` and `start-tls` to `REQUIRED` or `OPTIONAL`.

### Configuring SSL/TLS

To establish a TLS connection, you need to configure a *named* configuration using the [TLS registry](/guides/tls-registry-reference/index.md):

```properties
quarkus.tls.my-mailer.trust-store.p12.path=server-truststore.p12
quarkus.tls.my-mailer.trust-store.p12.password=secret

quarkus.mailer.tls-configuration-name=my-mailer # Reference the named configuration
```

> [!WARNING]
> When using the mailer, using a *named* configuration is required to avoid conflicts with other TLS configurations. The mailer will not use the default TLS configuration.

When you configure a *named* TLS configuration, TLS is enabled by default. If your SMTP server uses a valid (trusted) certificate, and thus do not require a specific TLS configuration, you need to enable TLS explicitly (as you do not have to configure a trust store):

```properties
quarkus.mailer.tls=true
```

> [!IMPORTANT]
> When `quarkus.tls.trust-all` is set to `true`, the trust store configuration is ignored. This is not recommended for production. Also, we recommend avoiding using `quarkus.tls.trust-all`, and use a named configuration instead if `trust-all` is required:
>
> ```properties
> quarkus.tls.my-mailer.trust-all=true
> quarkus.mailer.tls-configuration-name=my-mailer # Reference the named configuration
> ```
>
> You can also use the deprecated `quarkus.mailer.trust-all=true` property.

## Multiple mailer configurations

Some applications require to send mails through different SMTP servers.

This use case is perfectly supported in Quarkus and you can configure several mailers:

```properties
quarkus.mailer.from=your-from-address@gmail.com <1>
quarkus.mailer.host=smtp.gmail.com

quarkus.mailer.aws.from=your-from-address@gmail.com <2>
quarkus.mailer.aws.host=${ses.smtp}
quarkus.mailer.aws.port=587

quarkus.mailer.sendgrid.from=your-from-address@gmail.com <3>
quarkus.mailer.sendgrid.host=${sendgrid.smtp-host}
quarkus.mailer.sendgrid.port=465
```

1. Configuration for the default mailer.
2. Configuration for a mailer named `aws`.
3. Configuration for a mailer named `sendgrid`.

Then, access your named mailers by using the `@MailerName` CDI qualifier:

```java
@Inject <1>
Mailer mailer;

@Inject <1>
ReactiveMailer reactiveMailer;

@Inject <1>
@Location("hello")
MailTemplate mailTemplate;

@Inject
@MailerName("aws") <2>
Mailer mailer;

@Inject
@MailerName("aws") <2>
ReactiveMailer reactiveMailer;

@Inject
@MailerName("aws") <2>
@Location("hello")
MailTemplate mailTemplate;

@Inject
@MailerName("sendgrid") <3>
Mailer mailer;

@Inject
@MailerName("sendgrid") <3>
ReactiveMailer reactiveMailer;

@Inject
@MailerName("sendgrid") <3>
@Location("hello")
MailTemplate mailTemplate;
```

1. Inject instances without qualifier for the default configuration.
2. Inject instances with the `@MailerName("aws")` qualifier for the `aws` configuration.
3. Inject instances with the `@MailerName("sendgrid")` qualifier for the `sendgrid` configuration.

> [!WARNING]
> Type-safe template using `@CheckedTemplate` are currently only supported for the default mailer configuration.
>
> Use `MailTemplate` injection for the named mailer configurations.

<a id="popular"></a>
## Mailer configuration for popular email services

This section provides the configurations to use with popular mail services.

<a id="gmail-specific-configuration"></a>
### Gmail specific configuration

If you want to use the Gmail SMTP server, first create a dedicated password in `Google Account > Security > App passwords` or go to <https://myaccount.google.com/apppasswords>.

> [!NOTE]
> You need to switch on 2-Step Verification at <https://myaccount.google.com/security> in order to access the App passwords page.

When done, you can configure your Quarkus application by adding the following properties to your `application.properties`:

With `STARTTLS`:

```properties
quarkus.mailer.auth-methods=DIGEST-MD5 CRAM-SHA256 CRAM-SHA1 CRAM-MD5 PLAIN LOGIN
quarkus.mailer.from=YOUREMAIL@gmail.com
quarkus.mailer.host=smtp.gmail.com
quarkus.mailer.port=587
quarkus.mailer.start-tls=REQUIRED
quarkus.mailer.username=YOUREMAIL@gmail.com
quarkus.mailer.password=YOURGENERATEDAPPLICATIONPASSWORD

quarkus.mailer.mock=false # In dev mode, prevent from using the mock SMTP server
```

Or with TLS/SSL:

```properties
quarkus.mailer.auth-methods=DIGEST-MD5 CRAM-SHA256 CRAM-SHA1 CRAM-MD5 PLAIN LOGIN
quarkus.mailer.from=YOUREMAIL@gmail.com
quarkus.mailer.host=smtp.gmail.com
quarkus.mailer.port=465
quarkus.mailer.tls=true
quarkus.mailer.username=YOUREMAIL@gmail.com
quarkus.mailer.password=YOURGENERATEDAPPLICATIONPASSWORD

quarkus.mailer.mock=false # In dev mode, prevent from using the mock SMTP server
```

> [!NOTE]
> The `quarkus.mailer.auth-methods` configuration option is needed for the Quarkus mailer to support password authentication with Gmail. By default, both the mailer and Gmail default to `XOAUTH2` which requires registering an application, getting tokens, etc.

### AWS SES - Simple Email Service

#### Prerequisites

1. SES Identity Check, follow the process to setup the DKIM verification
2. Retrieve SMTP endpoint from <https://us-east-1.console.aws.amazon.com/ses/home>, example: `email-smtp.us-east-1.amazonaws.com`
3. Create SMTP credentials if needed
4. If you are in a sandbox, also verify the recipients (using email verification)

#### Configuration

```properties
ses.smtp=...
ses.user=...
ses.password=...
ses.from=an email address from the verified domain

quarkus.mailer.host=${ses.smtp}
quarkus.mailer.port=587
quarkus.mailer.tls=false
quarkus.mailer.username=${ses.user}
quarkus.mailer.password=${ses.password}
quarkus.mailer.start-tls=REQUIRED
quarkus.mailer.login=REQUIRED
quarkus.mailer.from=${ses.from}

quarkus.mailer.mock=false # In dev mode, prevent from using the mock SMTP server
```

### MailJet

The mailjet integration is used on an SMTP relay. You are going to send the email using this SMTP server.

#### Prerequisites

1. Create a mailJet account and the API key / Secret Key
2. The sender address must be verified (SPF + DKIM) and the email explicitly added to the verified list

#### Configuration

```properties
mailjet.smtp-host=in-v3.mailjet.com
mailjet.api-key=...
mailjet.secret-key=...
mailjet.from=the verified sender address

quarkus.mailer.host=${mailjet.smtp-host}
quarkus.mailer.port=465
quarkus.mailer.username=${mailjet.api-key}
quarkus.mailer.password=${mailjet.secret-key}
quarkus.mailer.start-tls=OPTIONAL
quarkus.mailer.tls=true
quarkus.mailer.login=REQUIRED
quarkus.mailer.from=${mailjet.from}

quarkus.mailer.mock=false # In dev mode, prevent from using the mock SMTP server
```

### Sendgrid

#### Prerequisites

- Follow the instruction to verify the sender domain using DKIM

#### Configuration

```properties
sendgrid.smtp-host=smtp.sendgrid.net
sendgrid.username=apikey
sendgrid.key=...

quarkus.mailer.host=${sendgrid.smtp-host}
quarkus.mailer.port=465
quarkus.mailer.username=${sendgrid.username}
quarkus.mailer.password=${sendgrid.key}
quarkus.mailer.start-tls=OPTIONAL
quarkus.mailer.login=REQUIRED
quarkus.mailer.from=...
quarkus.mailer.tls=true

quarkus.mailer.mock=false # In dev mode, prevent from using the mock SMTP server
```

### Office 365

#### Prerequisites

1. Enable SMTP Access to your Office 365 mailbox, you can do it from the administration console (see [this guide](https://learn.microsoft.com/en-us/exchange/clients-and-mobile-in-exchange-online/authenticated-client-smtp-submission) for more information)

#### Configuration

```properties
quarkus.mailer.from=NAME<YOUREMAIL@YOURDOMAIN.COM>
quarkus.mailer.host=smtp.office365.com
quarkus.mailer.port=587
quarkus.mailer.username=YOUREMAIL@YOURDOMAIN.COM
quarkus.mailer.password=YOURPASSWORD
quarkus.mailer.auth-methods=LOGIN
quarkus.mailer.login=REQUIRED

quarkus.mailer.mock=false # In dev mode, prevent from using the mock SMTP server
```

<a id="configuration-reference"></a>
## Mailer Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-mailer_quarkus-mailer-cache-attachments"></a> [`quarkus.mailer.cache-attachments`](#quarkus-mailer_quarkus-mailer-cache-attachments) config_property_copy_button:+++quarkus.mailer.cache-attachments+++[]<br><br>Caches data from attachment's Stream to a temporary file. It tries to delete it after sending email.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_CACHE_ATTACHMENTS+++[] | boolean | `false` |
| <a id="quarkus-mailer_section_quarkus-mailer"></a> [Additional named mailers](#quarkus-mailer_section_quarkus-mailer) | Type | Default |
| <a id="quarkus-mailer_quarkus-mailer-from"></a> [`quarkus.mailer.from`](#quarkus-mailer_quarkus-mailer-from) config_property_copy_button:+++quarkus.mailer.from+++[]<br><br>`quarkus.mailer."mailer-name".from` config_property_copy_button:+++quarkus.mailer."mailer-name".from+++[]<br><br>Sets the default `from` attribute when not specified in the `io.quarkus.mailer.Mail` instance. It's the sender email address.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_FROM+++[] | string |  |
| <a id="quarkus-mailer_quarkus-mailer-mock"></a> [`quarkus.mailer.mock`](#quarkus-mailer_quarkus-mailer-mock) config_property_copy_button:+++quarkus.mailer.mock+++[]<br><br>`quarkus.mailer."mailer-name".mock` config_property_copy_button:+++quarkus.mailer."mailer-name".mock+++[]<br><br>Enables the mock mode. When enabled, mails are not sent, but stored in an in-memory mailbox. The content of the emails is also printed on the console.<br><br>Disabled by default on PROD, enabled by default on DEV and TEST modes.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_MOCK+++[] | boolean |  |
| <a id="quarkus-mailer_quarkus-mailer-bounce-address"></a> [`quarkus.mailer.bounce-address`](#quarkus-mailer_quarkus-mailer-bounce-address) config_property_copy_button:+++quarkus.mailer.bounce-address+++[]<br><br>`quarkus.mailer."mailer-name".bounce-address` config_property_copy_button:+++quarkus.mailer."mailer-name".bounce-address+++[]<br><br>Sets the default bounce email address. A bounced email, or bounce, is an email message that gets rejected by a mail server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_BOUNCE_ADDRESS+++[] | string |  |
| <a id="quarkus-mailer_quarkus-mailer-host"></a> [`quarkus.mailer.host`](#quarkus-mailer_quarkus-mailer-host) config_property_copy_button:+++quarkus.mailer.host+++[]<br><br>`quarkus.mailer."mailer-name".host` config_property_copy_button:+++quarkus.mailer."mailer-name".host+++[]<br><br>Sets the SMTP host name.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_HOST+++[] | string | `localhost` |
| <a id="quarkus-mailer_quarkus-mailer-port"></a> [`quarkus.mailer.port`](#quarkus-mailer_quarkus-mailer-port) config_property_copy_button:+++quarkus.mailer.port+++[]<br><br>`quarkus.mailer."mailer-name".port` config_property_copy_button:+++quarkus.mailer."mailer-name".port+++[]<br><br>The SMTP port. The default value depends on the configuration. The port 25 is used as default when `ssl` is disabled. This port continues to be used primarily for SMTP relaying. SMTP relaying is the transmission of email from email server to email server. The port 587 is the default port when `ssl` is enabled. It ensures that email is submitted securely.<br><br>Note that the port 465 may be used by SMTP servers, however, IANA has reassigned a new service to this port, and it should no longer be used for SMTP communications.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_PORT+++[] | int |  |
| <a id="quarkus-mailer_quarkus-mailer-username"></a> [`quarkus.mailer.username`](#quarkus-mailer_quarkus-mailer-username) config_property_copy_button:+++quarkus.mailer.username+++[]<br><br>`quarkus.mailer."mailer-name".username` config_property_copy_button:+++quarkus.mailer."mailer-name".username+++[]<br><br>Sets the username to connect to the SMTP server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_USERNAME+++[] | string |  |
| <a id="quarkus-mailer_quarkus-mailer-password"></a> [`quarkus.mailer.password`](#quarkus-mailer_quarkus-mailer-password) config_property_copy_button:+++quarkus.mailer.password+++[]<br><br>`quarkus.mailer."mailer-name".password` config_property_copy_button:+++quarkus.mailer."mailer-name".password+++[]<br><br>Sets the password to connect to the SMTP server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_PASSWORD+++[] | string |  |
| <a id="quarkus-mailer_quarkus-mailer-tls-configuration-name"></a> [`quarkus.mailer.tls-configuration-name`](#quarkus-mailer_quarkus-mailer-tls-configuration-name) config_property_copy_button:+++quarkus.mailer.tls-configuration-name+++[]<br><br>`quarkus.mailer."mailer-name".tls-configuration-name` config_property_copy_button:+++quarkus.mailer."mailer-name".tls-configuration-name+++[]<br><br>The name of the TLS configuration to use.<br><br>If a name is configured, it uses the configuration from `quarkus.tls.<name>.*` If a name is configured, but no TLS configuration is found with that name then an error will be thrown.<br><br>If no TLS configuration name is set then, the specific TLS configuration (from `quarkus.mailer.*`) will be used.<br><br>The default TLS configuration is **not** used by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_TLS_CONFIGURATION_NAME+++[] | string |  |
| <a id="quarkus-mailer_quarkus-mailer-tls"></a> [`quarkus.mailer.tls`](#quarkus-mailer_quarkus-mailer-tls) config_property_copy_button:+++quarkus.mailer.tls+++[]<br><br>`quarkus.mailer."mailer-name".tls` config_property_copy_button:+++quarkus.mailer."mailer-name".tls+++[]<br><br>Whether the connection should be secured using TLS.<br><br>SMTP allows establishing connection with or without TLS. When establishing a connection with TLS, the connection is secured and encrypted. When establishing a connection without TLS, it can be secured and encrypted later using the STARTTLS command. In this case, the connection is initially unsecured and unencrypted. To configure this case, set this property to `false` and `start-tls` to `REQUIRED` Note that if a TLS configuration is set, TLS is enabled automatically. So, setting this property to `false` is required to not establish a connection with TLS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_TLS+++[] | boolean |  |
| <a id="quarkus-mailer_quarkus-mailer-max-pool-size"></a> [`quarkus.mailer.max-pool-size`](#quarkus-mailer_quarkus-mailer-max-pool-size) config_property_copy_button:+++quarkus.mailer.max-pool-size+++[]<br><br>`quarkus.mailer."mailer-name".max-pool-size` config_property_copy_button:+++quarkus.mailer."mailer-name".max-pool-size+++[]<br><br>Sets the max number of open connections to the mail server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_MAX_POOL_SIZE+++[] | int | `10` |
| <a id="quarkus-mailer_quarkus-mailer-own-host-name"></a> [`quarkus.mailer.own-host-name`](#quarkus-mailer_quarkus-mailer-own-host-name) config_property_copy_button:+++quarkus.mailer.own-host-name+++[]<br><br>`quarkus.mailer."mailer-name".own-host-name` config_property_copy_button:+++quarkus.mailer."mailer-name".own-host-name+++[]<br><br>Sets the hostname to be used for HELO/EHLO and the Message-ID.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_OWN_HOST_NAME+++[] | string |  |
| <a id="quarkus-mailer_quarkus-mailer-keep-alive"></a> [`quarkus.mailer.keep-alive`](#quarkus-mailer_quarkus-mailer-keep-alive) config_property_copy_button:+++quarkus.mailer.keep-alive+++[]<br><br>`quarkus.mailer."mailer-name".keep-alive` config_property_copy_button:+++quarkus.mailer."mailer-name".keep-alive+++[]<br><br>Sets if connection pool is enabled. If the connection pooling is disabled, the max number of sockets is enforced nevertheless.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_KEEP_ALIVE+++[] | boolean | `true` |
| <a id="quarkus-mailer_quarkus-mailer-disable-esmtp"></a> [`quarkus.mailer.disable-esmtp`](#quarkus-mailer_quarkus-mailer-disable-esmtp) config_property_copy_button:+++quarkus.mailer.disable-esmtp+++[]<br><br>`quarkus.mailer."mailer-name".disable-esmtp` config_property_copy_button:+++quarkus.mailer."mailer-name".disable-esmtp+++[]<br><br>Disable ESMTP.<br><br>The RFC-1869 states that clients should always attempt `EHLO` as first command to determine if ESMTP is supported, if this returns an error code, `HELO` is tried to use the *regular* SMTP command.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_DISABLE_ESMTP+++[] | boolean | `false` |
| <a id="quarkus-mailer_quarkus-mailer-start-tls"></a> [`quarkus.mailer.start-tls`](#quarkus-mailer_quarkus-mailer-start-tls) config_property_copy_button:+++quarkus.mailer.start-tls+++[]<br><br>`quarkus.mailer."mailer-name".start-tls` config_property_copy_button:+++quarkus.mailer."mailer-name".start-tls+++[]<br><br>Sets the TLS security mode for the connection. Either `DISABLED`, `OPTIONAL` or `REQUIRED`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_START_TLS+++[] | string | `OPTIONAL` |
| <a id="quarkus-mailer_quarkus-mailer-dkim-enabled"></a> [`quarkus.mailer.dkim.enabled`](#quarkus-mailer_quarkus-mailer-dkim-enabled) config_property_copy_button:+++quarkus.mailer.dkim.enabled+++[]<br><br>`quarkus.mailer."mailer-name".dkim.enabled` config_property_copy_button:+++quarkus.mailer."mailer-name".dkim.enabled+++[]<br><br>Enables DKIM signing.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_DKIM_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-mailer_quarkus-mailer-dkim-private-key"></a> [`quarkus.mailer.dkim.private-key`](#quarkus-mailer_quarkus-mailer-dkim-private-key) config_property_copy_button:+++quarkus.mailer.dkim.private-key+++[]<br><br>`quarkus.mailer."mailer-name".dkim.private-key` config_property_copy_button:+++quarkus.mailer."mailer-name".dkim.private-key+++[]<br><br>Configures the PKCS++#++8 format private key used to sign the email.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_DKIM_PRIVATE_KEY+++[] | string |  |
| <a id="quarkus-mailer_quarkus-mailer-dkim-private-key-path"></a> [`quarkus.mailer.dkim.private-key-path`](#quarkus-mailer_quarkus-mailer-dkim-private-key-path) config_property_copy_button:+++quarkus.mailer.dkim.private-key-path+++[]<br><br>`quarkus.mailer."mailer-name".dkim.private-key-path` config_property_copy_button:+++quarkus.mailer."mailer-name".dkim.private-key-path+++[]<br><br>Configures the PKCS++#++8 format private key file path.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_DKIM_PRIVATE_KEY_PATH+++[] | string |  |
| <a id="quarkus-mailer_quarkus-mailer-dkim-auid"></a> [`quarkus.mailer.dkim.auid`](#quarkus-mailer_quarkus-mailer-dkim-auid) config_property_copy_button:+++quarkus.mailer.dkim.auid+++[]<br><br>`quarkus.mailer."mailer-name".dkim.auid` config_property_copy_button:+++quarkus.mailer."mailer-name".dkim.auid+++[]<br><br>Configures the Agent or User Identifier (AUID).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_DKIM_AUID+++[] | string |  |
| <a id="quarkus-mailer_quarkus-mailer-dkim-selector"></a> [`quarkus.mailer.dkim.selector`](#quarkus-mailer_quarkus-mailer-dkim-selector) config_property_copy_button:+++quarkus.mailer.dkim.selector+++[]<br><br>`quarkus.mailer."mailer-name".dkim.selector` config_property_copy_button:+++quarkus.mailer."mailer-name".dkim.selector+++[]<br><br>Configures the selector used to query the public key.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_DKIM_SELECTOR+++[] | string |  |
| <a id="quarkus-mailer_quarkus-mailer-dkim-sdid"></a> [`quarkus.mailer.dkim.sdid`](#quarkus-mailer_quarkus-mailer-dkim-sdid) config_property_copy_button:+++quarkus.mailer.dkim.sdid+++[]<br><br>`quarkus.mailer."mailer-name".dkim.sdid` config_property_copy_button:+++quarkus.mailer."mailer-name".dkim.sdid+++[]<br><br>Configures the Signing Domain Identifier (SDID).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_DKIM_SDID+++[] | string |  |
| <a id="quarkus-mailer_quarkus-mailer-dkim-header-canon-algo"></a> [`quarkus.mailer.dkim.header-canon-algo`](#quarkus-mailer_quarkus-mailer-dkim-header-canon-algo) config_property_copy_button:+++quarkus.mailer.dkim.header-canon-algo+++[]<br><br>`quarkus.mailer."mailer-name".dkim.header-canon-algo` config_property_copy_button:+++quarkus.mailer."mailer-name".dkim.header-canon-algo+++[]<br><br>Configures the canonicalization algorithm for signed headers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_DKIM_HEADER_CANON_ALGO+++[] | `simple`, `relaxed` |  |
| <a id="quarkus-mailer_quarkus-mailer-dkim-body-canon-algo"></a> [`quarkus.mailer.dkim.body-canon-algo`](#quarkus-mailer_quarkus-mailer-dkim-body-canon-algo) config_property_copy_button:+++quarkus.mailer.dkim.body-canon-algo+++[]<br><br>`quarkus.mailer."mailer-name".dkim.body-canon-algo` config_property_copy_button:+++quarkus.mailer."mailer-name".dkim.body-canon-algo+++[]<br><br>Configures the canonicalization algorithm for mail body.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_DKIM_BODY_CANON_ALGO+++[] | `simple`, `relaxed` |  |
| <a id="quarkus-mailer_quarkus-mailer-dkim-body-limit"></a> [`quarkus.mailer.dkim.body-limit`](#quarkus-mailer_quarkus-mailer-dkim-body-limit) config_property_copy_button:+++quarkus.mailer.dkim.body-limit+++[]<br><br>`quarkus.mailer."mailer-name".dkim.body-limit` config_property_copy_button:+++quarkus.mailer."mailer-name".dkim.body-limit+++[]<br><br>Configures the body limit to sign. Must be greater than zero.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_DKIM_BODY_LIMIT+++[] | int |  |
| <a id="quarkus-mailer_quarkus-mailer-dkim-signature-timestamp"></a> [`quarkus.mailer.dkim.signature-timestamp`](#quarkus-mailer_quarkus-mailer-dkim-signature-timestamp) config_property_copy_button:+++quarkus.mailer.dkim.signature-timestamp+++[]<br><br>`quarkus.mailer."mailer-name".dkim.signature-timestamp` config_property_copy_button:+++quarkus.mailer."mailer-name".dkim.signature-timestamp+++[]<br><br>Configures to enable or disable signature sign timestamp.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_DKIM_SIGNATURE_TIMESTAMP+++[] | boolean |  |
| <a id="quarkus-mailer_quarkus-mailer-dkim-expire-time"></a> [`quarkus.mailer.dkim.expire-time`](#quarkus-mailer_quarkus-mailer-dkim-expire-time) config_property_copy_button:+++quarkus.mailer.dkim.expire-time+++[]<br><br>`quarkus.mailer."mailer-name".dkim.expire-time` config_property_copy_button:+++quarkus.mailer."mailer-name".dkim.expire-time+++[]<br><br>Configures the expire time in seconds when the signature sign will be expired. Must be greater than zero.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_DKIM_EXPIRE_TIME+++[] | long |  |
| <a id="quarkus-mailer_quarkus-mailer-dkim-signed-headers"></a> [`quarkus.mailer.dkim.signed-headers`](#quarkus-mailer_quarkus-mailer-dkim-signed-headers) config_property_copy_button:+++quarkus.mailer.dkim.signed-headers+++[]<br><br>`quarkus.mailer."mailer-name".dkim.signed-headers` config_property_copy_button:+++quarkus.mailer."mailer-name".dkim.signed-headers+++[]<br><br>Configures the signed headers in DKIM, separated by commas. The order in the list matters.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_DKIM_SIGNED_HEADERS+++[] | list of string |  |
| <a id="quarkus-mailer_quarkus-mailer-login"></a> [`quarkus.mailer.login`](#quarkus-mailer_quarkus-mailer-login) config_property_copy_button:+++quarkus.mailer.login+++[]<br><br>`quarkus.mailer."mailer-name".login` config_property_copy_button:+++quarkus.mailer."mailer-name".login+++[]<br><br>Sets the login mode for the connection. Either `NONE`, `DISABLED`, `REQUIRED` or `XOAUTH2`.<br><br>\* NONE means a login will be attempted if the server supports it and login credentials are set \* DISABLED means no login will be attempted \* REQUIRED means that a login will be attempted if the server supports it and the send operation will fail otherwise \* XOAUTH2 means that a login will be attempted using Google Gmail Oauth2 tokens<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_LOGIN+++[] | string | `NONE` |
| <a id="quarkus-mailer_quarkus-mailer-auth-methods"></a> [`quarkus.mailer.auth-methods`](#quarkus-mailer_quarkus-mailer-auth-methods) config_property_copy_button:+++quarkus.mailer.auth-methods+++[]<br><br>`quarkus.mailer."mailer-name".auth-methods` config_property_copy_button:+++quarkus.mailer."mailer-name".auth-methods+++[]<br><br>Sets the allowed authentication methods. These methods will be used only if the server supports them. If not set, all supported methods may be used.<br><br>The list is given as a space separated list, such as `DIGEST-MD5 CRAM-SHA256 CRAM-SHA1 CRAM-MD5 PLAIN LOGIN`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_AUTH_METHODS+++[] | string |  |
| <a id="quarkus-mailer_quarkus-mailer-key-store-password"></a> [`quarkus.mailer.key-store-password`](#quarkus-mailer_quarkus-mailer-key-store-password) config_property_copy_button:+++quarkus.mailer.key-store-password+++[]<br><br>`quarkus.mailer."mailer-name".key-store-password` config_property_copy_button:+++quarkus.mailer."mailer-name".key-store-password+++[]<br><br>Sets the trust store password if any.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_KEY_STORE_PASSWORD+++[] | string |  |
| <a id="quarkus-mailer_quarkus-mailer-truststore-password"></a> [`quarkus.mailer.truststore.password`](#quarkus-mailer_quarkus-mailer-truststore-password) config_property_copy_button:+++quarkus.mailer.truststore.password+++[]<br><br>`quarkus.mailer."mailer-name".truststore.password` config_property_copy_button:+++quarkus.mailer."mailer-name".truststore.password+++[]<br><br>Sets the trust store password if any. Note that the password is only used for JKS and PCK++#++12 trust stores.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_TRUSTSTORE_PASSWORD+++[] | string |  |
| <a id="quarkus-mailer_quarkus-mailer-truststore-paths"></a> [`quarkus.mailer.truststore.paths`](#quarkus-mailer_quarkus-mailer-truststore-paths) config_property_copy_button:+++quarkus.mailer.truststore.paths+++[]<br><br>`quarkus.mailer."mailer-name".truststore.paths` config_property_copy_button:+++quarkus.mailer."mailer-name".truststore.paths+++[]<br><br>Sets the location of the trust store files. If you use JKS or PCK++#++12, only one path is allowed. If you use PEM files, you can specify multiple paths.<br><br>The relative paths are relative to the application working directly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_TRUSTSTORE_PATHS+++[] | list of string |  |
| <a id="quarkus-mailer_quarkus-mailer-truststore-type"></a> [`quarkus.mailer.truststore.type`](#quarkus-mailer_quarkus-mailer-truststore-type) config_property_copy_button:+++quarkus.mailer.truststore.type+++[]<br><br>`quarkus.mailer."mailer-name".truststore.type` config_property_copy_button:+++quarkus.mailer."mailer-name".truststore.type+++[]<br><br>Sets the trust store type. By default, it guesses the type from the file name extension. For instance, `truststore.pem` will be seen as a PEM file, while `truststore.jks` will be seen as a JKS file. `truststore.p12` and `truststore.pfx` will both be seen as PKCS++#++12 files. Accepted values are: `JKS`, `PEM`, `PKCS`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_TRUSTSTORE_TYPE+++[] | string |  |
| <a id="quarkus-mailer_quarkus-mailer-multi-part-only"></a> [`quarkus.mailer.multi-part-only`](#quarkus-mailer_quarkus-mailer-multi-part-only) config_property_copy_button:+++quarkus.mailer.multi-part-only+++[]<br><br>`quarkus.mailer."mailer-name".multi-part-only` config_property_copy_button:+++quarkus.mailer."mailer-name".multi-part-only+++[]<br><br>Whether the mail should always been sent as multipart even if they don't have attachments. When sets to true, the mail message will be encoded as multipart even for simple mails without attachments.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_MULTI_PART_ONLY+++[] | boolean | `false` |
| <a id="quarkus-mailer_quarkus-mailer-allow-rcpt-errors"></a> [`quarkus.mailer.allow-rcpt-errors`](#quarkus-mailer_quarkus-mailer-allow-rcpt-errors) config_property_copy_button:+++quarkus.mailer.allow-rcpt-errors+++[]<br><br>`quarkus.mailer."mailer-name".allow-rcpt-errors` config_property_copy_button:+++quarkus.mailer."mailer-name".allow-rcpt-errors+++[]<br><br>Sets if sending allows recipients errors. If set to true, the mail will be sent to the recipients that the server accepted, if any.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_ALLOW_RCPT_ERRORS+++[] | boolean | `false` |
| <a id="quarkus-mailer_quarkus-mailer-pipelining"></a> [`quarkus.mailer.pipelining`](#quarkus-mailer_quarkus-mailer-pipelining) config_property_copy_button:+++quarkus.mailer.pipelining+++[]<br><br>`quarkus.mailer."mailer-name".pipelining` config_property_copy_button:+++quarkus.mailer."mailer-name".pipelining+++[]<br><br>Enables or disables the pipelining capability if the SMTP server supports it.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_PIPELINING+++[] | boolean | `true` |
| <a id="quarkus-mailer_quarkus-mailer-pool-cleaner-period"></a> [`quarkus.mailer.pool-cleaner-period`](#quarkus-mailer_quarkus-mailer-pool-cleaner-period) config_property_copy_button:+++quarkus.mailer.pool-cleaner-period+++[]<br><br>`quarkus.mailer."mailer-name".pool-cleaner-period` config_property_copy_button:+++quarkus.mailer."mailer-name".pool-cleaner-period+++[]<br><br>Sets the connection pool cleaner period. Zero disables expiration checks and connections will remain in the pool until they are closed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_POOL_CLEANER_PERIOD+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-mailer_quarkus-mailer) | `1S` |
| <a id="quarkus-mailer_quarkus-mailer-keep-alive-timeout"></a> [`quarkus.mailer.keep-alive-timeout`](#quarkus-mailer_quarkus-mailer-keep-alive-timeout) config_property_copy_button:+++quarkus.mailer.keep-alive-timeout+++[]<br><br>`quarkus.mailer."mailer-name".keep-alive-timeout` config_property_copy_button:+++quarkus.mailer."mailer-name".keep-alive-timeout+++[]<br><br>Set the keep alive timeout for the SMTP connection. This value determines how long a connection remains unused in the pool before being evicted and closed. A timeout of 0 means there is no timeout.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_KEEP_ALIVE_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-mailer_quarkus-mailer) | `300S` |
| <a id="quarkus-mailer_quarkus-mailer-ntlm-workstation"></a> [`quarkus.mailer.ntlm.workstation`](#quarkus-mailer_quarkus-mailer-ntlm-workstation) config_property_copy_button:+++quarkus.mailer.ntlm.workstation+++[]<br><br>`quarkus.mailer."mailer-name".ntlm.workstation` config_property_copy_button:+++quarkus.mailer."mailer-name".ntlm.workstation+++[]<br><br>Sets the workstation used on NTLM authentication.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_NTLM_WORKSTATION+++[] | string |  |
| <a id="quarkus-mailer_quarkus-mailer-ntlm-domain"></a> [`quarkus.mailer.ntlm.domain`](#quarkus-mailer_quarkus-mailer-ntlm-domain) config_property_copy_button:+++quarkus.mailer.ntlm.domain+++[]<br><br>`quarkus.mailer."mailer-name".ntlm.domain` config_property_copy_button:+++quarkus.mailer."mailer-name".ntlm.domain+++[]<br><br>Sets the domain used on NTLM authentication.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_NTLM_DOMAIN+++[] | string |  |
| <a id="quarkus-mailer_quarkus-mailer-approved-recipients"></a> [`quarkus.mailer.approved-recipients`](#quarkus-mailer_quarkus-mailer-approved-recipients) config_property_copy_button:+++quarkus.mailer.approved-recipients+++[]<br><br>`quarkus.mailer."mailer-name".approved-recipients` config_property_copy_button:+++quarkus.mailer."mailer-name".approved-recipients+++[]<br><br>Allows sending emails to these recipients only.<br><br>Approved recipients are compiled to a `Pattern` and must be a valid regular expression. The created `Pattern` is case-insensitive as emails are case insensitive. Provided patterns are trimmed before being compiled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_APPROVED_RECIPIENTS+++[] | list of [Pattern](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html) |  |
| <a id="quarkus-mailer_quarkus-mailer-log-rejected-recipients"></a> [`quarkus.mailer.log-rejected-recipients`](#quarkus-mailer_quarkus-mailer-log-rejected-recipients) config_property_copy_button:+++quarkus.mailer.log-rejected-recipients+++[]<br><br>`quarkus.mailer."mailer-name".log-rejected-recipients` config_property_copy_button:+++quarkus.mailer."mailer-name".log-rejected-recipients+++[]<br><br>Log rejected recipients as warnings.<br><br>If false, the rejected recipients will be logged at the DEBUG level.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_LOG_REJECTED_RECIPIENTS+++[] | boolean | `false` |
| <a id="quarkus-mailer_quarkus-mailer-log-invalid-recipients"></a> [`quarkus.mailer.log-invalid-recipients`](#quarkus-mailer_quarkus-mailer-log-invalid-recipients) config_property_copy_button:+++quarkus.mailer.log-invalid-recipients+++[]<br><br>`quarkus.mailer."mailer-name".log-invalid-recipients` config_property_copy_button:+++quarkus.mailer."mailer-name".log-invalid-recipients+++[]<br><br>Log invalid recipients as warnings.<br><br>If false, the invalid recipients will not be logged and the thrown exception will not contain the invalid email address.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_LOG_INVALID_RECIPIENTS+++[] | boolean | `false` |
| <a id="quarkus-mailer_quarkus-mailer-timeout"></a> [`quarkus.mailer.timeout`](#quarkus-mailer_quarkus-mailer-timeout) config_property_copy_button:+++quarkus.mailer.timeout+++[]<br><br>`quarkus.mailer."mailer-name".timeout` config_property_copy_button:+++quarkus.mailer."mailer-name".timeout+++[]<br><br>Sets the timeout duration for blocking mailer operations. When using the blocking mailer (`io.quarkus.mailer.Mailer` interface), this timeout determines how long to wait for the mail to be sent before throwing a `java.util.concurrent.TimeoutException`.<br><br>This prevents indefinite thread blocking when SMTP servers are misconfigured or unreachable. A value of 0 means no timeout (wait indefinitely) - which is not recommended.<br><br>Default is 60 seconds.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-mailer_quarkus-mailer) | `60S` |
| <a id="quarkus-mailer_quarkus-mailer-max-mails-per-connection"></a> [`quarkus.mailer.max-mails-per-connection`](#quarkus-mailer_quarkus-mailer-max-mails-per-connection) config_property_copy_button:+++quarkus.mailer.max-mails-per-connection+++[]<br><br>`quarkus.mailer."mailer-name".max-mails-per-connection` config_property_copy_button:+++quarkus.mailer."mailer-name".max-mails-per-connection+++[]<br><br>Sets the max emails count per connection before it gets closed.<br><br>Some SMTP servers have the requirement to allow only a number of emails sent per connection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MAILER_MAX_MAILS_PER_CONNECTION+++[] | long |  |

<a id="duration-note-anchor-quarkus-mailer_quarkus-mailer"></a>

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
