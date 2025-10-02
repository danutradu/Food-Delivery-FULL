package com.example.food.cart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupLoader implements ApplicationRunner {

    @Value("${catalog.service.url}")
    private String catalogServiceUrl;

    private final MenuItemCacheService menuItemCacheService;
    private final TaskScheduler taskScheduler;
    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:8085")
            .build();

    private volatile boolean cacheInitialized = false;
    private ScheduledFuture<?> retryTask;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initializeCache()
                .doOnError(error -> {
                    log.warn("Initial cache load failed, starting retry scheduler: {}", error.getMessage());
                    startRetryScheduler();
                })
                .doOnSuccess(result -> {
                    cacheInitialized = true;
                    log.info("Menu item cache initialization completed");
                })
                .subscribe();
    }

    private void startRetryScheduler() {
        if (retryTask == null) {
            retryTask = taskScheduler.scheduleWithFixedDelay(
                    this::retryInitializeCache,
                    Duration.ofSeconds(30)
            );
        }
    }

    private Mono<Void> initializeCache() {
        log.info("Initializing menu item cache from catalog-service...");

        return webClient.get()
                .uri("/restaurants")
                .retrieve()
                .bodyToMono(RestaurantDto[].class)
                .timeout(Duration.ofSeconds(5))
                .flatMapIterable(restaurants -> Arrays.asList(restaurants))
                .flatMap(restaurant ->
                        webClient.get()
                                .uri("/restaurants/{restaurantId}/menu", restaurant.id())
                                .retrieve()
                                .bodyToMono(MenuItemDto[].class)
                                .timeout(Duration.ofSeconds(5))
                                .doOnNext(menuItems -> {
                                    for (var item : menuItems) {
                                        menuItemCacheService.updateMenuItem(
                                                restaurant.id(),
                                                item.id(),
                                                item.name(),
                                                item.priceCents(),
                                                item.currency(),
                                                item.available()
                                        );
                                    }
                                })
                )
                .then();
    }

    private void retryInitializeCache() {
        if (!cacheInitialized) {
            log.info("Cache not initialized, retrying...");
            initializeCache()
                    .doOnSuccess(result -> {
                        cacheInitialized = true;
                        retryTask.cancel(false);
                        log.info("Menu item cache initialization completed, stopped retry scheduler");
                    })
                    .doOnError(error -> log.warn("Retry failed: {}", error.getMessage()))
                    .subscribe();
        }
    }

    public record RestaurantDto(UUID id, String name) {
    }

    public record MenuItemDto(UUID id, String name, int priceCents, String currency, boolean available) {
    }
}
