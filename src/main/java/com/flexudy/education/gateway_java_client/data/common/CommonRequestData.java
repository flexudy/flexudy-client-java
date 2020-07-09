package com.flexudy.education.gateway_java_client.data.common;

import java.io.InputStream;
import java.util.Optional;

public interface CommonRequestData {

    ContentType DEFAULT_CONTENT_TYPE = ContentType.BOOK;
    int DEFAULT_JOB_POLL_SECONDS_WAIT = 10;

    Optional<InputStream> getContentInputStream();
    Optional<String> getContentUrl();
    ContentType getContentType();
    int getJobPollingWaitInterval();
}
