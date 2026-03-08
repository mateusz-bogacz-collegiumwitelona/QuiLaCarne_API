package com.example.restaurant.repository;

import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.repository.interfaces.ITableRespository;
import com.example.restaurant.repository.interfaces.jpa.IJpaTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TableRespository implements ITableRespository {
    private final IJpaTableRepository _jpaTableRepo;

    @Override
    public List<TableListResponse> findAllTables(String lang) {
        return _jpaTableRepo.findAll()
                .stream()
                .map(table -> TableListResponse.builder()
                        .token(table.getToken())
                        .tableNuber(table.getTableNumber())
                        .capacity(table.getCapacity())
                        .status(table.getTableStatus()
                                .stream()
                                .findFirst()
                                .map(s -> s.translate(lang))
                                .orElse("UNKNOWN")
                        )
                        .updatedAt(table.getUpdatedAt())
                        .build()
                )
                .toList();
    }
}
