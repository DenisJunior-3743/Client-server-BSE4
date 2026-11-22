package com.trustbank.loanapp.service;

import com.trustbank.loanapp.domain.ApplicantProfile;
import com.trustbank.loanapp.domain.Role;
import com.trustbank.loanapp.domain.User;
import com.trustbank.loanapp.repo.ApplicantProfileRepository;
import com.trustbank.loanapp.repo.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicantDirectoryServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final ApplicantProfileRepository profiles = mock(ApplicantProfileRepository.class);
    private final ApplicantDirectoryService directory = new ApplicantDirectoryService(users, profiles);

    @Test void pageIncludesBorrowersWithoutProfilesAndMasksNationalIds() {
        var newcomer = borrower("new@example.test");
        var existing = borrower("existing@example.test");
        var profile = new ApplicantProfile();
        profile.setUserId(existing.getId());
        profile.setNationalId("CM123456789012");
        var pageRequest = PageRequest.of(0, 2);
        when(users.findByRoleOrderByCreatedAtDescIdDesc(Role.BORROWER, pageRequest))
                .thenReturn(new PageImpl<>(List.of(newcomer, existing), pageRequest, 3));
        when(profiles.findAllByUserIdIn(List.of(newcomer.getId(), existing.getId())))
                .thenReturn(List.of(profile));

        var page = directory.list(0, 2);

        assertEquals(3, page.totalElements());
        assertEquals(2, page.totalPages());
        assertEquals(newcomer.getId(), page.items().getFirst().id());
        assertNull(page.items().getFirst().profile());
        var member = page.items().get(1);
        assertEquals(existing.getEmail(), member.email());
        assertEquals("CM" + "*".repeat(10) + "12", member.profile().nationalId());
        verify(profiles).findAllByUserIdIn(List.of(newcomer.getId(), existing.getId()));
    }

    private static User borrower(String email) {
        var user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(Role.BORROWER);
        user.setEmail(email);
        return user;
    }
}
