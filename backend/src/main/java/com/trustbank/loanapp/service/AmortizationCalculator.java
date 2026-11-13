package com.trustbank.loanapp.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Reducing-balance loan arithmetic. Server-side only (fat server for money rules): a client may
 * display these numbers but never computes the ones that count.
 *
 * <p>Pure functions on immutable BigDecimals: no fields, so nothing to share between threads.
 * Amounts are whole Uganda shillings.
 */
public final class AmortizationCalculator {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal TWELVE_HUNDRED = BigDecimal.valueOf(1200);

    private AmortizationCalculator() {}

    public record Installment(int number, BigDecimal principal, BigDecimal interest, BigDecimal total) {}

    /** Equal monthly installment: P * r / (1 - (1 + r)^-n), with r the monthly rate. */
    public static BigDecimal monthlyInstallment(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        BigDecimal r = annualRatePercent.divide(TWELVE_HUNDRED, MC);
        if (r.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(months), 0, RoundingMode.HALF_UP);
        }
        BigDecimal growth = BigDecimal.ONE.add(r).pow(months, MC);
        BigDecimal factor = r.multiply(growth, MC).divide(growth.subtract(BigDecimal.ONE), MC);
        return principal.multiply(factor, MC).setScale(0, RoundingMode.HALF_UP);
    }

    /** Full schedule. The last installment absorbs rounding so the principal sums exactly. */
    public static List<Installment> schedule(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        BigDecimal r = annualRatePercent.divide(TWELVE_HUNDRED, MC);
        BigDecimal installment = monthlyInstallment(principal, annualRatePercent, months);
        BigDecimal balance = principal.setScale(0, RoundingMode.HALF_UP);
        List<Installment> rows = new ArrayList<>(months);
        for (int n = 1; n <= months; n++) {
            BigDecimal interest = balance.multiply(r, MC).setScale(0, RoundingMode.HALF_UP);
            BigDecimal principalPart = n == months ? balance : installment.subtract(interest).min(balance);
            rows.add(new Installment(n, principalPart, interest, principalPart.add(interest)));
            balance = balance.subtract(principalPart);
        }
        return rows;
    }
}
