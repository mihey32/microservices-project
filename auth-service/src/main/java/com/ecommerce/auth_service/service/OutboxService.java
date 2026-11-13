package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.model.OutboxEvent;
import com.ecommerce.auth_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void saveEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .createdAt(Instant.now())
                    .processed(false)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.debug("Outbox event saved: type={}, aggregateId={}", eventType, aggregateId);

        } catch (Exception e) {
            log.error("Failed to serialize outbox event payload", e);
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }
}
