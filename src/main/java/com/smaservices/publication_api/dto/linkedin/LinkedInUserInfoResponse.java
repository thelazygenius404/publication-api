package com.smaservices.publication_api.dto.linkedin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LinkedInUserInfoResponse(
        String sub
) {
}