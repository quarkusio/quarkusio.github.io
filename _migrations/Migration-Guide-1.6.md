---
date: 2020-07-08
---
## Maximum size allowed for request body limited to 10 MB

The maximum size allowed for the request body used to be unlimited by default, it's now limited to a safer default value of 10 MB.

You can go back to the previous unlimited behavior by explicitly adding `quarkus.http.limits.max-body-size=` to your `application.properties`.

## Hibernate ORM

The charset for DDL generation and SQL import files is now UTF-8 by default instead of being the system charset.

You can override it by adding `quarkus.hibernate-orm.database.charset=<your charset>` to your `application.properties`.

## OptaPlanner

The OptaPlanner Quarkus extensions have been moved to the OptaPlanner project and are now part of the Quarkus Platform.

The new GA are:

 * `org.optaplanner:optaplanner-quarkus`
 * `org.optaplanner:optaplanner-quarkus-jackson`
 * `org.optaplanner:optaplanner-quarkus-jsonb`

The versions are included in the Platform BOM.

## Native image and trustStore

The trustStore handling for native images was broken in previous versions since the migration to GraalVM 19.3.

It has been fixed. Be careful that the trustStore will be included in the native image at compilation time.