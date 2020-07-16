package com.flexudy.education.gateway_java_client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexudy.education.gateway_java_client.data.common.AsyncRequestData;
import com.flexudy.education.gateway_java_client.data.common.CommonRequestData;
import com.flexudy.education.gateway_java_client.data.quiz.WHQuestion;
import com.flexudy.education.gateway_java_client.service.network.Environment;
import com.flexudy.education.gateway_java_client.service.network.HostResolver;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.flexudy.education.gateway_java_client.data.quiz.ClozeQuestion;
import com.flexudy.education.gateway_java_client.data.common.JobId;
import com.flexudy.education.gateway_java_client.data.summary.Summary;
import com.google.common.base.Throwables;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static okhttp3.RequestBody.create;

@Slf4j
public class FlexudyGatewayClient implements SynchronousClient, AsynchronousClient {

    private static final int HTTP_UN_AUTHORIZED_STATUS_CODE = 401;
    private static final int HTTP_FORBIDDEN_STATUS_CODE = 403;
    private static final int HTTP_NOT_FOUND_STATUS_CODE = 404;

    private static final String HTTP_UN_AUTHORIZED_MESSAGE = "Please check your license key is valid";
    private static final String HTTP_FORBIDDEN_MESSAGE = "Please check your license key is authorized to make this " +
            "request (wallet balance or active subscription)";

    private static final String CLOZE_QUIZ_API_PATH = "/api/v1/cloze-quiz/generate";
    private static final String WH_QUIZ_API_PATH = "/api/v1/wh-quiz/generate";
    private static final String SUMMARY_API_PATH = "/api/v1/summary/generate";

    private static final String CLOZE_QUIZ_JOB_API_PATH = "/api/v1/cloze-quiz/queue";
    private static final String WH_QUIZ_JOB_API_PATH = "/api/v1/wh-quiz/queue";
    private static final String SUMMARY_JOB_API_PATH = "/api/v1/summary/queue";

    private static final String CLOZE_QUIZ_JOB_RESULTS_API_PATH = CLOZE_QUIZ_JOB_API_PATH + "/results";
    private static final String WH_QUIZ_JOB_RESULTS_API_PATH = WH_QUIZ_JOB_API_PATH + "/results";
    private static final String SUMMARY_JOB_RESULTS_API_PATH = SUMMARY_JOB_API_PATH + "/results";

    private static final String LICENSE_KEY_HEADER_PARAM = "licenseKey";
    private static final String RAW_FILE_PARAM = "file";
    private static final String FILE_URL_PARAM = "url";
    private static final String WEB_HOOK_URL_PARAM = "webHookUrl";
    private static final String TEXT_CONTENT_PARAM = "textContent";
    private static final String JOB_ID_PARAM = "jobId";
    private static final String CONTENT_TYPE_PARAM = "contentType";

    private final String licenseKey;
    private final Environment environment;
    private final HttpClientConfig httpClientConfig;
    @Getter(value = AccessLevel.PRIVATE)
    private final ObjectMapper objectMapper;

    @Builder
    public FlexudyGatewayClient(@NonNull String licenseKey,
                                Environment environment,
                                HttpClientConfig httpClientConfig,
                                ObjectMapper objectMapper) {
        this.licenseKey = licenseKey;
        this.environment = Optional.ofNullable(environment).orElse(Environment.PRODUCTION);
        this.httpClientConfig = Optional.ofNullable(httpClientConfig).orElse(HttpClientConfig.builder().build());
        this.objectMapper = Optional.ofNullable(objectMapper).orElse(newObjectMapper());
    }

    @Override
    public List<ClozeQuestion> generateClozeQuiz(@NonNull CommonRequestData quizRequest) {
        validateCommonParameters(quizRequest);
        log.debug("Preparing to generate Cloze questions");
        final Request request = new Request.Builder().url(resolveApiUrl(CLOZE_QUIZ_API_PATH).toString())
                                                     .header(LICENSE_KEY_HEADER_PARAM, licenseKey)
                                                     .post(resolveCommonDataRequestBodyBuilder(quizRequest).build()).build();
        return generateContent(request, jsonToClozeQuestionsFunction);
    }

    @Override
    public List<WHQuestion> generateWHQuiz(CommonRequestData quizRequest) {
        validateCommonParameters(quizRequest);
        log.debug("Preparing to generate WH questions");
        final Request request = new Request.Builder().url(resolveApiUrl(WH_QUIZ_API_PATH).toString())
                                                     .header(LICENSE_KEY_HEADER_PARAM, licenseKey)
                                                    .post(resolveCommonDataRequestBodyBuilder(quizRequest).build()).build();
        return generateContent(request, jsonToWHQuestionsFunction);
    }

