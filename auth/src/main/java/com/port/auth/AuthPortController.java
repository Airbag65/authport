package com.port.auth;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.port.auth.Storage.LoginResult;
import com.port.auth.types.LoginReq;
import com.port.auth.types.LoginRes;
import com.port.auth.types.NewUserReq;
import com.port.auth.types.NewUserRes;
import com.port.auth.types.SignOutReq;
import com.port.auth.types.User;
import com.port.auth.types.ValidateReq;

@RestController
public class AuthPortController {
    private Storage st;

    public AuthPortController() {
        this.st = new Storage();
    }

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
        Optional<User> user = this.st.getUserWithEmail(req.getEmail());
        if (!user.isEmpty()) {
            return ResponseEntity.status(418).body("Already exists");
        }
        if (!this.st.createNewUser(req.getEmail(), req.getPassword(), req.getName(), req.getSurname())) {
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
        Optional<User> user = this.st.getUserWithEmail(req.getEmail());
        if (user.isEmpty()) {
            return ResponseEntity.status(404).body("Not found");
        }
        if (!req.getPassword().equals(user.get().getPassword())) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        LoginResult result = this.st.loginUser(req.getEmail(), req.getPassword(), req.getClientIdentifier(),
                req.getRemoteAddr());
        switch (result.status) {
            case SUCCESS:
                LoginRes res = new LoginRes(200, "OK", result.authToken, user.get().getName(), user.get().getSurname());
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

    @PutMapping("/signOut")
    public ResponseEntity<String> signOut(@RequestHeader("Content-Type") String contentType,
            @RequestBody SignOutReq req) {
        if (!contentType.equals("application/json")) {
            return ResponseEntity.status(400).body("Bad request");
        }
        Optional<User> user = this.st.getUserWithEmail(req.getEmail());
        if (user.isEmpty()) {
            return ResponseEntity.status(404).body("Not found");
        }
        if (!this.st.userIsLoggedIn(user.get().getAuthToken(), req.getIpAddr(), req.getClientIdentifier())) {
            return ResponseEntity.status(304).body("Not signed in");
        }

        if (this.st.signOutUser(req.getEmail(), req.getClientIdentifier(), req.getIpAddr())) {
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(new NewUserRes(200, "OK")
                            .toJsonString());
        }
        return ResponseEntity.status(500).body("Internal server error");
    }

    @PostMapping("/valid")
    public ResponseEntity<String> validate(@RequestHeader("Content-Type") String contentType,
            @RequestBody ValidateReq req) {
        if (!contentType.equals("application/json")) {
            return ResponseEntity.status(400).body("Bad request");
        }
        if (!this.st.validateUserToken(req.getAuthToken(), req.getEmail())) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok().body("OK");
    }

    @GetMapping("health")
    public ResponseEntity<String> health() {
        String response = "{\"status\": \"OK\"}";
        return ResponseEntity.ok().body(response);
    }
}
