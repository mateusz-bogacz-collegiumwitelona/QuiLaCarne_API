package com.example.restaurant.tasks;

import com.example.restaurant.services.interfaces.IBanServices;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BanSchedulerTest {

    @Mock
    private IBanServices _banServices;

    @InjectMocks
    private BanScheduler _banScheduler;

    @Test
    @DisplayName("unban: Should trigger processing of expired bans in BanServices")
    void unban_ShouldCallProcessExpiredBans() {
        _banScheduler.unban();
        verify(_banServices, times(1)).processExpiredBans();
    }
}