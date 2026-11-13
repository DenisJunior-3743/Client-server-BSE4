package com.trustbank.loanapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "applicant_profiles")
@Getter
@Setter
public class ApplicantProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "national_id", nullable = false)
    private String nationalId;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    /** {employer, jobTitle, employmentType} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "employment_info", nullable = false)
    private Map<String, String> employmentInfo = new LinkedHashMap<>();

    @Column(name = "income_monthly", nullable = false)
    private BigDecimal incomeMonthly;

    /** {district, city, street} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, String> address = new LinkedHashMap<>();

    @Version
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
