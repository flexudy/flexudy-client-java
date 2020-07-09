package com.flexudy.education.gateway_java_client;

import com.flexudy.education.gateway_java_client.data.common.QuizRequest;
import com.flexudy.education.gateway_java_client.data.quiz.ClozeQuestion;
import com.flexudy.education.gateway_java_client.data.summary.SummaryRequest;
import com.flexudy.education.gateway_java_client.service.FlexudyGatewayClient;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Driver {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        final String licenseKey = "715d26f5-260a-43c9-838f-82c9664ca701";
        final FlexudyGatewayClient client = FlexudyGatewayClient.builder().licenseKey(licenseKey).build();

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
