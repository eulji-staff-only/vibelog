package com.vibelog.domain.auth.model.policy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * [SocialLoginRegistrations]
 * 역할 : 구글·깃허브 로그인 설정 (API-USR-06·07, #91). 키는 환경 변수(GOOGLE_CLIENT_ID 등)로만 받는다.
 * 책임 : - 키가 있는 제공자만 켠다. 하나도 없으면 비어 있고, SecurityConfig 가 소셜 로그인을 켜지 않는다 (로컬 개발 기본).
 *          Spring Boot 기본 설정(spring.security.oauth2.client.*)은 키가 비어 있으면 앱이 뜨지 않아 쓰지 않는다.
 *        - 구글은 openid 없이 profile·email 만 받는다. 깃허브는 이메일을 숨긴 계정도 있어 user:email 을 같이 받는다.
 *        - 로그인·가입 화면은 enabledIds() 로 버튼을 보여 줄지 정한다.
 */
@Component
public class SocialLoginRegistrations implements ClientRegistrationRepository, Iterable<ClientRegistration> {

    private final Map<String, ClientRegistration> registrations = new LinkedHashMap<>();

    public SocialLoginRegistrations(@Value("${vibelog.oauth.google-client-id}") String googleId,
                                    @Value("${vibelog.oauth.google-client-secret}") String googleSecret,
                                    @Value("${vibelog.oauth.github-client-id}") String githubId,
                                    @Value("${vibelog.oauth.github-client-secret}") String githubSecret) {
        if (!googleId.isBlank()) {
            add(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(googleId).clientSecret(googleSecret).scope("profile", "email").build());
        }
        if (!githubId.isBlank()) {
            add(CommonOAuth2Provider.GITHUB.getBuilder("github")
                    .clientId(githubId).clientSecret(githubSecret).scope("read:user", "user:email").build());
        }
    }

    private void add(ClientRegistration registration) {
        registrations.put(registration.getRegistrationId(), registration);
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return registrations.get(registrationId);
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        return registrations.values().iterator();
    }

    /* "google", "github" 중 켜진 것 */
    public List<String> enabledIds() {
        return List.copyOf(registrations.keySet());
    }
}
