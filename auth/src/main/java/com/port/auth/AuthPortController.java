package com.port.auth;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthPortController {

  @GetMapping("/")
  public String index() {
    return "HOME";
  }

  @PostMapping("/new")
  public ResponseEntity<String> newUser(@RequestHeader("Content-Type") String contentType,
      @RequestBody NewUserReq req) {
    if (!contentType.equals("application/json")) {
      return ResponseEntity.status(400).body("Bad request");
    }
    Storage st = new Storage();
    Optional<User> user = st.getUserWithEmail(req.getEmail());
    if (!user.isEmpty()) {
      return ResponseEntity.status(418).body("Already exists");
    }
    if (!st.createNewUser(req.getEmail(), req.getPassword(), req.getName(), req.getSurname())) {
      return ResponseEntity.status(500).body("Internal server error");
    }
    // TODO: do the logic
    NewUserRes res = new NewUserRes(200, "OK", "NOT_A_REAL_TOKEN", req.getName(), req.getSurname());
    return ResponseEntity.ok()
        .header("Content-Type", "application/json")
        .body(res.toJsonString());
  }
}
