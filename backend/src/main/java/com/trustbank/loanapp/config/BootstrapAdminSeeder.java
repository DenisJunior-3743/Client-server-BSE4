package com.trustbank.loanapp.config;

import com.trustbank.loanapp.domain.Role;
import com.trustbank.loanapp.domain.User;
import com.trustbank.loanapp.repo.UserRepository;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the initial admin once. Restarts never reset existing account passwords. */
@Component
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true", matchIfMissing = true)
public class BootstrapAdminSeeder implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;
    private final String email;
    private final String password;

    public BootstrapAdminSeeder(UserRepository users, PasswordEncoder encoder, JdbcTemplate jdbc,
            @Value("${app.bootstrap-admin.email:admin@bank.com}") String email,
            @Value("${app.bootstrap-admin.password:Admin123}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.jdbc = jdbc;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Serialize bootstrap across concurrent host instances using the SQL transaction.
        jdbc.execute("SELECT pg_advisory_xact_lock(817263540)");
        var existing = users.findByEmail(email);
        if (existing.isPresent()) {
            if (existing.get().getRole() != Role.ADMIN)
                throw new IllegalStateException("Bootstrap admin email belongs to a non-admin account");
            return;
        }
        if (email.isBlank() || password.length() < 8)
            throw new IllegalStateException("Bootstrap admin requires an email and a password of at least 8 characters");
        User admin = new User();
        admin.setEmail(email);
        admin.setPasswordHash(encoder.encode(password));
        admin.setFullName("Bank Administrator");
        admin.setPhone("");
        admin.setRole(Role.ADMIN);
        admin.setBranch("Head Office");
        admin.setAvatarColor("#1E3A8A");
        admin.setPhoneVerified(true);
        users.saveAndFlush(admin);
    }
}
