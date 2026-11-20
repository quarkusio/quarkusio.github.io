# Quarkus tools in your favorite IDE

The following IDEs have support for the community developed Quarkus Tools:

- [Quarkus Tools for Visual Studio Code](https://marketplace.visualstudio.com/items?itemName=redhat.vscode-quarkus)
- [Quarkus Tools for Eclipse](https://marketplace.eclipse.org/content/quarkus-tools)
- [Quarkus Tools for IntelliJ IDEA](https://plugins.jetbrains.com/plugin/13234-quarkus/versions)
- [Quarkus Tools for Eclipse Che](https://github.com/eclipse-che/che-devfile-registry/tree/main/devfiles/quarkus)

In addition, IntelliJ IDEA has additional support for Quarkus in their Ultimate non-open source version.

- [IntelliJ IDEA Ultimate Edition built-in Quarkus support](https://www.jetbrains.com/help/idea/quarkus.html)

The table below gives an overview of the current IDEs with links and a high-level overview of their features.

|  | ![VSCode](/guides/images/visualstudiocode.svg) {empty} + VSCode Quarkus Tools | ![Eclipse](/guides/images/eclipseide.svg) {empty} + Eclipse Quarkus Tools | ![IntelliJ](/guides/images/intellijidea.svg) {empty} + IntelliJ Quarkus Tools | ![IntelliJ](/guides/images/intellijidea.svg) {empty} + IntelliJ IDEA Ultimate | ![Eclipse Che](/guides/images/eclipseche.svg) {empty} + Eclipse Che |
| --- | --- | --- | --- | --- | --- |
| Description | Visual Studio Code extension to install using the marketplace | Eclipse plugin to install into Eclipse using an update-site | IntelliJ IDEA plugin that works in IntelliJ IDEA Community and Ultimate. Available from Marketplace. | Built-in Quarkus features available only in IntelliJ IDEA Ultimate | Built-in Quarkus features available in Eclipse Che incl. che.openshift.io. |
| Status | Stable | Stable | Stable | Stable | Stable |
| Downloads | [Marketplace](https://marketplace.visualstudio.com/items?itemName=redhat.vscode-quarkus) {empty} + [Development Builds](https://download.jboss.org/jbosstools/vscode/snapshots/vscode-quarkus/?C=M;O=D) | [Development Update Site](https://download.jboss.org/jbosstools/photon/snapshots/builds/jbosstools-quarkus_master/) | [Marketplace](https://plugins.jetbrains.com/plugin/13234-quarkus/versions) {empty} + [Development Builds](https://download.jboss.org/jbosstools/intellij/snapshots/intellij-quarkus/) | [Installer](https://www.jetbrains.com/idea/nextversion/) | [Start Che Workspace](https://che.openshift.io/f?url=https://raw.githubusercontent.com/redhat-developer/devfile/master/getting-started/quarkus/devfile.yaml) |
| Source | [GitHub](https://github.com/redhat-developer/vscode-quarkus) | [GitHub](https://github.com/jbosstools/jbosstools-quarkus) | [GitHub](https://github.com/redhat-developer/intellij-quarkus) | Closed-Source |  |
| [Quarkus Language Server](https://github.com/redhat-developer/quarkus-ls) | [check] | [check] | [check] | [times] | [check] |
| Wizards w/code.quarkus.io | [check] | [check] | [check] | [check] | [check] |
| Custom Wizard | [times] | [times] | [check] | [check] | [times] |
| Config editor | [check] | [check] | [check] | [times] | [check] |
| Config autocompletion | [check] | [check] | [check] | [check] | [check] |
| Config validation | [check] | [check] | [check] | [check] | [check] |
| Config jump to definition | [check] | [check] | [check] | ? | [check] |
| Config profiles | [check] | [check] | [check] | [check] | [check] |
| Config outline | [check] | [check] | [check] | [times] | [check] |
| Easy Launch debug/dev:mode | [check] | [check] | [check] | [check] | [check] |
| Quarkus Code Snippets | [check] | [check] | [check] | [times] | [check] |
| Injection Discovery/Navigation | [times] | [times] | [times] | [check] | [times] |
