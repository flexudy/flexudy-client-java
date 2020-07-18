package com.flexudy.education.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexudy.education.client.data.common.AsyncRequestData;
import com.flexudy.education.client.data.common.CommonRequestData;
import com.flexudy.education.client.data.quiz.WHQuestion;
import com.flexudy.education.client.service.network.Environment;
import com.flexudy.education.client.service.network.HostResolver;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.flexudy.education.client.data.quiz.ClozeQuestion;
import com.flexudy.education.client.data.common.JobId;
import com.flexudy.education.client.data.summary.Summary;
import com.google.common.base.Throwables;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.UrlValidator;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static java.util.UUID.randomUUID;
import static okhttp3.RequestBody.create;

@Slf4j
public class FlexudyClient implements SynchronousClient, AsynchronousClient {

    @VisibleForTesting
    static final String HTTP_UN_AUTHORIZED_MESSAGE = "Please check your license key is valid";
    static final String HTTP_FORBIDDEN_MESSAGE = "Please check your license key is authorized to make this " +
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

    @VisibleForTesting
    static final String LICENSE_KEY_HEADER_PARAM = "licenseKey";
    private static final String RAW_FILES_PARAM = "files";
    private static final String FILE_URLS_PARAM = "urls";
    private static final String WEB_HOOK_URL_PARAM = "webHookUrl";
    private static final String TEXT_CONTENT_PARAM = "textContent";
    private static final String JOB_ID_PARAM = "jobId";
    private static final String CONTENT_TYPE_PARAM = "contentType";

    @Getter(value = AccessLevel.PACKAGE)
    private final String licenseKey;

    @Getter(value = AccessLevel.PACKAGE)
    private final Environment environment;

    @Getter(value = AccessLevel.PACKAGE)
    private final HttpClientConfig httpClientConfig;

    @Getter(value = AccessLevel.PACKAGE)
    private final ObjectMapper objectMapper;

    @Getter(value = AccessLevel.PACKAGE)
    private final UrlValidator urlValidator;

    @Builder
    FlexudyClient(@NonNull String licenseKey,
                  Environment environment,
                  HttpClientConfig httpClientConfig,
                  ObjectMapper objectMapper,
                  UrlValidator urlValidator) {
        this.licenseKey = licenseKey;
        this.environment = Optional.ofNullable(environment).orElse(Environment.PRODUCTION);
        this.httpClientConfig = Optional.ofNullable(httpClientConfig).orElse(HttpClientConfig.builder().build());
        this.objectMapper = Optional.ofNullable(objectMapper).orElse(newObjectMapper());
        this.urlValidator = Optional.ofNullable(urlValidator).orElse(new UrlValidator());
    }

    @Override
    public List<ClozeQuestion> generateClozeQuiz(@NonNull CommonRequestData quizRequest) {
        validateCommonParameters(quizRequest);
        log.debug("Preparing to generate Cloze questions");
        final Request request = new Request.Builder().url(resolveApiUrl(CLOZE_QUIZ_API_PATH).toString())
                                                     .header(LICENSE_KEY_HEADER_PARAM, licenseKey)
                                                     .post(resolveCommonDataRequestBodyBuilder(quizRequest).build()).build();
        return generateContent(request, jsonToClozeQuestionsParser);
    }

    @Override
    public List<WHQuestion> generateWHQuiz(@NonNull CommonRequestData quizRequest) {
        validateCommonParameters(quizRequest);
        log.debug("Preparing to generate WH questions");
        final Request request = new Request.Builder().url(resolveApiUrl(WH_QUIZ_API_PATH).toString())
                                                     .header(LICENSE_KEY_HEADER_PARAM, licenseKey)
                                                    .post(resolveCommonDataRequestBodyBuilder(quizRequest).build()).build();
        return generateContent(request, jsonToWHQuestionsParser);
    }

    @Override
    public Summary generateSummary(@NonNull CommonRequestData summaryRequest) {
        validateCommonParameters(summaryRequest);
        log.debug("Preparing to generate summary");
        final Request request = new Request.Builder().header(LICENSE_KEY_HEADER_PARAM, licenseKey)
                                                     .url(resolveApiUrl(SUMMARY_API_PATH).toString())
                                           .post(resolveCommonDataRequestBodyBuilder(summaryRequest).build()).build();
        return generateContent(request, jsonToSummaryParser);
    }

    @Override
    public Future<List<ClozeQuestion>> submitClozeQuizJob(@NonNull AsyncRequestData quizRequest) {
        final JobId submittedJob = submitQuestionJobRequest(quizRequest, CLOZE_QUIZ_JOB_API_PATH);
        return (Future<List<ClozeQuestion>>) pollJobResult(quizRequest.getJobPollingWaitInterval(),
                                                           submittedJob.getJobId(),
                                                           id -> pollClozeQuizResults(id));
    }

