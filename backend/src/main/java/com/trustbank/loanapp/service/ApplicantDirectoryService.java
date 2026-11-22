package com.trustbank.loanapp.service;

import com.trustbank.loanapp.domain.ApplicantProfile;
import com.trustbank.loanapp.domain.Role;
import com.trustbank.loanapp.domain.User;
import com.trustbank.loanapp.repo.ApplicantProfileRepository;
import com.trustbank.loanapp.repo.UserRepository;
import com.trustbank.loanapp.web.dto.AuthDtos.ApplicantDto;
import com.trustbank.loanapp.web.dto.AuthDtos.ApplicantPageDto;
import com.trustbank.loanapp.web.dto.DtoMapper;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicantDirectoryService {
    private final UserRepository users;
    private final ApplicantProfileRepository profiles;

    public ApplicantDirectoryService(UserRepository users, ApplicantProfileRepository profiles) {
        this.users = users;
        this.profiles = profiles;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ApplicantPageDto list(int page, int size) {
        var borrowers = users.findByRoleOrderByCreatedAtDescIdDesc(Role.BORROWER, PageRequest.of(page, size));
        var ids = borrowers.getContent().stream().map(User::getId).toList();
        Map<UUID, ApplicantProfile> byUser = ids.isEmpty() ? Map.of()
                : profiles.findAllByUserIdIn(ids).stream()
                        .collect(Collectors.toMap(ApplicantProfile::getUserId, Function.identity()));
        var items = borrowers.getContent().stream().map(user -> {
            var profile = byUser.get(user.getId());
            return new ApplicantDto(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                    user.isPhoneVerified(), user.isActive(), user.getCreatedAt(),
                    profile == null ? null : DtoMapper.profile(profile, true));
        }).toList();
        return new ApplicantPageDto(items, borrowers.getNumber(), borrowers.getSize(),
                borrowers.getTotalElements(), borrowers.getTotalPages());
    }
}
