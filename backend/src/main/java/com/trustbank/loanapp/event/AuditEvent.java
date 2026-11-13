package com.trustbank.loanapp.event;

import java.util.Map;
import java.util.UUID;

/** Something worth an audit-trail row that is not a loan status change (staff created, document verified...). */
public record AuditEvent(String entityType, UUID entityId, String action, UUID actorId, Map<String, Object> details) {}
