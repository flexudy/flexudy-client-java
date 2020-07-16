package com.flexudy.education.gateway_java_client.data.common;

import java.util.Optional;

public interface AsyncRequestData extends CommonRequestData {
    int DEFAULT_JOB_POLL_SECONDS_WAIT = 10;
    int getJobPollingWaitInterval();
    Optional<String> getWebHookUrl();
}
