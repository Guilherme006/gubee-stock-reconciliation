package com.gubee.stockreconciliation.adapter.out.observability;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component("kafka")
@Profile("!test")
class KafkaHealthIndicator implements HealthIndicator {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final KafkaProperties kafkaProperties;

    KafkaHealthIndicator(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Override
    public Health health() {
        var config = Map.<String, Object>of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                String.join(",", kafkaProperties.getBootstrapServers())
        );

        try (var adminClient = AdminClient.create(config)) {
            var clusterId = adminClient.describeCluster()
                    .clusterId()
                    .get(TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            return Health.up()
                    .withDetail("clusterId", clusterId)
                    .build();
        } catch (Exception exception) {
            return Health.down(exception).build();
        }
    }
}
