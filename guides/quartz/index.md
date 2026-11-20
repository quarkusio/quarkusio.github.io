# Scheduling periodic tasks with Quartz

Modern applications often need to run specific tasks periodically. In this guide, you learn how to schedule periodic clustered tasks using the [Quartz](http://www.quartz-scheduler.org/) extension.

> [!TIP]
> If you only need to run in-memory scheduler use the [Scheduler](/guides/scheduler/index.md) extension.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Docker and Docker Compose or [Podman](/guides/podman/index.md), and Docker Compose
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## Architecture

In this guide, we are going to expose one Rest API `tasks` to visualise the list of tasks created by a Quartz job running every 10 seconds.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `quartz-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/quartz-quickstart).

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:quartz-quickstart \
    --extension='rest-jackson,quartz,hibernate-orm-panache,flyway,jdbc-postgresql' \
    --no-code
cd quartz-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=quartz-quickstart \
    -Dextensions='rest-jackson,quartz,hibernate-orm-panache,flyway,jdbc-postgresql' \
    -DnoCode
cd quartz-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=quartz-quickstart"`

It generates:

- the Maven structure
- a landing page accessible on `http://localhost:8080`
- example `Dockerfile` files for both `native` and `jvm` modes
- the application configuration file

The Maven project also imports the Quarkus Quartz extension.

If you already have your Quarkus project configured, you can add the `quartz` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add quartz
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='quartz'
```

**Gradle**

```bash
./gradlew addExtension --extensions='quartz'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-quartz</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-quartz")
```

> [!TIP]
> To use a JDBC store, the `quarkus-agroal` extension, which provides the datasource support, is also required.

## Creating the Task Entity

In the `org.acme.quartz` package, create the `Task` class, with the following content:

```java
package org.acme.quartz;

import jakarta.persistence.Entity;
import java.time.Instant;
import jakarta.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name="TASKS")
public class Task extends PanacheEntity { <1>
    public Instant createdAt;

    public Task() {
        createdAt = Instant.now();
    }

    public Task(Instant time) {
        this.createdAt = time;
    }
}
```

1. Declare the entity using [Panache](/guides/hibernate-orm-panache/index.md)

## Creating a scheduled job

In the `org.acme.quartz` package, create the `TaskBean` class, with the following content:

```java
package org.acme.quartz;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.transaction.Transactional;

import io.quarkus.scheduler.Scheduled;

@ApplicationScoped <1>
public class TaskBean {

    @Transactional
    @Scheduled(every = "10s", identity = "task-job") <2>
    void schedule() {
        Task task = new Task(); <3>
        task.persist(); <4>
    }
}
```

1. Declare the bean in the *application* scope
2. Use the `@Scheduled` annotation to instruct Quarkus to run this method every 10 seconds and set the unique identifier for this job.
3. Create a new `Task` with the current start time.
4. Persist the task in database using [Panache](/guides/hibernate-orm-panache/index.md).

> [!TIP]
> The `@Scheduled#description()` value, if set, is stored in the `DESCRIPTION` column of the `QRTZ_JOB_DETAILS` table.

### Scheduling Jobs Programmatically

