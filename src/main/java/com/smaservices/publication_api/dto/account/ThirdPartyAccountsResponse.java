package com.smaservices.publication_api.dto.account;

public record ThirdPartyAccountsResponse(
        AccountProviderStatusResponse wordpress,
        AccountProviderStatusResponse linkedin
) {
}