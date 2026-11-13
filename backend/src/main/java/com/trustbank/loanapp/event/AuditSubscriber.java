package com.trustbank.loanapp.event;

import com.trustbank.loanapp.config.AsyncConfig;
import com.trustbank.loanapp.domain.AuditLog;
import com.trustbank.loanapp.repo.AuditLogRepository;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Subscriber 2: the audit trail. Knows nothing about notifications, and the services know nothing about it. */
@Component
public class AuditSubscriber {

    private static final Logger log = LoggerFactory.getLogger(AuditSubscriber.class);

    private final AuditLogRepository auditLogs;
    private final Clock clock;

    public AuditSubscriber(AuditLogRepository auditLogs, Clock clock) {
        this.auditLogs = auditLogs;
        this.clock = clock;
    }

    @Async(AsyncConfig.BACKGROUND_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLoanStatusChanged(LoanStatusChangedEvent event) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reference", event.reference());
        details.put("from", event.from() == null ? null : event.from().name());
        details.put("to", event.to().name());
        if (event.note() != null) {
            details.put("note", event.note());
        }
        write("LOAN_APPLICATION", event.applicationId(), "STATUS_" + event.to().name(), event.actorId(), details);
    }

    @Async(AsyncConfig.BACKGROUND_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAuditEvent(AuditEvent event) {
        write(event.entityType(), event.entityId(), event.action(), event.actorId(), event.details());
    }

    private void write(String entityType, UUID entityId, String action, UUID actorId, Map<String, Object> details) {
        try {
            AuditLog entry = new AuditLog();
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setAction(action);
            entry.setPerformedBy(actorId);
            entry.setPerformedAt(clock.instant());
            entry.setDetails(details == null ? new LinkedHashMap<>() : new LinkedHashMap<>(details));
            auditLogs.save(entry);
        } catch (RuntimeException ex) {
            log.error("Could not write audit entry {} for {} {}", action, entityType, entityId, ex);
        }
    }
}
