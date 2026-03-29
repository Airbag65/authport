package com.port.auth.types;

public class ValidateReq {
    private String auth_token;
    private String email;

    public ValidateReq(String auth_token, String email) {
        this.auth_token = auth_token;
        this.email = email;
    }

    public String getAuthToken() {
        return this.auth_token;
    }

    public String getEmail() {
        return this.email;
    }
}
