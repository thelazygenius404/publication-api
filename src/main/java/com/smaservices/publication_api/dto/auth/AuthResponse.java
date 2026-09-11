package com.smaservices.publication_api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthResponse(
        @JsonProperty("access_token")
        String accessToken
) {
}