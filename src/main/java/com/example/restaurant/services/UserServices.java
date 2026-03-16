package com.example.restaurant.services;

import com.example.restaurant.dto.request.UpdatePasswordRequest;
import com.example.restaurant.enums.TokenTypeEnum;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.repository.interfaces.IVerificationTokenRepository;
import com.example.restaurant.services.interfaces.IUserServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServices implements IUserServices {
    private final IUserRepository _userRepo;
    private final EmailServices _emailServices;
    private final IVerificationTokenRepository _tokenRepo;

    @Override
    public ResultHandler<String> updatePassword(String userToken, UpdatePasswordRequest request) {
        try {
            if (!request.getPassword().equals(request.getConfirmPassword()))
                return ResultHandler.failure(
                        "Passwords do not match",
                        HttpStatus.BAD_REQUEST.value()
                );

            boolean isChanged = _userRepo.updatePassword(
                    userToken,
                    request.getOldPassword(),
                    request.getPassword()
            );

            if (!isChanged)
                return ResultHandler.failure(
                        "Invalid old Password",
                        HttpStatus.BAD_REQUEST.value()
                );

            return ResultHandler.success(
                    "Password updated",
                    HttpStatus.OK.value()
            );
        } catch (RuntimeException rex) {
            return ResultHandler.failure(
                    rex.getMessage(),
                    HttpStatus.NOT_FOUND.value()
            );
        } catch (Exception ex) {
            return ResultHandler.failure(
                    ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
    }

    @Override
    public ResultHandler<String> updateEmail(String userToken, String email) {
        try {
            var isUserExist = _userRepo.findMinimalByEmail(email);

            if (isUserExist.isPresent() && !isUserExist.get().token().equals(userToken))
                return ResultHandler.failure(
                        "The email is being used by someone else",
                        HttpStatus.BAD_REQUEST.value()
                );

            _userRepo.updateEmail(userToken, email);

            String token = _tokenRepo.createToken(
                    userToken,
                    TokenTypeEnum.EMAIL_UPDATE,
                    60
            );

            _emailServices.sendEmailChangeVerification(email, token);

            return ResultHandler.success(
                    "Verification link sent to the new email",
                    HttpStatus.OK.value()
            );
        } catch (RuntimeException rex) {
            return ResultHandler.failure(
                    rex.getMessage(),
                    HttpStatus.NOT_FOUND.value()
            );
        } catch (Exception ex) {
            return ResultHandler.failure(
                    ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
    }

    @Override
    @Transactional
    public ResultHandler<String> confirmEmailChange(String userToken, String token) {
        try {
            boolean isValidToken = _tokenRepo.validateToken(userToken, token, TokenTypeEnum.EMAIL_UPDATE);

            if (!isValidToken)
                return ResultHandler.failure(
                        "Invalid or expired token",
                        HttpStatus.BAD_REQUEST.value()
                );

            boolean isConfirmed = _userRepo.confirmEmailChange(userToken);

            if (!isConfirmed)
                return ResultHandler.failure(
                        "No pending email update found",
                        HttpStatus.BAD_REQUEST.value()
                );
            return ResultHandler.success(
                    "Email updated successfully",
                    HttpStatus.OK.value()
            );
        } catch (Exception ex) {
            return ResultHandler.failure(
                    ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
    }

    @Override
    @Transactional
    public ResultHandler<String> updateUserName(String userName, String userToken) {
        try {
            if (_userRepo.existsByUsername(userName))
                return ResultHandler.failure(
                        "Username is already taken",
                        HttpStatus.BAD_REQUEST.value()
                );
            
            boolean isChanged = _userRepo.changeUserName(userToken, userName);

            if (!isChanged)
                return ResultHandler.failure(
                        "Can't change user name",
                        HttpStatus.BAD_REQUEST.value()
                );

            return ResultHandler.success(
                    "User name changed successfully",
                    HttpStatus.OK.value()
            );
        } catch (RuntimeException rex) {
            return ResultHandler.failure(
                    rex.getMessage(),
                    HttpStatus.NOT_FOUND.value()
            );
        } catch (Exception ex) {
            return ResultHandler.failure(
                    ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
    }
}
