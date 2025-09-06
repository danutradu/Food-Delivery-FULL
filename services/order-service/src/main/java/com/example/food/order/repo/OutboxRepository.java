package com.example.food.order.repo;

import com.example.food.order.domain.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface OutboxRepository extends JpaRepository<OutboxEventEntity, UUID> {
  List<OutboxEventEntity> findTop50ByPublishedFalseOrderByCreatedAtAsc();
}
