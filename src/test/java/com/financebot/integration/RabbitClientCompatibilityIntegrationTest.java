package com.financebot.integration;

import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RabbitClientCompatibilityIntegrationTest {
    @Container
    static final GenericContainer<?> RABBIT = new GenericContainer<>("rabbitmq:4-management-alpine")
            .withEnv("RABBITMQ_DEFAULT_USER", "compatibility_test")
            .withEnv("RABBITMQ_DEFAULT_PASS", "test-only-broker-password")
            .withExposedPorts(5672)
            .waitingFor(Wait.forLogMessage(".*Server startup complete.*\\n", 1)
                    .withStartupTimeout(Duration.ofSeconds(90)));

    @Test
    void securityUpdatedClientSupportsPublisherConfirmsRedeliveryAndAcknowledgement() throws Exception {
        var factory = new ConnectionFactory();
        factory.setHost(RABBIT.getHost());
        factory.setPort(RABBIT.getMappedPort(5672));
        factory.setUsername("compatibility_test");
        factory.setPassword("test-only-broker-password");
        try (var connection = factory.newConnection(); var channel = connection.createChannel()) {
            String queue = channel.queueDeclare("", false, true, true, null).getQueue();
            byte[] body = "opaque-notification-id".getBytes(StandardCharsets.UTF_8);
            channel.confirmSelect();
            channel.basicPublish("", queue, null, body);
            channel.waitForConfirmsOrDie(5000);

            var original = channel.basicGet(queue, false);
            assertThat(original).isNotNull();
            assertThat(original.getBody()).isEqualTo(body);
            channel.basicNack(original.getEnvelope().getDeliveryTag(), false, true);
            // basicGet é síncrono e usa o mesmo canal: o nack anterior já foi processado.
            var redelivered = channel.basicGet(queue, false);
            assertThat(redelivered).isNotNull();
            assertThat(redelivered.getEnvelope().isRedeliver()).isTrue();
            assertThat(redelivered.getBody()).isEqualTo(body);
            channel.basicAck(redelivered.getEnvelope().getDeliveryTag(), false);
            assertThat(channel.basicGet(queue, true)).isNull();
        }
    }
}
