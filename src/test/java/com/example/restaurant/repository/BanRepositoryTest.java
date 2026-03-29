package com.example.restaurant.repository;

import com.example.restaurant.exceptions.StatusNotFoundException;
import com.example.restaurant.models.Bans;
import com.example.restaurant.models.lookup.BanStatus;
import com.example.restaurant.repository.interfaces.jpa.IJpaBanRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaBanStatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BanRepositoryTest {

    @Mock
    private IJpaBanStatusRepository _jpaStatusRepo;
    @Mock
    private IJpaBanRepository _jpaBanRepo;

    @InjectMocks
    private BanRepository _banRepository;

    @Test
    void findStatusByToken_ShouldReturnStatus_WhenExists() {
        String token = "ACTIVE";
        BanStatus status = new BanStatus();
        when(_jpaStatusRepo.findByToken(token)).thenReturn(Optional.of(status));

        BanStatus result = _banRepository.findStatusByToken(token);

        assertNotNull(result);
        assertEquals(status, result);
        verify(_jpaStatusRepo, times(1)).findByToken(token);
    }

    @Test
    void findStatusByToken_ShouldThrowException_WhenNotFound() {
        String token = "NON_EXISTENT";
        when(_jpaStatusRepo.findByToken(token)).thenReturn(Optional.empty());

        StatusNotFoundException exception = assertThrows(StatusNotFoundException.class, () -> {
            _banRepository.findStatusByToken(token);
        });

        assertEquals("Ban status not found", exception.getMessage());
        verify(_jpaStatusRepo, times(1)).findByToken(token);
    }

    @Test
    void save_ShouldCallJpaRepositorySave() {
        Bans ban = new Bans();

        _banRepository.save(ban);

        verify(_jpaBanRepo, times(1)).save(ban);
    }
}