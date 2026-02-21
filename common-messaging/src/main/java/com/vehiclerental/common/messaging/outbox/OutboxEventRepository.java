package com.vehiclerental.common.messaging.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = 'PUBLISHED' AND e.publishedAt < :cutoff")
    int deletePublishedBefore(@Param("cutoff") Instant cutoff);
}