An injected `io.quarkus.scheduler.Scheduler` can be used to [schedule a job programmatically](/guides/scheduler-reference/index.md#programmatic_scheduling). However, it is also possible to leverage the Quartz API directly. You can inject the underlying `org.quartz.Scheduler` in any bean:

```java
package org.acme.quartz;

@ApplicationScoped
public class TaskBean {

    @Inject
    org.quartz.Scheduler quartz; <1>

    void onStart(@Observes StartupEvent event) throws SchedulerException {
       JobDetail job = JobBuilder.newJob(MyJob.class)
                         .withIdentity("myJob", "myGroup")
                         .build();
       Trigger trigger = TriggerBuilder.newTrigger()
                            .withIdentity("myTrigger", "myGroup")
                            .startNow()
                            .withSchedule(
                               SimpleScheduleBuilder.simpleSchedule()
                                  .withIntervalInSeconds(10)
                                  .repeatForever())
                            .build();
       quartz.scheduleJob(job, trigger); <2>
    }

    @Transactional
    void performTask() {
        Task task = new Task();
        task.persist();
    }

    // A new instance of MyJob is created by Quartz for every job execution
    public static class MyJob implements Job {

       @Inject
       TaskBean taskBean;

       public void execute(JobExecutionContext context) throws JobExecutionException {
          taskBean.performTask(); <3>
       }

    }
}
```

1. Inject the underlying `org.quartz.Scheduler` instance.
2. Schedule a new job using the Quartz API.
3. Invoke the `TaskBean#performTask()` method from the job. Jobs are also [container-managed](/guides/cdi/index.md) beans if they belong to a [bean archive](/guides/cdi-reference/index.md).

> [!NOTE]
> By default, the scheduler is not started unless a `@Scheduled` business method is found. You may need to force the start of the scheduler for "pure" programmatic scheduling. See also [Quartz Configuration Reference](#quartz-configuration-reference).

## Updating the application configuration file

Edit the `application.properties` file and add the below configuration:

```properties
# Quartz configuration
quarkus.quartz.clustered=true <1>
quarkus.quartz.store-type=jdbc-cmt <2>
quarkus.quartz.misfire-policy.task-job=ignore-misfire-policy <3>

# Datasource configuration.
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=quarkus_test
quarkus.datasource.password=quarkus_test
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost/quarkus_test

# Hibernate configuration
quarkus.hibernate-orm.schema-management.strategy=none
quarkus.hibernate-orm.log.sql=true
quarkus.hibernate-orm.sql-load-script=no-file

# flyway configuration
quarkus.flyway.connect-retries=10
quarkus.flyway.table=flyway_quarkus_history
quarkus.flyway.migrate-at-start=true
quarkus.flyway.baseline-on-migrate=true
quarkus.flyway.baseline-version=1.0
quarkus.flyway.baseline-description=Quartz
```

1. Indicate that the scheduler will be run in clustered mode
2. Use the database store to persist job related information so that they can be shared between nodes
3. The misfire policy can be configured for each job. `task-job` is the identity of the job.

Valid misfire policy for cron jobs are: `smart-policy`, `ignore-misfire-policy`, `fire-now` and `cron-trigger-do-nothing`. Valid misfire policy for interval jobs are: `smart-policy`, `ignore-misfire-policy`, `fire-now`, `simple-trigger-reschedule-now-with-existing-repeat-count`, `simple-trigger-reschedule-now-with-remaining-repeat-count`, `simple-trigger-reschedule-next-with-existing-count` and `simple-trigger-reschedule-next-with-remaining-count`.

## Creating a REST resource and a test

Create the `org.acme.quartz.TaskResource` class with the following content:

```java
package org.acme.quartz;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/tasks")
public class TaskResource {

    @GET
    public List<Task> listAll() {
        return Task.listAll(); <1>
    }
}
```

1. Retrieve the list of created tasks from the database

You also have the option to create a `org.acme.quartz.TaskResourceTest` test with the following content:

```java
package org.acme.quartz;

import io.quarkus.test.junit.QuarkusTest;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class TaskResourceTest {

    @Test
    public void tasks() throws InterruptedException {
        Thread.sleep(1000); // wait at least a second to have the first task created
        given()
                .when().get("/tasks")
                .then()
                .statusCode(200)
                .body("size()", is(greaterThanOrEqualTo(1))); <1>
    }
}
```

1. Ensure that we have a `200` response and at least one task created

## Creating Quartz Tables

Add a SQL migration file named `src/main/resources/db/migration/V2.0.0\__QuarkusQuartzTasks.sql` with the content copied from file with the content from [V2.0.0__QuarkusQuartzTasks.sql](https://github.com/quarkusio/quarkus-quickstarts/blob/main/quartz-quickstart/src/main/resources/db/migration/V2.0.0__QuarkusQuartzTasks.sql).

## Configuring the load balancer

In the root directory, create a `nginx.conf` file with the following content:

```conf
user  nginx;

events {
    worker_connections   1000;
}

http {
        server {
              listen 8080;
              location / {
                proxy_pass http://tasks:8080; <1>
              }
        }
}
```

1. Route all traffic to our tasks application

## Setting Application Deployment

In the root directory, create a `docker-compose.yml` file with the following content:

```yaml
version: '3'

services:
  tasks: <1>
    image: quarkus-quickstarts/quartz:1.0
    build:
      context: ./
      dockerfile: src/main/docker/Dockerfile.${QUARKUS_MODE:-jvm}
    environment:
      QUARKUS_DATASOURCE_URL: jdbc:postgresql://postgres/quarkus_test
    networks:
      - tasks-network
    depends_on:
      - postgres

  nginx: <2>
    image: nginx:latest
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - tasks
    ports:
      - 8080:8080
    networks:
      - tasks-network

  db: <3>
    image: docker.io/library/postgres:18
    container_name: quarkus_test
    environment:
      - POSTGRES_USER=quarkus_test
      - POSTGRES_PASSWORD=quarkus_test
      - POSTGRES_DB=quarkus_test
    ports:
      - 5432:5432
    networks:
      - tasks-network

networks:
  tasks-network:
    driver: bridge
```

1. Define the tasks service
2. Define the nginx load balancer to route incoming traffic to an appropriate node
3. Define the configuration to run the database

## Running the database

In a separate terminal, run the below command:

```bash
docker-compose up postgres <1>
```

1. Start the database instance using the configuration options supplied in the `docker-compose.yml` file

## Run the application in Dev Mode

Run the application with:

**CLI**

```bash
quarkus dev
```

**Maven**

```bash
./mvnw quarkus:dev
```

**Gradle**

```bash
./gradlew --console=plain quarkusDev
```

After a few seconds, open another terminal and run `curl localhost:8080/tasks` to verify that we have at least one task created.

As usual, the application can be packaged using:

**CLI**

```bash
quarkus build
```

**Maven**

```bash
./mvnw install
```

**Gradle**

```bash
./gradlew build
```

and executed with `java -jar target/quarkus-app/quarkus-run.jar`.

You can also generate the native executable with:

**CLI**

```bash
quarkus build --native
```

**Maven**

```bash
./mvnw install -Dnative
```

**Gradle**

```bash
./gradlew build -Dquarkus.native.enabled=true
```

## Packaging the application and run several instances

The application can be packaged using:

**CLI**

```bash
quarkus build
```

**Maven**

```bash
./mvnw install
```

**Gradle**

```bash
./gradlew build
```

Once the build is successful, run the below command:

```bash
docker-compose up --scale tasks=2 --scale nginx=1 <1>
```

1. Start two instances of the application and a load balancer

After a few seconds, in another terminal, run `curl localhost:8080/tasks` to verify that tasks were only created at different instants and in an interval of 10 seconds.

You can also generate the native executable with:

**CLI**

```bash
quarkus build --native
```

**Maven**

```bash
./mvnw install -Dnative
```

**Gradle**

```bash
./gradlew build -Dquarkus.native.enabled=true
```

> [!WARNING]
> It's the responsibility of the deployer to clear/remove the previous state, i.e. stale jobs and triggers. Moreover, the applications that form the "Quartz cluster" should be identical, otherwise an unpredictable result may occur.

<a id="quartz-configure-instance-id"></a>
## Configuring the Instance ID

By default, the scheduler is configured with a simple instance ID generator using the machine hostname and the current timestamp, so you don't need to worry about setting a appropriate `instance-id` for each node when running in clustered mode. However, you can define a specific `instance-id` by yourself setting a configuration property reference or using other generators.

```properties
quarkus.quartz.instance-id=${HOST:AUTO} <1>
```

1. This will expand the `HOST` environment variable and use `AUTO` as the default value if `HOST` is not set.

The example below configure the generator `org.quartz.simpl.HostnameInstanceIdGenerator` named as `hostname`, so you can use its name as `instance-id` to be used. That generator uses just the machine hostname and can be appropriate in environments providing unique names for the nodes.

```properties
quarkus.quartz.instance-id=hostname
quarkus.quartz.instance-id-generators.hostname.class=org.quartz.simpl.HostnameInstanceIdGenerator
```

> [!WARNING]
> It's the responsibility of the deployer to define appropriate instance identifiers. Moreover, the applications that form the "Quartz cluster" should contain unique instance identifiers, otherwise an unpredictable result may occur. It's recommended to use an appropriate instance ID generator rather than specifying explicit identifiers.

<a id="quartz-register-plugin-listeners"></a>
## Registering Plugin and Listeners

You can register `plugins`, `job-listeners` and `trigger-listeners` through Quarkus configuration.

The example below registers the plugin `org.quartz.plugins.history.LoggingJobHistoryPlugin` named as `jobHistory` with the property `jobSuccessMessage` defined as `Job [{1}.{0}] execution complete and reports: {8}`

```properties
quarkus.quartz.plugins.jobHistory.class=org.quartz.plugins.history.LoggingJobHistoryPlugin
quarkus.quartz.plugins.jobHistory.properties.jobSuccessMessage=Job [{1}.{0}] execution complete and reports: {8}
```

You can also register a listener programmatically with an injected `org.quartz.Scheduler`:

```java
public class MyListenerManager {
    void onStart(@Observes StartupEvent event, org.quartz.Scheduler scheduler) throws SchedulerException {
        scheduler.getListenerManager().addJobListener(new MyJogListener());
        scheduler.getListenerManager().addTriggerListener(new MyTriggerListener());
    }
}
```

<a id="virtual-threads"></a>
## Run scheduled methods on virtual threads

Methods annotated with `@Scheduled` can also be annotated with `@RunOnVirtualThread`. In this case, the method is invoked on a virtual thread.

The method must return `void` and your Java runtime must provide support for virtual threads. Read [the virtual thread guide](/guides/virtual-threads/index.md) for more details.

> [!WARNING]
> This feature cannot be combined with the `run-blocking-method-on-quartz-thread` option. If `run-blocking-method-on-quartz-thread` is set, the scheduled method runs on a (platform) thread managed by Quartz.

<a id="quartz-configuration-reference"></a>
## Quartz Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-quartz_quarkus-quartz-clustered"></a> [`quarkus.quartz.clustered`](#quarkus-quartz_quarkus-quartz-clustered) config_property_copy_button:+++quarkus.quartz.clustered+++[]<br><br>Enable cluster mode or not.<br><br>If enabled make sure to set the appropriate cluster properties.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_CLUSTERED+++[] | boolean | `false` |
| [lock] <a id="quarkus-quartz_quarkus-quartz-cluster-checkin-interval"></a> [`quarkus.quartz.cluster-checkin-interval`](#quarkus-quartz_quarkus-quartz-cluster-checkin-interval) config_property_copy_button:+++quarkus.quartz.cluster-checkin-interval+++[]<br><br>The frequency (in milliseconds) at which the scheduler instance checks-in with other instances of the cluster.<br><br>Ignored if using a `ram` store i.e `StoreType#RAM`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_CLUSTER_CHECKIN_INTERVAL+++[] | long | `15000` |
| [lock] <a id="quarkus-quartz_quarkus-quartz-store-type"></a> [`quarkus.quartz.store-type`](#quarkus-quartz_quarkus-quartz-store-type) config_property_copy_button:+++quarkus.quartz.store-type+++[]<br><br>The type of store to use.<br><br>When using `StoreType#JDBC_CMT` or `StoreType#JDBC_TX` configuration values make sure that you have the datasource configured. See [Configuring your datasource](https://quarkus.io/guides/datasource) for more information.<br><br>To create Quartz tables, you can perform a schema migration via the [Flyway extension](https://quarkus.io/guides/flyway) using a SQL script matching your database picked from [Quartz repository](https://github.com/quartz-scheduler/quartz/blob/master/quartz-core/src/main/resources/org/quartz/impl/jdbcjobstore).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_STORE_TYPE+++[] | `ram`, `jdbc-tx`, `jdbc-cmt` | `ram` |
| [lock] <a id="quarkus-quartz_quarkus-quartz-thread-pool-class"></a> [`quarkus.quartz.thread-pool-class`](#quarkus-quartz_quarkus-quartz-thread-pool-class) config_property_copy_button:+++quarkus.quartz.thread-pool-class+++[]<br><br>The class name of the thread pool implementation to use.<br><br>It's important to bear in mind that Quartz threads are not used to execute scheduled methods, instead the regular Quarkus thread pool is used by default. See also `quarkus.quartz.run-blocking-scheduled-method-on-quartz-thread`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_THREAD_POOL_CLASS+++[] | string | `org.quartz.simpl.SimpleThreadPool` |
| [lock] <a id="quarkus-quartz_quarkus-quartz-datasource"></a> [`quarkus.quartz.datasource`](#quarkus-quartz_quarkus-quartz-datasource) config_property_copy_button:+++quarkus.quartz.datasource+++[]<br><br>The name of the datasource to use.<br><br>Ignored if using a `ram` store i.e `StoreType#RAM`.<br><br>Optionally needed when using the `jdbc-tx` or `jdbc-cmt` store types. If not specified, defaults to using the default datasource.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_DATASOURCE+++[] | string |  |
| [lock] <a id="quarkus-quartz_quarkus-quartz-defer-datasource-check"></a> [`quarkus.quartz.defer-datasource-check`](#quarkus-quartz_quarkus-quartz-defer-datasource-check) config_property_copy_button:+++quarkus.quartz.defer-datasource-check+++[]<br><br>If set to true, defers datasource check to runtime. False by default.<br><br>Used in combination with runtime configuration `QuartzRuntimeConfig#deferredDatasourceName()`.<br><br>It is considered a configuration error to specify a datasource via `QuartzBuildTimeConfig#dataSourceName()` along with setting this property to `true`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_DEFER_DATASOURCE_CHECK+++[] | boolean | `false` |
| [lock] <a id="quarkus-quartz_quarkus-quartz-table-prefix"></a> [`quarkus.quartz.table-prefix`](#quarkus-quartz_quarkus-quartz-table-prefix) config_property_copy_button:+++quarkus.quartz.table-prefix+++[]<br><br>The prefix for quartz job store tables.<br><br>Ignored if using a `ram` store i.e `StoreType#RAM`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_TABLE_PREFIX+++[] | string | `QRTZ_` |
| [lock] <a id="quarkus-quartz_quarkus-quartz-select-with-lock-sql"></a> [`quarkus.quartz.select-with-lock-sql`](#quarkus-quartz_quarkus-quartz-select-with-lock-sql) config_property_copy_button:+++quarkus.quartz.select-with-lock-sql+++[]<br><br>The SQL string that selects a row in the "LOCKS" table and places a lock on the row.<br><br>Ignored if using a `ram` store i.e `StoreType#RAM`.<br><br>If not set, the default value of Quartz applies, for which the "++{++0++}++" is replaced during run-time with the `table-prefix`, the "++{++1++}++" with the `instance-name`.<br><br>An example SQL string `SELECT * FROM {0}LOCKS WHERE SCHED_NAME = {1} AND LOCK_NAME = ? FOR UPDATE`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_SELECT_WITH_LOCK_SQL+++[] | string |  |
| [lock] <a id="quarkus-quartz_quarkus-quartz-driver-delegate"></a> [`quarkus.quartz.driver-delegate`](#quarkus-quartz_quarkus-quartz-driver-delegate) config_property_copy_button:+++quarkus.quartz.driver-delegate+++[]<br><br>Allows users to specify fully qualified class name for a custom JDBC driver delegate.<br><br>This property is optional and leaving it empty will result in Quarkus automatically choosing appropriate default driver delegate implementation.<br><br>Note that any custom implementation has to be a subclass of existing Quarkus implementation such as `io.quarkus.quartz.runtime.jdbc.QuarkusPostgreSQLDelegate` or `io.quarkus.quartz.runtime.jdbc.QuarkusMSSQLDelegate`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_DRIVER_DELEGATE+++[] | string |  |
| [lock] <a id="quarkus-quartz_quarkus-quartz-serialize-job-data"></a> [`quarkus.quartz.serialize-job-data`](#quarkus-quartz_quarkus-quartz-serialize-job-data) config_property_copy_button:+++quarkus.quartz.serialize-job-data+++[]<br><br>Instructs JDBCJobStore to serialize JobDataMaps in the BLOB column.<br><br>Ignored if using a `ram` store i.e `StoreType#RAM`.<br><br>If this is set to `true`, the JDBCJobStore will store the JobDataMaps in their serialize form in the BLOB Column. This is useful when you want to store complex JobData objects other than String. This is equivalent of setting `org.quartz.jobStore.useProperties` to `false`. **NOTE: When this option is set to `true`, all the non-String classes used in JobDataMaps have to be registered for serialization when building a native image**<br><br>If this is set to `false` (the default), the values can be stored as name-value pairs rather than storing more complex objects in their serialized form in the BLOB column. This can be handy, as you avoid the class versioning issues that can arise from serializing your non-String classes into a BLOB. This is equivalent of setting `org.quartz.jobStore.useProperties` to `true`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_SERIALIZE_JOB_DATA+++[] | boolean | `false` |
| <a id="quarkus-quartz_quarkus-quartz-instance-name"></a> [`quarkus.quartz.instance-name`](#quarkus-quartz_quarkus-quartz-instance-name) config_property_copy_button:+++quarkus.quartz.instance-name+++[]<br><br>The name of the Quartz instance.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_INSTANCE_NAME+++[] | string | `QuarkusQuartzScheduler` |
| <a id="quarkus-quartz_quarkus-quartz-instance-id"></a> [`quarkus.quartz.instance-id`](#quarkus-quartz_quarkus-quartz-instance-id) config_property_copy_button:+++quarkus.quartz.instance-id+++[]<br><br>The identifier of Quartz instance that must be unique for all schedulers working as if they are the same *logical* Scheduler within a cluster. Use the default value `AUTO` or some of the configured [instance ID generators](https://quarkus.io/guides/quartz#quarkus-quartz_quarkus.quartz.instance-id-generators-instance-id-generators) if you wish the identifier to be generated for you.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_INSTANCE_ID+++[] | string | `AUTO` |
| <a id="quarkus-quartz_quarkus-quartz-batch-trigger-acquisition-fire-ahead-time-window"></a> [`quarkus.quartz.batch-trigger-acquisition-fire-ahead-time-window`](#quarkus-quartz_quarkus-quartz-batch-trigger-acquisition-fire-ahead-time-window) config_property_copy_button:+++quarkus.quartz.batch-trigger-acquisition-fire-ahead-time-window+++[]<br><br>The amount of time in milliseconds that a trigger is allowed to be acquired and fired ahead of its scheduled fire time.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_BATCH_TRIGGER_ACQUISITION_FIRE_AHEAD_TIME_WINDOW+++[] | long | `0` |
| <a id="quarkus-quartz_quarkus-quartz-batch-trigger-acquisition-max-count"></a> [`quarkus.quartz.batch-trigger-acquisition-max-count`](#quarkus-quartz_quarkus-quartz-batch-trigger-acquisition-max-count) config_property_copy_button:+++quarkus.quartz.batch-trigger-acquisition-max-count+++[]<br><br>The maximum number of triggers that a scheduler node is allowed to acquire (for firing) at once.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_BATCH_TRIGGER_ACQUISITION_MAX_COUNT+++[] | int | `1` |
| <a id="quarkus-quartz_quarkus-quartz-thread-count"></a> [`quarkus.quartz.thread-count`](#quarkus-quartz_quarkus-quartz-thread-count) config_property_copy_button:+++quarkus.quartz.thread-count+++[]<br><br>The size of scheduler thread pool. This will initialize the number of worker threads in the pool.<br><br>It's important to bear in mind that Quartz threads are not used to execute scheduled methods, instead the regular Quarkus thread pool is used by default. See also `quarkus.quartz.run-blocking-scheduled-method-on-quartz-thread`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_THREAD_COUNT+++[] | int | `10` |
| <a id="quarkus-quartz_quarkus-quartz-thread-priority"></a> [`quarkus.quartz.thread-priority`](#quarkus-quartz_quarkus-quartz-thread-priority) config_property_copy_button:+++quarkus.quartz.thread-priority+++[]<br><br>Thread priority of worker threads in the pool.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_THREAD_PRIORITY+++[] | int | `5` |
| <a id="quarkus-quartz_quarkus-quartz-misfire-threshold"></a> [`quarkus.quartz.misfire-threshold`](#quarkus-quartz_quarkus-quartz-misfire-threshold) config_property_copy_button:+++quarkus.quartz.misfire-threshold+++[]<br><br>Defines how late the schedulers should be to be considered misfired.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_MISFIRE_THRESHOLD+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-quartz_quarkus-quartz) | `60S` |
| <a id="quarkus-quartz_quarkus-quartz-shutdown-wait-time"></a> [`quarkus.quartz.shutdown-wait-time`](#quarkus-quartz_quarkus-quartz-shutdown-wait-time) config_property_copy_button:+++quarkus.quartz.shutdown-wait-time+++[]<br><br>The maximum amount of time Quarkus will wait for currently running jobs to finish. If the value is `0`, then Quarkus will not wait at all for these jobs to finish - it will call `org.quartz.Scheduler.shutdown(false)` in this case.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_SHUTDOWN_WAIT_TIME+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-quartz_quarkus-quartz) | `10S` |
| <a id="quarkus-quartz_quarkus-quartz-simple-trigger-misfire-policy"></a> [`quarkus.quartz.simple-trigger.misfire-policy`](#quarkus-quartz_quarkus-quartz-simple-trigger-misfire-policy) config_property_copy_button:+++quarkus.quartz.simple-trigger.misfire-policy+++[]<br><br>The quartz misfire policy for this job.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_SIMPLE_TRIGGER_MISFIRE_POLICY+++[] | `smart-policy`, `ignore-misfire-policy`, `fire-now`, `simple-trigger-reschedule-now-with-existing-repeat-count`, `simple-trigger-reschedule-now-with-remaining-repeat-count`, `simple-trigger-reschedule-next-with-remaining-count`, `simple-trigger-reschedule-next-with-existing-count`, `cron-trigger-do-nothing` | `smart-policy` |
| <a id="quarkus-quartz_quarkus-quartz-cron-trigger-misfire-policy"></a> [`quarkus.quartz.cron-trigger.misfire-policy`](#quarkus-quartz_quarkus-quartz-cron-trigger-misfire-policy) config_property_copy_button:+++quarkus.quartz.cron-trigger.misfire-policy+++[]<br><br>The quartz misfire policy for this job.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_CRON_TRIGGER_MISFIRE_POLICY+++[] | `smart-policy`, `ignore-misfire-policy`, `fire-now`, `simple-trigger-reschedule-now-with-existing-repeat-count`, `simple-trigger-reschedule-now-with-remaining-repeat-count`, `simple-trigger-reschedule-next-with-remaining-count`, `simple-trigger-reschedule-next-with-existing-count`, `cron-trigger-do-nothing` | `smart-policy` |
| <a id="quarkus-quartz_quarkus-quartz-unsupported-properties-full-property-key"></a> [`quarkus.quartz.unsupported-properties."full-property-key"`](#quarkus-quartz_quarkus-quartz-unsupported-properties-full-property-key) config_property_copy_button:+++quarkus.quartz.unsupported-properties."full-property-key"+++[]<br><br>Properties that should be passed on directly to Quartz. Use the full configuration property key here, for instance ``quarkus.quartz.unsupported-properties."org.quartz.scheduler.jmx.export" = true`)`.<br><br>Properties set here are completely unsupported: as Quarkus doesn't generally know about these properties and their purpose, there is absolutely no guarantee that they will work correctly, and even if they do, that may change when upgrading to a newer version of Quarkus (even just a micro/patch version).<br><br>Consider using a supported configuration property before falling back to unsupported ones. If none exists, make sure to file a feature request so that a supported configuration property can be added to Quarkus, and more importantly so that the configuration property is tested regularly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_UNSUPPORTED_PROPERTIES__FULL_PROPERTY_KEY_+++[] | Map\<String,String> |  |
| <a id="quarkus-quartz_quarkus-quartz-run-blocking-scheduled-method-on-quartz-thread"></a> [`quarkus.quartz.run-blocking-scheduled-method-on-quartz-thread`](#quarkus-quartz_quarkus-quartz-run-blocking-scheduled-method-on-quartz-thread) config_property_copy_button:+++quarkus.quartz.run-blocking-scheduled-method-on-quartz-thread+++[]<br><br>When set to `true`, blocking scheduled methods are invoked on a thread managed by Quartz instead of a thread from the regular Quarkus thread pool (default).<br><br>When this option is enabled, blocking scheduled methods do not run on a `duplicated context`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_RUN_BLOCKING_SCHEDULED_METHOD_ON_QUARTZ_THREAD+++[] | boolean | `false` |
| <a id="quarkus-quartz_quarkus-quartz-deferred-datasource-name"></a> [`quarkus.quartz.deferred-datasource-name`](#quarkus-quartz_quarkus-quartz-deferred-datasource-name) config_property_copy_button:+++quarkus.quartz.deferred-datasource-name+++[]<br><br>The name of the datasource to use.<br><br>This property in valid only in combination with `QuartzBuildTimeConfig#deferDatasourceCheck()`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_DEFERRED_DATASOURCE_NAME+++[] | string |  |
| <a id="quarkus-quartz_section_quarkus-quartz-instance-id-generators"></a> [Instance ID generators](#quarkus-quartz_section_quarkus-quartz-instance-id-generators) | Type | Default |
| [lock] <a id="quarkus-quartz_quarkus-quartz-instance-id-generators-generator-name-class"></a> [`quarkus.quartz.instance-id-generators."generator-name".class`](#quarkus-quartz_quarkus-quartz-instance-id-generators-generator-name-class) config_property_copy_button:+++quarkus.quartz.instance-id-generators."generator-name".class+++[]<br><br>Class name for the configuration.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_INSTANCE_ID_GENERATORS__GENERATOR_NAME__CLASS+++[] | string | required [exclamation circle] |
| [lock] <a id="quarkus-quartz_quarkus-quartz-instance-id-generators-generator-name-properties-property-key"></a> [`quarkus.quartz.instance-id-generators."generator-name".properties."property-key"`](#quarkus-quartz_quarkus-quartz-instance-id-generators-generator-name-properties-property-key) config_property_copy_button:+++quarkus.quartz.instance-id-generators."generator-name".properties."property-key"+++[]<br><br>The properties passed to the class.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_INSTANCE_ID_GENERATORS__GENERATOR_NAME__PROPERTIES__PROPERTY_KEY_+++[] | Map\<String,String> |  |
| <a id="quarkus-quartz_section_quarkus-quartz-trigger-listeners"></a> [Trigger listeners](#quarkus-quartz_section_quarkus-quartz-trigger-listeners) | Type | Default |
| [lock] <a id="quarkus-quartz_quarkus-quartz-trigger-listeners-listener-name-class"></a> [`quarkus.quartz.trigger-listeners."listener-name".class`](#quarkus-quartz_quarkus-quartz-trigger-listeners-listener-name-class) config_property_copy_button:+++quarkus.quartz.trigger-listeners."listener-name".class+++[]<br><br>Class name for the configuration.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_TRIGGER_LISTENERS__LISTENER_NAME__CLASS+++[] | string | required [exclamation circle] |
| [lock] <a id="quarkus-quartz_quarkus-quartz-trigger-listeners-listener-name-properties-property-key"></a> [`quarkus.quartz.trigger-listeners."listener-name".properties."property-key"`](#quarkus-quartz_quarkus-quartz-trigger-listeners-listener-name-properties-property-key) config_property_copy_button:+++quarkus.quartz.trigger-listeners."listener-name".properties."property-key"+++[]<br><br>The properties passed to the class.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_TRIGGER_LISTENERS__LISTENER_NAME__PROPERTIES__PROPERTY_KEY_+++[] | Map\<String,String> |  |
| <a id="quarkus-quartz_section_quarkus-quartz-job-listeners"></a> [Job listeners](#quarkus-quartz_section_quarkus-quartz-job-listeners) | Type | Default |
| [lock] <a id="quarkus-quartz_quarkus-quartz-job-listeners-listener-name-class"></a> [`quarkus.quartz.job-listeners."listener-name".class`](#quarkus-quartz_quarkus-quartz-job-listeners-listener-name-class) config_property_copy_button:+++quarkus.quartz.job-listeners."listener-name".class+++[]<br><br>Class name for the configuration.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_JOB_LISTENERS__LISTENER_NAME__CLASS+++[] | string | required [exclamation circle] |
| [lock] <a id="quarkus-quartz_quarkus-quartz-job-listeners-listener-name-properties-property-key"></a> [`quarkus.quartz.job-listeners."listener-name".properties."property-key"`](#quarkus-quartz_quarkus-quartz-job-listeners-listener-name-properties-property-key) config_property_copy_button:+++quarkus.quartz.job-listeners."listener-name".properties."property-key"+++[]<br><br>The properties passed to the class.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_JOB_LISTENERS__LISTENER_NAME__PROPERTIES__PROPERTY_KEY_+++[] | Map\<String,String> |  |
| <a id="quarkus-quartz_section_quarkus-quartz-plugins"></a> [Plugins](#quarkus-quartz_section_quarkus-quartz-plugins) | Type | Default |
| [lock] <a id="quarkus-quartz_quarkus-quartz-plugins-plugin-name-class"></a> [`quarkus.quartz.plugins."plugin-name".class`](#quarkus-quartz_quarkus-quartz-plugins-plugin-name-class) config_property_copy_button:+++quarkus.quartz.plugins."plugin-name".class+++[]<br><br>Class name for the configuration.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_PLUGINS__PLUGIN_NAME__CLASS+++[] | string | required [exclamation circle] |
| [lock] <a id="quarkus-quartz_quarkus-quartz-plugins-plugin-name-properties-property-key"></a> [`quarkus.quartz.plugins."plugin-name".properties."property-key"`](#quarkus-quartz_quarkus-quartz-plugins-plugin-name-properties-property-key) config_property_copy_button:+++quarkus.quartz.plugins."plugin-name".properties."property-key"+++[]<br><br>The properties passed to the class.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_PLUGINS__PLUGIN_NAME__PROPERTIES__PROPERTY_KEY_+++[] | Map\<String,String> |  |
| <a id="quarkus-quartz_section_quarkus-quartz-misfire-policy"></a> [Misfire policy per job configuration](#quarkus-quartz_section_quarkus-quartz-misfire-policy) | Type | Default |
| <a id="quarkus-quartz_quarkus-quartz-misfire-policy-identity"></a> [`quarkus.quartz.misfire-policy."identity"`](#quarkus-quartz_quarkus-quartz-misfire-policy-identity) config_property_copy_button:+++quarkus.quartz.misfire-policy."identity"+++[]<br><br>The quartz misfire policy for this job.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_QUARTZ_MISFIRE_POLICY__IDENTITY_+++[] | `smart-policy`, `ignore-misfire-policy`, `fire-now`, `simple-trigger-reschedule-now-with-existing-repeat-count`, `simple-trigger-reschedule-now-with-remaining-repeat-count`, `simple-trigger-reschedule-next-with-remaining-count`, `simple-trigger-reschedule-next-with-existing-count`, `cron-trigger-do-nothing` | `smart-policy` |

<a id="duration-note-anchor-quarkus-quartz_quarkus-quartz"></a>

> [!NOTE]
> **About the Duration format**
>
> To write duration values, use the standard `java.time.Duration` format. See the [Duration#parse() Java API documentation](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html#parse(java.lang.CharSequence)) for more information.
>
> You can also use a simplified format, starting with a number:
>
> - If the value is only a number, it represents time in seconds.
> - If the value is a number followed by `ms`, it represents time in milliseconds.
>
> In other cases, the simplified format is translated to the `java.time.Duration` format for parsing:
>
> - If the value is a number followed by `h`, `m`, or `s`, it is prefixed with `PT`.
> - If the value is a number followed by `d`, it is prefixed with `P`.
