# REST microservice example project

## Technologies and frameworks 

* Java 11, but source code is Java 8 compatible; 
* Jersey as JAX-RS framework;
* Grizzly as underlying http-server, skipping a servlet container;
* HK2 as light-weight JSR-330 dependency injection framework;
* Swagger as OpenAPI framework and UI, generating specs from the model (annotated Java classes);
* H2 as in-memory storage;
* Jooq as database framework, generating model Java classes;
* Flyway as database versioning tool.

This base allows to build the light-weight and acceptable performance micro-services with
a little supporting code.
How well the framework or library is maintained and how active is community were also taken
into account.

There's no standard validation solutions were applied during its restrictions,
for example some validation actions have to be performed in the same database transaction
as a business-logic that leads to a duplication of a input-parameter validation and 
storage-level validations. So in this project template one custom approach is used, leaving
aside a Java Bean Validation and others.

## The project modules 
* `microservice-webapp` base module for the particular microservice implementations;
* A sample implementation in `account-microservice` module;
* `build-tools` includes the extensions:
  * for Jooq code-generation plugin to reflect table column comments as Swagger annotation in 
  the model Java classes
  * the styles for checkstyle plugin

## Code quality
The code quality is controlled during the application build by next Maven plugins:
* Checkstyle analyzes vs Sun's Java style, generating reports in
  `account-microservice/target/checkstyle-result.xml`
* PMD, generating reports in `account-microservice/target/site/pmd.html` and
  `account-microservice/target/site/cpd.html`
* Dependency plugin, assures all used dependencies are explicitly declared while unused. 
  dependencies are prohibited.  
* Shade plugin assures there's no Java class interference by different dependencies.
* Jacoco checks for a 100% unit test coverage, generating reports in 
  `account-microservice/target/jacoco-report/index.html`.

Any issue caught by the above checks leads to Maven build failure.  

## Points to improve
* The microservice architecture often requires distributed transaction support that can be
  achieved with JTA framework supporting JAX-RS call chain, for example Atomikos.
* The H2 is used in-memory mode that's definitely should be changed for production. Also JDBC
  pool should be changed to HikariCP or analogous.  
* Even though the given technology stack makes good balance between a simplicity of implementation,
  reliability, a performance and following of standards, for top-performance solutions may be 
  reasonable to skip some layers like Jackson and Jooq and to switch to more direct manipulation
  with a network via Netty channels and with a database via JDBC, or to apply some NoSQL database.
* Message format may be changed from JSON to some binary variations, like Protobuf.  
* If there's no necessity to expose Swagger-UI, the Jetty's underlying http server can be easily
  switched to Netty. 
* Other code-quality tools, like FindBugs, can be added to the Maven build.
 

