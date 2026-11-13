package com.trustbank.loanapp.repo;

import com.trustbank.loanapp.domain.Role;
import com.trustbank.loanapp.domain.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRoleInOrderByCreatedAtAsc(Collection<Role> roles);

    long countByRoleIn(Collection<Role> roles);
}
