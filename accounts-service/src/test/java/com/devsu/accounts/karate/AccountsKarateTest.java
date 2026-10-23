package com.devsu.accounts.karate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import com.devsu.accounts.TestcontainersConfiguration;
import com.devsu.accounts.application.port.out.CustomerReferenceRepositoryPort;
import com.intuit.karate.Results;
import com.intuit.karate.Runner;

/**
 * Runs the Karate API tests (src/test/resources/karate) against the real application on a random port, backed by
 * PostgreSQL and RabbitMQ containers, so they execute together with every other test in {@code ./mvnw test}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AccountsKarateTest {

	@LocalServerPort
	private int port;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private CustomerReferenceRepositoryPort customerReferences;

	@BeforeEach
	void setUp() {
		CustomerEvents.init(rabbitTemplate, customerReferences);
	}

	@Test
	@DisplayName("Karate API scenarios")
	void karateScenarios() {
		Results results = Runner.path("classpath:karate")
				.systemProperty("baseUrl", "http://localhost:" + port)
				.outputCucumberJson(false)
				.parallel(1);

		assertThat(results.getFailCount()).as(results.getErrorMessages()).isZero();
	}
}
