package com.example.restaurant.services;

import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.mappers.DictionaryMapper;
import com.example.restaurant.repository.interfaces.IAllergensRepository;
import com.example.restaurant.services.interfaces.IAllergensServices;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AllergensServices implements IAllergensServices {
    private final IAllergensRepository _allergenRepo;

    @Override
    public List<EntityResponse> getDictionary() {
        String lang = LocaleContextHolder.getLocale().getLanguage();
        return DictionaryMapper.map(_allergenRepo.findAll(), lang);
    }
}
