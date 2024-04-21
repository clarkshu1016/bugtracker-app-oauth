package com.bugtracker.bugtrackerclient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class OAuth2LoginSecurityConfig {

    private static final List<String> SOCIAL_PROVIDERS = List.of("Google", "Facebook", "GitLab", "GitHub");

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {

            http
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers("/login").permitAll()
                                .requestMatchers("/bugtracker/ui").authenticated()
                                .requestMatchers("/bugtracker/ui/admin/**").hasAnyRole("bugtracker.admin")
                                .requestMatchers("/bugtracker/ui/**").hasAnyRole("bugtracker.admin", "bugtracker.user")
                                .anyRequest().authenticated())
                .oauth2Login(oauth2 ->
                    oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(epConfig -> epConfig.oidcUserService(this.userService()))
                        .authorizationEndpoint(
                            cfg -> cfg.authorizationRequestResolver(
                                    pkceResolver(clientRegistrationRepository))))
                .oauth2Client(Customizer.withDefaults())
                .logout(logout -> logout
                                    .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                                    .logoutSuccessHandler(oidcLogoutSuccessHandler()));

            return http.build();
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(this.clientRegistrationRepository);

        // Sets the location that the End-User's User Agent will be redirected to
        // after the logout has been performed at the Provider
        oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/bugtracker/ui");
        return oidcLogoutSuccessHandler;
    }

    public OAuth2AuthorizationRequestResolver pkceResolver(ClientRegistrationRepository repo) {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return resolver;
    }

    private OAuth2UserService<OidcUserRequest, OidcUser> userService() {

        final OidcUserService delegate = new OidcUserService();
        return (userRequest) -> {

            // Delegate to the default implementation for loading a user
            OidcUser oidcUser = delegate.loadUser(userRequest);

            // Not used in this logic
            OAuth2AccessToken accessToken = userRequest.getAccessToken();

            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            // Handle different clients
            ClientRegistration cliReg = userRequest.getClientRegistration();
            if (isSocial(cliReg.getClientName())) {
                mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_bugtracker.user"));
            }
            else {
                List<String> roles = (List<String>) oidcUser.getIdToken().getClaim("roles");
                if (roles == null) {
                    roles = List.of();
                }

                List<SimpleGrantedAuthority> listAuthorities
                        = roles.stream().map(SimpleGrantedAuthority::new).toList();
                mappedAuthorities.addAll(listAuthorities);
            }


            // Create new DefaultOidcUser with authorities
            return new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        };

    }

    private boolean isSocial(String clientName) {
        return (SOCIAL_PROVIDERS.contains(clientName));
    }

//    @Bean
//    public AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientServiceAndManager (
//            ClientRegistrationRepository clientRegistrationRepository,
//            OAuth2AuthorizedClientService authorizedClientService) {
//
//        OAuth2AuthorizedClientProvider authorizedClientProvider =
//                OAuth2AuthorizedClientProviderBuilder.builder()
//                        .clientCredentials()
//                        .build();
//
//        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
//                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
//                        clientRegistrationRepository, authorizedClientService);
//        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
//
//        return authorizedClientManager;
//    }

}
