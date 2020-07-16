# Flexudy API Java Client Library

[![Build Status](https://travis-ci.com/flexudy/flexudy-client-java.svg?branch=master)](https://travis-ci.com/flexudy/flexudy-client-java)

The official [Flexudy](https://flexudy.com) Java client library.

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

Please see the [REST API Docs](https://developers.flexudy.com) for the most
up-to-date documentation.

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
        final HttpClientConfig httpConfig = HttpClientConfig.builder()
                                                            .connectTimeoutSeconds(60)
                                                            .readTimeoutSeconds(30)
                                                            .writeTimeoutSeconds(20)
                                                            .build();
        final FlexudyGatewayClient client = FlexudyGatewayClient.builder()
                                                                .licenseKey(licenseKey)
                                                                .httpClientConfig(httpConfig)
                                                                .environment(Environment.PRODUCTION)
                                                                .build();

        
    }
}


```

FlexudyGatewayExample.java

```java

import com.flexudy.education.gateway_java_client.data.common.AsyncRequestData;
import com.flexudy.education.gateway_java_client.data.common.CommonRequestData;
import com.flexudy.education.gateway_java_client.data.common.CommonRequestData.SimpleAsyncRequestData;
import com.flexudy.education.gateway_java_client.data.common.CommonRequestData.SimpleCommonRequestData;
import com.flexudy.education.gateway_java_client.data.quiz.ClozeQuestion;
import com.flexudy.education.gateway_java_client.data.quiz.WHQuestion;
import com.flexudy.education.gateway_java_client.data.summary.Summary;
import com.flexudy.education.gateway_java_client.service.FlexudyGatewayClient;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MINUTES;

public class Driver {

    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
        final String sampleText = "The capital of the UAE is Abu Dhabi. The capital of France is Paris";
        final String licenseKey = "3deaeb03-4425-435d-b04c-2ca6428f6d96";
        
        final CommonRequestData commonRequestData = SimpleCommonRequestData.builder()
                                                                           .contentUrl("https://flexudy.com")
                                                                           .build();
        final AsyncRequestData asyncRequestData = SimpleAsyncRequestData.builder()
                                                                        .textContent(sampleText)
                                                                        .build();
        
        final FlexudyGatewayClient client = FlexudyGatewayClient.builder()
                                                                .licenseKey(licenseKey)
                                                                .build();
        
        System.out.println("\n*********** Generating Cloze Questions for a URL source ***********\n");
        System.out.println(String.format("Cloze Quiz: %s", client.generateClozeQuiz(commonRequestData)));
        
        System.out.println("\n*********** Generating WH Questions for a URL source ***********\n");
        System.out.println(String.format("WH Quiz: %s", client.generateWHQuiz(commonRequestData)));
        
        System.out.println("\n*********** Generating Summary for a URL source ***********\n");
        System.out.println(client.generateSummary(commonRequestData).getSummary());
        
        System.out.println("\n*********** Generating Cloze Questions for an Asynchronous URL source ***********\n");
        final Future<List<ClozeQuestion>> futureClozeQuestions = client.submitClozeQuizJob(asyncRequestData);
        System.out.println(String.format("Async Cloze Quiz: %s", futureClozeQuestions.get(5, MINUTES)));
        
        System.out.println("\n*********** Generating WH Questions for an Asynchronous URL source ***********\n");
        final Future<List<WHQuestion>> futureWHQuestions = client.submitWHQuizJob(asyncRequestData);
        System.out.println(String.format("Async WH Quiz: %s", futureWHQuestions.get(5, MINUTES)));
        
        System.out.println("\n*********** Generating Summary for an Asynchronous URL source  ***********\n");
        final Future<Summary> futureSummary = client.submitSummaryJob(asyncRequestData);
        System.out.println("Async Summary: " + futureSummary.get(5, MINUTES));
        
        System.exit(0);
    }
}
```

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