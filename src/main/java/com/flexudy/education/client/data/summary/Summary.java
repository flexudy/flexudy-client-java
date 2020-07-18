package com.flexudy.education.client.data.summary;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class Summary {

    @JsonProperty("summary")
    private List<String> facts;

}
