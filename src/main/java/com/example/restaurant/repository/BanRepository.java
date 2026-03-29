package com.example.restaurant.repository;

import com.example.restaurant.exceptions.StatusNotFoundException;
import com.example.restaurant.models.Bans;
import com.example.restaurant.models.lookup.BanStatus;
import com.example.restaurant.repository.interfaces.IBanRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaBanRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaBanStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BanRepository implements IBanRepository {
    private final IJpaBanStatusRepository _jpaStatusRepo;
    private final IJpaBanRepository _jpaBanRepo;

    @Override
    public BanStatus findStatusByToken(String token) {
        return _jpaStatusRepo.findByToken(token)
                .orElseThrow(() -> new StatusNotFoundException("Ban status not found"));
    }

    @Override
    public void save(Bans ban) {
        _jpaBanRepo.save(ban);
    }
}
