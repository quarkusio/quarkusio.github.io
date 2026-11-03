# Quarkus.io Website

## Getting Started

These instructions will get you a copy of the Quarkus.io website up and running on your local machine for development and testing purposes.

### Prerequisites

- Java 21+
- Maven (or use the included `./mvnw` wrapper — no separate Maven install required)

### Running locally

1. Fork the [project repository](https://github.com/quarkusio/quarkusio.github.io), then clone your fork:
    ```sh
    git clone git@github.com:YOUR_USER_NAME/quarkusio.github.io.git
    ```
2. Change into the project directory:
    ```sh
    cd quarkusio.github.io
    ```
3. Start the dev server:
    ```sh
    ./serve.sh
    ```
    This runs `mvn quarkus:dev` and serves the full site (including guides) at [http://localhost:8042](http://localhost:8042).

    For a faster startup without guides:
    ```sh
    ./serve-noguides.sh
    ```

    Or with only the latest guides (latest and main branches):
    ```sh
    ./serve-only-latest-guides.sh
    ```

> [!NOTE]
> The startup process may take a minute or two on the first run while Maven downloads dependencies and Roq generates the site. Subsequent starts are faster. Once ready, you will see output like:
>
> ```
> Listening on: http://0.0.0.0:8042
> ```

5. Now browse to [http://localhost:8042](http://localhost:8042).


### Deploying to GitHub Pages

The website deployment is automatically performed by GitHub Actions (when commits are pushed to the `main` branch).
If for some reason you need to deploy from your local machine, follow these instructions:

1. Install the [act](https://github.com/nektos/act#installation) executable to run GitHub Actions locally
2. Run `act -s GITHUB_TOKEN=<GITHUB_TOKEN>`, where *<GITHUB_TOKEN>* needs to be replaced with a token that allows you to push to the https://github.com/quarkusio/quarkusio.github.io repository.

## Writing a blog

> [!WARNING]
> Using generative AI in *assisting* writing is fine, but please don't use it to write entire posts.
> Used badly, generative AI has a tendency to use complex words and phrasing. This makes the content hard to read and understand. Always review your blog with a human reader in mind, make sure it's factually correct and especially keep the human touch and opinions in the content. We want _your_ voice!

To write a blog:

- create an author entry in [_data/authors.yaml](https://github.com/quarkusio/quarkusio.github.io/blob/main/_data/authors.yaml)
  - `emailhash` you can get by running `echo -n your@email.org | md5sum` on Linux or `echo -n your@email.org | md5` on macOS using an email you have registered from the [Gravatar service](https://gravatar.com),

- create a blog entry under [content/posts](https://github.com/quarkusio/quarkusio.github.io/tree/main/content/posts)
  - the file name is `yyyy-mm-dd-slug.adoc` (the publication date of the blog).
- `tags` should be used with some care as an archive page is created for each of them. Below are some basic rules to try to follow:
  - `quarkus-release` used for Quarkus release blogs
  - `announcement` used for general announcements with some impact.
  - `extension` used for blogs related to a specific extension.
  - `user-story` used for stories from users/companies adopting Quarkus.
  - `development-tips` used for blogs with tips to develop using Quarkus or Quarkus itself.
  - add a tech specific tag like `kafka` if your post has a significant mention/relevance to that technology.
  - tags is a space-separated list: `tags: extension grpc`
  - tags must be in lowercase
- it's in asciidoc format, there is an example at [2019-06-05-quarkus-and-web-ui-development-mode.adoc](https://github.com/quarkusio/quarkusio.github.io/blob/main/content/posts/2019-06-05-quarkus-and-web-ui-development-mode.adoc)
  - Be aware that the `date` attribute in the file name defines when the article will be published. Posts with a future date will not be visible in production until that date arrives; the pull request preview, dev mode and `./serve.sh` show them regardless of date.
- send a pull request against the main branch and voilà



## Translations/Localization (l10n)

The primary site (quarkus.io) is written in English.

There are separate repositories for community driven localized versions of quarkus.io:

- [ja.quarkus.io](https://github.com/quarkusio/ja.quarkus.io) for Japanese
- [cn.quarkus.io](https://github.com/quarkusio/cn.quarkus.io) for Chinese (simplified)
- [es.quarkus.io](https://github.com/quarkusio/es.quarkus.io) for Spanish
- [pt.quarkus.io](https://github.com/quarkusio/pt.quarkus.io) for Brazilian Portuguese

If you want to contribute to those efforts read the README in those projects. If you would like to
start another translation, please open an issue in this main repo.

#### Enable DNS for l10n site

Once a localized site has enough of its content translated, DNS needs to be enabled. To do that get one of the Red Hat admins to submit
a ticket to IT asking for XX domain:

```
We need a CNAME record set up for XX.quarkus.io to have it serve out GitHub pages.

The CNAME record for XX.quarkus.io should point to "quarkusio.github.io.".
```

See Step 5 on https://docs.github.com/en/github/working-with-github-pages/managing-a-custom-domain-for-your-github-pages-site for more information.

## Using the Quarkus theme in your site

The look of quarkus.io (layout, header and navigation, footers, colours and light/dark modes) is available as a
[Roq](https://iamroq.dev/) theme, so other Roq sites, such as the localized sites, can look like quarkus.io.
Its sources are in [`theme/`](theme/), and it is published in a Maven repository at https://quarkus.io/maven/.

1. Add the repository and the theme to your site's `pom.xml`:

    ```xml
    <repositories>
        <repository>
            <id>quarkus-website</id>
            <url>https://quarkus.io/maven/</url>
            <snapshots><enabled>true</enabled></snapshots>
        </repository>
    </repositories>
    ```

    ```xml
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-website-theme</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    ```

2. Use the theme's `base` layout for your pages:

    ```html
    ---
    layout: base
    title: My page
    ---
    <h1>{=page.title}</h1>
    ```

3. Add the theme styles to a stylesheet in your site's `web/` directory (e.g. `web/style.scss`):

    ```scss
    @use "quarkus-theme";
    ```

    The theme styles are not bundled on their own, so that you decide where they go relative to your own styles:
    usually first, so your styles can build on them, as quarkus.io does at the top of [`web/main.scss`](web/main.scss).

4. Describe your footer in `projectfooter.yaml` (footer text and links), in your site's data directory. Start from
   quarkus.io's [`_data/projectfooter.yaml`](_data/projectfooter.yaml). The menus are part of the theme, so they are
   the same on every site using it, and their links point at https://quarkus.io/. (quarkus.io and the localized sites
   built from this repository set `quarkus-website-theme.links-url=/` in `config/application.properties`, so their
   menu links stay on the site, e.g. ja.quarkus.io/about.)

5. Optionally, customize the layout by overriding its `lang`, `head`, `header`, `footer` or `scripts` sections in your
   own `templates/layouts/base.html`, as quarkus.io does in [`templates/layouts/base.html`](templates/layouts/base.html):

    ```html
    ---
    theme-layout: base
    ---
    {#insert /}

    {#footer}
    {#include partials/quarkus/footer-minimal.html /}
    {/footer}
    ```

[`theme/example-minimal-site/`](theme/example-minimal-site/) is a complete minimal site using the theme, and [`theme/README.md`](theme/README.md)
has more details, including how to work on the theme itself.

## Contributing

Please read [CONTRIBUTING.md](https://github.com/quarkusio/quarkusio.github.io/tree/main/CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

> [!IMPORTANT]
> The guides are maintained in the main Quarkus repository and pull requests should be submitted there: https://github.com/quarkusio/quarkus/tree/main/docs/src/main/asciidoc.

## License

This website is licensed under the [Creative Commons Attribution 3.0](https://creativecommons.org/licenses/by/3.0/).
