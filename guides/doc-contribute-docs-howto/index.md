# Contribute to Quarkus documentation

Contribute to the documentation by using the recommended diataxis content types, steps, workflow, and style guidance to ensure the content successfully renders on the Quarkus website portal.

Quarkus docs use [AsciiDoc](https://asciidoc.org/) markup.

## Prerequisites

- You have an editor or IDE that provides syntax highlighting and previews for AsciiDoc, either natively or with a plugin.
- You have reviewed the following Quarkus contributor resources:
  - The [Documentation](https://github.com/quarkusio/quarkus/blob/main/CONTRIBUTING.md#documentation) section of the Quarkus "Contributing" guide.
  - The [Quarkus style and content guidelines](/guides/doc-reference/index.md) for the required syntax, preferred style, and other conventions.
  - The [Quarkus documentation content types](/guides/doc-concept/index.md).
- You have the [AsciiDoc syntax reference](https://docs.asciidoctor.org/asciidoc/latest/syntax-quick-reference/) nearby.

## Locate the source files for Quarkus docs

- AsciiDoc files are in the `src/main/asciidoc` directory within the `docs` module of the [Quarkus GitHub repository](https://github.com/quarkusio/quarkus/tree/main/docs).
- The Quarkus doc templates are located in the `src/main/asciidoc/_templates` directory within the `docs` module of the [Quarkus GitHub repository](https://github.com/quarkusio/quarkus/tree/main/docs).
- Configuration documentation is generated from JavaDoc comments in Java source files.
- Java, YAML, and other source files can also be [referenced](/guides/doc-reference/index.md#reference-source-code) by AsciiDoc files.
- The [Quarkus documentation](https://quarkus.io/guides/) menu page, also known as the doc index page, is sourced in the [quarkusio.github.io](https://github.com/quarkusio/quarkusio.github.io) repository.

## Create Quarkus content in AsciiDoc

To ensure that your content shows up correctly on the [Quarkus documentation home page](https://quarkus.io/guides/), use the following steps:

1. Decide on a [Diataxis](/guides/doc-concept/index.md) content type that best fits the content that you are contributing.

   > [!TIP]
   > To help you decide, see the content type descriptions in [Titles and headings](/guides/doc-reference/index.md#titles-headings) on the "About Quarkus documentation" page.


1. Go to the `src/main/asciidoc/_templates` directory, and make a copy of the relevant template for the content type you have chosen. Be sure to: \*\* Use the filename syntax of`<category>-<titlekeyword>-<titlekeyword>.adoc`. For example, `security-basic-authentication.adoc`. \*\* Include the diataxis type (concept, howto, reference, tutorial) in the file name if it makes sense to do so. For example, `telemetry-micrometer.adoc` is a reference, and `telemetry-micrometer-tutorial.adoc` is a tutorial. \*\* Save the file to the `docs/src/main/asciidoc` folder in the `quarkus` repository.

1. Set the minimum required header information to ensure that the content renders correctly in the website portal and on the documentation home page, as outlined in the following example:

   ```asciidoc
   [id="security-basic-authentication"] <1>
   = Secure a Quarkus application with basic authentication <2>
   include::_attributes.adoc[] <3>
   :diataxis-type: howto <4>
    <5>
   ```

   1. Set the `id` value to be the same as the file name but without the extension.
   2. For information about how to create a good title for each content type, see [Titles and headings](/guides/doc-reference/index.md#titles-headings) on the "Quarkus style and content guidelines" page.
   3. The `_attributes.adoc` include is required to ensure that attributes get resolved and the table of contents is generated.
   4. Specify the diataxis type: `concept`, `howto`, `reference`, or `tutorial`.
   5. Insert a blank line after all document attributes and before the abstract.

   > [!IMPORTANT]
   > Ensure there are no blank lines between the document id and title, the attribute include (`include::_attributes.adoc[]`) and the declaration of other document attributes(`:attribute:`).


1. Add the guide file to the appropriate category or subcategory in `docs/src/main/resources/categories.yaml`.

   For more information, see [Categories](/guides/doc-reference/index.md#categories) on the "Quarkus style and content guidelines" page.


1. Add an abstract that describes the purpose of the guide.

   > [!IMPORTANT]
   > The first sentence of the abstract must explain the value and some benefit of the content in less than 27 words because this automatically displays on the [Quarkus guides homepage](https://quarkus.io/guides/). There must also be a line break before and after the abstract.


For more information about the minimum header requirements, see [Document structure](/guides/doc-reference/index.md#doc-structure) on the "Quarkus style and content guidelines" page.

<a id="add-prerequisites"></a>
## Add a prerequisites section

For how-to and tutorial topics, include a prerequisites section just after the abstract. Declaring prerequisites clarifies the starting place for both how-to and tutorial content. Include them even though they might seem obvious to knowledgeable users.

**An example prerequisite with callout explanations**

```asciidoc
.Prerequisites <1>

:prerequisites-time: 30 minutes <2>
include::{includes}/prerequisites.adoc[] <3>
* <an additional prerequisite> <4>
```

1. Section heading for the prerequisites
2. Optional: An attribute that modifies the prerequisites
3. An include statement for the `prerequisites.adoc` file
4. Optional: An additional prerequisite not covered by the attributes

By default, `include::./_includes/prerequisites.adoc[]` inserts the following asciidoc:

```
To complete this guide, you need:

ifdef::prerequisites-time[]
* Roughly {prerequisites-time}
endif::[]
ifndef::prerequisites-time[]
* Roughly 15 minutes
endif::[]
* An IDE
ifdef::prerequisites-ide[{prerequisites-ide}]
* JDK 17+ installed with `JAVA_HOME` configured appropriately
ifndef::prerequisites-no-maven[]
* Apache Maven {maven-version}
endif::[]
ifdef::prerequisites-docker[]
* A working container runtime (Docker or xref:podman.adoc[Podman])
endif::[]
ifdef::prerequisites-docker-compose[]
* Docker and Docker Compose or xref:podman.adoc[Podman], and Docker Compose
endif::[]
ifndef::prerequisites-no-cli[]
* Optionally the xref:cli-tooling.adoc[Quarkus CLI] if you want to use it
endif::[]
ifndef::prerequisites-no-graalvm[]
ifndef::prerequisites-graalvm-mandatory[]
* Optionally Mandrel or GraalVM installed and xref:building-native-image.adoc#configuring-graalvm[configured appropriately] if you want to build a native executable (or Docker if you use a native container build)
endif::[]
ifdef::prerequisites-graalvm-mandatory[]
* Mandrel or GraalVM installed and xref:building-native-image.adoc#configuring-graalvm[configured appropriately]
endif::[]
endif::[]
```

Optionally, you can add, remove, or modify the default prerequisites by inserting the following attributes on the line before the `include::./_includes/prerequisites.adoc[]` macro.

- `{prerequisites-time}: <number of minutes>` overrides the default value of 15 minutes. For example, `{prerequisites-time}: 30` adds `* Roughly 30 minutes`.
- `{prerequisites-no-maven}` removes `* Apache Maven <maven version>`.
- `{prerequisites-docker}` adds `* A working container runtime (Docker or xref:podman.adoc[Podman])`.
- `{prerequisites-docker-compose}` adds `Docker and Docker Compose or xref:podman.adoc[Podman], and Docker Compose`.
- `{prerequisites-no-cli}` removes `* Optionally the xref:cli-tooling.adoc[Quarkus CLI] if you want to use it`.
- `{prerequisites-no-graalvm}` or `{prerequisites-graalvm-mandatory}` remove `* Optionally Mandrel or GraalVM installed and xref:building-native-image.adoc#configuring-graalvm[configured appropriately] if you want to build a native executable (or Docker if you use a native container build)`.
- `{prerequisites-graalvm-mandatory}` adds `* Mandrel or GraalVM installed and xref:building-native-image.adoc#configuring-graalvm[configured appropriately]`.

For more information about these attributes, inspect the content of the `docs/src/main/asciidoc/_includes/prerequisites.adoc` file.

## Retire and redirect an existing Quarkus AsciiDoc source file

As content evolves, you might want to restructure an existing piece of Quarkus content into one or more content types and retire the existing AsciiDoc source file.

If you are retiring or renaming a published Quarkus AsciiDoc source file, ensure that the restructure does not break existing bookmarks and links to the original content. Configure a URL redirect by adding an alias to the updated file:

1. Add an `aliases` entry to the file's frontmatter that lists the old URL:

   ---

   aliases:

   - /guides/\<original_asciidoc_filename> // \<1>

     ---

     \<1> The name of the original AsciiDoc source file that you are retiring, without the `.adoc` file extension.


<a id="anchors-howto"></a>
## Use anchors to cross-reference in-file and cross-file content

An anchor, also called an ID, can be defined almost anywhere in the document, including on a section title, discrete heading, paragraph, image, delimited block, inline phrase, etc.

The callout functions for these anchors vary based on whether you call a local ID or the ID from another file, but the anchored ID creation remains the same.

### Create an anchored ID

To create an ID for a new file or a section to which you want to refer, insert the anchor ID as follows:

- Use lower-case characters.
- Separate each word with a dash character.
- Enclose the ID in double square brackets.

In this section, we will use an anchor created above the level-2 heading for demonstration purposes.

```asciidoc
[[title-heading]]
== Title heading
```

### Call an anchored ID in the same file

To call an anchor created in the same file, insert the anchored ID in a `<<>>` xref macro.

**Inter-document anchored ID call example**

```asciidoc
<<title-heading>>
```

This macro creates an URL with a name automatically sourced from the anchored heading, section, or table.

> [!WARNING]
> Do not use the `<<>>` format with the verbatim heading or section description, such as `\<<Title heading>>`.

When you want to specify a non-default caption for your URL, specify the anchored ID and desired name separated by `,` without white space.

**Anchor with a custom URL caption example**

```asciidoc
<<title-heading,Title heading description that fits the context of your content>>
```

### Call an anchored ID from a different file

To call an anchor created in a different file, insert the anchor to an `xref` macro and specify the full name of the hosting file and the desired anchored ID.

**Cross-document anchored ID call example**

```asciidoc
xref:<other-file-name>.adoc#title-heading[Title heading]
```

Breaking this example apart, we are using the `xref` macro to refer to another file (`xref:<name-of-the-file>.adoc[Human-readable label]`) and inserting the anchor ID for the target section (`#title-heading`) just after the file name.

For more guidelines on writing cross-references, including the recommended path attributes, see [Cross-references](/guides/doc-reference/index.md#cross-references).

## Preview and build Quarkus documentation

Before you submit a pull request, preview the HTML output of your AsciiDoc source by using one of the following build methods:

- For minor documentation changes, you can use the AsciiDoc syntax highlighting and preview provided by your IDE.
- For significant changes or updates to generated configuration documentation, build the `docs` module locally and run the Vale linter as outlined in the following sections.

### Build the `docs` module locally

The following will build all modules in the Quarkus repository (except test modules) and install them in your local maven repository with the `999-SNAPSHOT` version:

```shell
./mvnw -DquicklyDocs
```

Running `-DquicklyDocs` produces:

- Generated AsciiDoc (`adoc` files) describing configuration properties in the `target/asciidoc/generated/config/` directory.
- AsciiDoc output (`html` files) in the `docs/target/generated-docs/` directory.
- YAML files containing metadata for all documents individually (`docs/target/indexByFile.yaml`) and grouped by document type (`target/indexByType.yaml`).
- YAML files that list metadata errors by file (`docs/target/errorsByFile.yaml`) and by error type (`docs/target/errorsByType.yaml`)

Review the resulting output and fix any issues before you submit your changes in a PR for review.

As you make changes, you can rebuild the `docs` module specifically to update the generated HTML:

```shell
./mvnw -f docs clean install
```

> [!NOTE]
> When updating extension configuration:
>
> 1. Revise the Javadoc in the extension
> 2. Build the extension to regenerate content in `target/asciidoc/generated/config/`
> 3. Build the `docs` module to view the rendered results.

## Lint documentation changes with Vale

Our builds use [Vale](https://vale.sh) to check grammar, style, and word usage in the English versions of our documents. We created our own Vale style ruleset to ensure that content aligns with the preferred Quarkus style guidelines.

> [!NOTE]
> - The Quarkus configuration for Vale is in `docs/.vale.ini`.
> - The Quarkus style rules are in YAML format in the `docs/.vale/styles` directory.

### Containerized Vale

This approach requires a working container runtime (Docker or [Podman](/guides/podman/index.md)).

The `docs` module has a JUnit test that will run the Vale linter in a container (using [Testcontainers](https://www.testcontainers.org/)). It verifies both Quarkus document metadata and Vale style rules.

Run the test in one of the following ways:

```shell
./mvnw -f docs test -Dvale -DvaleLevel=suggestion # <1>
./mvnw -f docs test -Dvale=git -DvaleLevel=warning # <2>
./mvnw -f docs test -Dvale='doc-.*' -DvaleLevel=error # <3>
```

1. Run the Vale linter for all `*.adoc` files in the `src/main/asciidoc` directory of the `docs` module. Include suggestions, warnings, and errors in the results.
2. Run the Vale linter for any modified `*.adoc` files in the `docs` module (`git status`). Include warnings and errors in the results.
3. Run the Vale linter for `*.adoc` files that match the regular expression (Java Pattern syntax). Include errors in the results.

### Use the Vale CLI

If you install the [Vale CLI](https://vale.sh/docs/vale-cli/installation/), you must specify the configuration file and the directory or list of files to scan:

```shell
# Run from the Quarkus project root
vale --config=docs/.vale.ini --minAlertLevel=warning docs/src/main/asciidoc

# Run from within the docs directory
vale --minAlertLevel=warning src/main/asciidoc
```

For more information, see the [Vale CLI Manual](https://vale.sh/manual/).

### Vale IDE plugins

[Vale IDE integrations](https://vale.sh/docs/integrations/guide/) require the Vale CLI to be installed.

Each IDE integration has its own configuration requirements. The Visual Studio Code IDE extension, for example, requires a definition of the Vale CLI path:

```json
"vale.valeCLI.path": "/path/to/vale"
```

## Creating pull requests for doc updates

Submit your proposed changes to the core Quarkus docs by [creating a pull request](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request-from-a-fork) against the `main` branch of the Quarkus repository from your own [repository fork](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/working-with-forks/about-forks).

Reviews for code and documentation have different (but overlapping) participants. To simplify collaborative review, either isolate changes to docs in separate PRs or ensure that a PR has a single focused purpose. For example:

- Create a single PR that adds a configuration option for an extension and updates related materials (how-to, reference) to explain the change.
- Create a single PR for related changes to a group of documents; some examples: Correcting the usage of a term, correcting a recurring error, or moving common content into a shared file.
- If there are extensive code changes and documentation changes, create a separate PR for the documentation changes and include the relationship in the issue description.

GitHub automatically adds the `area/documentation` label to pull requests that contain changes to documentation files.

For more information about managing pull requests, see [Information for Quarkus Committers](https://github.com/quarkusio/quarkus/blob/main/COMMITTERS.adoc).

### Automatic style checking on the PR diff

The Vale linter job automatically runs when a PR is created or updated. If your content updates do not align with the key style or terminology preferences of the Quarkus community, the updated line on the diff tab gets annotated with the Vale recommendations. To ensure that your content gets approved, fix the linter errors, warnings, and suggestions.

We welcome your feedback on the Quarkus documentation style guidelines.

If you disagree with the Vale results, add the yellow PR label `needs-vale-rule-tweak`.

## Previewing doc changes on the Quarkus website

After your PR is merged to `main` and the branch is synchronized with the [Quarkus.io](https://github.com/quarkusio/quarkusio.github.io/tree/develop/_generated-doc) website repository, you can preview the resulting build output on the [Main branch (SNAPSHOT)](https://quarkus.io/version/main/guides/) documentation page of the Quarkus site.

> [!IMPORTANT]
> The `main` branch of the `quarkus` repository is synchronized daily at 1 AM GMT, so you cannot preview your changes on the [Main branch (SNAPSHOT)](https://quarkus.io/version/main/guides/) until after the next site refresh occurs.
