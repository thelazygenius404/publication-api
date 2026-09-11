package com.smaservices.publication_api.dto.linkedin;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LinkedInAuthorizationUrlResponse(
        @JsonProperty("authorization_url")
        String authorizationUrl
) {
}