    @Override
    public Future<List<WHQuestion>> submitWHQuizJob(@NonNull AsyncRequestData quizRequest) {
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

    private Future<?> pollJobResult(int sleepSeconds, String jobId, Function<String, Optional<?>> pollHandler) {
        return Executors.newSingleThreadExecutor().submit(() -> {
            Optional<?> result;
            try {
                do {
                    log.debug("Waiting for {} seconds before polling job result from server", sleepSeconds);
                    sleep(sleepSeconds);
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
        return (Optional<List<ClozeQuestion>>) retrieveJobResult(CLOZE_QUIZ_JOB_RESULTS_API_PATH, jobId, jsonToClozeQuestionsParser);
    }

    private Optional<List<ClozeQuestion>> pollWHQuizResults(String jobId) {
        return (Optional<List<ClozeQuestion>>) retrieveJobResult(WH_QUIZ_JOB_RESULTS_API_PATH, jobId, jsonToWHQuestionsParser);
    }

    private Optional<Summary> pollSummaryResults(String jobId) {
        return (Optional<Summary>) retrieveJobResult(SUMMARY_JOB_RESULTS_API_PATH, jobId, jsonToSummaryParser);
    }

    private Function<String, List<ClozeQuestion>> jsonToClozeQuestionsParser = (rawJson) -> {
        try {
            return getObjectMapper().readValue(rawJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    };

    private Function<String, List<WHQuestion>> jsonToWHQuestionsParser = (rawJson) -> {
        try {
            return getObjectMapper().readValue(rawJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    };

    private Function<String, Summary> jsonToSummaryParser = (rawJson) -> {
        try {
            return getObjectMapper().readValue(rawJson, Summary.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    };

    private Function<String, JobId> jsonToJobIdFunction = (rawJson) -> {
        try {
            return getObjectMapper().readValue(rawJson, JobId.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    };

    private Optional<?> retrieveJobResult(String path, String jobId, Function<String, ?> mapperFunction) {
        log.debug("Retrieving results for job {}", jobId);
        final HttpUrl httpUrl = resolveApiUrl(path).addQueryParameter(JOB_ID_PARAM, jobId).build();
        try (Response response = newHttpClient().newCall(new Request.Builder().header(LICENSE_KEY_HEADER_PARAM,
                licenseKey).url(httpUrl).build()).execute()) {
            if (response.isSuccessful()) {
                log.debug("Successfully retrieved results for job {}", jobId);
                return Optional.of(mapperFunction.apply(response.body().string()));
            } else if (HttpStatus.SC_NOT_FOUND == response.code()) {
                log.debug("Results for job {} is not found/unavailable", jobId);
                return Optional.empty();
            }
            log.debug("Failed to retrieve results for job {} due to {} status code", jobId, response.code());
            throw new RuntimeException(String.format(getUserFriendlyErrorMessage(response)));
        } catch (IOException ex) {
            log.debug("An error occurred when executing the job results retrieval HTTP call", ex);
            throw new IllegalStateException(Throwables.getStackTraceAsString(ex));
        }
    }

    private <T> T generateContent(Request request, Function<String, T> mapperFunction) {
        try (Response response = newHttpClient().newCall(request).execute()) {
            if (response.isSuccessful()) {
                log.debug("Successfully generate content for request {}", request);
                return mapperFunction.apply(response.body().string());
            }
            log.debug("Failed to generate content due to {} status code", response.code());
            throw new RuntimeException(String.format(getUserFriendlyErrorMessage(response)));
        } catch (IOException ex) {
            log.debug("An error occurred when executing the job results retrieval HTTP call", ex);
            throw new IllegalStateException(Throwables.getStackTraceAsString(ex));
        }
    }

    private String getUserFriendlyErrorMessage(Response response) throws IOException {
        final int statusCode = response.code();
        switch (statusCode) {
            case HttpStatus.SC_UNAUTHORIZED:
                return HTTP_UN_AUTHORIZED_MESSAGE;
            case HttpStatus.SC_FORBIDDEN:
                return HTTP_FORBIDDEN_MESSAGE;
            default:
                return String.format("An error occurred during processing, Status Code: %d, message: %s", statusCode,
                        response.body().string());
        }
    }

    private MultipartBody.Builder resolveCommonDataRequestBodyBuilder(CommonRequestData requestData) {
        final MultipartBody.Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        final List<String> contentUrls = requestData.getContentUrls();
        final List<InputStream> files = requestData.getFiles();

        bodyBuilder.addFormDataPart(CONTENT_TYPE_PARAM, requestData.getContentType().name());
        if (CollectionUtils.isNotEmpty(contentUrls)) {
            contentUrls.forEach(u -> {
                if (!urlValidator.isValid(u)) throw new IllegalArgumentException(String.format("%s is not a valid URL", u));
            });
            bodyBuilder.addFormDataPart(FILE_URLS_PARAM, String.join(",", contentUrls));
        }
        requestData.getTextContent().ifPresent((text) -> bodyBuilder.addFormDataPart(TEXT_CONTENT_PARAM, text));

        if (CollectionUtils.isNotEmpty(files)) {
            files.forEach(file -> bodyBuilder.addFormDataPart(RAW_FILES_PARAM,
                                                              randomUUID().toString(),
                                                              create(parseInputStream(file))));
        }

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
        if (CollectionUtils.isEmpty(requestData.getFiles()) && CollectionUtils.isEmpty(requestData.getContentUrls())
                && !requestData.getTextContent().isPresent()) {
            throw new IllegalArgumentException("Please set either the url, text content or content input stream");
        }
    }

    private byte[] parseInputStream(InputStream inputStream) {
        try {
            return toByteArray(inputStream);
        } catch (IOException ex) {
            throw new IllegalArgumentException(Throwables.getStackTraceAsString(ex));
        }
    }

    @VisibleForTesting
    byte[] toByteArray(InputStream inputStream) throws IOException {
        return IOUtils.toByteArray(inputStream);
    }

    @VisibleForTesting
    ObjectMapper newObjectMapper() {
        return new ObjectMapper();
    }

    @VisibleForTesting
    OkHttpClient newHttpClient() {
        return new OkHttpClient.Builder().writeTimeout(httpClientConfig.getWriteTimeoutSeconds(), TimeUnit.SECONDS)
                                         .readTimeout(httpClientConfig.getReadTimeoutSeconds(), TimeUnit.SECONDS)
                                         .connectTimeout(httpClientConfig.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                                         .build();
    }

    @VisibleForTesting
    void sleep(int sleepSeconds) throws InterruptedException {
        Thread.sleep(TimeUnit.SECONDS.toMillis(sleepSeconds));
    }

}