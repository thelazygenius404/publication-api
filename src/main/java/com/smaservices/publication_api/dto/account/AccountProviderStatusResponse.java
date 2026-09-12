package com.smaservices.publication_api.dto.account;

import java.time.Instant;

public record AccountProviderStatusResponse(
        boolean connected,
        AccountConnectionState status,
        String siteUrl,
        Instant accessTokenExpiresAt
) {
}