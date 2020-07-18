package com.flexudy.education.client.data.quiz;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClozeQuestionTest {

    private static final String RAW_CLOZE_QUESTION = "The capital of France is @_Paris_@";
    private static final String INVALID_CLOZE_QUESTION = "The capital of France is Paris";
    private static final String CUSTOM_BLANK_SYMBOL = "-------";
    private static final String CLOZE_QUESTION_DEFAULT_BLANKED = "The capital of France is ______";
    private static final String CLOZE_QUESTION_CUSTOM_BLANKED = "The capital of France is " + CUSTOM_BLANK_SYMBOL;
    private static final String CLOZE_QUESTION_ANSWER = "Paris";

    private ClozeQuestion clozeQuestion;

    @BeforeEach
    public void beforeEach() {
        this.clozeQuestion = new ClozeQuestion(RAW_CLOZE_QUESTION);
    }

    @Test
    public void testClozeQuestionWithDefaultBlank() {
        assertThat(clozeQuestion.getQuestionAnswer()).isEqualTo(RAW_CLOZE_QUESTION);
        assertThat(clozeQuestion.getQuestion()).isEqualTo(CLOZE_QUESTION_DEFAULT_BLANKED);
        assertThat(clozeQuestion.getAnswer()).isEqualTo(CLOZE_QUESTION_ANSWER);
    }

    @Test
    public void testClozeQuestionWithCustomBlank() {
        assertThat(clozeQuestion.getQuestion(CUSTOM_BLANK_SYMBOL)).isEqualTo(CLOZE_QUESTION_CUSTOM_BLANKED);
    }

    @Test
    public void testClozeQuestionWithInvalidQuestion() {
        this.clozeQuestion = new ClozeQuestion(INVALID_CLOZE_QUESTION);
        assertThat(clozeQuestion.getQuestion()).isEqualTo(INVALID_CLOZE_QUESTION);
        assertThat(clozeQuestion.getAnswer()).isEqualTo(StringUtils.EMPTY);
    }

}
