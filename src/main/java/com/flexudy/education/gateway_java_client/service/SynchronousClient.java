package com.flexudy.education.gateway_java_client.service;

import com.flexudy.education.gateway_java_client.data.quiz.ClozeQuestion;
import com.flexudy.education.gateway_java_client.data.common.QuizRequest;
import com.flexudy.education.gateway_java_client.data.summary.Summary;
import com.flexudy.education.gateway_java_client.data.summary.SummaryRequest;

import java.util.List;

public interface SynchronousClient {
    List<ClozeQuestion> generateClozeQuestions(QuizRequest quizRequest);
    Summary generateSummary(SummaryRequest quizRequest);
}
