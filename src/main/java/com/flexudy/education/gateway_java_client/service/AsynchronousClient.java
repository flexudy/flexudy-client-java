package com.flexudy.education.gateway_java_client.service;

import com.flexudy.education.gateway_java_client.data.quiz.ClozeQuestion;
import com.flexudy.education.gateway_java_client.data.common.QuizRequest;
import com.flexudy.education.gateway_java_client.data.summary.Summary;
import com.flexudy.education.gateway_java_client.data.summary.SummaryRequest;

import java.util.List;
import java.util.concurrent.Future;

public interface AsynchronousClient {
    Future<List<ClozeQuestion>> submitClozeQuestionJob(QuizRequest quizRequest);
    Future<Summary> submitSummaryJob(SummaryRequest summaryRequest);
}
