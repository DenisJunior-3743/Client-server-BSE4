package com.trustbank.loanapp.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.ConfirmType;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** Real PostgreSQL/Flyway, with optional real RabbitMQ. Uses and removes its own isolated schema. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = ".+")
class LoanMessagingIntegrationTest {
    private final String schema = "messaging_test_" + UUID.randomUUID().toString().replace("-", "");
    private com.zaxxer.hikari.HikariDataSource dataSource;
    private JdbcTemplate jdbc;
    private LoanMessageStore store;
    private NotificationProcessor processor;

    @BeforeAll void migrateIsolatedSchema() {
        dataSource = new com.zaxxer.hikari.HikariDataSource();
        dataSource.setJdbcUrl(System.getenv("TEST_DATABASE_URL"));
        dataSource.setUsername(System.getenv().getOrDefault("TEST_DATABASE_USER", "postgres"));
        dataSource.setPassword(System.getenv().getOrDefault("TEST_DATABASE_PASSWORD", "postgres"));
        dataSource.setSchema(schema);
        dataSource.setMinimumIdle(0);
        dataSource.setMaximumPoolSize(3);
        // Flyway changes session search_path internally. Do not reuse its connections in the test pool.
        var migrationSource = new org.springframework.jdbc.datasource.DriverManagerDataSource(
                System.getenv("TEST_DATABASE_URL"),
                System.getenv().getOrDefault("TEST_DATABASE_USER", "postgres"),
                System.getenv().getOrDefault("TEST_DATABASE_PASSWORD", "postgres"));
        Flyway.configure().dataSource(migrationSource).schemas(schema).defaultSchema(schema).target("6").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        UUID existingEvent = history(loan());
        Flyway.configure().dataSource(migrationSource).schemas(schema).defaultSchema(schema).load().migrate();
        assertEquals(existingEvent, jdbc.queryForObject("SELECT source_history_id FROM loan_message_outbox WHERE id = ?",
                UUID.class, existingEvent));
        store = new LoanMessageStore(jdbc, new ObjectMapper().findAndRegisterModules());
        processor = new NotificationProcessor(store);
    }

    @BeforeEach void clearFixtures() {
        assertEquals(schema, jdbc.queryForObject("SELECT current_schema()", String.class),
                "Refusing to touch fixtures outside the isolated test schema");
        jdbc.execute("TRUNCATE " + schema + ".users CASCADE");
    }

    @AfterAll void removeIsolatedSchema() {
        if (!schema.matches("messaging_test_[a-f0-9]{32}")) throw new IllegalStateException("Unsafe test schema");
        try {
            if (jdbc != null) jdbc.execute("DROP SCHEMA " + schema + " CASCADE");
        } finally { if (dataSource != null) dataSource.close(); }
    }

