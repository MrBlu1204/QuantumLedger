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
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(UserService userService, AuthService authService, JwtService jwtService, RefreshTokenService refreshTokenService) {

        this.userService = userService;
        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
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
        JwtService.AccessToken token = jwtService.createAccessToken(user);
        RefreshTokenService.IssuedRefreshToken refresh = refreshTokenService.issueForNewSession(user.getUserId());

        return new LoginResponse("Bearer", token.value(), token.expiresInSeconds(),
                refresh.value(), refresh.expiresInSeconds(), UserResponse.from(user));
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {

        AuthService.Session s = authService.refresh(request.refreshToken());

        return new LoginResponse("Bearer", s.access().value(), s.access().expiresInSeconds(),
                s.refresh().value(), s.refresh().expiresInSeconds(), UserResponse.from(s.user()));
    }


}
