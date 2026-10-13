package com.synccarreira.synccarreira_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ResourceServerConfig {

	@Value("${cors.origins}")
	private String corsOrigins;

	private static final String USERS = "/users/**";
	private static final String TRAILS = "/trails/**";
	private static final String INSTITUTIONS = "/institutions/**";
	private static final String ANSWERS = "/answers/**";
	private static final String CLASSES = "/classes/**";
	private static final String STUDENTS = "/students/**";
	private static final String PSYCHOLOGISTS = "/psychologists/**";
	private static final String AUTH = "/auth/**";
	private static final String APPOINTMENTS = "/appointments/**";
	private static final String PSYCHOLOGIST_PANEL = "/psychologist-panel/**";
	private static final String INFORMATION_LINKS = "/information-links/**";
	private static final String SYNTHESES = "/syntheses/**";
	private static final String JOURNEY = "/journey/**";
	private static final String QUESTIONS = "/questions/**";
	private static final String ADMIN = "ADMIN";
	private static final String USER = "USER";
	private static final String PSYCHOLOGIST = "PSICOLOGA";

	@Bean
	@Profile("test")
	@Order(1)
	public SecurityFilterChain h2SecurityFilterChain(HttpSecurity http) throws Exception {

		http.securityMatcher(PathRequest.toH2Console()).csrf(csrf -> csrf.disable())
				.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));
		return http.build();
	}

	@Bean
	@Order(3)
	public SecurityFilterChain rsSecurityFilterChain(HttpSecurity http) throws Exception {

		http.csrf(csrf -> csrf.disable());
		http.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/error").permitAll()
				.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
				.requestMatchers(HttpMethod.PUT, AUTH).permitAll()
				.requestMatchers(HttpMethod.GET, "/users/me").authenticated()
				.requestMatchers(USERS).hasRole(ADMIN)
				.requestMatchers(HttpMethod.POST, "/trails/*/can-access").hasRole(USER)
				.requestMatchers(HttpMethod.GET, TRAILS).permitAll()
				.requestMatchers(HttpMethod.POST, TRAILS).hasRole(ADMIN)
				.requestMatchers(HttpMethod.PUT, TRAILS).hasRole(ADMIN)
				.requestMatchers(HttpMethod.DELETE, TRAILS).hasRole(ADMIN)
				.requestMatchers(HttpMethod.POST, QUESTIONS).hasRole(ADMIN)
				.requestMatchers(HttpMethod.GET, QUESTIONS).hasAnyRole(PSYCHOLOGIST, ADMIN, USER)
				.requestMatchers(HttpMethod.PUT, QUESTIONS).hasRole(ADMIN)
				.requestMatchers(HttpMethod.DELETE, QUESTIONS).hasRole(ADMIN)
				.requestMatchers(HttpMethod.POST, ANSWERS).hasRole(USER)
				.requestMatchers(HttpMethod.GET, ANSWERS).hasRole(USER)
				.requestMatchers(HttpMethod.PUT, ANSWERS).hasRole(USER)
				.requestMatchers(HttpMethod.DELETE, ANSWERS).hasRole(USER)
				.requestMatchers(HttpMethod.POST, PSYCHOLOGISTS).hasRole(ADMIN)
				.requestMatchers(HttpMethod.GET, PSYCHOLOGISTS).hasRole(ADMIN)
				.requestMatchers(HttpMethod.PUT, PSYCHOLOGISTS).hasRole(ADMIN)
				.requestMatchers(HttpMethod.DELETE, PSYCHOLOGISTS).hasRole(ADMIN)
				.requestMatchers(HttpMethod.POST, STUDENTS).hasRole(ADMIN)
				.requestMatchers(HttpMethod.GET, STUDENTS).hasAnyRole(PSYCHOLOGIST, ADMIN)
				.requestMatchers(HttpMethod.PUT, STUDENTS).hasRole(ADMIN)
				.requestMatchers(HttpMethod.DELETE, STUDENTS).hasRole(ADMIN)
				.requestMatchers(HttpMethod.PATCH, STUDENTS).hasRole(ADMIN)
				.requestMatchers(HttpMethod.POST, INSTITUTIONS).hasRole(ADMIN)
				.requestMatchers(HttpMethod.GET, INSTITUTIONS).hasRole(ADMIN)
				.requestMatchers(HttpMethod.PUT, INSTITUTIONS).hasRole(ADMIN)
				.requestMatchers(HttpMethod.DELETE, INSTITUTIONS).hasRole(ADMIN)
				.requestMatchers(HttpMethod.POST, CLASSES).hasRole(ADMIN)
				.requestMatchers(HttpMethod.GET, CLASSES).hasRole(ADMIN)
				.requestMatchers(HttpMethod.PUT, CLASSES).hasRole(ADMIN)
				.requestMatchers(HttpMethod.DELETE, CLASSES).hasRole(ADMIN)
				.requestMatchers(HttpMethod.GET, "/appointments/psychologist/**").hasAnyRole(PSYCHOLOGIST, ADMIN)
				.requestMatchers(HttpMethod.GET, "/appointments/student/**").hasAnyRole(USER, ADMIN)
				.requestMatchers(HttpMethod.POST, APPOINTMENTS).hasAnyRole(PSYCHOLOGIST, ADMIN)
				.requestMatchers(HttpMethod.PUT, APPOINTMENTS).hasAnyRole(PSYCHOLOGIST, ADMIN)
				.requestMatchers(HttpMethod.PATCH, APPOINTMENTS).hasAnyRole(PSYCHOLOGIST, ADMIN)
				.requestMatchers(HttpMethod.GET, PSYCHOLOGIST_PANEL).hasAnyRole(PSYCHOLOGIST, ADMIN)
				.requestMatchers(HttpMethod.GET, "/information-links/me").hasRole(USER)
				.requestMatchers(HttpMethod.GET, INFORMATION_LINKS).hasAnyRole(PSYCHOLOGIST, ADMIN)
				.requestMatchers(HttpMethod.POST, INFORMATION_LINKS).hasAnyRole(PSYCHOLOGIST, ADMIN)
				.requestMatchers(HttpMethod.DELETE, INFORMATION_LINKS).hasAnyRole(PSYCHOLOGIST, ADMIN)
				.requestMatchers(HttpMethod.POST, SYNTHESES).hasRole(USER)
				.requestMatchers(HttpMethod.GET, SYNTHESES).hasRole(USER)
				.requestMatchers(HttpMethod.GET, JOURNEY).hasRole(USER)
				.requestMatchers(HttpMethod.POST, JOURNEY).hasRole(USER));
		http.oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer.jwt(Customizer.withDefaults()));
		http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
		return http.build();
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
		grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
		grantedAuthoritiesConverter.setAuthorityPrefix("");

		JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
		return jwtAuthenticationConverter;
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {

		String[] origins = corsOrigins.split(",");

		CorsConfiguration corsConfig = new CorsConfiguration();
		corsConfig.setAllowedOriginPatterns(Arrays.asList(origins));
		corsConfig.setAllowedMethods(Arrays.asList("POST", "GET", "PUT", "DELETE", "PATCH"));
		corsConfig.setAllowCredentials(true);
		corsConfig.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
		// Permite que o front leia o nome do arquivo nos downloads de relatórios
		corsConfig.setExposedHeaders(Arrays.asList("Content-Disposition"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", corsConfig);
		return source;
	}
	
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	CorsFilter corsFilter() {
	    return new CorsFilter(corsConfigurationSource());
	}
}
