# Using Pact and Quarkus to Tame Microservices Testing

In a microservices architecture, making sure each microservices works is (relatively) easy. The microservices are usually small, and easy to test. But how do you make sure the microservices work together? How do you know if the system as a whole works?

One answer is contract testing. Contract testing gives more confidence than testing individual services, but the cost is far lower than end-to-end testing.

## What's wrong with end-to-end testing?

Even when developing, standing up all the dependencies and consumers for an individual microservice can be hard work. Recently, our consulting colleagues received a plea for help from the CTO of a tech startup, who couldn't run his dev stack on a brand new laptop with 64 GB of RAM, because the application involved dozens of microservices and they consumed so many resources. (If this sounds familiar, Quarkus can help [lower the resource consumption of the stack](https://www.redhat.com/en/resources/greener-java-applications-detail#section-7), but that's a different topic!)

Teams sometimes address the challenge of local microservices development by providing remote environments into which local code can be injected. This is sometimes called ["remocal development"](https://blog.getambassador.io/testing-microservices-isolating-requests-not-environments-with-telepresence-f22535789253) or telepresence. Another, purely local, model, is [local virtual environments](https://eng.lyft.com/scaling-productivity-on-microservices-at-lyft-part-2-optimizing-for-fast-local-development-9f27a98b47ee).

While these environments can be useful, they can also be fragile, and managing them often needs a dedicated platform team.

## What's wrong with mocks?

When having 'real' versions of the rest of the system to test against is too heavy, teams often use mocks or stubs. (Mocks and stubs are subtly different, but for simplicity I'll use "mocks" to describe both.) Mocks have many advantages; they're lightweight and enable unit testing of code with external dependencies. However, mocks also have a big disadvantage; there's no guarantee the mock behaves like real the thing. Users of a service will bake their own assumptions about how a service behaves into a mock. If a service changes, it's up to consuming code to figure out what's changed and update the mocks.

Sometimes, the first time these assumptions are tested is in production.

## Contract tests

How can we make a link between the mock being used by a consumer, and the functional validation being done by the provider? This is where contract testing helps. A contract testing framework powers two things:

- A generated mock, which is used by the consumer to validate the consumer code behaves correctly. The mock is generated from the contract and examples.
- Generated functional tests, which is validates the provider behaves as expected. These tests are generated from the same contract and examples as the consumer's mock.

With [Test Driven Development(TDD)](https://en.wikipedia.org/wiki/Test-driven_development), you start with tests (a description of the desired behaviour) and work backwards to an implementation. You can do the same thing with contract testing; you start with the contract, which describes what the service needs to do, and work backwards to the implementation. This is known as "contract-first", and it can be a very effective development technique. My colleagues in Red Hat App Dev Consulting have written some great articles about [how they use contract-first development](https://appdev.consulting.redhat.com/tracks/contract-first/).

## Contract test options

There are a few different contract-testing frameworks out there, including Pact, Microcks, Spring Cloud Contract. Some teams also build up their own OpenAPI-based toolchains, such as Schemathesis for functional tests, and Prism for the mocking. Arguably the most popular contract testing solution is Pact, so it's where the Quarkiverse support for contract testing has started.

- Pact is polyglot, with bindings for almost all popular languages.
- It's an integrated solution which provides both mocks for consumers and functional tests for providers.
- It's standalone, and can be run without standing up any extra services, although a Pact Broker with some nice value-adds is available.
- Although Pact started as a REST-only solution, it is now [pluggable](https://docs.pact.io/blog/2022/11/11/pact-plugin-framework-launch), which allows it to support a range of protocols and transports

The Pact team have a [good overview](https://pactflow.io/blog/contract-testing-using-json-schemas-and-open-api-part-1/) of the advantages and disadvantages of schema-based testing (such as validation based on an OpenAPI spec) and contract testing.

## What's new with Pact and Quarkus

Using Pact with Quarkus isn't new; Quarkus contributors made [several](https://github.com/quarkusio/quarkus/commit/70902a005842a083814aea13567b8bc82574a021) [classloading adjustments](https://github.com/quarkusio/quarkus/commit/910227622e0e4c51c111872962cfd5364674673a#diff-40752b74da493b2aa06facf4b1ebd7a81736f6318dcfe76ddae90deafa57fad7) in Quarkus core to support Pact testing in Quarkus 2.0, but this support was limited. In particular, Pact tests couldn't run in continuous testing mode.

Quarkus 3.0 moves Pact support from Quarkus core to its own Quarkiverse extension, where it can be deeper. Quarkus core also includes classloading changes in the Kotlin extension and some classloading fixes in continuous testing itself. These mean that, with the Pact Quarkiverse [provider](http://quarkus.io/extensions/io.quarkiverse.pact/quarkus-pact-provider) and [consumer extensions](http://quarkus.io/extensions/io.quarkiverse.pact/quarkus-pact-consumer) Pact tests work properly with `quarkus test` and `quarkus dev`.

To install the consumer extensions, run

```
quarkus ext add io.quarkiverse.pact:quarkus-pact-consumer
```

The provider extension can be installed with

```
quarkus ext add io.quarkiverse.pact:quarkus-pact-provider
```

For a deeper dive into contract testing, check out [Quarkus Insights #117](https://www.youtube.com/watch?v=d9CSY8HuZ9U).

## Summary

If you're using microservices, you should seriously consider contract testing. With the new Pact extension, Quarkus 3 allows contract tests to be developed using the same great workflow as other tests.

## More resources

- [Documentation for the Pact extensions](https://quarkiverse.github.io/quarkiverse-docs/quarkus-pact/dev/index.html)
- [The Pact JVM binding documentation](https://docs.pact.io/implementation_guides/jvm)
- [Contract Testing Module](https://quarkus.io/quarkus-workshops/super-heroes/variants/bt-maven-os-all-ai-true-azure-true-cli-true-container-true-contract-testing-true-extension-true-locked-down-true-kubernetes-true-messaging-true-native-true-observability-false/spine/#contract-testing) of the Quarkus Superheroes workshop
- [Quarkus Insights about the Pact extension](https://www.youtube.com/watch?v=d9CSY8HuZ9U)
- [Sample application showing the Quarkus Pact extension](https://github.com/holly-cummins/pact-quarkus-sweater-demo)
- [Contract-first development](https://appdev.consulting.redhat.com/tracks/contract-first/) (with OpenAPIGenerator, Schemathesis, and Prism)
