package com.example.restaurant.config;

import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.JwtServices;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtServices _jwtServices;
  private final IUserRepository _userRepo;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {

    String jwt = extractJwtFromRequest(request);

    if (jwt != null) {
      authenticateUserFromJwt(jwt, request);
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

  private void authenticateUserFromJwt(String jwt, HttpServletRequest request) {
    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      return;
    }

    String username = _jwtServices.extractUsername(jwt);
    if (username == null) {
      return;
    }

    _userRepo
        .findByNormalizedUsername(username.trim().toUpperCase())
        .filter(user -> _jwtServices.isTokenValid(jwt, user))
        .ifPresent(
            user -> {
              UsernamePasswordAuthenticationToken authToken =
                  new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

              authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
              SecurityContextHolder.getContext().setAuthentication(authToken);
            });
  }
}
