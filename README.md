# Flexudy API Java client library

[![Maven Central](https://img.shields.io/maven-central/v/com.flexudy.education/gateway-java-client)](https://mvnrepository.com/artifact/com.flexudy.education/gateway-java-client)
[![Build Status](https://travis-ci.org/flexudy/flexudy-gateway-java.svg?branch=master)](https://travis-ci.org/flexudy/gateway-java-client-java)

The official [Flexudy][flexudy] Java client library.

## Installation

### Requirements

- Java 1.9 or later

### Gradle users

Add this dependency to your project's build file:

```groovy
implementation "com.flexudy.education:gateway-java-client:1.0"
```

### Maven users

Add this dependency to your project's POM:

```xml
<dependency>
  <groupId>com.flexudy.education</groupId>
  <artifactId>gateway-java-client</artifactId>
  <version>1.0</version>
</dependency>
```

### Others

You'll need to manually install the following JARs:

- The Gateway Client JAR from <https://github.com/flexudy/gateway-java-client/releases/latest>

## Documentation

Please see the [Java API docs][api-docs] for the most
up-to-date documentation.

You can also refer to the [online Javadoc][javadoc].

## Usage
The main interface to the API is via the FlexudyGatewayClient Facade Object as shown below.

### Configuring Environment
By default the Gateway connects to the `PRODUCTION` server at https://gateway.flexudy.com. In order, to
switch to another environment set it during the Gateway object construction.

### Configuring Timeouts

Connect, write and read timeouts can be configured using the HttpClientConfig object when creating the Gateway client 
object. It would default to the following if no custom client config is provided:
- connect timeout: 1 minutes
- write timeout: 3 minutes
- read timeout: 3 minutes

```java
import com.flexudy.education.gateway_java_client.service.FlexudyGatewayClient
import com.flexudy.education.gateway_java_client.service.HttpClientConfig;

public class Driver {

    public static void main(String[] args) {
        final HttpClientConfig httpConfig = HttpClientConfig.builder().connectTimeoutSeconds(60)
                                                                      .readTimeoutSeconds(30)
                                                                      .writeTimeoutSeconds(20)
                                                                      .build();
        final FlexudyGatewayClient client = FlexudyGatewayClient.builder().licenseKey(licenseKey)
                                                                          .httpClientConfig(httpConfig);
                                                                          .environment(Environment.PRODUCTION).build();

        
    }
}


```

FlexudyGatewayExample.java

```java
import com.flexudy.education.gateway_java_client.data.common.QuizRequest;
import com.flexudy.education.gateway_java_client.data.quiz.ClozeQuestion;
import com.flexudy.education.gateway_java_client.data.summary.SummaryRequest;
import com.flexudy.education.gateway_java_client.service.FlexudyGatewayClient;
import com.flexudy.education.gateway_java_client.service.network.Environment;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Driver {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        final String licenseKey = "715d26f5-260a-43c9-838f-82c9664ca701";
        final FlexudyGatewayClient client = FlexudyGatewayClient.builder().licenseKey(licenseKey).environment(Environment.PRODUCTION).build();

        System.out.println("Generating Cloze Questions for a URL source");
        final QuizRequest quizRequest = QuizRequest.builder().contentUrl("https://flexudy.com").build();
        client.generateClozeQuestions(quizRequest).forEach(q -> System.out.println(q.getQuestion() + "," + q.getAnswer()));

        System.out.println("Generating Summary for a URL source");
        final SummaryRequest summaryRequest = SummaryRequest.builder().contentUrl("https://forbes.com").build();
        System.out.println(client.generateSummary(summaryRequest).getSummary());

        System.out.println("Generating Cloze Questions for a URL source Asynchronously");
        final Future<List<ClozeQuestion>> futureQuestions = client.submitClozeQuestionJob(quizRequest);
        try {
            while (!futureQuestions.isDone()) {
                System.out.println("Waiting for future jobs to be done...");
                Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            }
        } catch (Exception ex) {
            futureQuestions.cancel(true);
        }
        futureQuestions.get().forEach(q -> System.out.println(q.getQuestion() + "," + q.getAnswer()));
    }
}
```

See the project's [functional tests][functional-tests] for more examples.

Please take care to set conservative read timeouts. Some API requests can take
some time, and a short timeout increases the likelihood of a problem within our
servers.

## Development

To run the tests:

```sh
./mvn test
```

You can run particular tests by passing `--tests Class#method`. Make sure you
use the fully qualified class name. For example:

```sh
mvn -Dtest=com.flexudy.education.gateway_java_client.service.FlexudyGatewayClient test
mvn -Dtest=com.flexudy.education.gateway_java_client.service.FlexudyGatewayClient#testGenerateSummary test
```

The library uses [google-java-format][google-java-format] for code formatting. Code must be
formatted before PRs are submitted, otherwise CI will fail.

The library uses [Project Lombok][lombok]. While it is not a requirement, you
might want to install a [plugin][lombok-plugins] for your favorite IDE to
facilitate development.

[google-java-format]: https://github.com/google/google-java-format
[lombok]: https://projectlombok.org
[lombok-plugins]: https://projectlombok.org/setup/overview

## License

```text
Copyright 2020 Flexudy

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```
