package com.example.restaurant.services.interfaces;

import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.request.AddTableRequest;
import com.example.restaurant.dto.request.TableFilterRequest;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.dto.response.TableListWrapperResponse;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects", "PMD.GodClass"})
public interface ITableServices {
    TableListWrapperResponse getTables(TableFilterRequest request);

    void changeStatusToClean(String tableToken);

    void changeStatusToOutOfService(String tableToken);

    void add(AddTableRequest request);

    void delete(String token);

    DictionaryResponse getDictionary();

    void addStatus(AddEntityRequest request);

    void removeStatus(String token);
}
