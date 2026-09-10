package com.smaservices.publication_api.dto;

import jakarta.validation.constraints.NotBlank;

public class WordPressAccountRequest {

    @NotBlank
    private String siteUrl;

    @NotBlank
    private String wpUsername;

    @NotBlank
    private String wpAppPassword;

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(
            String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public String getWpUsername() {
        return wpUsername;
    }

    public void setWpUsername(
            String wpUsername) {
        this.wpUsername = wpUsername;
    }

    public String getWpAppPassword() {
        return wpAppPassword;
    }

    public void setWpAppPassword(
            String wpAppPassword) {
        this.wpAppPassword =
                wpAppPassword;
    }
}