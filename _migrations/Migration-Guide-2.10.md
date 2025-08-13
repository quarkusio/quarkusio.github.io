---
date: 2022-06-21
---
## Keycloak Authorization and Keycloak 18.0.0

Keycloak version has been bumped to [18.0.0](https://www.keycloak.org/2022/04/keycloak-1800-released.html). It may affect `quarkus-keycloak-authorization` users who have authorization policies represented as `Java Script` and uploaded to Keycloak because the `upload-scripts` feature has been removed in Keycloak 18.0.0.
Please see [Keycloak Upgrading Guide](https://www.keycloak.org/docs/latest/upgrading/index.html#removal-of-the-code-upload-scripts-code-feature) for more information.

Here is a short summary of how to manage such Java Script policies in Keycloak 18.0.0.

Lets say you have the following authorization policy uploaded to Keycloak:

```java
 import org.keycloak.representations.idm.authorization.PolicyRepresentation;
 import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
 
 public void uploadPolicy() {
        ResourceServerRepresentation authorizationSettings = new ResourceServerRepresentation();
        PolicyRepresentation policyAdmin = createJSPolicy("Admin Policy", "var identity = $evaluation.context.identity;\n" +
                "\n" +
                "if (identity.hasRealmRole(\"admin\")) {\n" +
                "$evaluation.grant();\n" +
                "}", authorizationSettings);
        // Upload this policy using Keycloak Admin Client...
 }

 private static PolicyRepresentation createJSPolicy(String name, String code, ResourceServerRepresentation authorizationSettings) {
     PolicyRepresentation policy = new PolicyRepresentation();
     policy.setName(name);
     policy.setType("script-" + code);
     authorizationSettings.getPolicies().add(policy);
     return policy;
 }
```

For it to work with Keycloak `18.0.0` the JavaScript code needs to be moved to a policy file such as `admin.js`:

```
var identity = $evaluation.context.identity;
if (identity.hasRealmRole("admin")) {
$evaluation.grant();
}
```

and this `admin.js` needs to be referred to from the Java code:

```java
ResourceServerRepresentation authorizationSettings = new ResourceServerRepresentation();
PolicyRepresentation policyAdmin = createJSPolicy("Admin Policy", "admin-policy.js", authorizationSettings);
// Upload this policy using Keycloak Admin Client...
```

and from `keycloak-scripts.json`:

```json
{
  "policies": [
    {
      "fileName": "admin-policy.js"
    }
  ]
}
```

Finally `keycloak-scripts.json` and `admin-policy.js` need to be added to a jar and deployed to Keycloak.