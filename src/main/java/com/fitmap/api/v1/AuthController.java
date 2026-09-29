package com.fitmap.api.v1;

import com.github.catomat0.oauthhelper.jwt.OahJwt;
import com.github.catomat0.oauthhelper.oauth.OahOAuth;
import com.github.catomat0.oauthhelper.signuptoken.OahSignup;
import com.github.catomat0.oauthhelper.signuptoken.OahSignupTokenPayload;
import com.github.catomat0.oauthhelper.oauth.OahAuthorizeParams;
import com.github.catomat0.oauthhelper.jwt.OahJwtPayload;
import com.github.catomat0.oauthhelper.oauth.OahUserInfo;
import com.github.catomat0.oauthhelper.jwt.OahJwtException;
import com.github.catomat0.oauthhelper.jwt.OahJwtErrorCode;
import com.fitmap.domain.user.User;
import com.fitmap.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final OahOAuth oauth;
    private final OahJwt jwt;
    private final OahSignup signup;
    private final UserRepository userRepository;

    @GetMapping("/oauth2/{provider}/authorize")
    public void authorize(@PathVariable String provider, HttpServletResponse response) throws IOException {
        OahAuthorizeParams params = oauth.state().issue(provider);
        String url = oauth.authorize().build(provider, params);
        response.sendRedirect(url);
    }

    @GetMapping("/oauth2/{provider}/callback")
    public ResponseEntity<?> callback(@PathVariable String provider,
                                      @RequestParam String code,
                                      @RequestParam String state,
                                      HttpServletResponse response) {
        String codeVerifier = oauth.state().validateAndConsume(state, provider);
        if (codeVerifier == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_state"));
        }

        OahUserInfo info = oauth.login().fetchUserInfo(provider, code, codeVerifier);

        return userRepository
                .findByProviderAndProviderId(info.provider(), info.providerId())
                .<ResponseEntity<?>>map(user -> {
                    user.setLastLoginAt(LocalDateTime.now());
                    userRepository.save(user);
                    return issueJwt(user, response);
                })
                .orElseGet(() -> issueSignupToken(info, response));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest req,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        String token = signup.cookie().read(request);
        if (token == null || !signup.provider().validate(token)) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_signup_token"));
        }

        OahSignupTokenPayload p = signup.provider().parse(token);
        if (!signup.service().validateAndConsume(p.provider(), p.providerId(), token)) {
            return ResponseEntity.badRequest().body(Map.of("error", "signup_token_expired"));
        }

        User user = User.builder()
                .provider(p.provider())
                .providerId(p.providerId())
                .email(p.email())
                .name(req.nickname() != null ? req.nickname() : p.extra("nickname"))
                .termsAgreedAt(LocalDateTime.now())
                .privacyAgreedAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        signup.cookie().clear(response);
        return issueJwt(user, response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String oldRefresh = jwt.cookie().read(request);
        if (!jwt.provider().validateRefresh(oldRefresh)) {
            throw new OahJwtException(OahJwtErrorCode.TOKEN_TYPE_MISMATCH, "token type mismatch");
        }

        OahJwtPayload p = jwt.provider().parseRefresh(oldRefresh);
        if (!jwt.refresh().validateAndConsume(p.userId(), oldRefresh)) {
            return ResponseEntity.status(401).body(Map.of("error", "token_reused_or_expired"));
        }

        User user = userRepository.findById(Long.parseLong(p.userId())).orElseThrow();
        String newAccess = jwt.provider().generateAccessToken(p.userId(), user.getRole());
        String newRefresh = jwt.provider().generateRefreshToken(p.userId());
        jwt.refresh().save(p.userId(), newRefresh);

        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + newAccess);
        jwt.cookie().write(response, newRefresh);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwt.cookie().read(request);
        if (refreshToken != null && jwt.provider().validateRefresh(refreshToken)) {
            OahJwtPayload p = jwt.provider().parseRefresh(refreshToken);
            jwt.refresh().delete(p.userId());
        }
        jwt.cookie().clear(response);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<?> issueJwt(User user, HttpServletResponse response) {
        String uid = user.getId().toString();
        String access = jwt.provider().generateAccessToken(uid, user.getRole());
        String refresh = jwt.provider().generateRefreshToken(uid);
        jwt.refresh().save(uid, refresh);
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access);
        jwt.cookie().write(response, refresh);
        return ResponseEntity.ok(Map.of("registered", true));
    }

    private ResponseEntity<?> issueSignupToken(OahUserInfo info, HttpServletResponse response) {
        String token = signup.provider()
                .builder(info.provider(), info.providerId(), info.email())
                .claim("nickname", info.nickname())
                .build();
        signup.service().save(info.provider(), info.providerId(), token);
        signup.cookie().write(response, token);
        return ResponseEntity.ok(Map.of("registered", false));
    }

    public record SignupRequest(String nickname) {}
}
