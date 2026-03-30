package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.CreateBanRequest;
import com.example.restaurant.models.Bans;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.BanStatus;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.IBanRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Set;

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

    private Users admin;
    private Users client;
    private final String CLIENT_TOKEN = "client-token-123";

    @BeforeEach
    void setUp() {
        admin = new Users();
        admin.setToken(TestConstants.FAKE_USER_TOKEN);
        admin.setRoles(Set.of(createRole("ROLE_MANAGER")));

        client = new Users();
        client.setToken(CLIENT_TOKEN);
        client.setIsActive(true);
        client.setEmail(TestConstants.FAKE_EMAIL);
        client.setUsername(TestConstants.FAKE_USERNAME);
        client.setRoles(Set.of(createRole("ROLE_CLIENT")));
    }

    @Test
    @DisplayName("Ban Create: Success - Should deactivate user and save ban")
    void create_ShouldSucceed_WhenDataIsValid() {
        CreateBanRequest request = new CreateBanRequest();
        request.setClientToken(CLIENT_TOKEN);
        request.setReason("Violation");
        request.setExpiresAt(OffsetDateTime.now().plusDays(1));

        when(_userRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(admin);
        when(_userRepo.findByToken(CLIENT_TOKEN)).thenReturn(client);
        when(_banRepo.findStatusByToken("ACTIVE")).thenReturn(new BanStatus());

        assertDoesNotThrow(() -> _banServices.create(TestConstants.FAKE_USER_TOKEN, request));

        assertFalse(client.getIsActive());
        verify(_banRepo).save(any(Bans.class));
        verify(_userRepo).save(client);
        verify(_emailServices).sendEmailSetBan(anyString(), anyString(), eq("Violation"));
    }

    @Test
    @DisplayName("Ban Create: Failure - Admin cannot ban themselves")
    void create_ShouldThrowException_WhenAdminBansSelf() {
        CreateBanRequest request = new CreateBanRequest();
        request.setClientToken(TestConstants.FAKE_USER_TOKEN);

        assertThrows(IllegalStateException.class, () ->
                _banServices.create(TestConstants.FAKE_USER_TOKEN, request)
        );
    }

    @Test
    @DisplayName("Ban Create: Failure - Target is not a client")
    void create_ShouldThrowException_WhenTargetIsNotClient() {
        client.setRoles(Set.of(createRole("ROLE_WAITER")));
        when(_userRepo.findByToken(anyString())).thenReturn(admin).thenReturn(client);

        CreateBanRequest request = new CreateBanRequest();
        request.setClientToken(CLIENT_TOKEN);

        assertThrows(IllegalStateException.class, () -> _banServices.create(admin.getToken(), request));
    }

    private Roles createRole(String roleName) {
        Roles role = new Roles();
        role.setName(roleName);
        return role;
    }
}