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
    This runs `mvn quarkus:dev` and serves the full site (including guides) at [http://localhost:8080](http://localhost:8080).

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
> Listening on: http://0.0.0.0:8080
> ```

 If an error occurs mentioning a name conflict, try:
```sh
docker compose up --force-recreate
```

5. Now browse to [http://localhost:4000](http://localhost:4000).
#### Using a local Ruby environment
[Jekyll static site generator docs](https://jekyllrb.com/docs/).

1. Install a full [Ruby development environment](https://jekyllrb.com/docs/installation/). If you use `rvm`, run: `rvm use 3.2.3`.
2. Install [bundler](https://jekyllrb.com/docs/ruby-101/#bundler)  [gems](https://jekyllrb.com/docs/ruby-101/#gems)
   ```sh
   gem install bundler
   ```
3. Fork the [project repository](https://github.com/quarkusio/quarkusio.github.io), then clone your fork.
   ```sh
   git clone git@github.com:YOUR_USER_NAME/quarkusio.github.io.git
   ```
4. Change into the project directory:
   ```sh
   cd quarkusio.github.io
   ```
5. Use bundler to fetch all required gems in their respective versions
   ```sh
   bundle install
   ```
6. Build the site and make it available on a local server
   ```sh
   ./serve.sh
   ```
   Or if you want it faster and okay to not have guides included use the following:

   ```sh
   ./serve-noguides.sh
   ```


7. Now browse to http://localhost:4000

>[!NOTE]
>If you encounter any unexpected errors during the above, please refer to the [troubleshooting](https://jekyllrb.com/docs/troubleshooting/#configuration-problems) page or the [requirements](https://jekyllrb.com/docs/installation/#requirements) page, as you might be missing development headers or other prerequisites.

**For more regarding the use of Jekyll, please refer to the [Jekyll Step by Step Tutorial](https://jekyllrb.com/docs/step-by-step/01-setup/).**

### Deploying to GitHub Pages

The website deployment is automatically performed by GitHub Actions (when commits are pushed to the `main` branch).
If for some reason you need to deploy from your local machine, follow these instructions:

1. Install the [act](https://github.com/nektos/act#installation) executable to run GitHub Actions locally
2. Run `act -s GITHUB_TOKEN=<GITHUB_TOKEN>`, where *<GITHUB_TOKEN>* needs to be replaced with a token that allows you to push to the https://github.com/quarkusio/quarkusio.github.io repository.

## Writing a blog

> [!WARNING]
> Using generative AI in *assisting* writing is fine, but please don't use it to write entire posts.
> Used badly, generative AI has a tendency to use complex words and phrasing. This makes the content hard to read and understand. Always review your blog with a human reader in mind, make sure it's factually correct and especially keep the human touch and opinions in the content.

To write a blog:

- create an author entry in [_data/authors.yaml](https://github.com/quarkusio/quarkusio.github.io/blob/main/_data/authors.yaml)
  - `emailhash` you can get by running `echo -n your@email.org | md5sum` on Linux or `echo -n your@email.org | md5` on macOS using an email you have registered from the [Gravatar service](https://gravatar.com),

- create a blog entry under [content/posts](https://github.com/quarkusio/quarkusio.github.io/tree/main/content/posts)
  - the file name is `yyyy-mm-dd-slug.adoc`. Set the `date` to the same value in the front matter.
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
  - Be aware that the `date` attribute in the front matter defines when the article will be published. Posts with a future date will not be visible in production until that date arrives; use `./serve.sh` locally to preview them regardless of date.
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

## Contributing

Please read [CONTRIBUTING.md](https://github.com/quarkusio/quarkusio.github.io/tree/main/CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

> [!IMPORTANT]
> The guides are maintained in the main Quarkus repository and pull requests should be submitted there: https://github.com/quarkusio/quarkus/tree/main/docs/src/main/asciidoc.

## License

This website is licensed under the [Creative Commons Attribution 3.0](https://creativecommons.org/licenses/by/3.0/).
