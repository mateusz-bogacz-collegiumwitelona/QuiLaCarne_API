package com.example.restaurant.services;

import com.example.restaurant.dto.domain.CreateBanDomain;
import com.example.restaurant.models.Bans;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.BanStatus;
import com.example.restaurant.repository.interfaces.IBanRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BanServicesTest {

    @Mock
    private IBanRepository _banRepo;
    @Mock
    private EmailServices _emailServices;
    @Mock
    private IUserRepository _userRepo;

    @InjectMocks
    private BanServices _banServices;

    @Test
    void create_ShouldCreateBan_DeactivateUser_AndSendEmail() {
        Users client = new Users();
        client.setUsername("bad_user");
        client.setEmail("bad@example.com");
        client.setIsActive(true);

        Users admin = new Users();
        admin.setUsername("admin");

        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);
        CreateBanDomain domain = new CreateBanDomain(client, admin, "Spam and rudeness", expiresAt);

        BanStatus activeStatus = new BanStatus();
        when(_banRepo.findStatusByToken("ACTIVE")).thenReturn(activeStatus);

        _banServices.create(domain);

        assertFalse(client.getIsActive());
        verify(_userRepo, times(1)).save(client);

        ArgumentCaptor<Bans> banCaptor = ArgumentCaptor.forClass(Bans.class);
        verify(_banRepo, times(1)).save(banCaptor.capture());

        Bans savedBan = banCaptor.getValue();
        assertEquals(client, savedBan.getUser());
        assertEquals(admin, savedBan.getBannedBy());
        assertEquals("Spam and rudeness", savedBan.getReason());
        assertEquals(expiresAt, savedBan.getExpiresAt());
        assertTrue(savedBan.getIsActive());
        assertTrue(savedBan.getBanStatuses().contains(activeStatus));

        verify(_emailServices, times(1)).sendEmailSetBan(
                "bad@example.com",
                "bad_user",
                "Spam and rudeness"
        );
    }
}