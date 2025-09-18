package com.example.food.order.service;

import com.example.food.order.exception.OrderNotFoundException;
import com.example.food.order.model.OrderStatus;
import com.example.food.order.repository.OrderRepository;
import fd.delivery.DeliveryRequestedV1;
import fd.payment.PaymentAuthorizedV1;
import fd.payment.PaymentFailedV1;
import fd.restaurant.RestaurantAcceptanceRequestedV1;
import fd.restaurant.RestaurantAcceptedV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaListener {

    private final KafkaTemplate<String, SpecificRecord> kafka;
    private final OrderRepository orderRepository;

    @RetryableTopic(
            attempts = "${kafka.retry.attempts}",
            backoff = @Backoff(delayExpression = "${kafka.retry.delay}", multiplierExpression = "${kafka.retry.multiplier}", maxDelayExpression = "${kafka.retry.max-delay}"),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {Exception.class}
    )
    @KafkaListener(id = "order-on-payment-auth", topics = "fd.payment.authorized.v1", groupId = "order-service")
    public void onPaymentAuthorized(PaymentAuthorizedV1 event) {
        log.info("Payment authorized, requesting restaurant acceptance orderId={}", event.getOrderId());

        var order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + event.getOrderId()));

        RestaurantAcceptanceRequestedV1 out = new RestaurantAcceptanceRequestedV1(
                UUID.randomUUID(),
                Instant.now(),
                event.getOrderId(),
                order.getRestaurantId(),
                event.getAmountCents(),
                event.getCurrency()
        );

        ProducerRecord<String, SpecificRecord> rec = new ProducerRecord<>("fd.restaurant.acceptance-requested.v1", event.getOrderId().toString(), out);
        rec.headers().add("eventType", "fd.restaurant.RestaurantAcceptanceRequestedV1".getBytes());
        rec.headers().add("eventType", event.getEventId().toString().getBytes());

        try {
            kafka.send(rec).get(); // Synchronous send
            log.info("Published restaurant acceptance requested event orderId={}", event.getOrderId());

            // Only update status if publish succeeded
            order.setStatus(OrderStatus.PAYMENT_AUTHORIZED);
            orderRepository.save(order);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while publishing restaurant acceptance requested event orderId={}", event.getOrderId(), ex);
            throw new RuntimeException("Publishing interrupted", ex);
        } catch (Exception ex) {
            log.error("Failed to publish restaurant acceptance requested event orderId={}", event.getOrderId(), ex);
            throw new RuntimeException("Publishing failed", ex);
        }
    }

    @RetryableTopic(
            attempts = "${kafka.retry.attempts}",
            backoff = @Backoff(delayExpression = "${kafka.retry.delay}", multiplierExpression = "${kafka.retry.multiplier}", maxDelayExpression = "${kafka.retry.max-delay}"),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {Exception.class}
    )
    @KafkaListener(id = "order-on-restaurant-accepted", topics = "fd.restaurant.accepted.v1", groupId = "order-service")
    public void onRestaurantAccepted(RestaurantAcceptedV1 event) {
        log.info("Restaurant accepted, requesting delivery orderId={}", event.getOrderId());
        var order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + event.getOrderId()));

        DeliveryRequestedV1 out = new DeliveryRequestedV1(
                UUID.randomUUID(),
                Instant.now(),
                event.getOrderId(),
                event.getRestaurantId(),
                order.getCustomerUserId()
        );

        ProducerRecord<String, SpecificRecord> rec = new ProducerRecord<>("fd.delivery.requested.v1", event.getOrderId().toString(), out);
        rec.headers().add("eventType", "fd.delivery.DeliveryRequestedV1".getBytes());
        rec.headers().add("eventType", event.getEventId().toString().getBytes());

        try {
            kafka.send(rec).get(); // Synchronous send
            log.info("Published delivery requested event orderId={}", event.getOrderId());

            // Only update status if publish succeeded
            order.setStatus(OrderStatus.RESTAURANT_ACCEPTED);
            orderRepository.save(order);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while publishing delivery requested event orderId={}", event.getOrderId(), ex);
            throw new RuntimeException("Publishing interrupted", ex);
        } catch (Exception ex) {
            log.error("Failed to publish delivery requested event orderId={}", event.getOrderId(), ex);
            throw new RuntimeException("Publishing failed", ex);
        }
    }

    @RetryableTopic(
            attempts = "${kafka.retry.attempts}",
            backoff = @Backoff(delayExpression = "${kafka.retry.delay}", multiplierExpression = "${kafka.retry.multiplier}", maxDelayExpression = "${kafka.retry.max-delay}"),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {Exception.class}
    )
    @KafkaListener(id = "order-on-payment-failed", topics = "fd.payment.failed.v1", groupId = "order-service")
    public void onPaymentFailed(PaymentFailedV1 event) {
        log.info("Payment failed, cancelling order orderId={} reason={}", event.getOrderId(), event.getFailureReason());

        var order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + event.getOrderId()));

        order.setStatus(OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);

        log.info("Order cancelled due to payment failure orderId={}", event.getOrderId());
        // TODO: Notify customer about payment failure
        // TODO: Release any reserved inventory
    }

    @DltHandler
    public void handleDltPaymentAuthorized(PaymentAuthorizedV1 event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Payment authorized event failed after all retries, marking order as failed orderId={} topic={}", event.getOrderId(), topic);

        var order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + event.getOrderId()));

        order.setStatus(OrderStatus.PROCESSING_FAILED);
        orderRepository.save(order);

        // TODO: Notify customer about processing failure
        // TODO: Consider compensation actions
    }

    @DltHandler
    public void handleDltRestaurantAccepted(RestaurantAcceptedV1 event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Restaurant Accepted event failed after all retries, marking order as failed orderId={} topic={}", event.getOrderId(), topic);

        var order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + event.getOrderId()));

        order.setStatus(OrderStatus.PROCESSING_FAILED);
        orderRepository.save(order);

        // TODO: Notify customer about processing failure
        // TODO: Consider compensation actions
    }
}
