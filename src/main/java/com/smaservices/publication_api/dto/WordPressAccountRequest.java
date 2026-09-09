package com.smaservices.publication_api.dto;

public class WordPressAccountRequest {
    private String wpUsername;
    private String wpAppPassword;

    public String getWpUsername() { return wpUsername; }
    public void setWpUsername(String wpUsername) { this.wpUsername = wpUsername; }
    public String getWpAppPassword() { return wpAppPassword; }
    public void setWpAppPassword(String wpAppPassword) { this.wpAppPassword = wpAppPassword; }
}