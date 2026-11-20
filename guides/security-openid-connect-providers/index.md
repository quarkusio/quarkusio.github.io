# Configuring well-known OpenID Connect providers

This document explains how to configure well-known social OIDC and OAuth2 providers.

## Introduction

If you use [OpenID Connect Authorization Code Flow](/guides/security-oidc-code-flow-authentication/index.md) to protect Quarkus endpoints, then you need to configure Quarkus to tell it how to connect to OpenID Connect providers, how to authenticate to such providers, which scopes to use, and so on.

Sometimes you need to use the configuration to work around the fact that some providers do not implement OpenID Connect completely or when they are in fact [OAuth2 providers only](/guides/security-oidc-code-flow-authentication/index.md#oauth2).

The configuration of such providers can become complex, very technical and difficult to understand.

`quarkus.oidc.provider` configuration property has been introduced to refer to well-known OpenID Connect and OAuth2 providers. This property can be used to refer to a provider such as `github` with only a minimum number of customizations required, typically, an account specific `client id`, `client secret` and some properties have to be set to complete the configuration.

This property can be used in `application.properties` and in custom [TenantConfigResolvers](/guides/security-openid-connect-multitenancy/index.md#tenant-config-resolver) if the tenant configurations are created dynamically.

## Well Known Providers

<a id="apple"></a>
### Apple

In order to set up OIDC for Apple you need to create a developer account, and sign up for the 99$/year program, but you cannot test your application on `localhost` like most other OIDC providers: you will need to run it over HTTPS and make it publicly accessible, so for development purposes you may want to use a service such as <https://ngrok.com>.

Go to [Create a new identifier](https://developer.apple.com/account/resources/identifiers/list) and press `+`

![oidc apple 1](/guides/images/oidc-apple-1.png)

Don't touch anything, keep `App IDs` selected, and press `Continue`:

![oidc apple 2](/guides/images/oidc-apple-2.png)

Don't touch anything, keep `App` selected, and press `Continue`:

![oidc apple 3](/guides/images/oidc-apple-3.png)

Enter a description and a Bundle ID (use your application package name):

![oidc apple 4](/guides/images/oidc-apple-4.png)

Then scroll down to find the `Sign in with Apple` Capability, select it, and press `Continue`:

![oidc apple 5](/guides/images/oidc-apple-5.png)

Write down your App ID Prefix, then press `Register`:

![oidc apple 6](/guides/images/oidc-apple-6.png)

Back on the `Identifiers` page, press `+`:

![oidc apple 7](/guides/images/oidc-apple-7.png)

Select `Service IDs` and press `Continue`:

![oidc apple 8](/guides/images/oidc-apple-8.png)

Enter a description and Bundle ID (use your application package name), then press `Continue`:

![oidc apple 9](/guides/images/oidc-apple-9.png)

Now press `Register`:

![oidc apple 10](/guides/images/oidc-apple-10.png)

Back on the service list, click on your newly created service:

![oidc apple 11](/guides/images/oidc-apple-11.png)

Enable `Sign in with Apple` and press `Configure`:

![oidc apple 12](/guides/images/oidc-apple-12.png)

Add your domain and return URL (set to `<host>/apple`) and press `Next`:

![oidc apple 13](/guides/images/oidc-apple-13.png)

Now press `Done`:

![oidc apple 14](/guides/images/oidc-apple-14.png)

Now press `Continue`:

![oidc apple 15](/guides/images/oidc-apple-15.png)

And now press `Save`:

![oidc apple 16](/guides/images/oidc-apple-16.png)

Go to the [Keys](https://developer.apple.com/account/resources/authkeys/list) page on the left menu, and press `+`:

![oidc apple 17](/guides/images/oidc-apple-17.png)

Fill in a key name, enable `Sign in with Apple`, and press `Configure`:

![oidc apple 18](/guides/images/oidc-apple-18.png)

Select your Primary App ID and press `Save`:

![oidc apple 19](/guides/images/oidc-apple-19.png)

Back on the key page, press `Continue`:

![oidc apple 20](/guides/images/oidc-apple-20.png)

Now press `Register`:

![oidc apple 21](/guides/images/oidc-apple-21.png)

Write down your `Key ID`, download your key and save it to your Quarkus application in `src/main/resources/AuthKey_<KEYID>.p8`:

![oidc apple 22](/guides/images/oidc-apple-22.png)

You can now configure your `application.properties`:

```properties
quarkus.oidc.provider=apple
quarkus.oidc.client-id=<Bundle ID>
quarkus.oidc.credentials.jwt.key-file=AuthKey_<Key ID>.p8
quarkus.oidc.credentials.jwt.token-key-id=<Key ID>
quarkus.oidc.credentials.jwt.issuer=<App ID Prefix>
quarkus.oidc.credentials.jwt.subject=<Bundle ID}
```

`quarkus.oidc.provider=apple` will request Apple to add `openid`, `email` and `name` scopes to issued access tokens. For information about overriding these scopes or requesting more scopes, see the  [Provider scopes](#provider-scope) section.

<a id="discord"></a>
### Discord

Create a [Discord application](https://discord.com/developers/applications):

![oidc discord 1](/guides/images/oidc-discord-1.png)

You now can get your client id and secret:

![oidc discord 2](/guides/images/oidc-discord-2.png)

You can now configure your `application.properties`:

```properties
quarkus.oidc.provider=discord
quarkus.oidc.client-id=<Client ID>
quarkus.oidc.credentials.client-secret=<Client Secret>
```

<a id="facebook"></a>
### Facebook

Facebook will not let you test your application on `localhost` like most other OIDC providers: you will need to run it over HTTPS and make it publicly accessible, so for development purposes you may want to use a service such as <https://ngrok.com>.

In order to set up OIDC for Facebook start by [Creating an application](https://developers.facebook.com/apps/create/), select `Other` as an app type, and click `Next`.

![oidc facebook 1](/guides/images/oidc-facebook-1.png)

Now choose your application type. For this guide choose `Consumer` and click `Next` until you reach the screen below. Now enter an application name, and contact email, and press `Create app`:

![oidc facebook 2](/guides/images/oidc-facebook-2.png)

On the app page, click `Set up` on the `Facebook login` product:

![oidc facebook 3](/guides/images/oidc-facebook-3.png)

On the `Quickstart` page click on `Facebook login > Settings` on the left menu:

![oidc facebook 4](/guides/images/oidc-facebook-4.png)

First click on `Get Advanced Access` to switch `public_profile` to advanced access. Then enter your `Redirect URIs` (set to `<host>/facebook`) and press `Save changes`:

![oidc facebook 5](/guides/images/oidc-facebook-5.png)

Now go to `Settings > Basic` on the left-hand menu, and write down your `App ID` and `App secret`:

![oidc facebook 6](/guides/images/oidc-facebook-6.png)

You can now configure your `application.properties`:

```properties
quarkus.oidc.provider=facebook
quarkus.oidc.client-id=<App ID>
quarkus.oidc.credentials.secret=<App secret>
```

`quarkus.oidc.provider=facebook` will request Facebook to add `email` and `public_profile` scopes to issued access tokens. For information about overriding these scopes or requesting more scopes, see the  [Provider scopes](#provider-scope) section.

<a id="github"></a>
### GitHub

In order to set up OIDC for GitHub you need to create a new OAuth application in your [GitHub developer settings](https://github.com/settings/applications/new):

![oidc github 1](/guides/images/oidc-github-1.png)

Make sure to fill in the appropriate details, but more importantly the Authorization Callback URL, set to `http://localhost:8080/github` (if you intend to test this using the Quarkus dev mode).

Now click on `Register application` and you'll be shown your application page:

![oidc github 2](/guides/images/oidc-github-2.png)

You need to click on `Generate a new client secret`, confirm your credentials, and write down your Client ID and Client secret (especially that one, because you will not be able to see it again later from that page, but you can always recreate one, do don't worry too much):

![oidc github 3](/guides/images/oidc-github-3.png)

Now add the following configuration to your `application.properties`:

```properties
quarkus.oidc.provider=github
quarkus.oidc.client-id=<Client ID>
quarkus.oidc.credentials.secret=<Secret>
```

> [!NOTE]
> By default, Quarkus GitHub provider submits the client id and secret in the HTTP Authorization header. However, GitHub may require that both client id and secret are submitted as form parameters instead. When you get HTTP 401 error after logging in to GitHub and being redirected back to Quarkus MCP server, replace the `quarkus.oidc.credentials.secret=<Secret>` property with the following two properties instead:
>
> ```properties
> quarkus.oidc.credentials.client-secret.method=post
> quarkus.oidc.credentials.client-secret.value=<Secret>
> ```

`quarkus.oidc.provider=github` will request GitHub to add a `user:email` scope to issued access tokens. For information about overriding this scope or requesting more scopes, see the  [Provider scopes](#provider-scope) section.

> [!TIP]
> You can also send access tokens issued by GitHub to `quarkus.oidc.application-type=service` or `quarkus.oidc.application-type=hybrid` Quarkus applications.

<a id="google"></a>
### Google

In order to set up OIDC for Google you need to create a new project in your [Google Cloud Platform console](https://console.cloud.google.com/projectcreate):

Pick a project name and click on `CREATE`.

![oidc google 1](/guides/images/oidc-google-1.png)

Now make sure you select your project in the top selector, and click on the left-hand bar menu on `APIs and Services > OAuth consent screen`:

![oidc google 2](/guides/images/oidc-google-2.png)

Select `External` to authorise any Google user to log in to your application and press `CREATE`:

![oidc google 3](/guides/images/oidc-google-3.png)

Now you can fill in your application name, your support email, your developer contact information and press `SAVE AND CONTINUE`:

![oidc google 4](/guides/images/oidc-google-4.png)

Do not add any scopes on the next page, and press `SAVE AND CONTINUE`:

![oidc google 5](/guides/images/oidc-google-5.png)

Do not add any test user on the next page, and press `SAVE AND CONTINUE`:

![oidc google 6](/guides/images/oidc-google-6.png)

Click on the top menu `CREATE CREDENTIALS` > `OAuth client ID`:

![oidc google 7](/guides/images/oidc-google-7.png)

Select `Web application` as `Application type`, and add `http://localhost:8080/google` in the `Authorised redirect URIs` list, then press `CREATE`:

![oidc google 8](/guides/images/oidc-google-8.png)

Copy your Client ID and Client Secret:

![oidc google 9](/guides/images/oidc-google-9.png)

You can now configure your `application.properties`:

```properties
quarkus.oidc.provider=google
quarkus.oidc.client-id=<Client ID>
quarkus.oidc.credentials.secret=<Secret>
```

`quarkus.oidc.provider=google` will request Google to add `openid`, `email` and `profile` scopes to issued access tokens. For information about overriding these scopes or requesting more scopes, see the  [Provider scopes](#provider-scope) section.

> [!TIP]
> You can also send access tokens issued by Google to `quarkus.oidc.application-type=service` or `quarkus.oidc.application-type=hybrid` Quarkus applications.

<a id="linkedin"></a>
### LinkedIn

Create a [LinkedIn application](https://developer.linkedin.com/):

![oidc linkedin 1](/guides/images/oidc-linkedin-1.png)

Add the `Sign In with LinkedIn using OpenId Connect` product:

![oidc linkedin 2](/guides/images/oidc-linkedin-2.png)

You now can get your client id and secret. Don't forget to also add the authorized redirect URLs for your application:

![oidc linkedin 3](/guides/images/oidc-linkedin-3.png)

You can now configure your `application.properties`:

```properties
quarkus.oidc.provider=linkedin
quarkus.oidc.client-id=<Client ID>
quarkus.oidc.credentials.client-secret=<Client Secret>
```

<a id="mastodon"></a>
### Mastodon

Create a [Mastodon account](https://joinmastodon.org/). You must [pick a server](https://joinmastodon.org/servers), for example, `mastodon.social`. Select a `Development` option in your account and register an application, for example:

![oidc mastodon register app](/guides/images/oidc-mastodon-register-app.png)

Select the registered application:

![oidc mastodon registered apps](/guides/images/oidc-mastodon-registered-apps.png)

and note its client id and client secret properties, use them to configure your `application.properties`:

```properties
quarkus.oidc.provider=mastodon
quarkus.oidc.client-id=<Client ID>
quarkus.oidc.credentials.client-secret.value=<Client Secret>
```

By default, `quarkus.oidc.provider=mastodon` sets `quarkus.oidc.auth-server-url` to the `https://mastodon.social` Mastodon server address.

You can override `quarkus.oidc.auth-server-url` if you have created your account in another Mastodon server, for example:

```properties
quarkus.oidc.provider=mastodon
quarkus.oidc.auth-server-url=https://infosec.exchange
quarkus.oidc.client-id=<Client ID>
quarkus.oidc.credentials.client-secret.value=<Client Secret>
```

<a id="microsoft"></a>
### Microsoft

In order to set up OIDC for Microsoft you need to go to your [Microsoft Azure Portal](https://portal.azure.com), and search for `Microsoft Entra ID` formerly known as `Azure Active Directory` , then click on it:

![oidc microsoft 1](/guides/images/oidc-microsoft-1.png)

Once there, on the left side under `Manage`, click on `App registrations` then click on `New registration`:

![oidc microsoft 2](/guides/images/oidc-microsoft-2.png)

Fill in your application name, select `Accounts in any organizational directory (Any Azure AD directory - Multitenant) and personal Microsoft accounts (e.g. Skype, Xbox)` to allow anyone to log in, and add a `Web` Redirect URI as `http://localhost:8080/microsoft`, then click on `Register`:

![oidc microsoft 3](/guides/images/oidc-microsoft-3.png)

On that resulting page, copy the `Client Id` (under `Application (client) ID`, then click on `Add a certificate or secret`:

![oidc microsoft 4](/guides/images/oidc-microsoft-4.png)

Now, under `Client secrets (0)`, click on `New client secret`:

![oidc microsoft 5](/guides/images/oidc-microsoft-5.png)

Click on `Add` in that dialog without changing anything:

![oidc microsoft 6](/guides/images/oidc-microsoft-6.png)

On the resulting page, copy your `Secret Value`:

![oidc microsoft 7](/guides/images/oidc-microsoft-7.png)

You can now configure your `application.properties`:

```properties
quarkus.oidc.provider=microsoft
quarkus.oidc.client-id=<Client ID>
quarkus.oidc.credentials.secret=<Secret>
```

`quarkus.oidc.provider=microsoft` will request Microsoft to add `openid`, `email` and `profile` scopes to issued access tokens. For information about overriding these scopes or requesting more scopes, see the  [Provider scopes](#provider-scope) section.

> [!TIP]
> You can also send access tokens issued by Microsoft to `quarkus.oidc.application-type=service` or `quarkus.oidc.application-type=hybrid` Quarkus applications but you may need to set `quarkus.oidc.verify-access-token-with-user-info` configuration property to `true` if access tokens issued by Microsoft are not in JWT format.

> [!NOTE]
> Some Microsoft services may issue tokens whose signatures can only be verified if the current token's `nonce` header is reset with its SHA-256 digest value. If your application has to deal with such tokens then please enable an Azure token customizer:
>
> ```properties
> quarkus.oidc.provider=microsoft
> quarkus.oidc.client-id=<Client ID>
> quarkus.oidc.credentials.secret=<Secret>
> quarkus.oidc.token.customizer-name=azure-access-token-customizer
> ```

<a id="slack"></a>
### Slack

Create a [Slack application](https://api.slack.com/authentication/sign-in-with-slack#setup):

![oidc slack 1](/guides/images/oidc-slack-1.png)

Select application name, workspace, and remember it, you will need it later:

![oidc slack 2](/guides/images/oidc-slack-2.png)

Please save client id and secret displayed on the next page, you will need them later:

![oidc slack 3](/guides/images/oidc-slack-3.png)

Configure redirect URLs. Slack provider requires HTTPS protocol, for development purposes, you can use ngrok:

![oidc slack 4](/guides/images/oidc-slack-4.png)

You can now configure your `application.properties`:

```properties
quarkus.oidc.provider=slack
quarkus.oidc.client-id=<Client ID>
quarkus.oidc.credentials.secret=<Client Secret>
quarkus.oidc.authentication.extra-params.team=quarkus-slack <1>
```

1. Use the `team` parameter to refer to the workspace you chose during the Slack OIDC application registration.

Open your browser and navigate to your application `https://<your ngrok instance>.ngrok-free.app/`. Quarkus will redirect you to Slack provider on the first request where you can grant required permissions:

![oidc slack 5](/guides/images/oidc-slack-5.png)

<a id="spotify"></a>
### Spotify

Create a [Spotify application](https://developer.spotify.com/documentation/general/guides/authorization/app-settings/):

![oidc spotify 1](/guides/images/oidc-spotify-1.png)

Don't forget to add `http://localhost:8080` as a redirect URI for testing during development purposes. You should get a client id and secret generated once a Spotify application setup has been complete, for example:

![oidc spotify 2](/guides/images/oidc-spotify-2.png)

You can now configure your `application.properties`:

```properties
quarkus.oidc.provider=spotify
quarkus.oidc.client-id=<Client ID>
quarkus.oidc.credentials.secret=<Client Secret>
```

`quarkus.oidc.provider=spotify` will request Spotify to add `user-read-private` and `user-read-email` scopes to issued access tokens. For information about overriding these scopes or requesting more scopes, see the  [Provider scopes](#provider-scope) section.

<a id="strava"></a>
### Strava

Create a [Strava application](https://www.strava.com/settings/api):

![oidc strava 1](/guides/images/oidc-strava-1.png)

For example, set `Category` to `SocialMotivation`, and set `ApplicationCallbackDomain` to either `localhost` or the domain name provided by Ngrok, see the [HTTPS Redirect URI](#redirect_url) for more information.

You can now configure your `application.properties`:

```properties
quarkus.oidc.provider=strava
quarkus.oidc.client-id=<Client ID>
quarkus.oidc.credentials.client-secret=<Client Secret>
# default value is '/strava'
quarkus.oidc.authentication.redirect-path=/fitness/welcome <1>
```

1. Strava does not enforce that the redirect (callback) URI which is provided as an authorization code flow parameter is equal to the URI registered in the Strava application because it only requires configuring `ApplicationCallbackDomain`. For example, if `ApplicationCallbackDomain` is set to `www.my-strava-example.com`, Strava will accept redirect URIs such as `www.my-strava-example.com/a`, `www.my-strava-example.com/path/a`, which is not recommended by OAuth2 best security practices, see [Insufficient redirect_uri validation](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-security-topics#name-insufficient-redirect-uri-v) for more information. Therefore you must configure a redirect path when working with the Strava provider and Quarkus will enforce that the current request path matches the configured `quarkus.oidc.authentication.redirect-path` value before completing the authorization code flow. See the [Exact redirect URI match](#exact_redirect_uri_match) for more information.

<a id="twitch"></a>
### Twitch

Create a [Twitch application](https://dev.twitch.tv/console/apps):

![oidc twitch 1](/guides/images/oidc-twitch-1.png)

You can now configure your `application.properties`:

```properties
quarkus.oidc.provider=twitch
quarkus.oidc.client-id=<Client ID>
quarkus.oidc.credentials.client-secret.value=<Client Secret>
```

<a id="x"></a>
### <a id="twitter"></a> X (formerly Twitter)

You can use X (formerly Twitter) for OIDC login, but at the moment, it restricts access to the user's email, which means you will have to obtain it and verify it yourself.

In order to set up OIDC for X, start by [Creating a project](https://developer.twitter.com/en/portal/projects/new), enter a project name, and press `Next`:

![oidc twitter 1](/guides/images/oidc-twitter-1.png)

Enter a use case, and press `Next`:

![oidc twitter 2](/guides/images/oidc-twitter-2.png)

Enter a project description, and press `Next`:

![oidc twitter 3](/guides/images/oidc-twitter-3.png)

Now enter an application name, and press `Next`:

![oidc twitter 4](/guides/images/oidc-twitter-4.png)

Write down your keys, because they will not be displayed again, and press `App Settings`:

![oidc twitter 5](/guides/images/oidc-twitter-5.png)

Navigate down to the `User authentication settings` section and press `Set up`:

![oidc twitter 6](/guides/images/oidc-twitter-6.png)

Check the `OAuth 2.0` check box:

![oidc twitter 7](/guides/images/oidc-twitter-7.png)

Select `Web App` as application type, then fill in your application details. For the `Callback URI`, register the public URL where your Quarkus application completes the X authorization code flow. If the callback is not the application root such as `http://localhost:8080/`, set `quarkus.oidc.authentication.redirect-path` and register that full URL with X; See the [Exact redirect URI match](#exact_redirect_uri_match) section for how Quarkus validates redirect URIs against provider configuration.

> [!NOTE]
> X doesn't require https usage in practice, but won't accept your `Website URL` without it, so you can still use ngrok for it.

Now press `Save`:

![oidc twitter 8](/guides/images/oidc-twitter-8.png)

You can now copy your `Client ID` and `Client Secret` and press `Done`:

![oidc twitter 9](/guides/images/oidc-twitter-9.png)

You can now configure your `application.properties`:

```properties
quarkus.oidc.provider=x
quarkus.oidc.client-id=<Client ID>
quarkus.oidc.credentials.secret=<Client Secret>
```

`quarkus.oidc.provider=x` will request X to add `offline.access`, `tweet.read` and `users.read` scopes to issued access tokens. For information about overriding these scopes or requesting more scopes, see the  [Provider scopes](#provider-scope) section.

> [!NOTE]
> X provider requires Proof Key for Code Exchange (PKCE) which is supported by the `quarkus.oidc.provider=x` declaration. Quarkus has to encrypt the current PKCE code verifier in a state cookie while the authorization code flow with X is in progress and it will generate a secure random secret key for encrypting it.
>
> You can provide your own secret key for encrypting the PKCE code verifier if you prefer with the `quarkus.oidc.authentication.state-secret` property but note that this secret should be 32 characters long, and an error will be reported if it is less than 16 characters long.

> [!NOTE]
> X is the new name of Twitter. You can continue using `quarkus.oidc.provider=twitter` for the time being but it might become deprecated in the future.

<a id="provider-scope"></a>
## Provider scopes

Each provider declaration will request one or more token scopes added to access tokens issued by a given provider. For example, `quarkus.oidc.provider=google` will request `Google` to add `openid`, `email` and `profile` scopes.

You can override these scopes with `quarkus.oidc.authentication.scopes` property, for example, if you work with `Google` and would not like to have an `email` scope added to access tokens:

```properties
quarkus.oidc.provider=google
quarkus.oidc.authentication.scopes=oidc,profile
```

You may also want to add one or more scopes in addition to the scopes requested by default. For example, if you would like to use an access token issued by `Google` to access `Google Calendar` service, you can do it like this:

```properties
quarkus.oidc.provider=google
quarkus.oidc.authentication.extra-params.scope=https://www.googleapis.com/auth/calendar
```

It is simpler than using `quarkus.oidc.authentication.scopes` to add a new scope, because `quarkus.oidc.authentication.scopes` overrides the scopes already set by the provider declaration, which is why you need to list all the required scopes in this case:

```properties
quarkus.oidc.provider=google
quarkus.oidc.authentication.scopes=oidc,email,profile,https://www.googleapis.com/auth/calendar
```

## Support for multiple providers

If you would like to support authenticating users with more than one provider then a provider-specific tenant configuration resolution must be supported.

For more information, see the Quarkus [Using OpenID Connect (OIDC) Multi-Tenancy](/guides/security-openid-connect-multitenancy/index.md#static-tenant-resolution) guide.

## Access provider services with token propagation

Sometimes, only authenticating users with a social provider is not enough. A provider-specific service also needs to be accessed for the Quarkus OIDC `web-app` application to fetch or update data from the provider service on behalf of the currently authenticated user.

As mentioned in the [OIDC code flow mechanism for protecting web applications](/guides/security-oidc-code-flow-authentication/index.md) guide, ID and access tokens are returned after the authorization code flow has been completed, with some providers like `GitHub` returning an access token only. It is this access token that has to be propagated to services such as `Google Calendar`, or `Spotify Playlists` for the currently authenticated user to be able to use such services.

You do not have to bring provider-specific libraries in order to achieve this, but instead you can use a reactive `Token Propagation` filter, which can be bound to a given REST client with a simple annotation. For more information, see the Quarkus [Access token propagation](/guides/security-openid-connect-client-reference/index.md#token-propagation-rest) guide.

For example, after you have configured the [Google](#google) provider, you can have events added to the user's Google Calendar by using a REST client as shown in the following example:

```java
package org.acme.calendar;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.token.propagation.common.AccessToken;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(configKey="google-calendar-api")
@AccessToken <1>
@Path("/calendars/primary")
public interface GoogleCalendarClient {
    @POST
    @Path("events")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Uni<String> addEvent(Event event);
    public static class Event {
        public String summary;
        public String kind = "calendar#event";
        public Time start;
        public Time end
    }

    public static class Time {
        public String dateTime;
        public String timeZone = "Europe/CET";
        public Time() {
        }
        public Time(String value) {
            dateTime = value;
	}
    }
}
```

1. The `@AccessToken` annotation enables an access token propagation to the target service.

Finally,  you need to configure the Google Calendar address and request  the Google Calendar scope for an access token, as outlined in the following example:

```properties
quarkus.oidc.provider=google
quarkus.oidc.client-id=<Client ID>
quarkus.oidc.credentials.secret=<Secret>

# Add a required calendar scope
quarkus.oidc.authentication.extra-params.scope=https://www.googleapis.com/auth/calendar

# Point REST client to Google Calendar endpoint
quarkus.rest-client.google-calendar-api.url=https://www.googleapis.com/calendar/v3
```

Now you are ready to have users authenticated with Google and support updating their `Google` calendars on their behalf, for example:

```java
package org.acme.calendar;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.oidc.IdToken;
import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/calendar")
@Authenticated
public class CalendarService {

    @Inject
    @IdToken
    JsonWebToken jwt;

    @Inject
    @RestClient
    GoogleCalendarClient calendarClient;

    @GET
    @Path("/event")
    @Produces("text/plain")
    public Uni<String> get() {
        return calendarClient.addEvent(new Event()).onItem()
           .transform(c -> ("Hello " + jwt.getName() + ", new event: " + c));
    }
}
```

You must update the application registered with the [Google](#google) provider to list `http://localhost:8080/calendar/event` as one of the authorized redirect URIs if you would like to test this endpoint on the local host, for example:

![oidc google authorized redirects](/guides/images/oidc-google-authorized-redirects.png)

You might also have to register one or more test users:

![oidc google test users](/guides/images/oidc-google-test-users.png)

Follow the same approach if the endpoint must access other Google services.

The pattern of authenticating with a given provider, where the endpoint uses either an ID token or UserInfo (especially if an OAuth2-only provider such as `GitHub` is used) to get some information about the currently authenticated user and using an access token to access some downstream services (provider or application specific ones) on behalf of this user can be universally applied, irrespectively of which provider is used to secure the application.

<a id="exact_redirect_uri_match"></a>
## Exact redirect URI match

Most OIDC and OAuth2 providers with the exception of [Strava](#strava) will enforce that the authorization code flow can be completed only if the redirect URI matches precisely the redirect URI configured in a given provider's dashboard.

From the practical point of view, your Quarkus endpoint will most likely need to have the `quarkus.oidc.authentication.redirect-path` relative path property set to an initial entry path for all the authenticated users, for example, `quarkus.oidc.authentication.redirect-path=/authenticated`, which means that newly authenticated users will land on the `/authenticated` page, irrespectively of how many secured entry points your application has and which secured resource they initially accessed.

It is a typical flow for many OIDC `web-app` applications. Once the user lands on the initial secured page, your application can return an HTML page which uses links to guide users to other parts of the application or users can be immediately redirected to other application resources with the help of JAX-RS API.

If necessary, you can configure Quarkus to restore the original request URI after the authentication has been completed. For example:

```properties
quarkus.oidc.provider=strava <1>
quarkus.oidc.client-id=<Client ID>
quarkus.oidc.credentials.secret=<Secret>
quarkus.oidc.authentication.restore-path-after-redirect=true <2>
```

1. `strava` provider configuration is the only supported configuration which enforces the `quarkus.oidc.authentication.redirect-path` property with the `/strava` path which you can override with another path such as `/fitness`.
2. If the users access the `/run` endpoint before the authentication, then, once they have authenticated and been redirected to the configured redirect path such as `/strava`, they will land on the original request `/run` path.

You do not have to set `quarkus.oidc.authentication.redirect-path` immediately because Quarkus assumes the current request URL is an authorization code flow redirect URL if no `quarkus.oidc.authentication.redirect-path` is configured. For example, to test that a [Google](#google) authentication is working, you can have a Quarkus endpoint listening on `/google` and update the Google dashboard that `http://localhost:8080/google` redirect URI is supported. Setting `quarkus.oidc.authentication.redirect-path` property will be required once your secured application URL space grows.

<a id="redirect_url"></a>
## HTTPS Redirect URI

Some providers will only accept HTTPS-based redirect URIs. Tools such as [ngrok](https://ngrok.com/) [can be set up](https://linuxhint.com/set-up-use-ngrok/) to help testing such providers with Quarkus endpoints running on localhost in dev mode.

## Rate Limiting

Depending on your developer API subscription level, some providers may enforce a rather strict request rate limiting policy.

It may not be a problem when Quarkus fetches public verification keys from OIDC-compliant providers like the [Google](#google) provider and keeps verifying the user session with these keys locally. However, for pure OAuth2 providers where only an access token is available and which has to be verified indirectly by requesting UserInfo from the provider endpoint on every request done by an already authenticated user, it can become a problem.

Therefore, UserInfo is embedded in an internally generated ID token and is encrypted in the session cookie. You can disable it with `quarkus.oidc.cache-user-info-in-idtoken=false`.

Alternatively, use a default or custom UserInfo cache provider, please see the [Token Introspection and UserInfo cache](/guides/security-oidc-bearer-token-authentication/index.md#bearer-token-token-introspection-userinfo-cache) section of the "OpenID Connect (OIDC) Bearer token authentication" guide.

## References

- [OIDC code flow mechanism for protecting web applications](/guides/security-oidc-code-flow-authentication/index.md)
- [Quarkus Security overview](/guides/security-overview/index.md)
