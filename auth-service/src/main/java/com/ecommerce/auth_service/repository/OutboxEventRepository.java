package com.ecommerce.auth_service.repository;

import com.ecommerce.auth_service.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop100ByProcessedFalseOrderByCreatedAtAsc();
}
