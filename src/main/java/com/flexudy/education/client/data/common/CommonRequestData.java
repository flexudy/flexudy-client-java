package com.flexudy.education.client.data.common;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface CommonRequestData {

    ContentType DEFAULT_CONTENT_TYPE = ContentType.DOCUMENT;

    List<InputStream> getFiles();
    List<String> getContentUrls();
    Optional<String> getTextContent();
    ContentType getContentType();

    @Getter
    @SuperBuilder
    class SimpleCommonRequestData implements CommonRequestData {

        private List<InputStream> files;
        private List<String> contentUrls;
        private String textContent;
        private ContentType contentType;

        @Override
        public ContentType getContentType() {
            return Optional.ofNullable(contentType).orElse(DEFAULT_CONTENT_TYPE);
        }

        @Override
        public List<InputStream> getFiles() {
            return files;
        }

        @Override
        public List<String> getContentUrls() {
            return contentUrls;
        }

        @Override
        public Optional<String> getTextContent() {
            return Optional.ofNullable(textContent);
        }
    }

    @Getter
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

        public static SimpleAsyncRequestData fromCommonRequestData(@NonNull CommonRequestData commonRequestData) {
            return SimpleAsyncRequestData.builder().files(commonRequestData.getFiles())
                                                   .contentUrls(commonRequestData.getContentUrls())
                                                   .contentType(commonRequestData.getContentType())
                                                   .textContent(commonRequestData.getTextContent().orElse(null))
                                                   .build();
        }
    }
}
