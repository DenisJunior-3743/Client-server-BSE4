package com.trustbank.loanapp.messaging;

import io.swagger.v3.oas.annotations.Operation;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/messaging")
@PreAuthorize("hasRole('ADMIN')")
public class MessagingController {
    private final LoanMessageStore store;
    private final ObjectProvider<LoanOutboxRelay> relay;
    private final boolean enabled;
    private final boolean worker;

    public MessagingController(LoanMessageStore store, ObjectProvider<LoanOutboxRelay> relay,
            @Value("${app.messaging.enabled:false}") boolean enabled,
            @Value("${app.messaging.worker-enabled:false}") boolean worker) {
        this.store = store; this.relay = relay; this.enabled = enabled; this.worker = worker;
    }

    @GetMapping
    @Operation(summary = "Inspect durable message counts and configured transport (admin only)")
    public Map<String, Object> status() {
        var result = new LinkedHashMap<>(store.stats());
        result.put("transport", enabled ? "rabbitmq" : "embedded");
        result.put("workerEnabled", worker);
        return result;
    }

    @PostMapping("/publish")
    @Operation(summary = "Publish up to ten pending events; production delivery uses a persistent worker")
    public Map<String, Object> publish() {
        var publisher = relay.getIfAvailable();
        if (publisher == null) throw com.trustbank.loanapp.common.ApiException.rule(
                "MESSAGING_DISABLED", "Enable RabbitMQ and configure a broker first");
        return Map.of("published", publisher.publishBatch());
    }
    @PostMapping("/{eventId}/retry")
    @Operation(summary = "Requeue an unconsumed event after resolving a dead-letter or delivery failure (admin only)")
    public Map<String, Object> retry(@org.springframework.web.bind.annotation.PathVariable java.util.UUID eventId) {
        if (!store.requestRetry(eventId)) throw com.trustbank.loanapp.common.ApiException.conflict(
                "MESSAGE_NOT_RETRYABLE", "The message does not exist or has already been consumed");
        return Map.of("queued", true);
    }
}
