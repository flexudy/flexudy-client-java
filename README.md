# Flexudy API Java Client Library

[![Build Status](https://travis-ci.com/flexudy/flexudy-client-java.svg?branch=master)](https://travis-ci.com/flexudy/flexudy-client-java)
[![codecov](https://codecov.io/gh/flexudy/flexudy-client-java/branch/master/graph/badge.svg)](https://codecov.io/gh/flexudy/flexudy-client-java) 
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/dwyl/esta/issues)

The official [Flexudy](https://flexudy.com) Java client library.

## Installation

### Requirements

- Java 1.9 or later

### Gradle users

Add this dependency to your project's build file:

```groovy
implementation "com.flexudy.education:flexudy-client:1.0"
```

### Maven users

Add this dependency to your project's POM:

```xml
<dependency>
  <groupId>com.flexudy.education</groupId>
  <artifactId>flexudy-client</artifactId>
  <version>1.0</version>
</dependency>
```

### Others

You'll need to manually install the following JARs:

- The Flexudy Client JAR from [Official Releases Page](https://github.com/flexudy/flexudy-client-java/releases/latest)

## Documentation

Please see the [REST API Docs - Production](https://developers.flexudy.com) or [REST API Docs - Sandbox](https://developers-sandbox.flexudy.com) for the most
up-to-date documentation.

## Usage
The main interface to the API is via the `FlexudyClient` Facade Object as shown below.

### Configuring Environment
By default the Gateway connects to the `PRODUCTION` server at [Gateway API Server - Production](https://gateway.flexudy.com) In order, to
switch to another environment set it during the `FlexudyClient` object construction.

### Configuring Timeouts

Connect, write and read timeouts can be configured using the HttpClientConfig object when creating the Gateway client 
object. It would default to the following if no custom client config is provided:
- Connect timeout: **1 minute**
- Write timeout: **3 minutes**
- Read timeout: **3 minutes**

```java

import com.flexudy.education.client.service.FlexudyClient;
import com.flexudy.education.client.service.HttpClientConfig;
import com.flexudy.education.client.service.network.Environment;

public class Driver {

    public static void main(String[] args) {
        final HttpClientConfig httpConfig = HttpClientConfig.builder()
                                                            .connectTimeoutSeconds(60)
                                                            .readTimeoutSeconds(30)
                                                            .writeTimeoutSeconds(20)
                                                            .build();

        final FlexudyClient client = FlexudyClient.builder().licenseKey(licenseKey)
                                                            .httpClientConfig(httpConfig)
                                                            .environment(Environment.PRODUCTION)
                                                            .build();

        
    }
}

```

FlexudyClientExample.java

```java

import com.flexudy.education.client.data.common.AsyncRequestData;
import com.flexudy.education.client.data.common.CommonRequestData;
import com.flexudy.education.client.data.common.CommonRequestData.SimpleAsyncRequestData;
import com.flexudy.education.client.data.common.CommonRequestData.SimpleCommonRequestData;
import com.flexudy.education.client.data.quiz.ClozeQuestion;
import com.flexudy.education.client.data.quiz.OpenQuestion;import com.flexudy.education.client.data.quiz.WHQuestion;
import com.flexudy.education.client.data.summary.Summary;
import com.flexudy.education.client.service.FlexudyClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static com.flexudy.education.client.data.common.CommonRequestData.SimpleAsyncRequestData.fromCommonRequestData;
import static java.util.concurrent.TimeUnit.MINUTES;

public class Driver {

    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException, IOException {
        final InputStream sampleFile = new FileInputStream(new File("README.md"));
        final String sampleText = "The capital of the UAE is Abu Dhabi. The capital of France is Paris";
        final String licenseKey = "715d26f5-260a-43c9-838f-82c9664ca701";
        
        final CommonRequestData fileData = SimpleCommonRequestData.builder().files(List.of(sampleFile)).build();
        final CommonRequestData urlData = SimpleCommonRequestData.builder().contentUrls(List.of("https://flexudy.com"))
                                                                 .build();
        final AsyncRequestData textData = SimpleAsyncRequestData.builder().textContent(sampleText).build();
        
        final FlexudyClient client = FlexudyClient.builder().licenseKey(licenseKey).build();
        
        System.out.println("\n*********** Generating Cloze Questions for a URL source ***********\n");
        System.out.println(String.format("Cloze Quiz: %s", client.generateClozeQuiz(urlData)));
        
        System.out.println("\n*********** Generating WH Questions for a File source ***********\n");
        System.out.println(String.format("WH Quiz: %s", client.generateWHQuiz(fileData)));

        System.out.println("\n*********** Generating Open Questions for a URL source ***********\n");
        System.out.println(String.format("Open Quiz: %s", client.generateOpenQuiz(urlData)));
        
        System.out.println("\n*********** Generating Summary for a URL source ***********\n");
        System.out.println(client.generateSummary(urlData).getFacts());
        
        System.out.println("\n*********** Generating Cloze Questions for an Asynchronous URL Content ***********\n");
        final Future<List<ClozeQuestion>> futureClozeQuiz = client.submitClozeQuizJob(fromCommonRequestData(urlData));
        System.out.println(String.format("Async Cloze Quiz: %s", futureClozeQuiz.get(5, MINUTES)));
        
        System.out.println("\n*********** Generating WH Questions for an Asynchronous File Content ***********\n");
        final Future<List<WHQuestion>> futureWHQuestions = client.submitWHQuizJob(fromCommonRequestData(urlData));
        System.out.println(String.format("Async WH Quiz: %s", futureWHQuestions.get(5, MINUTES)));

        System.out.println("\n*********** Generating Open Questions for an Asynchronous File Content ***********\n");
        final Future<List<OpenQuestion>> futureOpenQuestions = client.submitOpenQuizJob(fromCommonRequestData(urlData));
        System.out.println(String.format("Async Open Quiz: %s", futureOpenQuestions.get(5, MINUTES)));
        
        System.out.println("\n*********** Generating Summary for an Asynchronous Text source  ***********\n");
        final Future<Summary> futureSummary = client.submitSummaryJob(fromCommonRequestData(textData));
        System.out.println("Async Summary: " + futureSummary.get(5, MINUTES).getFacts());
        
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
mvn test
```

The library uses [Project Lombok][lombok]. While it is not a requirement, you
might want to install a [plugin][lombok-plugins] for your favorite IDE to
facilitate development.

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