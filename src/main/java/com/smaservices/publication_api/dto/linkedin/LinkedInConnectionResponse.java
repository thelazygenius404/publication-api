package com.smaservices.publication_api.dto.linkedin;

import java.time.Instant;

public record LinkedInConnectionResponse(
        boolean connected,
        String provider,
        String memberId,
        Instant accessTokenExpiresAt
) {
}