package com.port.auth;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import com.port.auth.types.AuthKey;
import com.port.auth.types.User;

public class Storage {
    private Connection connection;
    private Statement statement;

    public Storage() {
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:.db");
            this.statement = connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace(System.err);
        }
    }

    public Optional<User> getUserWithEmail(String email) {
        try {
            String query = "SELECT * FROM user WHERE email = '%s'";
            ResultSet rs = this.statement.executeQuery(String.format(query, email));
            while (rs.next()) {
                User user = new User(rs.getString("email"), rs.getString("password"), rs.getString("name"),
                        rs.getString("surname"), rs.getInt("id"), rs.getString("auth_token"),
                        rs.getInt("logged_in_count"));
                return Optional.of(user);
            }
        } catch (SQLException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    public boolean createNewUser(String email, String password, String name, String surname) {
        try {
            String query = "INSERT INTO user(email, password, name, surname, logged_in_count) values('%s', '%s', '%s', '%s', 0)";
            this.statement.executeUpdate(String.format(query, email, password, name, surname));
            return true;
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            return false;
        }
    }

    private boolean userLoggedIn(String userToken, String ipAddr) {
        try {
            String query = "SELECT * FROM auth_key WHERE user_token = '%s' AND ip_addr = '%s'";
            ResultSet rs = this.statement.executeQuery(String.format(query, userToken, ipAddr));
            AuthKey authKey = null;
            while (rs.next()) {
                System.out.println(rs);
                authKey = new AuthKey(rs.getInt("id"), rs.getString("ip_addr"), rs.getString("user_token"),
                        rs.getString("client_token"), rs.getInt("token_expiry_date"),
                        rs.getString("client_identifier"));
            }
            if (authKey == null) {
                return false;
            }

        } catch (SQLException e) {
            System.err.println(e);
            return false;
        }
        return true;
    }

    public class LoginResult {
        public String authToken;
        public LoginStatus status;

        public LoginResult(String token, LoginStatus status) {
            this.authToken = token;
            this.status = status;
        }
    }

    public static enum LoginStatus {
        SUCCESS,
        FAILIURE,
        ALREADY_LOGGED_IN
    }

    public LoginResult loginUser(String email, String password, String clientIdentifier, String ipAddr) {
        String userToken;
        boolean firstTimeLogin = true;
        Optional<User> user = this.getUserWithEmail(email);
        if (user.isEmpty()) {
            return new LoginResult("", LoginStatus.FAILIURE);
        }
        if (user.get().getAuthToken() == null || user.get().getAuthToken().equals("")) {
            userToken = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        } else {
            firstTimeLogin = false;
            userToken = user.get().getAuthToken();
        }
        if (!firstTimeLogin && this.userLoggedIn(userToken, ipAddr)) {
            return new LoginResult("", LoginStatus.ALREADY_LOGGED_IN);
        }
        String clientToken = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        long time = ZonedDateTime.now().plusMonths(1).toEpochSecond();
        try {
            String userQuery = "UPDATE user SET auth_token = '%s', logged_in_count = %d WHERE email = '%s'";
            user.get().incrementLoggedInCount();
            this.statement
                    .execute(String.format(userQuery, userToken, user.get().getLoggedInCount(), user.get().getEmail()));
            String authKeyQuery = "INSERT INTO auth_key(ip_addr, user_token, client_token, token_expiry_date, client_identifier) values('%s', '%s', '%s', "
                    + time + ", '%s')";
            this.statement.execute(String.format(authKeyQuery, ipAddr, userToken, clientToken, clientIdentifier));
        } catch (SQLException e) {
            System.err.println(e);
            return new LoginResult("", LoginStatus.FAILIURE);
        }
        return new LoginResult(userToken + "+" + clientToken, LoginStatus.SUCCESS);
    }
}
