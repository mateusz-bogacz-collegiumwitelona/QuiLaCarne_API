package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.Bans;
import com.example.restaurant.models.lookup.BanStatus;

public interface IBanRepository {
    BanStatus findStatusByToken(String token);

    void save(Bans ban);
}
