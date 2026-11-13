package com.trustbank.loanapp.service;

import com.trustbank.loanapp.config.AppProperties;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Stand-in for the external credit bureau. The real thing is a slow network call with unpredictable
 * latency, which is exactly why it must never run on a request thread or inside a database
 * transaction (see CreditCheckWorker).
 */
@Component
public class CreditBureauClient {

    public record Query(UUID applicationId, BigDecimal amount, BigDecimal annualRatePercent, int termMonths,
            BigDecimal incomeMonthly, String employmentType) {}

    public record Report(String bureauReference, int score) {}

    private final CreditScoringService scoring;
    private final Duration latency;
    private final SecureRandom random = new SecureRandom(); // SecureRandom is thread-safe

    public CreditBureauClient(CreditScoringService scoring, AppProperties props) {
        this.scoring = scoring;
        this.latency = props.credit().bureauLatency();
    }

    public Report check(Query query) throws InterruptedException {
        Thread.sleep(latency.toMillis()); // simulated bureau latency
        int score = scoring.score(query.amount(), query.annualRatePercent(), query.termMonths(),
                query.incomeMonthly(), query.employmentType());
        return new Report("CB-" + (100_000 + random.nextInt(900_000)), score);
    }
}
