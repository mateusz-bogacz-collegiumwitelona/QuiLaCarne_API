package com.example.restaurant.services;

import com.example.restaurant.dto.domain.CreateBanDomain;
import com.example.restaurant.dto.request.CreateBanRequest;
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
import org.springframework.http.HttpStatus;

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
    void create_ShouldProcessRequestAndReturnSuccess() {
        String adminToken = "ADMIN_TOKEN";
        String clientToken = "CLIENT_TOKEN";

        CreateBanRequest request = new CreateBanRequest();
        request.setClientToken(clientToken);
        request.setReason("Rude behavior");
        request.setExpiresAt(OffsetDateTime.now().plusDays(10));

        Users admin = new Users();
        admin.setToken(adminToken);
        admin.setUsername("adminUser");

        Users client = new Users();
        client.setToken(clientToken);
        client.setUsername("badClient");
        client.setEmail("client@example.com");

        BanStatus activeStatus = new BanStatus();
        
        when(_userRepo.isInRole("ROLE_CLIENT", clientToken)).thenReturn(true);
        when(_userRepo.findByToken(adminToken)).thenReturn(admin);
        when(_userRepo.findByToken(clientToken)).thenReturn(client);
        when(_banRepo.findStatusByToken("ACTIVE")).thenReturn(activeStatus);

        var result = _banServices.create(adminToken, request);

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals("Ban created successfully", result.getMessage());

        assertFalse(client.getIsActive());
        verify(_userRepo, times(1)).save(client);

        verify(_emailServices, times(1)).sendEmailSetBan(
                "client@example.com",
                "badClient",
                "Rude behavior"
        );
    }

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