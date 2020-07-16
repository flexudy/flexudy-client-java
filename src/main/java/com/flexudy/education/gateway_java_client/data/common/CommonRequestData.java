package com.flexudy.education.gateway_java_client.data.common;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.io.InputStream;
import java.util.Optional;

public interface CommonRequestData {

    ContentType DEFAULT_CONTENT_TYPE = ContentType.DOCUMENT;

    Optional<InputStream> getContentInputStream();
    Optional<String> getContentUrl();
    Optional<String> getTextContent();
    ContentType getContentType();

    @Data
    @SuperBuilder
    class SimpleCommonRequestData implements CommonRequestData {

        private InputStream inputStream;
        private String contentUrl;
        private String textContent;
        private ContentType contentType;

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
        public Optional<String> getTextContent() {
            return Optional.ofNullable(textContent);
        }
    }

    @Data
    @SuperBuilder
    class SimpleAsyncRequestData extends SimpleCommonRequestData implements AsyncRequestData {

        private Integer jobPollingWaitInterval;
        private String webHookUrl;

        @Override
        public Optional<String> getWebHookUrl() {
            return Optional.ofNullable(webHookUrl);
        }

        @Override
        public int getJobPollingWaitInterval() {
            return Optional.ofNullable(jobPollingWaitInterval).orElse(DEFAULT_JOB_POLL_SECONDS_WAIT);
        }
    }
}
