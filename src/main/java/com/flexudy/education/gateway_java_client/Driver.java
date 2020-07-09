package com.flexudy.education.gateway_java_client;

import com.flexudy.education.gateway_java_client.data.common.QuizRequest;
import com.flexudy.education.gateway_java_client.data.quiz.ClozeQuestion;
import com.flexudy.education.gateway_java_client.data.summary.SummaryRequest;
import com.flexudy.education.gateway_java_client.service.FlexudyGatewayClient;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Driver {

    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
        final String licenseKey = "3deaeb03-4425-435d-b04c-2ca6428f6d96";
        final FlexudyGatewayClient client = FlexudyGatewayClient.builder().licenseKey(licenseKey).build();

        System.out.println("*********** Generating Cloze Questions for a URL source ***********\n");
        final QuizRequest quizRequest = QuizRequest.builder().contentUrl("https://flexudy.com").build();
        client.generateClozeQuestions(quizRequest).forEach(q -> System.out.println(q.getQuestion() + "," + q.getAnswer()));

        System.out.println("\n*********** Generating Summary for a URL source ***********\n");
        final SummaryRequest summaryRequest = SummaryRequest.builder().contentUrl("https://forbes.com").build();
        System.out.println(client.generateSummary(summaryRequest).getSummary());

        System.out.println("\n*********** Generating Cloze Questions for a URL source Asynchronously ***********");
        final Future<List<ClozeQuestion>> futureQuestions = client.submitClozeQuestionJob(quizRequest);

        System.out.println("*********** Blocking for asynchronous job to complete ***********\n");
        futureQuestions.get(5, TimeUnit.MINUTES).forEach(q -> System.out.println(q.getQuestion() + "," + q.getAnswer()));

        System.exit(0);
    }
}
