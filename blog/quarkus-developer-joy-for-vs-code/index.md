# Quarkus developer joy for VS Code

Earlier this year, we have been introduced to Quarkus, the next-generation, container-first framework for Java applications. As expected, such new frameworks and technologies make way for new developer tools focused on making the development experience even better. The recent [Quarkus Tools for Visual Studio Code release](https://marketplace.visualstudio.com/items?itemName=redhat.vscode-quarkus) aims to do just that, by bringing features specific to Quarkus project development within VS Code.

The VS Code extension was [originally](https://github.com/tsurdilo/quarkus-vsc) started as a project by [Tihomir Surdilovic](https://github.com/tsurdilo), from the Business Automation Team at Red Hat and became the foundation of today's version.

This blog post outlines what Quarkus Tools for Visual Studio Code has to offer so far: convenient features for an already convenient Java framework.

> [!NOTE]
> Quarkus Tools for Visual Studio Code is dependent on a couple of Java extensions for VS Code, so it is recommended that you have the [Java Extension Pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) installed.

> [!NOTE]
> For more detailed information about Quarkus Tools for Visual Studio Code, check out the [GitHub repository](https://github.com/redhat-developer/vscode-quarkus).

Watch a demo of Quarkus Tools for Visual Studio Code:

[XMrLP_7IbW0](XMrLP_7IbW0)

## Features

- [Generating a Maven-based Quarkus project](#generating-a-maven-based-quarkus-project)
- [Snippet support](#snippet-support)
- [Adding Quarkus extensions to a Quarkus project](#adding-quarkus-extensions-to-a-quarkus-project)
- [Debugging a Quarkus project](#debugging-a-quarkus-project)
- [Tree view for application.properties](#tree-view-for-application-properties)
- [Hover and documentation for application.properties](#hover-and-documentation-for-application-properties)
- [Completion for application.properties](#completion-for-application-properties)
- [Validation for application.properties](#validation-for-application-properties)

### Generating a Maven-based Quarkus project

Previously, generating a Maven-based Quarkus project required you to run a Maven command or create a project with [code.quarkus.io](https://code.quarkus.io/). Now you can create a new project right from a user interface in VS Code. This can be done through a new wizard that generates Maven-based Quarkus projects with the latest version of Quarkus.

To launch the wizard call the `Quarkus: Generate a Maven project` command from the command palette.

![Going through the project generation wizard](01_project_wizard.gif)

The wizard will ask you for the project’s groupId, artifactId, project version, package name, resource name, and target directory. It also lets you select Quarkus extensions to add to your new project. If you selected Quarkus extensions through the wizard before, the "Last Used" option lets you easily select all previously chosen extensions.

![The 'last used' option](02_last_used_option.webp)

After the project is generated, VS Code will automatically open a new window with the folder containing the new Quarkus project. Alongside the project files, Maven wrapper files are also included.

### Snippet support

The VS Code extension provides snippets to create new Quarkus resources and tests, which can help speed up your workflow. You can use these snippets by calling the `Insert Snippet` VS Code command or by simply typing the snippet prefix in a Java file. Pressing tab will place the cursor to the next location of interest.

![Using snippets](03_snippet.gif)

### Adding Quarkus extensions to a Quarkus project

The VS Code extension provides a separate wizard for adding Quarkus extensions to the current project.

To launch the wizard call the `Quarkus: Add extensions to current project` command from the command palette.

![Add extensions wizard](04_add_extension_wizard.gif)

After selecting the desired Quarkus extension(s), the Maven command to add the selected extensions will run in VS Code’s integrated terminal.

### Debugging a Quarkus project

Starting a debug session with the VS Code extension’s debug command automatically creates and adds a new VS Code task and debug configuration in the project folder. This step allows VS Code to automatically start the Quarkus application by running the `quarkus:dev` command when you start a debug session. The debugger will attach to the application right after it starts. When closing the debug session, you are provided with options to terminate the Quarkus application or keep it running.

The debug command is: `Quarkus: Debug current Quarkus project`.

![Message when terminating debug](05_debug_term.webp)

### Tree view for application.properties

Upon opening the application.properties file, the VS Code explorer outline will provide you with a tree view containing all property keys and values. This feature is especially useful for large application.properties files because it groups properties by their namespace and profile.

![Tree view](06_tree_view.webp)

### Hover and documentation for application.properties

The ability to hover over property keys makes it easy to view the property’s documentation. The documentation provides the property key’s value type, default value (if it exists), phase (whether the property applies at Quarkus runtime or build time), and the extension name.

![Hover over property key](07_hovering.webp)

### Completion for application.properties

As of now, there are more than 400 possible predefined Quarkus properties that could appear in an application.properties file. I challenge you to remember them all :). Because of the vast number of properties that could be configured, property key and value completion prove useful to anyone working with the application.properties file.

After opening the application.properties file, you can receive property key completion options as you type, or with the Ctrl+Space (⌥+Space for macOS) keyboard shortcut. The list of completion options is in sync with the Quarkus extensions that are currently available to the project. That means, whenever Quarkus extensions are added or removed, the list of completion options will be updated to provide you with the relevant completion options.

![Property key completion](08_key_completion.gif)

If the property key has a default value, it will be inserted automatically as a part of the completion.

![Default value completion](09_default_value_completion.gif)

Completion is also available for enumerated type and boolean values.

![Value completion](10_enum_bool_completion.gif)

There is also completion support for mapped properties. Pressing tab after editing the property will place the cursor to the next location.

![Mapped properties](11_map_property.gif)

### Validation for application.properties

Last but not least, we have validation support for application.properties. We currently provide validation support for unknown properties, duplicate properties, and for missing equals signs.

![Validation](12_validation.webp)

## Next steps

As you can see, Quarkus Tools for Visual Studio Code has great features to help provide a seamless developer experience and promote Quarkus project development within VS Code. However, this does not mean that the extension is perfect and bug-free; it is still being improved. We aspire to add new features (such as a CodeLens feature that would open REST endpoint URLs in a web browser) and improve existing features (such as completion, validation, snippets).

If you prefer IDEs such as Eclipse and IntelliJ IDEA, the good news is that Quarkus tooling for those IDEs are currently in progress. Because the application.properties features are provided via the [Language Server Protocol (LSP)](https://microsoft.github.io/language-server-protocol/), it is possible to provide the same features to other editors and IDEs.

If you have any issues or feature requests, please let us know by creating a GitHub issue [here](https://github.com/redhat-developer/vscode-quarkus/issues). We would be very happy to listen to any suggestions for improvement. If you would like to contribute to the project, the contributing guide is located in the GitHub repository [here](https://github.com/redhat-developer/vscode-quarkus/blob/master/CONTRIBUTING.md).

Thank you for reading!
