package com.example.restaurant.helpers;

import java.util.ArrayList;
import java.util.List;

public class ResultHandler<T> {
    public  boolean isSuccess;
    public  T data;
    public String message;
    public  int statusCode;
    public List<String> errorMessages;


    public boolean isSuccess() { return isSuccess; }
    public T getData() { return data; }
    public String getMessage() { return message; }
    public int getStatusCode() { return statusCode; }
    public List<String> getErrorMessages() { return errorMessages; }

    // z data
    public static <T> ResultHandler<T> success(String message, int statusCode, T data) {
        ResultHandler<T> result = new ResultHandler<>();
        result.isSuccess = true;
        result.message = message;
        result.statusCode = statusCode;
        result.data = data;
        return result;
    }

    // bez data
    public static <T> ResultHandler<T> success(String message, int statusCode) {
        return success(message, statusCode, null);
    }

    // z data
    public static  <T> ResultHandler<T> failure(String message, int statusCode, List<String> errors) {
        ResultHandler<T> result = new ResultHandler<>();
        result.isSuccess = false;
        result.message = message;
        result.statusCode = statusCode;
        result.errorMessages = errors != null ? errors : new ArrayList<>();
        return result;
    }

    //bez data
    public static <T> ResultHandler<T> failure(String message, int statusCode) {
        return failure(message, statusCode, null);
    }
}