package com.example.restaurant.services;

import com.example.restaurant.dto.request.LoginRequest;
import com.example.restaurant.dto.response.AuthResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.jpa.IJpaUserRepository;
import com.example.restaurant.services.interfaces.IAuthServices;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServices implements IAuthServices {
    private  final AuthenticationManager _authManager;
    private final JwtServices _jwtServices;
    private final IJpaUserRepository _jpaUserRepository;

    public ResultHandler<AuthResponse> authenticate(LoginRequest request) {
        try {
            var auth = _authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) auth.getPrincipal();

            if (!userDetails.isEnabled()) return ResultHandler.failure("User no enabled", HttpStatus.UNAUTHORIZED.value());

            String jwtToken = _jwtServices.generateToken(userDetails);

            if (jwtToken == null) return ResultHandler.failure("Jwt Token no generate", HttpStatus.INTERNAL_SERVER_ERROR.value());

            AuthResponse response = AuthResponse.builder()
                    .token(jwtToken)
                    .username(userDetails.getUsername())
                    .build();

            if (response == null) return ResultHandler.failure(
                    "Server error",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    null
            );

            return ResultHandler.success(
                    "Login successfull",
                    HttpStatus.OK.value(),
                    response);
        }
        catch (AuthenticationException aex)
        {
            return ResultHandler.failure(
                    "Invalid credentials",
                    HttpStatus.UNAUTHORIZED.value(),
                    List.of(aex.getMessage())
            );
        }
        catch (Exception ex)
        {
            return ResultHandler.failure(
                    "Server error",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    List.of(ex.getMessage())
            );
        }
    }

}
