package com.flexudy.education.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexudy.education.client.data.common.AsyncRequestData;
import com.flexudy.education.client.data.common.CommonRequestData;
import com.flexudy.education.client.data.common.CommonRequestData.SimpleAsyncRequestData;
import com.flexudy.education.client.data.common.CommonRequestData.SimpleCommonRequestData;
import com.flexudy.education.client.data.common.ContentType;
import com.flexudy.education.client.data.quiz.ClozeQuestion;
import com.flexudy.education.client.data.quiz.WHQuestion;
import com.flexudy.education.client.data.summary.Summary;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.UrlValidator;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.flexudy.education.client.data.common.CommonRequestData.SimpleAsyncRequestData.fromCommonRequestData;
import static com.flexudy.education.client.service.network.Environment.PRODUCTION;
import static com.flexudy.education.client.service.HttpClientConfig.*;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static okhttp3.ResponseBody.create;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class FlexudyClientTest {

    private static final MediaType APPLICATION_JSON_MEDIA_TYPE = MediaType.parse("application/json");
    private static final String RAW_CLOZE_QUESTION = "The capital of France is @_Paris_@";
    private static final String CUSTOM_BLANK_SYMBOL = "-------";
    private static final String CLOZE_QUESTION_DEFAULT_BLANKED = "The capital of France is ______";
    private static final String CLOZE_QUESTION_CUSTOM_BLANKED = "The capital of France is " + CUSTOM_BLANK_SYMBOL;
    private static final String CLOZE_QUESTION_ANSWER = "Paris";

    private static final String WH_QUESTION_TEXT = "What is the capital of the UAE?";
    private static final String WH_QUESTION_ANSWER = "Abu Dhabi";
    private static final String SUMMARY_FACT = "This is a summary";

    private static final String ClOZE_QUESTION_JSON_ARRAY = new JSONArray().put(new JSONObject().put("questionAnswer",
                                                                                       RAW_CLOZE_QUESTION)).toString();
    private static final String WH_QUESTION_JSON_ARRAY = new JSONArray().put(new JSONObject().put("question", WH_QUESTION_TEXT)
                                                                           .put("answer", WH_QUESTION_ANSWER)).toString();
    private static final String SUMMARY_FACTS_JSON = new JSONObject().put("summary", List.of(SUMMARY_FACT)).toString();
    private static final String JOB_ID_JSON = new JSONObject().put("jobId", 1).toString();

    private static final CommonRequestData TEXT_REQUEST_DATA = SimpleCommonRequestData.builder().textContent(RAW_CLOZE_QUESTION).build();
    private static final CommonRequestData FILE_REQUEST_DATA = SimpleCommonRequestData.builder().files(List.of(toInputStream(RAW_CLOZE_QUESTION,
                                                                                                       defaultCharset()))).build();
    private static final CommonRequestData URL_REQUEST_DATA = SimpleCommonRequestData.builder().contentUrls(List.of("https://flexudy.com")).build();
    private static final CommonRequestData IMAGE_REQUEST_DATA = SimpleCommonRequestData.builder().contentType(ContentType.IMAGE)
                                                                                       .textContent(RAW_CLOZE_QUESTION).build();

    private static final String LICENSE_KEY = "abc";

    private static final HttpClientConfig HTTP_CONFIG = builder().connectTimeoutSeconds(5L)
                                                                 .readTimeoutSeconds(5L)
                                                                 .writeTimeoutSeconds(5L).build();

    private OkHttpClient okHttpClient;
    private ObjectMapper objectMapper;
    private UrlValidator urlValidator;
    private FlexudyClient client;
    private Call call;

    @BeforeEach
    public void setUp() {
        this.client = spy(FlexudyClient.builder().licenseKey(LICENSE_KEY).build());
        this.okHttpClient = mock(OkHttpClient.class);
        this.call = mock(Call.class);
        this.objectMapper = mock(ObjectMapper.class);
        this.urlValidator = mock(UrlValidator.class);

        doReturn(okHttpClient).when(client).newHttpClient();
        when(okHttpClient.newCall(any(Request.class))).thenReturn(call);
    }

    @Test
    public void testConstructorWithCustomParameters() {
        final FlexudyClient customClient = new FlexudyClient(LICENSE_KEY, PRODUCTION, HTTP_CONFIG, objectMapper, urlValidator);

        assertThat(customClient.getLicenseKey()).isEqualTo(LICENSE_KEY);
        assertThat(customClient.getEnvironment()).isEqualTo(PRODUCTION);
        assertThat(customClient.getHttpClientConfig()).isEqualTo(HTTP_CONFIG);
        assertThat(customClient.getObjectMapper()).isEqualTo(objectMapper);
        assertThat(customClient.getUrlValidator()).isEqualTo(urlValidator);
    }

    @Test
    public void testBuilderWithCustomParameters() {
        final FlexudyClient customClient = FlexudyClient.builder().licenseKey(LICENSE_KEY).environment(PRODUCTION)
                .httpClientConfig(HTTP_CONFIG).objectMapper(objectMapper).urlValidator(urlValidator).build();

        assertThat(customClient.getLicenseKey()).isEqualTo(LICENSE_KEY);
        assertThat(customClient.getEnvironment()).isEqualTo(PRODUCTION);
        assertThat(customClient.getHttpClientConfig()).isEqualTo(HTTP_CONFIG);
        assertThat(customClient.getObjectMapper()).isEqualTo(objectMapper);
        assertThat(customClient.getUrlValidator()).isEqualTo(urlValidator);
    }

    @Test
    public void testCreateHttpClient() {
        when(client.newHttpClient()).thenCallRealMethod();
        final OkHttpClient httpClient = client.newHttpClient();

        assertThat(httpClient.connectTimeoutMillis()).isEqualTo(SECONDS.toMillis(DEFAULT_CONNECT_TIMEOUT_SECONDS));
        assertThat(httpClient.readTimeoutMillis()).isEqualTo(SECONDS.toMillis(DEFAULT_READ_TIMEOUT_SECONDS));
        assertThat(httpClient.writeTimeoutMillis()).isEqualTo(SECONDS.toMillis(DEFAULT_WRITE_TIMEOUT_SECONDS));
    }

    @Test
    public void testCreateClientWithNullLicenseKey() {
        assertThrows(NullPointerException.class, () -> new FlexudyClient(null, PRODUCTION, HTTP_CONFIG, objectMapper, urlValidator));
    }

    @Test
    public void testBuilderClientWithNullLicenseKey() {
        assertThrows(NullPointerException.class, () -> FlexudyClient.builder().licenseKey(null).build());
    }

    @Test
    public void testGenerateClozeQuizWithNullRequestData() {
        assertThrows(NullPointerException.class, () -> client.generateClozeQuiz(null));
    }

    @Test
    public void testGenerateWHQuizWithNullRequestData() {
        assertThrows(NullPointerException.class, () -> client.generateWHQuiz(null));
    }

    @Test
    public void testGenerateSummaryWithNullRequestData() {
        assertThrows(NullPointerException.class, () -> client.generateSummary(null));
    }

    @Test
    public void testSubmitClozeQuizRequestWithNullRequestData() {
        assertThrows(NullPointerException.class, () -> client.submitClozeQuizJob(null));
    }

    @Test
    public void testSubmitWHQuizRequestWithNullRequestData() {
        assertThrows(NullPointerException.class, () -> client.submitWHQuizJob(null));
    }

    @Test
    public void testSubmitSummaryWithNullRequestData() {
        assertThrows(NullPointerException.class, () -> client.submitSummaryJob(null));
    }

    @Test
    public void testGenerateClozeQuizWithNoDataSet() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> client.generateClozeQuiz(SimpleCommonRequestData.builder().build()));
        assertThat(exception.getMessage()).isEqualTo("Please set either the url, text content or content input stream");
    }

    @Test
    public void testGenerateClozeQuizWithInvalidLicenseKey() throws IOException {
        final FlexudyClient client = spy(FlexudyClient.builder().licenseKey(StringUtils.EMPTY).build());
        doReturn(okHttpClient).when(client).newHttpClient();
        when(okHttpClient.newCall(any(Request.class))).thenReturn(call);
        stubResponse(new int[]{HttpStatus.SC_UNAUTHORIZED}, StringUtils.EMPTY);
        final RuntimeException exception = assertThrows(RuntimeException.class,
                () -> client.generateClozeQuiz(FILE_REQUEST_DATA));
        assertThat(exception.getMessage()).isEqualTo(FlexudyClient.HTTP_UN_AUTHORIZED_MESSAGE);
    }

    @Test
    public void testGenerateClozeQuizWithNoSubscription() throws IOException {
        stubResponse(new int[]{HttpStatus.SC_FORBIDDEN}, StringUtils.EMPTY);
        final RuntimeException exception = assertThrows(RuntimeException.class,
                () -> client.generateClozeQuiz(FILE_REQUEST_DATA));
        assertThat(exception.getMessage()).isEqualTo(FlexudyClient.HTTP_FORBIDDEN_MESSAGE);
    }

    @Test
    public void testGenerateClozeQuizWithServerError() throws IOException {
        stubResponse(new int[]{HttpStatus.SC_INTERNAL_SERVER_ERROR}, StringUtils.EMPTY);
        final RuntimeException exception = assertThrows(RuntimeException.class,
                () -> client.generateClozeQuiz(FILE_REQUEST_DATA));
        assertThat(exception.getMessage()).isEqualTo("An error occurred during processing, Status Code: 500, message: ");
    }

    @Test
    public void testGenerateClozeQuizWithInvalidURL() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> client.generateClozeQuiz(SimpleCommonRequestData.builder().contentUrls(List.of("http")).build()));
        assertThat(exception.getMessage()).isEqualTo("http is not a valid URL");
    }

    @Test
    public void testGenerateClozeQuizWithProcessingError() throws IOException {
        when(call.execute()).thenThrow(new IOException("error"));
        assertThrows(IllegalStateException.class, () -> client.generateClozeQuiz(TEXT_REQUEST_DATA));
    }

    @Test
    public void testGenerateClozeQuizWithInvalidResponse() throws IOException {
        stubResponse(StringUtils.EMPTY);
        assertThrows(IllegalArgumentException.class, () -> client.generateClozeQuiz(FILE_REQUEST_DATA));
    }

    @Test
    public void testGenerateWHQuizWithInvalidResponse() throws IOException {
        stubResponse(StringUtils.EMPTY);
        assertThrows(IllegalArgumentException.class, () -> client.generateWHQuiz(FILE_REQUEST_DATA));
    }

    @Test
    public void testGenerateSummaryWithInvalidResponse() throws IOException {
        stubResponse(StringUtils.EMPTY);
        assertThrows(IllegalArgumentException.class, () -> client.generateSummary(FILE_REQUEST_DATA));
    }

    @Test
    public void testGenerateClozeQuiz() throws IOException {
        stubResponse(ClOZE_QUESTION_JSON_ARRAY);
        final ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);

        final List<ClozeQuestion> questions = client.generateClozeQuiz(TEXT_REQUEST_DATA);
        assertThat(questions).hasSize(1);

        final ClozeQuestion clozeQuestion = questions.get(0);
        assertThat(clozeQuestion.getQuestion()).isEqualTo(CLOZE_QUESTION_DEFAULT_BLANKED);
        assertThat(clozeQuestion.getQuestion(CUSTOM_BLANK_SYMBOL)).isEqualTo(CLOZE_QUESTION_CUSTOM_BLANKED);
        assertThat(clozeQuestion.getAnswer()).isEqualTo(CLOZE_QUESTION_ANSWER);

        verify(okHttpClient).newCall(requestArgumentCaptor.capture());
        final Request clozeQuizPostRequest = requestArgumentCaptor.getValue();
        assertThat(clozeQuizPostRequest.url().toString()).isEqualTo("https://gateway.flexudy.com/api/v1/cloze-quiz/generate");
        assertThat(clozeQuizPostRequest.header(FlexudyClient.LICENSE_KEY_HEADER_PARAM)).isEqualTo(LICENSE_KEY);
    }

    @Test
    public void testSubmitClozeQuiz() throws IOException, ExecutionException, InterruptedException {
        stubResponse(JOB_ID_JSON, ClOZE_QUESTION_JSON_ARRAY);
        final ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);

        final AsyncRequestData asyncRequestData = SimpleAsyncRequestData.builder().jobPollingWaitInterval(0)
                                                                        .textContent(UUID.randomUUID().toString())
                                                                        .webHookUrl("https://your-server/path")
                                                                        .build();
        final List<ClozeQuestion> questions = client.submitClozeQuizJob(asyncRequestData).get();
        assertThat(questions).hasSize(1);

        final ClozeQuestion clozeQuestion = questions.get(0);
        assertThat(clozeQuestion.getQuestion()).isEqualTo(CLOZE_QUESTION_DEFAULT_BLANKED);
        assertThat(clozeQuestion.getQuestion(CUSTOM_BLANK_SYMBOL)).isEqualTo(CLOZE_QUESTION_CUSTOM_BLANKED);
        assertThat(clozeQuestion.getAnswer()).isEqualTo(CLOZE_QUESTION_ANSWER);

        verify(okHttpClient, times(2)).newCall(requestArgumentCaptor.capture());
        final List<Request> clozeQuizPushPullRequests = requestArgumentCaptor.getAllValues();

        final Request submitJobRequest = clozeQuizPushPullRequests.get(0);
        assertThat(submitJobRequest.url().toString()).isEqualTo("https://gateway.flexudy.com/api/v1/cloze-quiz/queue");
        assertThat(submitJobRequest.header(FlexudyClient.LICENSE_KEY_HEADER_PARAM)).isEqualTo(LICENSE_KEY);

        final Request pollJobResultRequest = clozeQuizPushPullRequests.get(1);
        assertThat(pollJobResultRequest.url().toString()).isEqualTo("https://gateway.flexudy.com/api/v1/cloze-quiz/queue/results?jobId=1");
        assertThat(pollJobResultRequest.header(FlexudyClient.LICENSE_KEY_HEADER_PARAM)).isEqualTo(LICENSE_KEY);
    }

    @Test
    public void testSubmitClozeQuizWithBadJobIdJson() throws IOException {
        stubResponse(StringUtils.EMPTY);
        assertThrows(IllegalArgumentException.class, () -> client.submitClozeQuizJob(fromCommonRequestData(FILE_REQUEST_DATA)).get());
    }

    @Test
    public void testSubmitClozeQuizWithBadInputStream() throws IOException {
        doThrow(new IOException()).when(client).toByteArray(any(InputStream.class));
        assertThrows(IllegalArgumentException.class, () -> client.submitClozeQuizJob(fromCommonRequestData(FILE_REQUEST_DATA)).get());
    }

    @Test
    public void testSubmitClozeQuizWithRetrievalNetworkError() throws IOException {
        when(call.execute()).thenReturn(createResponse(HttpStatus.SC_CREATED, JOB_ID_JSON)).thenThrow(new IOException());
        final AsyncRequestData asyncRequestData = SimpleAsyncRequestData.builder().jobPollingWaitInterval(0)
                                                                        .textContent(UUID.randomUUID().toString())
                                                                        .build();
        final ExecutionException thrownException = assertThrows(ExecutionException.class,
                () -> client.submitClozeQuizJob(asyncRequestData).get());
        assertThat(thrownException.getCause()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testSubmitClozeQuizWithInterruptedException() throws IOException, InterruptedException {
        stubResponse(JOB_ID_JSON, ClOZE_QUESTION_JSON_ARRAY);

        final AsyncRequestData asyncRequestData = SimpleAsyncRequestData.builder().jobPollingWaitInterval(0)
                                                                        .textContent(UUID.randomUUID().toString())
                                                                        .build();
        doThrow(new InterruptedException()).when(client).sleep(0);
        final ExecutionException thrownException = assertThrows(ExecutionException.class,
                () -> client.submitClozeQuizJob(asyncRequestData).get());
        assertThat(thrownException.getCause()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testSubmitClozeQuizWithNoResult() throws IOException {
        stubResponse(new int[]{HttpStatus.SC_CREATED, HttpStatus.SC_NOT_FOUND}, JOB_ID_JSON, ClOZE_QUESTION_JSON_ARRAY);

        final ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
        final AsyncRequestData asyncRequestData = SimpleAsyncRequestData.builder().jobPollingWaitInterval(1)
                                                                        .textContent(UUID.randomUUID().toString()).build();

        assertThrows(TimeoutException.class, () -> client.submitClozeQuizJob(asyncRequestData).get( 5, SECONDS));

        verify(okHttpClient, times(5)).newCall(requestArgumentCaptor.capture());
        final List<Request> clozeQuizPushPullRequests = requestArgumentCaptor.getAllValues();

        final Request submitJobRequest = clozeQuizPushPullRequests.get(0);
        assertThat(submitJobRequest.url().toString()).isEqualTo("https://gateway.flexudy.com/api/v1/cloze-quiz/queue");
        assertThat(submitJobRequest.header(FlexudyClient.LICENSE_KEY_HEADER_PARAM)).isEqualTo(LICENSE_KEY);

        final Request pollJobResultRequest = clozeQuizPushPullRequests.get(1);
        assertThat(pollJobResultRequest.url().toString()).isEqualTo("https://gateway.flexudy.com/api/v1/cloze-quiz/queue/results?jobId=1");
        assertThat(pollJobResultRequest.header(FlexudyClient.LICENSE_KEY_HEADER_PARAM)).isEqualTo(LICENSE_KEY);
    }

    @Test
    public void testRetrieveClozeQuizResultsWithServerError() throws IOException {
        stubResponse(new int[]{HttpStatus.SC_CREATED, HttpStatus.SC_INTERNAL_SERVER_ERROR}, JOB_ID_JSON, StringUtils.EMPTY);

        final AsyncRequestData asyncRequestData = SimpleAsyncRequestData.builder().jobPollingWaitInterval(1)
                .textContent(UUID.randomUUID().toString()).build();

        final ExecutionException thrownException = assertThrows(ExecutionException.class,
                () -> client.submitClozeQuizJob(asyncRequestData).get());
        assertThat(thrownException.getCause()).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void testSubmitWHQuiz() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        stubResponse(JOB_ID_JSON, WH_QUESTION_JSON_ARRAY);
        final ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);

        final List<WHQuestion> questions = client.submitWHQuizJob(fromCommonRequestData(FILE_REQUEST_DATA)).get(1, MINUTES);
        assertThat(questions).hasSize(1);

        final WHQuestion actualQuestion = questions.get(0);
        assertThat(actualQuestion.getQuestion()).isEqualTo(WH_QUESTION_TEXT);
        assertThat(actualQuestion.getAnswer()).isEqualTo(WH_QUESTION_ANSWER);

        verify(okHttpClient, times(2)).newCall(requestArgumentCaptor.capture());
        final List<Request> WHQuizPushPullRequests = requestArgumentCaptor.getAllValues();

        final Request submitJobRequest = WHQuizPushPullRequests.get(0);
        assertThat(submitJobRequest.url().toString()).isEqualTo("https://gateway.flexudy.com/api/v1/wh-quiz/queue");
        assertThat(submitJobRequest.header(FlexudyClient.LICENSE_KEY_HEADER_PARAM)).isEqualTo(LICENSE_KEY);

        final Request pollJobResultRequest = WHQuizPushPullRequests.get(1);
        assertThat(pollJobResultRequest.url().toString()).isEqualTo("https://gateway.flexudy.com/api/v1/wh-quiz/queue/results?jobId=1");
        assertThat(pollJobResultRequest.header(FlexudyClient.LICENSE_KEY_HEADER_PARAM)).isEqualTo(LICENSE_KEY);
    }

    @Test
    public void testGenerateWHQuiz() throws IOException {
        stubResponse(WH_QUESTION_JSON_ARRAY);
        final ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);

        final List<WHQuestion> questions = client.generateWHQuiz(URL_REQUEST_DATA);
        assertThat(questions).hasSize(1);

        final WHQuestion actualQuestion = questions.get(0);
        assertThat(actualQuestion.getQuestion()).isEqualTo(WH_QUESTION_TEXT);
        assertThat(actualQuestion.getAnswer()).isEqualTo(WH_QUESTION_ANSWER);

        verify(okHttpClient).newCall(requestArgumentCaptor.capture());
        final Request whQuizPostRequest = requestArgumentCaptor.getValue();
        assertThat(whQuizPostRequest.url().toString()).isEqualTo("https://gateway.flexudy.com/api/v1/wh-quiz/generate");
        assertThat(whQuizPostRequest.header(FlexudyClient.LICENSE_KEY_HEADER_PARAM)).isEqualTo(LICENSE_KEY);
    }

    @Test
    public void testSubmitSummary() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        stubResponse(JOB_ID_JSON, SUMMARY_FACTS_JSON);
        final ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);

        final Summary summary = client.submitSummaryJob(fromCommonRequestData(IMAGE_REQUEST_DATA)).get(1, MINUTES);
        assertThat(summary.getFacts()).containsOnly(SUMMARY_FACT);

        verify(okHttpClient, times(2)).newCall(requestArgumentCaptor.capture());
        final List<Request> WHQuizPushPullRequests = requestArgumentCaptor.getAllValues();

        final Request submitJobRequest = WHQuizPushPullRequests.get(0);
        assertThat(submitJobRequest.url().toString()).isEqualTo("https://gateway.flexudy.com/api/v1/summary/queue");
        assertThat(submitJobRequest.header(FlexudyClient.LICENSE_KEY_HEADER_PARAM)).isEqualTo(LICENSE_KEY);

        final Request pollJobResultRequest = WHQuizPushPullRequests.get(1);
        assertThat(pollJobResultRequest.url().toString()).isEqualTo("https://gateway.flexudy.com/api/v1/summary/queue/results?jobId=1");
        assertThat(pollJobResultRequest.header(FlexudyClient.LICENSE_KEY_HEADER_PARAM)).isEqualTo(LICENSE_KEY);
    }

    @Test
    public void testGenerateSummary() throws IOException {
        stubResponse(SUMMARY_FACTS_JSON);
        final ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);

        final Summary summary = client.generateSummary(FILE_REQUEST_DATA);
        assertThat(summary.getFacts()).containsOnly(SUMMARY_FACT);

        verify(okHttpClient).newCall(requestArgumentCaptor.capture());
        final Request whQuizPostRequest = requestArgumentCaptor.getValue();
        assertThat(whQuizPostRequest.url().toString()).isEqualTo("https://gateway.flexudy.com/api/v1/summary/generate");
        assertThat(whQuizPostRequest.header(FlexudyClient.LICENSE_KEY_HEADER_PARAM)).isEqualTo(LICENSE_KEY);
    }

    private void stubResponse(String... expectedResponseBodies) throws IOException {
        final int[] statusCodes = new int[expectedResponseBodies.length];
        Arrays.fill(statusCodes, HttpStatus.SC_CREATED);
        stubResponse(statusCodes, expectedResponseBodies);
    }

    private void stubResponse(int[] statusCodes, String... expectedResponseBodies) throws IOException {
        final Response[] responses = new Response[expectedResponseBodies.length];
        for (int i = 0 ; i < responses.length ; i++) {
            responses[i] = createResponse(statusCodes[i], expectedResponseBodies[i]);
        }

        when(call.execute()).thenReturn(responses[0], Arrays.copyOfRange(responses, 1, responses.length));
    }

    private Response createResponse(int statusCode, String json) {
        return new Response.Builder().request(new Request.Builder().url("https://localhost").build()).code(statusCode)
                                                         .protocol(Protocol.HTTP_2).message(StringUtils.EMPTY)
                                                         .body(create(json, APPLICATION_JSON_MEDIA_TYPE)).build();
    }

}
