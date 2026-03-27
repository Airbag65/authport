package com.port.auth;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.port.auth.Storage.LoginResult;
import com.port.auth.types.LoginReq;
import com.port.auth.types.LoginRes;
import com.port.auth.types.NewUserReq;
import com.port.auth.types.NewUserRes;
import com.port.auth.types.User;

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
        NewUserRes res = new NewUserRes(200, "OK");
        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(res.toJsonString());
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestHeader("Content-Type") String contentType, @RequestBody LoginReq req) {
        if (!contentType.equals("application/json")) {
            return ResponseEntity.status(400).body("Bad request");
        }
        Storage st = new Storage();
        Optional<User> user = st.getUserWithEmail(req.getEmail());
        if (user.isEmpty()) {
            return ResponseEntity.status(404).body("Not found");
        }
        LoginResult result = st.loginUser(req.getEmail(), req.getPassword(), req.getClientIdentifier(),
                req.getRemoteAddr());
        switch (result.status) {
            case SUCCESS:
                LoginRes res = new LoginRes(200, "OK", result.authToken);
                return ResponseEntity.ok()
                        .header("Content-Type", "application/json")
                        .body(res.toJsonString());
            case FAILIURE:
                return ResponseEntity.status(500).body("Internal server error");
            case ALREADY_LOGGED_IN:
                return ResponseEntity.status(418).body("Already logged in");
            default:
                break;
        }
        return ResponseEntity.ok().body("");
    }
}
