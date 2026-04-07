package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.CreateBanRequest;
import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.models.Bans;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.BanStatus;
import com.example.restaurant.models.lookup.Roles;
import com.example.restaurant.repository.interfaces.IBanRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BanServicesTest {
    @Mock
    private IBanRepository _banRepo;

    @Mock
    private EmailServices _emailServices;

    @Mock
    private IUserRepository _userRepo;

    @Mock
    private NotificationServices _notification;

    @InjectMocks
    private BanServices _banServices;

    private Users admin;
    private Users client;
    private final String CLIENT_TOKEN = "client-token-123";

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

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
        verify(_notification, times(1)).sendToTopic(eq("security/bans"), anyString());
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
        client.setRoles(Set.of(createRole(TestConstants.ROLE_WAITER)));
        when(_userRepo.findByToken(anyString())).thenReturn(admin).thenReturn(client);

        CreateBanRequest request = new CreateBanRequest();
        request.setClientToken(CLIENT_TOKEN);

        assertThrows(IllegalStateException.class, () -> _banServices.create(admin.getToken(), request));
    }

    @Test
    @DisplayName("getDictionary: Returns empty list when repository returns empty")
    void getDictionary_ShouldReturnEmptyList_WhenRepoReturnsEmpty() {
        when(_banRepo.findAllStatuses()).thenReturn(new java.util.ArrayList<>());

        List<EntityResponse> result = _banServices.getDictionary();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getDictionary: Returns mapped elements with Polish names when language is pl")
    void getDictionary_ShouldReturnPolishNames_WhenLanguageIsPl() {
        LocaleContextHolder.setLocale(new Locale(TestConstants.LANG_PL));

        BanStatus status = new BanStatus();
        status.setToken(TestConstants.STATUS_ACTIVE);
        status.setNamePl("Aktywny PL");
        status.setNameEn("Active EN");

        when(_banRepo.findAllStatuses()).thenReturn(List.of(status));

        java.util.List<EntityResponse> result = _banServices.getDictionary();

        assertEquals(1, result.size());
        assertEquals(TestConstants.STATUS_ACTIVE, result.getFirst().getToken());
        assertEquals("Aktywny PL", result.getFirst().getName());
    }

    @Test
    @DisplayName("getDictionary: Returns mapped elements with English names when language is not pl")
    void getDictionary_ShouldReturnEnglishNames_WhenLanguageIsNotPl() {
        LocaleContextHolder.setLocale(new Locale(TestConstants.LANG_EN));

        BanStatus status = new BanStatus();
        status.setToken(TestConstants.STATUS_EXPIRED);
        status.setNamePl("Wygasły PL");
        status.setNameEn("Expired EN");

        when(_banRepo.findAllStatuses()).thenReturn(List.of(status));

        java.util.List<EntityResponse> result = _banServices.getDictionary();

        assertEquals(1, result.size());
        assertEquals(TestConstants.STATUS_EXPIRED, result.getFirst().getToken());
        assertEquals("Expired EN", result.getFirst().getName());
    }

    private Roles createRole(String roleName) {
        Roles role = new Roles();
        role.setName(roleName);
        return role;
    }
}