package com.trustbank.loanapp.repo;

import com.trustbank.loanapp.domain.Notification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderBySentAtDesc(UUID userId, Pageable page);

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);
}