    @Override
    public Summary generateSummary(@NonNull CommonRequestData summaryRequest) {
        validateCommonParameters(summaryRequest);
        log.debug("Preparing to generate summary");
        final Request request = new Request.Builder().header(LICENSE_KEY_HEADER_PARAM, licenseKey)
                                                     .url(resolveApiUrl(SUMMARY_API_PATH).toString())
                                           .post(resolveCommonDataRequestBodyBuilder(summaryRequest).build()).build();
        return generateContent(request, jsonToSummaryMapperFunction);
    }

    @Override
    public Future<List<ClozeQuestion>> submitClozeQuizJob(@NonNull AsyncRequestData quizRequest) {
        final JobId submittedJob = submitQuestionJobRequest(quizRequest, CLOZE_QUIZ_JOB_API_PATH);
        return (Future<List<ClozeQuestion>>) pollJobResult(quizRequest.getJobPollingWaitInterval(), submittedJob.getJobId(),
                                                           id -> pollClozeQuizResults(id));
    }

    @Override
    public Future<List<WHQuestion>> submitWHQuizJob(AsyncRequestData quizRequest) {
        final JobId submittedJob = submitQuestionJobRequest(quizRequest, WH_QUIZ_JOB_API_PATH);
        return (Future<List<WHQuestion>>) pollJobResult(quizRequest.getJobPollingWaitInterval(),
                                                        submittedJob.getJobId(),
                                                        id -> pollWHQuizResults(id));
    }

    @Override
    public Future<Summary> submitSummaryJob(@NonNull AsyncRequestData summaryRequest) {
        return (Future<Summary>) pollJobResult(summaryRequest.getJobPollingWaitInterval(),
                                               submitSummaryJobRequest(summaryRequest).getJobId(),
                                               id -> pollSummaryResults(id));
    }

    public Future<?> pollJobResult(int sleepSeconds, String jobId, Function<String, Optional<?>> pollHandler) {
        return Executors.newSingleThreadExecutor().submit(() -> {
            Optional<?> result;
            try {
                do {
                    log.debug("Waiting for {} before polling job result from server", sleepSeconds);
                    Thread.sleep(TimeUnit.SECONDS.toMillis(sleepSeconds));
                    log.debug("Polling job results from server....");
                    result = pollHandler.apply(jobId);
                } while (!result.isPresent());
            }  catch (InterruptedException ex) {
                throw new IllegalStateException(Throwables.getStackTraceAsString(ex));
            }
            return result.get();
        });
    }

    private JobId submitQuestionJobRequest(AsyncRequestData quizRequest, String apiPath) {
        validateCommonParameters(quizRequest);
        log.debug("Preparing to submit question generation request");
        return generateContent(new Request.Builder().header(LICENSE_KEY_HEADER_PARAM, licenseKey)
                                                    .url(resolveApiUrl(apiPath).toString())
                                                    .post(resolveAsyncDataRequestBodyBuilder(quizRequest).build()).build(),
                               jsonToJobIdFunction);
    }

    private JobId submitSummaryJobRequest(AsyncRequestData summaryRequest) {
        validateCommonParameters(summaryRequest);
        log.debug("Preparing to submit summary generation request");
        return generateContent(new Request.Builder().header(LICENSE_KEY_HEADER_PARAM, licenseKey)
                                                    .url(resolveApiUrl(SUMMARY_JOB_API_PATH).toString())
                                                    .post(resolveAsyncDataRequestBodyBuilder(summaryRequest).build()).build(),
                               jsonToJobIdFunction);
    }

    private Optional<List<ClozeQuestion>> pollClozeQuizResults(String jobId) {
        return (Optional<List<ClozeQuestion>>) retrieveJobResult(CLOZE_QUIZ_JOB_RESULTS_API_PATH, jobId, jsonToClozeQuestionsFunction);
    }

    private Optional<List<ClozeQuestion>> pollWHQuizResults(String jobId) {
        return (Optional<List<ClozeQuestion>>) retrieveJobResult(WH_QUIZ_JOB_RESULTS_API_PATH, jobId, jsonToWHQuestionsFunction);
    }

    private Optional<Summary> pollSummaryResults(String jobId) {
        return (Optional<Summary>) retrieveJobResult(SUMMARY_JOB_RESULTS_API_PATH, jobId, jsonToSummaryMapperFunction);
    }

