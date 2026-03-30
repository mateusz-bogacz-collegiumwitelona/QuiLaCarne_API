package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.domain.CreateBanDomain;
import com.example.restaurant.dto.request.CreateBanRequest;
import com.example.restaurant.helpers.ResultHandler;

public interface IBanServices {
    ResultHandler<Void> create(String adminToken, CreateBanRequest request);
    
    void create(CreateBanDomain domain);
}
