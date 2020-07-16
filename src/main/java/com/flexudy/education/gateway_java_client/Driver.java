package com.flexudy.education.gateway_java_client;

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
        final FlexudyGatewayClient client = FlexudyGatewayClient.builder().licenseKey(licenseKey).build();

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
