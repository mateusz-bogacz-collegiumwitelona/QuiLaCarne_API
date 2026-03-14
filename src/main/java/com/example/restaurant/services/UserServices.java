package com.example.restaurant.services;

import com.example.restaurant.dto.request.UpdatePasswordRequest;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IUserServices;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServices implements IUserServices {
    private final IUserRepository _userRepo;

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
}
