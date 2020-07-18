package com.flexudy.education.client.service;

import com.flexudy.education.client.data.common.AsyncRequestData;
import com.flexudy.education.client.data.quiz.ClozeQuestion;
import com.flexudy.education.client.data.quiz.WHQuestion;
import com.flexudy.education.client.data.summary.Summary;

import java.util.List;
import java.util.concurrent.Future;

public interface AsynchronousClient {
    Future<List<ClozeQuestion>> submitClozeQuizJob(AsyncRequestData quizRequest);
    Future<List<WHQuestion>> submitWHQuizJob(AsyncRequestData quizRequest);
    Future<Summary> submitSummaryJob(AsyncRequestData summaryRequest);
}
