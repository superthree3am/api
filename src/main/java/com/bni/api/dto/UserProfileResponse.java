package com.bni.api.dto;

public class UserProfileResponse {
    private String username;
    private String email;
    private String phone;
    private String fullName; // Sesuai dengan nama kolom full_name di entity User

    public UserProfileResponse() {
    }

    public UserProfileResponse(String username, String email, String phone, String fullName) {
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.fullName = fullName;
    }

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}