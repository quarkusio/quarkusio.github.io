---
date: 2022-05-11
---
## H2 upgrade

H2 has been upgraded from 1.4.197 to 2.1.210.

The new version adds several reserved keywords such as `user`, `value` or `timestamp` so you might have to adjust your model if you use any of these in your table or column names.

## HTTP compression

HTTP compression settings have been made build time configuration so they cannot be overridden at runtime anymore.
This allows for more optimizations.

Furthermore, not all HTTP responses are compressed by default. If compression support is enabled then the response body is compressed if:
1. A  RESTEasy Reactive resource method is annotated with `@io.quarkus.vertx.http.Compressed`, or
2. A reactive route is annotated with `@io.quarkus.vertx.http.Compressed`, or
3. The `Content-Type` header is set and the value is a compressed media type as configured via `quarkus.http.compress-media-types`.

NOTE: If the client does not support HTTP compression then the response body is not compressed.

## Log rotation

`quarkus.log.file.rotation.max-file-size` is now set to 10 MB by default.