package com.example.restaurant.services;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.domain.CreateBanDomain;
import com.example.restaurant.dto.request.CreateBanRequest;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.models.Bans;
import com.example.restaurant.repository.interfaces.IBanRepository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IBanServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BanServices implements IBanServices {
    private final IBanRepository _banRepo;
    private final EmailServices _emailServices;
    private final IUserRepository _userRepo;

    @Override
    @Transactional
    @Auditable(action = "BAN_USER")
    public ResultHandler<Void> create(String adminToken, CreateBanRequest request) {
        if (!_userRepo.isInRole("ROLE_CLIENT", request.getClientToken())) {
            return ResultHandler.failure(
                    "You can only ban users with the client role",
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        
        var admin = _userRepo.findByToken(adminToken);
        var client = _userRepo.findByToken(request.getClientToken());

        CreateBanDomain banDomain = new CreateBanDomain(
                client,
                admin,
                request.getReason(),
                request.getExpiresAt()
        );

        create(banDomain);

        return ResultHandler.success(
                "Ban created successfully",
                HttpStatus.OK.value()
        );
    }

    @Override
    @Transactional
    public void create(CreateBanDomain domain) {
        var status = _banRepo.findStatusByToken("ACTIVE");

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
}
