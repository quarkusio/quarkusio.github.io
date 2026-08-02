---


## Keystore default password

Quarkus used "password" as the default password for JWT key and keystores. 
This default value has been removed. So, if you used "password," you now need to configure that password in the `application.properties` file:

```
quarkus.oidc-client.credentials.jwt.key-store-password=password
quarkus.oidc-client.credentials.jwt.key-password=password
```
