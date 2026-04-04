package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.domain.CreateBanDomain;
import com.example.restaurant.dto.request.CreateBanRequest;
import com.example.restaurant.dto.response.EntityResponse;
import com.example.restaurant.helpers.DictionaryHelper;
import com.example.restaurant.models.Bans;
import com.example.restaurant.models.Users;
import com.example.restaurant.repository.interfaces.IBanRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IBanServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BanServices implements IBanServices {
    private final IBanRepository _banRepo;
    private final EmailServices _emailServices;
    private final IUserRepository _userRepo;

    private static final String ROLE_CLIENT = "ROLE_CLIENT";
    private static final String ROLE_MANAGER = "ROLE_MANAGER";
    private static final String STATUS_ACTIVE = "ACTIVE";

    @Override
    @Transactional
    @Auditable(action = "BAN_USER")
    public void create(String adminToken, CreateBanRequest request) {
        if (adminToken.equals(request.getClientToken()))
            throw new IllegalStateException("You cannot ban yourself");


        var admin = _userRepo.findByToken(adminToken);
        var client = _userRepo.findByToken(request.getClientToken());

        validatePermissions(admin, client);

        if (!client.getIsActive())
            throw new IllegalStateException("User is already inactive or banned");

        CreateBanDomain banDomain = new CreateBanDomain(
                client,
                admin,
                request.getReason(),
                request.getExpiresAt()
        );

        create(banDomain);
    }

    @Override
    @Transactional
    public void create(CreateBanDomain domain) {
        var status = _banRepo.findStatusByToken(STATUS_ACTIVE);

        Bans ban = new Bans();
        ban.setUser(domain.client());
        ban.setBannedBy(domain.admin());
        ban.setReason(domain.reason());
        ban.setExpiresAt(domain.expiresAt());
        ban.setIsActive(true);
        ban.setBanStatuses(Set.of(status));

        domain.client().setIsActive(false);

        _banRepo.save(ban);
        _userRepo.save(domain.client());

        _emailServices.sendEmailSetBan(
                domain.client().getEmail(),
                domain.client().getUsername(),
                domain.reason()
        );
    }

    @Override
    public List<EntityResponse> getDictionary() {
        String lang = LocaleContextHolder.getLocale().getLanguage();
        return DictionaryHelper.map(_banRepo.findAllStatuses(), lang);
    }

    private void validatePermissions(Users admin, Users client) {
        boolean isAdminManager = admin.getRoles().stream()
                .anyMatch(r -> r.getName().equals(ROLE_MANAGER));

        if (!isAdminManager) {
            throw new IllegalStateException("Only managers can issue bans");
        }

        boolean isTargetClient = client.getRoles().stream()
                .anyMatch(r -> r.getName().equals(ROLE_CLIENT));

        if (!isTargetClient) {
            throw new IllegalStateException("Targeted user must be a client");
        }
    }
}
