package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.models.Bans;
import com.example.restaurant.models.lookup.BanStatus;
import com.example.restaurant.repository.interfaces.jpa.IJpaBanRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaBanStatusRepository;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Find status: Return status if exist")
    void findStatusByToken_ShouldReturnStatus_WhenExists() {
        String token = TestConstants.STATUS_ACTIVE;
        BanStatus status = new BanStatus();
        when(_jpaStatusRepo.findByToken(token)).thenReturn(Optional.of(status));

        BanStatus result = _banRepository.findStatusByToken(token);

        assertNotNull(result);
        assertEquals(status, result);
        verify(_jpaStatusRepo, times(1)).findByToken(token);
    }

    @Test
    @DisplayName("Find allergens: throw execption when not found")
    void findStatusByToken_ShouldThrowException_WhenNotFound() {
        String token = TestConstants.TOKEN_NON_EXISTENT;
        when(_jpaStatusRepo.findByToken(token)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> _banRepository.findStatusByToken(token));

        assertEquals("Ban status not found", exception.getMessage());
        verify(_jpaStatusRepo, times(1)).findByToken(token);
    }

    @Test
    @DisplayName("Save: Success")
    void save_ShouldCallJpaRepositorySave() {
        Bans ban = new Bans();

        _banRepository.save(ban);

        verify(_jpaBanRepo, times(1)).save(ban);
    }
}