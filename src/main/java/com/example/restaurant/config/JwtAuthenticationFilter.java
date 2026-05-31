package com.example.restaurant.config;

import com.example.restaurant.exceptions.UserBlockedException;
import com.example.restaurant.models.Users;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.JwtServices;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtServices _jwtServices;
  private final IUserRepository _userRepo;
  private final HandlerExceptionResolver _resolver;

  public JwtAuthenticationFilter(
      JwtServices jwtServices,
      IUserRepository userRepo,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
    this._jwtServices = jwtServices;
    this._userRepo = userRepo;
    this._resolver = resolver;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {

    String jwt = extractJwtFromRequest(request);

    if (jwt != null) {
      boolean isBlocked = authenticateUserFromJwt(jwt, request, response);

      if (isBlocked) {
        return;
      }
    }

    chain.doFilter(request, response);
  }

  private String extractJwtFromRequest(HttpServletRequest request) {
    final String authHeader = request.getHeader("Authorization");

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }

    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if ("accessToken".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }

    return null;
  }

  private boolean authenticateUserFromJwt(
      String jwt, HttpServletRequest request, HttpServletResponse response) {
    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      return false;
    }

    String username = _jwtServices.extractUsername(jwt);
    if (username == null) {
      return false;
    }

    Optional<Users> userOpt = _userRepo.findByNormalizedUsername(username.trim().toUpperCase());

    if (userOpt.isPresent()) {
      Users user = userOpt.get();

      if (_jwtServices.isTokenValid(jwt, user)) {

        if (!user.isEnabled()) {
          _resolver.resolveException(
              request,
              response,
              null,
              new UserBlockedException("User account is blocked or inactive."));
          return true;
        }

        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    }

    return false;
  }
}
