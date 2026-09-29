package com.fitmap.config;

import com.github.catomat0.oauthhelper.jwt.OahJwt;
import com.github.catomat0.oauthhelper.jwt.OahJwtProvider;
import com.github.catomat0.oauthhelper.jwt.OahRefreshTokenCookieWriter;
import com.github.catomat0.oauthhelper.jwt.OahRefreshTokenService;
import com.github.catomat0.oauthhelper.oauth.OahAuthorizeUrlBuilder;
import com.github.catomat0.oauthhelper.oauth.OahLoginService;
import com.github.catomat0.oauthhelper.oauth.OahOAuth;
import com.github.catomat0.oauthhelper.oauth.OahStateService;
import com.github.catomat0.oauthhelper.signuptoken.OahSignup;
import com.github.catomat0.oauthhelper.signuptoken.OahSignupTokenCookieWriter;
import com.github.catomat0.oauthhelper.signuptoken.OahSignupTokenProvider;
import com.github.catomat0.oauthhelper.signuptoken.OahSignupTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OahConfig {

    @Bean
    public OahOAuth oahOAuth(OahStateService state, OahAuthorizeUrlBuilder authorize, OahLoginService login) {
        return new OahOAuth(state, authorize, login);
    }

    @Bean
    public OahJwt oahJwt(OahJwtProvider provider, OahRefreshTokenService refresh, OahRefreshTokenCookieWriter cookie) {
        return new OahJwt(provider, refresh, cookie);
    }

    @Bean
    public OahSignup oahSignup(OahSignupTokenProvider provider, OahSignupTokenService service, OahSignupTokenCookieWriter cookie) {
        return new OahSignup(provider, service, cookie);
    }
}
