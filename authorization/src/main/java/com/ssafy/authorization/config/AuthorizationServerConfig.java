package com.ssafy.authorization.config;

import static org.springframework.security.config.Customizer.*;

import java.util.Arrays;
import java.util.UUID;

import com.ssafy.authorization.member.login.filter.CustomAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

	private final RedisTemplate<String, String> redisTemplate;

	@Value("${oauth2.client.redirect-uri}")
	private String redirectBaseUrl;

	@Autowired
	public AuthorizationServerConfig(RedisTemplate<String, String> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

//	@Bean
//	public CorsConfigurationSource corsConfigurationSource() {
//		CorsConfiguration configuration = new CorsConfiguration();
//		configuration.setAllowedOrigins(Arrays.asList("http://localhost:9000")); // 허용할 오리진
//		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
//		configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
//
//		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//		source.registerCorsConfiguration("/**", configuration);
//		return source;
//	}
	@Bean
	@Order(1)
	SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
			throws Exception {
		OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
		http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
				.authorizationEndpoint(auth -> auth
						.consentPage("/oauth2/consent"));
		//				.oidc(withDefaults());
		http

				.addFilterBefore(new CustomAuthenticationFilter(redisTemplate), OAuth2AuthorizationRequestRedirectFilter.class)
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
	 SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
	 		throws Exception {
	 	http.csrf(csrf -> csrf.disable());
	 	http
	 			.authorizeHttpRequests((authorize) -> authorize
	 					.requestMatchers("/api/auth/waitSignal", "/css/**", "/favicon.ico", "/error","/image/**","/vendor/**",
	 						"/test/**","/login","/signup", "/sendemail","/certify","/forgot_password","/forgot_user","/find_user"
	 						,".well-known/jwks.json").permitAll()
	 					.anyRequest().authenticated()
	 			)
	 			.formLogin(formLogin -> formLogin
	 					.loginPage("/login")
	 			);

	 	return http.build();
	 }

	 // @Bean
	 // @Order(2)
	 // SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
	 // 		throws Exception {
		//  http
		// 		 .csrf(httpSecurityCsrfConfigurer -> httpSecurityCsrfConfigurer.disable())
		// 		 .authorizeHttpRequests(authorize -> authorize
		// 				 .requestMatchers("/api/auth/login", "/login_test", "/css/**", "/favicon.ico", "/error", "/test/**", "/login", "/sign_up", ".well-known/jwks.json").permitAll()
		// 				 .anyRequest().authenticated())
		// 		 .formLogin(formLogin -> formLogin
		// 				 .loginPage("/login_test")
		// 				 .successHandler((request, response, authentication) -> {
		// 					 System.out.println("Login success");
		// 				 })
		// 				 .failureHandler((request, response, exception) -> {
		// 					 System.out.println("Login failed");
		// 				 })
		// 		 );
	 // 	return http.build();
	 // }
	
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	RegisteredClientRepository jdbcRegisteredClientRepository(JdbcTemplate template) {
		return new JdbcRegisteredClientRepository(template);
	}

	@Bean
	OAuth2AuthorizationService jdbcOAuth2AuthorizationService(
		JdbcOperations jdbcOperations,
		RegisteredClientRepository registeredClientRepository) {

		RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
			.clientId("1234")
			.clientName("ssoauth")
			.clientSecret(passwordEncoder().encode("secret"))
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
			.redirectUri(redirectBaseUrl+"/login/oauth2/code/client")
			.postLogoutRedirectUri(redirectBaseUrl+"/logged-out")
			.scope(OidcScopes.OPENID)
			.scope(OidcScopes.PROFILE)
			.scope("profile")
			.scope("email")
			.scope("image")
			.scope("name")
			.scope("gender")
			.scope("phoneNumber")
			.scope("studentId")
			.clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
			.build();

		RegisteredClient deviceClient = RegisteredClient.withId(UUID.randomUUID().toString())
			.clientId("device-1234")
			.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
			.authorizationGrantType(AuthorizationGrantType.DEVICE_CODE)
			.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
			.scope("profile")
			.scope("email")
			.scope("image")
			.scope("name")
			.scope("gender")
			.scope("phoneNumber")
			.scope("studentId")
			.build();

		 // registeredClientRepository.save(registeredClient);
		 // registeredClientRepository.save(deviceClient);

		return new JdbcOAuth2AuthorizationService(jdbcOperations, registeredClientRepository);
	}

	@Bean
	OAuth2AuthorizationConsentService jdbcOAuth2AuthorizationConsentService(
		JdbcOperations jdbcOperations,
		RegisteredClientRepository registeredClientRepository) {
		return new JdbcOAuth2AuthorizationConsentService(jdbcOperations, registeredClientRepository);
	}

}