package com.example.restaurant.dto.request;

import lombok.Data;

@Data
public class EditEmployeeRequest {
  private String employeeToken;
  private String email;
  private String userName;
}