## Dependency tree  
``` 
com.github.edwgiz.sample.bank:account-microservice:jar:1.0
+- com.github.edwgiz.sample.bank:microservice-webapp:jar:1.0:compile
|  +- org.glassfish.jersey.containers:jersey-container-grizzly2-http:jar:2.30:compile
|  +- org.glassfish.grizzly:grizzly-framework:jar:2.4.4:compile
|  +- org.glassfish.grizzly:grizzly-http-server:jar:2.4.4:compile
|  |  \- org.glassfish.grizzly:grizzly-http:jar:2.4.4:compile
|  +- org.glassfish.jersey.core:jersey-common:jar:2.30:compile
|  |  \- com.sun.activation:jakarta.activation:jar:1.2.1:compile
|  +- org.glassfish.jersey.inject:jersey-hk2:jar:2.30:runtime
|  |  +- org.glassfish.hk2:hk2-locator:jar:2.6.1:runtime
|  |  \- org.javassist:javassist:jar:3.25.0-GA:runtime
|  +- org.glassfish.hk2:hk2-api:jar:2.6.1:compile
|  |  +- org.glassfish.hk2:hk2-utils:jar:2.6.1:compile
|  |  \- org.glassfish.hk2.external:aopalliance-repackaged:jar:2.6.1:compile
|  +- com.fasterxml.jackson.jaxrs:jackson-jaxrs-base:jar:2.10.2.1:compile
|  +- com.fasterxml.jackson.datatype:jackson-datatype-jsr310:jar:2.10.2:compile
|  +- com.h2database:h2:jar:1.4.200:compile
|  +- org.flywaydb:flyway-core:jar:6.2.2:compile
|  +- org.slf4j:slf4j-simple:jar:1.7.30:runtime
|  +- org.slf4j:slf4j-api:jar:1.7.30:compile
|  \- org.slf4j:jul-to-slf4j:jar:1.7.30:compile
+- org.apache.commons:commons-lang3:jar:3.9:compile
+- org.glassfish.hk2.external:jakarta.inject:jar:2.6.1:compile
+- org.glassfish.jersey.core:jersey-server:jar:2.30:compile
|  +- org.glassfish.jersey.core:jersey-client:jar:2.30:compile
|  +- org.glassfish.jersey.media:jersey-media-jaxb:jar:2.30:compile
|  +- jakarta.annotation:jakarta.annotation-api:jar:1.3.5:compile
|  \- jakarta.xml.bind:jakarta.xml.bind-api:jar:2.3.2:compile
+- jakarta.validation:jakarta.validation-api:jar:2.0.2:compile
+- jakarta.ws.rs:jakarta.ws.rs-api:jar:2.1.6:compile
+- com.fasterxml.jackson.core:jackson-databind:jar:2.10.2:compile
+- com.fasterxml.jackson.core:jackson-annotations:jar:2.10.2:compile
+- com.fasterxml.jackson.core:jackson-core:jar:2.10.2:compile
+- com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:jar:2.10.2.1:compile
|  \- com.fasterxml.jackson.module:jackson-module-jaxb-annotations:jar:2.10.2:compile
+- io.swagger.core.v3:swagger-annotations:jar:2.1.1:compile
+- org.jooq:jooq:jar:3.12.4:compile
|  \- org.reactivestreams:reactive-streams:jar:1.0.2:compile
+- org.junit.jupiter:junit-jupiter-api:jar:5.6.0:test
|  +- org.apiguardian:apiguardian-api:jar:1.1.0:test
|  +- org.opentest4j:opentest4j:jar:1.2.0:test
|  \- org.junit.platform:junit-platform-commons:jar:1.6.0:test
+- org.junit.jupiter:junit-jupiter-engine:jar:5.6.0:test
|  \- org.junit.platform:junit-platform-engine:jar:1.6.0:test
+- org.junit.vintage:junit-vintage-engine:jar:5.6.0:test
+- junit:junit:jar:4.13:test
|  \- org.hamcrest:hamcrest-core:jar:1.3:test
+- org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-inmemory:jar:2.30:test
\- org.glassfish.jersey.test-framework:jersey-test-framework-core:jar:2.30:test
```

## Build result
The microservice application assembles as all-in-one jar using Maven.
Example to run the build from a command-line
```
c:\>
c:\>cd C:\Projects\bank-services
C:\Projects\bank-services>SET JAVA_HOME=c:\Java\jdk-11.0.5
C:\Projects\bank-services>c:\Java\apache-maven-3.6.3\bin\mvn
```
The assembled all-in-one jar weight around 15 Mb.
 
## Start microservice instance
The direct jar start requires arguments to define a JVM system properties. 
`-Dwebserver.http.hostname=127.0.0.1`, `-Dwebserver.http.port=8080`

Example to run the application from a command-line
```
C:\Projects\bank-services>%JAVA_HOME%\bin\java -Dwebserver.http.hostname=127.0.0.1 -Dwebserver.http.port=8080 -jar ./account-microservice/target/account-microservice-1.0.jar
[main] INFO com.github.edwgiz.sample.bank.core.webapp.WebAppBase - Start server...
[main] INFO org.glassfish.grizzly.http.server.NetworkListener - Started listener bound to [127.0.0.1:8080]
[main] INFO org.glassfish.grizzly.http.server.HttpServer - [HttpServer] Started.
[main] INFO com.github.edwgiz.sample.bank.core.webapp.WebAppBase - Start server - done in 0.61 secs
```

Swagger UI should be accessible at `http://127.0.0.1:8080/swagger-ui/` after the application start.

### Docker

Example to build a docker image from a command-line
```
C:\Projects\bank-services\account-microservice>docker build -t account-microservice:1.0.0 .
```
Resulting docker image size is around 75Mb

### Performance test

The `account-microservice/src/test/jmeter/test-plan.jmx` JMeter 5 scenario with 32 threads has shown 
below the results on Core i7-7700k CPU

Label|# Samples|Average millis|Throughput 1/sec|Received KB/sec|Sent KB/sec
---|---|---|---|---|---
Account PUT|32000|2|13300.08313|3156.39|4497.95
Account GET|640000|1|19492.58368|4578.59|4357.15
Payment PUT|320000|5|5530.97345|377.34|2062.15
Payment GET|128000|710|44.96664|2006.25|13.3