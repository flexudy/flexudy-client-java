package com.flexudy.education.client.data.common;

import com.flexudy.education.client.data.common.CommonRequestData.SimpleAsyncRequestData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SimpleAsyncRequestDataTest {

    @Test
    public void testFromCommonRequestDataWithNullValue() {
        assertThrows(NullPointerException.class, () -> SimpleAsyncRequestData.fromCommonRequestData(null));
    }
}
