package com.trustbank.loanapp.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed view of the {@code app.*} block in application.yml. Immutable, so safe to share across threads. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Cors cors,
        Security security,
        Otp otp,
        Offer offer,
        Idempotency idempotency,
        Credit credit,
        Async async,
        boolean seedDemoData) {

    public record Jwt(String secret, Duration accessTokenTtl, String issuer) {}

    public record Cors(List<String> allowedOrigins) {}

    public record Security(boolean requireHttps, long maxJsonBodyBytes, RateLimit rateLimit) {}

    public record RateLimit(int requestsPerMinute, int authRequestsPerMinute) {}

    public record Otp(Duration ttl, int maxAttempts, boolean devEcho) {}

    public record Offer(Duration acceptanceWindow) {}

    public record Idempotency(Duration ttl) {}

    public record Credit(int passScore, Duration bureauLatency) {}

    public record Async(int corePoolSize, int maxPoolSize, int queueCapacity) {}
}
