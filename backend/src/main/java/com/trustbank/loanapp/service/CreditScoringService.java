package com.trustbank.loanapp.service;

import com.trustbank.loanapp.config.AppProperties;
import com.trustbank.loanapp.domain.CheckResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/**
 * Loan eligibility scoring. Lives on the server only: the mobile app never sees the formula, and a
 * modified client cannot grade itself.
 *
 * <p>Coursework model: affordability (installment / income) plus an employment adjustment, mapped
 * onto the usual 300-850 band.
 */
@Service
public class CreditScoringService {

    private final int passScore;

    public CreditScoringService(AppProperties props) {
        this.passScore = props.credit().passScore();
    }

    public int score(BigDecimal amount, BigDecimal annualRatePercent, int termMonths, BigDecimal incomeMonthly,
            String employmentType) {
        BigDecimal installment = AmortizationCalculator.monthlyInstallment(amount, annualRatePercent, termMonths);
        // Share of monthly income the installment would eat. 0.30 is comfortable, 0.60+ is not.
        double burden = installment.divide(incomeMonthly, 4, RoundingMode.HALF_UP).doubleValue();
        double raw = 850 - (burden * 500);
        if ("FORMAL".equals(employmentType)) {
            raw += 25;
        } else if ("INFORMAL".equals(employmentType)) {
            raw -= 35;
        }
        return (int) Math.max(300, Math.min(850, Math.round(raw)));
    }

    public CheckResult resultFor(int score) {
        return score >= passScore ? CheckResult.PASSED : CheckResult.FAILED;
    }
}
