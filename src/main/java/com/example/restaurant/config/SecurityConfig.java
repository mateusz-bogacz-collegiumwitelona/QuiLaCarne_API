package com.example.restaurant.config;

import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.models.Users;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
//import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
  //    @Value("${app.cors.allowed-origins}")
  //    private String _frontedURL;

  private final JwtAuthenticationFilter _jwtAuthenticationFilter;
  private final AuthenticationProvider _authenticationProvider;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      AuthenticationProvider authenticationProvider) {
    _jwtAuthenticationFilter = jwtAuthenticationFilter;
    _authenticationProvider = authenticationProvider;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();

    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
    requestHandler.setCsrfRequestAttributeName(null);

    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .anonymous(anon -> anon.principal(new Users()))
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(csrfTokenRepository)
                    .csrfTokenRequestHandler(requestHandler)
                    .ignoringRequestMatchers(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/google",
                        "/api/auth/verify-2fa")
                    .ignoringRequestMatchers(
                        request -> {
                          String authHeader = request.getHeader("Authorization");
                          return authHeader != null && authHeader.startsWith("Bearer ");
                        }))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/test",
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/register-confirm",
                        "/api/auth/reset-password",
                        "/api/auth/set-password",
                        "/api/auth/google",
                        "/api/auth/verify-2fa",
                        "/api/auth/refresh",
                        "/api/auth/csrf",
                        "/api/dishes/menu/public")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .authenticationProvider(_authenticationProvider)
        .addFilterBefore(_jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(new CsrfConfig(), JwtAuthenticationFilter.class)
        .exceptionHandling(
            exception ->
                exception
                    .authenticationEntryPoint(
                        (request, response, authException) -> {
                          response.setContentType("application/json;charset=UTF-8");
                          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                          ResultHandler<Void> result =
                              ResultHandler.failure(
                                  "Unauthorized - Valid JWT token is required",
                                  HttpStatus.UNAUTHORIZED.value());
                          response.getWriter().write(new ObjectMapper().writeValueAsString(result));
                        })
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) -> {
                          response.setContentType("application/json;charset=UTF-8");
                          response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                          ResultHandler<Void> result =
                              ResultHandler.failure(
                                  "Forbidden - You don't have permission to access this resource",
                                  HttpStatus.FORBIDDEN.value());
                          response.getWriter().write(new ObjectMapper().writeValueAsString(result));
                        }));

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOrigins(
            List.of("https://quilacarne.com.pl", "https://www.quilacarne.com.pl"));

    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

    configuration.setAllowedHeaders(
        List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "X-XSRF-TOKEN",
            "X-CSRF-TOKEN",
            "X-Request-ID"));

    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public GoogleAuthenticator googleAuthenticator() {
    return new GoogleAuthenticator();
  }
}
