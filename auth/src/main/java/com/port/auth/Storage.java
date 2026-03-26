package com.port.auth;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

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
            rs.getString("surname"), rs.getInt("id"), rs.getString("auth_token"), rs.getInt("logged_in_count"));
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
}
