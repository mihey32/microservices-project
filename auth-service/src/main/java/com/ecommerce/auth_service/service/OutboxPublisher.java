package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.model.OutboxEvent;
import com.ecommerce.auth_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> stringKafkaTemplate;

    @Scheduled(fixedDelayString = "${outbox.publish.interval:5000}" )
    @Transactional
    public void publishEvents() {
        List<OutboxEvent> events = outboxEventRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc();

        if (events.isEmpty()) {
            return;
        }

        log.info("Found {} unprocessed outbox events", events.size());

        for (OutboxEvent event : events) {

            try {
                stringKafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload()).get();
                event.setProcessed(true);
                event.setProcessedAt(Instant.now());
                outboxEventRepository.save(event);

                log.info("Successfully published outbox event: id={}, topic={}", event.getId(), event.getEventType());

            } catch (Exception e) {
                log.error("Failed to publish outbox event id={}. Will retry later.", event.getId(), e);
                break;
            }
        }
    }
}
