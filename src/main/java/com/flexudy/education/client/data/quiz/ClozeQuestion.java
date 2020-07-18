package com.flexudy.education.client.data.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class ClozeQuestion {

    private static final String DEFAULT_BLANK_SYMBOL = "______";
    private static final Pattern QUESTION_URN_PATTERN = Pattern.compile("(@_)((?!_@|@_).)*(_@)");
    private static final String ANSWER_URN_BEGIN_PATTERN = "@_";
    private static final String ANSWER_URN_END_PATTERN = "_@";
    private static final Pattern WRAP_TEXT_PATTERN = Pattern.compile("(-\n).");
    private static final Pattern NEXT_LINE_PATTERN = Pattern.compile("\n");
    private static final String SPACE_REPLACER = ":#SPACE#:";
    private static final String BEGIN_TAG_REPLACER = " :B-TAG#";
    private static final String END_TAG_REPLACER = " ";

    @VisibleForTesting
    @Getter(value = AccessLevel.PACKAGE)
    @JsonProperty
    private String questionAnswer;

    public String getQuestion(String blankSymbol) {
        String displayQuestion = QUESTION_URN_PATTERN.matcher(questionAnswer)
                                                     .replaceAll(Optional.ofNullable(blankSymbol)
                                                     .orElse(DEFAULT_BLANK_SYMBOL));
        return getNormalizedText(displayQuestion);
    }

    public String getQuestion() {
        return getQuestion(DEFAULT_BLANK_SYMBOL);
    }

    public String getAnswer() {
        final Matcher matcher = QUESTION_URN_PATTERN.matcher(questionAnswer);
        String answer = StringUtils.EMPTY;
        if (matcher.find()) {
            final  int start = matcher.start();
            final int end = matcher.end();
            answer = questionAnswer.substring(start, end);
            answer = answer.replace(ANSWER_URN_BEGIN_PATTERN, StringUtils.EMPTY);
            answer = answer.replace(ANSWER_URN_END_PATTERN, StringUtils.EMPTY);
            return answer;
        }
        return answer;
    }

    private String getNormalizedText(String text) {
        text = WRAP_TEXT_PATTERN.matcher(text).replaceAll(StringUtils.EMPTY);
        return StringUtils.normalizeSpace(NEXT_LINE_PATTERN.matcher(text).replaceAll(StringUtils.SPACE));
    }

}