    private UUID history(UUID loanId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO loan_status_history(id, loan_application_id, to_status) VALUES (?, ?, 'SUBMITTED')",
                id, loanId);
        return id;
    }

    private UUID loan() {
        UUID user = UUID.randomUUID(), loan = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id,email,password_hash,role,full_name,phone) "
                + "VALUES (?,?,'test-hash','BORROWER','Test borrower','0700000000')", user, user + "@example.test");
        UUID product = jdbc.queryForObject("SELECT id FROM loan_products LIMIT 1", UUID.class);
        jdbc.update("INSERT INTO loan_applications(id,reference,applicant_id,product_id,product_type,"
                + "amount_requested,interest_rate_annual,term_months,purpose,status) "
                + "VALUES (?, ?, ?, ?, 'PERSONAL', 1000000, 10, 12, 'Test', 'SUBMITTED')",
                loan, "TEST-" + loan.toString().substring(0, 12), user, product);
        return loan;
    }

    private long count(String table) { return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class); }

    private LoanMessageStore.ClaimedMessage statusMessage(UUID id) {
        return new LoanMessageStore.ClaimedMessage(id, LoanMessageType.LOAN_STATUS_CHANGED);
    }

    private UUID delinquency(UUID loan, OffsetDateTime occurredAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO loan_status_history (id, loan_application_id, from_status, to_status, changed_at) "
                + "VALUES (?, ?, 'ACTIVE', 'DELINQUENT', ?)", id, loan, occurredAt);
        return id;
    }

    private UUID staff(String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id,email,password_hash,role,full_name,phone) "
                + "VALUES (?,?,'test-hash',?,'Test staff','0700000000')", id, id + "@example.test", role);
        return id;
    }

    @Test void repeatDelinquencyUsesEventTimeAndCreatesOneAssignedStaffAlert() {
        UUID loan = loan(), officer = staff("LOAN_OFFICER");
        staff("ADMIN");
        jdbc.update("UPDATE loan_applications SET assigned_officer_id = ? WHERE id = ?", officer, loan);
        var now = OffsetDateTime.parse("2026-11-21T12:00:00Z");
        UUID first = delinquency(loan, now.minusDays(30));
        UUID second = delinquency(loan, now);

        // The later message can arrive before the earlier message. Both history rows are committed.
        processor.consumeStatusFromBroker(second);
        processor.consumeStatusFromBroker(first);
        processor.consumeStatusFromBroker(second);
        assertEquals(first, jdbc.queryForObject("SELECT (payload->>'previousEventId')::uuid "
                + "FROM loan_message_outbox WHERE event_type = 'REPEAT_DELINQUENCY'", UUID.class));
        assertEquals(2, count("notifications")); // only the usual borrower alerts before derived delivery

        var oldToken = UUID.randomUUID();
        jdbc.update("CALL claim_loan_messages(?, ?)", 10, oldToken);
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM loan_message_outbox WHERE lease_token = ?",
                Long.class, oldToken));
        var claimed = store.claim(UUID.randomUUID());
        assertEquals(1, claimed.size());
        var composite = claimed.getFirst();
        assertEquals(LoanMessageType.REPEAT_DELINQUENCY, composite.type());
        processor.consumeRepeatDelinquency(composite.id());
        processor.consumeRepeatDelinquency(composite.id());

        assertEquals(3, count("notifications"));
        assertEquals(officer, jdbc.queryForObject("SELECT user_id FROM notifications WHERE source_event_id = ?",
                UUID.class, composite.id()));
        assertFalse(store.requestRetry(composite.id()));
    }

    @Test void previousDelinquencyMustBeOnTheSameLoanAndInsideTheWindow() {
        UUID loan = loan(), otherLoan = loan();
        var now = OffsetDateTime.parse("2026-11-21T12:00:00Z");
        delinquency(loan, now.minusDays(30).minusSeconds(1));
        delinquency(otherLoan, now.minusDays(1));
        processor.consumeStatusFromBroker(delinquency(loan, now));
        assertEquals(3, count("loan_message_outbox"));
        assertEquals(1, count("notifications"));
    }

    @Test void staffAlertCanBeRetriedAfterAnEligibleRecipientIsAdded() {
        UUID loan = loan(), disabledOfficer = staff("LOAN_OFFICER");
        jdbc.update("UPDATE users SET active = false WHERE id = ?", disabledOfficer);
        jdbc.update("UPDATE loan_applications SET assigned_officer_id = ? WHERE id = ?", disabledOfficer, loan);
        var now = OffsetDateTime.parse("2026-11-21T12:00:00Z");
        delinquency(loan, now.minusDays(5));
        processor.consumeStatusFromBroker(delinquency(loan, now));
        UUID event = jdbc.queryForObject("SELECT id FROM loan_message_outbox "
                + "WHERE event_type = 'REPEAT_DELINQUENCY'", UUID.class);

        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> processor.consumeRepeatDelinquency(event));
        assertTrue(store.requestRetry(event));
        UUID admin = staff("ADMIN");
        processor.consumeRepeatDelinquency(event);
        assertEquals(admin, jdbc.queryForObject("SELECT user_id FROM notifications WHERE source_event_id = ?",
                UUID.class, event));
    }

    @Test void triggersProduceOneAuditAndDurableEvent() {
        UUID loan = loan(), event = history(loan);
        assertEquals(1, count("audit_logs"));
        assertEquals(1, count("loan_message_outbox"));
        assertEquals(loan, store.load(event).applicationId());
        assertEquals("STATUS_SUBMITTED", jdbc.queryForObject("SELECT action FROM audit_logs", String.class));
    }

    @Test void rollbackRemovesHistoryAuditAndOutboxTogether() {
        var tx = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        assertThrows(IllegalStateException.class, () -> tx.execute(status -> {
            history(loan());
            assertEquals(1, count("audit_logs"));
            throw new IllegalStateException("Roll back this application");
        }));
        assertEquals(0, count("loan_applications"));
        assertEquals(0, count("loan_status_history"));
        assertEquals(0, count("audit_logs"));
        assertEquals(0, count("loan_message_outbox"));
    }

    @Test void redeliveryCreatesOnlyOneNotificationAndPreservesReadState() {
        UUID event = history(loan());
        processor.consume(event);
        jdbc.update("UPDATE notifications SET read_at = now()");
        processor.consume(event);
        assertEquals(1, count("notifications"));
        assertNotNull(jdbc.queryForObject("SELECT read_at FROM notifications", java.sql.Timestamp.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM loan_message_outbox WHERE consumed_at IS NOT NULL", Long.class));
    }

    @Test void failedNotificationInsertDoesNotConsumeTheEvent() {
        UUID event = history(loan());
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> store.recordNotification(event, "x".repeat(501)));
        assertEquals(0, count("notifications"));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM loan_message_outbox WHERE consumed_at IS NOT NULL", Long.class));
        processor.consume(event);
        assertEquals(1, count("notifications"));
    }

    @Test void claimsSkipLocksRecoverExpiredLeaseAndFenceOldPublisher() throws Exception {
        UUID locked = history(loan()), free = history(loan());
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement("SELECT id FROM loan_message_outbox WHERE id = ? FOR UPDATE")) {
                statement.setObject(1, locked);
                statement.executeQuery().close();
            }
            assertEquals(java.util.List.of(statusMessage(free)), store.claim(first));
            connection.rollback();
        }
        assertEquals(java.util.List.of(statusMessage(locked)), store.claim(second));
        assertTrue(store.claim(UUID.randomUUID()).isEmpty());
        jdbc.update("UPDATE loan_message_outbox SET lease_until = now() - interval '1 second' WHERE id = ?", free);
        UUID replacement = UUID.randomUUID();
        assertEquals(java.util.List.of(statusMessage(free)), store.claim(replacement));
        store.complete(free, first); // expired claim cannot acknowledge another worker's lease
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM loan_message_outbox WHERE published_at IS NOT NULL", Long.class));
        store.complete(free, replacement);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM loan_message_outbox WHERE published_at IS NOT NULL", Long.class));
    }

    @Test void adminRetryOnlyRequeuesUnconsumedEvents() {
        UUID event = history(loan()), token = UUID.randomUUID();
        store.claim(token); store.complete(event, token);
        assertTrue(store.requestRetry(event));
        assertEquals(java.util.List.of(statusMessage(event)), store.claim(UUID.randomUUID()));
        processor.consume(event);
        assertFalse(store.requestRetry(event));
        assertFalse(store.requestRetry(UUID.randomUUID()));
    }

    @Test void embeddedModeCompletesNotificationAndOutboxAtomically() {
        UUID loan = loan(), event = history(loan);
        var tx = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        tx.executeWithoutResult(status -> {
            assertEquals(schema, jdbc.queryForObject("SELECT current_schema()", String.class));
            assertEquals(java.util.List.of(event), store.pendingForApplication(loan));
            for (UUID id : store.pendingForApplication(loan)) { processor.consume(id); store.completeEmbedded(id); }
        });
        assertEquals(1, count("notifications"));
        assertTrue(store.claim(UUID.randomUUID()).isEmpty());
        processor.consume(event);
        assertEquals(1, count("notifications"));
    }

    @Test void springTransactionFlushesHistoryTriggersAndEmbeddedNotificationBeforeCommit() {
        UUID loan = loan();
        var app = new org.springframework.boot.builder.SpringApplicationBuilder(
                com.trustbank.loanapp.LoanBackendApplication.class);
        app.initializers(context -> {
            var properties = new java.util.HashMap<String, Object>();
            String url = System.getenv("TEST_DATABASE_URL");
            properties.put("spring.datasource.url", url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema);
            properties.put("spring.datasource.hikari.schema", schema);
            properties.put("spring.jpa.properties.hibernate.default_schema", schema);
            properties.put("spring.datasource.username", System.getenv().getOrDefault("TEST_DATABASE_USER", "postgres"));
            properties.put("spring.datasource.password", System.getenv().getOrDefault("TEST_DATABASE_PASSWORD", "postgres"));
            properties.put("spring.flyway.enabled", "false");
            properties.put("app.bootstrap-admin.enabled", "false");
            properties.put("app.messaging.enabled", "false");
            properties.put("app.messaging.worker-enabled", "false");
            properties.put("app.seed-demo-data", "false");
            properties.put("app.documents.mongodb-uri", "");
            properties.put("server.port", "0");
            properties.put("spring.datasource.hikari.minimum-idle", "0");
            properties.put("logging.level.com.trustbank.loanapp", "INFO");
            context.getEnvironment().getPropertySources().addFirst(
                    new org.springframework.core.env.MapPropertySource("isolated-messaging-test", properties));
        });
        try (var context = app.run()) {
            var tx = new TransactionTemplate(context.getBean(org.springframework.transaction.PlatformTransactionManager.class));
            tx.executeWithoutResult(status -> {
                assertEquals(schema, context.getBean(JdbcTemplate.class).queryForObject("SELECT current_schema()", String.class));
                var application = context.getBean(com.trustbank.loanapp.repo.LoanApplicationRepository.class)
                        .findById(loan).orElseThrow();
                context.getBean(com.trustbank.loanapp.service.LoanStateMachine.class).move(application,
                        com.trustbank.loanapp.domain.LoanStatus.PENDING_DOCUMENTS, null, "Test documents");
            });
            assertEquals(1, count("notifications"));
            assertEquals(1, count("audit_logs"));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM loan_message_outbox "
                    + "WHERE published_at IS NOT NULL AND consumed_at IS NOT NULL", Long.class));
        }
    }

    @Test void realBrokerRoutesConfirmedMessagesDeduplicatesAndDeadLetters() throws Exception {
        Assumptions.assumeTrue(System.getenv("TEST_RABBITMQ_ADDRESS") != null);
        var factory = new CachingConnectionFactory();
        factory.setUri(System.getenv("TEST_RABBITMQ_ADDRESS"));
        factory.setPublisherConfirmType(ConfirmType.CORRELATED);
        factory.setPublisherReturns(true);
        var rabbit = new RabbitTemplate(factory);
        rabbit.setMandatory(true);
        var admin = new RabbitAdmin(factory);
        var config = new RabbitMessagingConfig();
        try {
            admin.declareExchange(config.loanExchange()); admin.declareExchange(config.loanDeadExchange());
            admin.declareQueue(config.loanQueue()); admin.declareQueue(config.loanDeadQueue());
            admin.declareBinding(config.loanBinding()); admin.declareBinding(config.loanDeadBinding());
            admin.declareQueue(config.staffAlertQueue()); admin.declareQueue(config.staffAlertDeadQueue());
            admin.declareBinding(config.staffAlertBinding()); admin.declareBinding(config.staffAlertDeadBinding());
            UUID event = history(loan());
            var publisher = new RabbitLoanPublisher(rabbit);
            publisher.publish(statusMessage(event));
            var message = rabbit.receive(RabbitMessagingConfig.QUEUE, 5000);
            assertNotNull(message);
            var consumer = new RabbitNotificationConsumer(processor);
            consumer.consume(message); consumer.consume(message);
            assertEquals(1, count("notifications"));

            UUID adminId = staff("ADMIN"), delinquentLoan = loan();
            var now = OffsetDateTime.parse("2026-11-21T12:00:00Z");
            delinquency(delinquentLoan, now.minusDays(1));
            publisher.publish(statusMessage(delinquency(delinquentLoan, now)));
            Message status = rabbit.receive(RabbitMessagingConfig.QUEUE, 5000);
            assertNotNull(status);
            consumer.consume(status);
            UUID compositeId = jdbc.queryForObject("SELECT id FROM loan_message_outbox "
                    + "WHERE event_type = 'REPEAT_DELINQUENCY'", UUID.class);
            publisher.publish(new LoanMessageStore.ClaimedMessage(compositeId, LoanMessageType.REPEAT_DELINQUENCY));
            Message alert = rabbit.receive(RabbitMessagingConfig.STAFF_ALERT_QUEUE, 5000);
            assertNotNull(alert);
            consumer.consumeStaffAlert(alert);
            consumer.consumeStaffAlert(alert);
            assertEquals(adminId, jdbc.queryForObject("SELECT user_id FROM notifications WHERE source_event_id = ?",
                    UUID.class, compositeId));
            rabbit.send(RabbitMessagingConfig.EXCHANGE, RabbitMessagingConfig.KEY,
                    new Message("invalid".getBytes(java.nio.charset.StandardCharsets.UTF_8), new MessageProperties()));
            var listener = new org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer(factory);
            listener.setQueueNames(RabbitMessagingConfig.QUEUE);
            listener.setDefaultRequeueRejected(false);
            listener.setMessageListener((org.springframework.amqp.core.MessageListener) consumer::consume);
            try {
                listener.start();
                assertNotNull(rabbit.receive(RabbitMessagingConfig.DEAD_QUEUE, 10000));
            } finally { listener.stop(); }
            admin.removeBinding(config.loanBinding());
            assertThrows(IllegalStateException.class, () -> publisher.publish(statusMessage(event)));
        } finally {
            admin.deleteQueue(RabbitMessagingConfig.STAFF_ALERT_QUEUE);
            admin.deleteQueue(RabbitMessagingConfig.STAFF_ALERT_DEAD_QUEUE);
            admin.deleteQueue(RabbitMessagingConfig.QUEUE); admin.deleteQueue(RabbitMessagingConfig.DEAD_QUEUE);
            admin.deleteExchange(RabbitMessagingConfig.EXCHANGE); admin.deleteExchange(RabbitMessagingConfig.DEAD_EXCHANGE);
            factory.destroy();
        }
    }
}
