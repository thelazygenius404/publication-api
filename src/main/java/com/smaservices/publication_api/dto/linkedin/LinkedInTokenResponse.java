package com.smaservices.publication_api.dto.linkedin;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LinkedInTokenResponse(

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("expires_in")
        Long expiresIn,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("refresh_token_expires_in")
        Long refreshTokenExpiresIn,

        String scope,

        @JsonProperty("token_type")
        String tokenType
) {
}