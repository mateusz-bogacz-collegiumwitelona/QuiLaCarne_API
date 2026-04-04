package com.example.restaurant.repository;

import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.models.Bans;
import com.example.restaurant.models.lookup.BanStatus;
import com.example.restaurant.repository.interfaces.IBanRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaBanRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaBanStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class BanRepository implements IBanRepository {
    private final IJpaBanStatusRepository _jpaStatusRepo;
    private final IJpaBanRepository _jpaBanRepo;

    @Override
    public BanStatus findStatusByToken(String token) {
        return _jpaStatusRepo.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Ban status not found"));
    }

    @Override
    public void save(Bans ban) {
        _jpaBanRepo.save(ban);
    }

    @Override
    public List<BanStatus> findAllStatuses() {
        return _jpaStatusRepo.findAll();
    }

    @Override
    public List<Bans> findExpiredActiveBans(OffsetDateTime time) {
        return _jpaBanRepo.findByIsActiveTrueAndExpiresAtBefore(time);
    }
}