    private Function<String, List<ClozeQuestion>> jsonToClozeQuestionsFunction = (rawJson) -> {
        try {
            return getObjectMapper().readValue(rawJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    };

    private Function<String, List<WHQuestion>> jsonToWHQuestionsFunction = (rawJson) -> {
        try {
            return getObjectMapper().readValue(rawJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    };

    private Function<String, Summary> jsonToSummaryMapperFunction = (rawJson) -> {
        try {
            return getObjectMapper().readValue(rawJson, Summary.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    };

    private Function<String, JobId> jsonToJobIdFunction = (rawJson) -> {
        try {
            return getObjectMapper().readValue(rawJson, JobId.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    };

    private Optional<?> retrieveJobResult(String path, String jobId, Function<String, ?> mapperFunction) {
        final HttpUrl httpUrl = resolveApiUrl(path).addQueryParameter(JOB_ID_PARAM, jobId).build();
        try (Response response = newHttpClient().newCall(new Request.Builder().header(LICENSE_KEY_HEADER_PARAM,
                licenseKey).url(httpUrl).build()).execute()) {
            if (response.isSuccessful()) {
                return Optional.of(mapperFunction.apply(response.body().string()));
            } else if (HTTP_NOT_FOUND_STATUS_CODE == response.code()) {
                return Optional.empty();
            }
            throw new RuntimeException(String.format(getUserFriendlyErrorMessage(response)));
        } catch (IOException ex) {
            throw new IllegalStateException(Throwables.getStackTraceAsString(ex));
        }
    }

    private <T> T generateContent(Request request, Function<String, T> mapperFunction) {
        try (Response response = newHttpClient().newCall(request).execute()) {
            if (response.isSuccessful()) {
                return mapperFunction.apply(response.body().string());
            }
            throw new RuntimeException(String.format(getUserFriendlyErrorMessage(response)));
        } catch (IOException ex) {
            throw new IllegalStateException(Throwables.getStackTraceAsString(ex));
        }
    }

    private String getUserFriendlyErrorMessage(Response response) throws IOException {
        final int statusCode = response.code();
        switch (statusCode) {
            case HTTP_UN_AUTHORIZED_STATUS_CODE:
                return HTTP_UN_AUTHORIZED_MESSAGE;
            case HTTP_FORBIDDEN_STATUS_CODE:
                return HTTP_FORBIDDEN_MESSAGE;
            default:
                return String.format("An error occurred during processing, Status Code: %d, message: %s", statusCode,
                        response.body().string());
        }
    }

    private MultipartBody.Builder resolveCommonDataRequestBodyBuilder(CommonRequestData requestData) {
        final MultipartBody.Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        bodyBuilder.addFormDataPart(CONTENT_TYPE_PARAM, requestData.getContentType().name());
        requestData.getContentUrl().ifPresent((u) -> bodyBuilder.addFormDataPart(FILE_URL_PARAM, u));
        requestData.getTextContent().ifPresent((text) -> bodyBuilder.addFormDataPart(TEXT_CONTENT_PARAM, text));
        requestData.getContentInputStream().ifPresent(file -> bodyBuilder.addFormDataPart(RAW_FILE_PARAM, RAW_FILE_PARAM,
                create(parseInputStream(file))));

        return bodyBuilder;
    }

    private MultipartBody.Builder resolveAsyncDataRequestBodyBuilder(AsyncRequestData asyncRequestData) {
        final MultipartBody.Builder bodyBuilder = resolveCommonDataRequestBodyBuilder(asyncRequestData);
        asyncRequestData.getWebHookUrl().ifPresent(url -> bodyBuilder.addFormDataPart(WEB_HOOK_URL_PARAM, url));
        return bodyBuilder;
    }

    private HttpUrl.Builder resolveApiUrl(String apiPath) {
        return HttpUrl.parse(HostResolver.resolve(environment) + apiPath).newBuilder();
    }

    private void validateCommonParameters(CommonRequestData requestData) {
        if (!requestData.getContentInputStream().isPresent() && !requestData.getContentUrl().isPresent()
                && !requestData.getTextContent().isPresent()) {
            throw new IllegalArgumentException("Please set either the url, text content or content input stream");
        }
    }

    private byte[] parseInputStream(InputStream inputStream) {
        try {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException ex) {
            throw new IllegalArgumentException(Throwables.getStackTraceAsString(ex));
        }
    }

    @VisibleForTesting
    ObjectMapper newObjectMapper() {
        return new ObjectMapper();
    }

    @VisibleForTesting
    OkHttpClient newHttpClient() {
        return new OkHttpClient.Builder().writeTimeout(httpClientConfig.getWriteTimeoutSeconds(), TimeUnit.MINUTES)
                .readTimeout(httpClientConfig.getReadTimeoutSeconds(), TimeUnit.MINUTES)
                .connectTimeout(httpClientConfig.getConnectTimeoutSeconds(), TimeUnit.MINUTES)
                .build();
    }

}