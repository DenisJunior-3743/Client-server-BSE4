package com.trustbank.loanapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AmortizationCalculatorTest {

    private static final BigDecimal FIVE_MILLION = new BigDecimal("5000000");
    private static final BigDecimal RATE_19 = new BigDecimal("19.00");

    @Test
    void monthlyInstallmentMatchesTheReducingBalanceFormula() {
        // 5,000,000 UGX at 19% a year over 12 months
        assertEquals(new BigDecimal("460783"), AmortizationCalculator.monthlyInstallment(FIVE_MILLION, RATE_19, 12));
    }

    @Test
    void principalPartsAddUpToExactlyTheAmountLent() {
        List<AmortizationCalculator.Installment> rows = AmortizationCalculator.schedule(FIVE_MILLION, RATE_19, 12);
        assertEquals(12, rows.size());
        BigDecimal principal = rows.stream().map(AmortizationCalculator.Installment::principal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, principal.compareTo(FIVE_MILLION));
    }

    @Test
    void interestFallsEveryMonthAsTheBalanceReduces() {
        List<AmortizationCalculator.Installment> rows = AmortizationCalculator.schedule(FIVE_MILLION, RATE_19, 12);
        for (int i = 1; i < rows.size(); i++) {
            assertTrue(rows.get(i).interest().compareTo(rows.get(i - 1).interest()) < 0);
        }
    }

    @Test
    void everyInstallmentIsPrincipalPlusInterestInWholeShillings() {
        for (AmortizationCalculator.Installment row : AmortizationCalculator.schedule(new BigDecimal("777777"),
                new BigDecimal("24.00"), 7)) {
            assertEquals(0, row.total().compareTo(row.principal().add(row.interest())));
            assertTrue(row.total().stripTrailingZeros().scale() <= 0);
            assertTrue(row.total().signum() > 0);
        }
    }

    @Test
    void oneMonthLoanIsRepaidInASingleInstallment() {
        List<AmortizationCalculator.Installment> rows = AmortizationCalculator.schedule(new BigDecimal("200000"),
                new BigDecimal("24.00"), 1);
        assertEquals(1, rows.size());
        assertEquals(0, rows.get(0).principal().compareTo(new BigDecimal("200000")));
        assertEquals(0, rows.get(0).interest().compareTo(new BigDecimal("4000")));
    }
}
