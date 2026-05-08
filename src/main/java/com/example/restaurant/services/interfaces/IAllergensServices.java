package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.response.DictionaryResponse;

public interface IAllergensServices {
  DictionaryResponse getDictionary();

  void add(AddEntityRequest request);

  void remove(String token);
}
