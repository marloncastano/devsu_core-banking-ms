package com.devsu.accounts.karate;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.devsu.accounts.application.port.out.CustomerReferenceRepositoryPort;
import com.devsu.accounts.infrastructure.config.CustomerEventsTopology;

/**
 * Called from the Karate features ({@code Java.type(...)}): customers-service is not running in these tests, so
 * customers are created by publishing the same RabbitMQ event it would publish.
 */
public final class CustomerEvents {

	private static RabbitTemplate rabbitTemplate;
	private static CustomerReferenceRepositoryPort customerReferences;

	private CustomerEvents() {
	}

	static void init(RabbitTemplate template, CustomerReferenceRepositoryPort references) {
		rabbitTemplate = template;
		customerReferences = references;
	}

	/**
	 * Publishes a customer.created event and waits until accounts-service has stored the customer.
	 *
	 * @return the generated customerId.
	 */
	public static String created(String name) {
		UUID customerId = UUID.randomUUID();
		publish("customer.created", "CREATED", customerId, name, true);
		await().atMost(Duration.ofSeconds(10)).until(() -> customerReferences.findById(customerId).isPresent());
		return customerId.toString();
	}

	/**
	 * Publishes a customer.deactivated event and waits until the local copy reflects it.
	 */
	public static void deactivated(String customerId, String name) {
		UUID id = UUID.fromString(customerId);
		publish("customer.deactivated", "DEACTIVATED", id, name, false);
		await().atMost(Duration.ofSeconds(10))
				.until(() -> customerReferences.findById(id).map(customer -> !customer.active()).orElse(false));
	}

	private static void publish(String routingKey, String type, UUID customerId, String name, boolean active) {
		rabbitTemplate.convertAndSend(CustomerEventsTopology.EXCHANGE, routingKey, Map.of(
				"eventId", UUID.randomUUID().toString(),
				"eventType", type,
				"customerId", customerId.toString(),
				"name", name,
				"active", active,
				"occurredAt", Instant.now().toString()));
	}
}
