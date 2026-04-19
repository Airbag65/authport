package com.port.auth;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

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
            PreparedStatement stmt = this.connection.prepareStatement("SELECT * FROM user WHERE email = ?");
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
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
            this.statement.execute("BEGIN TRANSACTION");
            PreparedStatement stmt = this.connection.prepareStatement(
                    "INSERT INTO user(email, password, name, surname, logged_in_count) values(?,?,?,?,0)");
            stmt.setString(1, email);
            stmt.setString(2, password);
            stmt.setString(3, name);
            stmt.setString(4, surname);
            stmt.execute();
            this.statement.execute("COMMIT");
            return true;
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            return false;
        }
    }

    public boolean userIsLoggedIn(String userToken, String ipAddr, String clientIdentifier) {
        try {
            PreparedStatement stmt = this.connection.prepareStatement(
                    "SELECT * FROM auth_key WHERE user_token = ? AND ip_addr = ? AND client_identifier = ?");
            stmt.setString(1, userToken);
            stmt.setString(2, ipAddr);
            stmt.setString(3, clientIdentifier);
            ResultSet rs = stmt.executeQuery();
            AuthKey authKey = null;
            while (rs.next()) {
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
        if (!firstTimeLogin && this.userIsLoggedIn(userToken, ipAddr, clientIdentifier)) {
            return new LoginResult("", LoginStatus.ALREADY_LOGGED_IN);
        }
        String clientToken = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        long time = ZonedDateTime.now().plusMonths(1).toEpochSecond();
        try {
            this.statement.execute("BEGIN TRANSACTION");
            PreparedStatement stmt = this.connection
                    .prepareStatement("UPDATE user SET auth_token = ?, logged_in_count = ? WHERE email = ?");
            user.get().incrementLoggedInCount();
            stmt.setString(1, userToken);
            stmt.setInt(2, user.get().getLoggedInCount());
            stmt.setString(3, user.get().getEmail());
            stmt.execute();
            stmt = this.connection.prepareStatement(
                    "INSERT INTO auth_key(ip_addr, user_token, client_token, token_expiry_date, client_identifier) values(?,?,?,"
                            + time + ",?)");
            stmt.setString(1, ipAddr);
            stmt.setString(2, userToken);
            stmt.setString(3, clientToken);
            stmt.setString(4, clientIdentifier);
            stmt.execute();
            this.statement.execute("COMMIT");
        } catch (SQLException e) {
            System.err.println(e);
            return new LoginResult("", LoginStatus.FAILIURE);
        }
        return new LoginResult(userToken + "+" + clientToken, LoginStatus.SUCCESS);
    }

    public boolean signOutUser(String email, String clientIdentifier, String ipAddr) {
        try {
            Optional<User> user = this.getUserWithEmail(email);
            this.statement.execute("BEGIN TRANSACTION");
            PreparedStatement stmt = this.connection.prepareStatement(
                    "DELETE FROM auth_key WHERE ip_addr = ? AND user_token = ? AND client_identifier = ?");
            stmt.setString(1, ipAddr);
            stmt.setString(2, user.get().getAuthToken());
            stmt.setString(3, clientIdentifier);
            stmt.execute();
            user.get().decrementLoggedInCount();
            if (user.get().getLoggedInCount() == 0) {
                System.out.println("Complete log out");
                stmt = this.connection
                        .prepareStatement("UPDATE user SET auth_token = '', logged_in_count = 0 WHERE email = ?");
            } else {
                System.out.println("Partial log out");
                stmt = this.connection.prepareStatement(
                        "UPDATE user SET logged_in_count = " + user.get().getLoggedInCount() + " WHERE email = ?");
            }
            stmt.setString(1, email);
            stmt.execute();
            this.statement.execute("COMMIT");
            return true;

        } catch (SQLException e) {
            System.err.println(e);
            return false;
        }
    }

    public boolean validateUserToken(String auth_token, String email) {
        String[] tokens = auth_token.split(Pattern.quote("+"));
        Optional<User> user = this.getUserWithEmail(email);
        if (!user.get().getAuthToken().equals(tokens[0])) {
            return false;
        }
        AuthKey ak = null;
        try {
            String getClientToken = "SELECT * FROM auth_key WHERE user_token = '%s' AND client_token = '%s'";
            ResultSet rs = this.statement.executeQuery(String.format(getClientToken, tokens[0], tokens[1]));
            while (rs.next()) {
                ak = new AuthKey(rs.getInt("id"), rs.getString("ip_addr"), rs.getString("user_token"),
                        rs.getString("client_token"), rs.getInt("token_expiry_date"),
                        rs.getString("client_identifier"));
            }
        } catch (SQLException e) {
            System.err.println(e);
            return false;
        }
        if (ak == null) {
            return false;
        }

        if (!ak.getClientToken().equals(tokens[1])) {
            return false;
        }

        long currentTime = Instant.now().getEpochSecond();
        if (currentTime > ak.getTokenExpiryDate()) {
            this.signOutUser(email, ak.getClientIdentifier(), ak.getIpAddr());
            return false;
        }
        return true;
    }

    public Optional<User> getUserWithToken(String authToken) {
        try {
            String[] tokens = authToken.split(Pattern.quote("+"));
            AuthKey ak = null;
            PreparedStatement stmt = this.connection
                    .prepareStatement("SELECT * FROM auth_key WHERE user_token = ? AND client_token = ?");
            stmt.setString(1, tokens[0]);
            stmt.setString(2, tokens[1]);
            ResultSet authKeyRs = stmt.executeQuery();
            while (authKeyRs.next()) {
                ak = new AuthKey(authKeyRs.getInt("id"), authKeyRs.getString("ip_addr"),
                        authKeyRs.getString("user_token"),
                        authKeyRs.getString("client_token"), authKeyRs.getInt("token_expiry_date"),
                        authKeyRs.getString("client_identifier"));
            }
            if (ak == null) {
                return Optional.empty();
            }
            stmt = this.connection.prepareStatement("SELECT * FROM user WHERE auth_token = ?");
            stmt.setString(1, tokens[0]);
            ResultSet userRs = stmt.executeQuery();
            while (userRs.next()) {
                User user = new User(userRs.getString("email"), userRs.getString("password"), userRs.getString("name"),
                        userRs.getString("surname"), userRs.getInt("id"), userRs.getString("auth_token"),
                        userRs.getInt("logged_in_count"));
                return Optional.of(user);
            }
        } catch (SQLException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
