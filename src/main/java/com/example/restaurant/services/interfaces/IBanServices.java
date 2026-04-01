package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.domain.CreateBanDomain;
import com.example.restaurant.dto.request.CreateBanRequest;
import com.example.restaurant.dto.response.EntityResponse;

import java.util.List;

public interface IBanServices {
    void create(String adminToken, CreateBanRequest request);

    void create(CreateBanDomain domain);

    List<EntityResponse> getDictionary();
}
