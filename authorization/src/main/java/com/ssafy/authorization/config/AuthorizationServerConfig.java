package com.ssafy.authorization.config;

import com.ssafy.authorization.filter.CustomUsernamePasswordAuthenticationFilter;
import com.ssafy.authorization.member.model.domain.Member;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {


	private final CustomUsernamePasswordAuthenticationFilter customUsernamePasswordAuthenticationFilter;
	@Autowired
	public AuthorizationServerConfig(CustomUsernamePasswordAuthenticationFilter customUsernamePasswordAuthenticationFilter) {

		this.customUsernamePasswordAuthenticationFilter = customUsernamePasswordAuthenticationFilter;
	}

	@Bean
	@Order(1)
	SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
		throws Exception {
		OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
		http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
			.authorizationEndpoint(auth -> auth
				.consentPage("/oauth2/consent"))
				.oidc(withDefaults());


		http
			.exceptionHandling((exceptions) -> exceptions
				.defaultAuthenticationEntryPointFor(
					new LoginUrlAuthenticationEntryPoint("/login"),
					new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
				)
			)
			.oauth2ResourceServer((resourceServer) -> resourceServer
				.jwt(withDefaults()));

		return http.build();
	}



	@Bean
	@Order(2)
	SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager)
		throws Exception {
		// customUsernamePasswordAuthenticationFilter.setCustomAuthenticationManager(authenticationManager);

		http.csrf(csrf -> csrf.disable());


		http
			.authorizeHttpRequests((request) -> request.requestMatchers(CorsUtils::isPreFlightRequest).permitAll());
		http
			.cors(corsCustomizer -> corsCustomizer.configurationSource(new CorsConfigurationSource() {

				@Override
				public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
					// CORS 설정 예시
					UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
					CorsConfiguration configuration = new CorsConfiguration();

					// 모든 출처 허용
					configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
					// 모든 메소드 허용
					configuration.setAllowedMethods(
						Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
					// 허용할 헤더 설정
					configuration.setAllowedHeaders(
						Arrays.asList("Authorization", "Cache-Control", "Content-Type", "Accept",
							"X-Requested-With", "remember-me"));
					// 브라우저가 응답에서 접근할 수 있는 헤더 설정
					configuration.setExposedHeaders(Arrays.asList("Set-Cookie", "Authorization"));
					// 자격 증명 허용 설정
					configuration.setAllowCredentials(true);
					// 사전 요청의 캐시 시간(초) 설정
					configuration.setMaxAge(3600L);

					// 모든 URL에 대해 CORS 설정 적용
					source.registerCorsConfiguration("/**", configuration);

					return configuration;
				}
			}));
		http
			.authorizeHttpRequests((authorize) -> authorize
				.requestMatchers("/js/**","/api/auth/**", "/css/**", "/favicon.ico", "/error","/image/**","/vendor/**",
					"/test/**","/login","/signup", "/sendemail","/certify","/forgot_password","/forgot_user","/find_user"
					,".well-known/jwks.json","/login_stats/**","/api/ttt/*").permitAll()
				.requestMatchers("/ws").permitAll()
				.anyRequest().authenticated()
			)
			.formLogin(formLogin -> formLogin
				.loginPage("/login")
			);

		http.addFilterBefore(customUsernamePasswordAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);


		return http.build();

	}



	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	@Bean
	public LobHandler lobHandler() {
		return new DefaultLobHandler();
	}

	@Bean
	public RowMapper<OAuth2Authorization> authorizationRowMapper(RegisteredClientRepository registeredClientRepository) {
		return new TestOauth2ServiceImpl.OAuth2AuthorizationRowMapper(registeredClientRepository);
	}

	@Bean
	public Function<OAuth2Authorization, List<SqlParameterValue>> authorizationParametersMapper() {
		return new TestOauth2ServiceImpl.OAuth2AuthorizationParametersMapper();
	}

	@Bean
	public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(UserDetailsService userDetailsService) {
		return (context) -> {
			OAuth2TokenType tokenType = context.getTokenType();
			if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
				String username = context.getPrincipal().getName();
				Member user = (Member) userDetailsService.loadUserByUsername(username);
				context.getClaims().issuedAt(Instant.now());
				// 2 hour expiry time for access token
				context.getClaims().expiresAt(Instant.now().plusSeconds(7200));
			} else if (OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
				// 10 days expiry time for refresh token
				context.getClaims().expiresAt(Instant.now().plusSeconds(864000));
			}
		};
	}


}