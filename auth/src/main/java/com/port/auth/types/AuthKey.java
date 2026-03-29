package com.port.auth.types;

public class AuthKey {
    private int id;
    private String ipAddr;
    private String userToken;
    private String clientToken;
    private long tokenExpiryDate;
    private String clientIdentifier;

    public AuthKey(int id, String ip, String userToken, String clientToken, long expiry, String identifier) {
        this.id = id;
        this.ipAddr = ip;
        this.userToken = userToken;
        this.clientToken = clientToken;
        this.tokenExpiryDate = expiry;
        this.clientIdentifier = identifier;
    }

    public int getId() {
        return this.id;
    }

    public String getIpAddr() {
        return this.ipAddr;
    }

    public String getUserToken() {
        return this.userToken;
    }

    public String getClientToken() {
        return this.clientToken;
    }

    public long getTokenExpiryDate() {
        return this.tokenExpiryDate;
    }

    public String getClientIdentifier() {
        return this.clientIdentifier;
    }
}
