# Use JFR to profile and monitor native executables

Quarkus native executables offer many benefits such as even faster start-up time and low footprint. However, one major drawback is that such native binaries can be less observable. The good news is that you can still use JDK Flight Recorder (JFR) when running your Quarkus applications as native executables.

Native executable JFR is still limited compared to regular JFR in the JVM. However, major improvements have been made over the past couple of years that make it a very powerful tool to profile and monitor your native executables.

Check out these recent articles to find out more about how you can start using JFR with native executables:

- [How to monitor Quarkus native executables with JFR](https://developers.redhat.com/articles/2023/06/13/how-monitor-quarkus-native-executables-jfr)
- [Improvements to Native Image JFR support in GraalVM for JDK 20](https://developers.redhat.com/articles/2023/06/13/improvements-native-image-jfr-support-graalvm-jdk-20)
