package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.CreateBanRequest;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.BanStatus;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.IBanRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Set;

import static com.example.restaurant.TestConstants.CLIENT_TOKEN;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @BeforeEach
    void setUp() {
        admin = new Users();
        admin.setToken(TestConstants.FAKE_USER_TOKEN);
        admin.setRoles(Set.of(createRole(TestConstants.ROLE_MANAGER)));

        client = new Users();
        client.setToken(CLIENT_TOKEN);
        client.setIsActive(true);
        client.setEmail(TestConstants.FAKE_EMAIL);
        client.setUsername(TestConstants.FAKE_USERNAME);
        client.setRoles(Set.of(createRole(TestConstants.ROLE_CLIENT)));
    }

    @Test
    void create_ShouldSucceed_WhenDataIsValid() {
        CreateBanRequest request = new CreateBanRequest();
        request.setClientToken(CLIENT_TOKEN);
        request.setReason("Violation");
        request.setExpiresAt(OffsetDateTime.now().plusDays(1));

        when(_userRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(admin);
        when(_userRepo.findByToken(CLIENT_TOKEN)).thenReturn(client);
        when(_banRepo.findStatusByToken("ACTIVE")).thenReturn(new BanStatus());

        var result = _banServices.create(TestConstants.FAKE_USER_TOKEN, request);

        assertTrue(result.isSuccess());
        verify(_emailServices).sendEmailSetBan(
                TestConstants.FAKE_EMAIL,
                TestConstants.FAKE_USERNAME,
                "Violation"
        );
    }

    @Test
    void create_ShouldThrowException_WhenAdminBansSelf() {
        CreateBanRequest request = new CreateBanRequest();
        request.setClientToken(TestConstants.FAKE_USER_TOKEN);

        assertThrows(IllegalStateException.class, () ->
                _banServices.create(TestConstants.FAKE_USER_TOKEN, request)
        );
    }

    private Roles createRole(String roleName) {
        Roles role = new Roles();
        role.setName(roleName);
        return role;
    }
}