package com.quantumledger.app.auth;

import com.quantumledger.app.user.User;
import com.quantumledger.app.user.UserResponse;
import com.quantumledger.app.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {

        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody SignupRequest signupRequest) {
        String countryCode = signupRequest.countryCode() == null ? "+91" : signupRequest.countryCode();
        User user = userService.register(
                signupRequest.title(), signupRequest.firstName(), signupRequest.lastName(), signupRequest.email(),
                countryCode, signupRequest.phone(), signupRequest.password());

        return UserResponse.from(user);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        User user = authService.login(loginRequest.email(), loginRequest.password());
        return new LoginResponse(user.getUserId(), user.getFullName(), "Credentials verified. Token: <placeholder>");
    }


}
