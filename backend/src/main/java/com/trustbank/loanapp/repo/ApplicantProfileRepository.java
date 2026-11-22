package com.trustbank.loanapp.repo;

import com.trustbank.loanapp.domain.ApplicantProfile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicantProfileRepository extends JpaRepository<ApplicantProfile, UUID> {

    Optional<ApplicantProfile> findByUserId(UUID userId);

    List<ApplicantProfile> findAllByUserIdIn(Collection<UUID> userIds);

    boolean existsByNationalIdAndUserIdNot(String nationalId, UUID userId);
}
