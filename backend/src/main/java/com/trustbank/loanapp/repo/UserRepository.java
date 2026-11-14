package com.trustbank.loanapp.repo;

import com.trustbank.loanapp.domain.Role;
import com.trustbank.loanapp.domain.User;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRoleInOrderByCreatedAtAsc(Collection<Role> roles);

    List<User> findByActiveTrueAndRoleInOrderByCreatedAtAsc(Collection<Role> roles);

    /** Lock admins in a stable order so concurrent removals cannot remove the last administrator. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.active = true and u.role = :role order by u.id")
    List<User> findActiveByRoleForUpdate(@Param("role") Role role);

    long countByRoleIn(Collection<Role> roles);
}
