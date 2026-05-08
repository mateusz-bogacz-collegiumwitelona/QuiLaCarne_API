package com.example.restaurant.repository.interfaces;

import com.example.restaurant.models.Bans;
import com.example.restaurant.models.lookup.BanStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IBanRepository {
  BanStatus findStatusByToken(String token);

  void save(Bans ban);

  List<BanStatus> findAllStatuses();

  List<Bans> findExpiredActiveBans(OffsetDateTime time);

  long countStatuses();

  long count();

  Page<Bans> findAll(Pageable pageable);
}
