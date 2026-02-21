package com.vehiclerental.common.messaging.cleanup;

import com.vehiclerental.common.messaging.outbox.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class OutboxCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanupScheduler.class);

    private final OutboxEventRepository outboxEventRepository;

    public OutboxCleanupScheduler(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldPublishedEvents() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        int deletedCount = outboxEventRepository.deletePublishedBefore(cutoff);
        log.info("Outbox cleanup: deleted {} published events older than 7 days", deletedCount);
    }
}
