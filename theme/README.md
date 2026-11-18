# Quarkus Roq theme

The look of quarkus.io, packaged as a [Roq](https://iamroq.dev/) theme so other sites can use it.

It provides:

- the `base` layout (`templates/theme-layouts/quarkus/base.html`), with `lang`, `head`, `header`, `footer`
  and `scripts` sections that sites can override,
- the header with the main navigation, call to action, language switcher and colour mode toggle
  (`templates/partials/quarkus/header.html`); the menus are part of the theme, so they are the same on every site
  using it, and their links point at https://quarkus.io/ unless `quarkus-website-theme.links-url` is set (quarkus.io,
  and the localized sites built from this repository, set it to `/` so the links stay on the site),
- the project footer and the minimal (Commonhaus) footer,
- the colours, colour modes (light, dark, system), grid and global styles, and the header and footer styles
  (`web/app/`),
- the logos, `colormode.js` and `mobile-nav.js` (`public/assets/`).

## How quarkus.io uses it

quarkus.io adds `theme/src/main/resources` to its resources (see the root `pom.xml`), so `./serve.sh` needs no
pre-build and theme changes hot-reload like any site file. `templates/layouts/base.html` overrides the theme
`base` layout to add the site head (meta, CSP, search, tracking), the community site banner and the guide scripts.

`theme/pom.xml` packages the same sources as a jar. Since quarkus.io never uses that jar, `example-minimal-site/` is a minimal
site built against it on every pull request (see the `theme` job in `.github/workflows/build.yml`).

## Using the theme in another site

The jar is published in a Maven repository at https://quarkus.io/maven/ on every deploy:

```xml
<repositories>
    <repository>
        <id>quarkus-website</id>
        <url>https://quarkus.io/maven/</url>
        <snapshots><enabled>true</enabled></snapshots>
    </repository>
</repositories>

<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-website-theme</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Then, in the site:

- use `layout: base` (or a layout based on it) for pages; the jar sets `site.theme=quarkus`,
- add the theme styles to a stylesheet in `web/` with `@use "quarkus-theme";` (`web/app/_quarkus-theme.scss` lists the
  theme stylesheets). It is a Sass partial, which isn't bundled on its own, so each site decides where the theme
  styles go relative to its own, usually first so the site styles can build on them, like quarkus.io's
  `web/main.scss`,
- provide the footer content in `projectfooter.yaml`, in the site data dir: `footer_text`, `links` and
  `more_links`, see quarkus.io's `_data/projectfooter.yaml`.

`example-minimal-site/` is a complete minimal site.

## Developing the theme

Run `./serve.sh` (or `./serve-noguides.sh`) at the repository root and edit the files in `theme/src/main/resources`.

To check the packaged jar like CI does:

```sh
./mvnw -f theme/pom.xml install
cd theme/example-minimal-site && QUARKUS_ROQ_GENERATOR_BATCH=true ../../mvnw package quarkus:run -DskipTests
./verify.sh target/roq
```

Keep in mind that a site using the jar differs from quarkus.io:

- the jar needs `META-INF/beans.xml`, otherwise its templates and web assets are ignored,
- templates use the alternative expression syntax (`{=expr}`), which `templates/.qute` enables for the jar,
- sites may use Qute strict rendering: guard optional data with `??`,
- styles reference theme images by URL, which `application.properties` keeps out of the bundling.
