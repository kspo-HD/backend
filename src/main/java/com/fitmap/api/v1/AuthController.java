package com.fitmap.api.v1;

import com.fitmap.common.ApiResponse;
import com.fitmap.domain.user.User;
import com.fitmap.service.AuthService;
import com.github.catomat0.oauthhelper.jwt.OahJwt;
import com.github.catomat0.oauthhelper.jwt.OahJwtErrorCode;
import com.github.catomat0.oauthhelper.jwt.OahJwtException;
import com.github.catomat0.oauthhelper.jwt.OahJwtPayload;
import com.github.catomat0.oauthhelper.oauth.OahAuthorizeParams;
import com.github.catomat0.oauthhelper.oauth.OahOAuth;
import com.github.catomat0.oauthhelper.oauth.OahUserInfo;
import com.github.catomat0.oauthhelper.signuptoken.OahSignup;
import com.github.catomat0.oauthhelper.signuptoken.OahSignupTokenPayload;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final AuthService authService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @GetMapping("/oauth2/{provider}/authorize")
    public void authorize(@PathVariable String provider, HttpServletResponse response) throws IOException {
        OahAuthorizeParams params = oauth.state().issue(provider);
        String url = oauth.authorize().build(provider, params);
        response.sendRedirect(url);
    }

    @GetMapping("/oauth2/{provider}/callback")
    public void callback(@PathVariable String provider,
                         @RequestParam String code,
                         @RequestParam String state,
                         HttpServletResponse response) throws IOException {
        String codeVerifier = oauth.state().validateAndConsume(state, provider);
        if (codeVerifier == null) {
            response.sendRedirect(frontendUrl + "/login?error=invalid_state");
            return;
        }

        OahUserInfo info = oauth.login().fetchUserInfo(provider, code, codeVerifier);

        authService.findOrNull(info.provider(), info.providerId())
                .ifPresentOrElse(
                        user -> {
                            user.setLastLoginAt(LocalDateTime.now());
                            authService.updateLastLogin(user);
                            try { redirectWithJwt(user, response); } catch (IOException e) { throw new RuntimeException(e); }
                        },
                        () -> {
                            try { redirectToOnboarding(info, response); } catch (IOException e) { throw new RuntimeException(e); }
                        }
                );
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> signup(@RequestBody SignupRequest req,
                                                                    HttpServletRequest request,
                                                                    HttpServletResponse response) {
        String token = signup.cookie().read(request);
        if (token == null || !signup.provider().validate(token)) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("invalid_signup_token"));
        }

        OahSignupTokenPayload p = signup.provider().parse(token);
        if (!signup.service().validateAndConsume(p.provider(), p.providerId(), token)) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("signup_token_expired"));
        }

        User user = User.builder()
                .provider(p.provider())
                .providerId(p.providerId())
                .email(p.email())
                .name(req.nickname() != null ? req.nickname() : p.extra("nickname"))
                .interestCategory(req.interestCategory())
                .interestSido(req.interestSido())
                .budgetRange(req.budgetRange())
                .termsAgreedAt(LocalDateTime.now())
                .privacyAgreedAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();

        authService.createUser(user);
        signup.cookie().clear(response);
        return issueJwt(user, response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(HttpServletRequest request, HttpServletResponse response) {
        String oldRefresh = jwt.cookie().read(request);
        if (!jwt.provider().validateRefresh(oldRefresh)) {
            throw new OahJwtException(OahJwtErrorCode.TOKEN_TYPE_MISMATCH, "token type mismatch");
        }

        OahJwtPayload p = jwt.provider().parseRefresh(oldRefresh);
        if (!jwt.refresh().validateAndConsume(p.userId(), oldRefresh)) {
            return ResponseEntity.status(401).body(ApiResponse.fail("token_reused_or_expired"));
        }

        User user = authService.findById(Long.parseLong(p.userId()));
        String newAccess = jwt.provider().generateAccessToken(p.userId(), user.getRole());
        String newRefresh = jwt.provider().generateRefreshToken(p.userId());
        jwt.refresh().save(p.userId(), newRefresh);

        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + newAccess);
        jwt.cookie().write(response, newRefresh);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwt.cookie().read(request);
        if (refreshToken != null && jwt.provider().validateRefresh(refreshToken)) {
            OahJwtPayload p = jwt.provider().parseRefresh(refreshToken);
            jwt.refresh().delete(p.userId());
        }
        jwt.cookie().clear(response);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private void redirectWithJwt(User user, HttpServletResponse response) throws IOException {
        String uid = user.getId().toString();
        String access = jwt.provider().generateAccessToken(uid, user.getRole());
        String refresh = jwt.provider().generateRefreshToken(uid);
        jwt.refresh().save(uid, refresh);
        jwt.cookie().write(response, refresh);
        response.sendRedirect(frontendUrl + "/auth/callback?access_token=" + access);
    }

    private void redirectToOnboarding(OahUserInfo info, HttpServletResponse response) throws IOException {
        String token = signup.provider()
                .builder(info.provider(), info.providerId(), info.email())
                .claim("nickname", info.nickname())
                .build();
        signup.service().save(info.provider(), info.providerId(), token);
        signup.cookie().write(response, token);
        response.sendRedirect(frontendUrl + "/onboarding");
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> issueJwt(User user, HttpServletResponse response) {
        String uid = user.getId().toString();
        String access = jwt.provider().generateAccessToken(uid, user.getRole());
        String refresh = jwt.provider().generateRefreshToken(uid);
        jwt.refresh().save(uid, refresh);
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access);
        jwt.cookie().write(response, refresh);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("registered", true)));
    }

    public record SignupRequest(String nickname, String interestCategory, String interestSido, String budgetRange) {}
}
