package com.port.auth;

import java.sql.Connection;
import java.sql.DriverManager;
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
            this.statement.execute("BEGIN TRANSACTION");
            String query = "INSERT INTO user(email, password, name, surname, logged_in_count) values('%s', '%s', '%s', '%s', 0)";
            this.statement.executeUpdate(String.format(query, email, password, name, surname));
            this.statement.execute("COMMIT");
            return true;
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            return false;
        }
    }

    public boolean userIsLoggedIn(String userToken, String ipAddr, String clientIdentifier) {
        try {
            String query = "SELECT * FROM auth_key WHERE user_token = '%s' AND ip_addr = '%s' AND client_identifier = '%s'";
            ResultSet rs = this.statement.executeQuery(String.format(query, userToken, ipAddr, clientIdentifier));
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
            String userQuery = "UPDATE user SET auth_token = '%s', logged_in_count = %d WHERE email = '%s'";
            user.get().incrementLoggedInCount();
            this.statement
                    .execute(String.format(userQuery, userToken, user.get().getLoggedInCount(), user.get().getEmail()));
            String authKeyQuery = "INSERT INTO auth_key(ip_addr, user_token, client_token, token_expiry_date, client_identifier) values('%s', '%s', '%s', "
                    + time + ", '%s')";
            this.statement.execute(String.format(authKeyQuery, ipAddr, userToken, clientToken, clientIdentifier));
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
            String removeAuthKey = "DELETE FROM auth_key WHERE ip_addr = '%s' AND user_token = '%s' AND client_identifier = '%s'";
            this.statement.execute(String.format(removeAuthKey, ipAddr, user.get().getAuthToken(), clientIdentifier));
            user.get().decrementLoggedInCount();
            String updateUser;
            if (user.get().getLoggedInCount() == 0) {
                System.out.println("Complete log out");
                updateUser = "UPDATE user SET auth_token = '', logged_in_count = 0 WHERE email = '%s'";
            } else {
                System.out.println("Partial log out");
                updateUser = "UPDATE user SET logged_in_count = " + user.get().getLoggedInCount()
                        + " WHERE email = '%s'";
            }
            this.statement.execute(String.format(updateUser, email));
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

        if (!ak.getClientToken().equals(tokens[1])){
            return false;
        }
        
        long currentTime = Instant.now().getEpochSecond();
        if (currentTime > ak.getTokenExpiryDate()) {
            this.signOutUser(email, ak.getClientIdentifier(), ak.getIpAddr());
            return false;
        }
        return true;
    }
}
