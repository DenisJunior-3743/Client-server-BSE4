package com.trustbank.loanapp.config;

import com.trustbank.loanapp.domain.Role;
import com.trustbank.loanapp.domain.User;
import com.trustbank.loanapp.repo.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the seven staff accounts of web/DEMO_CREDENTIALS.md on an empty database. Only runs when
 * {@code app.seed-demo-data=true} (the dev profile). Never enable it in production: the password is
 * public.
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_PASSWORD = "Passw0rd1";

    private record Seed(String email, String fullName, String phone, Role role, String branch, String color) {}

    private static final List<Seed> STAFF = List.of(
            new Seed("grace.officer@bankloan.ug", "Grace Namutebi", "0772345671", Role.LOAN_OFFICER, "Mbarara Branch", "#1E3A8A"),
            new Seed("ronald.officer@bankloan.ug", "Ronald Okello", "0772345672", Role.LOAN_OFFICER, "Kampala Branch", "#0F766E"),
            new Seed("sarah.analyst@bankloan.ug", "Sarah Atim", "0772345673", Role.CREDIT_ANALYST, "Mbarara Branch", "#B45309"),
            new Seed("david.analyst@bankloan.ug", "David Mugisha", "0772345674", Role.CREDIT_ANALYST, "Kampala Branch", "#7C3AED"),
            new Seed("esther.manager@bankloan.ug", "Esther Kirabo", "0772345675", Role.BRANCH_MANAGER, "Mbarara Branch", "#BE123C"),
            new Seed("peter.manager@bankloan.ug", "Peter Ssekandi", "0772345676", Role.BRANCH_MANAGER, "Kampala Branch", "#0369A1"),
            new Seed("admin@bankloan.ug", "Josephine Nabirye", "0772345677", Role.ADMIN, "Head Office", "#1E3A8A"));

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        int created = 0;
        for (Seed seed : STAFF) {
            if (users.existsByEmail(seed.email())) {
                continue;
            }
            User user = new User();
            user.setEmail(seed.email());
            user.setPasswordHash(hash);
            user.setFullName(seed.fullName());
            user.setPhone(seed.phone());
            user.setRole(seed.role());
            user.setBranch(seed.branch());
            user.setAvatarColor(seed.color());
            user.setPhoneVerified(true);
            users.save(user);
            created++;
        }
        if (created > 0) {
            log.warn("Seeded {} demo staff accounts (password in web/DEMO_CREDENTIALS.md). Dev only.", created);
        }
    }
}
