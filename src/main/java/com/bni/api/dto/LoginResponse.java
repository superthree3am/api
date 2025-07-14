package com.bni.api.dto;

public class LoginResponse {
    
    private int status;
    private String message;
    private String token;
    private String username;

    // Constructors
    public LoginResponse() {}

    public LoginResponse(int status, String message, String token, String username) {
        this.status = status;
        this.message = message;
        this.token = token;
        this.username = username;
    }

    // Getters and setters
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}