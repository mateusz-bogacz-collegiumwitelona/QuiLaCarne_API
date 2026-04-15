package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.domain.CreateBanDomain;
import com.example.restaurant.dto.request.CreateBanRequest;
import com.example.restaurant.dto.response.DictionaryResponse;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects", "PMD.GodClass"})
public interface IBanServices {
    void create(String adminToken, CreateBanRequest request);

    void add(CreateBanDomain domain);

    DictionaryResponse getDictionary();

    void processExpiredBans();
}
