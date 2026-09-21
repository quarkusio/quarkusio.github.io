---
layout: post
title: 'The quarkus.io site is now built with Roq'
tags: 
  - announcement
  - website
synopsis: 'We converted 40,000 files from Jekyll to Roq.'
author: hcummins
---

Have you looked at [quarkus.io](http://quarkus.io) recently? Of course you have, you’re reading this blog. Notice anything different? Well, different apart from [the much-improved guide navigation](https://quarkus.io/blog/new-guide-navigation/)? You probably haven’t.

But if you look _very_ closely at the site footer, you’ll see a change. Instead of saying “built with Jekyll”, the site now says “built with Roq”. 
What’s [Roq](https://iamroq.dev/)? Like Jekyll, it’s a static site generator (SSG). 
Unlike Jekyll, which is Ruby-based, it runs on Java. And it doesn’t run on any old Java, it runs on our favourite Quarkus-Java. 
So it’s efficient, delightful for development, typesafe, and just generally awesome. 
The idea behind Roq (and, I suspect, the reason for its name) is that Quarkus is really good at making dynamic sites, but not all sites need to be dynamic. 
What if you could just ‘crystallise’ your site and all its backing data, and serve that statically?

So why did [quarkus.io](http://quarkus.io) switch from Jekyll to Roq? Honestly, why _wouldn’t_ we switch? Jekyll was frustrating to build locally. Some of us could only make it work in a container. Some of us couldn’t make the container work and could only get the site building by doing the Ruby-version-install dance and hoping nothing upstream disturbed our environment. 
Jekyll was slow to build, and slow to serve pages. All the code backing our site, including any custom plugins, was written in Ruby, a language that most of us don’t know well. 
That meant there were lots of customisations we wanted to do that we just ... didn't.
Jekyll was a fantastic platform and served us well, but it was time to move on. Roq is Java, and it’s _our_ Java. If we find problems, we can fix them. And on the flipside, when we find problems and fix them, that makes Roq better for everyone else, too.

So with so many advantages, why didn’t we move earlier? Time and complexity. The quarkus.io site has about 4700 pages, in a mix of HTML, Markdown, and AsciiDoc. It’s translated into four languages. We’ve written a number of custom asciidoc extensions and Jekyll plugins, particularly to make our rich guides work. We have dynamic search embedded in with the static parts. It’s a lot.

But this year, we decided the time had come. Roq was mature and picking up new users all the time, and LLMs were getting good enough to help with the move.

## How did we do it?

The Roq documentation has [good guidance](https://iamroq.dev/docs/migrating/) on how to use an LLM for Jekyll-to-Roq, and some helper skills and prompts.
Many users who’ve migrated from Jekyll to Roq were able to convert simply by pointing an LLM at the site and letting it run.
For the Quarkus site, we decided that approach wouldn’t work. The quarkus.io site is too complex, with too many edge cases, for a one-shot conversion to have a chance of success. 
And our content changes too often for it to be reasonable to freeze everything during a migration. 
We ship many updates every single day. 
Building up a comprehensive set of skills and iteratively re-converting might have worked, but it would have wasted a lot of tokens to keep re-running the same LLM-driven transformation over and over again. Another option was a long-running fork where we mirrored incoming changes while fixing problems. That might have worked, but it would have been complicated to manage. Instead, we opted to create reproducible scripts.

This is the approach we followed:
- **Write tests.** Because none of us like writing Ruby, we didn’t have any tests for our old site. Every change relied on manual validation, but that’s not feasible when changing 4700 pages at once. Following the classic refactoring playbook, we wrapped the existing site in tests, before touching anything. (Bonus: we now have tests!)
- **Add Lighthouse tests to catch performance regressions.** Like integration tests, [Lighthouse](https://developer.chrome.com/docs/lighthouse/overview) was another piece of infrastructure we probably should have had, but didn’t. Wiring Lighthouse checks into our CI helped us spot some performance problems with the original site, even before converting anything. It then helped us catch some subtle functional and non-functional regressions in the converted site.
- **Create a PR with the converted site.** The PR's surge.sh preview allowed the broader team to track progress, from “oh dear, why are all the pages missing or malformed?” to “we’ve looked, and we've squinted, and we've scrolled, and we can’t see a difference.”
- **Generate conversion scripts.** This was an iterative process. We used LLMs to build up and refine the scripts until eventually all the tests passed. (Bonus: we now have Jekyll to Roq converter scripts!)
- **Write new translation tooling.** Most Jekyll features we relied on had equivalents in Roq, but one big gap was the localization tooling, [`jekyll-l10n`](https://rubygems.org/gems/jekyll-l10n/). We had to write a Roq equivalent, [`asciidoc-jruby-l10n`](https://github.com/quarkiverse/quarkus-roq/tree/main/roq-plugin/asciidoc-jruby-l10n).
- **Merge the monster PR.** Once everyone was happy, we pushed the "Merge" button on the PR and cut over.
- **Post-merge panic.** It turned out, some things were missed. There is nothing like two hundred pairs of eyeballs to help discover gaps in the tests, and corners of the site that should have been squinted-at and scrolled-to a bit more intensively. Tables of contents had gone missing, links in older guide versions (which we don’t preview in CI) were broken, some titles had been replaced by file names … Luckily, none of the regressions were catastrophic, and they could all be fixed within a few days.
- **Rescue open PRs.** What about people who had open PRs against the site, and were confronted with a 40,000-file interactive rebase? The same conversion scripts we used on the main site could be run against their PRs, so open PRs could be re-shaped to the new layout without anybody losing work.

Several weeks after flipping the big switch, we’ve now closed the [working group](https://github.com/orgs/quarkusio/projects/79).

## The end result

So, what was achieved? 
The converter was 10,000 lines of fresh code for the `quarkus-roq` repository. 
I’d like to apologise to my colleagues who had to review it, especially since most of the code was boring. 
Running it against the site created 40,000 changed files. 
I’d like to apologise to the GitHub tooling, which found this volume of changes … challenging.

But of course the goal of the exercise wasn’t to annoy reviewers and make my GitHub statistics look good. 
What was _really_ achieved? Our tests now execute 42% faster than they did against the old site. 
Our Lighthouse performance scores had a small but noticeable improvement. 
Subjectively, live reload feels way faster. 
Doing local authoring is now just a `./mvnw quarkus:dev` call instead of a multi-hour journey debugging Ruby configuration.
We've unlocked our wishlist of site features that we can code up in Java.

As a bonus of scripting the conversion process, we now have a converter everyone can use on their own sites. 
It’s still being refined, and isn’t fully documented in the Roq docs yet, but you can have a play with the [`roq-it-jekyll`](https://github.com/quarkiverse/quarkus-roq/blob/main/migration/roq-it-jekyll) script.

## Under the covers

If you’d like a deeper dive, I presented the migration journey on a Quarkus community call. The [minutes and recording](https://github.com/quarkusio/quarkus/discussions/55599) are on GitHub Discussions.

Roq and Jekyll both use data files and templates for site generation, and that makes the conversion easier than it would have been for, say, Gatsby to Roq. 
Here's what had to change:
- **Layout.** Files are arranged differently on disk, but most of the layout mappings are straightforward.
- **Configuration.** Site configuration is handled differently in Roq and Jekyll. There *is* a mapping, but it took some experimentation to get right.
- **Templating language.** Jekyll uses Liquid, and Roq uses Qute. There's a fairly straightforward syntactic mapping, but the semantics for variable scope are very different. Handling scoping, mutability, cross-file-merging, and other variable misalignments ended up being the most complex part of the conversion.
- **Plugins.** We had a set of Jekyll plugins that needed manual conversion.

Want to know how Qute compares to Liquid? This shows the before and after for part of our site. 

![a section of Liquid template, animated into a section of Qute template](/assets/images/posts/jekyll-to-roq/liquid-to-qute.gif)