# Using security with .properties file

Quarkus provides support for properties file-based authentication intended for development and testing purposes. It is not recommended to use this authentication in production as, at present, only plain-text and MD5 hashed passwords are used, and properties files are generally too limited.

Add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-elytron-security-properties-file</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-elytron-security-properties-file")
```

## Configuration

The elytron-security-properties-file extension currently supports two different realms for storing authentication and authorization information. Both support storage of this information in properties files.

The following sections detail the specific configuration properties.

> [!NOTE]
> Basic authentication must be explicitly enabled with `quarkus.http.auth.basic=true`, if more than one authentication mechanism is used.

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-elytron-security-properties-file_quarkus-security-users-embedded-plain-text"></a> [`quarkus.security.users.embedded.plain-text`](#quarkus-elytron-security-properties-file_quarkus-security-users-embedded-plain-text) config_property_copy_button:+++quarkus.security.users.embedded.plain-text+++[]<br><br>If the passwords are stored in the property file as plain text, e.g. `quarkus.security.users.embedded.users.alice=AlicesSecretPassword`. If this is false (the default) then it is expected that passwords are hashed as per the `algorithm` config property.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_USERS_EMBEDDED_PLAIN_TEXT+++[] | boolean | `false` |
| <a id="quarkus-elytron-security-properties-file_quarkus-security-users-embedded-algorithm"></a> [`quarkus.security.users.embedded.algorithm`](#quarkus-elytron-security-properties-file_quarkus-security-users-embedded-algorithm) config_property_copy_button:+++quarkus.security.users.embedded.algorithm+++[]<br><br>The algorithm with which user password is hashed. The library expects a password prepended with the username and the realm, in the form ALG( username ":" realm ":" password ) in hexadecimal format.<br><br>For example, on a Unix-like system we can produce the expected hash for Alice logging in to the Quarkus realm with password AlicesSecretPassword using `echo -n "alice:Quarkus:AlicesSecretPassword" \| sha512sum`, and thus set `quarkus.security.users.embedded.users.alice=c8131...4546` (full hash output abbreviated here). This property is ignored if `plainText` is true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_USERS_EMBEDDED_ALGORITHM+++[] | `digest-md5`, `digest-sha`, `digest-sha-256`, `digest-sha-384`, `digest-sha-512`, `digest-sha-512-256` | `digest-sha-512` |
| <a id="quarkus-elytron-security-properties-file_quarkus-security-users-embedded-users-users"></a> [`quarkus.security.users.embedded.users."users"`](#quarkus-elytron-security-properties-file_quarkus-security-users-embedded-users-users) config_property_copy_button:+++quarkus.security.users.embedded.users."users"+++[]<br><br>The realm users user1=password++ ++nuser2=password2... mapping. See [Embedded Users](#embedded-users).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_USERS_EMBEDDED_USERS__USERS_+++[] | Map\<String,String> | `none` |
| <a id="quarkus-elytron-security-properties-file_quarkus-security-users-embedded-roles-roles"></a> [`quarkus.security.users.embedded.roles."roles"`](#quarkus-elytron-security-properties-file_quarkus-security-users-embedded-roles-roles) config_property_copy_button:+++quarkus.security.users.embedded.roles."roles"+++[]<br><br>The realm roles user1=role1,role2,...++ ++nuser2=role1,role2,... mapping See [Embedded Roles](#embedded-roles).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_USERS_EMBEDDED_ROLES__ROLES_+++[] | Map\<String,String> | `none` |
| <a id="quarkus-elytron-security-properties-file_section_quarkus-security-users-file"></a> [Property Files Realm Configuration](#quarkus-elytron-security-properties-file_section_quarkus-security-users-file) | Type | Default |
| [lock] <a id="quarkus-elytron-security-properties-file_quarkus-security-users-file-realm-name"></a> [`quarkus.security.users.file.realm-name`](#quarkus-elytron-security-properties-file_quarkus-security-users-file-realm-name) config_property_copy_button:+++quarkus.security.users.file.realm-name+++[]<br><br>The realm name. This is used when generating a hashed password<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_USERS_FILE_REALM_NAME+++[] | string | `Quarkus` |
| [lock] <a id="quarkus-elytron-security-properties-file_quarkus-security-users-file-enabled"></a> [`quarkus.security.users.file.enabled`](#quarkus-elytron-security-properties-file_quarkus-security-users-file-enabled) config_property_copy_button:+++quarkus.security.users.file.enabled+++[]<br><br>Determine whether security via the file realm is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_USERS_FILE_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-elytron-security-properties-file_quarkus-security-users-file-plain-text"></a> [`quarkus.security.users.file.plain-text`](#quarkus-elytron-security-properties-file_quarkus-security-users-file-plain-text) config_property_copy_button:+++quarkus.security.users.file.plain-text+++[]<br><br>If the properties are stored in plain text. If this is false (the default) then it is expected that the passwords are of the form HEX( MD5( username ":" realm ":" password ) )<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_USERS_FILE_PLAIN_TEXT+++[] | boolean | `false` |
| [lock] <a id="quarkus-elytron-security-properties-file_quarkus-security-users-file-users"></a> [`quarkus.security.users.file.users`](#quarkus-elytron-security-properties-file_quarkus-security-users-file-users) config_property_copy_button:+++quarkus.security.users.file.users+++[]<br><br>Classpath resource name of properties file containing user to password mappings. See [Users.properties](#users-properties).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_USERS_FILE_USERS+++[] | string | `users.properties` |
| [lock] <a id="quarkus-elytron-security-properties-file_quarkus-security-users-file-roles"></a> [`quarkus.security.users.file.roles`](#quarkus-elytron-security-properties-file_quarkus-security-users-file-roles) config_property_copy_button:+++quarkus.security.users.file.roles+++[]<br><br>Classpath resource name of properties file containing user to role mappings. See [Roles.properties](#roles-properties).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_USERS_FILE_ROLES+++[] | string | `roles.properties` |
| <a id="quarkus-elytron-security-properties-file_section_quarkus-security-users-embedded"></a> [Embedded Realm Configuration](#quarkus-elytron-security-properties-file_section_quarkus-security-users-embedded) | Type | Default |
| [lock] <a id="quarkus-elytron-security-properties-file_quarkus-security-users-embedded-realm-name"></a> [`quarkus.security.users.embedded.realm-name`](#quarkus-elytron-security-properties-file_quarkus-security-users-embedded-realm-name) config_property_copy_button:+++quarkus.security.users.embedded.realm-name+++[]<br><br>The realm name. This is used when generating a hashed password<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_USERS_EMBEDDED_REALM_NAME+++[] | string | `Quarkus` |
| [lock] <a id="quarkus-elytron-security-properties-file_quarkus-security-users-embedded-enabled"></a> [`quarkus.security.users.embedded.enabled`](#quarkus-elytron-security-properties-file_quarkus-security-users-embedded-enabled) config_property_copy_button:+++quarkus.security.users.embedded.enabled+++[]<br><br>Determine whether security via the embedded realm is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_USERS_EMBEDDED_ENABLED+++[] | boolean | `false` |

### Properties Files Realm Configuration

The properties files realm supports the mapping of users to passwords and users to roles with a combination of properties files. They are configured with properties starting with `quarkus.security.users.file`.

**Example of the `application.properties` file section for property files realm**

```properties
quarkus.security.users.file.enabled=true
quarkus.security.users.file.users=test-users.properties
quarkus.security.users.file.roles=test-roles.properties
quarkus.security.users.file.realm-name=MyRealm
quarkus.security.users.file.plain-text=true
```

#### Users.properties

The `quarkus.security.users.file.users` configuration property specifies a classpath resource which is a properties file with a user-to-password mapping, one per line.

The following [Example of \`test-users.properties\`](#test-users-example) illustrates the format:

<a id="test-users-example"></a>

**Example of `test-users.properties`**

```properties
scott=jb0ss <1>
jdoe=p4ssw0rd <2>
stuart=test
noadmin=n0Adm1n
```

1. User `scott` has password defined as `jb0ss`
2. User `jdoe` has password defined as `p4ssw0rd`

This file has usernames and passwords stored in plain text, which is not recommended. If plain text is set to false (or omitted) in the config, then passwords must be stored in the form `MD5 ( username : realm : password )`. This can be generated for the first example above by running the command `echo -n scott:MyRealm:jb0ss | md5` from the command line.

#### Roles.properties

**Example of `test-roles.properties`**

```properties
scott=Admin,admin,Tester,user <1>
jdoe=NoRolesUser <2>
stuart=admin,user <3>
noadmin=user
```

1. User `scott` has been assigned the roles `Admin`, `admin`, `Tester` and `user`
2. User `jdoe` has been assigned the role `NoRolesUser`
3. User `stuart` has been assigned the roles `admin` and `user`.

### Embedded Realm Configuration

The embedded realm also supports the mapping of users to passwords and users to roles. It uses the main `application.properties` Quarkus configuration file to embed this information. They are configured with properties starting with `quarkus.security.users.embedded`.

The following is an example application.properties file section illustrating the embedded realm configuration:

**Example of the `application.properties` file section for embedded realm**

```properties
quarkus.security.users.embedded.enabled=true
quarkus.security.users.embedded.plain-text=true
quarkus.security.users.embedded.users.scott=jb0ss
quarkus.security.users.embedded.users.stuart=test
quarkus.security.users.embedded.users.jdoe=p4ssw0rd
quarkus.security.users.embedded.users.noadmin=n0Adm1n
quarkus.security.users.embedded.roles.scott=Admin,admin,Tester,user
quarkus.security.users.embedded.roles.stuart=admin,user
quarkus.security.users.embedded.roles.jdoe=NoRolesUser
quarkus.security.users.embedded.roles.noadmin=user
```

As with the first example, this file has usernames and passwords stored in plain text, which is not recommended. If plain text is set to false (or omitted) in the config, then passwords must be stored in the form `MD5 ( username : realm : password )`. This can be generated for the first example above by running the command `echo -n scott:MyRealm:jb0ss | md5` from the command line.

#### Embedded Users

The user to password mappings are specified in the `application.properties` file by properties keys of the form `quarkus.security.users.embedded.users.<user>=<password>`. The following [Example of passwords](#password-example) illustrates the syntax with 4 user-to-password mappings:

<a id="password-example"></a>

**Example of passwords**

```properties
quarkus.security.users.embedded.users.scott=jb0ss # <1>
quarkus.security.users.embedded.users.stuart=test # <2>
quarkus.security.users.embedded.users.jdoe=p4ssw0rd
quarkus.security.users.embedded.users.noadmin=n0Adm1n
```

1. User `scott` has password `jb0ss`
2. User `stuart` has password `test`

#### Embedded Roles

The user to role mappings are specified in the `application.properties` file by properties keys of the form `quarkus.security.users.embedded.roles.<user>=role1[,role2[,role3[,...]]]`. The following [Example of roles](#roles-example) illustrates the syntax with 4 user-to-role mappings:

<a id="roles-example"></a>

**Example of roles**

```properties
quarkus.security.users.embedded.roles.scott=Admin,admin,Tester,user # <1>
quarkus.security.users.embedded.roles.stuart=admin,user # <2>
quarkus.security.users.embedded.roles.jdoe=NoRolesUser
quarkus.security.users.embedded.roles.noadmin=user
```

1. User `scott` has roles `Admin`, `admin`, `Tester`, and `user`
2. User `stuart` has roles `admin` and `user`

## References

- [Quarkus Security overview](/guides/security-overview/index.md)
