# Quarkus support in IDEs

Quarkus is still young and thus it might come as a surprise to some there are actually already a good amount of Quarkus support in most of the various IDE's.

Its important to realize that Quarkus is using mainly standard technology and API thus no special IDE tooling is required to use Quarkus, but if you do like dedicated Quarkus integration in your IDE then see the list below.

The current list is:

- Quarkus Tools for Visual Studio Code
- JBoss Tools Quarkus Tools for Eclipse
- Quarkus Tools for IntelliJ
- IntelliJ Ultimate Edition built-in Quarkus support
- Eclipse Che is on its way

The table below gives an overview of the current IDE's with links and a high-level overview of their features.

If you want support for Quarkus in your favorite IDE/Editor then scroll down...

|  | ![200](https://simpleicons.org/icons/visualstudiocode.svg) {empty} + VSCode Quarkus Tools | ![200](https://simpleicons.org/icons/eclipseide.svg) {empty} + Eclipse Quarkus Tools | ![100](https://simpleicons.org/icons/intellijidea.svg) {empty} + IntelliJ Quarkus Tools | ![100](https://simpleicons.org/icons/intellijidea.svg) {empty} + Intellij Ultimate | ![100](https://simpleicons.org/icons/eclipseche.svg) {empty} + Eclipse Che |
| --- | --- | --- | --- | --- | --- |
| Description | Visual Studio Code extension to install using the marketplace | Eclipse plugin to install into Eclipse using an updatesite | Intellij plugin that works in Intellij Community and Ultimate. Available from Marketplace. | Built-in Quarkus features available only in IntelliJ Ultimate | Built-in Quarkus features available in Eclipse Che incl. che.openshift.io. |
| Status | Stable | Alpha | Beta | Stable (since 2019.3) | Coming Soon! |
| Downloads | [Marketplace](https://marketplace.visualstudio.com/items?itemName=redhat.vscode-quarkus) {empty} + [Development Builds](https://download.jboss.org/jbosstools/vscode/snapshots/vscode-quarkus/?C=M;O=D) | [Development Update Site](https://download.jboss.org/jbosstools/photon/snapshots/builds/jbosstools-quarkus_master/) | [Marketplace](https://plugins.jetbrains.com/plugin/13234-quarkus/versions) {empty} + [Development Builds](https://download.jboss.org/jbosstools/intellij/snapshots/intellij-quarkus/) | [Installer](https://www.jetbrains.com/idea/nextversion/) |  |
| Source | [GitHub](https://github.com/redhat-developer/vscode-quarkus) | [GitHub](https://github.com/jbosstools/jbosstools-quarkus) | [GitHub](https://github.com/redhat-developer/intellij-quarkus) | Closed-Source |  |
| [Quarkus Language Server](https://github.com/redhat-developer/quarkus-ls) | [check] | [check] | [check] | [times] | [check] |
| Wizards w/code.quarkus.io | [check] | [check] | [[times]](https://issues.jboss.org/browse/JBIDE-26950) | [check] | [check] |
| Custom Wizard | [times] | [times] | [check] | [check] | [times] |
| Config editor | [check] | [check] | [check] | [times] | [check] |
| Config autocompletion | [check] | [check] | [check] | [check] | [check] |
| Config validation | [check] | [check] | [check] | [check] | [check] |
| Config profiles | [check] | [check] | [check] | [check] | [check] |
| Config outline | [check] | [check] | [check] | [times] | [check] |
| Easy Launch debug/dev:mode | [check] | [times] | [check] | [check] | [check] |
| Quarkus Code Snippets | [check] | [times] | [times] | [times] | [check] |
| Injection Discovery/Navigation | [times] | [times] | [times] | [check] | [times] |

## What about Quarkus Tools in \<my favorite IDE/editor> ?

What is amazing is that the first three are almost all at feature parity because they are using the awesome [Quarkus Language Server](https://github.com/redhat-developer/quarkus-ls) made by the Red Hat Developer team. If you are interested in enabling Quarkus tooling support for your favorite IDE/editor you should give that language server a go - and let me know once you have it working and get added to this list!

I can't wait to add emacs and vi to this list :)

Have fun! {empty}\
 + /max
