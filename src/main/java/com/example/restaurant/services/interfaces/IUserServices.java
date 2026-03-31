package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.domain.UserDomain;
import com.example.restaurant.dto.request.*;

import java.util.Optional;

public interface IUserServices {
    String create(RegisterRequest request, String userRole, boolean isActive);

    void createEmployee(AddEmployeeRequest request);

    void activeUser(String userToken);

    Optional<UserDomain> findMinimalByEmail(String email);

    void changePassword(String token, String newPassword);

    void updatePassword(String userToken, ChangePasswordRequest request);

    void updateEmail(String userToken, String email);

    void confirmEmailChange(String userToken, String token);

    void updateUserName(String userName, String userToken);

    void deleteAccount(String userToken);

    void editEmployee(EditEmployeeRequest request);

    void changeEmployeePassword(String adminToken, ChangeEmployeePasswordRequest request);
}
