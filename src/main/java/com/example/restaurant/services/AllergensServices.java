package com.example.restaurant.services;

import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.repository.interfaces.IAllergensRepository;
import com.example.restaurant.services.interfaces.IAllergensServices;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AllergensServices implements IAllergensServices {
    private final IAllergensRepository _allergenRepo;

    @Override
    public List<EntityResponse> getDictionary() {
        String lang = LocaleContextHolder.getLocale().getLanguage();

        var respone = _allergenRepo.findAll();

        if (ObjectUtils.isEmpty(respone)) return new ArrayList<>();

        return respone.stream().map(r -> EntityResponse
                .builder()
                .name("pl".equalsIgnoreCase(lang) ? r.getNamePl() : r.getNameEn())
                .token(r.getToken())
                .build()
        ).toList();
    }
}
