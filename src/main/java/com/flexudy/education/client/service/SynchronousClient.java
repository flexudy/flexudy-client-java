package com.flexudy.education.client.service;

import com.flexudy.education.client.data.common.CommonRequestData;
import com.flexudy.education.client.data.quiz.ClozeQuestion;
import com.flexudy.education.client.data.quiz.WHQuestion;
import com.flexudy.education.client.data.summary.Summary;

import java.util.List;

public interface SynchronousClient {
    List<ClozeQuestion> generateClozeQuiz(CommonRequestData quizRequest);
    List<WHQuestion> generateWHQuiz(CommonRequestData quizRequest);
    Summary generateSummary(CommonRequestData quizRequest);
}
