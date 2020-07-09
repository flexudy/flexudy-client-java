package com.flexudy.education.gateway_java_client.data.summary;

import com.flexudy.education.gateway_java_client.data.common.CommonRequestData;
import com.flexudy.education.gateway_java_client.data.common.ContentType;
import lombok.Builder;
import lombok.Data;

import java.io.InputStream;
import java.util.Optional;

@Data
@Builder
public class SummaryRequest implements CommonRequestData {

    private InputStream inputStream;
    private String contentUrl;
    private ContentType contentType;
    private Integer jobPollingWaitInterval;

    @Override
    public ContentType getContentType() {
        return Optional.ofNullable(contentType).orElse(DEFAULT_CONTENT_TYPE);
    }

    @Override
    public Optional<InputStream> getContentInputStream() {
        return Optional.ofNullable(inputStream);
    }

    @Override
    public Optional<String> getContentUrl() {
        return Optional.ofNullable(contentUrl);
    }

    @Override
    public int getJobPollingWaitInterval() {
        return Optional.ofNullable(jobPollingWaitInterval).orElse(DEFAULT_JOB_POLL_SECONDS_WAIT);
    }

}