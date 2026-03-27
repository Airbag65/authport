package com.port.auth.types;

public class LoginRes {
    private int responseCode;
    private String responseMessage;
    private String authToken;

    public LoginRes(int code, String message, String token) {
        this.responseCode = code;
        this.responseMessage = message;
        this.authToken = token;
    }

    public int getResponseCode() {
        return this.responseCode;
    }

    public String getResponseMessage() {
        return this.responseMessage;
    }

    public String getAuthToken() {
        return this.authToken;
    }

    public String toJsonString() {
        String res = "{\n";
        res += "\t\"response_code\": " + this.getResponseCode() + ",\n";
        res += "\t\"response_message\": " + "\"" + this.getResponseMessage() + "\",\n";
        res += "\t\"auth_token\": " + "\"" + this.getAuthToken() + "\"\n}";
        return res;
    }
}
