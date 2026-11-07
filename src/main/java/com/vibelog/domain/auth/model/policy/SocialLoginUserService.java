package com.vibelog.domain.auth.model.policy;

import com.vibelog.domain.auth.model.entity.SocialProvider;
import com.vibelog.domain.auth.model.service.SocialMemberService;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/*
 * [SocialLoginUserService]
 * 역할 : 구글·깃허브가 돌려준 사용자 정보를 우리 회원(LoginMember)으로 바꾼다 (API-USR-07, #91).
 * 책임 : - 제공자마다 다른 이름의 값(고유 번호·이메일·이름)을 꺼내 SocialMemberService 에 넘긴다.
 *        - 깃허브는 이메일을 숨긴 계정이면 /user/emails 에서 대표·인증된 이메일을 찾는다.
 *        - 이메일을 받지 못했거나 회원 규칙에 걸리면 OAuth2AuthenticationException 으로 바꿔 던진다.
 *          오류 코드(SOCIAL_EMAIL_CONFLICT 등)는 SocialLoginFailureHandler 가 로그인 화면 안내로 바꾼다.
 */
@Component
@RequiredArgsConstructor
public class SocialLoginUserService extends DefaultOAuth2UserService {

    private static final String GITHUB_EMAILS = "https://api.github.com/user/emails";

    private final SocialMemberService socialMemberService;
    private final RestClient restClient = RestClient.create();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        Map<String, Object> attributes = super.loadUser(request).getAttributes();
        SocialProvider provider = SocialProvider.valueOf(
                request.getClientRegistration().getRegistrationId().toUpperCase(Locale.ROOT));
        String providerUserId;
        String email;
        String name;
        if (provider == SocialProvider.GOOGLE) {
            providerUserId = String.valueOf(attributes.get("sub"));
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
        } else {
            providerUserId = String.valueOf(attributes.get("id"));
            email = attributes.get("email") instanceof String value ? value : githubPrimaryEmail(request);
            name = attributes.get("name") instanceof String value ? value : (String) attributes.get("login");
        }
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("SOCIAL_EMAIL_MISSING"), "이메일을 받지 못했습니다.");
        }
        try {
            return socialMemberService.login(provider, providerUserId, email, name);
        } catch (BusinessException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error(e.getErrorCode().name()), e.getMessage());
        }
    }

    private String githubPrimaryEmail(OAuth2UserRequest request) {
        List<Map<String, Object>> emails = restClient.get().uri(GITHUB_EMAILS)
                .headers(headers -> headers.setBearerAuth(request.getAccessToken().getTokenValue()))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return emails == null ? null : emails.stream()
                .filter(email -> Boolean.TRUE.equals(email.get("primary")) && Boolean.TRUE.equals(email.get("verified")))
                .map(email -> (String) email.get("email"))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
